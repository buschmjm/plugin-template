package com.mmorpg.stats;

/**
 * Defines a castable ability with cooldown, resource costs, and effects.
 * Abilities are registered in AbilityRegistry and referenced by ID.
 * Effects are applied via the buff system and EntityStatMap.
 */
public class AbilityDefinition {

    private final String id;
    private final String name;
    private final String description;
    private final float cooldownSeconds;
    private final float manaCost;
    private final float staminaCost;

    // Self-buff template (cloned on cast, null if none)
    private final BuffInstance selfBuff;

    // Instant heal amount applied to caster (0 if none)
    private final float instantHeal;

    // Instant self-damage (cost or side-effect, 0 if none)
    private final float selfDamage;

    // Stat requirement to unlock this ability (null = always available)
    private final String requiredStat;
    private final int requiredPoints;

    private AbilityDefinition(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.description = builder.description;
        this.cooldownSeconds = builder.cooldownSeconds;
        this.manaCost = builder.manaCost;
        this.staminaCost = builder.staminaCost;
        this.selfBuff = builder.selfBuff;
        this.instantHeal = builder.instantHeal;
        this.selfDamage = builder.selfDamage;
        this.requiredStat = builder.requiredStat;
        this.requiredPoints = builder.requiredPoints;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public float getCooldownSeconds() { return cooldownSeconds; }
    public float getManaCost() { return manaCost; }
    public float getStaminaCost() { return staminaCost; }
    public BuffInstance getSelfBuff() { return selfBuff; }
    public float getInstantHeal() { return instantHeal; }
    public float getSelfDamage() { return selfDamage; }
    public String getRequiredStat() { return requiredStat; }
    public int getRequiredPoints() { return requiredPoints; }

    public boolean hasSelfBuff() { return selfBuff != null; }
    public boolean hasInstantHeal() { return instantHeal > 0; }
    public boolean hasSelfDamage() { return selfDamage > 0; }
    public boolean hasRequirement() { return requiredStat != null && requiredPoints > 0; }

    /** Check if a player meets the stat requirement to use this ability. */
    public boolean isUnlockedBy(PlayerStatData stats) {
        if (!hasRequirement()) return true;
        int playerPoints = switch (requiredStat) {
            case "STR" -> stats.getStrength();
            case "DEX" -> stats.getDexterity();
            case "VIT" -> stats.getVitality();
            case "INT" -> stats.getIntellect();
            case "PRE" -> stats.getPresence();
            case "ARC" -> stats.getArcana();
            default -> 0;
        };
        return playerPoints >= requiredPoints;
    }

    /** Get a human-readable requirement string like "STR 10". */
    public String getRequirementText() {
        if (!hasRequirement()) return "";
        return requiredStat + " " + requiredPoints;
    }

    // -- Builder --

    public static Builder builder(String id, String name) {
        return new Builder(id, name);
    }

    public static class Builder {
        private final String id;
        private final String name;
        private String description = "";
        private float cooldownSeconds = 0f;
        private float manaCost = 0f;
        private float staminaCost = 0f;
        private BuffInstance selfBuff = null;
        private float instantHeal = 0f;
        private float selfDamage = 0f;
        private String requiredStat = null;
        private int requiredPoints = 0;

        Builder(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public Builder description(String desc) { this.description = desc; return this; }
        public Builder cooldown(float seconds) { this.cooldownSeconds = seconds; return this; }
        public Builder manaCost(float cost) { this.manaCost = cost; return this; }
        public Builder staminaCost(float cost) { this.staminaCost = cost; return this; }
        public Builder selfBuff(BuffInstance buff) { this.selfBuff = buff; return this; }
        public Builder instantHeal(float amount) { this.instantHeal = amount; return this; }
        public Builder selfDamage(float amount) { this.selfDamage = amount; return this; }
        public Builder requires(String stat, int points) { this.requiredStat = stat; this.requiredPoints = points; return this; }

        public AbilityDefinition build() { return new AbilityDefinition(this); }
    }
}
