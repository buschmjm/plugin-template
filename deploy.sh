#!/bin/bash
# MMORPG Stats Mod — Build & Deploy Script (local testing only)
# Usage:
#   ./deploy.sh          Build + deploy JAR to Hytale client Mods folder
#
# For server deployment, use: sudo kubernetes/update-hytale.sh

set -e

# ── Config ──
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAVA_HOME="${JAVA_HOME:-/home/jbuschmann/jdk/jdk-25.0.2+10}"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

JAR_NAME="mmorpg-stats.jar"

# Detect if running inside a Flatpak sandbox (e.g. VS Code Flatpak)
# If so, wrap host filesystem commands with flatpak-spawn --host
if [ -f "/.flatpak-info" ]; then
    HOST="flatpak-spawn --host"
    echo "ℹ  Running inside Flatpak sandbox — using flatpak-spawn for host access"
else
    HOST=""
fi

# Local Hytale client (flatpak) — for single-player / local testing
CLIENT_MODS="$HOME/.var/app/com.hypixel.HytaleLauncher/data/Hytale/UserData/Mods"

# ── Functions ──

build() {
    echo "══════════════════════════════════════"
    echo "  Building MMORPG Stats Mod..."
    echo "══════════════════════════════════════"
    cd "$PROJECT_DIR"
    ./gradlew shadowJar --quiet
    echo "✓ Build successful: build/libs/$JAR_NAME"

    # Keep mods/ in sync — update-hytale.sh deploys from mods/*.jar to the server
    cp "$PROJECT_DIR/build/libs/$JAR_NAME" "$PROJECT_DIR/mods/$JAR_NAME"
    echo "✓ Synced to mods/$JAR_NAME (picked up by server deploy pipeline)"
}

stage() {
    echo ""
    echo "══════════════════════════════════════"
    echo "  Deploying to Hytale client..."
    echo "══════════════════════════════════════"
    $HOST mkdir -p "$CLIENT_MODS"
    $HOST cp "$PROJECT_DIR/build/libs/$JAR_NAME" "$CLIENT_MODS/$JAR_NAME"
    echo "✓ Copied to $CLIENT_MODS/$JAR_NAME"

    # Copy all external mod JARs from mods/ directory
    MODS_DIR="$PROJECT_DIR/mods"
    if [ -d "$MODS_DIR" ]; then
        count=0
        for jar in "$MODS_DIR"/*.jar; do
            [ -f "$jar" ] || continue
            filename="$(basename "$jar")"
            $HOST cp "$jar" "$CLIENT_MODS/$filename"
            count=$((count + 1))
        done
        echo "✓ Copied $count external mod JARs from mods/"

        # Copy zip content packs (armor, weapon, mount, creature assets)
        zipcount=0
        for zip in "$MODS_DIR"/*.zip; do
            [ -f "$zip" ] || continue
            filename="$(basename "$zip")"
            $HOST cp "$zip" "$CLIENT_MODS/$filename"
            zipcount=$((zipcount + 1))
        done
        echo "✓ Copied $zipcount content pack ZIPs from mods/"
    fi

    # Enable mod in all existing world configs
    SAVES_DIR="$HOME/.var/app/com.hypixel.HytaleLauncher/data/Hytale/UserData/Saves"
    if $HOST test -d "$SAVES_DIR"; then
        $HOST find "$SAVES_DIR" -maxdepth 2 -name "config.json" | while read -r cfg; do
            if $HOST grep -q '"com.mmorpg:MMORPGStats"' "$cfg" 2>/dev/null; then
                $HOST sed -i '/"com.mmorpg:MMORPGStats"/,/}/ s/"Enabled": false/"Enabled": true/' "$cfg"
            fi
        done
        echo "✓ Mod enabled in all world configs"
    fi

    # Ensure player has OP in all world permissions
    if $HOST test -d "$SAVES_DIR"; then
        $HOST find "$SAVES_DIR" -maxdepth 2 -name "permissions.json" | while read -r perm; do
            if $HOST grep -q '"Adventure"' "$perm" 2>/dev/null && ! $HOST grep -q '"OP"' "$perm" 2>/dev/null; then
                $HOST sed -i 's/"Adventure"/"Adventure", "OP"/' "$perm"
                echo "✓ Granted OP in $(dirname "$perm")"
            fi
        done
    fi
    echo ""
    echo "Current mods in Hytale client:"
    $HOST ls -lh "$CLIENT_MODS/"
}

# ── Main ──

build
stage
echo ""
echo "══════════════════════════════════════"
echo "  Done! JAR deployed to Hytale client."
echo "  Launch Hytale → Create World → the"
echo "  mod loads automatically."
echo ""
echo "  In-game: type /stats"
echo ""
echo "  For server deployment, run on the"
echo "  k3s host:"
echo "  sudo kubernetes/update-hytale.sh"
echo "══════════════════════════════════════"
