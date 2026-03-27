package com.mmorpg.stats;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.event.KillFeedEvent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;

import java.util.logging.Logger;

/**
 * Listens for KillFeedEvent.KillerMessage events on entities.
 * When a player kills something, awards XP and handles level-ups.
 */
public class KillXpHandler extends EntityEventSystem<EntityStore, KillFeedEvent.KillerMessage> {

    private static final Logger LOGGER = Logger.getLogger("MMORPGStats");
    private static final Query<EntityStore> QUERY = Query.any();

    private final ComponentType<EntityStore, PlayerStatData> statType;
    private final ComponentType<EntityStore, PlayerGoldData> goldType;

    public KillXpHandler() {
        super(KillFeedEvent.KillerMessage.class);
        this.statType = StatsPlugin.getInstance().getPlayerStatDataType();
        this.goldType = StatsPlugin.getInstance().getPlayerGoldDataType();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public void onSystemRegistered() {
        LOGGER.info("[KillXpHandler] System registered successfully");
    }

    @Override
    public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk,
                       Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer,
                       KillFeedEvent.KillerMessage event) {
        Ref<EntityStore> killerRef = chunk.getReferenceTo(entityIndex);
        if (killerRef == null || !killerRef.isValid()) return;

        // Only award XP to players
        PlayerStatData stats = store.getComponent(killerRef, statType);
        if (stats == null) return;

        // Scale XP based on the killed mob's max HP
        Ref<EntityStore> targetRef = event.getTargetRef();
        float mobMaxHp = 100.0f; // default fallback
        if (targetRef != null && targetRef.isValid()) {
            EntityStatMap targetStats = (EntityStatMap) store.getComponent(
                    targetRef, EntityStatMap.getComponentType());
            if (targetStats != null) {
                EntityStatValue hp = targetStats.get(DefaultEntityStatTypes.getHealth());
                if (hp != null) {
                    mobMaxHp = hp.getMax();
                }
            }
        }
        int xpGain = StatCalculation.calculateKillXp(mobMaxHp);

        // Calculate mob level from its position and apply XP scaling
        int mobLevel = 1;
        if (targetRef != null && targetRef.isValid()) {
            TransformComponent mobTransform = store.getComponent(
                    targetRef, TransformComponent.getComponentType());
            if (mobTransform != null) {
                var pos = mobTransform.getPosition();
                mobLevel = StatCalculation.calculateMobLevel(pos.getX(), pos.getZ());
            }
        }
        float levelXpMult = StatCalculation.calculateLevelDifferenceXpMultiplier(
                mobLevel, stats.getLevel());
        xpGain = Math.max(1, (int) (xpGain * levelXpMult));

        // Apply guild shared XP bonus
        var guildType = StatsPlugin.getInstance().getGuildDataType();
        if (guildType != null) {
            GuildData guildData = store.getComponent(killerRef, guildType);
            if (guildData != null && guildData.isInGuild()) {
                float guildBonus = GuildRegistry.getSharedXpBonus(guildData.getGuildName());
                if (guildBonus > 0) {
                    xpGain = Math.max(1, (int) (xpGain * (1.0f + guildBonus)));
                }
            }
        }

        int levelsGained = stats.addXp(xpGain);

        // Track kill count for leaderboard
        stats.addKill();

        // Award gold based on mob HP, scaled by level difference
        int goldGain = StatCalculation.calculateKillGold(mobMaxHp);
        goldGain = Math.max(1, (int) (goldGain * levelXpMult));
        PlayerGoldData goldData = store.getComponent(killerRef, goldType);
        if (goldData != null) {
            goldData.addGold(goldGain);
        }

        // Quest tracking: kill + XP
        QuestTracker.record(killerRef, store, QuestObjective.KILL_MOBS);
        QuestTracker.record(killerRef, store, QuestObjective.EARN_XP, xpGain);

        // If the killer is a player, send notifications, log gold, and refresh HUD
        PlayerRef playerRef = store.getComponent(killerRef, PlayerRef.getComponentType());
        if (playerRef != null && goldData != null) {
            GoldLedger.log(playerRef.getUsername(), "KILL", goldGain,
                    goldData.getGold(), "Lv" + mobLevel + " mob");
        }
        if (playerRef != null) {
            // Notify XP + gold gain with mob level
            String mobLevelTag = mobLevel > 1 ? " (Lv" + mobLevel + " mob)" : "";
            NotificationUtil.sendNotification(
                    playerRef.getPacketHandler(),
                    Message.raw("+" + xpGain + " XP  +" + goldGain + " Gold" + mobLevelTag));

            // Handle level-ups
            if (levelsGained > 0) {
                // Apply stat effects for new level
                StatEffectApplier.applyStats(killerRef, store, stats);

                NotificationUtil.sendNotification(
                        playerRef.getPacketHandler(),
                        Message.raw("Level Up! You are now level " + stats.getLevel()),
                        Message.raw(stats.getAvailablePoints() + " stat points available!"),
                        NotificationStyle.Success);
            }

            // Refresh HUD
            Player player = store.getComponent(killerRef, Player.getComponentType());
            if (player != null) {
                var customHud = player.getHudManager().getCustomHud();
                if (customHud instanceof MmorpgHud mHud) {
                    mHud.forceRefresh(killerRef, store);
                } else if (customHud instanceof CombatHud hud) {
                    hud.forceRefresh(killerRef, store);
                }
            }
        }
    }
}
