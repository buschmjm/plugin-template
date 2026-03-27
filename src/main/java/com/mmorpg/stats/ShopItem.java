package com.mmorpg.stats;

/**
 * Defines all items available in the shopkeeper's store.
 * Each item has a display name, price in gold, asset reference,
 * and combat stat bonuses applied when equipped.
 */
public enum ShopItem {

    // ======================================
    // -- Swords --
    // ======================================
    STORMBREAKER("stormbreaker", "Stormbreaker",
            "A crackling blade forged in a lightning storm.",
            "", "MMORPG_Stormbreaker",
            750, 10, 0.85f, 0f, 0f, 0f, 0f, 600),

    CRIMSON_WARBLADE("crimson_warblade", "Cobalt Warblade",
            "A blade forged from cobalt ore, cold and unyielding.",
            "", "MMORPG_Crimson_Warblade",
            900, 12, 1.00f, 0f, 0f, 0f, 0f, 800),

    FROSTMOURNE("frostmourne", "Frostmourne",
            "An ancient runeblade radiating frozen death. Drains stamina on hit.",
            "", "MMORPG_Frostmourne",
            1200, 14, 1.20f, 50f, 0f, 0f, 0f, 1200),

    FROZEN_RUNIC_BLADE("frozen_runic_blade", "Frozen Runic Blade",
            "A frost-etched mythic blade humming with ancient runes.",
            "", "Frozen_Runic_Blade",
            1400, 15, 1.35f, 50f, 0f, 0f, 0f, 1400),

    AZURE_FLAMEBLADE("azure_flameblade", "Heavenly Grace",
            "A blessed mithril blade wreathed in divine clouds.",
            "", "MMORPG_Azure_Flameblade",
            1500, 16, 1.50f, 0f, 0f, 0f, 0f, 1500),

    CHRONO_BLADE("chrono_blade", "Chrono Blade",
            "A mythic sword that warps time around its strikes.",
            "", "Chrono_Blade",
            1600, 16, 1.45f, 0f, 0.10f, 0f, 0f, 1600),

    EMERALD_CRYSTAL_BLADE("emerald_crystal", "Emerald Crystal Blade",
            "Carved from a living crystal shard, pulsing with life.",
            "", "MMORPG_Emerald_Crystal_Blade",
            2000, 18, 1.80f, 100f, 0f, 0f, 0f, 2000),

    SAKURA_NO_TSURUGI("sakura_no_tsurugi", "Sakura No Tsurugi",
            "A mythic katana forged under the cherry blossoms.",
            "", "Sakura_No_Tsurugi",
            2500, 20, 1.90f, 0f, 0.12f, 0f, 0f, 2500),

    MYSTICAL_SPELLBLADE("mystical_spellblade", "Mystical Spellblade",
            "A mythic blade channeling devastating arcane energy.",
            "", "Mystical_Spellblade",
            3000, 14, 1.00f, 0f, 0f, 0f, 150f, 3000),

    SPIRIT_CALIBUR("spirit_calibur", "Spirit Calibur",
            "A legendary blade of pure spiritual energy. Immense power.",
            "", "Sword_Spirit_Calibur",
            4000, 24, 2.20f, 150f, 0.05f, 0f, 0f, 4000),

    ANDURIL("anduril", "Anduril, Flame of the West",
            "The legendary reforged blade of kings. Unmatched in power.",
            "", "MMORPG_Anduril",
            5000, 22, 2.50f, 200f, 0f, 0f, 0f, 5000),

    // ======================================
    // -- Axes & Heavy Weapons --
    // ======================================
    BERSERKERS_CLEAVER("berserker_cleaver", "Berserker's Cleaver",
            "A massive axe that sends foes flying on impact.",
            "", "MMORPG_Berserkers_Cleaver",
            1000, 14, 1.30f, 0f, 0f, 0f, 0f, 900),

    DUAL_COBALT_WAR_AXE("dual_cobalt_war_axe", "Dual Cobalt War Axes",
            "Twin cobalt axes for relentless heavy strikes.",
            "", "DualCobaltWarAxe",
            1200, 16, 1.20f, 0f, 0f, 0f, 0f, 1200),

    DEATHS_HARVEST("deaths_harvest", "Death's Harvest",
            "Each swing reaps life from your enemies.",
            "", "MMORPG_Deaths_Harvest",
            1800, 22, 1.60f, 0f, 0f, 0f, 0f, 1800),

    FIRE_STEEL_MACE("fire_steel_mace", "Fire Steel Mace",
            "A burning mace forged in volcanic steel.",
            "", "FireSteelMace",
            2000, 20, 1.50f, 80f, 0f, 0f, 0f, 2000),

    SENTINELS_WILL("sentinels_will", "Sentinel's Will",
            "A mythic polearm wielded by an immortal sentinel.",
            "", "Sentinels_Will",
            2200, 24, 1.70f, 100f, 0f, 0f, 0f, 2200),

    DUAL_ADAMANTITE_WAR_AXE("dual_adam_war_axe", "Dual Adamantite War Axes",
            "Twin adamantite axes of devastating legendary power.",
            "", "DualAdamantiteWarAxe",
            2500, 22, 1.80f, 100f, 0f, 0f, 0f, 2500),

    DEMON_LORD_GREAT_AXE("demon_lord_axe", "Demon Lord Great Axe",
            "A mythic axe torn from the hands of a demon lord.",
            "", "Demon_Lord_Great_Axe",
            2800, 28, 2.00f, 150f, 0f, 0f, 0f, 2800),

    // ======================================
    // -- Daggers --
    // ======================================
    SHADOWFANG_DAGGER("shadowfang", "Shadowfang Dagger",
            "A cursed dagger that strikes from the shadows.",
            "", "MMORPG_Shadowfang_Dagger",
            600, 6, 0.60f, 0f, 0.15f, 0f, 0f, 500),

    CYBER_DAGGERS("cyber_daggers", "Cyber Daggers",
            "Mythic tech-enhanced daggers with blinding speed.",
            "", "Cyber_Daggers",
            1200, 5, 0.80f, 0f, 0.20f, 50f, 0f, 1200),

    BLOOD_MOON_DAGGERS("blood_moon_daggers", "Blood Moon Daggers",
            "Legendary daggers bathed in the light of a blood moon.",
            "", "Daggers_Blood_Moon",
            3500, 10, 1.20f, 0f, 0.25f, 50f, 0f, 3500),

    // ======================================
    // -- Spears --
    // ======================================
    DRAGON_PIERCER("dragon_piercer", "Dragon Piercer",
            "A spear that can punch through dragon scales.",
            "", "MMORPG_Dragon_Piercer",
            1100, 14, 1.10f, 0f, 0f, 50f, 0f, 1000),

    // ======================================
    // -- Staves --
    // ======================================
    ARCHMAGES_SCEPTER("archmage_scepter", "Archmage's Scepter",
            "Channels raw arcane energy into devastating spells.",
            "", "MMORPG_Archmages_Scepter",
            2500, 18, 0.50f, 0f, 0f, 0f, 200f, 2500),

    // ======================================
    // -- Bows --
    // ======================================
    CRUDE_LONGBOW("crude_longbow", "Crude Longbow",
            "A simple but functional long-range bow.",
            "", "Crude_Longbow",
            300, 8, 0.50f, 0f, 0f, 0f, 0f, 300),

    COPPER_LONGBOW("copper_longbow", "Copper Longbow",
            "A longbow reinforced with copper fittings.",
            "", "Copper_Longbow",
            500, 10, 0.65f, 0f, 0f, 0f, 0f, 500),

    IRON_LONGBOW("iron_longbow", "Iron Longbow",
            "A sturdy iron-frame longbow with improved accuracy.",
            "", "Iron_Longbow",
            800, 12, 0.80f, 0f, 0.05f, 0f, 0f, 800),

    LUMINA_WHISPER("lumina_whisper", "Lumina Whisper",
            "A vestige bow that hums with radiant light.",
            "", "Lumina_Whisper",
            2000, 18, 1.40f, 0f, 0.10f, 0f, 50f, 2000),

    MIST_WEAVER("mist_weaver", "Mist Weaver",
            "A vestige bow woven from enchanted mist threads.",
            "", "Mist_Weaver",
            2200, 18, 1.50f, 0f, 0.08f, 0f, 80f, 2200),

    AZURE_VORTEX("azure_vortex", "Azure Vortex",
            "A legendary bow that fires bolts of swirling azure energy.",
            "", "Shortbow_Azure_Vortex",
            3500, 20, 1.80f, 0f, 0.15f, 50f, 0f, 3500),

    // ======================================
    // -- Crossbows --
    // ======================================
    THORIUM_CROSSBOW("thorium_crossbow", "Thorium Crossbow",
            "A rare crossbow built with thorium components.",
            "", "Weapon_Crossbow_Thorium",
            600, 10, 0.70f, 0f, 0.05f, 0f, 0f, 600),

    COBALT_CROSSBOW("cobalt_crossbow", "Cobalt Crossbow",
            "A rare crossbow with a cobalt-reinforced frame.",
            "", "Weapon_Crossbow_Cobalt",
            900, 12, 0.90f, 0f, 0.05f, 0f, 0f, 900),

    ADAMANTITE_CROSSBOW("adamantite_crossbow", "Adamantite Crossbow",
            "A rare crossbow forged from adamantite alloy.",
            "", "Weapon_Crossbow_Adamantite",
            1500, 16, 1.20f, 0f, 0.08f, 0f, 0f, 1500),

    MITHRIL_CROSSBOW("mithril_crossbow", "Mithril Crossbow",
            "An epic crossbow of lightweight mithril. Deadly precision.",
            "", "Weapon_Crossbow_Mithril",
            2000, 18, 1.50f, 0f, 0.10f, 0f, 0f, 2000),

    // ======================================
    // -- Elemental Weapons --
    // ======================================
    ICE_SWORD("ice_sword", "Magic Ice Sword",
            "A legendary blade wreathed in eternal frost.",
            "", "IceSword",
            2500, 18, 1.60f, 0f, 0f, 0f, 80f, 2500),

    FLAME_SABER("flame_saber", "Flame Saber",
            "A legendary saber engulfed in roaring flames.",
            "", "BetterFlameSword",
            2500, 20, 1.70f, 0f, 0f, 50f, 0f, 2500),

    THUNDER_SWORD("thunder_sword", "Thunder Sword",
            "A legendary blade crackling with storm lightning.",
            "", "ThunderSword",
            2500, 20, 1.60f, 0f, 0.10f, 0f, 0f, 2500),

    SERPENT_SWORD("serpent_sword", "Serpent Sword",
            "A legendary venomous blade with a serpentine edge.",
            "", "SerpentSword",
            2800, 22, 1.80f, 80f, 0f, 0f, 0f, 2800),

    VOID_SWORD("void_sword", "Void Sword",
            "A legendary blade forged in the void between worlds.",
            "", "VoidShortSword",
            3200, 22, 1.90f, 0f, 0.10f, 0f, 50f, 3200),

    PURPLE_VOID_SWORD("purple_void_sword", "Purple Void Sword",
            "A legendary void blade pulsing with dark purple energy.",
            "", "PurpleVoidSword",
            3200, 22, 1.90f, 50f, 0.10f, 0f, 0f, 3200),

    // ======================================
    // -- Premium Weapons --
    // ======================================
    HEARSIL_SWORD("hearsil_sword", "Hearsil Sword",
            "An empyreal draconicus blade of immense power. Forged in dragonfire.",
            "", "Weapon/Sword/Hearsil_Sword",
            6000, 26, 2.60f, 200f, 0.10f, 0f, 0f, 5000),

    HALOX_SWORD("halox_sword", "Halox Sword",
            "An empyreal blade crackling with void-touched energy.",
            "", "Weapon/Sword/Halox_Sword",
            6000, 26, 2.50f, 150f, 0.12f, 50f, 0f, 5000),

    SERABICE_SWORD("serabice_sword", "Serabice Sword",
            "An empyreal blade shimmering with celestial radiance.",
            "", "Weapon/Sword/Serabice_Sword",
            6500, 28, 2.70f, 200f, 0.08f, 0f, 100f, 5000),

    JOLYN_BATTLEAXE("jolyn_battleaxe", "Jolyn Battleaxe",
            "An empyreal scythe-axe channeling void whirlwinds.",
            "", "Jolyn_Battleaxe",
            6500, 30, 2.80f, 250f, 0f, 0f, 0f, 5000),

    PHOENIX_DAGGERS("phoenix_daggers", "Phoenix Daggers",
            "Empyreal daggers wreathed in phoenix flame. Blinding speed.",
            "", "Weapon/Daggers/Phoenix_Daggers",
            5500, 12, 1.40f, 0f, 0.30f, 80f, 0f, 4500),

    JON_MACE("jon_mace", "Jon's Mace",
            "An empyreal mace of crushing force and thunderous impact.",
            "", "Weapon/Mace/Jon_Mace",
            6000, 28, 2.40f, 300f, 0f, 0f, 0f, 5000),

    DUAL_RUNE_BLADE("dual_rune_blade", "Dual Rune Blade",
            "Epic dual rune blades crackling with ancient magic.",
            "", "DualRuneBlade",
            5000, 20, 2.00f, 0f, 0.20f, 50f, 50f, 4000),

    GHOST_SWORD("ghost_sword", "Ghost Sword",
            "A spectral blade that phases through armor.",
            "", "GhostSword",
            4500, 18, 1.80f, 0f, 0.15f, 0f, 100f, 3500),

    ZWEIHANDER("zweihander", "Zweihander",
            "A massive two-handed greatsword of devastating cleave.",
            "", "Zweihander",
            5500, 32, 2.60f, 200f, 0f, 0f, 0f, 5000),

    LAHAT_CHEREB("lahat_chereb", "Lahat Chereb",
            "A legendary flaming sword of divine judgment.",
            "", "LahatChereb",
            5500, 24, 2.30f, 150f, 0.10f, 0f, 80f, 4500),

    FIRE_WHIP("fire_whip", "Fire Whip",
            "A blazing chain whip with devastating reach.",
            "", "FireWhip",
            4500, 16, 1.60f, 0f, 0.12f, 100f, 0f, 3500);

    private final String id;
    private final String displayName;
    private final String description;
    private final String assetFile;
    private final String gameItemId;
    private final long price;
    private final int baseDamage;                 // base physical damage per hit
    private final float bonusDamageMultiplier;   // added to melee multiplier
    private final float bonusHp;                 // added to max HP
    private final float bonusCritChance;         // added to crit chance
    private final float bonusStamina;            // added to max stamina
    private final float bonusMana;               // added to max mana
    private final int maxDurability;              // custom max durability (overrides parent)

    ShopItem(String id, String displayName, String description, String assetFile,
             String gameItemId, long price, int baseDamage, float bonusDamageMultiplier, float bonusHp,
             float bonusCritChance, float bonusStamina, float bonusMana, int maxDurability) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.assetFile = assetFile;
        this.gameItemId = gameItemId;
        this.price = price;
        this.baseDamage = baseDamage;
        this.bonusDamageMultiplier = bonusDamageMultiplier;
        this.bonusHp = bonusHp;
        this.bonusCritChance = bonusCritChance;
        this.bonusStamina = bonusStamina;
        this.bonusMana = bonusMana;
        this.maxDurability = maxDurability;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public String getAssetFile() { return assetFile; }
    public String getGameItemId() { return gameItemId; }
    public long getPrice() { return price; }
    public int getBaseDamage() { return baseDamage; }
    public float getBonusDamageMultiplier() { return bonusDamageMultiplier; }
    public float getBonusHp() { return bonusHp; }
    public float getBonusCritChance() { return bonusCritChance; }
    public float getBonusStamina() { return bonusStamina; }
    public float getBonusMana() { return bonusMana; }
    public int getMaxDurability() { return maxDurability; }

    /** Find a ShopItem by its ID (case-insensitive). */
    public static ShopItem findById(String id) {
        for (ShopItem item : values()) {
            if (item.id.equalsIgnoreCase(id)) return item;
        }
        return null;
    }

    /** Find a ShopItem by its 1-based display index. */
    public static ShopItem findByIndex(int index) {
        ShopItem[] items = values();
        if (index < 1 || index > items.length) return null;
        return items[index - 1];
    }
}
