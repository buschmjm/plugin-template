package com.mmorpg.stats;

/**
 * Immutable definition of a quest. Quest instances are created from these templates.
 */
public class QuestDefinition {
    private final String id;
    private final String name;
    private final String description;
    private final QuestObjective objective;
    private final int targetCount;
    private final int xpReward;
    private final int goldReward;

    public QuestDefinition(String id, String name, String description,
                           QuestObjective objective, int targetCount, int xpReward,
                           int goldReward) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.objective = objective;
        this.targetCount = targetCount;
        this.xpReward = xpReward;
        this.goldReward = goldReward;
    }

    /** Convenience constructor for quests with no gold reward. */
    public QuestDefinition(String id, String name, String description,
                           QuestObjective objective, int targetCount, int xpReward) {
        this(id, name, description, objective, targetCount, xpReward, 0);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public QuestObjective getObjective() { return objective; }
    public int getTargetCount() { return targetCount; }
    public int getXpReward() { return xpReward; }
    public int getGoldReward() { return goldReward; }
}
