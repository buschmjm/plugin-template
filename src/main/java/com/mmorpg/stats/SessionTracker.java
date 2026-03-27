package com.mmorpg.stats;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Tracks player session start times and daily login reward eligibility.
 * Daily reward state is persisted to disk so it survives server restarts.
 */
public final class SessionTracker {

    private static final Logger LOGGER = Logger.getLogger("MMORPGStats");
    private static final String DAILY_FILE = "mmorpg-daily-rewards.dat";

    /** Session start time (epoch millis) per player UUID */
    private static final ConcurrentHashMap<String, Long> sessionStart = new ConcurrentHashMap<>();

    /** Last daily reward date string ("YYYY-MM-DD") per player UUID - persisted */
    private static final ConcurrentHashMap<String, String> lastDailyReward = new ConcurrentHashMap<>();

    /** Daily login reward amount */
    public static final long DAILY_REWARD_GOLD = 50;

    private SessionTracker() {}

    /** Load persisted daily reward data from disk. Call on server start. */
    public static void loadDailyRewards() {
        Path path = Path.of(DAILY_FILE);
        if (!Files.exists(path)) return;
        try (InputStream in = Files.newInputStream(path)) {
            Properties props = new Properties();
            props.load(in);
            for (String key : props.stringPropertyNames()) {
                lastDailyReward.put(key, props.getProperty(key));
            }
            LOGGER.info("[MMORPG] Loaded daily rewards for " + lastDailyReward.size() + " players");
        } catch (IOException e) {
            LOGGER.warning("[MMORPG] Failed to load daily rewards: " + e.getMessage());
        }
    }

    /** Persist daily reward data to disk. */
    private static void saveDailyRewards() {
        try (OutputStream out = Files.newOutputStream(Path.of(DAILY_FILE),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            Properties props = new Properties();
            lastDailyReward.forEach(props::setProperty);
            props.store(out, "MMORPG Daily Login Rewards");
        } catch (IOException e) {
            LOGGER.warning("[MMORPG] Failed to save daily rewards: " + e.getMessage());
        }
    }

    /** Record a player joining. */
    public static void recordLogin(String uuid) {
        sessionStart.put(uuid, System.currentTimeMillis());
    }

    /** Remove a player's session on disconnect. */
    public static void recordLogout(String uuid) {
        sessionStart.remove(uuid);
    }

    /** Get formatted playtime for a player, e.g. "1h 23m" or "45m". */
    public static String getPlaytimeFormatted(String uuid) {
        Long start = sessionStart.get(uuid);
        if (start == null) return "";
        long elapsed = System.currentTimeMillis() - start;
        long totalMinutes = elapsed / 60_000;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }

    /**
     * Check if a player is eligible for the daily login reward today.
     * Returns true and marks as claimed if eligible. Persists to disk.
     */
    public static boolean claimDailyReward(String uuid) {
        String today = java.time.LocalDate.now().toString();
        String last = lastDailyReward.get(uuid);
        if (today.equals(last)) return false;
        lastDailyReward.put(uuid, today);
        saveDailyRewards();
        return true;
    }
}
