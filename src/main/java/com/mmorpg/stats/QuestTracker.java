package com.mmorpg.stats;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;

/**
 * Utility for advancing quest progress from any system.
 * Call the appropriate method when a trackable event occurs.
 */
public final class QuestTracker {

    private QuestTracker() {}

    /** Record progress for a quest objective. Notifies player on completion. */
    public static void record(Ref<EntityStore> ref, Store<EntityStore> store,
                              QuestObjective objective, int amount) {
        var questType = StatsPlugin.getInstance().getPlayerQuestDataType();
        if (questType == null) return;
        PlayerQuestData data = store.getComponent(ref, questType);
        if (data == null) return;

        List<String> newlyCompleted = data.incrementProgress(objective, amount);

        if (!newlyCompleted.isEmpty()) {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef != null) {
                for (String questId : newlyCompleted) {
                    List<QuestDefinition> active = WeeklyQuestPool.getActiveQuests();
                    for (QuestDefinition q : active) {
                        if (q.getId().equals(questId)) {
                            // Chat notification - tell player to redeem
                            StringBuilder msg = new StringBuilder();
                            msg.append("* Quest complete: ").append(q.getName()).append("!");
                            msg.append(" Open /menu > Quests to redeem your rewards.");
                            playerRef.sendMessage(Message.raw(msg.toString()));

                            // HUD popup notification
                            Player player = store.getComponent(ref, Player.getComponentType());
                            if (player != null) {
                                var hud = player.getHudManager().getCustomHud();
                                if (hud instanceof CombatHud combatHud) {
                                    combatHud.showQuestComplete(q.getName());
                                }
                            }
                            break;
                        }
                    }
                }
            }

            // Refresh the HUD to update tracked quest progress
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                var hud = player.getHudManager().getCustomHud();
                if (hud instanceof CombatHud combatHud) {
                    combatHud.forceRefresh(ref, store);
                }
            }
        }
    }

    /** Convenience: record 1 unit of progress */
    public static void record(Ref<EntityStore> ref, Store<EntityStore> store,
                              QuestObjective objective) {
        record(ref, store, objective, 1);
    }
}
