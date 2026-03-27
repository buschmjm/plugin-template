#!/bin/bash
# ═══════════════════════════════════════════════════════════════════════════════
# rollback-hytale.sh — Restore Hytale server to last backed-up state
# ═══════════════════════════════════════════════════════════════════════════════
#
# Restores mods + world data from backup/ in this repo and rolls the
# Kubernetes deployment back to its previous revision.
#
# Called automatically by update-hytale.sh on failure, or manually:
#   sudo /home/dad/hytale-mmorpg-mod/kubernetes/rollback-hytale.sh
#
# ═══════════════════════════════════════════════════════════════════════════════

set -euo pipefail

# ── Constants ─────────────────────────────────────────────────────────────────

REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BACKUP_DIR="$REPO_DIR/backup"

CURRENT_VERSION_FILE="/opt/hytale-builder/current_version.txt"

NAMESPACE="hytale"
DEPLOYMENT_NAME="hytale-server"
CONTAINER_NAME="hytale"

PVC_PATH="/var/lib/rancher/k3s/storage/pvc-2a0b4ba5-ac88-4e47-a9b9-6f8278d40263_hytale_hytale-server"
PVC_MODS="$PVC_PATH/mods"

ROLLOUT_TIMEOUT=120

# ── Helpers ───────────────────────────────────────────────────────────────────

log()     { echo ""; echo "══════════════════════════════════════"; echo "  $1"; echo "══════════════════════════════════════"; }
info()    { echo "  ✓ $1"; }
warn()    { echo "  ⚠ $1"; }
fail()    { echo ""; echo "  ✗ FAILED: $1" >&2; exit 1; }

# ── Pre-flight ────────────────────────────────────────────────────────────────

log "Rollback: Pre-flight checks"

[[ $EUID -eq 0 ]] || fail "This script must be run as root (sudo)"
[[ -d "$BACKUP_DIR" ]] || fail "No backup found at $BACKUP_DIR — cannot rollback"
[[ -d "$PVC_PATH" ]]   || fail "PVC path not found: $PVC_PATH"

BACKUP_VERSION="none"
if [[ -f "$BACKUP_DIR/backed_up_version.txt" ]]; then
    BACKUP_VERSION=$(cat "$BACKUP_DIR/backed_up_version.txt")
fi
info "Restoring to backed-up version: $BACKUP_VERSION"

# ── Step 1: Scale down production ─────────────────────────────────────────────

log "Step 1: Scaling down production"

k3s kubectl scale deployment/"$DEPLOYMENT_NAME" -n "$NAMESPACE" --replicas=0
k3s kubectl wait --for=delete pod -l "app.kubernetes.io/name=hytale" -n "$NAMESPACE" --timeout=120s 2>/dev/null || true
sleep 3
info "Production pod terminated"

# ── Step 2: Restore mods ─────────────────────────────────────────────────────

log "Step 2: Restoring mods from backup"

mkdir -p "$PVC_MODS"
rm -f "$PVC_MODS"/*.jar

if [[ -d "$BACKUP_DIR/mods" ]] && ls "$BACKUP_DIR/mods"/*.jar &>/dev/null; then
    cp -a "$BACKUP_DIR/mods"/*.jar "$PVC_MODS/"
    RESTORED_COUNT=$(ls "$PVC_MODS"/*.jar 2>/dev/null | wc -l)
    info "Restored $RESTORED_COUNT mod jar(s)"
else
    warn "No mod jars found in backup — mods folder is now empty"
fi

# ── Step 3: Restore world data ────────────────────────────────────────────────

log "Step 3: Restoring world data from backup"

if [[ -d "$BACKUP_DIR/world" ]] && [[ "$(ls -A "$BACKUP_DIR/world" 2>/dev/null)" ]]; then
    # Remove current world data (everything except mods/) then restore
    find "$PVC_PATH" -mindepth 1 -maxdepth 1 ! -name "mods" -exec rm -rf {} \;
    cp -a "$BACKUP_DIR/world"/. "$PVC_PATH/"
    info "World data restored"
else
    warn "No world data found in backup — skipping"
fi

# ── Step 4: Roll back Kubernetes deployment ───────────────────────────────────

log "Step 4: Rolling back deployment"

# Undo the deployment to the previous revision (restores the image tag)
k3s kubectl rollout undo deployment/"$DEPLOYMENT_NAME" -n "$NAMESPACE"
info "Deployment rolled back to previous revision"

# Restore the version tracker
if [[ "$BACKUP_VERSION" != "none" ]]; then
    echo "$BACKUP_VERSION" > "$CURRENT_VERSION_FILE"
    info "Version file restored: $BACKUP_VERSION"
fi

# ── Step 5: Scale up and verify ───────────────────────────────────────────────

log "Step 5: Starting restored server"

k3s kubectl scale deployment/"$DEPLOYMENT_NAME" -n "$NAMESPACE" --replicas=1

if k3s kubectl rollout status "deployment/$DEPLOYMENT_NAME" -n "$NAMESPACE" --timeout="${ROLLOUT_TIMEOUT}s"; then
    info "Server is up after rollback"
else
    echo ""
    echo "  ✗ WARNING: Server did not become healthy after rollback."
    echo "    Manual intervention is required."
    echo ""
    echo "── Pod status ──"
    k3s kubectl get pods -n "$NAMESPACE" -l "app.kubernetes.io/name=hytale" -o wide 2>/dev/null || true
    echo ""
    exit 1
fi

# ── Summary ───────────────────────────────────────────────────────────────────

log "Rollback complete"

echo ""
echo "  Restored version: $BACKUP_VERSION"
echo "  Mods restored:    $(ls "$PVC_MODS"/*.jar 2>/dev/null | wc -l) jar(s)"
echo "  World data:       restored from $BACKUP_DIR/world/"
echo ""
echo "  The server is live with the previous configuration."
echo ""
