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

public class SkillsPage extends InteractiveCustomUIPage<SkillsPage.SkillAction> {

    private static final int BAR_MAX_WIDTH = 380;

    /** Gathering skills. */
    private static final SkillType[] TAB_GATHER = {
            SkillType.MINING, SkillType.WOODCUTTING, SkillType.FARMING,
            SkillType.HERBALISM, SkillType.EXPLORATION
    };
    /** Crafting skills. */
    private static final SkillType[] TAB_CRAFT = {
            SkillType.CARPENTRY, SkillType.MASONRY, SkillType.SMITHING,
            SkillType.PROCESSING, SkillType.COOKING, SkillType.ALCHEMY,
            SkillType.TINKERING
    };
    /** Combat skills. */
    private static final SkillType[] TAB_COMBAT = {
            SkillType.HUNTING, SkillType.SLAYING, SkillType.VOIDSLAYING,
            SkillType.WARDING
    };

    private static final String[] TAB_TITLES = {"Gathering", "Crafting", "Combat"};
    private static final String[] TAB_COLORS = {"#4CAF50(0.8)", "#FF9800(0.8)", "#F44336(0.8)"};
    private static final SkillType[][] TABS = {TAB_GATHER, TAB_CRAFT, TAB_COMBAT};

    /** Current active tab index. */
    private int activeTab = 0;

    public SkillsPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, SkillAction.CODEC);
    }

    public SkillsPage(@Nonnull PlayerRef playerRef, int initialTab) {
        super(playerRef, CustomPageLifetime.CanDismiss, SkillAction.CODEC);
        this.activeTab = Math.max(0, Math.min(2, initialTab));
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("SkillsPage.ui");

        PlayerSkillData skillData = store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerSkillDataType());

        // Title shows current tab
        cmd.set("#Title.TextSpans", Message.raw(TAB_TITLES[activeTab] + " Skills"));

        // Explored label only on Gathering tab
        if (activeTab == 0 && skillData != null) {
            cmd.set("#ExploredLabel.TextSpans",
                    Message.raw("Chunks explored: " + skillData.getExploredCount()));
        }

        // Highlight active tab button with brighter outline
        String[] btnIds = {"#BtnGather", "#BtnCraft", "#BtnCombat"};
        // Active tab gets rebuilt with thicker outline
        cmd.remove(btnIds[activeTab]);
        String[] btnTexts = {"Gathering", "Crafting", "Combat"};
        String[][] btnBgs = {
                {"#3A5A3E(0.9)", "#7ACA7E"},
                {"#5A4A2E(0.9)", "#CABA6E"},
                {"#5A2A2E(0.9)", "#CA6A6E"}
        };
        cmd.appendInline("#TopBar",
                "TextButton " + btnIds[activeTab] + " { Anchor: (Width: 120, Height: 30); "
                + "Text: \"" + btnTexts[activeTab] + "\"; "
                + "Background: " + btnBgs[activeTab][0] + "; "
                + "OutlineColor: " + btnBgs[activeTab][1] + "; OutlineSize: 2; "
                + "Style: (Default: (LabelStyle: (HorizontalAlignment: Center, VerticalAlignment: Center))); }");

        // Build skill cards for the active tab
        String barColor = TAB_COLORS[activeTab];
        for (SkillType skill : TABS[activeTab]) {
            appendSkillCard(cmd, skillData, skill, barColor);
        }

        // Tab button events
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnBack", EventData.of("Action", "BACK"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnGather", EventData.of("Action", "TAB_0"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnCraft", EventData.of("Action", "TAB_1"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnCombat", EventData.of("Action", "TAB_2"), false);
    }

    private void appendSkillCard(UICommandBuilder cmd, PlayerSkillData skillData,
                                  SkillType skill, String barColor) {
        int level = skillData != null ? skillData.getLevel(skill) : 1;
        int xp = skillData != null ? skillData.getXp(skill) : 0;
        int xpNeeded = skillData != null ? skillData.xpForNextLevel(skill) : 100;
        boolean maxed = level >= StatConstants.MAX_SKILL_LEVEL;

        float fraction = maxed ? 1.0f
                : (xpNeeded > 0 ? Math.min(1.0f, (float) xp / xpNeeded) : 0f);
        int barWidth = (int) (BAR_MAX_WIDTH * fraction);

        String levelText = "Lv. " + level;
        String xpText = maxed ? "MAX" : xp + "/" + xpNeeded;

        cmd.appendInline("#SkillList",
                "Group { Anchor: (Height: 60); LayoutMode: Top; "
                + "Padding: (Top: 6, Bottom: 6, Left: 12, Right: 12); "
                + "Background: #2A2A4E(0.7); OutlineColor: #3A3A5E; OutlineSize: 1; "
                + "Group { Anchor: (Height: 22); LayoutMode: Left; "
                + "Label { Anchor: (Width: 160, Height: 22); Text: \""
                + skill.getDisplayName() + "\"; Style: (FontSize: 15, VerticalAlignment: Center); } "
                + "Label { Anchor: (Width: 80, Height: 22); Text: \""
                + levelText + "\"; Style: (FontSize: 14, HorizontalAlignment: Center, VerticalAlignment: Center); } "
                + "Label { Anchor: (Width: 220, Height: 22); Text: \""
                + skill.getDescription() + "\"; Style: (FontSize: 11, VerticalAlignment: Center); } "
                + "} "
                + "Group { Anchor: (Height: 18); LayoutMode: Left; "
                + "Group { Anchor: (Width: 380, Height: 12); Background: #1A1A2E(0.9); "
                + "OutlineColor: #3A3A5E; OutlineSize: 1; "
                + "Group { Anchor: (Left: 0, Top: 0, Bottom: 0, Width: " + barWidth + "); "
                + "Background: " + barColor + "; } "
                + "} "
                + "Label { Anchor: (Width: 100); Text: \"" + xpText + "\"; "
                + "Style: (FontSize: 12, HorizontalAlignment: Center, VerticalAlignment: Center); } "
                + "} "
                + "}");
        cmd.appendInline("#SkillList", "Group { Anchor: (Height: 4); }");
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store, @Nonnull SkillAction data) {
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

        if (data.action.startsWith("TAB_")) {
            int tab = Integer.parseInt(data.action.substring(4));
            Player player = store.getComponent(ref, Player.getComponentType());
            PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (player != null && pRef != null) {
                player.getPageManager().openCustomPage(ref, store, new SkillsPage(pRef, tab));
            }
        }
    }

    public static class SkillAction {
        public static final BuilderCodec<SkillAction> CODEC =
                BuilderCodec.<SkillAction>builder(SkillAction.class, SkillAction::new)
                        .addField(new KeyedCodec<>("Action", Codec.STRING),
                                (d, v) -> d.action = v, d -> d.action)
                        .build();
        private String action;
    }
}
