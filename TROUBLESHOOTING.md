# Hytale Server Troubleshooting Notes

Notes from the March 2026 server version upgrade (`2026.03.26-89796e57b`).

---

## Server Auth: "Server session token not available"

Players see `client.general.disconnect.serverAuthUnavailable` when connecting.

**Cause:** Server needs to authenticate with Hytale's backend after every fresh boot (or first time).

**Fix:**
1. Attach to the server console:
   ```bash
   sudo k3s kubectl attach -it deploy/hytale-server -n hytale
   ```
2. Type `/auth login device`
3. Visit the URL shown, enter the device code
4. Type `/auth persistence Encrypted` to survive restarts
5. Detach with **Ctrl+P, Ctrl+Q** (NOT Ctrl+C — that kills the server)

**Prerequisite:** Deployment must have `stdin: true` and `tty: true` on the container spec. If not:
```bash
sudo k3s kubectl patch deployment hytale-server -n hytale --type=json \
  -p='[{"op":"add","path":"/spec/template/spec/containers/0/stdin","value":true},{"op":"add","path":"/spec/template/spec/containers/0/tty","value":true}]'
```

---

## Incompatible Mods (version 2026.03.26)

These mods crash the server on `2026.03.26-89796e57b` with `failed to load` errors during asset validation. Removed until mod authors update them.

| Mod | Error |
|-----|-------|
| `HiddensHarvestDelights_0.0.9b-HOTFIX.zip` | `Mod HiddenIsme:Hidden's Harvest Delights failed to load` |
| `Salmakia_Kitchen_Furniture 1.1.1.zip` | `Mod Salmakia:Salmakia_Kitchen_Furniture failed to load` |
| `Salmakia_Living_Room_Furniture.zip` | `Mod Salmakia:Salmakia_Living_Furniture failed to load` |
| `Violets_Furnishings.zip` | `Mod Violet:Violet's Furnishings failed to load` |

Previously removed (older issue): `Loot4Everyone-1.3.6.jar` — `NoClassDefFoundError`

### How to bisect bad mods

1. Copy all jars to `/tmp/hytale-bisect/`
2. Add half the zips
3. Launch a test pod mounting that directory
4. Wait 90s, grep logs for `SEVERE|FAILED|failed to load`
5. If clean, add the other half; if crash, bisect that half
6. Repeat until individual bad mods identified

---

## Hytale Test NPC Validation Failures (non-fatal)

These SEVERE lines appear every boot — they come from Hytale's own test configs and are **not fatal**:

```
[NPC|P] FAIL: /Server/NPC/Roles/_Core/Tests/Test_Drop_Item.json: Throw speed 9.50 is too low...
[NPC|P] FAIL: /Server/NPC/Roles/_Core/Tests/Interaction/Test_Interaction_Shear.json: Throw speed 9.00 is too low...
```

The `update-hytale.sh --build` pipeline removes these from the Docker build context automatically.

---

## World Data Incompatibility Across Versions

Old world data from a previous Hytale version **may not be compatible** with a new server version. Symptoms:
- All blocks appear clear/invisible
- Player can't interact with anything
- Client may fail to load in on rejoin

**Fix:** If a simple server restart doesn't trigger migration, you need a fresh world. The `MigrationModule` plugin loads but may not handle all cross-version changes.

**Important:** After a restart, the old world may actually work — test before wiping.

---

## update-hytale.sh Health Check

The test pod health check looks for `Hytale Server Booted!` in logs. Non-fatal SEVERE lines (NPC validation, spawning conflicts, missing animations) do **NOT** fail the check.

Fatal patterns that indicate a real problem (only checked when boot message is absent):
- `Shutdown triggered`
- `Failed to setup the following plugins`
- `NoClassDefFoundError`
- `ClassNotFoundException`
- `failed to load`

---

## Build Mode (--build) Overview

```bash
sudo ~/hytale-mmorpg-mod/kubernetes/update-hytale.sh --build
```

1. Downloads latest game files via official Hytale downloader
2. Builds Docker image locally
3. Imports into k3s containerd
4. Tests in throwaway pod
5. Deploys to production with `imagePullPolicy: Never`

### Prerequisites
- Hytale downloader auth: `sudo ~/hytale-mmorpg-mod/kubernetes/hytale-auth.sh`
- Credentials at `/opt/hytale-builder/.hytale-downloader-credentials.json`
- Docker installed: `sudo apt install docker.io`

---

## Common Non-Fatal Log Warnings

These all appear during normal boot and can be ignored:

- `[HardwareUtil] Failed to get Hardware UUID` — no hardware UUID in containers
- `[Pets+|P] Failed to create PetsPlus dir` — read-only hostPath mounts in test pods
- `[Mounts+|P] Failed to create MountsPlus dir` — same
- `[BlockSetModule|P] Failed to find block name '...'` — mods referencing renamed vanilla blocks
- `[NPC|P] Animation X does not exist for model Y` — mods using animations not in current version
- `[TagSet|P] Tag Set '...' references '...' which is not a pattern` — mod NPC tag mismatches
- `[Spawning|P] Spawning Configuration X can't be utilised` — duplicate NPC/environment combos
- `[Config] Failed to initialize config: ... [ERROR: UNUSED LOG ARGUMENTS]` — plugin config format issues
