package com.mmorpg.stats;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global in-memory registry mapping chunk coordinates to claim owners.
 * Used for fast lookup during block event interception.
 * Rebuilt from player ClaimData on server start / player join.
 */
public final class ClaimRegistry {

    private ClaimRegistry() {}

    /** Map of "chunkX:chunkZ" -> owner UUID */
    private static final Map<String, String> chunkToOwner = new ConcurrentHashMap<>();

    /** Map of owner UUID -> guild name (cached for guild claim sharing) */
    private static final Map<String, String> playerGuildCache = new ConcurrentHashMap<>();

    /** Map of owner UUID -> claim color RGB (cached from ClaimData) */
    private static final Map<String, int[]> playerColorCache = new ConcurrentHashMap<>();

    /** Map of owner UUID -> player display name (cached for marker labels) */
    private static final Map<String, String> playerNameCache = new ConcurrentHashMap<>();

    /** Register a chunk claim for an owner */
    public static void register(int chunkX, int chunkZ, String ownerUuid) {
        chunkToOwner.put(chunkX + ":" + chunkZ, ownerUuid);
    }

    /** Unregister a chunk claim */
    public static void unregister(int chunkX, int chunkZ) {
        chunkToOwner.remove(chunkX + ":" + chunkZ);
    }

    /** Get the owner UUID of a chunk, or null if unclaimed */
    public static String getOwner(int chunkX, int chunkZ) {
        return chunkToOwner.get(chunkX + ":" + chunkZ);
    }

    /** Check if a chunk is claimed by anyone */
    public static boolean isClaimed(int chunkX, int chunkZ) {
        return chunkToOwner.containsKey(chunkX + ":" + chunkZ);
    }

    /** Check if a chunk is owned by a specific player */
    public static boolean isOwnedBy(int chunkX, int chunkZ, String uuid) {
        String owner = getOwner(chunkX, chunkZ);
        return owner != null && owner.equals(uuid);
    }

    /** Get all chunks owned by a player as a set of "x:z" keys */
    public static Set<String> getChunksOwnedBy(String uuid) {
        Set<String> result = new HashSet<>();
        for (Map.Entry<String, String> entry : chunkToOwner.entrySet()) {
            if (entry.getValue().equals(uuid)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /** Get total number of claimed chunks across all players */
    public static int getTotalClaims() {
        return chunkToOwner.size();
    }

    /** Get all claims as a snapshot map of chunkKey -> ownerUuid */
    public static Map<String, String> getAllClaims() {
        return new java.util.HashMap<>(chunkToOwner);
    }

    /** Clear all claims (for server restart) */
    public static void clearAll() {
        chunkToOwner.clear();
    }

    // -- Guild cache for claim sharing --

    /** Update a player's guild in the cache */
    public static void setPlayerGuild(String uuid, String guildName) {
        if (guildName == null || guildName.isEmpty()) {
            playerGuildCache.remove(uuid);
        } else {
            playerGuildCache.put(uuid, guildName);
        }
    }

    /** Get a player's cached guild name */
    public static String getPlayerGuild(String uuid) {
        return playerGuildCache.get(uuid);
    }

    /** Check if two players are in the same guild */
    public static boolean areInSameGuild(String uuid1, String uuid2) {
        String guild1 = playerGuildCache.get(uuid1);
        String guild2 = playerGuildCache.get(uuid2);
        return guild1 != null && guild1.equals(guild2);
    }

    // -- Color & name cache for map markers --

    /** Set a player's claim overlay color */
    public static void setOwnerColor(String uuid, int[] rgb) {
        playerColorCache.put(uuid, rgb);
    }

    /** Get a player's claim overlay color (default blue) */
    public static int[] getOwnerColor(String uuid) {
        return playerColorCache.getOrDefault(uuid, new int[]{70, 130, 230});
    }

    /** Set a player's cached display name */
    public static void setOwnerName(String uuid, String name) {
        playerNameCache.put(uuid, name);
    }

    /** Get a player's cached display name */
    public static String getOwnerName(String uuid) {
        return playerNameCache.get(uuid);
    }
}
