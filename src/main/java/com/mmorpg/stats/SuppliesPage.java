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
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Supplies shop UI — browse and purchase consumables, blocks, ores & services.
 * Replaces the old /shop supplies chat command with a proper interactive page.
 */
public class SuppliesPage extends InteractiveCustomUIPage<SuppliesPage.SuppliesAction> {

    private static final int ITEMS_PER_PAGE = 12;

    private ConsumableItem.Category activeCategory = ConsumableItem.Category.POTIONS;
    private int categoryPage = 0;
    private int selectedIndex = -1; // index within the full category list

    public SuppliesPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, SuppliesAction.CODEC);
    }

    // ── Build ──────────────────────────────────────────────────────────────

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("SuppliesPage.ui");

        PlayerGoldData gold = store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerGoldDataType());

        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnBack", EventData.of("Action", "BACK"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnBuy", EventData.of("Action", "BUY"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnPrev", EventData.of("Action", "PREV"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnNext", EventData.of("Action", "NEXT"), false);

        for (ConsumableItem.Category cat : ConsumableItem.Category.values()) {
            events.addEventBinding(CustomUIEventBindingType.Activating,
                    "#Cat" + cat.name(), EventData.of("Action", "CAT_" + cat.name()), false);
        }
        for (int i = 1; i <= ITEMS_PER_PAGE; i++) {
            events.addEventBinding(CustomUIEventBindingType.Activating,
                    "#Item" + i, EventData.of("Action", "SEL_" + i), false);
        }

        populateView(cmd, gold);
    }

    // ── Events ─────────────────────────────────────────────────────────────

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store, @Nonnull SuppliesAction data) {
        super.handleDataEvent(ref, store, data);
        if (data.action == null) return;

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());
        PlayerGoldData gold = store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerGoldDataType());

        if (data.action.equals("BACK")) {
            if (player != null && pRef != null) {
                player.getPageManager().openCustomPage(ref, store, new MainMenuPage(pRef));
            }
            return;
        }

        if (data.action.equals("BUY")) {
            handleBuy(ref, store, player, pRef, gold);
            return;
        }

        if (data.action.equals("PREV")) {
            if (categoryPage > 0) { categoryPage--; selectedIndex = -1; }
            refresh(ref, store, gold);
            return;
        }

        if (data.action.equals("NEXT")) {
            List<ConsumableItem> items = itemsFor(activeCategory);
            int totalPages = totalPages(items);
            if (categoryPage < totalPages - 1) { categoryPage++; selectedIndex = -1; }
            refresh(ref, store, gold);
            return;
        }

        if (data.action.startsWith("CAT_")) {
            String catName = data.action.substring(4);
            for (ConsumableItem.Category cat : ConsumableItem.Category.values()) {
                if (cat.name().equals(catName)) {
                    activeCategory = cat;
                    categoryPage = 0;
                    selectedIndex = -1;
                    break;
                }
            }
            refresh(ref, store, gold);
            return;
        }

        if (data.action.startsWith("SEL_")) {
            try {
                int slot = Integer.parseInt(data.action.substring(4)); // 1-based
                List<ConsumableItem> items = itemsFor(activeCategory);
                int globalIdx = categoryPage * ITEMS_PER_PAGE + (slot - 1);
                if (globalIdx >= 0 && globalIdx < items.size()) {
                    selectedIndex = globalIdx;
                }
            } catch (NumberFormatException ignored) {}
            refresh(ref, store, gold);
        }
    }

    // ── Buy logic ──────────────────────────────────────────────────────────

    private void handleBuy(Ref<EntityStore> ref, Store<EntityStore> store,
                           Player player, PlayerRef pRef, PlayerGoldData gold) {
        if (gold == null || pRef == null) return;
        List<ConsumableItem> items = itemsFor(activeCategory);
        if (selectedIndex < 0 || selectedIndex >= items.size()) return;
        ConsumableItem item = items.get(selectedIndex);

        if (!gold.canAfford(item.getPrice())) {
            pRef.sendMessage(Message.raw("Not enough gold! Need " + item.getPrice() + "g."));
            refresh(ref, store, gold);
            return;
        }

        gold.spend(item.getPrice());
        store.putComponent(ref, StatsPlugin.getInstance().getPlayerGoldDataType(), gold);
        GoldLedger.log(pRef.getUsername(), "SUPPLY_BUY", -item.getPrice(),
                gold.getGold(), item.getDisplayName());

        if (item == ConsumableItem.STAT_RESET) {
            // Service: reset all stat allocations
            PlayerStatData stats = store.getComponent(ref,
                    StatsPlugin.getInstance().getPlayerStatDataType());
            if (stats != null) {
                stats.setStrength(0);
                stats.setDexterity(0);
                stats.setVitality(0);
                stats.setIntellect(0);
                stats.setPresence(0);
                stats.setArcana(0);
                store.putComponent(ref, StatsPlugin.getInstance().getPlayerStatDataType(), stats);
                StatEffectApplier.applyStats(ref, store, stats);
                pRef.sendMessage(Message.raw("Stats reset! Points refunded. Use /stats to reallocate."));
            }
        } else if (!item.getGameItemId().isEmpty() && player != null) {
            player.giveItem(new ItemStack(item.getGameItemId(), item.getQuantity()), ref, store);
            pRef.sendMessage(Message.raw("Purchased: " + item.getDisplayName()
                    + (item.getQuantity() > 1 ? " x" + item.getQuantity() : "") + "!"));
        }

        refresh(ref, store, store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerGoldDataType()));
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

        List<ConsumableItem> allItems = itemsFor(activeCategory);
        int totalPages = totalPages(allItems);
        int pageStart = categoryPage * ITEMS_PER_PAGE;

        // Category tab labels
        for (ConsumableItem.Category cat : ConsumableItem.Category.values()) {
            String label = cat.getDisplayName().replace("[", "").replace("]", "");
            cmd.set("#Cat" + cat.name() + ".TextSpans",
                    Message.raw(cat == activeCategory ? "[ " + label + " ]" : label));
        }

        // Page nav
        cmd.set("#PageLabel.TextSpans", Message.raw(
                "Page " + (categoryPage + 1) + "/" + Math.max(1, totalPages)));

        // Item list rows
        for (int slot = 1; slot <= ITEMS_PER_PAGE; slot++) {
            int globalIdx = pageStart + (slot - 1);
            if (globalIdx < allItems.size()) {
                ConsumableItem item = allItems.get(globalIdx);
                boolean selected = globalIdx == selectedIndex;
                String prefix = selected ? "> " : "  ";
                String qty = item.getQuantity() > 1 ? " x" + item.getQuantity() : "";
                cmd.set("#Item" + slot + ".TextSpans",
                        Message.raw(prefix + item.getDisplayName() + qty
                                + "  " + item.getPrice() + "g"));
            } else {
                cmd.set("#Item" + slot + ".TextSpans", Message.raw(""));
            }
        }

        // Detail panel
        if (selectedIndex >= 0 && selectedIndex < allItems.size()) {
            ConsumableItem item = allItems.get(selectedIndex);
            cmd.set("#DetailName.TextSpans", Message.raw(item.getDisplayName()));
            cmd.set("#DetailDesc.TextSpans", Message.raw(item.getDescription()));
            cmd.set("#DetailCategory.TextSpans",
                    Message.raw("Category: " + item.getCategory().getDisplayName()
                            .replace("[", "").replace("]", "")));
            cmd.set("#DetailQty.TextSpans",
                    Message.raw(item.isService() ? "Type: Service"
                            : "Quantity: " + item.getQuantity() + "x per purchase"));
            cmd.set("#DetailPrice.TextSpans", Message.raw("Price: " + item.getPrice() + "g"));
            cmd.set("#BtnBuy.TextSpans", Message.raw("Buy  (" + item.getPrice() + "g)"));
        } else {
            cmd.set("#DetailName.TextSpans", Message.raw("Select an item"));
            cmd.set("#DetailDesc.TextSpans", Message.raw(""));
            cmd.set("#DetailCategory.TextSpans", Message.raw(""));
            cmd.set("#DetailQty.TextSpans", Message.raw(""));
            cmd.set("#DetailPrice.TextSpans", Message.raw(""));
            cmd.set("#BtnBuy.TextSpans", Message.raw("Buy"));
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static List<ConsumableItem> itemsFor(ConsumableItem.Category cat) {
        List<ConsumableItem> result = new ArrayList<>();
        for (ConsumableItem item : ConsumableItem.values()) {
            if (item.getCategory() == cat) result.add(item);
        }
        return result;
    }

    private int totalPages(List<ConsumableItem> items) {
        if (items.isEmpty()) return 1;
        return (items.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
    }

    // ── Action codec ───────────────────────────────────────────────────────

    public static class SuppliesAction {
        public static final BuilderCodec<SuppliesAction> CODEC =
                BuilderCodec.<SuppliesAction>builder(SuppliesAction.class, SuppliesAction::new)
                        .addField(new KeyedCodec<>("Action", Codec.STRING),
                                (d, v) -> d.action = v, d -> d.action)
                        .build();
        private String action;
    }
}
