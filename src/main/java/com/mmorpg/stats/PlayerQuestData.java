package com.mmorpg.stats;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * ECS component tracking weekly quest progress per player.
 * Persisted via BSON. Resets when the week changes.
 */
public class PlayerQuestData implements Component<EntityStore> {

    /** The week ID this data belongs to. If stale, progress resets. */
    private long weekId;

    /** Quest ID -> current progress count */
    private final Map<String, Integer> progress = new LinkedHashMap<>();

    /** Quest IDs completed this week */
    private final Set<String> completed = new LinkedHashSet<>();

    /** Quest IDs the player is actively tracking on HUD */
    private final Set<String> tracked = new LinkedHashSet<>();

    /** Quest IDs whose rewards have been redeemed */
    private final Set<String> redeemed = new LinkedHashSet<>();

    public static final BuilderCodec<PlayerQuestData> CODEC =
            BuilderCodec.<PlayerQuestData>builder(PlayerQuestData.class, PlayerQuestData::new)
                    .addField(new KeyedCodec<>("WeekId", Codec.LONG),
                            (data, value) -> data.weekId = value,
                            data -> data.weekId)
                    .addField(new KeyedCodec<>("QuestProgress", Codec.STRING),
                            PlayerQuestData::parseProgress,
                            PlayerQuestData::serializeProgress)
                    .addField(new KeyedCodec<>("QuestCompleted", Codec.STRING),
                            PlayerQuestData::parseCompleted,
                            PlayerQuestData::serializeCompleted)
                    .addField(new KeyedCodec<>("QuestTracked", Codec.STRING),
                            PlayerQuestData::parseTracked,
                            PlayerQuestData::serializeTracked)
                    .addField(new KeyedCodec<>("QuestRedeemed", Codec.STRING),
                            PlayerQuestData::parseRedeemed,
                            PlayerQuestData::serializeRedeemed)
                    .build();

    public PlayerQuestData() {
        this.weekId = WeeklyQuestPool.currentWeekId();
    }

    /** Check and reset if the week has changed. Returns true if reset occurred. */
    public boolean ensureCurrentWeek() {
        long current = WeeklyQuestPool.currentWeekId();
        if (weekId != current) {
            weekId = current;
            progress.clear();
            completed.clear();
            tracked.clear();
            redeemed.clear();
            return true;
        }
        return false;
    }

    /** Increment progress for a quest objective type. Returns list of newly completed quest IDs. */
    public List<String> incrementProgress(QuestObjective objective, int amount) {
        ensureCurrentWeek();
        List<QuestDefinition> active = WeeklyQuestPool.getActiveQuests();
        List<String> newlyCompleted = new ArrayList<>();

        for (QuestDefinition quest : active) {
            if (quest.getObjective() != objective) continue;
            if (completed.contains(quest.getId())) continue;

            int current = progress.getOrDefault(quest.getId(), 0) + amount;
            progress.put(quest.getId(), current);

            if (current >= quest.getTargetCount()) {
                completed.add(quest.getId());
                newlyCompleted.add(quest.getId());
            }
        }
        return newlyCompleted;
    }

    public int getProgress(String questId) {
        return progress.getOrDefault(questId, 0);
    }

    public boolean isCompleted(String questId) {
        return completed.contains(questId);
    }

    public int getCompletedCount() {
        return completed.size();
    }

    public long getWeekId() { return weekId; }

    public boolean isTracked(String questId) {
        return tracked.contains(questId);
    }

    public void toggleTracked(String questId) {
        if (!tracked.remove(questId)) {
            tracked.add(questId);
        }
    }

    public Set<String> getTrackedIds() {
        return Collections.unmodifiableSet(tracked);
    }

    public boolean isRedeemed(String questId) {
        return redeemed.contains(questId);
    }

    /** Mark a quest as redeemed. Returns true if it was newly redeemed. */
    public boolean redeem(String questId) {
        if (!completed.contains(questId)) return false;
        return redeemed.add(questId);
    }

    /** Count of quests completed AND redeemed. */
    public int getRedeemedCount() {
        return redeemed.size();
    }

    /** Returns true if any quest is completed but not yet redeemed. */
    public boolean hasUnredeemed() {
        for (String id : completed) {
            if (!redeemed.contains(id)) return true;
        }
        return false;
    }

    // -- Serialization helpers --

    private static void parseProgress(PlayerQuestData data, String value) {
        if (value == null || value.isEmpty()) return;
        for (String entry : value.split(";")) {
            String[] parts = entry.split("=", 2);
            if (parts.length == 2) {
                try {
                    data.progress.put(parts[0], Integer.parseInt(parts[1]));
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    private static String serializeProgress(PlayerQuestData data) {
        StringBuilder sb = new StringBuilder();
        for (var entry : data.progress.entrySet()) {
            if (!sb.isEmpty()) sb.append(';');
            sb.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return sb.toString();
    }

    private static void parseCompleted(PlayerQuestData data, String value) {
        if (value == null || value.isEmpty()) return;
        Collections.addAll(data.completed, value.split(";"));
    }

    private static String serializeCompleted(PlayerQuestData data) {
        return String.join(";", data.completed);
    }

    private static void parseTracked(PlayerQuestData data, String value) {
        if (value == null || value.isEmpty()) return;
        Collections.addAll(data.tracked, value.split(";"));
    }

    private static String serializeTracked(PlayerQuestData data) {
        return String.join(";", data.tracked);
    }

    private static void parseRedeemed(PlayerQuestData data, String value) {
        if (value == null || value.isEmpty()) return;
        Collections.addAll(data.redeemed, value.split(";"));
    }

    private static String serializeRedeemed(PlayerQuestData data) {
        return String.join(";", data.redeemed);
    }

    @Nonnull
    @Override
    public PlayerQuestData clone() {
        PlayerQuestData copy = new PlayerQuestData();
        copy.weekId = this.weekId;
        copy.progress.putAll(this.progress);
        copy.completed.addAll(this.completed);
        copy.tracked.addAll(this.tracked);
        copy.redeemed.addAll(this.redeemed);
        return copy;
    }
}
