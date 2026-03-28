#!/bin/bash
# ═══════════════════════════════════════════════════════════════════════════════
# update-hytale.sh — Full Hytale server update & deployment pipeline
# ═══════════════════════════════════════════════════════════════════════════════
#
# Runs directly on the k3s host. Two modes:
#
#   (default) Pull latest everhytale/hytale-server image from Docker Hub.
#             Checks every 12h for new Hytale releases. No credentials needed.
#
#   --build   Download official game files directly from Hytale using the
#             official Hytale downloader, then build & import a custom image.
#             Use this when the Docker Hub image is stale (everhytale updates
#             every 12h, but if they fall behind, --build gets the real latest).
#             Requires a Hytale account — run kubernetes/hytale-auth.sh first.
#
# Usage:
#   sudo /home/dad/hytale-mmorpg-mod/kubernetes/update-hytale.sh
#   sudo /home/dad/hytale-mmorpg-mod/kubernetes/update-hytale.sh --build
#
# ═══════════════════════════════════════════════════════════════════════════════

set -euo pipefail

# ── Constants ─────────────────────────────────────────────────────────────────

REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
KUBERNETES_DIR="$REPO_DIR/kubernetes"
MODS_DIR="$REPO_DIR/mods"
CONFIGS_DIR="$REPO_DIR/configs"
BACKUP_DIR="$REPO_DIR/backup"

# Docker Hub image — community-maintained, auto-built every 12h from official releases
REGISTRY_IMAGE="docker.io/everhytale/hytale-server:latest"

# --build mode: official Hytale downloader + game files
BUILD_DIR="/opt/hytale-builder"
DOWNLOADER_DIR="$BUILD_DIR/hytale-downloader"
DOWNLOADER="$DOWNLOADER_DIR/hytale-downloader-linux-amd64"
# Credentials are managed by the downloader itself via its own OAuth flow.
# Run 'sudo kubernetes/hytale-auth.sh' to authenticate (prompts once in browser).
CREDENTIALS_FILE="$BUILD_DIR/.hytale-downloader-credentials.json"
GAME_FILES_DIR="$BUILD_DIR/game-files"
LOCAL_IMAGE_BASE="hytale-server"

# Kubernetes
NAMESPACE="hytale"
DEPLOYMENT_NAME="hytale-server"
CONTAINER_NAME="hytale"
TEST_POD_NAME="hytale-test"

# PVC — the actual host path backing the hytale-server PersistentVolumeClaim
PVC_PATH="/var/lib/rancher/k3s/storage/pvc-2a0b4ba5-ac88-4e47-a9b9-6f8278d40263_hytale_hytale-server"
PVC_MODS="$PVC_PATH/mods"
PVC_CONFIGS="$PVC_PATH/configs"

# Staging
TEST_STAGING="/tmp/hytale-test-mods"

# Timing
TEST_BOOT_WAIT=90
ROLLOUT_TIMEOUT=120

# Mode
BUILD_MODE=false
[[ "${1:-}" == "--build" ]] && BUILD_MODE=true

# State
IMAGE_UPDATED=false
OLD_IMAGE=""
NEW_IMAGE=""

# ── Helpers ───────────────────────────────────────────────────────────────────

log()     { echo ""; echo "══════════════════════════════════════"; echo "  $1"; echo "══════════════════════════════════════"; }
info()    { echo "  ✓ $1"; }
warn()    { echo "  ⚠ $1"; }
fail()    { echo ""; echo "  ✗ FAILED: $1" >&2; exit 1; }

cleanup_test_pod() {
    k3s kubectl delete pod "$TEST_POD_NAME" -n "$NAMESPACE" --ignore-not-found --wait=false >/dev/null 2>&1 || true
    rm -rf "$TEST_STAGING"
}

# ── Pre-flight checks ────────────────────────────────────────────────────────

log "Pre-flight checks"

[[ $EUID -eq 0 ]] || fail "This script must be run as root (sudo)"
[[ -d "$REPO_DIR/mods" ]] || fail "Mods directory not found: $REPO_DIR/mods"
[[ -d "$PVC_PATH" ]]      || fail "PVC path not found: $PVC_PATH"

if [[ "$BUILD_MODE" == true ]]; then
    command -v unzip &>/dev/null || fail "unzip is required for --build mode: sudo apt install unzip"
    command -v docker &>/dev/null || command -v nerdctl &>/dev/null || fail "docker or nerdctl required for --build mode"
fi

if command -v docker &>/dev/null; then
    DOCKER_CMD="docker"
elif command -v nerdctl &>/dev/null; then
    DOCKER_CMD="nerdctl"
else
    DOCKER_CMD=""
fi

info "Repo:  $REPO_DIR"
info "PVC:   $PVC_PATH"
info "Mode:  $([[ "$BUILD_MODE" == true ]] && echo "build from source (official Hytale downloader)" || echo "pull from Docker Hub (everhytale/hytale-server:latest)")"

# ── Step 1: Backup current server state ──────────────────────────────────────

log "Step 1: Backing up current server state"

rm -rf "$BACKUP_DIR"
mkdir -p "$BACKUP_DIR/mods" "$BACKUP_DIR/world"

if [[ -d "$PVC_MODS" ]] && ls "$PVC_MODS"/*.jar &>/dev/null; then
    cp -a "$PVC_MODS"/*.jar "$BACKUP_DIR/mods/"
    info "Backed up $(ls "$BACKUP_DIR/mods/" | wc -l) mod jar(s)"
else
    warn "No mods found in PVC to back up"
fi

find "$PVC_PATH" -mindepth 1 -maxdepth 1 ! -name "mods" -exec cp -a {} "$BACKUP_DIR/world/" \;
info "Backed up world data from PVC"

OLD_IMAGE=$(k3s kubectl get deployment "$DEPLOYMENT_NAME" -n "$NAMESPACE" \
    -o jsonpath='{.spec.template.spec.containers[0].image}' 2>/dev/null || echo "unknown")
echo "$OLD_IMAGE" > "$BACKUP_DIR/backed_up_image.txt"
info "Current image: $OLD_IMAGE"

# ── Step 2: Get latest game image ─────────────────────────────────────────────

if [[ "$BUILD_MODE" == true ]]; then

    # ── Build mode: download official game files from Hytale + build image ────

    log "Step 2: Downloading official Hytale game files"

    # Download the Hytale downloader binary if not present
    if [[ ! -x "$DOWNLOADER" ]]; then
        info "Downloading Hytale downloader..."
        mkdir -p "$DOWNLOADER_DIR"
        curl -fsSL "https://downloader.hytale.com/hytale-downloader.zip" \
            -o "$DOWNLOADER_DIR/hytale-downloader.zip" \
            || fail "Failed to download from https://downloader.hytale.com/hytale-downloader.zip"
        unzip -q "$DOWNLOADER_DIR/hytale-downloader.zip" -d "$DOWNLOADER_DIR"
        rm "$DOWNLOADER_DIR/hytale-downloader.zip"
        chmod +x "$DOWNLOADER"
        info "Hytale downloader installed at $DOWNLOADER"
    else
        info "Hytale downloader already present"
    fi

    # Check credentials — created by the downloader's own auth flow (hytale-auth.sh)
    [[ -f "$CREDENTIALS_FILE" ]] || fail "Hytale credentials not found: $CREDENTIALS_FILE\n  Authenticate first: sudo $KUBERNETES_DIR/hytale-auth.sh"

    # Get latest version using downloader's own credentials
    HYTALE_VERSION=$(
        "$DOWNLOADER" \
            -credentials-path "$CREDENTIALS_FILE" \
            -print-version \
            -skip-update-check \
            2>&1
    ) || fail "Could not query latest Hytale version: $HYTALE_VERSION\n  Try re-authenticating: sudo $KUBERNETES_DIR/hytale-auth.sh"
    info "Latest Hytale version: $HYTALE_VERSION"

    # Skip download if game files are already current
    CURRENT_DOWNLOADED=""
    [[ -f "$GAME_FILES_DIR/.version" ]] && CURRENT_DOWNLOADED=$(cat "$GAME_FILES_DIR/.version")

    if [[ "$CURRENT_DOWNLOADED" == "$HYTALE_VERSION" ]]; then
        info "Game files already at $HYTALE_VERSION — skipping download"
    else
        info "Downloading game files for $HYTALE_VERSION ..."
        "$DOWNLOADER" \
            -credentials-path "$CREDENTIALS_FILE" \
            -download-path "$BUILD_DIR/game.zip" \
            -skip-update-check \
            || fail "Hytale game download failed"
        rm -rf "$GAME_FILES_DIR"
        mkdir -p "$GAME_FILES_DIR"
        unzip -q game.zip -d "$GAME_FILES_DIR"
        rm game.zip
        echo "$HYTALE_VERSION" > "$GAME_FILES_DIR/.version"
        info "Game files downloaded and extracted"
    fi

    log "Step 2b: Building Docker image hytale-server:$HYTALE_VERSION"

    # Set up build context: game files + Dockerfile
    BUILD_CONTEXT="$BUILD_DIR/docker-build"
    rm -rf "$BUILD_CONTEXT"
    mkdir -p "$BUILD_CONTEXT"
    cp -r "$GAME_FILES_DIR/Server" "$BUILD_CONTEXT/Server"
    cp "$GAME_FILES_DIR/Assets.zip" "$BUILD_CONTEXT/Assets.zip"
    cp "$KUBERNETES_DIR/Dockerfile" "$BUILD_CONTEXT/Dockerfile"

    # Remove Hytale's broken dev/test NPC configs — they ship with throw speed
    # values that fail validation and crash the server on 2026.03.26+
    rm -rf "$BUILD_CONTEXT/Server/NPC/Roles/_Core/Tests" \
           "$BUILD_CONTEXT/Server/NPC/Roles/_Core/Tests_Development"
    info "Removed broken test NPC configs from build context"

    NEW_IMAGE="${LOCAL_IMAGE_BASE}:${HYTALE_VERSION}"
    $DOCKER_CMD build \
        -t "${LOCAL_IMAGE_BASE}:latest" \
        -t "$NEW_IMAGE" \
        "$BUILD_CONTEXT" \
        || fail "Docker image build failed"
    rm -rf "$BUILD_CONTEXT"
    info "Built image: $NEW_IMAGE"

    log "Step 2c: Importing image into k3s containerd"
    $DOCKER_CMD save "$NEW_IMAGE" | k3s ctr images import - \
        || fail "Failed to import image into k3s"
    info "Image imported: $NEW_IMAGE"

    IMAGE_UPDATED=true

else

    # ── Pull mode: pull latest from Docker Hub ─────────────────────────────────

    log "Step 2: Pulling latest Hytale image from Docker Hub"

    # Record current digest so we can detect if anything changed
    OLD_DIGEST=$(k3s ctr images list 2>/dev/null \
        | awk '/everhytale\/hytale-server:latest/ {print $3}' | head -1 || echo "none")

    info "Pulling $REGISTRY_IMAGE ..."
    k3s ctr images pull "$REGISTRY_IMAGE" \
        || fail "Failed to pull $REGISTRY_IMAGE — check internet connectivity"
    info "Pull complete"

    NEW_DIGEST=$(k3s ctr images list 2>/dev/null \
        | awk '/everhytale\/hytale-server:latest/ {print $3}' | head -1 || echo "unknown")

    if [[ "$OLD_DIGEST" != "$NEW_DIGEST" && "$OLD_DIGEST" != "none" ]]; then
        info "New image available (digest: $OLD_DIGEST → $NEW_DIGEST)"
        IMAGE_UPDATED=true
    elif [[ "$OLD_DIGEST" == "none" ]]; then
        info "Image pulled for the first time"
        IMAGE_UPDATED=true
    else
        info "Image is already up to date ($NEW_DIGEST)"
    fi

    NEW_IMAGE="$REGISTRY_IMAGE"

fi

# ── Step 3: Stage mods for testing ───────────────────────────────────────────

log "Step 3: Staging mods for test pod"

cleanup_test_pod
mkdir -p "$TEST_STAGING"

MOD_COUNT=0
if ls "$MODS_DIR"/*.jar &>/dev/null; then
    cp "$MODS_DIR"/*.jar "$TEST_STAGING/"
    MOD_COUNT=$(ls "$TEST_STAGING"/*.jar 2>/dev/null | wc -l)
fi
if ls "$MODS_DIR"/*.zip &>/dev/null; then
    cp "$MODS_DIR"/*.zip "$TEST_STAGING/"
    ZIP_COUNT=$(ls "$TEST_STAGING"/*.zip 2>/dev/null | wc -l)
    MOD_COUNT=$((MOD_COUNT + ZIP_COUNT))
fi
info "Staged $MOD_COUNT mod(s) from $MODS_DIR"
info "Test image: $NEW_IMAGE"

# ── Step 4: Spin up test pod ─────────────────────────────────────────────────

log "Step 4: Launching test pod"

# Local images (--build mode) must not be pulled; Hub images should always pull
PULL_POLICY="$([[ "$BUILD_MODE" == true ]] && echo "Never" || echo "Always")"

cat <<EOF | k3s kubectl apply -f -
apiVersion: v1
kind: Pod
metadata:
  name: $TEST_POD_NAME
  namespace: $NAMESPACE
  labels:
    app: hytale-test
spec:
  restartPolicy: Never
  containers:
  - name: hytale
    image: $NEW_IMAGE
    imagePullPolicy: $PULL_POLICY
    volumeMounts:
    - name: test-mods
      mountPath: /server/mods
  volumes:
  - name: test-mods
    hostPath:
      path: $TEST_STAGING
      type: Directory
EOF

info "Test pod created, waiting ${TEST_BOOT_WAIT}s for it to stabilize or crash..."

HEALTHY=false
for i in $(seq 1 "$TEST_BOOT_WAIT"); do
    PHASE=$(k3s kubectl get pod "$TEST_POD_NAME" -n "$NAMESPACE" \
        -o jsonpath='{.status.phase}' 2>/dev/null || echo "Unknown")
    case "$PHASE" in
        Running)  : ;;  # still alive — keep waiting the full window
        Failed|Unknown|Succeeded) break ;;
    esac
    sleep 1
done

# Only potentially healthy if still Running after the full wait
PHASE=$(k3s kubectl get pod "$TEST_POD_NAME" -n "$NAMESPACE" \
    -o jsonpath='{.status.phase}' 2>/dev/null || echo "Unknown")
[[ "$PHASE" == "Running" ]] && HEALTHY=true

# Dump full logs to a file for diagnosis, then show the important parts
TEST_LOG="/tmp/hytale-test-pod.log"
k3s kubectl logs "$TEST_POD_NAME" -n "$NAMESPACE" > "$TEST_LOG" 2>/dev/null || true
LOG_LINES=$(wc -l < "$TEST_LOG" 2>/dev/null || echo 0)

echo ""
echo "── Full test pod log saved to $TEST_LOG ($LOG_LINES lines) ──"
echo "── Test pod logs (full output, deduped) ──"
# Show all unique lines to skip the repeated "failed to validate npc's" spam
awk '!seen[$0]++' "$TEST_LOG" 2>/dev/null || echo "(no logs available)"
echo "────────────────────────────────────"

RESTARTS=$(k3s kubectl get pod "$TEST_POD_NAME" -n "$NAMESPACE" \
    -o jsonpath='{.status.containerStatuses[0].restartCount}' 2>/dev/null || echo "0")
[[ "$RESTARTS" -gt 0 ]] && { warn "Test pod restarted $RESTARTS time(s) — treating as unhealthy"; HEALTHY=false; }

# Check for successful boot message — this is the definitive signal
if grep -q "Hytale Server Booted!" "$TEST_LOG" 2>/dev/null; then
    info "Server boot message found — server started successfully"
else
    # No boot message: scan for known fatal errors to provide useful diagnostics
    if grep -qE "Shutdown triggered|Failed to setup the following plugins|NoClassDefFoundError|ClassNotFoundException|failed to load" "$TEST_LOG" 2>/dev/null; then
        warn "Server logs indicate a fatal startup error — treating as unhealthy"
    else
        warn "Server did not emit boot success message — treating as unhealthy"
    fi
    HEALTHY=false
fi

FINAL_PHASE=$(k3s kubectl get pod "$TEST_POD_NAME" -n "$NAMESPACE" \
    -o jsonpath='{.status.phase}' 2>/dev/null || echo "Unknown")
cleanup_test_pod

if [[ "$HEALTHY" != true ]]; then
    echo ""
    echo "  ✗ TEST FAILED — Pod phase: $FINAL_PHASE, Restarts: $RESTARTS"
    echo "    No changes have been made to the production server."
    echo "    Review the logs above to diagnose the issue."
    echo ""
    exit 1
fi

info "Test pod healthy (stable for ${TEST_BOOT_WAIT}s, 0 restarts)"

# ── Step 5: Deploy to production ─────────────────────────────────────────────

log "Step 5: Deploying to production"

k3s kubectl scale deployment/"$DEPLOYMENT_NAME" -n "$NAMESPACE" --replicas=0
info "Scaled down production pod"
k3s kubectl wait --for=delete pod -l "app.kubernetes.io/name=hytale" -n "$NAMESPACE" --timeout=120s 2>/dev/null || true
sleep 3
info "Production pod terminated"

# In --build mode (version upgrade), clear server-generated data from the PVC.
# The old server wrote NPC/entity/world data in a format the new version can't read.
# Mods and configs are preserved; everything else gets wiped so the new server
# starts clean. World data was already backed up in Step 1.
if [[ "$BUILD_MODE" == true ]]; then
    log "Step 5a: Clearing old server data from PVC (version upgrade)"
    find "$PVC_PATH" -mindepth 1 -maxdepth 1 \
        ! -name "mods" ! -name "configs" \
        -exec rm -rf {} \;
    info "Old server data cleared (mods and configs preserved, world backed up in Step 1)"
fi

# Sync mods
log "Step 5b: Syncing mods to PVC"
mkdir -p "$PVC_MODS"
rm -f "$PVC_MODS"/*.jar "$PVC_MODS"/*.zip
if ls "$MODS_DIR"/*.jar &>/dev/null; then
    cp "$MODS_DIR"/*.jar "$PVC_MODS/"
    info "Deployed $(ls "$PVC_MODS"/*.jar 2>/dev/null | wc -l) mod(s)"
else
    warn "No mod jars found in $MODS_DIR"
fi
if ls "$MODS_DIR"/*.zip &>/dev/null; then
    cp "$MODS_DIR"/*.zip "$PVC_MODS/"
fi

# Sync configs
log "Step 5c: Syncing configs to PVC"
if [[ -d "$CONFIGS_DIR" ]]; then
    mkdir -p "$PVC_CONFIGS"
    cp -a "$CONFIGS_DIR"/. "$PVC_CONFIGS/"
    info "Deployed $(find "$PVC_CONFIGS" -type f | wc -l) config file(s)"
else
    warn "No configs directory found in repo"
fi

# Update deployment image
if [[ "$IMAGE_UPDATED" == true ]]; then
    log "Step 5d: Updating deployment image"
    k3s kubectl set image "deployment/$DEPLOYMENT_NAME" "$CONTAINER_NAME=$NEW_IMAGE" -n "$NAMESPACE"
    # Ensure correct pull policy: Never for local images, Always for Hub images
    if [[ "$BUILD_MODE" == true ]]; then
        k3s kubectl patch deployment "$DEPLOYMENT_NAME" -n "$NAMESPACE" \
            --type=json \
            -p='[{"op":"replace","path":"/spec/template/spec/containers/0/imagePullPolicy","value":"Never"}]'
    else
        k3s kubectl patch deployment "$DEPLOYMENT_NAME" -n "$NAMESPACE" \
            --type=json \
            -p='[{"op":"replace","path":"/spec/template/spec/containers/0/imagePullPolicy","value":"Always"}]'
    fi
    info "Deployment image updated to: $NEW_IMAGE"
fi

k3s kubectl scale deployment/"$DEPLOYMENT_NAME" -n "$NAMESPACE" --replicas=1
info "Scaled up production pod"

# ── Step 6: Production health check ──────────────────────────────────────────

log "Step 6: Waiting for production rollout"

if ! k3s kubectl rollout status "deployment/$DEPLOYMENT_NAME" -n "$NAMESPACE" --timeout="${ROLLOUT_TIMEOUT}s"; then
    echo ""
    echo "  ✗ PRODUCTION ROLLOUT FAILED"
    echo ""
    echo "── Pod status ──"
    k3s kubectl get pods -n "$NAMESPACE" -l "app.kubernetes.io/name=hytale" -o wide 2>/dev/null || true
    echo ""
    echo "── Pod logs (last 40 lines) ──"
    FAIL_POD=$(k3s kubectl get pods -n "$NAMESPACE" -l "app.kubernetes.io/name=hytale" \
        -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")
    [[ -n "$FAIL_POD" ]] && k3s kubectl logs "$FAIL_POD" -n "$NAMESPACE" --tail=40 2>/dev/null || true
    echo "────────────────────────────────────"
    echo ""
    echo "  Running automatic rollback..."
    "$KUBERNETES_DIR/rollback-hytale.sh"
    echo ""
    echo "  ✗ Rollback complete. Review logs above."
    echo ""
    exit 1
fi

sleep 10
PROD_POD=$(k3s kubectl get pods -n "$NAMESPACE" -l "app.kubernetes.io/name=hytale" \
    -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")
if [[ -n "$PROD_POD" ]]; then
    PROD_RESTARTS=$(k3s kubectl get pod "$PROD_POD" -n "$NAMESPACE" \
        -o jsonpath='{.status.containerStatuses[0].restartCount}' 2>/dev/null || echo "0")
    PROD_PHASE=$(k3s kubectl get pod "$PROD_POD" -n "$NAMESPACE" \
        -o jsonpath='{.status.phase}' 2>/dev/null || echo "Unknown")

    if [[ "$PROD_RESTARTS" -gt 0 || "$PROD_PHASE" != "Running" ]]; then
        echo ""
        echo "  ✗ PRODUCTION UNSTABLE — Phase: $PROD_PHASE, Restarts: $PROD_RESTARTS"
        echo ""
        echo "── Pod logs (last 40 lines) ──"
        k3s kubectl logs "$PROD_POD" -n "$NAMESPACE" --tail=40 2>/dev/null || echo "(no logs)"
        echo "────────────────────────────────────"
        echo ""
        echo "  Running automatic rollback..."
        "$KUBERNETES_DIR/rollback-hytale.sh"
        echo ""
        echo "  ✗ Rollback complete. Production restored to previous state."
        exit 1
    fi
fi

# ── Summary ───────────────────────────────────────────────────────────────────

log "Update complete!"

echo ""
if [[ "$IMAGE_UPDATED" == true ]]; then
    echo "  Image:    $OLD_IMAGE → $NEW_IMAGE"
else
    echo "  Image:    $OLD_IMAGE (already latest)"
fi
echo "  Mods:     $(ls "$PVC_MODS"/*.jar "$PVC_MODS"/*.zip 2>/dev/null | wc -l) file(s) deployed"
echo "  Configs:  $(find "$PVC_CONFIGS" -type f 2>/dev/null | wc -l) file(s)"
echo "  Server:   live and healthy"
echo ""

#
# Runs directly on the k3s host. Handles:
#   1. Backup current server state (mods + world) into this repo
#   2. Check for & download new Hytale game version
#   3. Build & import new Docker image if version changed
#   4. Stage mods from this repo into a test pod for validation
#   5. Deploy to production if test passes
#   6. Automatic rollback if anything fails post-backup
#
# Usage:
#   sudo /home/dad/hytale-mmorpg-mod/kubernetes/update-hytale.sh
#
# ═══════════════════════════════════════════════════════════════════════════════

set -euo pipefail

# ── Constants ─────────────────────────────────────────────────────────────────

# This repo (absolute path — derived from script location)
REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
KUBERNETES_DIR="$REPO_DIR/kubernetes"
MODS_DIR="$REPO_DIR/mods"
CONFIGS_DIR="$REPO_DIR/configs"
BACKUP_DIR="$REPO_DIR/backup"

# Hytale builder paths
BUILD_DIR="/opt/hytale-builder"
DOWNLOADER="$BUILD_DIR/hytale-downloader-linux-amd64"
CURRENT_VERSION_FILE="$BUILD_DIR/current_version.txt"

# Kubernetes
NAMESPACE="hytale"
DEPLOYMENT_NAME="hytale-server"
CONTAINER_NAME="hytale"
TEST_POD_NAME="hytale-test"

# PVC — the actual host path backing the hytale-server PersistentVolumeClaim
PVC_PATH="/var/lib/rancher/k3s/storage/pvc-2a0b4ba5-ac88-4e47-a9b9-6f8278d40263_hytale_hytale-server"
PVC_MODS="$PVC_PATH/mods"
PVC_CONFIGS="$PVC_PATH/configs"

# Staging
TEST_STAGING="/tmp/hytale-test-mods"

# Timing
TEST_BOOT_WAIT=45       # seconds to let test pod boot before checking health
ROLLOUT_TIMEOUT=120     # seconds to wait for prod rollout

# State tracking
IMAGE_UPDATED=false
MODS_UPDATED=false
OLD_VERSION="none"
NEW_VERSION=""

# ── Helpers ───────────────────────────────────────────────────────────────────

log()     { echo ""; echo "══════════════════════════════════════"; echo "  $1"; echo "══════════════════════════════════════"; }
info()    { echo "  ✓ $1"; }
warn()    { echo "  ⚠ $1"; }
fail()    { echo ""; echo "  ✗ FAILED: $1" >&2; exit 1; }

cleanup_test_pod() {
    k3s kubectl delete pod "$TEST_POD_NAME" -n "$NAMESPACE" --ignore-not-found --wait=false >/dev/null 2>&1 || true
    rm -rf "$TEST_STAGING"
}

# ── Pre-flight checks ────────────────────────────────────────────────────────

log "Pre-flight checks"

[[ $EUID -eq 0 ]] || fail "This script must be run as root (sudo)"

[[ -d "$REPO_DIR/mods" ]]      || fail "Mods directory not found: $REPO_DIR/mods"
[[ -d "$PVC_PATH" ]]           || fail "PVC path not found: $PVC_PATH"

[[ -x "$DOWNLOADER" ]] || fail "Hytale downloader not found: $DOWNLOADER\n  Install it: sudo mkdir -p $BUILD_DIR && sudo cp hytale-downloader-linux-amd64 $DOWNLOADER && sudo chmod +x $DOWNLOADER\n  See README.md → Server Setup for details."
info "Downloader: $DOWNLOADER"

# Detect container build tool
if command -v docker &>/dev/null; then
    DOCKER_CMD="docker"
elif command -v nerdctl &>/dev/null; then
    DOCKER_CMD="nerdctl"
else
    fail "Neither docker nor nerdctl found"
fi

info "Repo:       $REPO_DIR"
info "PVC:        $PVC_PATH"
info "Builder:    $DOCKER_CMD"
info "Downloader: $DOWNLOADER"

# Read current running version
if [[ -f "$CURRENT_VERSION_FILE" ]]; then
    OLD_VERSION=$(cat "$CURRENT_VERSION_FILE")
fi
info "Current version: $OLD_VERSION"

# ── Step 1: Backup current server state ──────────────────────────────────────

log "Step 1: Backing up current server state"

# Clean previous backup (overwrite each time — always keep last known good)
rm -rf "$BACKUP_DIR"
mkdir -p "$BACKUP_DIR/mods"
mkdir -p "$BACKUP_DIR/world"

# Backup mods
if [[ -d "$PVC_MODS" ]] && ls "$PVC_MODS"/*.jar &>/dev/null; then
    cp -a "$PVC_MODS"/*.jar "$BACKUP_DIR/mods/"
    info "Backed up $(ls "$BACKUP_DIR/mods/" | wc -l) mod jar(s)"
else
    warn "No mods found in PVC to back up"
fi

# Backup world data (everything in PVC except mods/)
# This captures saves, configs, and any other server-generated data
find "$PVC_PATH" -mindepth 1 -maxdepth 1 ! -name "mods" -exec cp -a {} "$BACKUP_DIR/world/" \;
info "Backed up world data from PVC"

# Save the version that was running at backup time
echo "$OLD_VERSION" > "$BACKUP_DIR/backed_up_version.txt"
info "Backup stored in: $BACKUP_DIR"

# ── Step 2: Check for new Hytale game version ────────────────────────────────

log "Step 2: Checking for Hytale updates"

cd "$BUILD_DIR"
NEW_VERSION=$("$DOWNLOADER" -print-version 2>/dev/null) || fail "Could not query latest Hytale version"

if [[ "$NEW_VERSION" == "$OLD_VERSION" ]]; then
    info "Game version is already up to date: $OLD_VERSION"
else
    info "New version available: $NEW_VERSION (current: $OLD_VERSION)"

    # ── Step 3: Download & build new image ────────────────────────────────

    log "Step 3: Downloading Hytale $NEW_VERSION"

    "$DOWNLOADER" || fail "Hytale download failed"

    ZIP_FILE="$BUILD_DIR/$NEW_VERSION.zip"
    [[ -f "$ZIP_FILE" ]] || fail "Expected zip not found: $ZIP_FILE"

    unzip -o "$ZIP_FILE" -d "$BUILD_DIR"
    info "Extracted server files"

    log "Step 3b: Building Docker image"

    # Copy Dockerfile from repo into build context
    cp "$KUBERNETES_DIR/Dockerfile" "$BUILD_DIR/Dockerfile"

    $DOCKER_CMD build -t hytale-server:latest -t "hytale-server:$NEW_VERSION" "$BUILD_DIR" \
        || fail "Docker image build failed"
    info "Built image: hytale-server:$NEW_VERSION"

    log "Step 3c: Importing image into k3s"

    $DOCKER_CMD save "hytale-server:$NEW_VERSION" | k3s ctr images import - \
        || fail "Failed to import image into k3s containerd"
    info "Image imported into k3s"

    IMAGE_UPDATED=true

    # Clean up build artifacts
    rm -f "$ZIP_FILE"
    rm -rf "$BUILD_DIR/Server"
    rm -f "$BUILD_DIR/Assets.zip"
    rm -f "$BUILD_DIR/Dockerfile"
    info "Cleaned up build artifacts"
fi

# ── Step 4: Stage mods for testing ───────────────────────────────────────────

log "Step 4: Staging mods for test pod"

cleanup_test_pod
mkdir -p "$TEST_STAGING"

# Copy all mod jars from repo
MOD_COUNT=0
if ls "$MODS_DIR"/*.jar &>/dev/null; then
    cp "$MODS_DIR"/*.jar "$TEST_STAGING/"
    MOD_COUNT=$(ls "$TEST_STAGING"/*.jar 2>/dev/null | wc -l)
fi
# Also copy .zip mod packs
if ls "$MODS_DIR"/*.zip &>/dev/null; then
    cp "$MODS_DIR"/*.zip "$TEST_STAGING/"
    ZIP_COUNT=$(ls "$TEST_STAGING"/*.zip 2>/dev/null | wc -l)
    MOD_COUNT=$((MOD_COUNT + ZIP_COUNT))
fi
info "Staged $MOD_COUNT mod(s) from $MODS_DIR"

# Determine which image tag to use for the test pod
if [[ "$IMAGE_UPDATED" == true ]]; then
    TEST_IMAGE="hytale-server:$NEW_VERSION"
else
    # Use whatever image the current deployment is running
    TEST_IMAGE=$(k3s kubectl get deployment "$DEPLOYMENT_NAME" -n "$NAMESPACE" \
        -o jsonpath='{.spec.template.spec.containers[0].image}' 2>/dev/null) \
        || fail "Could not determine current deployment image"
fi
info "Test image: $TEST_IMAGE"

# ── Step 5: Spin up test pod ─────────────────────────────────────────────────

log "Step 5: Launching test pod"

cat <<EOF | k3s kubectl apply -f -
apiVersion: v1
kind: Pod
metadata:
  name: $TEST_POD_NAME
  namespace: $NAMESPACE
  labels:
    app: hytale-test
spec:
  restartPolicy: Never
  containers:
  - name: hytale
    image: $TEST_IMAGE
    imagePullPolicy: Never
    volumeMounts:
    - name: test-mods
      mountPath: /server/mods
  volumes:
  - name: test-mods
    hostPath:
      path: $TEST_STAGING
      type: Directory
EOF

info "Test pod created, waiting for it to start..."

# Wait for the container to at least start running and stay up
HEALTHY=false
for i in $(seq 1 "$TEST_BOOT_WAIT"); do
    PHASE=$(k3s kubectl get pod "$TEST_POD_NAME" -n "$NAMESPACE" -o jsonpath='{.status.phase}' 2>/dev/null || echo "Unknown")

    case "$PHASE" in
        Running)
            # Pod is running — wait until it's been stable for at least 15s
            if [[ $i -ge 15 ]]; then
                HEALTHY=true
                break
            fi
            ;;
        Failed|Unknown)
            break
            ;;
        Succeeded)
            # Server exited cleanly — unexpected but not a crash
            warn "Test pod exited with Succeeded status (server shut down gracefully)"
            break
            ;;
    esac
    sleep 1
done

# Grab logs regardless of outcome for diagnosis
echo ""
echo "── Test pod logs (last 30 lines) ──"
k3s kubectl logs "$TEST_POD_NAME" -n "$NAMESPACE" --tail=30 2>/dev/null || echo "(no logs available)"
echo "────────────────────────────────────"

# Check container restart count (catches crash-loop behavior)
RESTARTS=$(k3s kubectl get pod "$TEST_POD_NAME" -n "$NAMESPACE" \
    -o jsonpath='{.status.containerStatuses[0].restartCount}' 2>/dev/null || echo "0")
if [[ "$RESTARTS" -gt 0 ]]; then
    warn "Test pod restarted $RESTARTS time(s) — treating as unhealthy"
    HEALTHY=false
fi

FINAL_PHASE=$(k3s kubectl get pod "$TEST_POD_NAME" -n "$NAMESPACE" \
    -o jsonpath='{.status.phase}' 2>/dev/null || echo "Unknown")

cleanup_test_pod

if [[ "$HEALTHY" != true ]]; then
    echo ""
    echo "  ✗ TEST FAILED — Pod phase: $FINAL_PHASE, Restarts: $RESTARTS"
    echo "    The test pod did not reach a stable Running state."
    echo "    No changes have been made to the production server."
    echo "    Review the logs above to diagnose the issue."
    echo ""
    exit 1
fi

info "Test pod healthy (Running for ${TEST_BOOT_WAIT}s, 0 restarts)"

# ── Step 6: Deploy to production ─────────────────────────────────────────────

log "Step 6: Deploying to production"

# Scale down to safely update PVC
k3s kubectl scale deployment/"$DEPLOYMENT_NAME" -n "$NAMESPACE" --replicas=0
info "Scaled down production pod"

# Wait for pod to terminate
k3s kubectl wait --for=delete pod -l "app.kubernetes.io/name=hytale" -n "$NAMESPACE" --timeout=120s 2>/dev/null || true
sleep 3
info "Production pod terminated"

# Update mods on PVC
log "Step 6b: Syncing mods to PVC"

mkdir -p "$PVC_MODS"
rm -f "$PVC_MODS"/*.jar "$PVC_MODS"/*.zip
info "Cleared old mods from PVC"

if ls "$MODS_DIR"/*.jar &>/dev/null; then
    cp "$MODS_DIR"/*.jar "$PVC_MODS/"
    DEPLOYED_COUNT=$(ls "$PVC_MODS"/*.jar 2>/dev/null | wc -l)
    info "Deployed $DEPLOYED_COUNT mod(s) to PVC"
else
    warn "No mod jars found in $MODS_DIR — PVC mods folder is now empty"
fi

# Also copy .zip mod packs
if ls "$MODS_DIR"/*.zip &>/dev/null; then
    cp "$MODS_DIR"/*.zip "$PVC_MODS/"
    ZIP_COUNT=$(ls "$PVC_MODS"/*.zip 2>/dev/null | wc -l)
    info "Deployed $ZIP_COUNT mod zip(s) to PVC"
fi
MODS_UPDATED=true

# Sync configs to PVC
log "Step 6c: Syncing configs to PVC"

if [[ -d "$CONFIGS_DIR" ]]; then
    mkdir -p "$PVC_CONFIGS"
    # Use rsync-like copy: mirror the configs directory structure
    cp -a "$CONFIGS_DIR"/. "$PVC_CONFIGS/"
    CONFIG_COUNT=$(find "$PVC_CONFIGS" -type f | wc -l)
    info "Deployed $CONFIG_COUNT config file(s) to PVC"
else
    warn "No configs directory found in repo"
fi

# Update deployment image if we built a new one
if [[ "$IMAGE_UPDATED" == true ]]; then
    log "Step 6d: Updating deployment image"
    k3s kubectl set image "deployment/$DEPLOYMENT_NAME" "$CONTAINER_NAME=hytale-server:$NEW_VERSION" -n "$NAMESPACE"
    info "Deployment image set to hytale-server:$NEW_VERSION"
fi

# Scale back up
k3s kubectl scale deployment/"$DEPLOYMENT_NAME" -n "$NAMESPACE" --replicas=1
info "Scaled up production pod"

# ── Step 7: Production health check ──────────────────────────────────────────

log "Step 7: Waiting for production rollout"

if k3s kubectl rollout status "deployment/$DEPLOYMENT_NAME" -n "$NAMESPACE" --timeout="${ROLLOUT_TIMEOUT}s"; then
    info "Production rollout successful"
else
    echo ""
    echo "  ✗ PRODUCTION ROLLOUT FAILED"
    echo "    The deployment did not become healthy within ${ROLLOUT_TIMEOUT}s."
    echo ""

    # Show pod status for diagnosis
    echo "── Pod status ──"
    k3s kubectl get pods -n "$NAMESPACE" -l "app.kubernetes.io/name=hytale" -o wide 2>/dev/null || true
    echo ""
    echo "── Pod logs (last 40 lines) ──"
    FAIL_POD=$(k3s kubectl get pods -n "$NAMESPACE" -l "app.kubernetes.io/name=hytale" \
        -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")
    if [[ -n "$FAIL_POD" ]]; then
        k3s kubectl logs "$FAIL_POD" -n "$NAMESPACE" --tail=40 2>/dev/null || echo "(no logs)"
    fi
    echo "────────────────────────────────────"
    echo ""
    echo "  Running automatic rollback..."
    echo ""

    "$KUBERNETES_DIR/rollback-hytale.sh"

    echo ""
    echo "  ✗ Rollback complete. Production restored to previous state."
    echo "    Review the logs above to diagnose what went wrong."
    echo ""
    exit 1
fi

# Extra stability check — wait and verify the pod isn't crash-looping
sleep 10
PROD_POD=$(k3s kubectl get pods -n "$NAMESPACE" -l "app.kubernetes.io/name=hytale" \
    -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")
if [[ -n "$PROD_POD" ]]; then
    PROD_RESTARTS=$(k3s kubectl get pod "$PROD_POD" -n "$NAMESPACE" \
        -o jsonpath='{.status.containerStatuses[0].restartCount}' 2>/dev/null || echo "0")
    PROD_PHASE=$(k3s kubectl get pod "$PROD_POD" -n "$NAMESPACE" \
        -o jsonpath='{.status.phase}' 2>/dev/null || echo "Unknown")

    if [[ "$PROD_RESTARTS" -gt 0 || "$PROD_PHASE" != "Running" ]]; then
        echo ""
        echo "  ✗ PRODUCTION UNSTABLE — Phase: $PROD_PHASE, Restarts: $PROD_RESTARTS"
        echo ""
        echo "── Pod logs (last 40 lines) ──"
        k3s kubectl logs "$PROD_POD" -n "$NAMESPACE" --tail=40 2>/dev/null || echo "(no logs)"
        echo "────────────────────────────────────"
        echo ""
        echo "  Running automatic rollback..."
        echo ""

        "$KUBERNETES_DIR/rollback-hytale.sh"

        echo ""
        echo "  ✗ Rollback complete. Production restored to previous state."
        exit 1
    fi
fi

# ── Step 8: Finalize ─────────────────────────────────────────────────────────

log "Step 8: Finalizing"

# Save new version if the game was updated
if [[ "$IMAGE_UPDATED" == true ]]; then
    echo "$NEW_VERSION" > "$CURRENT_VERSION_FILE"
    info "Saved version: $NEW_VERSION"
fi

# ── Summary ───────────────────────────────────────────────────────────────────

log "Update complete!"

echo ""
if [[ "$IMAGE_UPDATED" == true ]]; then
    echo "  Game version:  $OLD_VERSION → $NEW_VERSION"
else
    echo "  Game version:  $OLD_VERSION (unchanged)"
fi
echo "  Mods deployed: $(ls "$PVC_MODS"/*.jar "$PVC_MODS"/*.zip 2>/dev/null | wc -l) file(s)"
echo "  Configs:       $(find "$PVC_CONFIGS" -type f 2>/dev/null | wc -l) file(s)"
echo "  Backup:        $BACKUP_DIR"
echo ""
echo "  The server is live and healthy."
echo ""
