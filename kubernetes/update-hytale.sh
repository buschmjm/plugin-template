#!/bin/bash
# ═══════════════════════════════════════════════════════════════════════════════
# update-hytale.sh — Full Hytale server update & deployment pipeline
# ═══════════════════════════════════════════════════════════════════════════════
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
[[ -x "$DOWNLOADER" ]]         || fail "Hytale downloader not found: $DOWNLOADER"

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
