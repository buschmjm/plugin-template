package com.mmorpg.stats;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Player command to view weekly quests and progress.
 *   /quest - Show active weekly quests with progress
 */
public class QuestCommand extends AbstractAsyncCommand {

    public QuestCommand() {
        super("quest", "View weekly quests");
    }

    @Override
    protected boolean canGeneratePermission() { return false; }

    @Nonnull
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
        Ref<EntityStore> ref = ctx.senderAsPlayerRef();
        if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        return CompletableFuture.runAsync(() -> {
            var questType = StatsPlugin.getInstance().getPlayerQuestDataType();
            PlayerQuestData data = store.ensureAndGetComponent(ref, questType);
            data.ensureCurrentWeek();

            List<QuestDefinition> active = WeeklyQuestPool.getActiveQuests();

            ctx.sendMessage(Message.raw("=== Weekly Quests (resets in "
                    + WeeklyQuestPool.timeUntilReset() + ") ==="));

            for (QuestDefinition quest : active) {
                int progress = data.getProgress(quest.getId());
                boolean done = data.isCompleted(quest.getId());
                String status = done ? "[DONE]"
                        : "[" + progress + "/" + quest.getTargetCount() + "]";
                String rewardText = "+" + quest.getXpReward() + " XP";
                if (quest.getGoldReward() > 0) {
                    rewardText += ", +" + quest.getGoldReward() + " Gold";
                }
                ctx.sendMessage(Message.raw(
                        status + " " + quest.getName() + " - " + quest.getDescription()
                        + " (" + rewardText + ")"));
            }

            int completed = data.getCompletedCount();
            ctx.sendMessage(Message.raw(completed + "/" + active.size()
                    + " quests completed this week."));
            ctx.sendMessage(Message.raw("Story quests coming soon! Stay tuned."));
            ctx.sendMessage(Message.raw("========================"));
        }, world);
    }
}
