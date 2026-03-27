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
import java.util.concurrent.CompletableFuture;

/**
 * Player command /online - shows all online players with class, guild, level, and time online.
 */
public class OnlineCommand extends AbstractAsyncCommand {

    public OnlineCommand() {
        super("online", "Show all players currently online");
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

        return CompletableFuture.runAsync(() -> {
            var players = world.getPlayerRefs();
            ctx.sendMessage(Message.raw("=== Online Players (" + players.size() + ") ==="));

            for (PlayerRef pr : players) {
                Ref<EntityStore> pRef = pr.getReference();
                if (pRef == null || !pRef.isValid()) continue;

                String name = pr.getUsername();

                // Level
                String levelStr = "";
                var statType = StatsPlugin.getInstance().getPlayerStatDataType();
                PlayerStatData stats = store.getComponent(pRef, statType);
                if (stats != null) {
                    levelStr = "Lv" + stats.getLevel();
                }

                // Class
                String classStr = "";
                var classType = StatsPlugin.getInstance().getPlayerClassDataType();
                PlayerClassData classData = store.getComponent(pRef, classType);
                if (classData != null) {
                    classStr = classData.getDisplayedTitleName();
                }

                // Guild
                String guildStr = "";
                var guildType = StatsPlugin.getInstance().getGuildDataType();
                GuildData guildData = store.getComponent(pRef, guildType);
                if (guildData != null && guildData.isInGuild()) {
                    guildStr = "<" + guildData.getGuildName() + ">";
                }

                // Time online
                String timeStr = SessionTracker.getPlaytimeFormatted(pr.getUuid().toString());

                StringBuilder line = new StringBuilder("  ");
                line.append(name);
                if (!levelStr.isEmpty()) line.append(" - ").append(levelStr);
                if (!classStr.isEmpty()) line.append(" ").append(classStr);
                if (!guildStr.isEmpty()) line.append(" ").append(guildStr);
                if (!timeStr.isEmpty()) line.append(" (").append(timeStr).append(")");

                ctx.sendMessage(Message.raw(line.toString()));
            }

            ctx.sendMessage(Message.raw("==========================="));
        }, world);
    }
}
