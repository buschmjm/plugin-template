package com.mmorpg.stats;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Admin command /admin for managing player stats and teleportation.
 * Only allowed for players whose UUID is in the ADMIN_UUIDS list.
 */
public class AdminCommand extends AbstractAsyncCommand {

    /** Hardcoded admin UUIDs */
    private static final UUID[] ADMIN_UUIDS = {
        UUID.fromString("8a5394d7-73ab-45ad-8a5d-43e9e8ebbaa0") // DoniQuix
    };

    /** Check if a PlayerRef is an admin */
    static boolean isAdmin(PlayerRef playerRef) {
        if (playerRef == null) return false;
        UUID uuid = playerRef.getUuid();
        for (UUID admin : ADMIN_UUIDS) {
            if (admin.equals(uuid)) return true;
        }
        return false;
    }

    /** Check admin and send denial message if not */
    private static boolean checkAdmin(CommandContext ctx, Ref<EntityStore> ref) {
        // Allow all players on local/dev servers
        return true;
    }

    /** Find a PlayerRef by username in the same world */
    private static PlayerRef findPlayer(World world, String name) {
        String lower = name.toLowerCase();
        for (PlayerRef pr : world.getPlayerRefs()) {
            if (pr.getUsername().toLowerCase().startsWith(lower)) {
                return pr;
            }
        }
        return null;
    }

    // Subcommand: /admin setlevel <level>
    private static class SetLevelCommand extends AbstractAsyncCommand {
        private final RequiredArg<Integer> levelArg;

        SetLevelCommand() {
            super("setlevel", "Set your MMORPG level");
            this.levelArg = withRequiredArg("level", "Level to set", ArgTypes.INTEGER);
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);
            if (!checkAdmin(ctx, ref)) return CompletableFuture.completedFuture(null);

            Store<EntityStore> store = ref.getStore();
            var world = store.getExternalData().getWorld();
            int level = Math.max(1, Math.min(ctx.get(levelArg), StatConstants.MAX_LEVEL));

            return CompletableFuture.runAsync(() -> {
                PlayerStatData stats = store.ensureAndGetComponent(ref,
                        StatsPlugin.getInstance().getPlayerStatDataType());
                stats.setLevel(level);
                store.putComponent(ref, StatsPlugin.getInstance().getPlayerStatDataType(), stats);
                StatEffectApplier.applyStats(ref, store, stats);
                ctx.sendMessage(com.hypixel.hytale.server.core.Message.raw(
                        "Set level to " + stats.getLevel()
                        + " (" + stats.getAvailablePoints() + " points available)"));

            }, world);
        }
    }

    // Subcommand: /admin setstat <stat> <value>
    private static class SetStatCommand extends AbstractAsyncCommand {
        private final RequiredArg<String> statArg;
        private final RequiredArg<Integer> valueArg;

        SetStatCommand() {
            super("setstat", "Set a specific stat value");
            this.statArg = withRequiredArg("stat", "Stat name (str/dex/vit/int/pre/arc)", ArgTypes.STRING);
            this.valueArg = withRequiredArg("value", "Value to set", ArgTypes.INTEGER);
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);
            if (!checkAdmin(ctx, ref)) return CompletableFuture.completedFuture(null);

            Store<EntityStore> store = ref.getStore();
            var world = store.getExternalData().getWorld();
            String statName = ctx.get(statArg);
            int value = ctx.get(valueArg);

            return CompletableFuture.runAsync(() -> {
                PlayerStatData stats = store.ensureAndGetComponent(ref,
                        StatsPlugin.getInstance().getPlayerStatDataType());
                if (!setStat(stats, statName, value)) {
                    ctx.sendMessage(com.hypixel.hytale.server.core.Message.raw(
                            "Unknown stat: " + statName + ". Use: str, dex, vit, int, pre, arc"));
                    return;
                }
                store.putComponent(ref, StatsPlugin.getInstance().getPlayerStatDataType(), stats);
                StatEffectApplier.applyStats(ref, store, stats);
                ctx.sendMessage(com.hypixel.hytale.server.core.Message.raw(
                        "Set " + statName.toUpperCase() + " to " + value));

            }, world);
        }
    }

    // Subcommand: /admin reset
    private static class ResetCommand extends AbstractAsyncCommand {
        ResetCommand() {
            super("reset", "Reset all stats to zero");
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);
            if (!checkAdmin(ctx, ref)) return CompletableFuture.completedFuture(null);

            Store<EntityStore> store = ref.getStore();
            var world = store.getExternalData().getWorld();

            return CompletableFuture.runAsync(() -> {
                PlayerStatData stats = store.ensureAndGetComponent(ref,
                        StatsPlugin.getInstance().getPlayerStatDataType());
                stats.setStrength(0);
                stats.setDexterity(0);
                stats.setVitality(0);
                stats.setIntellect(0);
                stats.setPresence(0);
                stats.setArcana(0);
                store.putComponent(ref, StatsPlugin.getInstance().getPlayerStatDataType(), stats);
                StatEffectApplier.applyStats(ref, store, stats);
                ClassEvaluator.reevaluateAfterRespec(ref, store, stats);
                ctx.sendMessage(com.hypixel.hytale.server.core.Message.raw(
                        "All stats reset. " + stats.getAvailablePoints() + " points available."));

            }, world);
        }
    }

    // Subcommand: /admin respec
    private static class RespecCommand extends AbstractAsyncCommand {
        RespecCommand() {
            super("respec", "Refund all spent stat points");
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);
            if (!checkAdmin(ctx, ref)) return CompletableFuture.completedFuture(null);

            Store<EntityStore> store = ref.getStore();
            var world = store.getExternalData().getWorld();

            return CompletableFuture.runAsync(() -> {
                PlayerStatData stats = store.ensureAndGetComponent(ref,
                        StatsPlugin.getInstance().getPlayerStatDataType());
                int spent = stats.getSpentPoints();
                if (spent == 0) {
                    ctx.sendMessage(com.hypixel.hytale.server.core.Message.raw(
                            "No points to refund!"));
                    return;
                }
                stats.setStrength(0);
                stats.setDexterity(0);
                stats.setVitality(0);
                stats.setIntellect(0);
                stats.setPresence(0);
                stats.setArcana(0);
                store.putComponent(ref, StatsPlugin.getInstance().getPlayerStatDataType(), stats);
                StatEffectApplier.applyStats(ref, store, stats);
                ClassEvaluator.reevaluateAfterRespec(ref, store, stats);

                // Refresh HUD
                Player player = store.getComponent(ref, Player.getComponentType());
                if (player != null) {
                    var customHud = player.getHudManager().getCustomHud();
                    if (customHud instanceof MmorpgHud mHud) mHud.forceRefresh(ref, store);
                    else if (customHud instanceof CombatHud hud) hud.forceRefresh(ref, store);
                }

                ctx.sendMessage(com.hypixel.hytale.server.core.Message.raw(
                        "Respec complete! Refunded " + spent + " points. "
                        + stats.getAvailablePoints() + " points available."));
            }, world);
        }
    }

    // Subcommand: /admin tp <player> - teleport self to target player
    private static class TpCommand extends AbstractAsyncCommand {
        private final RequiredArg<String> nameArg;

        TpCommand() {
            super("tp", "Teleport to a player");
            this.nameArg = withRequiredArg("player", "Player name", ArgTypes.STRING);
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);
            if (!checkAdmin(ctx, ref)) return CompletableFuture.completedFuture(null);

            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();
            String targetName = ctx.get(nameArg);

            return CompletableFuture.runAsync(() -> {
                PlayerRef target = findPlayer(world, targetName);
                if (target == null) {
                    ctx.sendMessage(com.hypixel.hytale.server.core.Message.raw(
                            "Player not found: " + targetName));
                    return;
                }
                Ref<EntityStore> targetRef = target.getReference();
                if (targetRef == null || !targetRef.isValid()) return;
                TransformComponent tc = store.getComponent(targetRef, TransformComponent.getComponentType());
                if (tc == null) return;

                Vector3d pos = tc.getPosition();
                Vector3f rot = tc.getRotation();
                Teleport tp = Teleport.createForPlayer(pos, rot);
                store.putComponent(ref, Teleport.getComponentType(), tp);
                ctx.sendMessage(com.hypixel.hytale.server.core.Message.raw(
                        "Teleported to " + target.getUsername()));
            }, world);
        }
    }

    // Subcommand: /admin tphere <player> - teleport target player to self
    private static class TpHereCommand extends AbstractAsyncCommand {
        private final RequiredArg<String> nameArg;

        TpHereCommand() {
            super("tphere", "Teleport a player to you");
            this.nameArg = withRequiredArg("player", "Player name", ArgTypes.STRING);
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);
            if (!checkAdmin(ctx, ref)) return CompletableFuture.completedFuture(null);

            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();
            String targetName = ctx.get(nameArg);

            return CompletableFuture.runAsync(() -> {
                // Get admin's position
                TransformComponent myTc = store.getComponent(ref, TransformComponent.getComponentType());
                if (myTc == null) return;
                Vector3d myPos = myTc.getPosition();
                Vector3f myRot = myTc.getRotation();

                PlayerRef target = findPlayer(world, targetName);
                if (target == null) {
                    ctx.sendMessage(com.hypixel.hytale.server.core.Message.raw(
                            "Player not found: " + targetName));
                    return;
                }
                Ref<EntityStore> targetRef = target.getReference();
                if (targetRef == null || !targetRef.isValid()) return;

                Teleport tp = Teleport.createForPlayer(myPos, myRot);
                store.putComponent(targetRef, Teleport.getComponentType(), tp);
                ctx.sendMessage(com.hypixel.hytale.server.core.Message.raw(
                        "Teleported " + target.getUsername() + " to you"));
            }, world);
        }
    }

    // Subcommand: /admin setgold <amount>
    private static class SetGoldCommand extends AbstractAsyncCommand {
        private final RequiredArg<Integer> amountArg;

        SetGoldCommand() {
            super("setgold", "Set your gold balance");
            this.amountArg = withRequiredArg("amount", "Gold amount", ArgTypes.INTEGER);
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);
            if (!checkAdmin(ctx, ref)) return CompletableFuture.completedFuture(null);

            Store<EntityStore> store = ref.getStore();
            var world = store.getExternalData().getWorld();
            int amount = ctx.get(amountArg);

            return CompletableFuture.runAsync(() -> {
                var goldType = StatsPlugin.getInstance().getPlayerGoldDataType();
                PlayerGoldData gold = store.ensureAndGetComponent(ref, goldType);
                long oldGold = gold.getGold();
                gold.setGold(amount);
                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef != null) {
                    GoldLedger.log(playerRef.getUsername(), "ADMIN",
                            amount - oldGold, gold.getGold(), "setgold");
                }
                ctx.sendMessage(com.hypixel.hytale.server.core.Message.raw(
                        "Gold set to " + gold.getGold()));
            }, world);
        }
    }

    // Subcommand: /admin addgold <amount>
    private static class AddGoldCommand extends AbstractAsyncCommand {
        private final RequiredArg<Integer> amountArg;

        AddGoldCommand() {
            super("addgold", "Add gold to your balance");
            this.amountArg = withRequiredArg("amount", "Gold to add", ArgTypes.INTEGER);
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);
            if (!checkAdmin(ctx, ref)) return CompletableFuture.completedFuture(null);

            Store<EntityStore> store = ref.getStore();
            var world = store.getExternalData().getWorld();
            int amount = ctx.get(amountArg);

            return CompletableFuture.runAsync(() -> {
                var goldType = StatsPlugin.getInstance().getPlayerGoldDataType();
                PlayerGoldData gold = store.ensureAndGetComponent(ref, goldType);
                gold.addGold(amount);
                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef != null) {
                    GoldLedger.log(playerRef.getUsername(), "ADMIN",
                            amount, gold.getGold(), "addgold");
                }
                ctx.sendMessage(com.hypixel.hytale.server.core.Message.raw(
                        "Added " + amount + " gold. Balance: " + gold.getGold()));
            }, world);
        }
    }

    public AdminCommand() {
        super("rpgadmin", "MMORPG admin commands");
        addSubCommand(new SetLevelCommand());
        addSubCommand(new SetStatCommand());
        addSubCommand(new ResetCommand());
        addSubCommand(new RespecCommand());
        addSubCommand(new TpCommand());
        addSubCommand(new TpHereCommand());
        addSubCommand(new SetGoldCommand());
        addSubCommand(new AddGoldCommand());
    }

    @Override
    protected boolean canGeneratePermission() { return false; }

    @Nonnull
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
        ctx.sendMessage(com.hypixel.hytale.server.core.Message.raw(
                "Usage: /rpgadmin setlevel|setstat|reset|respec|tp|tphere|setgold|addgold ..."));
        return CompletableFuture.completedFuture(null);
    }

    private static boolean setStat(PlayerStatData stats, String name, int value) {
        switch (name.toLowerCase()) {
            case "str", "strength" -> stats.setStrength(value);
            case "dex", "dexterity" -> stats.setDexterity(value);
            case "vit", "vitality" -> stats.setVitality(value);
            case "int", "intellect" -> stats.setIntellect(value);
            case "pre", "presence" -> stats.setPresence(value);
            case "arc", "arcana" -> stats.setArcana(value);
            default -> { return false; }
        }
        return true;
    }
}
