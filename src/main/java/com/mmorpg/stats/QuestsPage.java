package com.mmorpg.stats;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.List;

public class QuestsPage extends InteractiveCustomUIPage<QuestsPage.QuestAction> {

    private static final int BAR_MAX_WIDTH = 340;

    public QuestsPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, QuestAction.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("QuestsPage.ui");

        PlayerQuestData questData = store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerQuestDataType());

        cmd.set("#ResetTimer.TextSpans", Message.raw(
                "Resets in: " + WeeklyQuestPool.timeUntilReset()));

        List<QuestDefinition> activeQuests = WeeklyQuestPool.getActiveQuests();
        int completedCount = 0;

        if (questData != null) {
            questData.ensureCurrentWeek();
            completedCount = questData.getCompletedCount();
        }

        cmd.set("#CompletedCount.TextSpans", Message.raw(
                "Completed: " + completedCount + "/" + activeQuests.size()));

        for (int i = 0; i < 5; i++) {
            int n = i + 1;
            String prefix = "#Q" + n;
            if (i < activeQuests.size()) {
                QuestDefinition def = activeQuests.get(i);
                int progress = questData != null ? questData.getProgress(def.getId()) : 0;
                boolean done = questData != null && questData.isCompleted(def.getId());
                boolean redeemed = questData != null && questData.isRedeemed(def.getId());
                boolean tracked = questData != null && questData.isTracked(def.getId());

                // Status icon
                cmd.set(prefix + "Status.TextSpans", Message.raw(
                        redeemed ? "✔" : (done ? "*" : "○")));

                // Quest name
                cmd.set(prefix + "Name.TextSpans", Message.raw(def.getName()));

                // Description
                cmd.set(prefix + "Desc.TextSpans", Message.raw(def.getDescription()));

                // Progress text
                cmd.set(prefix + "Progress.TextSpans", Message.raw(
                        progress + "/" + def.getTargetCount()));

                // Progress bar fill
                float fraction = def.getTargetCount() > 0
                        ? Math.min(1.0f, (float) progress / def.getTargetCount()) : 0f;
                int barWidth = (int) (BAR_MAX_WIDTH * fraction);
                cmd.remove(prefix + "BarFill");
                cmd.appendInline(prefix + "BarBg",
                        "Group " + prefix + "BarFill { Anchor: (Left: 0, Top: 0, Bottom: 0, Width: " + barWidth + "); }");

                // Reward text
                StringBuilder reward = new StringBuilder();
                reward.append(def.getXpReward()).append(" XP");
                if (def.getGoldReward() > 0) reward.append(" ").append(def.getGoldReward()).append("g");
                cmd.set(prefix + "Reward.TextSpans", Message.raw(reward.toString()));

                // Button: Redeem (if done & not redeemed) > Track (otherwise)
                if (done && !redeemed) {
                    cmd.set("#BtnTrack" + n + ".TextSpans", Message.raw("Redeem"));
                    events.addEventBinding(CustomUIEventBindingType.Activating,
                            "#BtnTrack" + n, EventData.of("Action", "REDEEM_" + n), false);
                } else if (redeemed) {
                    cmd.set("#BtnTrack" + n + ".TextSpans", Message.raw("Redeemed ✔"));
                } else {
                    cmd.set("#BtnTrack" + n + ".TextSpans", Message.raw(
                            tracked ? "* Tracked" : "Track"));
                    events.addEventBinding(CustomUIEventBindingType.Activating,
                            "#BtnTrack" + n, EventData.of("Action", "TRACK_" + n), false);
                }
            } else {
                // Hide unused card
                cmd.set(prefix + "Status.TextSpans", Message.raw(""));
                cmd.set(prefix + "Name.TextSpans", Message.raw(""));
                cmd.set(prefix + "Desc.TextSpans", Message.raw(""));
                cmd.set(prefix + "Progress.TextSpans", Message.raw(""));
                cmd.set(prefix + "Reward.TextSpans", Message.raw(""));
                cmd.set("#BtnTrack" + n + ".TextSpans", Message.raw(""));
            }
        }

        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnBack", EventData.of("Action", "BACK"), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store, @Nonnull QuestAction data) {
        super.handleDataEvent(ref, store, data);
        if (data.action == null) return;

        if ("BACK".equals(data.action)) {
            Player player = store.getComponent(ref, Player.getComponentType());
            PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (player != null && pRef != null) {
                player.getPageManager().openCustomPage(ref, store, new MainMenuPage(pRef));
            }
            return;
        }

        if (data.action.startsWith("TRACK_")) {
            int index;
            try {
                index = Integer.parseInt(data.action.substring(6)) - 1;
            } catch (NumberFormatException e) {
                return;
            }

            List<QuestDefinition> activeQuests = WeeklyQuestPool.getActiveQuests();
            if (index < 0 || index >= activeQuests.size()) return;

            PlayerQuestData questData = store.getComponent(ref,
                    StatsPlugin.getInstance().getPlayerQuestDataType());
            if (questData == null) return;

            String questId = activeQuests.get(index).getId();
            questData.toggleTracked(questId);

            // Refresh the HUD to show/hide tracked quests
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                var hud = player.getHudManager().getCustomHud();
                if (hud instanceof CombatHud combatHud) {
                    combatHud.forceRefresh(ref, store);
                }
            }

            // Re-open the page to reflect the change
            PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (player != null && pRef != null) {
                player.getPageManager().openCustomPage(ref, store, new QuestsPage(pRef));
            }
        }

        if (data.action.startsWith("REDEEM_")) {
            int index;
            try {
                index = Integer.parseInt(data.action.substring(7)) - 1;
            } catch (NumberFormatException e) {
                return;
            }

            List<QuestDefinition> activeQuests = WeeklyQuestPool.getActiveQuests();
            if (index < 0 || index >= activeQuests.size()) return;

            PlayerQuestData questData = store.getComponent(ref,
                    StatsPlugin.getInstance().getPlayerQuestDataType());
            if (questData == null) return;

            QuestDefinition def = activeQuests.get(index);
            if (!questData.redeem(def.getId())) return;

            // Award XP
            var statType = StatsPlugin.getInstance().getPlayerStatDataType();
            PlayerStatData stats = store.getComponent(ref, statType);
            if (stats != null) {
                stats.addXp(def.getXpReward());
            }

            // Award gold
            PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (def.getGoldReward() > 0) {
                var goldType = StatsPlugin.getInstance().getPlayerGoldDataType();
                PlayerGoldData goldData = store.getComponent(ref, goldType);
                if (goldData != null) {
                    goldData.addGold(def.getGoldReward());
                    if (pRef != null) {
                        GoldLedger.log(pRef.getUsername(), "QUEST",
                                def.getGoldReward(), goldData.getGold(),
                                def.getName());
                    }
                }
            }

            // Chat confirmation
            if (pRef != null) {
                StringBuilder msg = new StringBuilder();
                msg.append("* Redeemed: ").append(def.getName()).append("!");
                msg.append(" +").append(def.getXpReward()).append(" XP");
                if (def.getGoldReward() > 0) {
                    msg.append("  +").append(def.getGoldReward()).append(" Gold");
                }
                pRef.sendMessage(Message.raw(msg.toString()));
            }

            // Refresh HUD
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                var hud = player.getHudManager().getCustomHud();
                if (hud instanceof CombatHud combatHud) {
                    combatHud.forceRefresh(ref, store);
                }
            }

            // Re-open page
            if (player != null && pRef != null) {
                player.getPageManager().openCustomPage(ref, store, new QuestsPage(pRef));
            }
        }
    }

    public static class QuestAction {
        public static final BuilderCodec<QuestAction> CODEC =
                BuilderCodec.<QuestAction>builder(QuestAction.class, QuestAction::new)
                        .addField(new KeyedCodec<>("Action", Codec.STRING),
                                (d, v) -> d.action = v, d -> d.action)
                        .build();
        private String action;
    }
}
