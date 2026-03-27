package com.mmorpg.stats;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

/**
 * ECS event systems that intercept block placement, breaking, and damage on claimed land.
 * Cancels the event if the acting entity is not the claim owner, a trusted player,
 * or a guild member - unless the relevant claim setting flag is enabled.
 */
public final class ClaimProtectionSystems {

    private static final Logger LOGGER = Logger.getLogger("MMORPGStats");
    private static final Query<EntityStore> QUERY = Query.any();

    private ClaimProtectionSystems() {}

    /**
     * Check if a player is allowed to interact with a claimed chunk.
     * Returns true if the action should be BLOCKED (caller should cancel the event).
     */
    static boolean shouldBlock(Ref<EntityStore> ref, Store<EntityStore> store,
                               int chunkX, int chunkZ, int requiredFlag) {
        // Check if chunk is claimed
        String ownerUuid = ClaimRegistry.getOwner(chunkX, chunkZ);
        if (ownerUuid == null) return false; // unclaimed land - allow

        // Get the acting player's UUID
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return false; // non-player entity - allow (mobs etc.)

        String actorUuid = playerRef.getUuid().toString();

        // Owner always has access
        if (ownerUuid.equals(actorUuid)) return false;

        // Check if actor is trusted by the claim owner
        var claimType = StatsPlugin.getInstance().getClaimDataType();
        // We need to find the owner's entity ref to check their ClaimData...
        // Since we can't easily look up by UUID, we check the guild and trust via stored data.
        // The ClaimRegistry tracks guild membership for sharing.

        // Check guild membership - same guild members can access guild-claimed land
        if (ClaimRegistry.areInSameGuild(ownerUuid, actorUuid)) return false;

        // Check if the owner's claim settings allow this action for visitors
        // We iterate players to find the owner's ClaimData
        for (PlayerRef pr : store.getExternalData().getWorld().getPlayerRefs()) {
            Ref<EntityStore> ownerRef = pr.getReference();
            if (ownerRef == null || !ownerRef.isValid()) continue;
            if (!pr.getUuid().toString().equals(ownerUuid)) continue;

            ClaimData ownerClaim = store.getComponent(ownerRef, claimType);
            if (ownerClaim == null) break;

            // Check if actor is individually trusted
            if (ownerClaim.isTrusted(actorUuid)) return false;

            // Check if the setting flag allows visitors
            if (ownerClaim.hasFlag(requiredFlag)) return false;

            break;
        }

        // Block the action
        return true;
    }

    /** Send a "protected land" notification to the player */
    static void notifyBlocked(Ref<EntityStore> ref, Store<EntityStore> store) {
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef != null) {
            playerRef.sendMessage(Message.raw("This land is claimed! You don't have permission."));
        }
    }

    // -- Block Break Protection --

    public static class BreakProtection
            extends EntityEventSystem<EntityStore, BreakBlockEvent> {

        public BreakProtection() {
            super(BreakBlockEvent.class);
        }

        @Override
        public Query<EntityStore> getQuery() { return QUERY; }

        @Override
        public Set<Dependency<EntityStore>> getDependencies() {
            return Collections.emptySet();
        }

        @Override
        public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk,
                           Store<EntityStore> store, CommandBuffer<EntityStore> cb,
                           BreakBlockEvent event) {
            if (event.isCancelled()) return;
            Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
            if (ref == null || !ref.isValid()) return;

            try {
                Vector3i target = event.getTargetBlock();
                int chunkX = ClaimData.blockToChunk(target.getX());
                int chunkZ = ClaimData.blockToChunk(target.getZ());

                if (shouldBlock(ref, store, chunkX, chunkZ, ClaimData.FLAG_BREAK_BLOCKS)) {
                    event.setCancelled(true);
                    notifyBlocked(ref, store);
                }
            } catch (Exception e) {
                LOGGER.warning("[MMORPG] Error in break protection: " + e.getMessage());
            }
        }
    }

    // -- Block Place Protection --

    public static class PlaceProtection
            extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

        public PlaceProtection() {
            super(PlaceBlockEvent.class);
        }

        @Override
        public Query<EntityStore> getQuery() { return QUERY; }

        @Override
        public Set<Dependency<EntityStore>> getDependencies() {
            return Collections.emptySet();
        }

        @Override
        public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk,
                           Store<EntityStore> store, CommandBuffer<EntityStore> cb,
                           PlaceBlockEvent event) {
            if (event.isCancelled()) return;
            Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
            if (ref == null || !ref.isValid()) return;

            try {
                Vector3i target = event.getTargetBlock();
                int chunkX = ClaimData.blockToChunk(target.getX());
                int chunkZ = ClaimData.blockToChunk(target.getZ());

                if (shouldBlock(ref, store, chunkX, chunkZ, ClaimData.FLAG_PLACE_BLOCKS)) {
                    event.setCancelled(true);
                    notifyBlocked(ref, store);
                }
            } catch (Exception e) {
                LOGGER.warning("[MMORPG] Error in place protection: " + e.getMessage());
            }
        }
    }

    // -- MMORPG Item Placement Guard --
    // Prevents ability/class gem items from being placed as blocks.
    // The gems inherit from Rock_Gem_Ruby etc. which are placeable; placing them
    // converts the item into a vanilla gem block and the MMORPG item is lost on pickup.

    public static class MmorpgItemPlaceGuard
            extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

        public MmorpgItemPlaceGuard() {
            super(PlaceBlockEvent.class);
        }

        @Override
        public Query<EntityStore> getQuery() { return QUERY; }

        @Override
        public Set<Dependency<EntityStore>> getDependencies() {
            return Collections.emptySet();
        }

        @Override
        public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk,
                           Store<EntityStore> store, CommandBuffer<EntityStore> cb,
                           PlaceBlockEvent event) {
            if (event.isCancelled()) return;
            var item = event.getItemInHand();
            if (item == null) return;
            String itemId = item.getItemId();
            if (itemId != null && itemId.startsWith("MMORPG_")) {
                event.setCancelled(true);
                Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
                if (ref != null && ref.isValid()) {
                    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                    if (playerRef != null) {
                        playerRef.sendMessage(Message.raw("MMORPG items cannot be placed as blocks."));
                    }
                }
            }
        }
    }

    // -- Block Damage Protection (mining progress) --

    public static class DamageBlockProtection
            extends EntityEventSystem<EntityStore, DamageBlockEvent> {

        public DamageBlockProtection() {
            super(DamageBlockEvent.class);
        }

        @Override
        public Query<EntityStore> getQuery() { return QUERY; }

        @Override
        public Set<Dependency<EntityStore>> getDependencies() {
            return Collections.emptySet();
        }

        @Override
        public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk,
                           Store<EntityStore> store, CommandBuffer<EntityStore> cb,
                           DamageBlockEvent event) {
            if (event.isCancelled()) return;
            Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
            if (ref == null || !ref.isValid()) return;

            try {
                Vector3i target = event.getTargetBlock();
                int chunkX = ClaimData.blockToChunk(target.getX());
                int chunkZ = ClaimData.blockToChunk(target.getZ());

                if (shouldBlock(ref, store, chunkX, chunkZ, ClaimData.FLAG_BREAK_BLOCKS)) {
                    event.setCancelled(true);
                    // Don't notify on every mining tick - too spammy
                }
            } catch (Exception e) {
                LOGGER.warning("[MMORPG] Error in damage block protection: " + e.getMessage());
            }
        }
    }
}
