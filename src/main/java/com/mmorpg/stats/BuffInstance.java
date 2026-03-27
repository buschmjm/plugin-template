package com.mmorpg.stats;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single active buff or debuff effect on an entity.
 * Instances are mutable - duration is decremented each tick by BuffTickSystem.
 */
public class BuffInstance {

    private final String id;
    private final String displayName;
    private final BuffType type;
    private float duration;  // seconds remaining; -1 = permanent
    private final boolean permanent;
    private final boolean isDebuff;

    // STAT_MODIFIER: stat index -> additive modifier amount
    private final Map<Integer, Float> statModifiers;

    // Bonus damage multiplier (additive, e.g. 0.25 = +25% damage)
    private final float bonusDamageMultiplier;

    // DAMAGE_OVER_TIME / HEAL_OVER_TIME
    private final float tickInterval;    // seconds between ticks
    private final float tickAmount;      // damage or heal per tick
    private float timeSinceLastTick;

    private BuffInstance(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.type = builder.type;
        this.duration = builder.duration;
        this.permanent = builder.duration < 0;
        this.isDebuff = builder.isDebuff;
        this.statModifiers = Map.copyOf(builder.statModifiers);
        this.bonusDamageMultiplier = builder.bonusDamageMultiplier;
        this.tickInterval = builder.tickInterval;
        this.tickAmount = builder.tickAmount;
        this.timeSinceLastTick = 0f;
    }

    /** Create a copy with independent duration tracking. */
    public BuffInstance copy() {
        Builder b = new Builder(id, displayName, type);
        b.duration = this.permanent ? -1f : this.duration;
        b.isDebuff = this.isDebuff;
        b.statModifiers.putAll(this.statModifiers);
        b.bonusDamageMultiplier = this.bonusDamageMultiplier;
        b.tickInterval = this.tickInterval;
        b.tickAmount = this.tickAmount;
        return new BuffInstance(b);
    }

    // Getters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public BuffType getType() { return type; }
    public float getDuration() { return duration; }
    public boolean isPermanent() { return permanent; }
    public boolean isDebuff() { return isDebuff; }
    public Map<Integer, Float> getStatModifiers() { return statModifiers; }
    public float getBonusDamageMultiplier() { return bonusDamageMultiplier; }
    public float getTickInterval() { return tickInterval; }
    public float getTickAmount() { return tickAmount; }

    /** Returns true if this buff has expired (non-permanent and duration <= 0). */
    public boolean isExpired() {
        return !isPermanent() && duration <= 0;
    }

    /**
     * Advance time by deltaTime. Returns true if a DoT/HoT tick should fire.
     */
    public boolean tick(float deltaTime) {
        if (!isPermanent()) {
            duration -= deltaTime;
        }
        if (type == BuffType.DAMAGE_OVER_TIME || type == BuffType.HEAL_OVER_TIME) {
            timeSinceLastTick += deltaTime;
            if (tickInterval > 0 && timeSinceLastTick >= tickInterval) {
                timeSinceLastTick -= tickInterval;
                return true;
            }
        }
        return false;
    }

    // -- Builder --

    public static Builder builder(String id, String displayName, BuffType type) {
        return new Builder(id, displayName, type);
    }

    public static class Builder {
        private final String id;
        private final String displayName;
        private final BuffType type;
        private float duration = -1f;
        private boolean isDebuff = false;
        private final Map<Integer, Float> statModifiers = new HashMap<>();
        private float bonusDamageMultiplier = 0f;
        private float tickInterval = 1f;
        private float tickAmount = 0f;

        Builder(String id, String displayName, BuffType type) {
            this.id = id;
            this.displayName = displayName;
            this.type = type;
        }

        public Builder duration(float seconds) { this.duration = seconds; return this; }
        public Builder debuff() { this.isDebuff = true; return this; }
        public Builder statModifier(int statIndex, float amount) {
            this.statModifiers.put(statIndex, amount);
            return this;
        }
        public Builder bonusDamage(float multiplier) { this.bonusDamageMultiplier = multiplier; return this; }
        public Builder tickInterval(float seconds) { this.tickInterval = seconds; return this; }
        public Builder tickAmount(float amount) { this.tickAmount = amount; return this; }

        public BuffInstance build() { return new BuffInstance(this); }
    }
}
