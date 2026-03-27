package com.mmorpg.stats;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Ticks player entities to process active buffs/debuffs.
 * Buff data is stored in TransientDataStore, not the ECS, to reduce memory overhead.
 * Queries for PlayerStatData (which all players have) instead of a dedicated buff component.
 */
public class BuffTickSystem extends EntityTickingSystem<EntityStore> {

    private static final Logger LOGGER = Logger.getLogger("MMORPGStats");
    private static final int CHUNK_SIZE = 16;
    private static final float EXPLORE_CHECK_INTERVAL = 1.0f;

    private final Query<EntityStore> query;
    private final ComponentType<EntityStore, PlayerSkillData> skillType;
    private float exploreAccum = 0f;

    public BuffTickSystem() {
        // Query for entities with PlayerStatData (all players)
        this.query = Archetype.of(StatsPlugin.getInstance().getPlayerStatDataType());
        this.skillType = StatsPlugin.getInstance().getPlayerSkillDataType();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void onSystemRegistered() {
        LOGGER.info("[BuffTickSystem] System registered successfully");
    }

    @Override
    public void tick(float deltaTime, int entityIndex, ArchetypeChunk<EntityStore> chunk,
                     Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
        if (ref == null || !ref.isValid()) return;

        try {
            tickPlayer(deltaTime, ref, store);
        } catch (Exception e) {
            LOGGER.severe("[MMORPG] Error in tick system: " + e.getMessage());
        }
    }

    private void tickPlayer(float deltaTime, Ref<EntityStore> ref, Store<EntityStore> store) {
        // -- Buff processing --
        BuffComponent comp = TransientDataStore.getBuffs(ref);
        List<BuffInstance> buffs = comp.getBuffs();
        if (!buffs.isEmpty()) {
            boolean anyExpired = false;
            Iterator<BuffInstance> it = buffs.iterator();
            while (it.hasNext()) {
                BuffInstance buff = it.next();

                boolean shouldTick = buff.tick(deltaTime);

                // Apply DoT/HoT tick
                if (shouldTick) {
                    applyTickEffect(ref, store, buff);
                }

                // Remove expired buffs
                if (buff.isExpired()) {
                    BuffManager.removeStatModifiers(ref, store, buff);
                    it.remove();
                    anyExpired = true;

                    // Notify player of buff expiry
                    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                    if (playerRef != null) {
                        NotificationUtil.sendNotification(playerRef.getPacketHandler(),
                                Message.raw(buff.getDisplayName() + " expired"));
                    }
                }
            }

            // Refresh HUD if any buffs expired (stat changes)
            if (anyExpired) {
                Player player = store.getComponent(ref, Player.getComponentType());
                if (player != null) {
                    var customHud = player.getHudManager().getCustomHud();
                    if (customHud instanceof MmorpgHud mHud) {
                        mHud.forceRefresh(ref, store);
                    } else if (customHud instanceof CombatHud hud) {
                        hud.refresh(ref, store);
                    }
                }
            }
        }

        // -- Mana regeneration --
        EntityStatMap statMap = (EntityStatMap) store.getComponent(ref,
                EntityStatMap.getComponentType());
        if (statMap != null) {
            // Mana regen: base + INT scaling, boosted by class mana regen passive
            com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue mana =
                    statMap.get(DefaultEntityStatTypes.getMana());
            if (mana != null && mana.get() < mana.getMax()) {
                PlayerStatData stats = store.getComponent(ref,
                        StatsPlugin.getInstance().getPlayerStatDataType());
                float regenRate = StatConstants.BASE_MANA_REGEN
                        + (stats != null ? stats.getIntellect() * StatConstants.MANA_REGEN_PER_INT : 0);
                // Apply class passive: mana regen bonus
                PlayerClassData classData = store.getComponent(ref,
                        StatsPlugin.getInstance().getPlayerClassDataType());
                if (classData != null && classData.getCachedManaRegenPercent() > 0) {
                    regenRate *= (1.0f + classData.getCachedManaRegenPercent());
                }
                statMap.addStatValue(DefaultEntityStatTypes.getMana(), regenRate * deltaTime);
            }

            // HP regen: class passive only (no base HP regen)
            PlayerClassData classData2 = store.getComponent(ref,
                    StatsPlugin.getInstance().getPlayerClassDataType());
            if (classData2 != null && classData2.getCachedHpRegenPercent() > 0) {
                com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue hp =
                        statMap.get(DefaultEntityStatTypes.getHealth());
                if (hp != null && hp.get() < hp.getMax()) {
                    float hpRegenRate = hp.getMax() * classData2.getCachedHpRegenPercent() * deltaTime;
                    statMap.addStatValue(DefaultEntityStatTypes.getHealth(), hpRegenRate);
                }
            }
        }

        // -- Exploration tracking (throttled to 1 Hz) --
        exploreAccum += deltaTime;
        if (exploreAccum >= EXPLORE_CHECK_INTERVAL) {
            exploreAccum = 0f;
            tickExploration(ref, store);
        }

        // -- Periodic HUD refresh (throttled internally to 500ms) --
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            var customHud = player.getHudManager().getCustomHud();
            if (customHud instanceof MmorpgHud mHud) {
                mHud.refresh(ref, store);
            } else if (customHud instanceof CombatHud hud) {
                hud.refresh(ref, store);
            }
        }
    }

    private void tickExploration(Ref<EntityStore> ref, Store<EntityStore> store) {
        PlayerSkillData skills = store.getComponent(ref, skillType);
        if (skills == null) return;

        TransformComponent transform = store.getComponent(ref,
                TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d pos = transform.getPosition();
        int chunkX = (int) Math.floor(pos.getX()) / CHUNK_SIZE;
        int chunkZ = (int) Math.floor(pos.getZ()) / CHUNK_SIZE;

        if (!skills.markExplored(chunkX, chunkZ)) return;

        // Persist the updated explored chunks set
        store.putComponent(ref, skillType, skills);

        // Quest tracking: chunk explored
        QuestTracker.record(ref, store, QuestObjective.EXPLORE_CHUNKS);

        int xp = StatConstants.EXPLORATION_XP_PER_CHUNK;
        int levelsGained = skills.addXp(SkillType.EXPLORATION, xp);

        if (levelsGained > 0) {
            int newLevel = skills.getLevel(SkillType.EXPLORATION);
            int newRadius = skills.getMapRadius();

            // Apply updated view radius to the player's world map
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null && player.getWorldMapTracker() != null) {
                player.getWorldMapTracker().setViewRadiusOverride(newRadius);
            }

            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef != null) {
                NotificationUtil.sendNotification(playerRef.getPacketHandler(),
                        Message.raw("Exploration level " + newLevel + "! Map radius: " + newRadius + " chunks"));
            }
        }
    }

    private void applyTickEffect(Ref<EntityStore> ref, Store<EntityStore> store,
                                 BuffInstance buff) {
        EntityStatMap statMap = (EntityStatMap) store.getComponent(ref,
                EntityStatMap.getComponentType());
        if (statMap == null) return;

        int healthIndex = DefaultEntityStatTypes.getHealth();
        if (buff.getType() == BuffType.DAMAGE_OVER_TIME) {
            statMap.subtractStatValue(healthIndex, buff.getTickAmount());
        } else if (buff.getType() == BuffType.HEAL_OVER_TIME) {
            float healAmount = buff.getTickAmount();
            // Scale by ARC healing multiplier + class healing output passive
            PlayerStatData pStats = store.getComponent(ref,
                    StatsPlugin.getInstance().getPlayerStatDataType());
            if (pStats != null) {
                healAmount *= StatCalculation.calculateHealingMultiplier(pStats.getArcana());
            }
            PlayerClassData classData = store.getComponent(ref,
                    StatsPlugin.getInstance().getPlayerClassDataType());
            if (classData != null && classData.getCachedHealingOutputPercent() > 0) {
                healAmount *= (1.0f + classData.getCachedHealingOutputPercent());
            }
            statMap.addStatValue(healthIndex, healAmount);
        }
    }
}
