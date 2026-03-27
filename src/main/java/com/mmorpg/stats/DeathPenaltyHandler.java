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
import com.hypixel.hytale.server.core.modules.entity.damage.event.KillFeedEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;

import java.util.logging.Logger;

/**
 * Listens for KillFeedEvent.DecedentMessage - dispatched on the dying entity.
 * When a player dies, applies an XP penalty (lose a fraction of current level XP).
 */
public class DeathPenaltyHandler extends EntityEventSystem<EntityStore, KillFeedEvent.DecedentMessage> {

    private static final Logger LOGGER = Logger.getLogger("MMORPGStats");
    private static final Query<EntityStore> QUERY = Query.any();

    private final ComponentType<EntityStore, PlayerStatData> statType;

    public DeathPenaltyHandler() {
        super(KillFeedEvent.DecedentMessage.class);
        this.statType = StatsPlugin.getInstance().getPlayerStatDataType();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public void onSystemRegistered() {
        LOGGER.info("[DeathPenaltyHandler] System registered successfully");
    }

    @Override
    public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk,
                       Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer,
                       KillFeedEvent.DecedentMessage event) {
        Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
        if (ref == null || !ref.isValid()) return;

        // Only penalize players (they have PlayerStatData)
        PlayerStatData stats = store.getComponent(ref, statType);
        if (stats == null) return;

        int penalty = StatCalculation.calculateDeathXpPenalty(stats.getLevel(), stats.getXp());
        if (penalty <= 0) return;

        // Mutate in-place (live reference)
        stats.setXp(stats.getXp() - penalty);

        // Notify player
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef != null) {
            NotificationUtil.sendNotification(
                    playerRef.getPacketHandler(),
                    Message.raw("You died! Lost " + penalty + " XP"),
                    Message.raw(""),
                    NotificationStyle.Danger);
        }
    }
}
