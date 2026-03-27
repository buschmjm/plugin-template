package com.mmorpg.stats;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.vector.Vector3d;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Command /spawnshop - marks the shopkeeper NPC location and broadcasts it.
 * Uses the kweebec_-_razor_leaf_ranger.glb model asset (for future NPC entity).
 * Available to all players (canGeneratePermission -> false).
 */
public class SpawnShopCommand extends AbstractAsyncCommand {

    private static final Logger LOGGER = Logger.getLogger("MMORPGStats");

    /** Stored shop location (world-wide singleton for now) */
    private static volatile String shopLocation = null;

    /** Get the current shop location string, or null if not set. */
    public static String getShopLocation() { return shopLocation; }

    public SpawnShopCommand() {
        super("spawnshop", "Set the shopkeeper NPC at your location");
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
            // Get the player's position
            TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
            if (tc == null) {
                ctx.sendMessage(Message.raw("Could not determine your position."));
                return;
            }

            Vector3d pos = tc.getPosition();
            int x = (int) pos.getX();
            int y = (int) pos.getY();
            int z = (int) pos.getZ();

            shopLocation = x + ", " + y + ", " + z;

            ctx.sendMessage(Message.raw("* Shopkeeper placed at your location!"));
            ctx.sendMessage(Message.raw("  Position: " + shopLocation));
            ctx.sendMessage(Message.raw("  Type /shop to browse wares."));

            // Broadcast to all players in the world
            for (PlayerRef pr : world.getPlayerRefs()) {
                pr.sendMessage(Message.raw(
                        "* A shopkeeper has appeared at " + shopLocation
                        + "! Type /shop to browse weapons."));
            }

            LOGGER.info("[MMORPG] Shopkeeper placed at " + shopLocation);
        }, world);
    }
}
