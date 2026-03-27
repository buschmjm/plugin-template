package com.mmorpg.stats;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

/**
 * Player command /allocate &lt;stat&gt; [amount] - spend stat points via command line.
 *   /allocate str 5   - add 5 points to strength
 *   /allocate dex     - add 1 point to dexterity (default)
 *   /allocate info    - show current stat summary
 */
public class AllocateCommand extends AbstractAsyncCommand {

    // -- Subcommand: /allocate info --
    private static class InfoSubcommand extends AbstractAsyncCommand {
        InfoSubcommand() { super("info", "Show stat point summary"); }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);

            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();

            return CompletableFuture.runAsync(() -> {
                var statType = StatsPlugin.getInstance().getPlayerStatDataType();
                PlayerStatData stats = store.ensureAndGetComponent(ref, statType);

                ctx.sendMessage(Message.raw("=== Stat Points (Level " + stats.getLevel() + ") ==="));
                ctx.sendMessage(Message.raw("Available: " + stats.getAvailablePoints()
                        + " / " + stats.getTotalPoints()));
                ctx.sendMessage(Message.raw("  STR: " + stats.getStrength()
                        + "  DEX: " + stats.getDexterity()
                        + "  VIT: " + stats.getVitality()));
                ctx.sendMessage(Message.raw("  INT: " + stats.getIntellect()
                        + "  PRE: " + stats.getPresence()
                        + "  ARC: " + stats.getArcana()));
                ctx.sendMessage(Message.raw("Usage: /allocate <str|dex|vit|int|pre|arc> [amount]"));
                ctx.sendMessage(Message.raw("================================"));
            }, world);
        }
    }

    private final RequiredArg<String> statArg;
    private final DefaultArg<Integer> amountArg;

    public AllocateCommand() {
        super("allocate", "Spend stat points on a stat");
        this.statArg = withRequiredArg("stat", "Stat name: str, dex, vit, int, pre, arc", ArgTypes.STRING);
        this.amountArg = withDefaultArg("amount", "Points to add", ArgTypes.INTEGER, 1, "1");
        addSubCommand(new InfoSubcommand());
    }

    @Override
    protected boolean canGeneratePermission() { return false; }

    @Nonnull
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
        Ref<EntityStore> ref = ctx.senderAsPlayerRef();
        if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();
        String statName = ctx.get(statArg).toLowerCase();
        int amount = ctx.get(amountArg);

        return CompletableFuture.runAsync(() -> {
            if (amount <= 0) {
                ctx.sendMessage(Message.raw("Amount must be positive."));
                return;
            }

            var statType = StatsPlugin.getInstance().getPlayerStatDataType();
            PlayerStatData stats = store.ensureAndGetComponent(ref, statType);

            int available = stats.getAvailablePoints();
            if (available <= 0) {
                ctx.sendMessage(Message.raw("No stat points available! Level up to earn more."));
                return;
            }

            int toSpend = Math.min(amount, available);

            int oldValue;
            String displayName;

            switch (statName) {
                case "str", "strength" -> {
                    displayName = "STR";
                    oldValue = stats.getStrength();
                    toSpend = Math.min(toSpend, StatConstants.MAX_STAT_POINTS - oldValue);
                    if (toSpend <= 0) { ctx.sendMessage(Message.raw("STR is at max (" + StatConstants.MAX_STAT_POINTS + ")!")); return; }
                    stats.setStrength(oldValue + toSpend);
                }
                case "dex", "dexterity" -> {
                    displayName = "DEX";
                    oldValue = stats.getDexterity();
                    toSpend = Math.min(toSpend, StatConstants.MAX_STAT_POINTS - oldValue);
                    if (toSpend <= 0) { ctx.sendMessage(Message.raw("DEX is at max (" + StatConstants.MAX_STAT_POINTS + ")!")); return; }
                    stats.setDexterity(oldValue + toSpend);
                }
                case "vit", "vitality" -> {
                    displayName = "VIT";
                    oldValue = stats.getVitality();
                    toSpend = Math.min(toSpend, StatConstants.MAX_STAT_POINTS - oldValue);
                    if (toSpend <= 0) { ctx.sendMessage(Message.raw("VIT is at max (" + StatConstants.MAX_STAT_POINTS + ")!")); return; }
                    stats.setVitality(oldValue + toSpend);
                }
                case "int", "intellect" -> {
                    displayName = "INT";
                    oldValue = stats.getIntellect();
                    toSpend = Math.min(toSpend, StatConstants.MAX_STAT_POINTS - oldValue);
                    if (toSpend <= 0) { ctx.sendMessage(Message.raw("INT is at max (" + StatConstants.MAX_STAT_POINTS + ")!")); return; }
                    stats.setIntellect(oldValue + toSpend);
                }
                case "pre", "presence" -> {
                    displayName = "PRE";
                    oldValue = stats.getPresence();
                    toSpend = Math.min(toSpend, StatConstants.MAX_STAT_POINTS - oldValue);
                    if (toSpend <= 0) { ctx.sendMessage(Message.raw("PRE is at max (" + StatConstants.MAX_STAT_POINTS + ")!")); return; }
                    stats.setPresence(oldValue + toSpend);
                }
                case "arc", "arcana" -> {
                    displayName = "ARC";
                    oldValue = stats.getArcana();
                    toSpend = Math.min(toSpend, StatConstants.MAX_STAT_POINTS - oldValue);
                    if (toSpend <= 0) { ctx.sendMessage(Message.raw("ARC is at max (" + StatConstants.MAX_STAT_POINTS + ")!")); return; }
                    stats.setArcana(oldValue + toSpend);
                }
                default -> {
                    ctx.sendMessage(Message.raw("Unknown stat: " + statName
                            + ". Use: str, dex, vit, int, pre, arc"));
                    return;
                }
            }

            // Apply stat effects
            StatEffectApplier.applyStats(ref, store, stats);

            // Persist stat changes
            store.putComponent(ref, statType, stats);

            // Evaluate class titles
            ClassEvaluator.evaluate(ref, store, stats);

            // Check for first class unlock (free offhand item)
            StatsPlugin.checkFirstClassUnlock(ref, store, stats);

            // Quest tracking
            for (int i = 0; i < toSpend; i++) {
                QuestTracker.record(ref, store, QuestObjective.SPEND_STAT_POINTS);
            }

            if (toSpend < amount) {
                ctx.sendMessage(Message.raw("Only " + toSpend + " points available (requested " + amount + ")."));
            }

            ctx.sendMessage(Message.raw("* " + displayName + ": " + oldValue + " -> "
                    + (oldValue + toSpend) + " (+" + toSpend + ")"));
            ctx.sendMessage(Message.raw("Remaining points: " + stats.getAvailablePoints()
                    + " / " + stats.getTotalPoints()));
        }, world);
    }
}
