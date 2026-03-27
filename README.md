# Hytale MMO Modded Server

Modded Hytale MMO server. 43 mods (1 custom-built + 42 third-party) covering RPG leveling, classes, abilities, pets, mounts, dungeons, quests, masks, cosmetics, and more.

Runs on a home k3s (lightweight Kubernetes) cluster. All server management is automated via scripts in `kubernetes/`.

---

## Repo Structure

```
├── src/                  # MMORPGStats mod source code (Java 25)
├── mods/                 # All mod JARs/ZIPs deployed to the server
├── configs/              # Server configs (Pets+, Mounts+, loot tables, masks, permissions)
├── kubernetes/           # Server management scripts & Dockerfile
│   ├── update-hytale.sh  # Full deploy pipeline (backup → test → deploy)
│   ├── rollback-hytale.sh# Restore from last backup
│   └── Dockerfile        # Hytale server Docker image
├── backup/               # Last known good server state (auto-populated)
├── deploy.sh             # Local client testing only
└── build.gradle.kts      # Gradle build config
```

## Deployment

### Local Testing (client)
```bash
./deploy.sh
```
Builds the mod and copies it to the local Hytale client Mods folder.

### Server Deployment (k3s host)
```bash
sudo /path/to/kubernetes/update-hytale.sh
```
Backs up → checks for game updates → builds image → tests in a throwaway pod → deploys to production → auto-rolls back on failure.

### Rollback
```bash
sudo /path/to/kubernetes/rollback-hytale.sh
```
Restores mods, world data, and Kubernetes deployment from last backup.

---

## Mod List

### Server Plugins

| Mod | Description | Source |
|-----|-------------|--------|
| MMORPGStats | Custom MMORPG core — stats, classes, abilities, quests, shop, guilds, claims, and combat HUD | Custom (in-house) |
| AdminUI | Server administration GUI | CurseForge |
| BetterMap | World map with cave mode and waypoints | CurseForge |
| Extended Teleporters | Removes teleporter limit, adds private/restricted/trust teleporters | CurseForge |
| EyeSpy | HUD that displays info about what you're looking at | CurseForge |
| GhostBlockRemover | Detects and removes ghost blocks from removed mods | CurseForge |
| Gravestones | Keep items safe on death with gravestones | CurseForge |
| Loot4Everyone | Individual loot chests for every player | CurseForge |
| Mounts+ | Rideable mounts with storage and eggs | CurseForge |
| Pets+ | Pet system with XP, leveling, and combat | CurseForge |
| ReviveMe | Downed and revival system for multiplayer | CurseForge |

### Cosmetics and Wardrobe

| Mod | Description | Source |
|-----|-------------|--------|
| HayHay's Animal Masks | Animal masks (crafting removed, see Mask System) | CurseForge |
| HayHay's Mob Cosmetics | Mob-themed cosmetics for the Wardrobe API | CurseForge |
| HayHay's Plague Doctor Cosmetic | Plague Doctor cosmetic set for Wardrobe API | CurseForge |
| Horns and Antlers: Wardrobe Pack | Horn and antler cosmetics for Wardrobe | CurseForge |
| MelonicsWardrobes | Wardrobe addon with anime cosmetics | CurseForge |
| Mobstar's Capes | Craftable cosmetic capes | CurseForge |
| Violet's Wardrobe | Craftable cosmetic wardrobe items | CurseForge |

### Content and Gameplay

| Mod | Description | Source |
|-----|-------------|--------|
| Aures Farm Decor | 100+ decorative farm items, fences, and a crafting station | CurseForge |
| Aures Horses | 70+ horse skin variations and appearances | CurseForge |
| Aures Livestock | Animal color variations and skins | CurseForge |
| Aures Paintings with Dragons | 50+ paintings including dragon-themed art | CurseForge |
| Aures Rare Monsters | 8+ rare monster variants with unique drops and taming | CurseForge |
| Better Gliders | Craftable and upgradeable gliders | CurseForge |
| Blook's Bandits - RPG Mobs | Bandit ambushes in forested biomes at sunset | CurseForge |
| Blook's Pirates - RPG Mobs | Pirate enemies on Zone 2 and 3 shores | CurseForge |
| Cats | Tameable cat companions with multiple breeds | CurseForge |
| Craftable Chalk Variants | Adds all chalk variants to the Builder's Workbench | CurseForge |
| Eftann's Mythic Weapons | High quality new weapons | CurseForge |
| Hidden's Harvest Delights | Food mod with 80+ recipes, new crops, and fruit trees | CurseForge |
| Kami's Magical Items | Magical weapons, staffs, wands, runes, and spellbooks | CurseForge |
| Longbow Collection | Longbows with headshot mechanics and visual effects | CurseForge |
| Major Dungeons | Custom dungeons, enemies, and items | CurseForge |
| More Crossbow Tiers | Thorium, Cobalt, Adamantite, and Mithril crossbows | CurseForge |
| Saurian's Variety Dinosaurs | 22+ dinosaur skin variants with custom sounds | CurseForge |
| Seedling Crops | Crop growing system | — |
| Shinku's Powerful Weapons | 3 legendary weapons — Spirit Calibur, Azure Vortex, Blood Moon Daggers | CurseForge |
| Traveling Mounts | Tamed mountable companions with color variations | CurseForge |
| Wan's Wonder Weapons | Collection of unique relic weapons | CurseForge |
| Yorch's Armory | Armor and weapon variations | CurseForge |
| YUNG's HyDungeons | Procedurally generated dungeon instances | CurseForge |
| Ancient Constructs | Construct Workbench, gardener/warrior summons, Ancient Titan boss | CurseForge |
| Chocobo Tales | Tameable Chocobo mounts with breeding, Gysahl Greens farming | CurseForge |
| Drakortha's Expanded Armours | Diamond and Emerald tier armor sets with crafting bench | CurseForge |
| Emprywynium | Empyreal-tier weapons — Hearsil, Halox, Serabice, Jolyn, Phoenix, Jon | CurseForge |
| Fantastic Knight Armor Pack | 6 armor sets — FrostWarden, DuneWalker, AquaKnight, LavaKnight, Sakura, GraveKnight | CurseForge |
| Floating Pets | Floating pet companions with egg-maker system | CurseForge |
| Mutant NPCs | Mutant Skeleton, Zombie, Hound, Robotic variants — night spawns | CurseForge |
| PJ Forgotten Creatures | Bramblekin, Mushee, Ghoul, Shadow Knight, Saurian Hunter, and more | CurseForge |
| The Armory | 200+ weapons, armor sets (Demon, Rook, Warden), crowns, amulets, shields | CurseForge |

### Furniture and Decoration

| Mod | Description | Source |
|-----|-------------|--------|
| HayHay's Animal Head Plushies | 135+ craftable animal head plushies | CurseForge |
| Salmakia Kitchen Furniture | Kitchen furniture, stoves, and decor | CurseForge |
| Salmakia Living Room Furniture | Sofas, fireplaces, curtains, and decor | CurseForge |
| Violet's Furnishings | Expanded furniture and decor | CurseForge |
| Violet's Music Players | Music player items | CurseForge |
| Violet's Plushies | Plushie collectibles with custom sounds | CurseForge |

---

## Server Configuration

### Core Settings

| Setting | Value | Notes |
|---------|-------|-------|
| Max Level | 100 | 5 stat points per level (500 total) |
| Max Stat Cap | 500 | Any single stat can reach 500 |
| XP Curve | 100 × 1.15^(level−2) | Exponential scaling per level |
| Kill XP | 25 × (mobHP / 100) | Clamped to 5–500 XP |
| Kill Gold | 10 × (mobHP / 100) | Clamped to 2–200 gold |

### Monster Level System

Mob level is determined by distance from world spawn:

| Distance | Level |
|----------|-------|
| 0–99 blocks | Lv 1 |
| 100–199 blocks | Lv 2 |
| Every 100 blocks | +1 level |
| 9,900+ blocks | Lv 100 |

Mobs are initialized with HP/damage scaling and a `[Lv##]` nameplate on first damage event.

### Mob Scaling per Level

| Stat | Formula | Per Level | At Lv 10 |
|------|---------|-----------|----------|
| HP | 1.0 + (level−1) × 0.08 | +8% | 1.72× |
| Damage | 1.0 + (level−1) × 0.05 | +5% | 1.45× |

### Mob Categories

| Category | Mobs | Combat Skill | Bonus |
|----------|------|-------------|-------|
| Animal | deer, sheep, rabbit, kweebec | Hunting | +0.5% per skill level |
| Beast | wolf, bear, spider, trork, goblin | Slaying | +0.5% per skill level |
| Undead | skeleton, zombie, lich, wraith | Warding | +0.5% per skill level |
| Void | void, corrupt, shadow, abyssal | Voidslaying | +0.5% per skill level |

### Level Gap XP Scaling

| Condition | Modifier |
|-----------|----------|
| Mob 1–10 levels higher | 1.0× to 1.5× (linear bonus) |
| Same level | 1.0× |
| Mob 1–3 levels lower | 1.0× (no penalty) |
| Mob 4–15 levels lower | 1.0× to 0.1× (linear penalty) |
| Mob 15+ levels lower | 0.1× (floor) |

---

## Stat System

6 stats, 5 points per level, 500 max per stat. Points are spent via `/allocate <stat>` or the stat UI (`/stats`).

### Resource Pools

| Stat | Pool | Formula | Per Point |
|------|------|---------|-----------|
| VIT | Max HP | 100 + VIT × 5 | +5 HP |
| DEX | Max Stamina | 100 + DEX × 5 | +5 Stamina |
| ARC | Max Mana | 100 + ARC × 8 | +8 Mana |
| VIT | Max Oxygen | 100 + VIT × 5 | +5 Oxygen |
| INT | Mana Regen | 0.5 + INT × 0.1 | +0.1/tick |

### Offensive Scaling

| Stat | Effect | Formula | Per Point |
|------|--------|---------|-----------|
| STR | Melee Damage | 1.0 + STR × 0.02 | +2% |
| DEX | Light/Bow Damage | 1.0 + DEX × 0.016 | +1.6% |
| INT | Spell Damage | 1.0 + INT × 0.02 | +2% |
| ARC | Healing Output | 1.0 + ARC × 0.02 | +2% |
| DEX | Crit Chance | DEX × 0.003 | +0.3% (cap 75%) |
| STR | Crit Multiplier | 1.5 + STR × 0.008 | +0.8% |

### Defensive Scaling

| Stat | Effect | Formula | Per Point |
|------|--------|---------|-----------|
| VIT | Flat Damage Resistance | VIT × 0.5 | +0.5 DR |
| PRE | Miss (Dodge) Chance | PRE × 0.001 | +0.1% (cap 50%) |
| DEX | Fall Damage Reduction | DEX × 0.002 | +0.2% (cap 100%) |
| PRE | Vendor Discount | PRE × 0.00198 | +0.198% |

### Damage Pipeline

**Attacking:** STR melee multiplier → class melee bonus → class spell bonus → buff bonus → weapon bonus → combat skill bonus → DEX crit roll (STR crit multiplier)

**Defending:** class damage negate → PRE miss chance → DEX fall DR → VIT flat resistance → class % reduction → class low-HP (<25%) bonus resistance

---

## Class System

Classes are unlocked by allocating stat points past tier thresholds. Single-stat classes require one stat; dual-stat classes require both stats to meet the threshold independently. All earned tier passives stack cumulatively. Titles persist through level-ups but are cleared on respec.

### Tier Thresholds

Single-stat classes:

| Tier | Title Pattern | Requirement |
|------|--------------|-------------|
| 1 | Apprentice {Class} | 12 points |
| 2 | Acolyte {Class} | 30 points |
| 3 | {Class} | 60 points |
| 4 | Grand {Class} | 120 points |
| 5 | {Unique Name} | 150 points |

Dual-stat classes use half threshold per stat (e.g. Tier 1 = 6 in each stat).

### Single-Stat Classes (6)

| Class | Stat | Tier 5 Title | Max Passives |
|-------|------|-------------|--------------|
| Knight | STR | Worldbreaker | +65% melee, +20% crit mult, +5% DR |
| Rogue | DEX | Phantom | +60% crit mult, +20% melee |
| Brawler | VIT | Golem | +20% max HP, +16% DR, 8% negate, +8% regen, +25% low HP resist |
| Wizard | INT | Sage | +75% spell, +8% mana, +10% mana regen |
| Protector | PRE | Sovereign | +55% miss, +28% DR, +8% shop discount |
| Druid | ARC | World-Root | +18% mana, +52% healing, +15% spell, +10% mana regen |

### Dual-Stat Classes (15)

| Class | Stats | Tier 5 Title | Max Passives |
|-------|-------|-------------|--------------|
| Duelist | STR+DEX | Bladelord | +25% melee, +46% crit mult |
| Warrior | STR+VIT | Colossus | +28% melee, +24% max HP, +15% DR |
| Warlock | STR+INT | Runic Destroyer | +43% melee, +33% spell, +8% mana |
| Champion | STR+PRE | Legend | +46% melee, +35% miss, +10% DR |
| Beastwarrior | STR+ARC | Primal Lord | +46% melee, +13% healing, +5% max HP, +8% spell |
| Pursuer | DEX+VIT | Inevitable | +32% crit mult, +10% stamina, +5% max HP, +8% regen, +8% DR |
| Trickster | DEX+INT | Mirage | +16% spell, +38% crit mult, +5% mana |
| Swashbuckler | DEX+PRE | Blade Saint | +30% crit mult, +19% melee, +25% miss |
| Ranger | DEX+ARC | Forestborn | +19% melee, +29% crit mult, +5% mana, +8% healing |
| Blightcaster | VIT+INT | Ruinmage | +15% DR, +43% spell, +10% max HP |
| Sentinel | VIT+PRE | Bastion | +45% miss, +29% max HP, +21% DR |
| Paladin | VIT+ARC | Holy Avenger | +50% healing, +13% max HP, +10% DR, +5% regen |
| Scholar | INT+PRE | Mastermind | +43% spell, +45% miss, +3% DR |
| Arcanist | INT+ARC | Primordial | +41% spell, +18% mana, +18% mana regen |
| Hexguard | PRE+ARC | Hexsovereign | +35% healing, +35% miss, +10% DR, +8% mana, +5% mana regen |

---

## Ability System

24 abilities (4 per stat), unlocked at 5 → 15 → 30 → 50 stat points. Abilities are bound to slots 1–3 via `/skill bind <slot> <id>` and cast with `/skill 1/2/3` or Q/E/R keybinds.

### STR Abilities (Knight)

| Ability | Req | Cooldown | Effect |
|---------|-----|----------|--------|
| War Cry | STR 5 | 40s | +35% damage for 12s |
| Bull Rush | STR 15 | 30s | +80 stamina for 12s |
| Iron Skin | STR 30 | 50s | +50 max HP for 20s |
| Berserker Rage | STR 50 | 45s | Sacrifice 20 HP, +50% damage for 8s |

### DEX Abilities (Rogue)

| Ability | Req | Cooldown | Effect |
|---------|-----|----------|--------|
| Swift Strikes | DEX 5 | 20s | +15% damage for 10s |
| Shadow Step | DEX 15 | 35s | +100 stamina for 12s |
| Evasion | DEX 30 | 35s | +50 stamina, +15% damage for 10s |
| Phantom Dash | DEX 50 | 55s | +120 stamina, +20% damage for 10s |

### VIT Abilities (Brawler)

| Ability | Req | Cooldown | Effect |
|---------|-----|----------|--------|
| Iron Fist | VIT 5 | 30s | +20% damage, +30 HP for 12s |
| Endurance | VIT 15 | 35s | +80 stamina for 15s |
| Second Wind | VIT 30 | 45s | Heal 5 HP/s for 10s |
| Living Bastion | VIT 50 | 60s | +100 HP, heal 3 HP/s for 12s |

### INT Abilities (Wizard)

| Ability | Req | Cooldown | Effect |
|---------|-----|----------|--------|
| Mind Spike | INT 5 | 30s | +25% damage for 10s (20 mana) |
| Arcane Step | INT 15 | 35s | +80 stamina, +40 mana for 12s (10 mana) |
| Mana Shield | INT 30 | 45s | +60 mana, +30 HP for 12s (20 mana) |
| Spell Surge | INT 50 | 55s | +45% damage, +80 mana for 10s (30 mana) |

### PRE Abilities (Commander)

| Ability | Req | Cooldown | Effect |
|---------|-----|----------|--------|
| Inspire | PRE 5 | 40s | Heal 20 HP, +20% damage for 12s |
| Rally | PRE 15 | 40s | Heal 15 HP, +80 stamina for 15s |
| Battle Standard | PRE 30 | 50s | +25% damage, +40 HP for 15s |
| Commander's Aura | PRE 50 | 60s | Heal 20 HP, +20% damage, +40 stamina for 12s |

### ARC Abilities (Druid)

| Ability | Req | Cooldown | Effect |
|---------|-----|----------|--------|
| Life Tap | ARC 5 | 25s | +40 mana for 8s |
| Nature's Sprint | ARC 15 | 30s | +80 stamina for 12s (15 mana) |
| Quick Recovery | ARC 30 | 20s | Instantly restore 30 HP (20 mana) |
| Nature's Blessing | ARC 50 | 55s | Heal 40 HP, +60 mana for 15s (25 mana) |

---

## Mask System

All 30 HayHay Animal Masks have their crafting recipes removed. They are split into 4 tiers:

### Tier 1: Prey Animals (Rare Quality)

Bought from the shop for 2,000–3,000g, also available as quest rewards. Stats: 5% resist, +8 HP, 200 dur.

Cow, Pig Pink, Pig Wild, Warthog, Ram, Mosshorn, Mosshorn 2

### Tier 2: Predator Animals (Epic Quality)

Bought from the shop for 4,000–7,500g, also available as quest rewards. Stats: 8% resist, +14 HP, 250 dur.

Fox, Cat, Black Wolf, Grey Wolf, Bear, Polar Bear, White Wolf, Feran, Sabertooth Tiger (×3)

### Tier 3: Kweebec Saplings (Epic Quality)

Rare drops from Kweebec mobs (weight 2) and dungeon chests (weight 8). Not sold in the shop.

All 7 colors (Brown, Green, Dark Brown, Yellow, Orange, Pink, Red): 12% resist, +22 HP, 400 dur, 5% dmg enhance

### Tier 4: Undead and Monsters (Legendary Quality)

Dungeon boss drops only. Not sold in the shop. Stats: 16% resist, +35 HP, 500 dur, 7% damage enhance.

| Mask | Source |
|------|--------|
| Undead Pig | Azaroth (D01) |
| Undead Pig 2 | Katherina (D02) |
| Klops | Katherina (D02) |
| Klops Orange | Baron (D03) |
| Cactee | Baron (D03) / Dark Titan |

---

## Combat Pets

10 combat pets across 5 rarity tiers. 2 are purchasable from the Pet Shop; the rest drop as eggs from mobs and dungeon bosses.

| Pet | Rarity | Source | Drop Rate |
|-----|--------|--------|-----------|
| Forest Wolf | Common | Pet Shop (1,000g) or Zone 1 wolves | ~2% drop |
| Wild Cat | Common | Zone 1 cats | ~2% |
| Desert Raptor | Uncommon | Zone 2 raptors | ~2% |
| Shore Crab | Uncommon | Zone 2 crabs | ~2% |
| Frost Bear (Polar Bear) | Rare | Pet Shop (6,000g) or Zone 3 polar bears | ~1% drop |
| Ice Pterodactyl | Rare | Zone 3 pterodactyls | ~1% |
| Cave Rex | Epic | Zone 4 T-Rex | ~1% |
| Triceratops | Epic | Zone 4 triceratops | ~1% |
| Skeleton Champion | Legendary | Dungeon bosses (Azaroth, Baron) + YUNG's chests | — |
| Ancient Guardian | Legendary | Dungeon bosses (Katherina, Baron, Dark Titan) | — |

---

## Mount System

Four Mounts+ mounts purchasable from the Mount Shop; also available via Chocobo Tales and dungeon drops:

| Mount | Rarity | Tier | Speed | HP | Storage | Respawn | Source |
|-------|--------|------|-------|----|---------|---------|--------|
| Black Wolf | Common | 1 | 1.2× | 50 | 9 slots | 60s | Mount Shop (2,000g) |
| Polar Bear | Uncommon | 2 | 1.3× | 80 | 18 slots | 120s | Mount Shop (5,000g) |
| Chocobo | Rare | 2 | 1.6× | 90 | 18 slots | 120s | Chocobo Tales (tame with capture crate) |
| Cave Rex | Rare | 3 | 1.5× | 120 | 36 slots | 180s | Mount Shop (12,000g) or D01 drop |
| Frost Dragon | Legendary | 4 | 1.8× | 200 | 54 slots | 300s | Mount Shop (25,000g) or D03 drop |

The 13 Traveling Mounts (craftable at the Farmingbench) are also available as lower-stat alternatives.

---

## Dungeon Instance IDs

| Instance ID | Mod | Notes |
|-------------|-----|-------|
| Yungs_HyDungeons_Skeleton_Dungeon | YUNG's HyDungeons | ExactIdMatch: true |
| MJ_Instance_D01 | Major Dungeons | ExactIdMatch: false (game appends UUIDs) |
| MJ_Instance_D02 | Major Dungeons | ExactIdMatch: false |
| MJ_Instance_D03 | Major Dungeons | ExactIdMatch: false |

### Dungeon Instance Levels

| Dungeon | Level Range |
|---------|-------------|
| YUNG's Skeleton Dungeon | 50–65 |
| Major Dungeons D01 | 55–70 |
| Major Dungeons D02 | 70–85 |
| Major Dungeons D03 | 85–100 |

---

## Dungeon Loot Tables

### Boss Drops

| Boss | Dungeon | Level | Weapons (weight) | Pet Eggs (weight) | Masks (weight) |
|------|---------|-------|-------------------|-------------------|----------------|
| Azaroth | D01 | 55–70 | Frozen Runic Blade (12), Soulblight Longsword (12) | Skeleton Champion (8) | Undead Pig (10) |
| Katherina | D02 | 70–85 | Sakura No Tsurugi (12), Void Requiem Scythe (12), Void Staff (10) | Ancient Guardian (10) | Klops (10), Undead Pig 2 (10) |
| Baron | D03 | 85–100 | God Slayer Axe (10), Mystical Spellblade (10), Mjollnir Mace (10), Gaia's Wrath Sword (10), Void Spellbook (8) | Both Legendary (8 each) | Klops Orange (8), Cactee (8) |
| Dark Titan | D03 mini | 85–100 | Sentinel's Will (12) | Ancient Guardian (12) | Cactee (10) |

### Cult Knight (Mini Boss)

- Chrono Blade (weight 15)
- Random T3 Kweebec Mask (weight 12, split across 7 colors)

### YUNG's Skeleton Dungeon Chests (Level 50–65)

- Mid-tier weapons: Frostburn Dagger, Heartroot Dagger, Helioram Mace, Fire Staff (weight 15 total)
- T3 Kweebec Mask (weight 8)
- Skeleton Champion Egg (weight 3)

---

## Class Intro Quests

8 class quests unlock after completing the tutorial. Each gives a starter weapon:

| Quest | Weapon Type | Kill Requirement | Reward |
|-------|-------------|-----------------|--------|
| Path of the Warrior | Sword | 10 Trorks, 5 Spiders | Iron Sword variant |
| Path of the Berserker | Battleaxe | 15 Trorks | Iron Battleaxe |
| Path of the Ranger | Crossbow | 8 Wolves, 8 Spiders | Iron Crossbow |
| Path of the Mage | Wand | 10 Skeletons | Kami's Fire Wand |
| Path of the Spearman | Spear | 10 Trorks, 5 Wolves | Iron Spear |
| Path of the Rogue | Dagger | 12 Trorks, 5 Skeletons | Wan's Ashthorn Dagger |
| Path of the Crusader | Mace | 15 Skeletons | Wan's Helioram Mace |
| Path of the Samurai | Sword | 10 Trorks, 8 Wolves, 5 Spiders | Cobalt Sword |

---

## Shop and Economy

Handled by the MMORPGStats mod. No external config — all data persisted via ECS components.

### Mount Shop

Opened from `/menu` → Mounts. Grants the mount directly to the player's collection (no egg required). Uses Mounts+ API via reflection.

| Mount | Type Key | Price |
|-------|----------|-------|
| Black Wolf | `Wolf_Black` | 2,000g |
| Polar Bear | `Bear_Polar` | 5,000g |
| Cave Rex | `Rex_Cave` | 12,000g |
| Frost Dragon | `Dragon_Frost` | 25,000g |

Command hint shown in UI: `/mounts` to manage, `/mounts storage` for mount inventory.

### Pet Shop

Opened from `/menu` → Pets. Grants the pet directly to the player's collection (no egg required). Uses Pets+ API via reflection.

| Pet | Type Key | Price |
|-----|----------|-------|
| Forest Wolf | `Wolf` | 1,000g |
| Polar Bear | `Bear_Polar` | 6,000g |

Command hint shown in UI: `/pets` to manage and summon.

### Weapon Shop Categories (8 tabs, up to 11 items per tab)

| Category | Items | Price Range |
|----------|-------|-------------|
| Swords (11) | Stormbreaker, Cobalt Warblade, Frostmourne, Frozen Runic Blade, Heavenly Grace, Chrono Blade, Emerald Crystal Blade, Sakura No Tsurugi, Mystical Spellblade, Spirit Calibur, Anduril | 750–5,000g |
| Axes (7) | Berserker's Cleaver, Dual Cobalt War Axes, Death's Harvest, Fire Steel Mace, Sentinel's Will, Dual Adamantite War Axes, Demon Lord Great Axe | 1,000–2,800g |
| Daggers (3) | Shadowfang Dagger, Cyber Daggers, Blood Moon Daggers | 600–3,500g |
| Spears (1) | Dragon Piercer | 1,100g |
| Staves (1) | Archmage's Scepter | 2,500g |
| Bows (6) | Crude Longbow, Copper Longbow, Iron Longbow, Lumina Whisper, Mist Weaver, Azure Vortex | 300–3,500g |
| Crossbows (4) | Thorium, Cobalt, Adamantite, Mithril | 600–2,000g |
| Elemental (6) | Magic Ice Sword, Flame Saber, Thunder Sword, Serpent Sword, Void Sword, Purple Void Sword | 2,500–3,200g |

### Premium Weapons (1 tab, 11 items)

Higher-tier weapons from Emprywynium and The Armory with superior stats and prices.

| Weapon | Source | Price |
|--------|--------|-------|
| Hearsil Sword | Emprywynium | 6,000g |
| Halox Sword | Emprywynium | 6,000g |
| Serabice Sword | Emprywynium | 6,500g |
| Jolyn Battleaxe | Emprywynium | 6,500g |
| Phoenix Daggers | Emprywynium | 5,500g |
| Jon's Mace | Emprywynium | 6,000g |
| Dual Rune Blade | The Armory | 5,000g |
| Ghost Sword | The Armory | 4,500g |
| Zweihander | The Armory | 5,500g |
| Lahat Chereb | The Armory | 5,500g |
| Fire Whip | The Armory | 4,500g |

### Armor Shop (4 tabs: Helmets, Chest, Gloves, Legs)

Individual armor pieces from Fantastic Knight, Drakortha's Expanded Armours, and The Armory.

| Set | Source | Tier | Resist | HP | Price/pc |
|-----|--------|------|--------|----|----------|
| FrostWarden | Fantastic Knight | Cobalt (Rare) | 11.5% | +22 | 400–600g |
| DuneWalker | Fantastic Knight | Thorium (Rare) | 11.5% | +22 | 450–650g |
| AquaKnight | Fantastic Knight | Rare | 11.5% | +22 | 400–600g |
| LavaKnight | Fantastic Knight | Adamantite | 13.5% | +24 | 500–700g |
| Sakura | Fantastic Knight | Thorium (Light) | 11.5% | +22 | 450–650g |
| GraveKnight | Fantastic Knight | Rare | 10.0% | +18 | 450g (head only) |
| Rook | The Armory | Iron-tier | 12.0% | +20 | 700–900g |
| Warden | The Armory | Iron-tier | 13.0% | +22 | 750–950g |
| Demon | The Armory | Adamantite (Epic) | 14.4% | +26 | 850–1,200g |
| Diamond | Drakortha's | Legendary | 22.0% | +28 | 1,200–1,600g |
| Emerald | Drakortha's | Legendary | 22.0% | +28 | 1,200–1,600g |
| Supplies | Potions, food, blocks (20+ types), ores, repair kits, services | 15–750g |
| Offhand | 6 class offhand gems | 25g |
| Prey Masks (T1) | 7 masks | 2,000–3,000g |
| Predator Masks (T2) | 11 masks | 4,000–7,500g |

### Weapon Sources

| Source | Crafting | Shop | Drop |
|--------|----------|------|------|
| Wan's Wonder Weapons | ✅ Craftable (slightly nerfed) | ❌ | ❌ |
| Kami's Magical Items | ❌ | ❌ | ✅ Loot table drops only |
| Eftann's Mythic Weapons | ❌ | ✅ | ❌ |
| Longbow Collection | ❌ | ✅ | ❌ |
| More Crossbow Tiers | ❌ | ✅ | ❌ |
| Shinku's Powerful Weapons | ❌ | ✅ | ✅ Also dungeon drops |
| Yorch's Armory (elemental) | ❌ | ✅ | ❌ |
| Emprywynium (premium) | ✅ High-tier crafting | ✅ Premium tab | ❌ |
| The Armory (weapons) | ✅ Various benches | ✅ Premium tab | ❌ |
| The Armory (armor) | ✅ Armor bench | ✅ Armor tabs | ❌ |
| Drakortha's Expanded Armours | ✅ Expanded Armor Bench | ✅ Armor tabs | ❌ |
| Fantastic Knight Armor | ✅ Various benches | ✅ Armor tabs | ❌ |

### Economy

- Currency is gold, managed by MMORPGStats
- Daily login reward grants bonus gold
- Players can trade gold with `/trade <player> <amount>`
- Vendor discounts scale with PRE stat + class passives
- Stat respec available via `/shop reset` (costs gold per allocated point)

---

## Modded Weapon Item IDs

### Kami's Magical Items (`MI_Weapon_` prefix)

| ID | Type |
|----|------|
| MI_Weapon_Wand_Fire / Ice / Nature / Void | Wand |
| MI_Weapon_Staff_Fire / Ice / Nature / Void | Staff |
| MI_Weapon_Spellbook_Fire / Ice / Nature / Void / Summon / Rat_Summon | Spellbook |
| MI_Weapon_Rune_Fire / Ice / Nature / Void / Shield / Movement / Magelight | Rune |
| MI_Weapon_Scroll_Horse / Emberwulf / Rat / Wisp / Undead_Horse / Undead_Knight / Undead_Ranger / Undead_Hound | Scroll |
| MI_Weapon_Sword_Runic | Sword |

### Wan's Wonder Weapons (`WanMine_` prefix)

| ID | Type |
|----|------|
| WanMine_Ashthorn_Dagger / Frostburn_Dagger / Heartroot_Dagger / Nightshade_Dagger / Chromatic_Cleaver_Dagger | Dagger |
| Wanmine_Weapon_DayNight_Dagger | Dagger (Eclipsebound) |
| WanMine_Lethal_Leftovers_Sword / Gaias_Wrath_Sword / Quasar_Cosmic_Sword | Sword |
| WanMine_Soulblight_Longsword | Longsword |
| WanMine_Maelstrom_Mace / Helioram_Mace / Mjollnir_Mace | Mace |
| WanMine_God_Slayer_Battleaxe | Battleaxe |
| WanMine_Void_Requiem_Scythe | Scythe |

### Eftann's Mythic Weapons

| ID | Type |
|----|------|
| Demon_Lord_Great_Axe / Sentinels_Will | Battleaxe |
| Cyber_Daggers | Dagger |
| Sakura_No_Tsurugi | Katana |
| Mystical_Spellblade | Magical Sword |
| Chrono_Blade / Frozen_Runic_Blade | Sword |

### Longbow Collection

| ID | Type |
|----|------|
| Crude_Longbow / Copper_Longbow / Iron_Longbow | Longbow |
| Lumina_Whisper / Mist_Weaver | Longbow (Vestige) |

### More Crossbow Tiers

| ID | Type |
|----|------|
| Weapon_Crossbow_Thorium / Cobalt / Adamantite / Mithril | Crossbow |

### Shinku's Powerful Weapons

| ID | Type |
|----|------|
| Sword_Spirit_Calibur | Sword (Legendary) |
| Shortbow_Azure_Vortex | Shortbow (Legendary) |
| Daggers_Blood_Moon | Daggers (Legendary) |

### Yorch's Armory (shop elemental weapons)

| ID | Type |
|----|------|
| IceSword / BetterFlameSword / ThunderSword / SerpentSword | Elemental Sword (Legendary) |
| VoidShortSword / PurpleVoidSword | Void Sword (Legendary) |
| DualCobaltWarAxe / DualAdamantiteWarAxe | Dual War Axe |
| FireSteelMace | Mace |

### Emprywynium (`Weapon/` prefix)

| ID | Type |
|----|------|
| Weapon/Sword/Hearsil_Sword | Empyreal Sword |
| Weapon/Sword/Halox_Sword | Empyreal Sword |
| Weapon/Sword/Serabice_Sword | Empyreal Sword |
| Weapon/Daggers/Phoenix_Daggers | Empyreal Daggers |
| Weapon/Mace/Jon_Mace | Empyreal Mace |
| Jolyn_Battleaxe | Empyreal Battleaxe/Scythe |

### The Armory (premium weapons)

| ID | Type |
|----|------|
| DualRuneBlade | Epic Dual Daggers |
| GhostSword | Ghost Sword |
| Zweihander | Greatsword |
| LahatChereb | Legendary Flaming Sword |
| FireWhip | Fire Whip |
| Relentless | Sword |
| ScorpianFlail | Flail |
| Hepta_Axe | Axe |
| Fist | Fist Weapon |

### The Armory (armor sets)

| Set | Slots | Game Item IDs |
|-----|-------|---------------|
| Demon | Head, Chest, Hands, Legs | DemonHelm, DemonChestplate, DemonGauntlets, DemonLeggings |
| Rook (Rusty) | Head, Chest, Hands, Legs | RookRustyHelm, RookChestRusty, RookGauntletsRusty, RookBootsRusty |
| Warden | Head, Chest, Hands, Legs | WardenHelm, WardenChest, WardenSleeves, WardenLeggings |
| Priest | Head, Chest, Hands, Legs | PriestHelm, PriestChest, PriestSleeves, PriestRobes |

### Drakortha's Expanded Armours

| Set | Slots | Game Item IDs |
|-----|-------|---------------|
| Diamond | Head, Chest, Hands, Legs | Armor_Diamond_Head, Armor_Diamond_Chest, Armor_Diamond_Hands, Armor_Diamond_Legs |
| Emerald | Head, Chest, Hands, Legs | Armor_Emerald_Head, Armor_Emerald_Chest, Armor_Emerald_Hands, Armor_Emerald_Legs |

### Fantastic Knight Armor Pack

| Set | Slots | Game Item IDs |
|-----|-------|---------------|
| FrostWarden | Head, Chest, Hands, Legs | Armor_FrostWarden_Head/Chest/Hands/Legs |
| DuneWalker | Head, Chest, Hands, Legs | Armor_DuneWalker_Head/Chest/Hands/Legs |
| AquaKnight | Head, Chest, Hands, Legs | Armor_AquaKnight_Head/Chest/Hands/Legs |
| LavaKnight | Head, Chest, Hands, Legs | Armor_LavaKnight_Head/Chest/Hands/Legs |
| Sakura | Head, Chest, Hands, Legs | Armor_Sakura_Head/Chest/Hands/Legs |
| GraveKnight | Head, Chest | Armor_GraveKnight_Head/Chest |

---

## Aures Rare Monsters

Rare monster variants with enhanced stats and unique drops. These spawn naturally in specific biomes.

| Monster | Biome | Difficulty | Notable Drops |
|---------|-------|-----------|---------------|
| Strange Rabbit | Plains, Forests | ☆ | Hide, Void Essence, Wildmeat |
| Golden Antelope | Savannas, Deserts | ☆ | Fire Essence, Gold, Gold Horns Ground |
| Monster Bat | Caves | ☆ | Light Hide, Poop |
| Silver Horn | Azure/Northern Forests | ☆☆ | Ice Essence, Silver Horns Ground (helmet material) |
| Young Snow Werewolf | Northern Lands | ☆☆ | Ice Essence, Wildmeat |
| Rusted Machine | Abandoned Mines | ☆☆ | Copper/Iron Bars, Ruby, Rusted Drill |
| Cursed Bear | Forests, Swamps | ☆☆☆ | Heavy Hide, Void Essence, Empire Bearscuit |
| Snow Werewolf | Northern Lands | ☆☆☆ | Ice Essence, Cindercloth, Throat of Werewolf (taming) |
| Egg Kidnapper | Scarack Areas | — | Trader NPC (not combat) |
| Old Mosshorn | Swamps | ☆ | Heavy Hide, Seeds |

Rare monsters are configured via loot tables in `configs/loot_tables/` for enhanced drops.

---

## Creature Spawn System

All added creature spawns from PJ Forgotten Creatures, Mutant NPCs, and Ancient Constructs are controlled through a master spawn config. Default mod spawn files are overridden to prevent uncontrolled spawning.

### Master Spawn Config (`configs/spawns/master_spawns.json`)

| Zone | Creatures | Time | Notes |
|------|-----------|------|-------|
| Zone 1 Forests | Bramblekin Shaman, Mushee | Day | Rare tribal + peaceful mushroom |
| Zone 1 Swamps | Ghoul, Zombie Aberrant | Night | Undead pack spawns |
| Zone 3 Glacial | Shadow Knight | Night | Elite undead, very rare |
| Zone 4 Jungles | Saurian Hunter, Slothian Warrior, Grooble, Mannequin | Day | Mixed intelligent + mythic |
| Zone 4 Jungles | Construct Ancient Titan | Day | Boss-level, very rare |
| Multi-zone Void | Necromancer Void | Night | Void caster, all zones |
| Multi-zone Night | Mutant Skeleton, Zombie, Hound, Robotic variants | Night | Moon-phase scaled, despawn at dawn |

Override files are deployed to `Server/NPC/Spawn/World/` and replace the mod defaults.

---

## Tinkering Skill

A new profession skill tied to the Ancient Constructs mod's Construct Workbench.

- **Material Save Chance**: Higher Tinkering level increases chance to not consume crafting materials at Construct benches
- **Faster Craft Speed**: Reduced crafting time at Construct Workbenches
- XP is earned by crafting items at the Construct Workbench
- Works the same as other crafting professions (Carpentry, Smithing, etc.)

---

## Configs

Server configs are stored in `configs/` and deployed to the PVC by `update-hytale.sh`.

| Config | Path | Description |
|--------|------|-------------|
| Pets+ | `configs/Hyronix_PetsPlus/config.json` | Pet combat, XP, leveling, rarity scaling |
| Mounts+ | `configs/MountsPlus/config.json` | Mount entities, speeds, storage, health |
| Loot Tables | `configs/loot_tables/Drops/NPCs/` | Per-mob drop tables (bosses, zone mobs) |
| Loot Tables | `configs/loot_tables/Drops/Prefabs/` | Dungeon chest loot tables |
| Mask Overrides | `configs/mask_overrides/Server/Item/Items/` | 30 mask stat overrides (HayHay masks) |
| Permissions | `configs/permissions.json` | User groups and permission nodes |
| Spawn Overrides | `configs/spawns/master_spawns.json` | Master creature spawn config |
| Spawn Overrides | `configs/spawns/Spawns_Zone*_*.json` | Individual zone override files |
| Spawn Overrides | `configs/spawns/Mutant_Night_Spawns.json` | Mutant NPC night spawn override |

---

## Blook's Bandits and Pirates

### Bandits (Zone 1)

- Spawn in Forests and Swamps at night (19:00–21:00)
- Assassin (55%), Brute (5%), Rabbit decoy (40%)
- Drop Fabric Scraps (100%) and Bandit Mask (22%)

### Pirates (Zone 2 and 3)

- Human pirates on Zone 2 shores during the day (7:00–18:00), includes Sailors, Gunners, and a Captain
- Skeleton pirates on Zone 3 shores, spawn all day
- Drop Boom Powder (100%), Fabric (100%), Cutlass (10%)

---

## Mod Commands

Only mods that add commands are listed here.

### MMORPGStats

| Command | Description | Permission |
|---------|-------------|------------|
| `/menu` | Open the main MMORPG menu hub | None |
| `/stats` | Open the stat allocation UI | None |
| `/allocate <stat> [amount]` | Spend stat points (str/dex/vit/int/pre/arc) | None |
| `/allocate info` | Show stat point summary and distribution | None |
| `/class list` | List all earned class titles | None |
| `/class info <class>` | Show tier details, thresholds, and passives | None |
| `/class select <title>` | Select a title to display | None |
| `/class auto <on/off>` | Toggle auto-update for displayed title | None |
| `/skill list` | List all skills with unlock status, costs, cooldowns | None |
| `/skill use <skill>` | Cast a skill by ID | None |
| `/skill bind <1-3> <skill>` | Bind a skill to quick-cast slot | None |
| `/skill binds` | Show current skill slot bindings | None |
| `/skill 1 / 2 / 3` | Quick-cast bound skill | None |
| `/profession` | Show all profession levels and XP | None |
| `/profession info <skill>` | Show detailed profession info | None |
| `/quest` | View weekly quests with progress and reset timer | None |
| `/gold` | Check gold balance | None |
| `/goldlog` | View last 15 gold transactions | None |
| `/trade <player> <amount>` | Send a gold trade offer (60s timeout) | None |
| `/trade accept / decline` | Accept or decline a pending trade | None |
| `/shop` | Browse weapons for sale | None |
| `/shop buy <number>` | Buy a weapon by list number | None |
| `/shop equip <number>` | Equip an owned weapon | None |
| `/shop unequip` | Unequip current weapon | None |
| `/shop inventory` | Show equipped weapon | None |
| `/shop supplies` | Browse potions, food, blocks, ores, services | None |
| `/shop purchase <number>` | Buy a supply item or service | None |
| `/shop offhand` | Browse class offhand items | None |
| `/shop offhand buy <number>` | Purchase a class offhand item | None |
| `/shop reset` | Reset stat points (costs gold per point spent) | None |
| `/spawnshop` | Place shopkeeper NPC at current location | None |
| `/lb` | Show level leaderboard (default) | None |
| `/lb level / gold / kills` | Top 10 by level, gold, or kills | None |
| `/claim` | Claim the chunk you're standing in | None |
| `/claim unclaim` | Unclaim the current chunk | None |
| `/claim info` | Show who owns the current chunk | None |
| `/claim list` | List all your claimed chunks with count/limit | None |
| `/claim trust <player>` | Trust a player on all your claims | None |
| `/claim untrust <player>` | Remove a player's trust | None |
| `/claim setting <flag>` | Toggle a protection flag (break/place/doors/containers/pvp/mobdamage) | None |
| `/claim settings` | View all current protection settings | None |
| `/claim color <color>` | Set your claim map overlay color | None |
| `/guild` | View guild info (level, XP, rank, donations, perks) | None |
| `/guild create <name>` | Create a guild (costs 500 gold) | None |
| `/guild invite <player>` | Invite a player to your guild | Officer+ |
| `/guild join <name>` | Join a guild | None |
| `/guild leave` | Leave your guild | None |
| `/guild donate <amount>` | Donate gold to level up the guild | None |
| `/guild promote <player>` | Promote a member to Officer | Leader |
| `/guild demote <player>` | Demote an Officer to Member | Leader |
| `/guild kick <player>` | Kick a member from the guild | Officer+ |
| `/guild disband` | Disband the guild | Leader |
| `/guild info <name>` | View info about any guild | None |
| `/online` | Show online players with level, class, guild, playtime | None |
| `/rpghelp` | Help overview with all topics | None |
| `/rpghelp commands` | List all available commands | None |
| `/rpghelp guide` | Overview of all game systems | None |
| `/rpgadmin setlevel <level>` | Set your MMORPG level | Admin |
| `/rpgadmin setstat <stat> <value>` | Set a stat to an exact value | Admin |
| `/rpgadmin reset` | Reset all stats to zero | Admin |
| `/rpgadmin respec` | Refund all spent stat points | Admin |
| `/rpgadmin tp <player>` | Teleport to a player | Admin |
| `/rpgadmin tphere <player>` | Teleport a player to you | Admin |
| `/rpgadmin setgold <amount>` | Set gold balance | Admin |
| `/rpgadmin addgold <amount>` | Add gold to balance | Admin |

### AdminUI

| Command | Description | Permission |
|---------|-------------|------------|
| `/admin` | Open the main AdminUI interface | AdminUI.ui.open |
| `/admin wl` | Whitelist management | AdminUI.whitelist.open |
| `/admin m` | Mute management | AdminUI.mute.open |
| `/admin b` | Ban management | AdminUI.ban.open |
| `/admin p` | Player management | AdminUI.player.open |
| `/admin w` | Warps management | AdminUI.warp.open |
| `/admin st` | Live server stats | AdminUI.stats.open |
| `/admin bk` | Backup management | AdminUI.backup.open |

### BetterMap

| Command | Description | Permission |
|---------|-------------|------------|
| `/bettermap` or `/bm` | Open the BetterMap config UI | None |
| `/bm waypoint menu` or `/wp` | Open waypoint manager | None |
| `/bm min / max <value>` | Set personal zoom scale | None |
| `/bm config radar <range>` | Set radar range (-1 for infinite) | bettermap.command.config |
| `/bm config location` | Toggle location HUD | bettermap.command.config |
| `/bm config hideplayers` | Hide player cursors on map | bettermap.command.config |
| `/bm config waypointteleport` | Toggle waypoint teleportation | bettermap.command.config |
| `/bm config shareallexploration` | Toggle shared map mode | bettermap.command.config |
| `/bm config worldborder` | Toggle world border visualization | bettermap.command.config |
| `/bm reload` | Reload configuration file | bettermap.command.reload |

### GhostBlockRemover

| Command | Description |
|---------|-------------|
| `/ghost scan` | Scan loaded chunks in a 5-chunk radius |
| `/ghost list` | List found ghost blocks |
| `/ghost remove <block_id>` | Remove all instances of a specific block ID |
| `/ghost remove all` | Remove ALL detected ghost blocks |
| `/ghost autocheck <true\|false>` | Toggle automatic scanning of new chunks |

### Gravestones

| Command | Description |
|---------|-------------|
| `/gravestone` | Show all settings and commands |
| `/gsmodel` | Toggle custom or vanilla gravestone style |
| `/gslimit <count>` | Set max gravestones per player |
| `/gsprotection` | Toggle owner-only gravestone access |
| `/gstimer <min>` | Set auto-break timer (0 = off) |

### Loot4Everyone

| Command | Description |
|---------|-------------|
| `/lootconfig` | Open the config UI |
| `/generatelc` | Generate a loot chest (look at container) |
| `/editlc` | Edit loot chest droplist |
| `/deletelc` | Delete a loot chest |
| `/resetlc` | Reset loot chest for all or specific players |
| `/setautoresetlc` | Configure automatic loot chest reset interval |

### Mounts+

| Command | Description | Permission |
|---------|-------------|------------|
| `/mounts` | Open mount management UI | None |
| `/mounts redeem` | Redeem a mount egg (hold egg in hand) | None |
| `/mounts storage` | Open storage of your spawned mount | Owner only |
| `/mounts transfer <player> <id>` | Transfer a mount to another player | Owner only |
| `/mounts add <player> <type>` | Give a mount to a player | mounts.admin |
| `/mounts admin` | Open the admin mount management UI | mounts.admin |

### Pets+

| Command | Description | Permission |
|---------|-------------|------------|
| `/pets` | Open pet management UI | None |
| `/pets transfer <player> <id>` | Transfer a pet to another player | Owner only |
| `/pets add <player> <type>` | Give a pet to a player | pets.admin |
| `/pets admin` | Open admin pet management UI | pets.admin |

### ReviveMe

| Command | Description | Permission |
|---------|-------------|------------|
| `/reviveme down <player>` | Force a player into the downed state | reviveme.command.admin |
| `/reviveme revive <player>` | Instantly revive a downed player | reviveme.command.admin |

### YUNG's HyDungeons

| Command | Description | Permission |
|---------|-------------|------------|
| `/inst` | Teleport to a dungeon instance | OP |
| `/inst exit` | Exit the current instance | OP |

---

## Permissions

Permissions are managed through Hytale's built-in `permissions.json`. No third-party permissions mod is needed. The OP group grants `*` (all permissions). MMORPGStats commands do not use Hytale permissions — player commands are available to everyone, and admin commands use a hardcoded UUID whitelist.

---

## Architecture

### ECS Components (persisted)
- `PlayerStatData` — 6 stats, level, XP, kill count
- `PlayerClassData` — earned titles, equipped title, passive bonuses
- `PlayerSkillData` — profession levels, XP, exploration tracking
- `PlayerQuestData` — weekly quest progress
- `PlayerGoldData` — gold balance
- `PlayerEquipmentData` — equipped weapon
- `ClaimData` — claimed chunks, trusted players, protection settings
- `GuildData` — guild name, rank, donations, quests completed

### Systems
- `DamageHandler` — stat-based damage/crit calculation, mob level scaling
- `KillXpHandler` — XP/gold from mob kills with level scaling, guild XP bonus
- `DeathPenaltyHandler` — XP loss on death
- `BuffTickSystem` — per-tick buff processing & exploration tracking
- `GatheringSkillHandler` — Mining/Woodcutting/Farming XP
- `CraftingSkillHandler` — Crafting/Processing XP
- `ClaimProtectionSystems` — Block break/place/damage protection on claimed land

### Global Registries (in-memory, rebuilt on join)
- `ClaimRegistry` — chunk → owner UUID mapping
- `GuildRegistry` — guild state, XP, levels, member tracking

### Key Configuration
All stat multipliers, thresholds, and XP curves are in `StatConstants.java`.

## Server Setup

One-time setup required on the k3s host before `update-hytale.sh` can run:

### 1. Create build directory

```bash
sudo mkdir -p /opt/hytale-builder
```

### 2. Install the Hytale downloader binary

The downloader is a Hytale-provided tool that checks for and downloads new server versions.
Obtain `hytale-downloader-linux-amd64` and install it:

```bash
sudo cp hytale-downloader-linux-amd64 /opt/hytale-builder/hytale-downloader-linux-amd64
sudo chmod +x /opt/hytale-builder/hytale-downloader-linux-amd64
```

Verify it works:

```bash
/opt/hytale-builder/hytale-downloader-linux-amd64 -print-version
```

### 3. Run the update script

```bash
sudo /home/dad/hytale-mmorpg-mod/kubernetes/update-hytale.sh
```

The script will fail immediately with a clear message if the downloader is not installed.

---

## Building

Requires Java 25.

```bash
./gradlew shadowJar
```

The output JAR goes to `build/libs/`.

## Debug Mode

`StatConstants.DEBUG_MAX_STATS = true` causes new players to start at level 20 with 100 unspent stat points and all professions at level 10. Set to `false` for production.

## Resources

- [Hytale Modding Guides](https://hytalemodding.dev)
- [Hytale Modding Discord](https://discord.gg/hytalemodding)
- [ScaffoldIt Plugin Docs](https://scaffoldit.dev)
