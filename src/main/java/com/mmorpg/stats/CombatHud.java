package com.mmorpg.stats;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;

/**
 * Persistent on-screen HUD showing HP, Mana, Stamina bars and Level/XP display.
 * One instance per player, set via player.getHudManager().setCustomHud().
 */
public class CombatHud extends CustomUIHud {

    private static final int BAR_MAX_WIDTH = 220;
    private static final long REFRESH_INTERVAL_MS = 500; // throttle: max 2 refreshes/sec
    private static final long POPUP_DURATION_MS = 5000; // popup visible for 5 seconds
    private static final long INITIAL_DELAY_MS = 5000; // delay before first refresh (longer for remote servers)

    private long lastRefreshTime = 0;
    private long popupExpireTime = 0;
    private final long createdTime;

    public CombatHud(@Nonnull PlayerRef playerRef) {
        super(playerRef);
        this.createdTime = System.currentTimeMillis();
    }

    @Override
    protected void build(@Nonnull UICommandBuilder cmd) {
        // Use file-based append - creates root document from .ui file
        // appendInline("", ...) crashes because after clear=true there's no root to append to
        cmd.append("CombatHud.ui");
    }

    /**
     * Push updated stat values to the HUD.
     * Throttled to max once per REFRESH_INTERVAL_MS to avoid flooding the client.
     */
    public void refresh(Ref<EntityStore> ref, Store<EntityStore> store) {
        long now = System.currentTimeMillis();
        // Skip refresh during initial delay - give client time to load the .ui document
        if (now - createdTime < INITIAL_DELAY_MS) return;
        if (now - lastRefreshTime < REFRESH_INTERVAL_MS) return;
        lastRefreshTime = now;

        PlayerStatData stats = store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerStatDataType());
        EntityStatMap statMap = (EntityStatMap) store.getComponent(ref,
                EntityStatMap.getComponentType());
        if (stats == null || statMap == null) return;

        // Weapon info
        PlayerEquipmentData equipData = store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerEquipmentDataType());
        Player player = store.getComponent(ref, Player.getComponentType());

        // Quest tracking
        PlayerQuestData questData = store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerQuestDataType());

        UICommandBuilder cmd = new UICommandBuilder();
        populateHud(cmd, stats, statMap, equipData, player, questData);
        try {
            this.update(false, cmd);
        } catch (Exception e) {
            // Swallow - client may not have finished loading the .ui document yet
        }
    }

    /**
     * Force refresh immediately, bypassing throttle.
     * Use for one-shot events like stat allocation or level-up.
     */
    public void forceRefresh(Ref<EntityStore> ref, Store<EntityStore> store) {
        lastRefreshTime = 0;
        refresh(ref, store);
    }

    /**
     * Show a quest completion popup on the HUD.
     * The popup auto-hides after POPUP_DURATION_MS on the next refresh.
     */
    public void showQuestComplete(String questName) {
        popupExpireTime = System.currentTimeMillis() + POPUP_DURATION_MS;

        UICommandBuilder cmd = new UICommandBuilder();
        // Clear existing popup children and add content
        cmd.clear("#QuestPopup");
        cmd.appendInline("#QuestPopup",
                "Label { Anchor: (Height: 22); Text: \"Quest Complete!\"; "
                + "Style: (FontSize: 16, HorizontalAlignment: Center, VerticalAlignment: Center); } "
                + "Label { Anchor: (Height: 18); Text: \"" + questName + "\"; "
                + "Style: (FontSize: 12, HorizontalAlignment: Center, VerticalAlignment: Center); } "
                + "Label { Anchor: (Height: 14); Text: \"Open /menu > Quests to redeem\"; "
                + "Style: (FontSize: 10, HorizontalAlignment: Center, VerticalAlignment: Center); }");
        try {
            this.update(false, cmd);
        } catch (Exception e) {
            // Swallow - client may not have the .ui loaded yet
        }
    }

    private void populateHud(UICommandBuilder cmd, PlayerStatData stats, EntityStatMap statMap,
                             PlayerEquipmentData equipData, Player player,
                             PlayerQuestData questData) {
        // Level & XP - rebuild labels with new text
        rebuildLabel(cmd, "#LevelRow", "#LevelLabel", 200, "Lv. " + stats.getLevel());
        int xpNeeded = stats.getXpForNextLevel();
        if (stats.getLevel() >= StatConstants.MAX_LEVEL) {
            rebuildLabel(cmd, "#LevelRow", "#XpLabel", 200, "MAX");
        } else {
            rebuildLabel(cmd, "#LevelRow", "#XpLabel", 200, "XP: " + stats.getXp() + " / " + xpNeeded);
        }

        // XP bar fill - remove and re-add with correct width
        float xpFraction = (xpNeeded > 0 && stats.getLevel() < StatConstants.MAX_LEVEL)
                ? (float) stats.getXp() / xpNeeded : 1.0f;
        rebuildBarFill(cmd, "#XpBarBg", "#XpBarFill", (int) (BAR_MAX_WIDTH * xpFraction));

        // HP bar
        EntityStatValue hp = statMap.get(DefaultEntityStatTypes.getHealth());
        if (hp != null) {
            float hpCur = hp.get();
            float hpMax = hp.getMax();
            rebuildLabel(cmd, "#HpRow", "#HpValue", 100, (int) hpCur + "/" + (int) hpMax);
            rebuildBarFill(cmd, "#HpBarBg", "#HpBarFill", barWidth(hpCur, hpMax));
        }

        // Mana bar
        EntityStatValue mana = statMap.get(DefaultEntityStatTypes.getMana());
        if (mana != null) {
            float manaCur = mana.get();
            float manaMax = mana.getMax();
            rebuildLabel(cmd, "#ManaRow", "#ManaValue", 100, (int) manaCur + "/" + (int) manaMax);
            rebuildBarFill(cmd, "#ManaBarBg", "#ManaBarFill", barWidth(manaCur, manaMax));
        }

        // Stamina bar
        EntityStatValue stamina = statMap.get(DefaultEntityStatTypes.getStamina());
        if (stamina != null) {
            float stamCur = stamina.get();
            float stamMax = stamina.getMax();
            rebuildLabel(cmd, "#StaminaRow", "#StaminaValue", 100, (int) stamCur + "/" + (int) stamMax);
            rebuildBarFill(cmd, "#StaminaBarBg", "#StaminaBarFill", barWidth(stamCur, stamMax));
        }

        // Weapon name & durability
        ShopItem weapon = (equipData != null) ? equipData.getEquippedShopItem() : null;
        if (weapon != null) {
            rebuildLabel(cmd, "#WeaponRow", "#WeaponLabel", 380, weapon.getDisplayName());

            // Read actual durability from held item
            if (player != null) {
                try {
                    var inv = player.getInventory();
                    var hotbar = inv.getHotbar();
                    ItemStack held = hotbar.getItemStack((short) inv.getActiveHotbarSlot());
                    if (held != null && held.getMaxDurability() > 0) {
                        double maxDur = held.getMaxDurability();
                        double curDur = held.getDurability();
                        rebuildLabel(cmd, "#DurRow", "#DurValue", 100,
                                (int) curDur + "/" + (int) maxDur);
                        rebuildBarFill(cmd, "#DurBarBg", "#DurBarFill",
                                barWidth((float) curDur, (float) maxDur));
                    } else {
                        rebuildLabel(cmd, "#DurRow", "#DurValue", 100, "--");
                        rebuildBarFill(cmd, "#DurBarBg", "#DurBarFill", 0);
                    }
                } catch (Exception e) {
                    rebuildLabel(cmd, "#DurRow", "#DurValue", 100, "--");
                    rebuildBarFill(cmd, "#DurBarBg", "#DurBarFill", 0);
                }
            }
        } else {
            rebuildLabel(cmd, "#WeaponRow", "#WeaponLabel", 380, "No weapon");
            rebuildLabel(cmd, "#DurRow", "#DurValue", 100, "");
            rebuildBarFill(cmd, "#DurBarBg", "#DurBarFill", 0);
        }

        // Tracked quests
        populateTrackedQuests(cmd, questData);

        // Auto-hide quest completion popup after duration
        if (popupExpireTime > 0 && System.currentTimeMillis() >= popupExpireTime) {
            popupExpireTime = 0;
            cmd.clear("#QuestPopup");
            cmd.appendInline("#QuestPopup",
                    "Label { Anchor: (Height: 0); Text: \"\"; }");
        }
    }

    private static final int QUEST_BAR_WIDTH = 160;

    private void populateTrackedQuests(UICommandBuilder cmd, PlayerQuestData questData) {
        // Clear children and rebuild inside existing #QuestPanel
        cmd.clear("#QuestPanel");

        if (questData == null) {
            cmd.appendInline("#QuestPanel",
                    "Label { Anchor: (Height: 0); Text: \"\"; }");
            return;
        }

        questData.ensureCurrentWeek();
        Set<String> trackedIds = questData.getTrackedIds();
        List<QuestDefinition> activeQuests = WeeklyQuestPool.getActiveQuests();

        // Count tracked quests to size the panel
        int count = 0;
        for (QuestDefinition def : activeQuests) {
            if (trackedIds.contains(def.getId())) count++;
        }

        if (count == 0) {
            cmd.appendInline("#QuestPanel",
                    "Label { Anchor: (Height: 0); Text: \"\"; }");
            return;
        }

        StringBuilder children = new StringBuilder();
        for (QuestDefinition def : activeQuests) {
            if (!trackedIds.contains(def.getId())) continue;

            int progress = questData.getProgress(def.getId());
            boolean done = questData.isCompleted(def.getId());
            String status = done ? "[Done] " : "";
            float fraction = def.getTargetCount() > 0
                    ? Math.min(1.0f, (float) progress / def.getTargetCount()) : 0f;
            int barW = (int) (QUEST_BAR_WIDTH * fraction);

            children.append("Group { Anchor: (Height: 28); LayoutMode: Top; Padding: (Left: 4, Right: 4); ")
                 .append("Label { Anchor: (Height: 14); Text: \"").append(status).append(def.getName()).append("\"; ")
                 .append("Style: (FontSize: 10, VerticalAlignment: Center); } ")
                 .append("Group { Anchor: (Height: 12); LayoutMode: Left; ")
                 .append("Group { Anchor: (Width: ").append(QUEST_BAR_WIDTH).append(", Height: 8); ")
                 .append("Group { Anchor: (Left: 0, Top: 0, Bottom: 0, Width: ").append(barW).append("); } } ")
                 .append("Label { Anchor: (Width: 60); Text: \"").append(progress).append("/").append(def.getTargetCount()).append("\"; ")
                 .append("Style: (FontSize: 10, HorizontalAlignment: Center, VerticalAlignment: Center); } } } ");
        }

        cmd.appendInline("#QuestPanel", children.toString());
    }

    private static void rebuildLabel(UICommandBuilder cmd, String parentSelector, String labelSelector, int width, String text) {
        cmd.remove(labelSelector);
        cmd.appendInline(parentSelector,
                "Label " + labelSelector + " { Anchor: (Width: " + width + "); Text: \"" + text + "\"; }");
    }

    private static void rebuildBarFill(UICommandBuilder cmd, String parentSelector, String fillSelector, int width) {
        cmd.remove(fillSelector);
        String id = fillSelector.substring(1); // strip leading #
        cmd.appendInline(parentSelector,
                "Group " + fillSelector + " { Anchor: (Left: 0, Top: 0, Bottom: 0, Width: " + width + "); }");
    }

    private static int barWidth(float current, float max) {
        if (max <= 0) return 0;
        return (int) (BAR_MAX_WIDTH * Math.min(1.0f, current / max));
    }
}
