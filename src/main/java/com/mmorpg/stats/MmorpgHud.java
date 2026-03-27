package com.mmorpg.stats;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Enhanced always-on HUD showing HP, Mana, Stamina bars, Level/XP, Gold,
 * Class title, and active quest tracker.
 */
public class MmorpgHud extends CustomUIHud {

    private static final int BAR_MAX_WIDTH = 210;
    private static final long REFRESH_INTERVAL_MS = 500;

    private long lastRefreshTime = 0;

    public MmorpgHud(@Nonnull PlayerRef playerRef) {
        super(playerRef);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder cmd) {
        cmd.append("MmorpgHud.ui");
    }

    public void refresh(Ref<EntityStore> ref, Store<EntityStore> store) {
        long now = System.currentTimeMillis();
        if (now - lastRefreshTime < REFRESH_INTERVAL_MS) return;
        lastRefreshTime = now;

        PlayerStatData stats = store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerStatDataType());
        EntityStatMap statMap = (EntityStatMap) store.getComponent(ref,
                EntityStatMap.getComponentType());
        PlayerGoldData gold = store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerGoldDataType());
        PlayerClassData classData = store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerClassDataType());
        PlayerQuestData questData = store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerQuestDataType());

        if (stats == null || statMap == null) return;

        UICommandBuilder cmd = new UICommandBuilder();
        populateHud(cmd, stats, statMap, gold, classData, questData);
        this.update(false, cmd);
    }

    public void forceRefresh(Ref<EntityStore> ref, Store<EntityStore> store) {
        lastRefreshTime = 0;
        refresh(ref, store);
    }

    private void populateHud(UICommandBuilder cmd, PlayerStatData stats,
                             EntityStatMap statMap, PlayerGoldData gold,
                             PlayerClassData classData, PlayerQuestData questData) {
        // Class title
        String classText = "";
        if (classData != null && classData.getDisplayedTitleName() != null) {
            classText = classData.getDisplayedTitleName();
        }
        cmd.set("#ClassLabel.TextSpans", Message.raw(classText));

        // Level & Gold
        rebuildLabel(cmd, "#LevelRow", "#LevelLabel", 160, "Lv. " + stats.getLevel());
        long goldVal = gold != null ? gold.getGold() : 0;
        rebuildLabel(cmd, "#LevelRow", "#GoldLabel", 160, "Gold: " + goldVal);

        // XP bar
        int xpNeeded = stats.getXpForNextLevel();
        float xpFrac = (xpNeeded > 0 && stats.getLevel() < StatConstants.MAX_LEVEL)
                ? (float) stats.getXp() / xpNeeded : 1.0f;
        rebuildBarFill(cmd, "#XpBarBg", "#XpBarFill", (int) (BAR_MAX_WIDTH * xpFrac));

        // HP
        EntityStatValue hp = statMap.get(DefaultEntityStatTypes.getHealth());
        if (hp != null) {
            rebuildLabel(cmd, "#HpRow", "#HpValue", 60,
                    (int) hp.get() + "/" + (int) hp.getMax());
            rebuildBarFill(cmd, "#HpBarBg", "#HpBarFill", barWidth(hp.get(), hp.getMax()));
        }

        // Mana
        EntityStatValue mana = statMap.get(DefaultEntityStatTypes.getMana());
        if (mana != null) {
            rebuildLabel(cmd, "#ManaRow", "#ManaValue", 60,
                    (int) mana.get() + "/" + (int) mana.getMax());
            rebuildBarFill(cmd, "#ManaBarBg", "#ManaBarFill", barWidth(mana.get(), mana.getMax()));
        }

        // Stamina
        EntityStatValue stamina = statMap.get(DefaultEntityStatTypes.getStamina());
        if (stamina != null) {
            rebuildLabel(cmd, "#StaminaRow", "#StaminaValue", 60,
                    (int) stamina.get() + "/" + (int) stamina.getMax());
            rebuildBarFill(cmd, "#StaminaBarBg", "#StaminaBarFill",
                    barWidth(stamina.get(), stamina.getMax()));
        }

        // Quest tracker (show top 2 active quests)
        if (questData != null) {
            questData.ensureCurrentWeek();
            var activeQuests = WeeklyQuestPool.getActiveQuests();
            int i = 0;
            for (QuestDefinition def : activeQuests) {
                if (i >= 2) break;
                if (questData.isCompleted(def.getId())) continue;
                int prog = questData.getProgress(def.getId());
                String label = i == 0 ? "#Quest1" : "#Quest2";
                cmd.set(label + ".TextSpans", Message.raw(
                        def.getName() + ": " + prog + "/" + def.getTargetCount()));
                i++;
            }
            // Clear unused slots
            if (i < 1) cmd.set("#Quest1.TextSpans", Message.raw(""));
            if (i < 2) cmd.set("#Quest2.TextSpans", Message.raw(""));
        }
    }

    private static void rebuildLabel(UICommandBuilder cmd, String parent, String sel,
                                     int width, String text) {
        cmd.remove(sel);
        cmd.appendInline(parent,
                "Label " + sel + " { Anchor: (Width: " + width + "); Text: \"" + text + "\"; }");
    }

    private static void rebuildBarFill(UICommandBuilder cmd, String parent, String sel, int width) {
        cmd.remove(sel);
        cmd.appendInline(parent,
                "Group " + sel + " { Anchor: (Left: 0, Top: 0, Bottom: 0, Width: " + width + "); }");
    }

    private static int barWidth(float current, float max) {
        if (max <= 0) return 0;
        return (int) (BAR_MAX_WIDTH * Math.min(1.0f, current / max));
    }
}
