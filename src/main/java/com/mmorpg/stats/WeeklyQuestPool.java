package com.mmorpg.stats;

import java.time.*;
import java.util.*;

/**
 * Pool of all possible weekly quests and rotation logic.
 * Each week, a fixed set of quests is selected based on the week number.
 * Quests rotate every Sunday at 12:00 UTC.
 */
public final class WeeklyQuestPool {

    private WeeklyQuestPool() {}

    /** Number of quests active per week */
    static final int QUESTS_PER_WEEK = 5;

    /** All possible weekly quests */
    private static final List<QuestDefinition> ALL_QUESTS = List.of(
        // Combat quests
        new QuestDefinition("slay_15", "Monster Hunter",
                "Slay 15 hostile mobs", QuestObjective.KILL_MOBS, 15, 150, 50),
        new QuestDefinition("slay_30", "Exterminator",
                "Slay 30 hostile mobs", QuestObjective.KILL_MOBS, 30, 300, 100),
        new QuestDefinition("slay_50", "Apex Predator",
                "Slay 50 hostile mobs", QuestObjective.KILL_MOBS, 50, 500, 200),
        new QuestDefinition("deal_500", "Damage Dealer",
                "Deal 500 total damage", QuestObjective.DEAL_DAMAGE, 500, 200, 75),
        new QuestDefinition("deal_2000", "Wrecking Ball",
                "Deal 2000 total damage", QuestObjective.DEAL_DAMAGE, 2000, 400, 150),
        new QuestDefinition("survive_300", "Thick Skinned",
                "Survive 300 damage taken", QuestObjective.SURVIVE_DAMAGE, 300, 200, 75),

        // Gathering quests
        new QuestDefinition("gather_50", "Prospector",
                "Gather 50 blocks", QuestObjective.GATHER_BLOCKS, 50, 150, 50),
        new QuestDefinition("gather_150", "Strip Miner",
                "Gather 150 blocks", QuestObjective.GATHER_BLOCKS, 150, 350, 125),
        new QuestDefinition("gather_300", "Excavator",
                "Gather 300 blocks", QuestObjective.GATHER_BLOCKS, 300, 500, 200),

        // Crafting quests
        new QuestDefinition("craft_10", "Tinkerer",
                "Craft 10 items", QuestObjective.CRAFT_ITEMS, 10, 200, 75),
        new QuestDefinition("craft_25", "Artisan",
                "Craft 25 items", QuestObjective.CRAFT_ITEMS, 25, 400, 150),

        // XP quests
        new QuestDefinition("earn_500xp", "Seeker",
                "Earn 500 XP", QuestObjective.EARN_XP, 500, 250, 100),
        new QuestDefinition("earn_2000xp", "Scholar",
                "Earn 2000 XP", QuestObjective.EARN_XP, 2000, 500, 200),
        new QuestDefinition("prof_xp_200", "Apprentice",
                "Earn 200 profession XP", QuestObjective.EARN_PROFESSION_XP, 200, 200, 75),
        new QuestDefinition("prof_xp_500", "Journeyman",
                "Earn 500 profession XP", QuestObjective.EARN_PROFESSION_XP, 500, 400, 150),

        // Exploration quests
        new QuestDefinition("explore_10", "Pathfinder",
                "Explore 10 new chunks", QuestObjective.EXPLORE_CHUNKS, 10, 200, 75),
        new QuestDefinition("explore_30", "Cartographer",
                "Explore 30 new chunks", QuestObjective.EXPLORE_CHUNKS, 30, 400, 150),

        // Skill quests
        new QuestDefinition("use_skills_5", "Practitioner",
                "Use skills 5 times", QuestObjective.USE_SKILL, 5, 150, 50),
        new QuestDefinition("use_skills_15", "Virtuoso",
                "Use skills 15 times", QuestObjective.USE_SKILL, 15, 300, 100),

        // Stat quests
        new QuestDefinition("spend_10pts", "Growing Stronger",
                "Spend 10 stat points", QuestObjective.SPEND_STAT_POINTS, 10, 200, 75),
        new QuestDefinition("spend_25pts", "Self Improvement",
                "Spend 25 stat points", QuestObjective.SPEND_STAT_POINTS, 25, 400, 150)
    );

    /**
     * Get the current week number since epoch, used as rotation seed.
     * Week resets Sunday at 12:00 UTC.
     */
    static long currentWeekId() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        // Shift so that Sunday 12:00 UTC is the boundary
        ZonedDateTime boundary = now.minusHours(12);
        // Use ISO week starting Monday; adjust by shifting to make Sunday the boundary
        long daysSinceEpoch = boundary.toLocalDate().toEpochDay();
        // Divide by 7 to get a week number
        return daysSinceEpoch / 7;
    }

    /**
     * Get the active quests for the current week.
     */
    public static List<QuestDefinition> getActiveQuests() {
        return getQuestsForWeek(currentWeekId());
    }

    /**
     * Get quests for a specific week ID (deterministic rotation).
     */
    static List<QuestDefinition> getQuestsForWeek(long weekId) {
        // Create a shuffled copy using the week as seed for deterministic rotation
        List<QuestDefinition> pool = new ArrayList<>(ALL_QUESTS);
        Collections.shuffle(pool, new Random(weekId * 31337L));
        return Collections.unmodifiableList(pool.subList(0, Math.min(QUESTS_PER_WEEK, pool.size())));
    }

    /**
     * Get time remaining until next reset (Sunday 12:00 UTC),
     * formatted as a human-readable string.
     */
    public static String timeUntilReset() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        // Find next Sunday at 12:00 UTC
        ZonedDateTime nextSunday = now.with(java.time.temporal.TemporalAdjusters
                .nextOrSame(DayOfWeek.SUNDAY))
                .withHour(12).withMinute(0).withSecond(0).withNano(0);
        // If we're past Sunday noon, go to next Sunday
        if (!now.isBefore(nextSunday)) {
            nextSunday = nextSunday.plusWeeks(1);
        }
        Duration remaining = Duration.between(now, nextSunday);
        long days = remaining.toDays();
        long hours = remaining.toHoursPart();
        if (days > 0) return days + "d " + hours + "h";
        long minutes = remaining.toMinutesPart();
        return hours + "h " + minutes + "m";
    }
}
