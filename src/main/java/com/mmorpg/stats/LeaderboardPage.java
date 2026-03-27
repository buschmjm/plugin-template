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
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LeaderboardPage extends InteractiveCustomUIPage<LeaderboardPage.LbAction> {

    private String currentTab = "LEVEL";

    public LeaderboardPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, LbAction.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("LeaderboardPage.ui");

        populateLeaderboard(cmd, ref, store, currentTab);

        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnLevel", EventData.of("Action", "LEVEL"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnGold", EventData.of("Action", "GOLD"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnKills", EventData.of("Action", "KILLS"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnBack", EventData.of("Action", "BACK"), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store, @Nonnull LbAction data) {
        super.handleDataEvent(ref, store, data);
        if (data.action == null) return;

        switch (data.action) {
            case "LEVEL", "GOLD", "KILLS" -> {
                currentTab = data.action;
                UICommandBuilder cmd = new UICommandBuilder();
                populateLeaderboard(cmd, ref, store, currentTab);
                this.sendUpdate(cmd, new UIEventBuilder(), false);
            }
            case "BACK" -> {
                Player player = store.getComponent(ref, Player.getComponentType());
                PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (player != null && pRef != null) {
                    player.getPageManager().openCustomPage(ref, store, new MainMenuPage(pRef));
                }
            }
        }
    }

    private void populateLeaderboard(UICommandBuilder cmd, Ref<EntityStore> ref,
                                     Store<EntityStore> store, String category) {
        cmd.set("#Title.TextSpans", Message.raw("🏆 Leaderboard - " + category));

        World world = store.getExternalData().getWorld();
        var statType = StatsPlugin.getInstance().getPlayerStatDataType();
        var goldType = StatsPlugin.getInstance().getPlayerGoldDataType();

        record Entry(String name, long value) {}
        List<Entry> entries = new ArrayList<>();

        for (PlayerRef pr : world.getPlayerRefs()) {
            Ref<EntityStore> playerRef = pr.getReference();
            if (playerRef == null || !playerRef.isValid()) continue;

            String name = pr.getUsername();
            long value = 0;

            switch (category) {
                case "LEVEL" -> {
                    PlayerStatData stats = store.getComponent(playerRef, statType);
                    if (stats != null) value = stats.getLevel();
                }
                case "GOLD" -> {
                    PlayerGoldData gold = store.getComponent(playerRef, goldType);
                    if (gold != null) value = gold.getGold();
                }
                case "KILLS" -> {
                    PlayerStatData stats = store.getComponent(playerRef, statType);
                    if (stats != null) value = stats.getKills();
                }
            }

            entries.add(new Entry(name, value));
        }

        entries.sort(Comparator.comparingLong(Entry::value).reversed());

        for (int i = 0; i < 10; i++) {
            String label = "#Entry" + (i + 1);
            if (i < entries.size()) {
                Entry e = entries.get(i);
                String medal = switch (i) {
                    case 0 -> "*";
                    case 1 -> "*";
                    case 2 -> "◆";
                    default -> (i + 1) + ".";
                };
                cmd.set(label + ".TextSpans", Message.raw(
                        medal + " " + e.name() + " - " + e.value()));
            } else {
                cmd.set(label + ".TextSpans", Message.raw(""));
            }
        }
    }

    public static class LbAction {
        public static final BuilderCodec<LbAction> CODEC =
                BuilderCodec.<LbAction>builder(LbAction.class, LbAction::new)
                        .addField(new KeyedCodec<>("Action", Codec.STRING),
                                (d, v) -> d.action = v, d -> d.action)
                        .build();
        private String action;
    }
}
