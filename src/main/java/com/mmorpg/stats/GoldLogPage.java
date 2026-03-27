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

public class GoldLogPage extends InteractiveCustomUIPage<GoldLogPage.LogAction> {

    public GoldLogPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, LogAction.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("GoldLogPage.ui");

        PlayerGoldData gold = store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerGoldDataType());
        PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());

        long balance = gold != null ? gold.getGold() : 0;
        cmd.set("#BalanceLabel.TextSpans", Message.raw("Balance: " + balance + " gold"));

        if (pRef != null) {
            List<String> entries = GoldLedger.getRecentTransactions(
                    pRef.getUsername(), 10);

            for (int i = 0; i < 10; i++) {
                String label = "#Log" + (i + 1);
                if (i < entries.size()) {
                    cmd.set(label + ".TextSpans", Message.raw(formatEntry(entries.get(i))));
                } else {
                    cmd.set(label + ".TextSpans", Message.raw(""));
                }
            }
        }

        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnBack", EventData.of("Action", "BACK"), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store, @Nonnull LogAction data) {
        super.handleDataEvent(ref, store, data);
        if (data.action == null) return;

        if ("BACK".equals(data.action)) {
            Player player = store.getComponent(ref, Player.getComponentType());
            PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (player != null && pRef != null) {
                player.getPageManager().openCustomPage(ref, store, new MainMenuPage(pRef));
            }
        }
    }

    /**
     * Format a raw ledger entry into a readable line.
     * Input format: timestamp|playerName|TYPE|+/-amount|bal:N|detail
     */
    private static String formatEntry(String raw) {
        String[] parts = raw.split("\\|");
        if (parts.length < 6) return raw;

        // Extract date+time, type, amount, detail
        String time = parts[0];
        // ISO timestamp like 2026-03-23T14:30:00Z -> "Mar 23 14:30"
        String display;
        try {
            java.time.Instant inst = java.time.Instant.parse(time);
            java.time.ZonedDateTime zdt = inst.atZone(java.time.ZoneId.systemDefault());
            display = String.format("%s %d  %02d:%02d",
                    zdt.getMonth().toString().substring(0, 3), zdt.getDayOfMonth(),
                    zdt.getHour(), zdt.getMinute());
        } catch (Exception e) {
            display = time.length() > 10 ? time.substring(5, 10) : time;
        }
        String type = parts[2].replace("_", " ");
        String amount = parts[3];
        String detail = parts[5];

        return display + "  |  " + type + "  " + amount + "  |  " + detail;
    }

    public static class LogAction {
        public static final BuilderCodec<LogAction> CODEC =
                BuilderCodec.<LogAction>builder(LogAction.class, LogAction::new)
                        .addField(new KeyedCodec<>("Action", Codec.STRING),
                                (d, v) -> d.action = v, d -> d.action)
                        .build();
        private String action;
    }
}
