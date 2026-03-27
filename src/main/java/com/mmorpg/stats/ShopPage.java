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
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ShopPage extends InteractiveCustomUIPage<ShopPage.ShopAction> {

    private static final int MAX_LIST_ITEMS = 11;

    /** Compute the discounted price for an item based on the player's PRE stat + class passives. */
    private static long discountedPrice(ShopItem item, Store<EntityStore> store, Ref<EntityStore> ref) {
        float discount = getDiscount(store, ref);
        return Math.max(1, Math.round(item.getPrice() * (1.0f - discount)));
    }

    /** Compute the discounted price for armor based on the player's PRE stat + class passives. */
    private static long discountedArmorPrice(ArmorItem item, Store<EntityStore> store, Ref<EntityStore> ref) {
        float discount = getDiscount(store, ref);
        return Math.max(1, Math.round(item.getPrice() * (1.0f - discount)));
    }

    private static float getDiscount(Store<EntityStore> store, Ref<EntityStore> ref) {
        PlayerStatData stats = store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerStatDataType());
        int pre = stats != null ? stats.getPresence() : 0;
        float discount = StatCalculation.calculateVendorDiscount(pre);
        PlayerClassData classData = store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerClassDataType());
        if (classData != null) {
            discount += classData.getCachedShopDiscountPercent();
        }
        return Math.min(discount, 0.99f);
    }

    private enum Category {
        SWORDS("Swords"), AXES("Axes"), DAGGERS("Daggers"), SPEARS("Spears"),
        STAVES("Staves"), BOWS("Bows"), CROSSBOWS("Xbows"), ELEMENTAL("Elemental"),
        PREMIUM("Premium"),
        HEAD("Helmets"), CHEST("Chest"), HANDS("Gloves"), LEGS("Legs");
        final String label;
        Category(String label) { this.label = label; }
        boolean isArmor() { return this == HEAD || this == CHEST || this == HANDS || this == LEGS; }
    }

    private static final String[][] CATEGORY_ITEMS = {
            // SWORDS
            {"stormbreaker", "crimson_warblade", "frostmourne", "frozen_runic_blade",
             "azure_flameblade", "chrono_blade", "emerald_crystal", "sakura_no_tsurugi",
             "mystical_spellblade", "spirit_calibur", "anduril"},
            // AXES
            {"berserker_cleaver", "dual_cobalt_war_axe", "deaths_harvest", "fire_steel_mace",
             "sentinels_will", "dual_adam_war_axe", "demon_lord_axe"},
            // DAGGERS
            {"shadowfang", "cyber_daggers", "blood_moon_daggers"},
            // SPEARS
            {"dragon_piercer"},
            // STAVES
            {"archmage_scepter"},
            // BOWS
            {"crude_longbow", "copper_longbow", "iron_longbow",
             "lumina_whisper", "mist_weaver", "azure_vortex"},
            // CROSSBOWS
            {"thorium_crossbow", "cobalt_crossbow", "adamantite_crossbow", "mithril_crossbow"},
            // ELEMENTAL
            {"ice_sword", "flame_saber", "thunder_sword", "serpent_sword",
             "void_sword", "purple_void_sword"},
            // PREMIUM
            {"hearsil_sword", "halox_sword", "serabice_sword", "jolyn_battleaxe",
             "phoenix_daggers", "jon_mace", "dual_rune_blade", "ghost_sword",
             "zweihander", "lahat_chereb", "fire_whip"},
    };

    private static final String[][] ARMOR_CATEGORY_ITEMS = {
            // HEAD
            {"frostwarden_head", "dunewalker_head", "aquaknight_head", "lavaknight_head",
             "sakura_head", "graveknight_head", "demon_head", "rook_head",
             "warden_head", "diamond_head", "emerald_head"},
            // CHEST
            {"frostwarden_chest", "dunewalker_chest", "aquaknight_chest", "lavaknight_chest",
             "sakura_chest", "demon_chest", "rook_chest", "warden_chest",
             "diamond_chest", "emerald_chest"},
            // HANDS
            {"frostwarden_hands", "dunewalker_hands", "aquaknight_hands", "lavaknight_hands",
             "sakura_hands", "demon_hands", "rook_hands", "warden_hands",
             "diamond_hands", "emerald_hands"},
            // LEGS
            {"frostwarden_legs", "dunewalker_legs", "aquaknight_legs", "lavaknight_legs",
             "sakura_legs", "demon_legs", "rook_legs", "warden_legs",
             "diamond_legs", "emerald_legs"},
    };

    private Category currentCategory = Category.SWORDS;
    private int selectedIndex = 0;

    public ShopPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, ShopAction.CODEC);
    }

    private List<ShopItem> getCategoryItems() {
        if (currentCategory.isArmor()) return List.of();
        int idx = currentCategory.ordinal();
        if (idx >= CATEGORY_ITEMS.length) return List.of();
        List<ShopItem> result = new ArrayList<>();
        String[] ids = CATEGORY_ITEMS[idx];
        for (String id : ids) {
            ShopItem item = ShopItem.findById(id);
            if (item != null) result.add(item);
        }
        return result;
    }

    private List<ArmorItem> getCategoryArmorItems() {
        if (!currentCategory.isArmor()) return List.of();
        int idx = currentCategory.ordinal() - Category.HEAD.ordinal();
        if (idx < 0 || idx >= ARMOR_CATEGORY_ITEMS.length) return List.of();
        List<ArmorItem> result = new ArrayList<>();
        String[] ids = ARMOR_CATEGORY_ITEMS[idx];
        for (String id : ids) {
            ArmorItem item = ArmorItem.findById(id);
            if (item != null) result.add(item);
        }
        return result;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("ShopPage.ui");

        PlayerGoldData gold = store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerGoldDataType());
        long balance = gold != null ? gold.getGold() : 0;
        cmd.set("#GoldDisplay.TextSpans", Message.raw("Gold: " + balance + "g"));

        cmd.set("#Title.TextSpans", Message.raw(
                currentCategory.isArmor() ? "Armor Shop" : "Weapon Shop"));

        populateItemList(cmd, store, ref);
        updateCategoryTabs(cmd);
        updateDetailPanel(cmd, store, ref);

        // Category tab events
        for (Category cat : Category.values()) {
            events.addEventBinding(CustomUIEventBindingType.Activating,
                    "#Cat" + cat.label, EventData.of("Action", "CAT_" + cat.name()), false);
        }
        // Item click events
        for (int i = 1; i <= MAX_LIST_ITEMS; i++) {
            events.addEventBinding(CustomUIEventBindingType.Activating,
                    "#Item" + i, EventData.of("Action", "SEL_" + i), false);
        }
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BuyBtn", EventData.of("Action", "BUY"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#EquipBtn", EventData.of("Action", "EQUIP"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnBack", EventData.of("Action", "BACK"), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store, @Nonnull ShopAction data) {
        super.handleDataEvent(ref, store, data);
        if (data.action == null) return;

        String action = data.action;

        // Category switching
        if (action.startsWith("CAT_")) {
            try {
                currentCategory = Category.valueOf(action.substring(4));
                selectedIndex = 0;
                refreshAll(ref, store);
            } catch (IllegalArgumentException ignored) {}
            return;
        }

        // Item selection
        if (action.startsWith("SEL_")) {
            int idx = Integer.parseInt(action.substring(4)) - 1;
            int listSize = currentCategory.isArmor()
                    ? getCategoryArmorItems().size() : getCategoryItems().size();
            if (idx >= 0 && idx < listSize) {
                selectedIndex = idx;
                refreshAll(ref, store);
            }
            return;
        }

        if (currentCategory.isArmor()) {
            handleArmorAction(action, ref, store);
        } else {
            handleWeaponAction(action, ref, store);
        }
    }

    private void handleArmorAction(String action, Ref<EntityStore> ref, Store<EntityStore> store) {
        List<ArmorItem> items = getCategoryArmorItems();
        switch (action) {
            case "BUY" -> {
                if (selectedIndex < 0 || selectedIndex >= items.size()) return;
                ArmorItem item = items.get(selectedIndex);
                long cost = discountedArmorPrice(item, store, ref);

                PlayerGoldData gold = store.getComponent(ref,
                        StatsPlugin.getInstance().getPlayerGoldDataType());
                PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (gold == null || pRef == null) return;

                if (!gold.canAfford(cost)) {
                    pRef.sendMessage(Message.raw("Not enough gold! Need " + cost + "g."));
                    return;
                }

                gold.spend(cost);
                store.putComponent(ref, StatsPlugin.getInstance().getPlayerGoldDataType(), gold);

                Player player = store.getComponent(ref, Player.getComponentType());
                if (player != null) {
                    ItemStack stack = new ItemStack(item.getGameItemId());
                    if (item.getMaxDurability() > 0) {
                        stack = stack.withMaxDurability(item.getMaxDurability());
                        stack = stack.withDurability(item.getMaxDurability());
                    }
                    player.giveItem(stack, ref, store);
                }

                GoldLedger.log(pRef.getUsername(), "SHOP_BUY", -cost,
                        gold.getGold(), item.getDisplayName());
                pRef.sendMessage(Message.raw("Purchased " + item.getDisplayName() + "!"));
                refreshAll(ref, store);
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

    private void handleWeaponAction(String action, Ref<EntityStore> ref, Store<EntityStore> store) {
        List<ShopItem> items = getCategoryItems();

        switch (action) {
            case "BUY" -> {
                if (selectedIndex < 0 || selectedIndex >= items.size()) return;
                ShopItem item = items.get(selectedIndex);
                long cost = discountedPrice(item, store, ref);

                PlayerGoldData gold = store.getComponent(ref,
                        StatsPlugin.getInstance().getPlayerGoldDataType());
                PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (gold == null || pRef == null) return;

                if (!gold.canAfford(cost)) {
                    pRef.sendMessage(Message.raw("Not enough gold! Need " + cost + "g."));
                    return;
                }

                gold.spend(cost);
                store.putComponent(ref, StatsPlugin.getInstance().getPlayerGoldDataType(), gold);

                PlayerEquipmentData equip = store.getComponent(ref,
                        StatsPlugin.getInstance().getPlayerEquipmentDataType());
                if (equip != null) {
                    equip.setEquippedItem(item.getId());
                    store.putComponent(ref, StatsPlugin.getInstance().getPlayerEquipmentDataType(), equip);
                }

                PlayerStatData stats = store.getComponent(ref,
                        StatsPlugin.getInstance().getPlayerStatDataType());
                if (stats != null) {
                    StatEffectApplier.applyStats(ref, store, stats);
                }

                Player player = store.getComponent(ref, Player.getComponentType());
                if (player != null) {
                    ItemStack stack = new ItemStack(item.getGameItemId());
                    if (item.getMaxDurability() > 0) {
                        stack = stack.withMaxDurability(item.getMaxDurability());
                        stack = stack.withDurability(item.getMaxDurability());
                    }
                    player.giveItem(stack, ref, store);
                }

                GoldLedger.log(pRef.getUsername(), "SHOP_BUY", -cost,
                        gold.getGold(), item.getDisplayName());

                pRef.sendMessage(Message.raw("Purchased " + item.getDisplayName() + "!"));
                refreshAll(ref, store);
            }
            case "EQUIP" -> {
                if (selectedIndex < 0 || selectedIndex >= items.size()) return;
                ShopItem item = items.get(selectedIndex);

                PlayerEquipmentData equip = store.getComponent(ref,
                        StatsPlugin.getInstance().getPlayerEquipmentDataType());
                PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (equip == null || pRef == null) return;

                equip.setEquippedItem(item.getId());
                store.putComponent(ref, StatsPlugin.getInstance().getPlayerEquipmentDataType(), equip);

                PlayerStatData stats = store.getComponent(ref,
                        StatsPlugin.getInstance().getPlayerStatDataType());
                if (stats != null) {
                    StatEffectApplier.applyStats(ref, store, stats);
                }

                pRef.sendMessage(Message.raw("Equipped " + item.getDisplayName() + "!"));
                refreshAll(ref, store);
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

    private void refreshAll(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        UICommandBuilder cmd = new UICommandBuilder();

        PlayerGoldData gold = store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerGoldDataType());
        long balance = gold != null ? gold.getGold() : 0;
        cmd.set("#GoldDisplay.TextSpans", Message.raw("Gold: " + balance + "g"));

        populateItemList(cmd, store, ref);
        updateCategoryTabs(cmd);
        updateDetailPanel(cmd, store, ref);
        this.sendUpdate(cmd, new UIEventBuilder(), false);
    }

    private void populateItemList(UICommandBuilder cmd, Store<EntityStore> store, Ref<EntityStore> ref) {
        PlayerGoldData gold = store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerGoldDataType());
        long balance = gold != null ? gold.getGold() : 0;

        if (currentCategory.isArmor()) {
            List<ArmorItem> items = getCategoryArmorItems();
            for (int i = 0; i < MAX_LIST_ITEMS; i++) {
                String label = "#Item" + (i + 1);
                if (i < items.size()) {
                    ArmorItem item = items.get(i);
                    long cost = discountedArmorPrice(item, store, ref);
                    StringBuilder sb = new StringBuilder();
                    if (i == selectedIndex) sb.append(">> ");
                    sb.append(item.getDisplayName()).append("  ").append(cost).append("g");
                    if (balance < cost) sb.append(" (!)");
                    cmd.set(label + ".TextSpans", Message.raw(sb.toString()));
                } else {
                    cmd.set(label + ".TextSpans", Message.raw(""));
                }
            }
        } else {
            List<ShopItem> items = getCategoryItems();
            PlayerEquipmentData equip = store.getComponent(ref,
                    StatsPlugin.getInstance().getPlayerEquipmentDataType());
            String equippedId = equip != null ? equip.getEquippedItem() : "";

            for (int i = 0; i < MAX_LIST_ITEMS; i++) {
                String label = "#Item" + (i + 1);
                if (i < items.size()) {
                    ShopItem item = items.get(i);
                    long cost = discountedPrice(item, store, ref);
                    StringBuilder sb = new StringBuilder();
                    if (i == selectedIndex) sb.append(">> ");
                    sb.append(item.getDisplayName()).append("  ");
                    if (item.getId().equals(equippedId)) {
                        sb.append("[EQUIPPED]");
                    } else {
                        sb.append(cost).append("g");
                        if (balance < cost) sb.append(" (!)");
                    }
                    cmd.set(label + ".TextSpans", Message.raw(sb.toString()));
                } else {
                    cmd.set(label + ".TextSpans", Message.raw(""));
                }
            }
        }
    }

    private void updateCategoryTabs(UICommandBuilder cmd) {
        for (Category cat : Category.values()) {
            String id = "#Cat" + cat.label;
            if (cat == currentCategory) {
                cmd.set(id + ".TextSpans", Message.raw("[ " + cat.label + " ]"));
            } else {
                cmd.set(id + ".TextSpans", Message.raw(cat.label));
            }
        }
    }

    private void updateDetailPanel(UICommandBuilder cmd, Store<EntityStore> store, Ref<EntityStore> ref) {
        if (currentCategory.isArmor()) {
            updateArmorDetailPanel(cmd, store, ref);
            return;
        }

        // Set weapon stat labels
        cmd.set("#StatHeader.TextSpans", Message.raw("- Weapon Stats -"));

        List<ShopItem> items = getCategoryItems();
        if (selectedIndex < 0 || selectedIndex >= items.size()) {
            cmd.set("#DetailName.TextSpans", Message.raw("Select a weapon"));
            cmd.set("#DetailDesc.TextSpans", Message.raw(""));
            cmd.set("#DetailPrice.TextSpans", Message.raw("--"));
            cmd.set("#DetailOwned.TextSpans", Message.raw(""));
            cmd.set("#ValDmg.TextSpans", Message.raw("--"));
            cmd.set("#ValHP.TextSpans", Message.raw("--"));
            cmd.set("#ValCrit.TextSpans", Message.raw("--"));
            cmd.set("#ValStam.TextSpans", Message.raw("--"));
            cmd.set("#ValMana.TextSpans", Message.raw("--"));
            cmd.set("#ValBaseDmg.TextSpans", Message.raw("--"));
            cmd.set("#ValDurability.TextSpans", Message.raw("--"));
            return;
        }

        ShopItem item = items.get(selectedIndex);
        long cost = discountedPrice(item, store, ref);
        PlayerEquipmentData equip = store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerEquipmentDataType());
        String equippedId = equip != null ? equip.getEquippedItem() : "";
        boolean isEquipped = item.getId().equals(equippedId);

        cmd.set("#DetailName.TextSpans", Message.raw(item.getDisplayName()));
        cmd.set("#DetailDesc.TextSpans", Message.raw(item.getDescription()));
        if (cost < item.getPrice()) {
            cmd.set("#DetailPrice.TextSpans", Message.raw(cost + " gold (was " + item.getPrice() + ")"));
        } else {
            cmd.set("#DetailPrice.TextSpans", Message.raw(item.getPrice() + " gold"));
        }

        // Stats
        cmd.set("#ValBaseDmg.TextSpans", Message.raw(
                String.valueOf(item.getBaseDamage())));
        cmd.set("#ValDurability.TextSpans", Message.raw(
                String.valueOf(item.getMaxDurability())));
        cmd.set("#ValDmg.TextSpans", Message.raw(
                "+" + String.format("%.0f%%", item.getBonusDamageMultiplier() * 100)));
        cmd.set("#ValHP.TextSpans", Message.raw(
                item.getBonusHp() > 0 ? "+" + String.format("%.0f", item.getBonusHp()) : "--"));
        cmd.set("#ValCrit.TextSpans", Message.raw(
                item.getBonusCritChance() > 0 ? "+" + String.format("%.0f%%", item.getBonusCritChance() * 100) : "--"));
        cmd.set("#ValStam.TextSpans", Message.raw(
                item.getBonusStamina() > 0 ? "+" + String.format("%.0f", item.getBonusStamina()) : "--"));
        cmd.set("#ValMana.TextSpans", Message.raw(
                item.getBonusMana() > 0 ? "+" + String.format("%.0f", item.getBonusMana()) : "--"));

        // Status
        if (isEquipped) {
            cmd.set("#DetailOwned.TextSpans", Message.raw("Currently Equipped"));
            cmd.set("#BuyBtn.TextSpans", Message.raw("Purchase"));
            cmd.set("#EquipBtn.TextSpans", Message.raw("-- Equipped --"));
        } else {
            cmd.set("#DetailOwned.TextSpans", Message.raw(""));
            cmd.set("#BuyBtn.TextSpans", Message.raw("Purchase"));
            cmd.set("#EquipBtn.TextSpans", Message.raw("Equip"));
        }
    }

    private void updateArmorDetailPanel(UICommandBuilder cmd, Store<EntityStore> store, Ref<EntityStore> ref) {
        // Set armor stat labels
        cmd.set("#StatHeader.TextSpans", Message.raw("- Armor Stats -"));

        List<ArmorItem> items = getCategoryArmorItems();
        if (selectedIndex < 0 || selectedIndex >= items.size()) {
            cmd.set("#DetailName.TextSpans", Message.raw("Select armor"));
            cmd.set("#DetailDesc.TextSpans", Message.raw(""));
            cmd.set("#DetailPrice.TextSpans", Message.raw("--"));
            cmd.set("#DetailOwned.TextSpans", Message.raw(""));
            cmd.set("#ValBaseDmg.TextSpans", Message.raw("--"));
            cmd.set("#ValDurability.TextSpans", Message.raw("--"));
            cmd.set("#ValDmg.TextSpans", Message.raw("--"));
            cmd.set("#ValHP.TextSpans", Message.raw("--"));
            cmd.set("#ValCrit.TextSpans", Message.raw("--"));
            cmd.set("#ValStam.TextSpans", Message.raw("--"));
            cmd.set("#ValMana.TextSpans", Message.raw("--"));
            return;
        }

        ArmorItem item = items.get(selectedIndex);
        long cost = discountedArmorPrice(item, store, ref);

        cmd.set("#DetailName.TextSpans", Message.raw(item.getDisplayName()));
        cmd.set("#DetailDesc.TextSpans", Message.raw(item.getDescription()));
        if (cost < item.getPrice()) {
            cmd.set("#DetailPrice.TextSpans", Message.raw(cost + " gold (was " + item.getPrice() + ")"));
        } else {
            cmd.set("#DetailPrice.TextSpans", Message.raw(item.getPrice() + " gold"));
        }

        // Armor-specific stats: reuse weapon stat labels with armor meanings
        cmd.set("#ValBaseDmg.TextSpans", Message.raw(
                String.format("%.1f%%", item.getDamageResistance() * 100)));
        cmd.set("#ValDurability.TextSpans", Message.raw(
                String.valueOf(item.getMaxDurability())));
        cmd.set("#ValDmg.TextSpans", Message.raw(
                item.getDamageEnhancement() > 0
                        ? "+" + String.format("%.0f%%", item.getDamageEnhancement() * 100) : "--"));
        cmd.set("#ValHP.TextSpans", Message.raw(
                item.getBonusHp() > 0 ? "+" + String.format("%.0f", item.getBonusHp()) : "--"));
        cmd.set("#ValCrit.TextSpans", Message.raw("--"));
        cmd.set("#ValStam.TextSpans", Message.raw("--"));
        cmd.set("#ValMana.TextSpans", Message.raw("--"));

        cmd.set("#DetailOwned.TextSpans", Message.raw(""));
        cmd.set("#BuyBtn.TextSpans", Message.raw("Purchase"));
        cmd.set("#EquipBtn.TextSpans", Message.raw(""));
    }

    public static class ShopAction {
        public static final BuilderCodec<ShopAction> CODEC =
                BuilderCodec.<ShopAction>builder(ShopAction.class, ShopAction::new)
                        .addField(new KeyedCodec<>("Action", Codec.STRING),
                                (d, v) -> d.action = v, d -> d.action)
                        .build();
        private String action;
    }
}
