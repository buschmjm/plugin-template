package com.mmorpg.stats;

/**
 * Pure stat calculation logic with no Hytale dependencies.
 * All formulas read multipliers from StatConstants.
 */
public final class StatCalculation {

    private StatCalculation() {}

    // -- Must-implement calculations --

    /** VIT -> max HP */
    public static float calculateMaxHp(int vitality) {
        return StatConstants.BASE_HP + vitality * StatConstants.HP_PER_VIT;
    }

    /** DEX -> stamina pool size */
    public static float calculateMaxStamina(int dexterity) {
        return StatConstants.BASE_STAMINA + dexterity * StatConstants.STAMINA_PER_DEX;
    }

    /** ARC -> max mana pool size */
    public static float calculateMaxMana(int arcana) {
        return StatConstants.BASE_MANA + arcana * StatConstants.MANA_PER_ARC;
    }

    /** VIT -> underwater breath duration */
    public static float calculateMaxOxygen(int vitality) {
        return StatConstants.BASE_OXYGEN + vitality * StatConstants.OXYGEN_PER_VIT;
    }

    /** INT -> mana regen rate */
    public static float calculateManaRegenRate(int intellect) {
        return StatConstants.BASE_MANA_REGEN + intellect * StatConstants.MANA_REGEN_PER_INT;
    }

    /** STR -> melee damage multiplier */
    public static float calculateMeleeDamageMultiplier(int strength) {
        return StatConstants.BASE_MELEE_DAMAGE_MULTIPLIER + strength * StatConstants.MELEE_DAMAGE_PER_STR;
    }

    // -- Implement-if-supported calculations --

    /** PRE -> miss chance (clamped 0.0 - 0.50) */
    public static float calculateMissChance(int presence) {
        return Math.min(0.50f, presence * StatConstants.MISS_CHANCE_PER_PRE);
    }

    /** DEX -> fall damage reduction fraction (clamped 0.0 - 1.0) */
    public static float calculateFallDamageReduction(int dexterity) {
        return Math.min(1.0f, dexterity * StatConstants.FALL_DMG_REDUCTION_PER_DEX);
    }

    /** DEX -> light weapon / bow damage multiplier */
    public static float calculateLightWeaponDamageMultiplier(int dexterity) {
        return 1.0f + dexterity * StatConstants.LIGHT_WEAPON_DAMAGE_PER_DEX;
    }

    /** VIT -> flat damage resistance */
    public static float calculateDamageResistance(int vitality) {
        return vitality * StatConstants.DAMAGE_RESISTANCE_PER_VIT;
    }

    /** INT -> spell damage multiplier */
    public static float calculateSpellDamageMultiplier(int intellect) {
        return 1.0f + intellect * StatConstants.SPELL_DAMAGE_PER_INT;
    }

    /** ARC -> healing output multiplier */
    public static float calculateHealingMultiplier(int arcana) {
        return 1.0f + arcana * StatConstants.HEALING_PER_ARC;
    }

    /** PRE -> vendor discount fraction (clamped 0.0 - 1.0) */
    public static float calculateVendorDiscount(int presence) {
        return Math.min(1.0f, presence * StatConstants.VENDOR_DISCOUNT_PER_PRE);
    }

    // -- Critical hit calculations --

    /** DEX -> crit chance (clamped to MAX_CRIT_CHANCE) */
    public static float calculateCritChance(int dexterity) {
        return Math.min(StatConstants.MAX_CRIT_CHANCE,
                dexterity * StatConstants.CRIT_CHANCE_PER_DEX);
    }

    /** STR -> crit damage multiplier */
    public static float calculateCritMultiplier(int strength) {
        return StatConstants.BASE_CRIT_MULTIPLIER
                + strength * StatConstants.CRIT_DAMAGE_PER_STR;
    }

    // -- XP calculations --

    /** XP required to go from (level-1) to level. Level 2 requires BASE_XP_PER_LEVEL. */
    public static int xpForLevel(int level) {
        if (level <= 1) return 0;
        return (int) (StatConstants.BASE_XP_PER_LEVEL
                * Math.pow(StatConstants.XP_LEVEL_SCALING, level - 2));
    }

    /** Total cumulative XP required to reach a given level from level 1. */
    public static int totalXpForLevel(int level) {
        int total = 0;
        for (int i = 2; i <= level; i++) {
            total += xpForLevel(i);
        }
        return total;
    }

    /**
     * Scale XP based on the killed mob's max HP relative to the reference HP.
     * Tougher mobs give more XP, weak mobs give less.
     */
    public static int calculateKillXp(float mobMaxHp) {
        if (mobMaxHp <= 0) return StatConstants.MIN_KILL_XP;
        float ratio = mobMaxHp / StatConstants.XP_REFERENCE_HP;
        int xp = (int) (StatConstants.BASE_KILL_XP * ratio);
        return Math.max(StatConstants.MIN_KILL_XP, Math.min(xp, StatConstants.MAX_KILL_XP));
    }

    /**
     * Scale gold based on the killed mob's max HP, same pattern as XP.
     */
    public static int calculateKillGold(float mobMaxHp) {
        if (mobMaxHp <= 0) return StatConstants.MIN_KILL_GOLD;
        float ratio = mobMaxHp / StatConstants.GOLD_REFERENCE_HP;
        int gold = (int) (StatConstants.BASE_KILL_GOLD * ratio);
        return Math.max(StatConstants.MIN_KILL_GOLD, Math.min(gold, StatConstants.MAX_KILL_GOLD));
    }

    /**
     * Calculate XP lost on death - a fraction of the XP needed for the current level.
     * Never drops below 0 XP.
     */
    public static int calculateDeathXpPenalty(int currentLevel, int currentXp) {
        int xpForCurrentLevel = xpForLevel(currentLevel + 1);
        int penalty = (int) (xpForCurrentLevel * StatConstants.DEATH_XP_PENALTY);
        return Math.min(penalty, currentXp);
    }

    // -- Mob level & scaling calculations --

    /**
     * Calculate mob level from distance to world origin (spawn).
     * Level 1 near spawn, increases by 1 every BLOCKS_PER_MOB_LEVEL blocks.
     * Uses horizontal distance only (XZ plane).
     */
    public static int calculateMobLevel(double x, double z) {
        double distance = Math.sqrt(x * x + z * z);
        int level = 1 + (int) (distance / StatConstants.BLOCKS_PER_MOB_LEVEL);
        return Math.max(StatConstants.MIN_MOB_LEVEL,
                Math.min(level, StatConstants.MAX_MOB_LEVEL));
    }

    /**
     * Effective HP multiplier for a mob at the given level.
     * Level 1 = 1.0x, each additional level adds MOB_HP_SCALE_PER_LEVEL.
     */
    public static float calculateMobHpMultiplier(int mobLevel) {
        return 1.0f + (mobLevel - 1) * StatConstants.MOB_HP_SCALE_PER_LEVEL;
    }

    /**
     * Damage multiplier for a mob at the given level.
     * Level 1 = 1.0x, each additional level adds MOB_DAMAGE_SCALE_PER_LEVEL.
     */
    public static float calculateMobDamageMultiplier(int mobLevel) {
        return 1.0f + (mobLevel - 1) * StatConstants.MOB_DAMAGE_SCALE_PER_LEVEL;
    }

    /**
     * XP multiplier based on the difference between mob level and player level.
     * - Mob higher than player: bonus up to +50% (encourages exploration)
     * - Mob same level as player: 1.0x
     * - Mob 3+ levels below player: starts decreasing
     * - Mob 15+ levels below player: floored at 10% (discourages farming low-level mobs)
     */
    public static float calculateLevelDifferenceXpMultiplier(int mobLevel, int playerLevel) {
        int diff = mobLevel - playerLevel; // positive = mob is higher

        if (diff > 0) {
            // Bonus for killing higher-level mobs, capped at XP_HIGH_LEVEL_CAP
            float bonusFraction = Math.min((float) diff / StatConstants.XP_HIGH_LEVEL_CAP, 1.0f);
            return 1.0f + bonusFraction * StatConstants.XP_HIGH_LEVEL_BONUS;
        } else if (diff >= -StatConstants.XP_PENALTY_START) {
            // Within tolerance - no penalty
            return 1.0f;
        } else {
            // Penalty zone: linear ramp from 1.0 down to XP_LOW_LEVEL_FLOOR
            int penaltyLevels = -diff - StatConstants.XP_PENALTY_START;
            int penaltyRange = StatConstants.XP_PENALTY_MAX - StatConstants.XP_PENALTY_START;
            float penaltyFraction = Math.min((float) penaltyLevels / penaltyRange, 1.0f);
            return Math.max(StatConstants.XP_LOW_LEVEL_FLOOR,
                    1.0f - penaltyFraction * (1.0f - StatConstants.XP_LOW_LEVEL_FLOOR));
        }
    }
}
