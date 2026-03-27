package com.mmorpg.stats;

/**
 * All multiplier values controlling how each stat point affects gameplay systems.
 * Adjust these values to tune stat scaling without touching logic code.
 */
public final class StatConstants {

    private StatConstants() {}

    // -- Level / point budget --

    /** Stat points granted per level */
    public static final int POINTS_PER_LEVEL = 5;

    /** Maximum points allowed in a single stat */
    public static final int MAX_STAT_POINTS = 500;

    /** Maximum player level */
    public static final int MAX_LEVEL = 100;

    // -- Debug / testing --

    /** When true, new players start at DEBUG_LEVEL with unspent points */
    public static final boolean DEBUG_MAX_STATS = false;

    /** Level to set players to in debug mode (grants POINTS_PER_LEVEL * level points) */
    public static final int DEBUG_LEVEL = 20;

    /** Debug skill level to set all profession skills to on join */
    public static final int DEBUG_SKILL_LEVEL = 10;

    // -- Class title system --

    /** Minimum level required to earn any class title */
    public static final int CLASS_TITLE_MIN_LEVEL = 5;

    /** Single-stat track thresholds per tier (T1-T5) */
    public static final int[] SINGLE_STAT_THRESHOLDS = {12, 30, 60, 120, 150};

    /** Dual-stat track thresholds per tier - applied to EACH primary stat */
    public static final int[] DUAL_STAT_THRESHOLDS = {6, 15, 30, 60, 75};

    // -- XP system --

    /** Base XP required for level 2 */
    public static final int BASE_XP_PER_LEVEL = 300;

    /** XP scaling factor per level (each level requires this multiplier more XP) */
    public static final float XP_LEVEL_SCALING = 1.15f;

    /** Base XP awarded per mob kill (scales with mob max HP) */
    public static final int BASE_KILL_XP = 25;

    /** Reference HP for XP scaling - mobs with this HP give exactly BASE_KILL_XP */
    public static final float XP_REFERENCE_HP = 100.0f;

    /** Minimum XP per kill (floor for weak mobs) */
    public static final int MIN_KILL_XP = 5;

    /** Maximum XP per kill (cap for bosses) */
    public static final int MAX_KILL_XP = 500;

    // -- Gold economy --

    /** Gold every new player starts with */
    public static final long STARTING_GOLD = 100;

    /** Base gold per mob kill (scales with mob HP like XP) */
    public static final int BASE_KILL_GOLD = 10;

    /** Reference HP for gold scaling - mobs with this HP give exactly BASE_KILL_GOLD */
    public static final float GOLD_REFERENCE_HP = 100.0f;

    /** Minimum gold per kill */
    public static final int MIN_KILL_GOLD = 2;

    /** Maximum gold per kill */
    public static final int MAX_KILL_GOLD = 200;

    /** Gold cost per invested stat point when resetting stats */
    public static final int RESET_COST_PER_POINT = 10;

    /** Fraction of current level XP lost on death (0.10 = 10%) */
    public static final float DEATH_XP_PENALTY = 0.10f;

    // -- Base values (before any stat modifications) --

    public static final float BASE_HP = 100.0f;
    public static final float BASE_STAMINA = 100.0f;
    public static final float BASE_MANA = 100.0f;
    public static final float BASE_OXYGEN = 100.0f;
    public static final float BASE_MANA_REGEN = 0.5f;
    public static final float BASE_MELEE_DAMAGE_MULTIPLIER = 1.0f;

    // -- Must-implement: per-point multipliers --

    /** VIT -> max HP: each point adds this much HP */
    public static final float HP_PER_VIT = 5.0f;

    /** DEX -> stamina pool: each point adds this much stamina */
    public static final float STAMINA_PER_DEX = 5.0f;

    /** ARC -> max mana: each point adds this much mana */
    public static final float MANA_PER_ARC = 8.0f;

    /** VIT -> breath duration: each point adds this much oxygen */
    public static final float OXYGEN_PER_VIT = 5.0f;

    /** INT -> mana regen rate: each point adds this much regen per tick */
    public static final float MANA_REGEN_PER_INT = 0.1f;

    /** STR -> melee damage: each point increases multiplier by this fraction (0.02 = 2%) */
    public static final float MELEE_DAMAGE_PER_STR = 0.02f;

    // -- Combat: per-point multipliers --

    /** PRE -> miss chance: each point adds this probability (0.001 = 0.1%) */
    public static final float MISS_CHANCE_PER_PRE = 0.001f;

    /** DEX -> fall damage reduction: each point reduces fall damage by this fraction (0.002 = 0.2%) */
    public static final float FALL_DMG_REDUCTION_PER_DEX = 0.002f;

    /** DEX -> light weapon / bow damage: each point increases multiplier by this fraction */
    public static final float LIGHT_WEAPON_DAMAGE_PER_DEX = 0.016f;

    /** VIT -> damage resistance: each point adds this flat damage reduction */
    public static final float DAMAGE_RESISTANCE_PER_VIT = 0.5f;

    /** INT -> spell damage: each point increases multiplier by this fraction */
    public static final float SPELL_DAMAGE_PER_INT = 0.02f;

    /** ARC -> healing output: each point increases multiplier by this fraction */
    public static final float HEALING_PER_ARC = 0.02f;

    /** PRE -> vendor discount: each point adds this fraction off prices (0.00198 -> 99% at 500) */
    public static final float VENDOR_DISCOUNT_PER_PRE = 0.00198f;

    // -- Critical hit system --

    /** DEX -> crit chance: each point adds this probability (0.003 = 0.3%) */
    public static final float CRIT_CHANCE_PER_DEX = 0.003f;

    /** Maximum crit chance (cap at 75%) */
    public static final float MAX_CRIT_CHANCE = 0.75f;

    /** Base crit damage multiplier (1.5x = 50% bonus damage) */
    public static final float BASE_CRIT_MULTIPLIER = 1.5f;

    /** STR -> crit damage: each point adds this to the crit multiplier */
    public static final float CRIT_DAMAGE_PER_STR = 0.008f;

    // -- Buff system --

    /** Prefix for buff stat modifiers in EntityStatMap */
    public static final String BUFF_MODIFIER_PREFIX = "buff_";

    // -- Skill / profession system --

    /** Maximum skill level for any profession */
    public static final int MAX_SKILL_LEVEL = 100;

    /** Base XP required for skill level 2 */
    public static final int SKILL_BASE_XP = 150;

    /** XP scaling factor per skill level */
    public static final float SKILL_XP_SCALING = 1.15f;

    // -- Block XP values (Mining/Woodcutting/Farming) --

    /** XP for breaking a normal block (dirt, stone, wood, etc.) */
    public static final int NORMAL_BLOCK_XP = 1;

    /** XP for breaking a common ore (iron, coal, copper, tin, etc.) */
    public static final int ORE_BLOCK_XP = 5;

    /** XP for breaking a rare ore (diamond, emerald, gold, etc.) */
    public static final int RARE_ORE_BLOCK_XP = 10;

    // -- Tool skill rewards (Mining, Woodcutting, Farming) --

    /** Gathering speed bonus per skill level (0.15% per level -> 15% at max) */
    public static final float TOOL_SPEED_PER_LEVEL = 0.0015f;

    /** Durability save chance per skill level (0.75% per level -> 75% at max) */
    public static final float DURABILITY_SAVE_PER_LEVEL = 0.0075f;

    // -- Crafting skill rewards --

    /** Material save chance per skill level (0.40% per level -> 40% at max) */
    public static final float MATERIAL_SAVE_PER_LEVEL = 0.004f;

    /** Crafting speed bonus per skill level (0.75% per level -> 75% at max) */
    public static final float CRAFTING_SPEED_PER_LEVEL = 0.0075f;

    // -- Exploration skill rewards --

    /** XP awarded per new chunk explored */
    public static final int EXPLORATION_XP_PER_CHUNK = 1;

    /** Bonus multiplier for chunks in undiscovered zones */
    public static final float EXPLORATION_NEW_ZONE_MULTIPLIER = 5.0f;

    /** Base map reveal radius in chunks (applied via WorldMapTracker) */
    public static final int BASE_MAP_RADIUS = 5;

    /** Gain +1 map radius every N exploration levels */
    public static final int LEVELS_PER_MAP_RADIUS = 3;

    // -- Combat skill rewards (Hunting, Slaying, Void Slaying, Warding) --

    /** Damage bonus per combat skill level (0.50% per level -> 50% at max) */
    public static final float COMBAT_SKILL_DAMAGE_PER_LEVEL = 0.005f;

    /** XP awarded per kill of the matching mob category */
    public static final int COMBAT_SKILL_XP_PER_KILL = 3;

    // -- Mob level & scaling system --

    /** Blocks from spawn per mob level (level = 1 + distance / BLOCKS_PER_MOB_LEVEL) */
    public static final float BLOCKS_PER_MOB_LEVEL = 100.0f;

    /** Maximum mob level */
    public static final int MAX_MOB_LEVEL = 100;

    /** Minimum mob level */
    public static final int MIN_MOB_LEVEL = 1;

    /** Effective HP scaling per mob level - each level adds this fraction to the mob's damage resistance.
     *  e.g., 0.20 = 20% more effective HP per level -> level 10 mob has 2.80x effective HP */
    public static final float MOB_HP_SCALE_PER_LEVEL = 0.20f;

    /** Damage scaling per mob level - each level adds this fraction to mob outgoing damage.
     *  e.g., 0.12 = 12% more damage per level -> level 10 mob does 2.08x damage */
    public static final float MOB_DAMAGE_SCALE_PER_LEVEL = 0.12f;

    // -- XP scaling by mob-player level difference --

    /** Maximum XP bonus for killing mobs above your level (0.50 = +50% at large gap) */
    public static final float XP_HIGH_LEVEL_BONUS = 0.50f;

    /** Level difference at which the full high-level bonus applies */
    public static final int XP_HIGH_LEVEL_CAP = 10;

    /** Level difference at which XP starts to get penalized (mob below player) */
    public static final int XP_PENALTY_START = 3;

    /** Level difference at which XP hits the floor (minimum) */
    public static final int XP_PENALTY_MAX = 15;

    /** Minimum XP multiplier for very low-level mobs (0.10 = 10% of normal XP) */
    public static final float XP_LOW_LEVEL_FLOOR = 0.10f;
}
