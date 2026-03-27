package com.mmorpg.stats;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

/**
 * Player command /shop - browse and buy weapons, supplies & services.
 *   /shop           - list weapons for sale
 *   /shop buy <#>   - buy a weapon by number
 *   /shop equip <#> - equip an owned weapon
 *   /shop unequip   - unequip current weapon
 *   /shop inventory - show owned weapons
 *   /shop supplies  - list potions, food, blocks, ores & services
 *   /shop purchase <#> - buy a supply item or service
 *   /shop reset     - reset stat points (1000g)
 */
public class ShopCommand extends AbstractAsyncCommand {

    /** Compute discounted price for an item based on player's PRE stat + class passives. */
    private static long discountedPrice(long basePrice, Ref<EntityStore> ref, Store<EntityStore> store) {
        PlayerStatData stats = store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerStatDataType());
        int pre = stats != null ? stats.getPresence() : 0;
        float discount = StatCalculation.calculateVendorDiscount(pre);
        // Add class passive shop discount
        PlayerClassData classData = store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerClassDataType());
        if (classData != null) {
            discount += classData.getCachedShopDiscountPercent();
        }
        discount = Math.min(discount, 0.99f);
        return Math.max(1, Math.round(basePrice * (1.0f - discount)));
    }

    // -- Subcommand: /shop buy <number> --
    private static class BuySubcommand extends AbstractAsyncCommand {
        private final RequiredArg<Integer> numberArg;

        BuySubcommand() {
            super("buy", "Buy an item by number");
            this.numberArg = withRequiredArg("number", "Item number from /shop list", ArgTypes.INTEGER);
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);

            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();
            int number = ctx.get(numberArg);

            return CompletableFuture.runAsync(() -> {
                ShopItem item = ShopItem.findByIndex(number);
                if (item == null) {
                    ctx.sendMessage(Message.raw("Invalid item number. Type /shop to see the list."));
                    return;
                }

                var equipType = StatsPlugin.getInstance().getPlayerEquipmentDataType();
                PlayerEquipmentData equipData = store.ensureAndGetComponent(ref, equipType);

                var goldType = StatsPlugin.getInstance().getPlayerGoldDataType();
                PlayerGoldData goldData = store.ensureAndGetComponent(ref, goldType);
                long cost = discountedPrice(item.getPrice(), ref, store);

                if (!goldData.canAfford(cost)) {
                    ctx.sendMessage(Message.raw("Not enough gold! You have " + goldData.getGold()
                            + " but " + item.getDisplayName() + " costs " + cost + "."));
                    return;
                }

                goldData.spend(cost);

                // Give the physical weapon item to the player
                Player player = store.getComponent(ref, Player.getComponentType());
                if (player != null) {
                    ItemStack stack = new ItemStack(item.getGameItemId());
                    if (item.getMaxDurability() > 0) {
                        stack = stack.withMaxDurability(item.getMaxDurability());
                        stack = stack.withDurability(item.getMaxDurability());
                    }
                    player.giveItem(stack, ref, store);
                }

                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef != null) {
                    GoldLedger.log(playerRef.getUsername(), "SHOP_BUY",
                            -cost, goldData.getGold(), item.getDisplayName());
                }

                ctx.sendMessage(Message.raw("* Purchased " + item.getDisplayName()
                        + " for " + cost + " gold!"));
                ctx.sendMessage(Message.raw("Use /shop equip " + number + " to wield it."));
                ctx.sendMessage(Message.raw("Remaining gold: " + goldData.getGold()));
            }, world);
        }
    }

    // -- Subcommand: /shop equip <number> --
    private static class EquipSubcommand extends AbstractAsyncCommand {
        private final RequiredArg<Integer> numberArg;

        EquipSubcommand() {
            super("equip", "Equip an owned item by number");
            this.numberArg = withRequiredArg("number", "Item number from /shop list", ArgTypes.INTEGER);
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);

            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();
            int number = ctx.get(numberArg);

            return CompletableFuture.runAsync(() -> {
                ShopItem item = ShopItem.findByIndex(number);
                if (item == null) {
                    ctx.sendMessage(Message.raw("Invalid item number. Type /shop to see the list."));
                    return;
                }

                var equipType = StatsPlugin.getInstance().getPlayerEquipmentDataType();
                PlayerEquipmentData equipData = store.ensureAndGetComponent(ref, equipType);

                equipData.setEquippedItem(item.getId());

                // Re-apply stats with equipment bonus
                var statType = StatsPlugin.getInstance().getPlayerStatDataType();
                PlayerStatData stats = store.getComponent(ref, statType);
                if (stats != null) {
                    StatEffectApplier.applyStats(ref, store, stats);
                }

                ctx.sendMessage(Message.raw("* Equipped: " + item.getDisplayName()));
                ctx.sendMessage(Message.raw("  +" + formatPercent(item.getBonusDamageMultiplier())
                        + " damage"));
                if (item.getBonusHp() > 0)
                    ctx.sendMessage(Message.raw("  +" + (int) item.getBonusHp() + " HP"));
                if (item.getBonusCritChance() > 0)
                    ctx.sendMessage(Message.raw("  +" + formatPercent(item.getBonusCritChance())
                            + " crit chance"));
                if (item.getBonusStamina() > 0)
                    ctx.sendMessage(Message.raw("  +" + (int) item.getBonusStamina() + " stamina"));
                if (item.getBonusMana() > 0)
                    ctx.sendMessage(Message.raw("  +" + (int) item.getBonusMana() + " mana"));
            }, world);
        }
    }

    // -- Subcommand: /shop unequip --
    private static class UnequipSubcommand extends AbstractAsyncCommand {
        UnequipSubcommand() {
            super("unequip", "Unequip your current weapon");
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);

            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();

            return CompletableFuture.runAsync(() -> {
                var equipType = StatsPlugin.getInstance().getPlayerEquipmentDataType();
                PlayerEquipmentData equipData = store.ensureAndGetComponent(ref, equipType);

                if (equipData.getEquippedItem().isEmpty()) {
                    ctx.sendMessage(Message.raw("No weapon equipped."));
                    return;
                }

                equipData.setEquippedItem("");

                // Re-apply stats without equipment
                var statType = StatsPlugin.getInstance().getPlayerStatDataType();
                PlayerStatData stats = store.getComponent(ref, statType);
                if (stats != null) {
                    StatEffectApplier.applyStats(ref, store, stats);
                }

                ctx.sendMessage(Message.raw("Weapon unequipped."));
            }, world);
        }
    }

    // -- Subcommand: /shop inventory --
    private static class InventorySubcommand extends AbstractAsyncCommand {
        InventorySubcommand() {
            super("inventory", "Show your owned items");
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);

            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();

            return CompletableFuture.runAsync(() -> {
                var equipType = StatsPlugin.getInstance().getPlayerEquipmentDataType();
                PlayerEquipmentData equipData = store.ensureAndGetComponent(ref, equipType);

                ctx.sendMessage(Message.raw("=== Your Equipped Weapon ==="));

                String equippedId = equipData.getEquippedItem();
                if (equippedId.isEmpty()) {
                    ctx.sendMessage(Message.raw("No weapon equipped. Use /shop to browse!"));
                } else {
                    ShopItem item = ShopItem.findById(equippedId);
                    if (item != null) {
                        ctx.sendMessage(Message.raw("  - " + item.getDisplayName()
                                + " (+" + formatPercent(item.getBonusDamageMultiplier())
                                + " dmg) [EQUIPPED]"));
                    }
                }

                ctx.sendMessage(Message.raw("======================"));
            }, world);
        }
    }

    // -- Subcommand: /shop reset --
    private static class ResetSubcommand extends AbstractAsyncCommand {
        ResetSubcommand() {
            super("reset", "Reset all stat points (" + StatConstants.RESET_COST_PER_POINT + "g per point)");
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);

            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();

            return CompletableFuture.runAsync(() -> {
                var goldType = StatsPlugin.getInstance().getPlayerGoldDataType();
                PlayerGoldData goldData = store.ensureAndGetComponent(ref, goldType);

                var statType = StatsPlugin.getInstance().getPlayerStatDataType();
                PlayerStatData stats = store.ensureAndGetComponent(ref, statType);

                int refunded = stats.getSpentPoints();
                if (refunded == 0) {
                    ctx.sendMessage(Message.raw("You have no stat points allocated to reset!"));
                    return;
                }

                long resetCost = (long) refunded * StatConstants.RESET_COST_PER_POINT;
                if (!goldData.canAfford(resetCost)) {
                    ctx.sendMessage(Message.raw("Not enough gold! You have " + goldData.getGold()
                            + " but a stat reset costs " + resetCost + "."));
                    return;
                }

                goldData.spend(resetCost);
                stats.setStrength(0);
                stats.setDexterity(0);
                stats.setVitality(0);
                stats.setIntellect(0);
                stats.setPresence(0);
                stats.setArcana(0);

                StatEffectApplier.applyStats(ref, store, stats);

                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef != null) {
                    GoldLedger.log(playerRef.getUsername(), "SHOP_BUY",
                            -resetCost, goldData.getGold(), "stat reset");
                }

                ctx.sendMessage(Message.raw("* Stats reset! " + refunded + " points refunded."));
                ctx.sendMessage(Message.raw("Available points: " + stats.getAvailablePoints()));
                ctx.sendMessage(Message.raw("Remaining gold: " + goldData.getGold()));
                ctx.sendMessage(Message.raw("Use /allocate <stat> [amount] to redistribute."));
            }, world);
        }
    }

    // -- Subcommand: /shop supplies --
    private static class SuppliesSubcommand extends AbstractAsyncCommand {
        SuppliesSubcommand() {
            super("supplies", "Browse potions, food, blocks, ores & services");
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);

            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();

            return CompletableFuture.runAsync(() -> {
                var goldType = StatsPlugin.getInstance().getPlayerGoldDataType();
                PlayerGoldData goldData = store.ensureAndGetComponent(ref, goldType);

                ctx.sendMessage(Message.raw("=== SUPPLIES & SERVICES ==="));
                ctx.sendMessage(Message.raw("Your gold: " + goldData.getGold()));

                ConsumableItem.Category currentCategory = null;
                ConsumableItem[] items = ConsumableItem.values();
                for (int i = 0; i < items.length; i++) {
                    ConsumableItem item = items[i];
                    if (item.getCategory() != currentCategory) {
                        currentCategory = item.getCategory();
                        ctx.sendMessage(Message.raw(""));
                        ctx.sendMessage(Message.raw(currentCategory.getDisplayName()));
                    }
                    String qty = item.isService() ? "" : " (x" + item.getQuantity() + ")";
                    long cost = discountedPrice(item.getPrice(), ref, store);
                    String priceStr = cost < item.getPrice()
                            ? cost + "g" : item.getPrice() + "g";
                    ctx.sendMessage(Message.raw("  " + (i + 1) + ". " + item.getDisplayName()
                            + " - " + priceStr + qty));
                }

                ctx.sendMessage(Message.raw(""));
                ctx.sendMessage(Message.raw("* Guild Services (use /guild commands)"));
                ctx.sendMessage(Message.raw("  - Guild Creation - " + GuildRegistry.GUILD_CREATION_COST
                        + "g (/guild create <name>)"));
                ctx.sendMessage(Message.raw("  - Guild Donation - any amount (/guild donate <gold>)"));
                ctx.sendMessage(Message.raw(""));
                ctx.sendMessage(Message.raw("/shop purchase <#> - Buy a supply item or service"));
                ctx.sendMessage(Message.raw(""));
                ctx.sendMessage(Message.raw("Want something added to the shop? Message Dad/Jake!"));
                ctx.sendMessage(Message.raw("==========================="));
            }, world);
        }
    }

    // -- Subcommand: /shop purchase <number> --
    private static class PurchaseSubcommand extends AbstractAsyncCommand {
        private final RequiredArg<Integer> numberArg;

        PurchaseSubcommand() {
            super("purchase", "Buy a supply item or service by number");
            this.numberArg = withRequiredArg("number", "Item number from /shop supplies", ArgTypes.INTEGER);
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);

            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();
            int number = ctx.get(numberArg);

            return CompletableFuture.runAsync(() -> {
                ConsumableItem item = ConsumableItem.findByIndex(number);
                if (item == null) {
                    ctx.sendMessage(Message.raw("Invalid item number. Type /shop supplies to see the list."));
                    return;
                }

                var goldType = StatsPlugin.getInstance().getPlayerGoldDataType();
                PlayerGoldData goldData = store.ensureAndGetComponent(ref, goldType);
                long cost = discountedPrice(item.getPrice(), ref, store);

                if (!goldData.canAfford(cost)) {
                    ctx.sendMessage(Message.raw("Not enough gold! You have " + goldData.getGold()
                            + " but " + item.getDisplayName() + " costs " + cost + "."));
                    return;
                }

                // Handle services
                if (item.isService()) {
                    handleService(ctx, ref, store, goldData, item);
                    return;
                }

                // Give real item to player inventory
                Player player = store.getComponent(ref, Player.getComponentType());
                if (player == null) {
                    ctx.sendMessage(Message.raw("Could not access your inventory."));
                    return;
                }

                goldData.spend(cost);
                ItemStack stack = new ItemStack(item.getGameItemId(), item.getQuantity());
                player.giveItem(stack, ref, store);

                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef != null) {
                    GoldLedger.log(playerRef.getUsername(), "SHOP_PURCHASE",
                            -cost, goldData.getGold(), item.getDisplayName());
                }

                ctx.sendMessage(Message.raw("* Purchased " + item.getDisplayName()
                        + " (x" + item.getQuantity() + ") for " + cost + " gold!"));
                ctx.sendMessage(Message.raw("Remaining gold: " + goldData.getGold()));
            }, world);
        }

        private void handleService(CommandContext ctx, Ref<EntityStore> ref,
                                   Store<EntityStore> store, PlayerGoldData goldData,
                                   ConsumableItem item) {
            switch (item) {
                case STAT_RESET -> {
                    var statType = StatsPlugin.getInstance().getPlayerStatDataType();
                    PlayerStatData stats = store.ensureAndGetComponent(ref, statType);
                    int refunded = stats.getSpentPoints();
                    if (refunded == 0) {
                        ctx.sendMessage(Message.raw("You have no stat points allocated to reset!"));
                        return;
                    }
                    long svcResetCost = (long) refunded * StatConstants.RESET_COST_PER_POINT;
                    if (!goldData.canAfford(svcResetCost)) {
                        ctx.sendMessage(Message.raw("Not enough gold! Need " + svcResetCost + "g."));
                        return;
                    }
                    goldData.spend(svcResetCost);
                    stats.setStrength(0);
                    stats.setDexterity(0);
                    stats.setVitality(0);
                    stats.setIntellect(0);
                    stats.setPresence(0);
                    stats.setArcana(0);
                    StatEffectApplier.applyStats(ref, store, stats);
                    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                    if (playerRef != null) {
                        GoldLedger.log(playerRef.getUsername(), "SHOP_PURCHASE",
                                -svcResetCost, goldData.getGold(), "stat reset (supply)");
                    }
                    ctx.sendMessage(Message.raw("* Stats reset! " + refunded + " points refunded."));
                    ctx.sendMessage(Message.raw("Available points: " + stats.getAvailablePoints()));
                    ctx.sendMessage(Message.raw("Remaining gold: " + goldData.getGold()));
                }
                default -> ctx.sendMessage(Message.raw("Unknown service."));
            }
        }
    }

    // -- Subcommand: /shop offhand - browse & buy class offhand items --
    private static class OffhandSubcommand extends AbstractAsyncCommand {
        OffhandSubcommand() {
            super("offhand", "Browse and buy class offhand items");
            addSubCommand(new OffhandBuySubcommand());
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);

            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();

            return CompletableFuture.runAsync(() -> {
                var equipType = StatsPlugin.getInstance().getPlayerEquipmentDataType();
                PlayerEquipmentData equipData = store.ensureAndGetComponent(ref, equipType);

                ctx.sendMessage(Message.raw("=== CLASS OFFHAND ITEMS ==="));
                ctx.sendMessage(Message.raw("Free! Each class has a unique offhand item."));
                ctx.sendMessage(Message.raw("Equip one to use Ability keybinds (Q/E/R)."));
                ctx.sendMessage(Message.raw(""));

                String ownedId = equipData.getOwnedOffhand();
                OffhandItem[] items = OffhandItem.values();
                for (int i = 0; i < items.length; i++) {
                    OffhandItem item = items[i];
                    String owned = item.getGameItemId().equals(ownedId) ? " [OWNED]" : "";
                    ctx.sendMessage(Message.raw("  " + (i + 1) + ". " + item.getDisplayName()
                            + " (" + item.getPrimaryStat() + " class)" + owned));
                    ctx.sendMessage(Message.raw("     " + item.getDescription()));
                }

                ctx.sendMessage(Message.raw(""));
                ctx.sendMessage(Message.raw("/shop offhand buy <#> - Get a class offhand item"));
                ctx.sendMessage(Message.raw("==========================="));
            }, world);
        }
    }

    // -- Subcommand: /shop offhand buy <number> --
    private static class OffhandBuySubcommand extends AbstractAsyncCommand {
        private final RequiredArg<Integer> numberArg;

        OffhandBuySubcommand() {
            super("buy", "Get a class offhand item");
            this.numberArg = withRequiredArg("number", "Item number from /shop offhand", ArgTypes.INTEGER);
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);

            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();
            int number = ctx.get(numberArg);

            return CompletableFuture.runAsync(() -> {
                OffhandItem item = OffhandItem.findByIndex(number);
                if (item == null) {
                    ctx.sendMessage(Message.raw("Invalid item number. Type /shop offhand to see the list."));
                    return;
                }

                var equipType = StatsPlugin.getInstance().getPlayerEquipmentDataType();
                PlayerEquipmentData equipData = store.ensureAndGetComponent(ref, equipType);

                // Charge 25 gold (unless granted free by stat unlock)
                int OFFHAND_COST = 25;
                var goldType = StatsPlugin.getInstance().getPlayerGoldDataType();
                PlayerGoldData goldData = store.ensureAndGetComponent(ref, goldType);
                if (!goldData.canAfford(OFFHAND_COST)) {
                    ctx.sendMessage(Message.raw("You need " + OFFHAND_COST
                            + " gold! You have " + goldData.getGold() + "."));
                    return;
                }

                goldData.spend(OFFHAND_COST);
                store.putComponent(ref, goldType, goldData);
                equipData.setOwnedOffhand(item.getGameItemId());

                // Place the item directly in the utility (offhand) slot
                Player player = store.getComponent(ref, Player.getComponentType());
                if (player != null) {
                    ItemStack stack = new ItemStack(item.getGameItemId());
                    var utilityContainer = player.getInventory().getUtility();
                    utilityContainer.removeItemStackFromSlot((short) 0);
                    utilityContainer.setItemStackForSlot((short) 0, stack);
                    player.getInventory().setActiveUtilitySlot(ref, (byte) 0, store);
                }

                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef != null) {
                    GoldLedger.log(playerRef.getUsername(), "SHOP_OFFHAND",
                            -OFFHAND_COST, goldData.getGold(), item.getDisplayName());
                }

                ctx.sendMessage(Message.raw("* Acquired: " + item.getDisplayName()
                        + "! (-" + OFFHAND_COST + "g)"));
                ctx.sendMessage(Message.raw("It's been placed in your offhand slot."));
                ctx.sendMessage(Message.raw("Bind skills with /skill bind <1-3> <id>"));
                ctx.sendMessage(Message.raw("Then use Ability keybinds (Q/E/R) to cast!"));
            }, world);
        }
    }

    public ShopCommand() {
        super("shop", "Browse and buy weapons, supplies & services");
        addSubCommand(new BuySubcommand());
        addSubCommand(new EquipSubcommand());
        addSubCommand(new UnequipSubcommand());
        addSubCommand(new InventorySubcommand());
        addSubCommand(new ResetSubcommand());
        addSubCommand(new SuppliesSubcommand());
        addSubCommand(new PurchaseSubcommand());
        addSubCommand(new OffhandSubcommand());
    }

    @Override
    protected boolean canGeneratePermission() { return false; }

    @Nonnull
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
        // Default: show the shop listing
        Ref<EntityStore> ref = ctx.senderAsPlayerRef();
        if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        return CompletableFuture.runAsync(() -> {
            var goldType = StatsPlugin.getInstance().getPlayerGoldDataType();
            PlayerGoldData goldData = store.ensureAndGetComponent(ref, goldType);

            var equipType = StatsPlugin.getInstance().getPlayerEquipmentDataType();
            PlayerEquipmentData equipData = store.ensureAndGetComponent(ref, equipType);

            ctx.sendMessage(Message.raw("=== SHOPKEEPER'S WARES ==="));
            ctx.sendMessage(Message.raw("Your gold: " + goldData.getGold()));
            ctx.sendMessage(Message.raw(""));

            ShopItem[] items = ShopItem.values();
            for (int i = 0; i < items.length; i++) {
                ShopItem item = items[i];
                String equipped = equipData.getEquippedItem();
                String stats = "+" + formatPercent(item.getBonusDamageMultiplier()) + " dmg";
                if (item.getBonusHp() > 0) stats += ", +" + (int) item.getBonusHp() + " HP";
                if (item.getBonusCritChance() > 0)
                    stats += ", +" + formatPercent(item.getBonusCritChance()) + " crit";
                if (item.getBonusStamina() > 0)
                    stats += ", +" + (int) item.getBonusStamina() + " stam";
                if (item.getBonusMana() > 0)
                    stats += ", +" + (int) item.getBonusMana() + " mana";

                String marker = item.getId().equals(equipped) ? " [EQUIPPED]" : "";
                long cost = discountedPrice(item.getPrice(), ref, store);
                String priceStr = cost < item.getPrice()
                        ? cost + "g (was " + item.getPrice() + "g)"
                        : item.getPrice() + "g";
                ctx.sendMessage(Message.raw(
                        (i + 1) + ". " + item.getDisplayName()
                        + " - " + priceStr + " (" + stats + ")" + marker));
            }

            ctx.sendMessage(Message.raw(""));

            // -- Class Offhand Items section --
            ctx.sendMessage(Message.raw("=== CLASS OFFHAND ITEMS (25g) ==="));
            ctx.sendMessage(Message.raw("One-time choice! Grants ability keybinds (Q/E/R)."));
            if (equipData.hasOffhand()) {
                OffhandItem ownedOff = equipData.getOwnedOffhandItem();
                ctx.sendMessage(Message.raw("  * " + (ownedOff != null ? ownedOff.getDisplayName() : "Unknown")
                        + " [CLAIMED]"));
            } else {
                OffhandItem[] offItems = OffhandItem.values();
                for (int i = 0; i < offItems.length; i++) {
                    OffhandItem off = offItems[i];
                    ctx.sendMessage(Message.raw("  " + (i + 1) + ". " + off.getDisplayName()
                            + " (" + off.getPrimaryStat() + " class)"));
                }
                ctx.sendMessage(Message.raw("/shop offhand - View details & claim"));
            }

            ctx.sendMessage(Message.raw(""));
            ctx.sendMessage(Message.raw("Type /shop supplies for potions, food, blocks & services"));
            ctx.sendMessage(Message.raw(""));
            ctx.sendMessage(Message.raw("/shop buy <#> - Purchase a weapon"));
            ctx.sendMessage(Message.raw("/shop equip <#> - Equip an owned weapon"));
            ctx.sendMessage(Message.raw("/shop unequip - Remove equipped weapon"));
            ctx.sendMessage(Message.raw("/shop inventory - View owned weapons"));
            ctx.sendMessage(Message.raw("/shop supplies - Potions, food, blocks & services"));
            ctx.sendMessage(Message.raw("/shop purchase <#> - Buy a supply item"));
            ctx.sendMessage(Message.raw("/shop offhand - Class offhand items (25g)"));
            ctx.sendMessage(Message.raw("/shop reset - Reset stat points (200g)"));
            ctx.sendMessage(Message.raw(""));
            ctx.sendMessage(Message.raw("Want something added to the shop? Message Dad/Jake!"));
            ctx.sendMessage(Message.raw("=========================="));
        }, world);
    }

    /** Format a float multiplier as a percentage string (e.g. 1.5f -> "150%"). */
    static String formatPercent(float value) {
        return (int) (value * 100) + "%";
    }
}
