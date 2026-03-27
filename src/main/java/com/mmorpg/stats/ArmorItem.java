package com.mmorpg.stats;

/**
 * Defines all armor pieces available in the shop.
 * Each piece has defensive stats (damage resistance, bonus HP) and a price.
 * Organized by slot: Head, Chest, Hands, Legs.
 */
public enum ArmorItem {

    // ======================================
    // -- Head --
    // ======================================
    FROSTWARDEN_HEAD("frostwarden_head", "FrostWarden Helm",
            "A frost-forged cobalt helm radiating icy power.",
            "Armor_FrostWarden_Head", Slot.HEAD,
            500, 0.1152f, 22f, 0f, 120),

    DUNEWALKER_HEAD("dunewalker_head", "DuneWalker Helm",
            "A desert-hardened thorium helm worn by dune nomads.",
            "Armor_DuneWalker_Head", Slot.HEAD,
            550, 0.1152f, 22f, 0f, 120),

    AQUAKNIGHT_HEAD("aquaknight_head", "AquaKnight Helm",
            "A deep-sea helm infused with ocean magic.",
            "Armor_AquaKnight_Head", Slot.HEAD,
            500, 0.1152f, 22f, 0f, 120),

    LAVAKNIGHT_HEAD("lavaknight_head", "LavaKnight Helm",
            "A volcanic helm forged in molten adamantite.",
            "Armor_LavaKnight_Head", Slot.HEAD,
            600, 0.1350f, 24f, 0f, 140),

    SAKURA_HEAD("sakura_head", "Sakura Helm",
            "A crystalwood helm blessed by cherry blossom spirits.",
            "Armor_Sakura_Head", Slot.HEAD,
            550, 0.1152f, 22f, 0.05f, 120),

    GRAVEKNIGHT_HEAD("graveknight_head", "GraveKnight Helm",
            "A cursed helm from the depths of a forgotten crypt.",
            "Armor_GraveKnight_Head", Slot.HEAD,
            450, 0.10f, 18f, 0f, 100),

    DEMON_HEAD("demon_head", "Demon Helm",
            "A fearsome adamantite helm wreathed in dark fire.",
            "DemonHelm", Slot.HEAD,
            1000, 0.144f, 26f, 0.06f, 150),

    ROOK_HEAD("rook_head", "Rook Helm",
            "A battle-worn knight's helm with a sturdy visor.",
            "RookRustyHelm", Slot.HEAD,
            800, 0.12f, 20f, 0f, 130),

    WARDEN_HEAD("warden_head", "Warden Helm",
            "A sentinel's helm designed for long patrols.",
            "WardenHelm", Slot.HEAD,
            850, 0.13f, 22f, 0f, 140),

    DIAMOND_HEAD("diamond_head", "Diamond Helm",
            "A legendary helm crafted from refined diamond. Unmatched defense.",
            "Armor_Diamond_Head", Slot.HEAD,
            1400, 0.22f, 28f, 0.08f, 240),

    EMERALD_HEAD("emerald_head", "Emerald Helm",
            "A legendary helm of living emerald crystal.",
            "Armor_Emerald_Head", Slot.HEAD,
            1400, 0.22f, 28f, 0.07f, 240),

    // ======================================
    // -- Chest --
    // ======================================
    FROSTWARDEN_CHEST("frostwarden_chest", "FrostWarden Chestplate",
            "Cobalt-forged plate armor reinforced with frost runes.",
            "Armor_FrostWarden_Chest", Slot.CHEST,
            600, 0.1152f, 22f, 0.06f, 120),

    DUNEWALKER_CHEST("dunewalker_chest", "DuneWalker Chestplate",
            "Thorium-scaled chest armor for desert survival.",
            "Armor_DuneWalker_Chest", Slot.CHEST,
            650, 0.1152f, 22f, 0f, 120),

    AQUAKNIGHT_CHEST("aquaknight_chest", "AquaKnight Chestplate",
            "Deep-sea plate armor with ocean enchantments.",
            "Armor_AquaKnight_Chest", Slot.CHEST,
            600, 0.1152f, 22f, 0f, 120),

    LAVAKNIGHT_CHEST("lavaknight_chest", "LavaKnight Chestplate",
            "Volcanic plate armor forged in molten adamantite.",
            "Armor_LavaKnight_Chest", Slot.CHEST,
            700, 0.1350f, 24f, 0f, 140),

    SAKURA_CHEST("sakura_chest", "Sakura Chestplate",
            "Crystalwood armor blessed with light-enhancing magic.",
            "Armor_Sakura_Chest", Slot.CHEST,
            650, 0.1152f, 22f, 0.05f, 120),

    DEMON_CHEST("demon_chest", "Demon Chestplate",
            "Dark adamantite plate wreathed in hellfire.",
            "DemonChestplate", Slot.CHEST,
            1200, 0.144f, 26f, 0.10f, 150),

    ROOK_CHEST("rook_chest", "Rook Chestplate",
            "Heavy knight's plate armor, battle-tested and reliable.",
            "RookChestRusty", Slot.CHEST,
            900, 0.12f, 20f, 0f, 130),

    WARDEN_CHEST("warden_chest", "Warden Chestplate",
            "A sentinel's armored vest for extended patrols.",
            "WardenChest", Slot.CHEST,
            950, 0.13f, 22f, 0f, 140),

    DIAMOND_CHEST("diamond_chest", "Diamond Chestplate",
            "Legendary diamond-reinforced plate. Supreme protection.",
            "Armor_Diamond_Chest", Slot.CHEST,
            1600, 0.22f, 28f, 0.08f, 240),

    EMERALD_CHEST("emerald_chest", "Emerald Chestplate",
            "Legendary emerald crystal armor radiating nature's power.",
            "Armor_Emerald_Chest", Slot.CHEST,
            1600, 0.22f, 28f, 0.07f, 240),

    // ======================================
    // -- Hands --
    // ======================================
    FROSTWARDEN_HANDS("frostwarden_hands", "FrostWarden Gauntlets",
            "Frost-forged cobalt gauntlets with icy grip.",
            "Armor_FrostWarden_Hands", Slot.HANDS,
            400, 0.1152f, 22f, 0f, 120),

    DUNEWALKER_HANDS("dunewalker_hands", "DuneWalker Gauntlets",
            "Thorium-plated gauntlets hardened by desert heat.",
            "Armor_DuneWalker_Hands", Slot.HANDS,
            450, 0.1152f, 22f, 0f, 120),

    AQUAKNIGHT_HANDS("aquaknight_hands", "AquaKnight Gauntlets",
            "Ocean-enchanted gauntlets for underwater combat.",
            "Armor_AquaKnight_Hands", Slot.HANDS,
            400, 0.1152f, 22f, 0f, 120),

    LAVAKNIGHT_HANDS("lavaknight_hands", "LavaKnight Gauntlets",
            "Volcanic gauntlets radiating intense heat.",
            "Armor_LavaKnight_Hands", Slot.HANDS,
            500, 0.1350f, 24f, 0f, 140),

    SAKURA_HANDS("sakura_hands", "Sakura Gauntlets",
            "Crystalwood gauntlets with light-enhancing runes.",
            "Armor_Sakura_Hands", Slot.HANDS,
            450, 0.1152f, 22f, 0.05f, 120),

    DEMON_HANDS("demon_hands", "Demon Gauntlets",
            "Dark adamantite gauntlets crackling with hellfire.",
            "DemonGauntlets", Slot.HANDS,
            850, 0.144f, 26f, 0.06f, 150),

    ROOK_HANDS("rook_hands", "Rook Gauntlets",
            "Heavy knight's gauntlets with reinforced knuckles.",
            "RookGauntletsRusty", Slot.HANDS,
            700, 0.12f, 20f, 0f, 130),

    WARDEN_HANDS("warden_hands", "Warden Sleeves",
            "Armored sleeves for a warden's patrol gear.",
            "WardenSleeves", Slot.HANDS,
            750, 0.13f, 22f, 0f, 140),

    DIAMOND_HANDS("diamond_hands", "Diamond Gauntlets",
            "Legendary diamond gauntlets. Unbreakable grip.",
            "Armor_Diamond_Hands", Slot.HANDS,
            1200, 0.22f, 28f, 0.08f, 240),

    EMERALD_HANDS("emerald_hands", "Emerald Gauntlets",
            "Legendary emerald gauntlets pulsing with nature magic.",
            "Armor_Emerald_Hands", Slot.HANDS,
            1200, 0.22f, 28f, 0.07f, 240),

    // ======================================
    // -- Legs --
    // ======================================
    FROSTWARDEN_LEGS("frostwarden_legs", "FrostWarden Greaves",
            "Frost-forged cobalt leg armor with icy enchantments.",
            "Armor_FrostWarden_Legs", Slot.LEGS,
            500, 0.1152f, 22f, 0f, 120),

    DUNEWALKER_LEGS("dunewalker_legs", "DuneWalker Greaves",
            "Thorium-plated leg armor built for desert marches.",
            "Armor_DuneWalker_Legs", Slot.LEGS,
            550, 0.1152f, 22f, 0f, 120),

    AQUAKNIGHT_LEGS("aquaknight_legs", "AquaKnight Greaves",
            "Ocean-enchanted leg armor for deep-sea combat.",
            "Armor_AquaKnight_Legs", Slot.LEGS,
            500, 0.1152f, 22f, 0f, 120),

    LAVAKNIGHT_LEGS("lavaknight_legs", "LavaKnight Greaves",
            "Volcanic leg armor radiating ember heat.",
            "Armor_LavaKnight_Legs", Slot.LEGS,
            600, 0.1350f, 24f, 0f, 140),

    SAKURA_LEGS("sakura_legs", "Sakura Greaves",
            "Crystalwood leg armor with light-enhancing magic.",
            "Armor_Sakura_Legs", Slot.LEGS,
            550, 0.1152f, 22f, 0.05f, 120),

    DEMON_LEGS("demon_legs", "Demon Leggings",
            "Dark adamantite leggings wreathed in hellfire.",
            "DemonLeggings", Slot.LEGS,
            1000, 0.144f, 26f, 0.06f, 150),

    ROOK_LEGS("rook_legs", "Rook Boots",
            "Heavy knight's boots with reinforced soles.",
            "RookBootsRusty", Slot.LEGS,
            700, 0.12f, 20f, 0f, 130),

    WARDEN_LEGS("warden_legs", "Warden Leggings",
            "Armored leggings for a warden's patrol gear.",
            "WardenLeggings", Slot.LEGS,
            750, 0.13f, 22f, 0f, 140),

    DIAMOND_LEGS("diamond_legs", "Diamond Greaves",
            "Legendary diamond leg armor. Supreme protection.",
            "Armor_Diamond_Legs", Slot.LEGS,
            1400, 0.22f, 28f, 0.08f, 240),

    EMERALD_LEGS("emerald_legs", "Emerald Greaves",
            "Legendary emerald leg armor radiating nature's power.",
            "Armor_Emerald_Legs", Slot.LEGS,
            1400, 0.22f, 28f, 0.07f, 240);

    public enum Slot {
        HEAD("Helmets"), CHEST("Chest"), HANDS("Gloves"), LEGS("Legs");
        private final String displayName;
        Slot(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    private final String id;
    private final String displayName;
    private final String description;
    private final String gameItemId;
    private final Slot slot;
    private final long price;
    private final float damageResistance;   // multiplicative % physical/projectile resistance
    private final float bonusHp;            // additive flat HP
    private final float damageEnhancement;  // multiplicative % damage class enhancement
    private final int maxDurability;

    ArmorItem(String id, String displayName, String description, String gameItemId,
              Slot slot, long price, float damageResistance, float bonusHp,
              float damageEnhancement, int maxDurability) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.gameItemId = gameItemId;
        this.slot = slot;
        this.price = price;
        this.damageResistance = damageResistance;
        this.bonusHp = bonusHp;
        this.damageEnhancement = damageEnhancement;
        this.maxDurability = maxDurability;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public String getGameItemId() { return gameItemId; }
    public Slot getSlot() { return slot; }
    public long getPrice() { return price; }
    public float getDamageResistance() { return damageResistance; }
    public float getBonusHp() { return bonusHp; }
    public float getDamageEnhancement() { return damageEnhancement; }
    public int getMaxDurability() { return maxDurability; }

    /** Find an ArmorItem by its ID (case-insensitive). */
    public static ArmorItem findById(String id) {
        for (ArmorItem item : values()) {
            if (item.id.equalsIgnoreCase(id)) return item;
        }
        return null;
    }
}
