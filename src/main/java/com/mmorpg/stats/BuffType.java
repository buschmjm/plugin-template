package com.mmorpg.stats;

/**
 * Types of buff/debuff effects.
 */
public enum BuffType {
    /** Modifies entity stat maximums (HP, mana, stamina, etc.) */
    STAT_MODIFIER,
    /** Deals damage over time at regular intervals */
    DAMAGE_OVER_TIME,
    /** Heals over time at regular intervals */
    HEAL_OVER_TIME
}
