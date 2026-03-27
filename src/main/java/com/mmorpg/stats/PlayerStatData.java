package com.mmorpg.stats;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * ECS component storing six base stats per player, plus level and point budget.
 * Persisted via putComponent with BSON codec serialization.
 */
public class PlayerStatData implements Component<EntityStore> {

    private int level;
    private int xp;
    private int strength;
    private int dexterity;
    private int vitality;
    private int intellect;
    private int presence;
    private int arcana;
    private int kills;

    public static final BuilderCodec<PlayerStatData> CODEC =
            BuilderCodec.<PlayerStatData>builder(PlayerStatData.class, PlayerStatData::new)
                    .addField(new KeyedCodec<>("Level", Codec.INTEGER),
                            (data, value) -> data.level = value,
                            data -> data.level)
                    .addField(new KeyedCodec<>("XP", Codec.INTEGER),
                            (data, value) -> data.xp = value,
                            data -> data.xp)
                    .addField(new KeyedCodec<>("Strength", Codec.INTEGER),
                            (data, value) -> data.strength = value,
                            data -> data.strength)
                    .addField(new KeyedCodec<>("Dexterity", Codec.INTEGER),
                            (data, value) -> data.dexterity = value,
                            data -> data.dexterity)
                    .addField(new KeyedCodec<>("Vitality", Codec.INTEGER),
                            (data, value) -> data.vitality = value,
                            data -> data.vitality)
                    .addField(new KeyedCodec<>("Intellect", Codec.INTEGER),
                            (data, value) -> data.intellect = value,
                            data -> data.intellect)
                    .addField(new KeyedCodec<>("Presence", Codec.INTEGER),
                            (data, value) -> data.presence = value,
                            data -> data.presence)
                    .addField(new KeyedCodec<>("Arcana", Codec.INTEGER),
                            (data, value) -> data.arcana = value,
                            data -> data.arcana)
                    .addField(new KeyedCodec<>("Kills", Codec.INTEGER),
                            (data, value) -> data.kills = value,
                            data -> data.kills)
                    .build();

    public PlayerStatData() {
        this.level = 1;
        this.xp = 0;
        this.strength = 0;
        this.dexterity = 0;
        this.vitality = 0;
        this.intellect = 0;
        this.presence = 0;
        this.arcana = 0;
        this.kills = 0;
    }

    public PlayerStatData(PlayerStatData clone) {
        this.level = clone.level;
        this.xp = clone.xp;
        this.strength = clone.strength;
        this.dexterity = clone.dexterity;
        this.vitality = clone.vitality;
        this.intellect = clone.intellect;
        this.presence = clone.presence;
        this.arcana = clone.arcana;
        this.kills = clone.kills;
    }

    // Getters
    public int getLevel() { return level; }
    public int getXp() { return xp; }
    public int getStrength() { return strength; }
    public int getDexterity() { return dexterity; }
    public int getVitality() { return vitality; }
    public int getIntellect() { return intellect; }
    public int getPresence() { return presence; }
    public int getArcana() { return arcana; }
    public int getKills() { return kills; }

    // Setters (clamped to 0..MAX_STAT_POINTS)
    public void setLevel(int level) { this.level = Math.max(1, Math.min(level, StatConstants.MAX_LEVEL)); }
    public void setXp(int xp) { this.xp = Math.max(0, xp); }

    /** XP required to reach the next level from current level. */
    public int getXpForNextLevel() {
        return StatCalculation.xpForLevel(level + 1);
    }

    /**
     * Adds XP and handles level-ups. Returns the number of levels gained.
     */
    public int addXp(int amount) {
        if (level >= StatConstants.MAX_LEVEL) return 0;
        xp += amount;
        int levelsGained = 0;
        while (level < StatConstants.MAX_LEVEL && xp >= getXpForNextLevel()) {
            xp -= getXpForNextLevel();
            level++;
            levelsGained++;
        }
        if (level >= StatConstants.MAX_LEVEL) {
            xp = 0; // cap XP at max level
        }
        return levelsGained;
    }
    public void setStrength(int v) { this.strength = clamp(v); }
    public void setDexterity(int v) { this.dexterity = clamp(v); }
    public void setVitality(int v) { this.vitality = clamp(v); }
    public void setIntellect(int v) { this.intellect = clamp(v); }
    public void setPresence(int v) { this.presence = clamp(v); }
    public void setArcana(int v) { this.arcana = clamp(v); }

    /** Increment kill counter by one. */
    public void addKill() { this.kills++; }
    public void setKills(int kills) { this.kills = Math.max(0, kills); }

    /** Total stat points currently spent across all stats. */
    public int getSpentPoints() {
        return strength + dexterity + vitality + intellect + presence + arcana;
    }

    /** Total stat points available at this level. */
    public int getTotalPoints() {
        return StatConstants.POINTS_PER_LEVEL * level;
    }

    /** Remaining unspent points. */
    public int getAvailablePoints() {
        return Math.max(0, getTotalPoints() - getSpentPoints());
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(value, StatConstants.MAX_STAT_POINTS));
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new PlayerStatData(this);
    }
}
