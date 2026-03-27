package com.mmorpg.stats;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Player command /lb - view the server leaderboard.
 *   /lb         - shows level leaderboard (default)
 *   /lb level   - top players by level
 *   /lb gold    - top players by gold
 *   /lb kills   - top players by kill count
 */
public class LeaderboardCommand extends AbstractAsyncCommand {

    private static final int MAX_ENTRIES = 10;

    private record LeaderboardEntry(String name, long value) {}

    // -- Subcommand: /lb level --
    private static class LevelSubcommand extends AbstractAsyncCommand {
        LevelSubcommand() { super("level", "Top players by level"); }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            return showLeaderboard(ctx, "Level", LeaderboardCategory.LEVEL);
        }
    }

    // -- Subcommand: /lb gold --
    private static class GoldSubcommand extends AbstractAsyncCommand {
        GoldSubcommand() { super("gold", "Top players by gold"); }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            return showLeaderboard(ctx, "Gold", LeaderboardCategory.GOLD);
        }
    }

    // -- Subcommand: /lb kills --
    private static class KillsSubcommand extends AbstractAsyncCommand {
        KillsSubcommand() { super("kills", "Top players by kills"); }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            return showLeaderboard(ctx, "Kills", LeaderboardCategory.KILLS);
        }
    }

    private enum LeaderboardCategory { LEVEL, GOLD, KILLS }

    public LeaderboardCommand() {
        super("lb", "View the server leaderboard");
        addSubCommand(new LevelSubcommand());
        addSubCommand(new GoldSubcommand());
        addSubCommand(new KillsSubcommand());
    }

    @Override
    protected boolean canGeneratePermission() { return false; }

    @Nonnull
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
        // Default: show level leaderboard
        return showLeaderboard(ctx, "Level", LeaderboardCategory.LEVEL);
    }

    private static CompletableFuture<Void> showLeaderboard(CommandContext ctx, String title,
                                                            LeaderboardCategory category) {
        Ref<EntityStore> ref = ctx.senderAsPlayerRef();
        if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        return CompletableFuture.runAsync(() -> {
            var statType = StatsPlugin.getInstance().getPlayerStatDataType();
            var goldType = StatsPlugin.getInstance().getPlayerGoldDataType();

            List<LeaderboardEntry> entries = new ArrayList<>();

            for (PlayerRef pr : world.getPlayerRefs()) {
                Ref<EntityStore> playerRef = pr.getReference();
                if (playerRef == null || !playerRef.isValid()) continue;

                String name = pr.getUsername();
                long value = 0;

                switch (category) {
                    case LEVEL -> {
                        PlayerStatData stats = store.getComponent(playerRef, statType);
                        if (stats != null) value = stats.getLevel();
                    }
                    case GOLD -> {
                        PlayerGoldData gold = store.getComponent(playerRef, goldType);
                        if (gold != null) value = gold.getGold();
                    }
                    case KILLS -> {
                        PlayerStatData stats = store.getComponent(playerRef, statType);
                        if (stats != null) value = stats.getKills();
                    }
                }

                entries.add(new LeaderboardEntry(name, value));
            }

            // Sort descending by value
            entries.sort(Comparator.comparingLong(LeaderboardEntry::value).reversed());

            ctx.sendMessage(Message.raw("=== Leaderboard: " + title + " ==="));

            if (entries.isEmpty()) {
                ctx.sendMessage(Message.raw("No players online."));
            } else {
                int count = Math.min(entries.size(), MAX_ENTRIES);
                for (int i = 0; i < count; i++) {
                    LeaderboardEntry e = entries.get(i);
                    String medal = switch (i) {
                        case 0 -> "*";
                        case 1 -> "*";
                        case 2 -> "◆";
                        default -> (i + 1) + ".";
                    };
                    ctx.sendMessage(Message.raw(medal + " " + e.name() + " - " + e.value()));
                }
            }

            ctx.sendMessage(Message.raw("Use /lb level, /lb gold, or /lb kills"));
            ctx.sendMessage(Message.raw("==========================="));
        }, world);
    }
}
