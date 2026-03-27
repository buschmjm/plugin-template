package com.mmorpg.stats;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * ECS component tracking earned class titles, displayed title, and cached passive bonuses.
 * Earned titles and display settings are persisted via BSON.
 * Cached combat bonuses are recomputed at runtime from earned titles.
 */
public class PlayerClassData implements Component<EntityStore> {

    // -- Persisted state --
    private final List<String> earnedTitles = new ArrayList<>();
    private String displayedTitle = "Apprentice";
    private boolean autoUpdate = true;

    // -- Cached combat bonuses (recomputed from earnedTitles) --
    private float cachedMeleeDamageBonus;
    private float cachedSpellDamageBonus;
    private float cachedMissChanceBonus;
    private float cachedDamageReductionPercent;
    private float cachedDamageNegateChance;
    private float cachedLowHpResistPercent;
    private float cachedMaxHpPercent;
    private float cachedManaPoolPercent;
    private float cachedStaminaPoolPercent;
    private float cachedHealingOutputPercent;
    private float cachedCritMultiplierBonus;
    private float cachedHpRegenPercent;
    private float cachedManaRegenPercent;
    private float cachedFallDamageReduction;
    private float cachedShopDiscountPercent;

    public static final BuilderCodec<PlayerClassData> CODEC =
            BuilderCodec.<PlayerClassData>builder(PlayerClassData.class, PlayerClassData::new)
                    .addField(new KeyedCodec<>("EarnedTitles", Codec.STRING),
                            PlayerClassData::parseTitles,
                            PlayerClassData::serializeTitles)
                    .addField(new KeyedCodec<>("DisplayedTitle", Codec.STRING),
                            (data, value) -> data.displayedTitle = value,
                            data -> data.displayedTitle)
                    .addField(new KeyedCodec<>("AutoUpdate", Codec.INTEGER),
                            (data, value) -> data.autoUpdate = (value != 0),
                            data -> data.autoUpdate ? 1 : 0)
                    .build();

    public PlayerClassData() {}

    // -- Title management --

    public Collection<String> getEarnedTitles() { return Collections.unmodifiableList(earnedTitles); }
    public String getDisplayedTitle() { return displayedTitle; }
    public boolean isAutoUpdate() { return autoUpdate; }

    public void setDisplayedTitle(String titleId) { this.displayedTitle = titleId; }
    public void setAutoUpdate(boolean auto) { this.autoUpdate = auto; }

    public boolean hasTitle(String titleId) { return earnedTitles.contains(titleId); }

    public void addTitle(String titleId) {
        if (!earnedTitles.contains(titleId)) earnedTitles.add(titleId);
    }

    public void clearTitles() {
        earnedTitles.clear();
        displayedTitle = "Apprentice";
        clearCachedBonuses();
    }

    /** Get the display name for the currently displayed title. */
    public String getDisplayedTitleName() {
        if ("Apprentice".equals(displayedTitle)) return "Apprentice";
        int sep = displayedTitle.lastIndexOf('_');
        if (sep < 0) return displayedTitle;
        String classId = displayedTitle.substring(0, sep);
        int tier;
        try { tier = Integer.parseInt(displayedTitle.substring(sep + 1)); }
        catch (NumberFormatException e) { return displayedTitle; }
        ClassDefinition cls = ClassRegistry.get(classId);
        if (cls == null) return displayedTitle;
        ClassTier t = cls.getTier(tier);
        return t != null ? t.titleName() : displayedTitle;
    }

    // -- Cached combat bonuses --

    public float getCachedMeleeDamageBonus() { return cachedMeleeDamageBonus; }
    public float getCachedSpellDamageBonus() { return cachedSpellDamageBonus; }
    public float getCachedMissChanceBonus() { return cachedMissChanceBonus; }
    public float getCachedDamageReductionPercent() { return cachedDamageReductionPercent; }
    public float getCachedDamageNegateChance() { return cachedDamageNegateChance; }
    public float getCachedLowHpResistPercent() { return cachedLowHpResistPercent; }
    public float getCachedMaxHpPercent() { return cachedMaxHpPercent; }
    public float getCachedManaPoolPercent() { return cachedManaPoolPercent; }
    public float getCachedStaminaPoolPercent() { return cachedStaminaPoolPercent; }
    public float getCachedHealingOutputPercent() { return cachedHealingOutputPercent; }
    public float getCachedCritMultiplierBonus() { return cachedCritMultiplierBonus; }
    public float getCachedHpRegenPercent() { return cachedHpRegenPercent; }
    public float getCachedManaRegenPercent() { return cachedManaRegenPercent; }
    public float getCachedFallDamageReduction() { return cachedFallDamageReduction; }
    public float getCachedShopDiscountPercent() { return cachedShopDiscountPercent; }

    /** Recompute all cached bonuses from current earned titles. */
    public void recomputePassives() {
        clearCachedBonuses();
        for (String titleId : earnedTitles) {
            int sep = titleId.lastIndexOf('_');
            if (sep < 0) continue;
            String classId = titleId.substring(0, sep);
            int tier;
            try { tier = Integer.parseInt(titleId.substring(sep + 1)); }
            catch (NumberFormatException e) { continue; }

            ClassDefinition cls = ClassRegistry.get(classId);
            if (cls == null) continue;
            ClassTier t = cls.getTier(tier);
            if (t == null) continue;

            for (PassiveEffect effect : t.passives()) {
                switch (effect.type()) {
                    case MELEE_DAMAGE_PERCENT -> cachedMeleeDamageBonus += effect.value();
                    case SPELL_DAMAGE_PERCENT -> cachedSpellDamageBonus += effect.value();
                    case MISS_CHANCE_PERCENT -> cachedMissChanceBonus += effect.value();
                    case DAMAGE_REDUCTION_PERCENT -> cachedDamageReductionPercent += effect.value();
                    case DAMAGE_NEGATE_CHANCE -> cachedDamageNegateChance += effect.value();
                    case LOW_HP_RESIST_PERCENT -> cachedLowHpResistPercent += effect.value();
                    case MAX_HP_PERCENT -> cachedMaxHpPercent += effect.value();
                    case MANA_POOL_PERCENT -> cachedManaPoolPercent += effect.value();
                    case STAMINA_POOL_PERCENT -> cachedStaminaPoolPercent += effect.value();
                    case HEALING_OUTPUT_PERCENT -> cachedHealingOutputPercent += effect.value();
                    case CRIT_MULTIPLIER_PERCENT -> cachedCritMultiplierBonus += effect.value();
                    case HP_REGEN_PERCENT -> cachedHpRegenPercent += effect.value();
                    case MANA_REGEN_PERCENT -> cachedManaRegenPercent += effect.value();
                    case FALL_DAMAGE_REDUCTION -> cachedFallDamageReduction += effect.value();
                    case SHOP_DISCOUNT_PERCENT -> cachedShopDiscountPercent += effect.value();
                }
            }
        }
    }

    private void clearCachedBonuses() {
        cachedMeleeDamageBonus = 0;
        cachedSpellDamageBonus = 0;
        cachedMissChanceBonus = 0;
        cachedDamageReductionPercent = 0;
        cachedDamageNegateChance = 0;
        cachedLowHpResistPercent = 0;
        cachedMaxHpPercent = 0;
        cachedManaPoolPercent = 0;
        cachedStaminaPoolPercent = 0;
        cachedHealingOutputPercent = 0;
        cachedCritMultiplierBonus = 0;
        cachedHpRegenPercent = 0;
        cachedManaRegenPercent = 0;
        cachedFallDamageReduction = 0;
        cachedShopDiscountPercent = 0;
    }

    // -- Serialization --

    private String serializeTitles() {
        return String.join(",", earnedTitles);
    }

    private void parseTitles(String csv) {
        earnedTitles.clear();
        if (csv != null && !csv.isEmpty()) {
            for (String s : csv.split(",")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) earnedTitles.add(trimmed);
            }
        }
        recomputePassives();
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        PlayerClassData copy = new PlayerClassData();
        copy.earnedTitles.addAll(this.earnedTitles);
        copy.displayedTitle = this.displayedTitle;
        copy.autoUpdate = this.autoUpdate;
        // Copy cached bonuses directly - avoid recomputing from titles every clone
        copy.cachedMeleeDamageBonus = this.cachedMeleeDamageBonus;
        copy.cachedSpellDamageBonus = this.cachedSpellDamageBonus;
        copy.cachedMissChanceBonus = this.cachedMissChanceBonus;
        copy.cachedDamageReductionPercent = this.cachedDamageReductionPercent;
        copy.cachedDamageNegateChance = this.cachedDamageNegateChance;
        copy.cachedLowHpResistPercent = this.cachedLowHpResistPercent;
        copy.cachedMaxHpPercent = this.cachedMaxHpPercent;
        copy.cachedManaPoolPercent = this.cachedManaPoolPercent;
        copy.cachedStaminaPoolPercent = this.cachedStaminaPoolPercent;
        copy.cachedHealingOutputPercent = this.cachedHealingOutputPercent;
        copy.cachedCritMultiplierBonus = this.cachedCritMultiplierBonus;
        copy.cachedHpRegenPercent = this.cachedHpRegenPercent;
        copy.cachedManaRegenPercent = this.cachedManaRegenPercent;
        copy.cachedFallDamageReduction = this.cachedFallDamageReduction;
        copy.cachedShopDiscountPercent = this.cachedShopDiscountPercent;
        return copy;
    }
}
