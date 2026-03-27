package com.mmorpg.stats;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Reads PlayerStatData and applies calculated effects to the player's EntityStatMap
 * using the modifier system.
 */
public final class StatEffectApplier {

    private StatEffectApplier() {}

    private static final String MOD_PREFIX = "mmorpg_stats_";

    /**
     * Applies all stat effects from the player's base stats to their EntityStatMap.
     * Must be called on the world thread.
     */
    public static void applyStats(Ref<EntityStore> ref, Store<EntityStore> store, PlayerStatData stats) {
        EntityStatMap statMap = (EntityStatMap) store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap == null) return;

        // -- Must-implement effects --

        // VIT -> max HP (additive modifier on max health)
        float hpBonus = stats.getVitality() * StatConstants.HP_PER_VIT;
        applyMaxModifier(statMap, DefaultEntityStatTypes.getHealth(), MOD_PREFIX + "vit_hp", hpBonus);

        // DEX -> stamina pool size (additive modifier on max stamina)
        float staminaBonus = stats.getDexterity() * StatConstants.STAMINA_PER_DEX;
        applyMaxModifier(statMap, DefaultEntityStatTypes.getStamina(), MOD_PREFIX + "dex_stamina", staminaBonus);

        // ARC -> max mana pool size (additive modifier on max mana)
        float manaBonus = stats.getArcana() * StatConstants.MANA_PER_ARC;
        applyMaxModifier(statMap, DefaultEntityStatTypes.getMana(), MOD_PREFIX + "arc_mana", manaBonus);

        // VIT -> underwater breath duration (additive modifier on max oxygen)
        float oxygenBonus = stats.getVitality() * StatConstants.OXYGEN_PER_VIT;
        applyMaxModifier(statMap, DefaultEntityStatTypes.getOxygen(), MOD_PREFIX + "vit_oxygen", oxygenBonus);

        // Equipment bonuses (HP, stamina, mana from equipped weapon)
        float equipHp = 0f, equipStamina = 0f, equipMana = 0f;
        var equipType = StatsPlugin.getInstance().getPlayerEquipmentDataType();
        if (equipType != null) {
            PlayerEquipmentData equipData = store.getComponent(ref, equipType);
            if (equipData != null) {
                ShopItem weapon = equipData.getEquippedShopItem();
                if (weapon != null) {
                    equipHp = weapon.getBonusHp();
                    equipStamina = weapon.getBonusStamina();
                    equipMana = weapon.getBonusMana();
                }
            }
        }
        applyMaxModifier(statMap, DefaultEntityStatTypes.getHealth(), MOD_PREFIX + "equip_hp", equipHp);
        applyMaxModifier(statMap, DefaultEntityStatTypes.getStamina(), MOD_PREFIX + "equip_stamina", equipStamina);
        applyMaxModifier(statMap, DefaultEntityStatTypes.getMana(), MOD_PREFIX + "equip_mana", equipMana);

        // INT -> mana regen rate: implemented in BuffTickSystem
        // STR -> melee damage, DEX -> dodge, VIT -> damage resistance: implemented in DamageHandler
        // ARC -> healing output: implemented in AbilityCommand + BuffTickSystem
        // PRE -> vendor discount: implemented in ShopPage + ShopCommand
        // Class passives (spell dmg, HP regen, mana regen, knockback, aggro, movement speed, shop discount):
        //   accumulated in PlayerClassData.recomputePassives(), applied in respective systems
    }

    /**
     * Applies class title percentage bonuses (HP%, mana%, stamina%) to EntityStatMap.
     * Called by ClassEvaluator when titles change.
     */
    public static void applyClassBonuses(Ref<EntityStore> ref, Store<EntityStore> store,
                                         PlayerClassData classData, PlayerStatData stats) {
        EntityStatMap statMap = (EntityStatMap) store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap == null) return;

        // Class passive: +X% max HP
        float baseMaxHp = StatConstants.BASE_HP + stats.getVitality() * StatConstants.HP_PER_VIT;
        float classHpBonus = baseMaxHp * classData.getCachedMaxHpPercent();
        applyMaxModifier(statMap, DefaultEntityStatTypes.getHealth(),
                MOD_PREFIX + "class_hp", classHpBonus);

        // Class passive: +X% max mana
        float baseMaxMana = StatConstants.BASE_MANA + stats.getArcana() * StatConstants.MANA_PER_ARC;
        float classManaBonus = baseMaxMana * classData.getCachedManaPoolPercent();
        applyMaxModifier(statMap, DefaultEntityStatTypes.getMana(),
                MOD_PREFIX + "class_mana", classManaBonus);

        // Class passive: +X% max stamina
        float baseMaxStamina = StatConstants.BASE_STAMINA + stats.getDexterity() * StatConstants.STAMINA_PER_DEX;
        float classStaminaBonus = baseMaxStamina * classData.getCachedStaminaPoolPercent();
        applyMaxModifier(statMap, DefaultEntityStatTypes.getStamina(),
                MOD_PREFIX + "class_stamina", classStaminaBonus);
    }

    /**
     * Remove all class-related stat modifiers. Called before respec/reevaluation.
     */
    public static void removeClassBonuses(Ref<EntityStore> ref, Store<EntityStore> store) {
        EntityStatMap statMap = (EntityStatMap) store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap == null) return;

        statMap.removeModifier(DefaultEntityStatTypes.getHealth(), MOD_PREFIX + "class_hp");
        statMap.removeModifier(DefaultEntityStatTypes.getMana(), MOD_PREFIX + "class_mana");
        statMap.removeModifier(DefaultEntityStatTypes.getStamina(), MOD_PREFIX + "class_stamina");
    }

    private static void applyMaxModifier(EntityStatMap statMap, int statIndex, String name, float amount) {
        if (amount == 0) {
            statMap.removeModifier(statIndex, name);
        } else {
            statMap.putModifier(statIndex, name,
                    new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.ADDITIVE, amount));
        }
    }
}
