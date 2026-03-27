package com.mmorpg.stats;

/**
 * Types of passive effects that the class title system can apply.
 * Simple stat-based effects are implemented immediately;
 * complex/triggered effects are tracked for future implementation.
 */
public enum PassiveType {
    MELEE_DAMAGE_PERCENT,       // +X% melee damage (DamageHandler)
    SPELL_DAMAGE_PERCENT,       // +X% spell/all damage (DamageHandler)
    MAX_HP_PERCENT,             // +X% max HP (StatEffectApplier)
    MISS_CHANCE_PERCENT,        // +X% enemy miss chance (DamageHandler - PRE)
    DAMAGE_REDUCTION_PERCENT,   // incoming damage reduced by X% (DamageHandler)
    MANA_POOL_PERCENT,          // +X% max mana (StatEffectApplier)
    STAMINA_POOL_PERCENT,       // +X% max stamina (StatEffectApplier)
    HEALING_OUTPUT_PERCENT,     // +X% healing output (AbilityCommand + BuffTickSystem)
    DAMAGE_NEGATE_CHANCE,       // X% chance to negate damage (DamageHandler)
    LOW_HP_RESIST_PERCENT,      // At <=25% HP, +X% DR (DamageHandler)
    CRIT_MULTIPLIER_PERCENT,    // +X crit damage multiplier (DamageHandler)
    HP_REGEN_PERCENT,           // +X% max HP regen/s (BuffTickSystem)
    MANA_REGEN_PERCENT,         // +X% mana regen rate (BuffTickSystem)
    FALL_DAMAGE_REDUCTION,      // +X% fall damage reduction (DamageHandler - DEX)
    SHOP_DISCOUNT_PERCENT,      // vendor prices -X% (ShopPage + ShopCommand)
}
