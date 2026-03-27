package com.mmorpg.stats;

/**
 * Types of quest objectives that can be tracked.
 */
public enum QuestObjective {
    /** Kill any mob */
    KILL_MOBS,
    /** Mine/gather blocks */
    GATHER_BLOCKS,
    /** Earn XP (any source) */
    EARN_XP,
    /** Use a skill */
    USE_SKILL,
    /** Explore new chunks */
    EXPLORE_CHUNKS,
    /** Craft items */
    CRAFT_ITEMS,
    /** Earn profession XP */
    EARN_PROFESSION_XP,
    /** Spend stat points */
    SPEND_STAT_POINTS,
    /** Deal damage */
    DEAL_DAMAGE,
    /** Take damage (survive) */
    SURVIVE_DAMAGE
}
