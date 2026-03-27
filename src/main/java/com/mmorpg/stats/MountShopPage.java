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
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Mount Shop — purchases a mount by directly adding it to the player's
 * mount collection via Mounts+ reflection API.
 */
public class MountShopPage extends InteractiveCustomUIPage<MountShopPage.MountShopAction> {

    // ── Mount definitions ──────────────────────────────────────────────────

    private static final MountEntry[] MOUNTS = {
        new MountEntry("Black Wolf",
                "A swift dark wolf. Ideal for fast travel.",
                "Common", 1.4f, 9, 2000,
                "Wolf_Black"),
        new MountEntry("Polar Bear",
                "A sturdy bear from frozen lands. Extra storage.",
                "Rare", 1.0f, 27, 5000,
                "Bear_Polar"),
        new MountEntry("Cave Rex",
                "A fearsome ancient predator. Built for battle.",
                "Epic", 1.5f, 54, 12000,
                "Rex_Cave"),
        new MountEntry("Frost Dragon",
                "A legendary ice dragon. Fastest mount available.",
                "Legendary", 1.8f, 54, 25000,
                "Dragon_Frost"),
    };

    private int selectedIndex = -1;

    public MountShopPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, MountShopAction.CODEC);
    }

    // ── Build ──────────────────────────────────────────────────────────────

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("MountShopPage.ui");

        PlayerGoldData gold = store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerGoldDataType());

        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnBack", EventData.of("Action", "BACK"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnBuy", EventData.of("Action", "BUY"), false);

        for (int i = 1; i <= MOUNTS.length; i++) {
            events.addEventBinding(CustomUIEventBindingType.Activating,
                    "#Item" + i, EventData.of("Action", "SEL_" + i), false);
        }

        populateView(cmd, gold);
    }

    // ── Events ─────────────────────────────────────────────────────────────

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull MountShopAction data) {
        super.handleDataEvent(ref, store, data);
        if (data.action == null) return;

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());
        PlayerGoldData gold = store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerGoldDataType());

        switch (data.action) {
            case "BACK" -> {
                if (player != null && pRef != null)
                    player.getPageManager().openCustomPage(ref, store, new MainMenuPage(pRef));
            }
            case "BUY" -> handleBuy(ref, store, player, pRef, gold);
            default -> {
                if (data.action.startsWith("SEL_")) {
                    try {
                        int slot = Integer.parseInt(data.action.substring(4));
                        if (slot >= 1 && slot <= MOUNTS.length) {
                            selectedIndex = slot - 1;
                        }
                    } catch (NumberFormatException ignored) {}
                    refresh(ref, store, gold);
                }
            }
        }
    }

    // ── Buy ────────────────────────────────────────────────────────────────

    private void handleBuy(Ref<EntityStore> ref, Store<EntityStore> store,
                           Player player, PlayerRef pRef, PlayerGoldData gold) {
        if (gold == null || pRef == null || player == null) return;
        if (selectedIndex < 0 || selectedIndex >= MOUNTS.length) {
            pRef.sendMessage(Message.raw("Select a mount first."));
            refresh(ref, store, gold);
            return;
        }

        MountEntry mount = MOUNTS[selectedIndex];
        if (!gold.canAfford(mount.price)) {
            pRef.sendMessage(Message.raw("Not enough gold! Need " + mount.price + "g."));
            refresh(ref, store, gold);
            return;
        }

        gold.spend(mount.price);
        store.putComponent(ref, StatsPlugin.getInstance().getPlayerGoldDataType(), gold);
        GoldLedger.log(pRef.getUsername(), "MOUNT_BUY", -mount.price,
                gold.getGold(), mount.displayName);

        UUID playerUUID = pRef.getUuid();
        boolean added = grantMount(playerUUID, mount.typeKey, mount.displayName);
        if (added) {
            pRef.sendMessage(Message.raw(mount.displayName
                    + " added to your mounts! Use /mounts to manage and ride it."));
        } else {
            // Refund if something went wrong
            gold.setGold(gold.getGold() + mount.price);
            store.putComponent(ref, StatsPlugin.getInstance().getPlayerGoldDataType(), gold);
            pRef.sendMessage(Message.raw("Failed to add mount — gold refunded. Try again."));
        }

        refresh(ref, store, store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerGoldDataType()));
    }

    // ── Reflection helper ──────────────────────────────────────────────────

    private static boolean grantMount(UUID playerUUID, String mountTypeKey, String displayName) {
        try {
            Class<?> pluginClass = Class.forName("com.hytale.mountsplus.MountPlugin");
            Object plugin = pluginClass.getMethod("getInstance").invoke(null);
            Object manager = pluginClass.getMethod("getMountManager").invoke(plugin);
            Class<?> managerClass = Class.forName("com.hytale.mountsplus.manager.MountManager");
            Method createMount = managerClass.getMethod("createMount",
                    UUID.class, String.class, String.class);
            Object result = createMount.invoke(manager, playerUUID, mountTypeKey, displayName);
            return result != null;
        } catch (Exception e) {
            java.util.logging.Logger.getLogger("MMORPGStats").severe("MountShopPage: grantMount failed: " + e.getMessage());
            return false;
        }
    }

    // ── View ───────────────────────────────────────────────────────────────

    private void refresh(Ref<EntityStore> ref, Store<EntityStore> store, PlayerGoldData gold) {
        UICommandBuilder cmd = new UICommandBuilder();
        populateView(cmd, gold);
        this.sendUpdate(cmd, new UIEventBuilder(), false);
    }

    private void populateView(UICommandBuilder cmd, PlayerGoldData gold) {
        long goldAmt = gold != null ? gold.getGold() : 0;
        cmd.set("#GoldDisplay.TextSpans", Message.raw("Gold: " + goldAmt + "g"));

        for (int i = 0; i < MOUNTS.length; i++) {
            MountEntry m = MOUNTS[i];
            boolean selected = i == selectedIndex;
            String prefix = selected ? "> " : "  ";
            String rarityTag = "[" + m.rarity.charAt(0) + "] ";
            cmd.set("#Item" + (i + 1) + ".TextSpans",
                    Message.raw(prefix + rarityTag + m.displayName + "  " + m.price + "g"));
        }

        if (selectedIndex >= 0 && selectedIndex < MOUNTS.length) {
            MountEntry m = MOUNTS[selectedIndex];
            cmd.set("#DetailName.TextSpans", Message.raw(m.displayName));
            cmd.set("#DetailDesc.TextSpans", Message.raw(m.description));
            cmd.set("#DetailRarity.TextSpans", Message.raw("Rarity: " + m.rarity));
            cmd.set("#DetailSpeed.TextSpans", Message.raw("Speed: " + m.speed + "x"));
            cmd.set("#DetailStorage.TextSpans", Message.raw("Storage: " + m.storageSlots + " slots"));
            cmd.set("#DetailPrice.TextSpans", Message.raw("Price: " + m.price + "g"));
            cmd.set("#DetailCommands.TextSpans", Message.raw("/mounts - manage  |  /mounts storage"));
        } else {
            cmd.set("#DetailName.TextSpans", Message.raw("Select a Mount"));
            cmd.set("#DetailDesc.TextSpans", Message.raw(""));
            cmd.set("#DetailRarity.TextSpans", Message.raw(""));
            cmd.set("#DetailSpeed.TextSpans", Message.raw(""));
            cmd.set("#DetailStorage.TextSpans", Message.raw(""));
            cmd.set("#DetailPrice.TextSpans", Message.raw(""));
            cmd.set("#DetailCommands.TextSpans", Message.raw(""));
        }
    }

    // ── Data record ────────────────────────────────────────────────────────

    private static final class MountEntry {
        final String displayName;
        final String description;
        final String rarity;
        final float speed;
        final int storageSlots;
        final long price;
        final String typeKey;

        MountEntry(String displayName, String description, String rarity,
                   float speed, int storageSlots, long price, String typeKey) {
            this.displayName = displayName;
            this.description = description;
            this.rarity = rarity;
            this.speed = speed;
            this.storageSlots = storageSlots;
            this.price = price;
            this.typeKey = typeKey;
        }
    }

    // ── Action codec ───────────────────────────────────────────────────────

    public static class MountShopAction {
        public static final BuilderCodec<MountShopAction> CODEC =
                BuilderCodec.<MountShopAction>builder(MountShopAction.class, MountShopAction::new)
                        .addField(new KeyedCodec<>("Action", Codec.STRING),
                                (d, v) -> d.action = v, d -> d.action)
                        .build();
        private String action;
    }
}
