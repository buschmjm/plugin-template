package com.mmorpg.stats;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Logger;

/**
 * Logs gold transactions to a rotating file.
 * Keeps only the last 14 days of entries.
 * Thread-safe - writes are buffered and flushed periodically.
 */
public final class GoldLedger {

    private static final Logger LOGGER = Logger.getLogger("MMORPGStats");
    private static final String LOG_FILE = "mmorpg-gold-ledger.log";
    private static final int RETENTION_DAYS = 14;
    private static final ConcurrentLinkedDeque<String> buffer = new ConcurrentLinkedDeque<>();
    private static volatile long lastFlush = 0;
    private static final long FLUSH_INTERVAL_MS = 30_000; // flush every 30 seconds

    private GoldLedger() {}

    /**
     * Log a gold transaction.
     * @param playerName  player involved
     * @param type        transaction type (KILL, QUEST, TRADE_SEND, TRADE_RECV, SHOP_BUY, SHOP_PURCHASE, ADMIN, GUILD_DONATE, GUILD_CREATE, DAILY_LOGIN, etc.)
     * @param amount      gold amount (positive = gained, negative = spent)
     * @param balance     new balance after transaction
     * @param detail      extra info (item name, target player, etc.)
     */
    public static void log(String playerName, String type, long amount, long balance, String detail) {
        String entry = Instant.now().toString()
                + "|" + playerName
                + "|" + type
                + "|" + (amount >= 0 ? "+" : "") + amount
                + "|bal:" + balance
                + "|" + detail;
        buffer.add(entry);

        long now = System.currentTimeMillis();
        if (now - lastFlush > FLUSH_INTERVAL_MS) {
            flush();
        }
    }

    /** Flush buffered entries to disk and prune old entries. */
    public static synchronized void flush() {
        lastFlush = System.currentTimeMillis();
        if (buffer.isEmpty()) return;

        try {
            Path path = Path.of(LOG_FILE);

            // Append buffered entries
            try (BufferedWriter writer = Files.newBufferedWriter(path,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                String entry;
                while ((entry = buffer.poll()) != null) {
                    writer.write(entry);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            LOGGER.warning("[MMORPG] Failed to write gold ledger: " + e.getMessage());
        }
    }

    /** Prune entries older than RETENTION_DAYS. Call periodically (e.g. on server start). */
    public static void prune() {
        Path path = Path.of(LOG_FILE);
        if (!Files.exists(path)) return;

        try {
            Instant cutoff = Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS);
            java.util.List<String> lines = Files.readAllLines(path);
            java.util.List<String> kept = new java.util.ArrayList<>();

            for (String line : lines) {
                int pipe = line.indexOf('|');
                if (pipe > 0) {
                    try {
                        Instant ts = Instant.parse(line.substring(0, pipe));
                        if (ts.isAfter(cutoff)) {
                            kept.add(line);
                        }
                    } catch (Exception e) {
                        // Malformed line - skip
                    }
                }
            }

            Files.write(path, kept, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            LOGGER.info("[MMORPG] Gold ledger pruned: kept " + kept.size()
                    + " entries, removed " + (lines.size() - kept.size()));
        } catch (IOException e) {
            LOGGER.warning("[MMORPG] Failed to prune gold ledger: " + e.getMessage());
        }
    }

    /**
     * Get recent transactions for a specific player (from disk + buffer).
     * Returns up to maxEntries entries, newest first.
     */
    public static java.util.List<String> getRecentTransactions(String playerName, int maxEntries) {
        java.util.List<String> results = new java.util.ArrayList<>();

        // Read from disk
        Path path = Path.of(LOG_FILE);
        if (Files.exists(path)) {
            try {
                java.util.List<String> lines = Files.readAllLines(path);
                for (int i = lines.size() - 1; i >= 0 && results.size() < maxEntries; i--) {
                    String line = lines.get(i);
                    // Format: timestamp|playerName|TYPE|amount|bal:N|detail
                    String[] parts = line.split("\\|", 3);
                    if (parts.length >= 2 && parts[1].equalsIgnoreCase(playerName)) {
                        results.add(line);
                    }
                }
            } catch (IOException e) {
                LOGGER.warning("[MMORPG] Failed to read gold ledger: " + e.getMessage());
            }
        }

        // Also check unflushed buffer entries
        for (String entry : buffer) {
            String[] parts = entry.split("\\|", 3);
            if (parts.length >= 2 && parts[1].equalsIgnoreCase(playerName)) {
                results.addFirst(entry);
                if (results.size() > maxEntries) {
                    results.removeLast();
                }
            }
        }

        return results;
    }
}
