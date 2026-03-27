package com.mmorpg.stats;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for transient per-entity data that doesn't need ECS persistence.
 * Keeps buff and ability data out of the ECS to reduce component count,
 * archetype storage overhead, and component clone() pressure.
 */
public final class TransientDataStore {

    private static final ConcurrentHashMap<Integer, BuffComponent> buffData = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, PlayerAbilityData> abilityData = new ConcurrentHashMap<>();

    private TransientDataStore() {}

    private static int key(Ref<EntityStore> ref) {
        return ref.hashCode();
    }

    // -- Buff data --

    public static BuffComponent getBuffs(Ref<EntityStore> ref) {
        return buffData.computeIfAbsent(key(ref), k -> new BuffComponent());
    }

    // -- Ability data --

    public static PlayerAbilityData getAbilities(Ref<EntityStore> ref) {
        return abilityData.computeIfAbsent(key(ref), k -> new PlayerAbilityData());
    }

    /** Clean up data for an entity that no longer exists. */
    public static void remove(Ref<EntityStore> ref) {
        int k = key(ref);
        buffData.remove(k);
        abilityData.remove(k);
    }

    /** Clear all transient data (e.g. on server shutdown). */
    public static void clear() {
        buffData.clear();
        abilityData.clear();
    }
}
