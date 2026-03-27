package com.mmorpg.stats;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemGroupDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.protocol.AnimationSlot;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * Intercepts Damage events on entities to apply MMORPG combat stats:
 * - STR -> increases outgoing melee damage
 * - DEX -> chance to dodge incoming damage
 * - VIT -> flat damage resistance (reduction)
 *
 * Positioned via dependencies: runs AFTER gatherDamageGroup, BEFORE filterDamageGroup.
 * This guarantees we modify damage BEFORE ApplyDamage subtracts HP.
 */
public class DamageHandler extends DamageEventSystem {

    private static final Logger LOGGER = Logger.getLogger("MMORPGStats");
    private static final Query<EntityStore> QUERY = Query.any();
    private static final String MOB_LEVEL_HP_MODIFIER = "mmorpg_mob_level_hp";

    private final Set<Dependency<EntityStore>> dependencies;
    private final ComponentType<EntityStore, PlayerStatData> statType;
    private final ComponentType<EntityStore, PlayerEquipmentData> equipType;
    private final ComponentType<EntityStore, PlayerSkillData> skillDataType;

    public DamageHandler() {
        // DamageModule loads before our plugin, so these are safe to access here
        this.dependencies = Set.of(
                new SystemGroupDependency<>(Order.AFTER,
                        DamageModule.get().getGatherDamageGroup()),
                new SystemGroupDependency<>(Order.BEFORE,
                        DamageModule.get().getFilterDamageGroup())
        );
        this.statType = StatsPlugin.getInstance().getPlayerStatDataType();
        this.equipType = StatsPlugin.getInstance().getPlayerEquipmentDataType();
        this.skillDataType = StatsPlugin.getInstance().getPlayerSkillDataType();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependencies;
    }

    @Override
    public void onSystemRegistered() {
        LOGGER.info("[DamageHandler] System registered successfully");
    }

    @Override
    public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk,
                       Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer,
                       Damage damage) {
        if (damage.isCancelled()) return;

        Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
        if (ref == null || !ref.isValid()) return;

        try {
            handleDamage(ref, store, commandBuffer, damage);
        } catch (Exception e) {
            LOGGER.severe("[MMORPG] Error in damage handler: " + e.getMessage());
        }
    }

    private void handleDamage(Ref<EntityStore> ref, Store<EntityStore> store,
                              CommandBuffer<EntityStore> commandBuffer, Damage damage) {
        Damage.Source source = damage.getSource();
        boolean isEntityDamage = source instanceof Damage.EntitySource;

        // Apply offensive stats from the damage source (STR multiplier + crit)
        if (source instanceof Damage.EntitySource entitySource) {
            Ref<EntityStore> attackerRef = entitySource.getRef();
            if (attackerRef != null && attackerRef.isValid()) {
                PlayerStatData attackerStats = store.getComponent(attackerRef, statType);
                if (attackerStats != null) {
                    // STR -> melee damage multiplier
                    float multiplier = StatCalculation.calculateMeleeDamageMultiplier(
                            attackerStats.getStrength());
                    if (multiplier != 1.0f) {
                        damage.setAmount(damage.getAmount() * multiplier);
                    }

                    // Class passive: melee damage bonus
                    PlayerClassData atkClassData = store.getComponent(attackerRef,
                            StatsPlugin.getInstance().getPlayerClassDataType());
                    if (atkClassData != null && atkClassData.getCachedMeleeDamageBonus() != 0f) {
                        damage.setAmount(damage.getAmount()
                                * (1.0f + atkClassData.getCachedMeleeDamageBonus()));
                    }

                    // Class passive: spell damage bonus (applied to all player hits)
                    if (atkClassData != null && atkClassData.getCachedSpellDamageBonus() != 0f) {
                        damage.setAmount(damage.getAmount()
                                * (1.0f + atkClassData.getCachedSpellDamageBonus()));
                    }

                    // Buff bonus damage
                    float buffBonus = BuffManager.getTotalBonusDamage(attackerRef, store);
                    if (buffBonus != 0f) {
                        damage.setAmount(damage.getAmount() * (1.0f + buffBonus));
                    }

                    // Equipment weapon bonus damage
                    PlayerEquipmentData equipData = store.getComponent(attackerRef, equipType);
                    if (equipData != null) {
                        ShopItem weapon = equipData.getEquippedShopItem();
                        if (weapon != null && weapon.getBonusDamageMultiplier() > 0f) {
                            damage.setAmount(damage.getAmount()
                                    * (1.0f + weapon.getBonusDamageMultiplier()));
                        }
                    }

                    // Combat skill damage bonus (Hunting/Slaying/VoidSlaying/Warding)
                    PlayerSkillData attackerSkills = store.getComponent(attackerRef, skillDataType);
                    if (attackerSkills != null) {
                        Nameplate targetNameplate = store.getComponent(ref,
                                Nameplate.getComponentType());
                        if (targetNameplate != null && targetNameplate.getText() != null) {
                            MobCategory category = MobCategory.classify(targetNameplate.getText());
                            SkillType combatSkill = category.getSkillType();
                            if (combatSkill != null) {
                                float skillBonus = attackerSkills.getCombatDamageBonus(combatSkill);
                                if (skillBonus > 0) {
                                    damage.setAmount(damage.getAmount() * (1.0f + skillBonus));
                                }
                            }
                        }
                    }

                    // DEX -> crit chance, STR -> crit multiplier + class crit bonus
                    float critChance = StatCalculation.calculateCritChance(
                            attackerStats.getDexterity());
                    if (critChance > 0 && ThreadLocalRandom.current().nextFloat() < critChance) {
                        float critMult = StatCalculation.calculateCritMultiplier(
                                attackerStats.getStrength());
                        // Add class passive crit multiplier bonus
                        if (atkClassData != null && atkClassData.getCachedCritMultiplierBonus() > 0) {
                            critMult += atkClassData.getCachedCritMultiplierBonus();
                        }
                        damage.setAmount(damage.getAmount() * critMult);

                        // Notify attacker of crit
                        PlayerRef atkPlayerRef = store.getComponent(
                                attackerRef, PlayerRef.getComponentType());
                        if (atkPlayerRef != null) {
                            NotificationUtil.sendNotification(
                                    atkPlayerRef.getPacketHandler(),
                                    Message.raw("CRITICAL HIT!"));
                        }
                    }
                } else {
                    // Attacker is NOT a player (mob) - apply mob level damage scaling
                    TransformComponent atkTransform = store.getComponent(
                            attackerRef, TransformComponent.getComponentType());
                    if (atkTransform != null) {
                        var pos = atkTransform.getPosition();
                        int mobLevel = StatCalculation.calculateMobLevel(pos.getX(), pos.getZ());
                        float mobDmgMult = StatCalculation.calculateMobDamageMultiplier(mobLevel);
                        if (mobDmgMult != 1.0f) {
                            damage.setAmount(damage.getAmount() * mobDmgMult);
                        }
                    }
                }
            }
        }

        // Apply defensive stats to the target taking damage
        PlayerStatData defenderStats = store.getComponent(ref, statType);
        if (defenderStats == null) {
            // Defender is NOT a player (mob) - apply mob level scaling on first hit
            initMobLevel(ref, store, commandBuffer);
            return;
        }

        // Read class passive bonuses for defender
        PlayerClassData defClassData = store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerClassDataType());

        // Class passive: chance to negate damage entirely (entity damage only)
        if (isEntityDamage && defClassData != null && defClassData.getCachedDamageNegateChance() > 0) {
            if (ThreadLocalRandom.current().nextFloat() < defClassData.getCachedDamageNegateChance()) {
                damage.setCancelled(true);
                return;
            }
        }

        // PRE -> miss chance + class miss bonus (entity damage only)
        if (isEntityDamage) {
            float missChance = StatCalculation.calculateMissChance(defenderStats.getPresence());
            if (defClassData != null) {
                missChance += defClassData.getCachedMissChanceBonus();
            }
            if (missChance > 0 && ThreadLocalRandom.current().nextFloat() < missChance) {
                damage.setCancelled(true);
                // Notify defender of miss
                PlayerRef defPlayerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (defPlayerRef != null) {
                    NotificationUtil.sendNotification(
                            defPlayerRef.getPacketHandler(),
                            Message.raw("\u00A7eMiss!"));
                }
                // Try to play a dodge/roll animation
                try {
                    AnimationUtils.playAnimation(ref, AnimationSlot.Action,
                            "dodge", false, store);
                } catch (Exception ignored) {
                    // Animation may not exist - notification is enough
                }
                return;
            }
        }

        // DEX -> fall damage reduction + class fall DR bonus
        if (!isEntityDamage && damage.getCause() == DamageCause.FALL) {
            float fallReduction = StatCalculation.calculateFallDamageReduction(
                    defenderStats.getDexterity());
            if (defClassData != null) {
                fallReduction += defClassData.getCachedFallDamageReduction();
            }
            if (fallReduction > 0) {
                fallReduction = Math.min(1.0f, fallReduction);
                damage.setAmount(damage.getAmount() * (1.0f - fallReduction));
                if (damage.getAmount() <= 0) {
                    damage.setCancelled(true);
                    PlayerRef defPlayerRef = store.getComponent(ref, PlayerRef.getComponentType());
                    if (defPlayerRef != null) {
                        NotificationUtil.sendNotification(
                                defPlayerRef.getPacketHandler(),
                                Message.raw("\u00A7aSafe landing!"));
                    }
                    return;
                }
            }
        }

        // VIT -> flat damage resistance
        float resistance = StatCalculation.calculateDamageResistance(defenderStats.getVitality());
        if (resistance > 0) {
            damage.setAmount(Math.max(0, damage.getAmount() - resistance));
        }

        // Class passive: percentage damage reduction
        if (defClassData != null && defClassData.getCachedDamageReductionPercent() > 0) {
            damage.setAmount(damage.getAmount()
                    * (1.0f - defClassData.getCachedDamageReductionPercent()));
        }

        // Class passive: low HP bonus resistance (at <=25% HP)
        if (defClassData != null && defClassData.getCachedLowHpResistPercent() > 0) {
            var statMap = (com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap)
                    store.getComponent(ref,
                    com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.getComponentType());
            if (statMap != null) {
                var hp = statMap.get(
                        com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes.getHealth());
                if (hp != null && hp.get() <= hp.getMax() * 0.25f) {
                    damage.setAmount(damage.getAmount()
                            * (1.0f - defClassData.getCachedLowHpResistPercent()));
                }
            }
        }

        // Quest tracking: damage dealt (attacker) and survived (defender)
        int finalDamage = (int) damage.getAmount();
        if (finalDamage > 0) {
            QuestTracker.record(ref, store, QuestObjective.SURVIVE_DAMAGE, finalDamage);
            if (source instanceof Damage.EntitySource es) {
                Ref<EntityStore> atkQRef = es.getRef();
                if (atkQRef != null && atkQRef.isValid()) {
                    QuestTracker.record(atkQRef, store, QuestObjective.DEAL_DAMAGE, finalDamage);
                }
            }
        }

        // Refresh defender's HUD so HP bar updates in real time
        Player defenderPlayer = store.getComponent(ref, Player.getComponentType());
        if (defenderPlayer != null) {
            var customHud = defenderPlayer.getHudManager().getCustomHud();
            if (customHud instanceof MmorpgHud mHud) {
                mHud.refresh(ref, store);
            } else if (customHud instanceof CombatHud hud) {
                hud.refresh(ref, store);
            }
        }

        // Refresh attacker's HUD too (their stamina may have changed)
        if (source instanceof Damage.EntitySource entitySrc) {
            Ref<EntityStore> atkRef = entitySrc.getRef();
            if (atkRef != null && atkRef.isValid()) {
                Player attackerPlayer = store.getComponent(atkRef, Player.getComponentType());
                if (attackerPlayer != null) {
                    var atkCustomHud = attackerPlayer.getHudManager().getCustomHud();
                    if (atkCustomHud instanceof MmorpgHud mHud) {
                        mHud.refresh(atkRef, store);
                    } else if (atkCustomHud instanceof CombatHud atkHud) {
                        atkHud.refresh(atkRef, store);
                    }
                }
            }
        }
    }

    /**
     * Initialize mob level scaling on first contact.
     * Applies a max HP modifier so the mob actually has more HP (damage numbers stay unscaled),
     * and sets the nameplate to show "[Lv.X]" above the mob's head.
     * Uses Nameplate.setText() which is a live mutable reference - safe inside ECS systems.
     */
    private void initMobLevel(Ref<EntityStore> ref, Store<EntityStore> store,
                               CommandBuffer<EntityStore> commandBuffer) {
        // Get mob position to determine level
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        // Check if already initialized via the HP modifier
        EntityStatMap statMap = (EntityStatMap) store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap != null) {
            if (statMap.getModifier(DefaultEntityStatTypes.getHealth(), MOB_LEVEL_HP_MODIFIER) != null) {
                return; // already scaled
            }
        }

        var pos = transform.getPosition();
        int mobLevel = StatCalculation.calculateMobLevel(pos.getX(), pos.getZ());

        // Apply HP modifier (even 0 for level 1 as an init marker)
        if (statMap != null) {
            EntityStatValue hp = statMap.get(DefaultEntityStatTypes.getHealth());
            if (hp != null) {
                float hpMult = StatCalculation.calculateMobHpMultiplier(mobLevel);
                float bonusHp = hp.getMax() * (hpMult - 1.0f);
                statMap.putModifier(DefaultEntityStatTypes.getHealth(), MOB_LEVEL_HP_MODIFIER,
                        new StaticModifier(Modifier.ModifierTarget.MAX,
                                StaticModifier.CalculationType.ADDITIVE, bonusHp));
            }
        }

        // Set nameplate to show mob level above head
        // CommandBuffer.ensureAndGetComponent creates the Nameplate if the mob doesn't have one,
        // and is ECS-safe (deferred structural change via command buffer)
        try {
            Nameplate nameplate = commandBuffer.ensureAndGetComponent(
                    ref, Nameplate.getComponentType());
            String currentText = nameplate.getText();
            if (currentText != null && !currentText.isEmpty() && !currentText.startsWith("[Lv")) {
                nameplate.setText("[Lv" + mobLevel + "] " + currentText);
            } else if (currentText == null || currentText.isEmpty()) {
                nameplate.setText("[Lv" + mobLevel + "]");
            }
        } catch (Exception e) {
            LOGGER.warning("[MMORPG] Failed to set mob nameplate: " + e.getMessage());
        }
    }
}
