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
 * Pet Shop — purchases a pet by directly adding it to the player's
 * pet collection via Pets+ reflection API.
 */
public class PetShopPage extends InteractiveCustomUIPage<PetShopPage.PetShopAction> {

    // ── Pet definitions ────────────────────────────────────────────────────

    private static final PetEntry[] PETS = {
        new PetEntry("Forest Wolf",
                "A loyal wolf companion that fights alongside you and boosts Stamina.",
                "Common", "Stamina", 10.0f, 20.0f, 10, 1000,
                "Wolf"),
        new PetEntry("Polar Bear",
                "A durable bear from the frozen lands. Grants Health and Defense bonuses.",
                "Rare", "Health, Defense", 14.0f, 20.0f, 20, 6000,
                "Bear_Polar"),
    };

    private int selectedIndex = -1;

    public PetShopPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, PetShopAction.CODEC);
    }

    // ── Build ──────────────────────────────────────────────────────────────

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("PetShopPage.ui");

        PlayerGoldData gold = store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerGoldDataType());

        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnBack", EventData.of("Action", "BACK"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnBuy", EventData.of("Action", "BUY"), false);

        for (int i = 1; i <= PETS.length; i++) {
            events.addEventBinding(CustomUIEventBindingType.Activating,
                    "#Item" + i, EventData.of("Action", "SEL_" + i), false);
        }

        populateView(cmd, gold);
    }

    // ── Events ─────────────────────────────────────────────────────────────

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull PetShopAction data) {
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
                        if (slot >= 1 && slot <= PETS.length) {
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
        if (selectedIndex < 0 || selectedIndex >= PETS.length) {
            pRef.sendMessage(Message.raw("Select a pet first."));
            refresh(ref, store, gold);
            return;
        }

        PetEntry pet = PETS[selectedIndex];
        if (!gold.canAfford(pet.price)) {
            pRef.sendMessage(Message.raw("Not enough gold! Need " + pet.price + "g."));
            refresh(ref, store, gold);
            return;
        }

        gold.spend(pet.price);
        store.putComponent(ref, StatsPlugin.getInstance().getPlayerGoldDataType(), gold);
        GoldLedger.log(pRef.getUsername(), "PET_BUY", -pet.price,
                gold.getGold(), pet.displayName);

        UUID playerUUID = pRef.getUuid();
        boolean added = grantPet(playerUUID, pet.typeKey, pet.displayName);
        if (added) {
            pRef.sendMessage(Message.raw(pet.displayName
                    + " added to your pets! Use /pets to manage and summon it."));
        } else {
            // Refund if something went wrong
            gold.setGold(gold.getGold() + pet.price);
            store.putComponent(ref, StatsPlugin.getInstance().getPlayerGoldDataType(), gold);
            pRef.sendMessage(Message.raw("Failed to add pet — gold refunded. Try again."));
        }

        refresh(ref, store, store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerGoldDataType()));
    }

    // ── Reflection helper ──────────────────────────────────────────────────

    private static boolean grantPet(UUID playerUUID, String petTypeKey, String displayName) {
        try {
            Class<?> pluginClass = Class.forName("com.hytale.petsplus.PetPlugin");
            Object plugin = pluginClass.getMethod("getInstance").invoke(null);
            Object manager = pluginClass.getMethod("getPetManager").invoke(plugin);
            Class<?> managerClass = Class.forName("com.hytale.petsplus.manager.PetManager");
            Method createPet = managerClass.getMethod("createPet",
                    UUID.class, String.class, String.class);
            Object result = createPet.invoke(manager, playerUUID, petTypeKey, displayName);
            return result != null;
        } catch (Exception e) {
            java.util.logging.Logger.getLogger("MMORPGStats").severe("PetShopPage: grantPet failed: " + e.getMessage());
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

        for (int i = 0; i < PETS.length; i++) {
            PetEntry p = PETS[i];
            boolean selected = i == selectedIndex;
            String prefix = selected ? "> " : "  ";
            String rarityTag = "[" + p.rarity.charAt(0) + "] ";
            cmd.set("#Item" + (i + 1) + ".TextSpans",
                    Message.raw(prefix + rarityTag + p.displayName + "  " + p.price + "g"));
        }

        if (selectedIndex >= 0 && selectedIndex < PETS.length) {
            PetEntry p = PETS[selectedIndex];
            cmd.set("#DetailName.TextSpans", Message.raw(p.displayName));
            cmd.set("#DetailDesc.TextSpans", Message.raw(p.description));
            cmd.set("#DetailRarity.TextSpans", Message.raw("Rarity: " + p.rarity));
            cmd.set("#DetailPerks.TextSpans", Message.raw("Perks: " + p.perks));
            cmd.set("#DetailDamage.TextSpans", Message.raw("Base Damage: " + p.baseDamage));
            cmd.set("#DetailHealth.TextSpans", Message.raw("Base Health: " + p.baseHealth));
            cmd.set("#DetailMaxLevel.TextSpans", Message.raw("Max Level: " + p.maxLevel));
            cmd.set("#DetailPrice.TextSpans", Message.raw("Price: " + p.price + "g"));
            cmd.set("#DetailCommands.TextSpans", Message.raw("/pets - manage  |  Right-click egg to claim"));
        } else {
            cmd.set("#DetailName.TextSpans", Message.raw("Select a Pet"));
            cmd.set("#DetailDesc.TextSpans", Message.raw(""));
            cmd.set("#DetailRarity.TextSpans", Message.raw(""));
            cmd.set("#DetailPerks.TextSpans", Message.raw(""));
            cmd.set("#DetailDamage.TextSpans", Message.raw(""));
            cmd.set("#DetailHealth.TextSpans", Message.raw(""));
            cmd.set("#DetailMaxLevel.TextSpans", Message.raw(""));
            cmd.set("#DetailPrice.TextSpans", Message.raw(""));
            cmd.set("#DetailCommands.TextSpans", Message.raw(""));
        }
    }

    // ── Data record ────────────────────────────────────────────────────────

    private static final class PetEntry {
        final String displayName;
        final String description;
        final String rarity;
        final String perks;
        final float baseDamage;
        final float baseHealth;
        final int maxLevel;
        final long price;
        final String typeKey;

        PetEntry(String displayName, String description, String rarity, String perks,
                 float baseDamage, float baseHealth, int maxLevel, long price, String typeKey) {
            this.displayName = displayName;
            this.description = description;
            this.rarity = rarity;
            this.perks = perks;
            this.baseDamage = baseDamage;
            this.baseHealth = baseHealth;
            this.maxLevel = maxLevel;
            this.price = price;
            this.typeKey = typeKey;
        }
    }

    // ── Action codec ───────────────────────────────────────────────────────

    public static class PetShopAction {
        public static final BuilderCodec<PetShopAction> CODEC =
                BuilderCodec.<PetShopAction>builder(PetShopAction.class, PetShopAction::new)
                        .addField(new KeyedCodec<>("Action", Codec.STRING),
                                (d, v) -> d.action = v, d -> d.action)
                        .build();
        private String action;
    }
}
