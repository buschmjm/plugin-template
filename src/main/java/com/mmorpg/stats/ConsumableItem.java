package com.mmorpg.stats;

/**
 * Defines consumable/repeatable items in the shop - potions, food, blocks, ores, and services.
 * Unlike ShopItem (weapons, buy-once), these can be purchased repeatedly.
 * Items with a non-empty gameItemId are delivered to the player's inventory.
 * Items with an empty gameItemId are services (stat reset, guild creation) handled by special logic.
 */
public enum ConsumableItem {

    // -- Services --
    STAT_RESET("stat_reset", "Stat Point Reset", "Resets all stat allocations, refunding your points.",
            "", 200, 1, Category.SERVICES),

    // -- Potions --
    HEALTH_POTION_MEDIUM("health_pot_med", "Health Potion (Medium)", "Restores a moderate amount of health.",
            "Potion_Health_Medium", 50, 3, Category.POTIONS),
    HEALTH_POTION_LARGE("health_pot_lg", "Health Potion (Large)", "Restores a large amount of health.",
            "Potion_Health_Large", 100, 3, Category.POTIONS),
    STAMINA_POTION_LARGE("stamina_pot_lg", "Stamina Potion (Large)", "Restores a large amount of stamina.",
            "Potion_Stamina_Large", 100, 3, Category.POTIONS),

    // -- Food & Farming --
    WHEAT("wheat", "Wheat Seeds", "Plantable wheat crop seeds.",
            "Plant_Crop_Wheat_Item", 25, 10, Category.FOOD),
    CARROTS("carrots", "Carrot Seeds", "Plantable carrot crop seeds.",
            "Plant_Crop_Carrot_Item", 40, 10, Category.FOOD),
    POTATOES("potatoes", "Potato Seeds", "Plantable potato crop seeds.",
            "Plant_Crop_Potato_Item", 40, 10, Category.FOOD),
    CORN("corn", "Corn Seeds", "Plantable corn crop seeds.",
            "Plant_Crop_Corn_Item", 40, 10, Category.FOOD),
    TOMATOES("tomatoes", "Tomato Seeds", "Plantable tomato crop seeds.",
            "Plant_Crop_Tomato_Item", 40, 10, Category.FOOD),

    // -- Blocks - Wood Logs --
    OAK_LOG("oak_log", "Oak Logs", "A bundle of oak wood logs.",
            "Block_Wood_Oak_Log", 30, 32, Category.BLOCKS),
    BIRCH_LOG("birch_log", "Birch Logs", "A bundle of birch wood logs.",
            "Block_Wood_Birch_Log", 30, 32, Category.BLOCKS),
    PINE_LOG("pine_log", "Pine Logs", "A bundle of pine wood logs.",
            "Block_Wood_Pine_Log", 30, 32, Category.BLOCKS),
    PALM_LOG("palm_log", "Palm Logs", "A bundle of palm wood logs.",
            "Block_Wood_Palm_Log", 30, 32, Category.BLOCKS),
    CACTUS_LOG("cactus_log", "Cactus Wood", "A bundle of cactus wood.",
            "Block_Wood_Cactus_Log", 30, 32, Category.BLOCKS),

    // -- Blocks - Stone Types --
    STONE("stone", "Stone Blocks", "A stack of sturdy stone blocks.",
            "Block_Stone", 30, 32, Category.BLOCKS),
    SANDSTONE("sandstone", "Sandstone Blocks", "A stack of desert sandstone.",
            "Block_Sandstone", 30, 32, Category.BLOCKS),
    COBBLESTONE("cobblestone", "Cobblestone Blocks", "A stack of rough cobblestone.",
            "Block_Cobblestone", 30, 32, Category.BLOCKS),
    GRANITE("granite", "Granite Blocks", "A stack of dark granite.",
            "Block_Granite", 30, 32, Category.BLOCKS),
    LIMESTONE("limestone", "Limestone Blocks", "A stack of pale limestone.",
            "Block_Limestone", 30, 32, Category.BLOCKS),

    // -- Blocks - Bricks & Decorative --
    CLAY_BRICKS("clay_bricks", "Clay Bricks", "A stack of fired clay bricks.",
            "Block_Brick_Clay", 40, 32, Category.BLOCKS),
    MARBLE("marble", "Marble Blocks", "A stack of polished marble.",
            "Block_Marble", 50, 32, Category.BLOCKS),
    OBSIDIAN("obsidian", "Obsidian Blocks", "A stack of volcanic obsidian glass.",
            "Block_Obsidian", 60, 32, Category.BLOCKS),
    GLASS("glass", "Glass Blocks", "A stack of clear glass panes.",
            "Block_Glass", 35, 32, Category.BLOCKS),

    // -- Blocks - Dirt & Terrain --
    DIRT("dirt", "Dirt Blocks", "A stack of plain dirt.",
            "Block_Dirt", 15, 32, Category.BLOCKS),
    SAND("sand", "Sand Blocks", "A stack of fine sand.",
            "Block_Sand", 15, 32, Category.BLOCKS),
    CLAY("clay", "Clay Blocks", "A stack of raw clay.",
            "Block_Clay", 20, 32, Category.BLOCKS),
    GRAVEL("gravel", "Gravel Blocks", "A stack of loose gravel.",
            "Block_Gravel", 15, 32, Category.BLOCKS),

    // -- Ores & Metals --
    COPPER_ORE("copper_ore", "Copper Ore", "Raw copper ore.",
            "Ore_Copper", 50, 5, Category.MATERIALS),
    IRON_ORE("iron_ore", "Iron Ore", "Raw iron ore.",
            "Ore_Iron", 75, 5, Category.MATERIALS),
    SILVER_ORE("silver_ore", "Silver Ore", "Raw silver ore.",
            "Ore_Silver", 150, 3, Category.MATERIALS),
    GOLD_BAR("gold_bar", "Gold Bar", "A refined gold bar for crafting.",
            "Ingredient_Bar_Gold", 200, 3, Category.MATERIALS),
    COBALT_ORE("cobalt_ore", "Cobalt Ore", "Rare cobalt ore.",
            "Ore_Cobalt", 300, 2, Category.MATERIALS),
    MITHRIL_ORE("mithril_ore", "Mithril Ore", "Very rare mithril ore.",
            "Ore_Mithril", 500, 1, Category.MATERIALS),
    ADAMANTITE_ORE("adamantite_ore", "Adamantite Ore", "Extremely rare adamantite.",
            "Ore_Adamantite", 750, 1, Category.MATERIALS),

    // -- Gems --
    RUBY("ruby", "Ruby", "A precious red gemstone.",
            "Rock_Gem_Ruby", 250, 1, Category.GEMS),
    SAPPHIRE("sapphire", "Sapphire", "A precious blue gemstone.",
            "Rock_Gem_Sapphire", 250, 1, Category.GEMS),
    DIAMOND("diamond", "Diamond", "A brilliant precious diamond.",
            "Rock_Gem_Diamond", 500, 1, Category.GEMS),

    // -- Crafting Materials --
    STORM_LEATHER("storm_leather", "Storm Leather", "Rare leather from storm creatures.",
            "Ingredient_Leather_Storm", 400, 1, Category.MATERIALS),
    VOIDHEART("voidheart", "Voidheart", "A dark pulsing shard of the void.",
            "Ingredient_Voidheart", 600, 1, Category.MATERIALS),
    DARK_COIN("dark_coin", "Dark Coin", "A sinister ancient coin.",
            "Dark_Coin_I", 100, 3, Category.MATERIALS),
    REPAIR_KIT("repair_kit", "Iron Repair Kit", "Repairs damaged tools and weapons.",
            "Tool_Repair_Kit_Iron", 150, 1, Category.MATERIALS);

    public enum Category {
        SERVICES("[Services]"),
        POTIONS("[Potions]"),
        FOOD("[Food]"),
        BLOCKS("[Blocks]"),
        MATERIALS("[Materials]"),
        GEMS("[Gems]");

        private final String displayName;
        Category(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    private final String id;
    private final String displayName;
    private final String description;
    private final String gameItemId;  // empty = service, not a real item
    private final long price;
    private final int quantity;       // how many items given per purchase
    private final Category category;

    ConsumableItem(String id, String displayName, String description,
                   String gameItemId, long price, int quantity, Category category) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.gameItemId = gameItemId;
        this.price = price;
        this.quantity = quantity;
        this.category = category;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public String getGameItemId() { return gameItemId; }
    public long getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public Category getCategory() { return category; }

    /** True if this is a service (stat reset, guild etc.) rather than a real item. */
    public boolean isService() { return gameItemId.isEmpty(); }

    /** Find by 1-based index across all consumables. */
    public static ConsumableItem findByIndex(int index) {
        ConsumableItem[] items = values();
        if (index < 1 || index > items.length) return null;
        return items[index - 1];
    }
}
