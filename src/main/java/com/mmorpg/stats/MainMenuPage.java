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

/**
 * Main MMORPG menu hub. Opened via /menu command.
 * Navigate to all subsystem pages from here.
 */
public class MainMenuPage extends InteractiveCustomUIPage<MainMenuPage.MenuAction> {

    public MainMenuPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, MenuAction.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("MainMenu.ui");

        // Show player info
        PlayerStatData stats = store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerStatDataType());
        PlayerGoldData gold = store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerGoldDataType());
        PlayerClassData classData = store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerClassDataType());

        StringBuilder info = new StringBuilder();
        if (stats != null) info.append("Lv. ").append(stats.getLevel());
        if (classData != null && classData.getDisplayedTitleName() != null) {
            info.append("  ").append(classData.getDisplayedTitleName());
        }
        if (gold != null) info.append("     Gold: ").append(gold.getGold());

        cmd.set("#PlayerInfo.TextSpans", Message.raw(info.toString()));

        // Register button events
        String[] buttons = {"Stats", "Shop", "Class", "Abilities", "Quests",
                "Skills", "Guild", "Claims", "Leaderboard", "GoldLog",
                "Mounts", "Pets", "Supplies"};
        for (String btn : buttons) {
            events.addEventBinding(CustomUIEventBindingType.Activating,
                    "#Btn" + btn, EventData.of("Nav", btn), false);
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store, @Nonnull MenuAction data) {
        super.handleDataEvent(ref, store, data);
        if (data.nav == null) return;

        PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (pRef == null) return;

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        switch (data.nav) {
            case "Stats" -> player.getPageManager().openCustomPage(ref, store, new StatsUI(pRef));
            case "Shop" -> player.getPageManager().openCustomPage(ref, store, new ShopPage(pRef));
            case "Class" -> player.getPageManager().openCustomPage(ref, store, new ClassPage(pRef));
            case "Abilities" -> player.getPageManager().openCustomPage(ref, store, new AbilitiesPage(pRef));
            case "Quests" -> player.getPageManager().openCustomPage(ref, store, new QuestsPage(pRef));
            case "Skills" -> player.getPageManager().openCustomPage(ref, store, new SkillsPage(pRef));
            case "Guild" -> player.getPageManager().openCustomPage(ref, store, new GuildPage(pRef));
            case "Claims" -> player.getPageManager().openCustomPage(ref, store, new ClaimsPage(pRef));
            case "Leaderboard" -> player.getPageManager().openCustomPage(ref, store, new LeaderboardPage(pRef));
            case "GoldLog" -> player.getPageManager().openCustomPage(ref, store, new GoldLogPage(pRef));
            case "Mounts" -> player.getPageManager().openCustomPage(ref, store, new MountShopPage(pRef));
            case "Pets" -> player.getPageManager().openCustomPage(ref, store, new PetShopPage(pRef));
            case "Supplies" -> player.getPageManager().openCustomPage(ref, store, new SuppliesPage(pRef));
        }
    }

    public static class MenuAction {
        public static final BuilderCodec<MenuAction> CODEC =
                BuilderCodec.<MenuAction>builder(MenuAction.class, MenuAction::new)
                        .addField(new KeyedCodec<>("Nav", Codec.STRING),
                                (d, v) -> d.nav = v, d -> d.nav)
                        .build();
        private String nav;
    }
}
