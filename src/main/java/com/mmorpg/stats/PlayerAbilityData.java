package com.mmorpg.stats;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks ability cooldowns per player.
 * Cooldowns are stored as epoch millis when the cooldown ends.
 * Stored in TransientDataStore (not the ECS) to reduce memory overhead.
 * Transient - not persisted across restarts.
 */
public class PlayerAbilityData {

    // abilityId -> cooldown end timestamp (System.currentTimeMillis)
    private final Map<String, Long> cooldowns = new HashMap<>();

    // Quick-cast slots (1-3), each maps to an ability ID or null
    private final String[] bindSlots = new String[3];

    public PlayerAbilityData() {}

    /** Bind an ability to a slot (0-2). */
    public void bindSlot(int slot, String abilityId) {
        if (slot >= 0 && slot < bindSlots.length) {
            bindSlots[slot] = abilityId;
        }
    }

    /** Get the ability ID bound to a slot (0-2), or null. */
    public String getSlot(int slot) {
        if (slot >= 0 && slot < bindSlots.length) {
            return bindSlots[slot];
        }
        return null;
    }

    /** Get all bind slots (for display). */
    public String[] getBindSlots() {
        return bindSlots;
    }

    /** Check if an ability is on cooldown. */
    public boolean isOnCooldown(String abilityId) {
        Long end = cooldowns.get(abilityId);
        return end != null && System.currentTimeMillis() < end;
    }

    /** Get remaining cooldown in seconds (0 if not on cooldown). */
    public float getRemainingCooldown(String abilityId) {
        Long end = cooldowns.get(abilityId);
        if (end == null) return 0f;
        long remaining = end - System.currentTimeMillis();
        return remaining > 0 ? remaining / 1000f : 0f;
    }

    /** Put an ability on cooldown. */
    public void startCooldown(String abilityId, float cooldownSeconds) {
        if (cooldownSeconds <= 0) return;
        cooldowns.put(abilityId, System.currentTimeMillis()
                + (long) (cooldownSeconds * 1000));
    }

    /** Clear all cooldowns. */
    public void clearCooldowns() {
        cooldowns.clear();
    }
}
