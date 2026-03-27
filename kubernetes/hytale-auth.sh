#!/bin/bash
# ═══════════════════════════════════════════════════════════════════════════════
# hytale-auth.sh — Authenticate the Hytale downloader
# ═══════════════════════════════════════════════════════════════════════════════
#
# Runs the official Hytale downloader's built-in OAuth2 device code flow and
# saves credentials to /opt/hytale-builder/.hytale-downloader-credentials.json
#
# Usage:
#   sudo /home/dad/hytale-mmorpg-mod/kubernetes/hytale-auth.sh
#
# ═══════════════════════════════════════════════════════════════════════════════

set -euo pipefail

BUILD_DIR="/opt/hytale-builder"
DOWNLOADER_DIR="$BUILD_DIR/hytale-downloader"
DOWNLOADER="$DOWNLOADER_DIR/hytale-downloader-linux-amd64"
CREDENTIALS_FILE="$BUILD_DIR/.hytale-downloader-credentials.json"

[[ $EUID -eq 0 ]] || { echo "Run with sudo"; exit 1; }

mkdir -p "$BUILD_DIR"

# Download the downloader if not present
if [[ ! -x "$DOWNLOADER" ]]; then
    echo "Downloading Hytale downloader..."
    mkdir -p "$DOWNLOADER_DIR"
    curl -fsSL "https://downloader.hytale.com/hytale-downloader.zip" \
        -o "$DOWNLOADER_DIR/hytale-downloader.zip"
    unzip -q "$DOWNLOADER_DIR/hytale-downloader.zip" -d "$DOWNLOADER_DIR"
    rm "$DOWNLOADER_DIR/hytale-downloader.zip"
    chmod +x "$DOWNLOADER"
fi

echo ""
echo "The Hytale downloader will now prompt you for browser authentication."
echo "Open the URL shown, enter the code, then come back here."
echo ""

# Let the downloader handle its own auth flow; it saves to CREDENTIALS_FILE
"$DOWNLOADER" \
    -credentials-path "$CREDENTIALS_FILE" \
    -print-version \
    -skip-update-check

chmod 600 "$CREDENTIALS_FILE" 2>/dev/null || true

echo ""
echo "Authentication complete. Credentials saved to: $CREDENTIALS_FILE"
echo ""
echo "You can now run:"
echo "  sudo /home/dad/hytale-mmorpg-mod/kubernetes/update-hytale.sh --build"
echo ""

#
# Uses OAuth2 Device Code Flow (RFC 8628) to authenticate your Hytale account.
# Saves credentials to /opt/hytale-builder/.hytale-downloader-credentials.json
# which is used by: update-hytale.sh --build
#
# Requirements:
#   - A Hytale account with server access
#   - curl and jq installed (sudo apt install curl jq)
#
# Usage:
#   sudo /home/dad/hytale-mmorpg-mod/kubernetes/hytale-auth.sh
#
# ═══════════════════════════════════════════════════════════════════════════════

set -euo pipefail

# ── Constants ─────────────────────────────────────────────────────────────────

CLIENT_ID="hytale-server"
OAUTH_URL="https://oauth.accounts.hytale.com"
ACCOUNTS_URL="https://account-data.hytale.com"
SESSIONS_URL="https://sessions.hytale.com"

BUILD_DIR="/opt/hytale-builder"
OUTPUT_FILE="$BUILD_DIR/.hytale-downloader-credentials.json"

# ── Helpers ───────────────────────────────────────────────────────────────────

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

fail() { echo -e "${RED}✗ $1${NC}" >&2; exit 1; }

# ── Pre-flight ────────────────────────────────────────────────────────────────

[[ $EUID -eq 0 ]] || fail "This script must be run as root (sudo)"
command -v curl &>/dev/null || fail "curl is required: sudo apt install curl"
command -v jq   &>/dev/null || fail "jq is required: sudo apt install jq"

mkdir -p "$BUILD_DIR"

echo ""
echo -e "${CYAN}╔═══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║        Hytale Server Authentication  —  Device Code Flow     ║${NC}"
echo -e "${CYAN}╚═══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# ── Step 1: Request device code ──────────────────────────────────────────────

echo -e "${GREEN}${BOLD}Step 1: Requesting device code...${NC}"

DEVICE_RESPONSE=$(curl -s -X POST "${OAUTH_URL}/oauth2/device/auth" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "client_id=${CLIENT_ID}" \
    -d "scope=openid offline auth:server")

DEVICE_CODE=$(echo "$DEVICE_RESPONSE"    | jq -r '.device_code')
USER_CODE=$(echo "$DEVICE_RESPONSE"      | jq -r '.user_code')
VERIFICATION_URI=$(echo "$DEVICE_RESPONSE" | jq -r '.verification_uri')
VERIFICATION_URI_COMPLETE=$(echo "$DEVICE_RESPONSE" | jq -r '.verification_uri_complete')
EXPIRES_IN=$(echo "$DEVICE_RESPONSE"     | jq -r '.expires_in')
INTERVAL=$(echo "$DEVICE_RESPONSE"       | jq -r '.interval')

[[ "$DEVICE_CODE" == "null" || -z "$DEVICE_CODE" ]] && {
    echo -e "${RED}Error requesting device code:${NC}"
    echo "$DEVICE_RESPONSE" | jq .
    exit 1
}

echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BOLD}        AUTHORIZATION REQUIRED${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "  1. Open this URL in your browser:"
echo ""
echo -e "     ${YELLOW}${BOLD}${VERIFICATION_URI}${NC}"
echo ""
echo -e "  2. Enter this code:"
echo ""
echo -e "     ${GREEN}${BOLD}    ${USER_CODE}    ${NC}"
echo ""
echo -e "  Or use the direct link:"
echo -e "     ${CYAN}${VERIFICATION_URI_COMPLETE}${NC}"
echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "  ${YELLOW}Code expires in ${EXPIRES_IN} seconds.${NC}"
echo ""

# ── Step 2: Poll for authorization ───────────────────────────────────────────

echo -e "${GREEN}${BOLD}Step 2: Waiting for you to authorize in browser...${NC}"
echo -e "  (Checking every ${INTERVAL} seconds)"
echo ""

ACCESS_TOKEN=""
REFRESH_TOKEN=""
TOKEN_EXPIRES_IN=""

attempts=0
max_attempts=$(( EXPIRES_IN / INTERVAL ))

while [[ $attempts -lt $max_attempts ]]; do
    TOKEN_RESPONSE=$(curl -s -X POST "${OAUTH_URL}/oauth2/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "client_id=${CLIENT_ID}" \
        -d "grant_type=urn:ietf:params:oauth:grant-type:device_code" \
        -d "device_code=${DEVICE_CODE}")

    ERROR=$(echo "$TOKEN_RESPONSE" | jq -r '.error // empty')

    if [[ -z "$ERROR" ]]; then
        ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE"  | jq -r '.access_token')
        REFRESH_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.refresh_token')
        TOKEN_EXPIRES_IN=$(echo "$TOKEN_RESPONSE" | jq -r '.expires_in')
        echo -e "\r${GREEN}✓ Authorization received!${NC}                    "
        break
    elif [[ "$ERROR" == "authorization_pending" ]]; then
        printf "\r  ⏳ Waiting... (%d/%d)" $((attempts + 1)) $max_attempts
    elif [[ "$ERROR" == "slow_down" ]]; then
        INTERVAL=$((INTERVAL + 5))
        printf "\r  ⏳ Slowing down, interval: %ds" $INTERVAL
    elif [[ "$ERROR" == "expired_token" ]]; then
        fail "Code expired. Please run the script again."
    elif [[ "$ERROR" == "access_denied" ]]; then
        fail "Access denied by user."
    else
        echo -e "\r${RED}✗ Error: $ERROR${NC}"
        echo "$TOKEN_RESPONSE" | jq .
        exit 1
    fi

    sleep "$INTERVAL"
    attempts=$((attempts + 1))
done

[[ -z "$ACCESS_TOKEN" ]] && fail "Timeout — authorization not completed in time. Please run again."

# ── Step 3: Get Hytale profiles ───────────────────────────────────────────────

echo ""
echo -e "${GREEN}${BOLD}Step 3: Retrieving Hytale profiles...${NC}"

PROFILES_RESPONSE=$(curl -s -X GET "${ACCOUNTS_URL}/my-account/get-profiles" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}")

OWNER=$(echo "$PROFILES_RESPONSE"       | jq -r '.owner')
PROFILES=$(echo "$PROFILES_RESPONSE"    | jq -r '.profiles')
PROFILE_COUNT=$(echo "$PROFILES"        | jq -r 'length')

[[ "$PROFILE_COUNT" -eq 0 ]] && fail "No Hytale profiles found on this account."

echo -e "  Owner: ${CYAN}${OWNER}${NC}"
echo -e "  Available profiles:"
echo ""
for i in $(seq 0 $((PROFILE_COUNT - 1))); do
    UUID=$(echo "$PROFILES"     | jq -r ".[$i].uuid")
    USERNAME=$(echo "$PROFILES" | jq -r ".[$i].username")
    echo -e "    [$i] ${BOLD}${USERNAME}${NC} (${UUID})"
done
echo ""

if [[ "$PROFILE_COUNT" -eq 1 ]]; then
    SELECTED_PROFILE=0
    echo -e "  ${YELLOW}Automatically selecting the only profile.${NC}"
else
    read -rp "  Select a profile [0-$((PROFILE_COUNT - 1))]: " SELECTED_PROFILE
    [[ "$SELECTED_PROFILE" =~ ^[0-9]+$ ]] && [[ "$SELECTED_PROFILE" -lt "$PROFILE_COUNT" ]] \
        || fail "Invalid selection."
fi

PROFILE_UUID=$(echo "$PROFILES"     | jq -r ".[$SELECTED_PROFILE].uuid")
PROFILE_USERNAME=$(echo "$PROFILES" | jq -r ".[$SELECTED_PROFILE].username")
echo -e "  ${GREEN}✓ Selected: ${BOLD}${PROFILE_USERNAME}${NC}"

# ── Step 4: Create game session ───────────────────────────────────────────────

echo ""
echo -e "${GREEN}${BOLD}Step 4: Creating game session...${NC}"

SESSION_RESPONSE=$(curl -s -X POST "${SESSIONS_URL}/game-session/new" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{\"uuid\": \"${PROFILE_UUID}\"}")

SESSION_TOKEN=$(echo "$SESSION_RESPONSE"    | jq -r '.sessionToken')
IDENTITY_TOKEN=$(echo "$SESSION_RESPONSE"   | jq -r '.identityToken')
SESSION_EXPIRES_AT=$(echo "$SESSION_RESPONSE" | jq -r '.expiresAt')

[[ "$SESSION_TOKEN" == "null" || -z "$SESSION_TOKEN" ]] && {
    echo -e "${RED}Error creating session:${NC}"
    echo "$SESSION_RESPONSE" | jq .
    exit 1
}

echo -e "  ${GREEN}✓ Session created!${NC}"
echo -e "  Expires: ${YELLOW}${SESSION_EXPIRES_AT}${NC}"

# ── Step 5: Save credentials ──────────────────────────────────────────────────

echo ""
echo -e "${GREEN}${BOLD}Step 5: Saving credentials to $OUTPUT_FILE${NC}"

cat > "$OUTPUT_FILE" <<EOF
{
    "owner": "${OWNER}",
    "profile": {
        "uuid": "${PROFILE_UUID}",
        "username": "${PROFILE_USERNAME}"
    },
    "oauth": {
        "access_token": "${ACCESS_TOKEN}",
        "refresh_token": "${REFRESH_TOKEN}",
        "expires_in": ${TOKEN_EXPIRES_IN}
    },
    "session": {
        "sessionToken": "${SESSION_TOKEN}",
        "identityToken": "${IDENTITY_TOKEN}",
        "expiresAt": "${SESSION_EXPIRES_AT}"
    },
    "created_at": "$(date -Iseconds)"
}
EOF

chmod 600 "$OUTPUT_FILE"
echo -e "  ${GREEN}✓ Credentials saved (permissions: 600)${NC}"

# ── Summary ───────────────────────────────────────────────────────────────────

echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}${BOLD}       ✓ AUTHENTICATION SUCCESSFUL!${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "  ${BOLD}Profile:${NC}      $PROFILE_USERNAME"
echo -e "  ${BOLD}UUID:${NC}         $PROFILE_UUID"
echo -e "  ${BOLD}Session exp:${NC}  $SESSION_EXPIRES_AT"
echo -e "  ${BOLD}Credentials:${NC}  $OUTPUT_FILE"
echo ""
echo -e "${GREEN}${BOLD}Next step — build and deploy the latest Hytale version:${NC}"
echo -e "  ${CYAN}sudo /home/dad/hytale-mmorpg-mod/kubernetes/update-hytale.sh --build${NC}"
echo ""
echo -e "${YELLOW}Note: OAuth tokens expire. If '--build' fails with a credentials error,${NC}"
echo -e "${YELLOW}re-run this script to refresh them.${NC}"
echo ""
