package com.mmorpg.stats;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * ECS component storing a player's guild membership.
 * Each player can be in one guild at a time.
 * Tracks personal contributions (gold donated, quests completed for guild).
 */
public class GuildData implements Component<EntityStore> {

    private String guildName;
    private String guildRank; // "leader", "officer", "member"
    private long goldDonated;
    private int questsCompleted; // quests completed while in this guild

    public static final BuilderCodec<GuildData> CODEC =
            BuilderCodec.<GuildData>builder(GuildData.class, GuildData::new)
                    .addField(new KeyedCodec<>("GuildName", Codec.STRING),
                            (data, value) -> data.guildName = value,
                            data -> data.guildName)
                    .addField(new KeyedCodec<>("GuildRank", Codec.STRING),
                            (data, value) -> data.guildRank = value,
                            data -> data.guildRank)
                    .addField(new KeyedCodec<>("GoldDonated", Codec.LONG),
                            (data, value) -> data.goldDonated = value,
                            data -> data.goldDonated)
                    .addField(new KeyedCodec<>("QuestsCompleted", Codec.INTEGER),
                            (data, value) -> data.questsCompleted = value,
                            data -> data.questsCompleted)
                    .build();

    public GuildData() {
        this.guildName = "";
        this.guildRank = "";
        this.goldDonated = 0;
        this.questsCompleted = 0;
    }

    public GuildData(GuildData clone) {
        this.guildName = clone.guildName;
        this.guildRank = clone.guildRank;
        this.goldDonated = clone.goldDonated;
        this.questsCompleted = clone.questsCompleted;
    }

    // -- Getters / Setters --

    public String getGuildName() { return guildName; }
    public void setGuildName(String guildName) { this.guildName = guildName != null ? guildName : ""; }

    public String getGuildRank() { return guildRank; }
    public void setGuildRank(String rank) { this.guildRank = rank != null ? rank : ""; }

    public long getGoldDonated() { return goldDonated; }
    public void setGoldDonated(long goldDonated) { this.goldDonated = Math.max(0, goldDonated); }
    public void addGoldDonated(long amount) { this.goldDonated += amount; }

    public int getQuestsCompleted() { return questsCompleted; }
    public void setQuestsCompleted(int count) { this.questsCompleted = Math.max(0, count); }
    public void incrementQuestsCompleted() { this.questsCompleted++; }

    /** Check if the player is in a guild */
    public boolean isInGuild() { return guildName != null && !guildName.isEmpty(); }

    /** Check if the player is the guild leader */
    public boolean isLeader() { return "leader".equals(guildRank); }

    /** Check if the player is an officer or leader */
    public boolean isOfficerOrAbove() { return "leader".equals(guildRank) || "officer".equals(guildRank); }

    /** Leave the guild (resets all guild-specific data) */
    public void leaveGuild() {
        this.guildName = "";
        this.guildRank = "";
        this.goldDonated = 0;
        this.questsCompleted = 0;
    }

    /** Join a guild as a member */
    public void joinGuild(String name) {
        this.guildName = name;
        this.guildRank = "member";
        this.goldDonated = 0;
        this.questsCompleted = 0;
    }

    /** Create a new guild as the leader */
    public void createGuild(String name) {
        this.guildName = name;
        this.guildRank = "leader";
        this.goldDonated = 0;
        this.questsCompleted = 0;
    }

    /** Get display rank title based on quests completed */
    public String getRankTitle() {
        if (!isInGuild()) return "None";
        if (isLeader()) return "Guild Leader";
        if ("officer".equals(guildRank)) return "Officer";
        // Member rank tiers based on quests completed
        if (questsCompleted >= 100) return "Champion";
        if (questsCompleted >= 50) return "Veteran";
        if (questsCompleted >= 20) return "Regular";
        if (questsCompleted >= 5) return "Initiate";
        return "Recruit";
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new GuildData(this);
    }
}
