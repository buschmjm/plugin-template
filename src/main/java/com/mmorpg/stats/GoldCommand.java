package com.mmorpg.stats;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

/**
 * Player command /gold - shows current gold balance.
 */
public class GoldCommand extends AbstractAsyncCommand {

    public GoldCommand() {
        super("gold", "Check your gold balance");
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
            var goldType = StatsPlugin.getInstance().getPlayerGoldDataType();
            PlayerGoldData data = store.ensureAndGetComponent(ref, goldType);
            ctx.sendMessage(Message.raw("=== Gold ==="));
            ctx.sendMessage(Message.raw("Balance: " + data.getGold() + " gold"));
            ctx.sendMessage(Message.raw("Earn gold by killing monsters and completing quests."));
            ctx.sendMessage(Message.raw("============="));
        }, world);
    }
}
