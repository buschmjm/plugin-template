package com.mmorpg.stats;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global in-memory registry for guild data.
 * Tracks guild membership, XP, level, and perks.
 * Guild data is rebuilt from player GuildData components on join.
 */
public final class GuildRegistry {

    private GuildRegistry() {}

    /** Guild XP required per level (cumulative) */
    public static final int GUILD_BASE_XP = 1000;
    public static final float GUILD_XP_SCALING = 1.25f;
    public static final int MAX_GUILD_LEVEL = 20;

    /** Gold-to-guild-XP conversion rate */
    public static final int GOLD_TO_GUILD_XP = 1; // 1 gold = 1 guild XP

    /** Shared XP bonus per guild level (1% per level) */
    public static final float SHARED_XP_BONUS_PER_LEVEL = 0.01f;

    /** Max shared XP bonus (20% at max guild level) */
    public static final float MAX_SHARED_XP_BONUS = 0.20f;

    /** Guild cost to create */
    public static final long GUILD_CREATION_COST = 500;

    // -- Guild state --

    private static final Map<String, GuildInfo> guilds = new ConcurrentHashMap<>();

    /** Lightweight guild state tracked globally */
    public static class GuildInfo {
        private final String name;
        private long totalXp;
        private int level;
        private final Set<String> memberUuids = ConcurrentHashMap.newKeySet();

        public GuildInfo(String name) {
            this.name = name;
            this.totalXp = 0;
            this.level = 1;
        }

        public String getName() { return name; }
        public long getTotalXp() { return totalXp; }
        public int getLevel() { return level; }
        public Set<String> getMemberUuids() { return Collections.unmodifiableSet(memberUuids); }
        public int getMemberCount() { return memberUuids.size(); }

        public void addXp(long xp) {
            this.totalXp += xp;
            recalculateLevel();
        }

        public void addMember(String uuid) { memberUuids.add(uuid); }
        public void removeMember(String uuid) { memberUuids.remove(uuid); }

        private void recalculateLevel() {
            int newLevel = 1;
            long xpNeeded = GUILD_BASE_XP;
            long accumulated = 0;
            while (newLevel < MAX_GUILD_LEVEL && accumulated + xpNeeded <= totalXp) {
                accumulated += xpNeeded;
                newLevel++;
                xpNeeded = (long)(GUILD_BASE_XP * Math.pow(GUILD_XP_SCALING, newLevel - 1));
            }
            this.level = newLevel;
        }

        /** Get XP required for the next level */
        public long getXpForNextLevel() {
            if (level >= MAX_GUILD_LEVEL) return 0;
            return (long)(GUILD_BASE_XP * Math.pow(GUILD_XP_SCALING, level - 1));
        }

        /** Get XP accumulated toward the current level */
        public long getXpInCurrentLevel() {
            long accumulated = 0;
            for (int i = 1; i < level; i++) {
                accumulated += (long)(GUILD_BASE_XP * Math.pow(GUILD_XP_SCALING, i - 1));
            }
            return totalXp - accumulated;
        }
    }

    // -- Public API --

    /** Create a new guild. Returns the GuildInfo or null if name already taken. */
    public static GuildInfo createGuild(String name, String leaderUuid) {
        String key = name.toLowerCase();
        if (guilds.containsKey(key)) return null;
        GuildInfo info = new GuildInfo(name);
        info.addMember(leaderUuid);
        guilds.put(key, info);
        return info;
    }

    /** Get guild info by name (case-insensitive) */
    public static GuildInfo getGuild(String name) {
        return guilds.get(name.toLowerCase());
    }

    /** Check if a guild exists */
    public static boolean guildExists(String name) {
        return guilds.containsKey(name.toLowerCase());
    }

    /** Get guild level (0 if guild doesn't exist) */
    public static int getGuildLevel(String name) {
        GuildInfo info = getGuild(name);
        return info != null ? info.getLevel() : 0;
    }

    /** Disband a guild */
    public static void disbandGuild(String name) {
        guilds.remove(name.toLowerCase());
    }

    /** Add a member to a guild */
    public static void addMember(String guildName, String uuid) {
        GuildInfo info = getGuild(guildName);
        if (info != null) info.addMember(uuid);
    }

    /** Remove a member from a guild */
    public static void removeMember(String guildName, String uuid) {
        GuildInfo info = getGuild(guildName);
        if (info != null) info.removeMember(uuid);
    }

    /** Donate gold to a guild (converts to guild XP) */
    public static void donateGold(String guildName, long goldAmount) {
        GuildInfo info = getGuild(guildName);
        if (info != null) {
            info.addXp(goldAmount * GOLD_TO_GUILD_XP);
        }
    }

    /** Get shared XP bonus multiplier for a guild (0.0 if no guild) */
    public static float getSharedXpBonus(String guildName) {
        GuildInfo info = getGuild(guildName);
        if (info == null) return 0.0f;
        return Math.min(info.getLevel() * SHARED_XP_BONUS_PER_LEVEL, MAX_SHARED_XP_BONUS);
    }

    /** Get all guild names */
    public static Set<String> getAllGuildNames() {
        Set<String> names = new HashSet<>();
        for (GuildInfo info : guilds.values()) {
            names.add(info.getName());
        }
        return names;
    }

    /** Clear all guilds (for server restart) */
    public static void clearAll() {
        guilds.clear();
    }

    /** Calculate guild level from XP (static utility for tests) */
    public static int calculateLevel(long totalXp) {
        int level = 1;
        long xpNeeded = GUILD_BASE_XP;
        long accumulated = 0;
        while (level < MAX_GUILD_LEVEL && accumulated + xpNeeded <= totalXp) {
            accumulated += xpNeeded;
            level++;
            xpNeeded = (long)(GUILD_BASE_XP * Math.pow(GUILD_XP_SCALING, level - 1));
        }
        return level;
    }
}
