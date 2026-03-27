package com.mmorpg.stats;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;

import java.util.Map;

/**
 * Static utility for managing buffs on entities.
 * Buff data is stored in TransientDataStore (not the ECS) to reduce memory overhead.
 */
public final class BuffManager {

    private BuffManager() {}

    /**
     * Add a buff to the entity. If a buff with the same ID already exists,
     * it is replaced (refreshed). Applies stat modifiers immediately.
     */
    public static void addBuff(Ref<EntityStore> ref, Store<EntityStore> store, BuffInstance buff) {
        BuffComponent comp = TransientDataStore.getBuffs(ref);

        // Remove existing buff with same ID (refresh)
        if (comp.hasBuff(buff.getId())) {
            removeBuff(ref, store, buff.getId());
        }

        BuffInstance instance = buff.copy();
        comp.addBuff(instance);

        // Apply stat modifiers
        if (instance.getType() == BuffType.STAT_MODIFIER || !instance.getStatModifiers().isEmpty()) {
            applyStatModifiers(ref, store, instance);
        }

        // Notify player
        notifyPlayer(ref, store, (instance.isDebuff() ? "Debuff: " : "Buff: ")
                + instance.getDisplayName());
    }

    /**
     * Remove a buff by ID. Removes its stat modifiers from EntityStatMap.
     */
    public static void removeBuff(Ref<EntityStore> ref, Store<EntityStore> store, String buffId) {
        BuffComponent comp = TransientDataStore.getBuffs(ref);

        BuffInstance existing = comp.getBuff(buffId);
        if (existing != null) {
            removeStatModifiers(ref, store, existing);
            comp.removeBuff(buffId);
        }
    }

    /** Remove all buffs from the entity. */
    public static void removeAllBuffs(Ref<EntityStore> ref, Store<EntityStore> store) {
        BuffComponent comp = TransientDataStore.getBuffs(ref);

        for (BuffInstance buff : comp.getBuffs()) {
            removeStatModifiers(ref, store, buff);
        }
        comp.clear();
    }

    /**
     * Get the total bonus damage multiplier from all active buffs.
     * Returns 0 if no damage buffs are active.
     */
    public static float getTotalBonusDamage(Ref<EntityStore> ref, Store<EntityStore> store) {
        BuffComponent comp = TransientDataStore.getBuffs(ref);

        float total = 0f;
        for (BuffInstance buff : comp.getBuffs()) {
            total += buff.getBonusDamageMultiplier();
        }
        return total;
    }

    // -- Internal helpers --

    static void applyStatModifiers(Ref<EntityStore> ref, Store<EntityStore> store,
                                   BuffInstance buff) {
        EntityStatMap statMap = (EntityStatMap) store.getComponent(ref,
                EntityStatMap.getComponentType());
        if (statMap == null) return;

        for (Map.Entry<Integer, Float> entry : buff.getStatModifiers().entrySet()) {
            String modName = StatConstants.BUFF_MODIFIER_PREFIX + buff.getId();
            statMap.putModifier(entry.getKey(), modName,
                    new StaticModifier(Modifier.ModifierTarget.MAX,
                            StaticModifier.CalculationType.ADDITIVE, entry.getValue()));
        }
    }

    static void removeStatModifiers(Ref<EntityStore> ref, Store<EntityStore> store,
                                    BuffInstance buff) {
        EntityStatMap statMap = (EntityStatMap) store.getComponent(ref,
                EntityStatMap.getComponentType());
        if (statMap == null) return;

        for (Integer statIndex : buff.getStatModifiers().keySet()) {
            String modName = StatConstants.BUFF_MODIFIER_PREFIX + buff.getId();
            statMap.removeModifier(statIndex, modName);
        }
    }

    private static void notifyPlayer(Ref<EntityStore> ref, Store<EntityStore> store,
                                     String message) {
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef != null) {
            NotificationUtil.sendNotification(playerRef.getPacketHandler(),
                    Message.raw(message));
        }
    }
}
