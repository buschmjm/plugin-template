package com.mmorpg.stats;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

/**
 * Player command to view and manage class titles.
 * /class list      - list earned titles
 * /class info <id> - show class details
 * /class select <titleId> - select displayed title
 * /class auto [on|off] - toggle auto-update
 */
public class ClassCommand extends AbstractAsyncCommand {

    // /class list
    private static class ListCommand extends AbstractAsyncCommand {
        ListCommand() { super("list", "List your earned class titles"); }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);
            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();

            return CompletableFuture.runAsync(() -> {
                ComponentType<EntityStore, PlayerClassData> classType =
                        StatsPlugin.getInstance().getPlayerClassDataType();
                PlayerClassData data = store.getComponent(ref, classType);
                if (data == null) return;

                ctx.sendMessage(Message.raw("=== Class Titles ==="));
                ctx.sendMessage(Message.raw("Displayed: " + data.getDisplayedTitleName()
                        + " (auto: " + (data.isAutoUpdate() ? "on" : "off") + ")"));

                if (data.getEarnedTitles().isEmpty()) {
                    ctx.sendMessage(Message.raw("No class titles earned yet. Reach level "
                            + StatConstants.CLASS_TITLE_MIN_LEVEL + " and allocate stats."));
                } else {
                    for (String titleId : data.getEarnedTitles()) {
                        String name = ClassEvaluator.getTitleDisplayName(titleId);
                        String marker = titleId.equals(data.getDisplayedTitle()) ? " [ACTIVE]" : "";
                        ctx.sendMessage(Message.raw("  " + name + " (" + titleId + ")" + marker));
                    }
                }
            }, world);
        }
    }

    // /class info <classId>
    private static class InfoCommand extends AbstractAsyncCommand {
        private final RequiredArg<String> classArg;
        InfoCommand() {
            super("info", "Show class details");
            this.classArg = withRequiredArg("class", "Class ID (e.g., knight, duelist)", ArgTypes.STRING);
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            String classId = ctx.get(classArg).toLowerCase();
            ClassDefinition cls = ClassRegistry.get(classId);
            if (cls == null) {
                ctx.sendMessage(Message.raw("Unknown class: " + classId));
                return CompletableFuture.completedFuture(null);
            }

            ctx.sendMessage(Message.raw("=== " + cls.getName() + " ==="));
            ctx.sendMessage(Message.raw("Category: " + cls.getCategory()
                    + " | Stats: " + String.join("+", cls.getPrimaryStats())));

            for (ClassTier tier : cls.getTiers()) {
                String thresh = cls.isDualStat()
                        ? tier.threshold() + " each"
                        : tier.threshold() + " pts";
                ctx.sendMessage(Message.raw("  T" + tier.tier() + " " + tier.titleName()
                        + " (" + thresh + ") - " + tier.passiveDesc()));
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    // /class select <titleId>
    private static class SelectCommand extends AbstractAsyncCommand {
        private final RequiredArg<String> titleArg;
        SelectCommand() {
            super("select", "Select displayed class title");
            this.titleArg = withRequiredArg("title", "Title ID (e.g., knight_3, brawler_5)", ArgTypes.STRING);
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);
            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();
            String titleId = ctx.get(titleArg).toLowerCase();

            return CompletableFuture.runAsync(() -> {
                ComponentType<EntityStore, PlayerClassData> classType =
                        StatsPlugin.getInstance().getPlayerClassDataType();
                PlayerClassData data = store.getComponent(ref, classType);
                if (data == null) return;

                if (!data.hasTitle(titleId)) {
                    ctx.sendMessage(Message.raw("You haven't earned that title. Use /class list to see your titles."));
                    return;
                }

                data.setDisplayedTitle(titleId);
                data.setAutoUpdate(false);
                data.recomputePassives();
                store.putComponent(ref, classType, data);

                PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (pRef != null) StatsPlugin.getInstance().updatePlayerNametag(ref, store, pRef);

                String name = ClassEvaluator.getTitleDisplayName(titleId);
                ctx.sendMessage(Message.raw("Now displaying: " + name + " (auto-update disabled)"));
            }, world);
        }
    }

    // /class auto <on|off>
    private static class AutoCommand extends AbstractAsyncCommand {
        private final RequiredArg<String> stateArg;
        AutoCommand() {
            super("auto", "Toggle auto-update for displayed title");
            this.stateArg = withRequiredArg("state", "on or off", ArgTypes.STRING);
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);
            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();
            String state = ctx.get(stateArg).toLowerCase();

            return CompletableFuture.runAsync(() -> {
                ComponentType<EntityStore, PlayerClassData> classType =
                        StatsPlugin.getInstance().getPlayerClassDataType();
                PlayerClassData data = store.getComponent(ref, classType);
                if (data == null) return;

                boolean on = state.equals("on") || state.equals("true") || state.equals("1");
                data.setAutoUpdate(on);
                if (on) ClassEvaluator.autoSelectTitle(data);
                data.recomputePassives();
                store.putComponent(ref, classType, data);

                PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (pRef != null) StatsPlugin.getInstance().updatePlayerNametag(ref, store, pRef);

                if (on) {
                    ctx.sendMessage(Message.raw("Auto-update enabled. Displaying: "
                            + data.getDisplayedTitleName()));
                } else {
                    ctx.sendMessage(Message.raw("Auto-update disabled."));
                }
            }, world);
        }
    }

    public ClassCommand() {
        super("class", "Class title management");
        addSubCommand(new ListCommand());
        addSubCommand(new InfoCommand());
        addSubCommand(new SelectCommand());
        addSubCommand(new AutoCommand());
    }

    @Override
    protected boolean canGeneratePermission() { return false; }

    @Nonnull @Override
    protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
        ctx.sendMessage(Message.raw("Usage: /class list | info <id> | select <titleId> | auto <on|off>"));
        return CompletableFuture.completedFuture(null);
    }
}
