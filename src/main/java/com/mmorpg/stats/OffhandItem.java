package com.mmorpg.stats;

/**
 * Defines the 6 class-archetype offhand items available in the gold shop (free).
 * Each maps to a primary stat archetype and grants Ability1/2/3 keybinds
 * when held in the offhand/utility slot.
 */
public enum OffhandItem {

    WAR_GAUNTLET("MMORPG_War_Gauntlet", "Ruby of Might",
            "A crimson gemstone that amplifies your melee strikes.",
            "knight", "STR"),

    SHADOW_DAGGER("MMORPG_Shadow_Dagger", "Sapphire of Agility",
            "A deep blue gemstone for swift, precise attacks.",
            "rogue", "DEX"),

    GUARDIAN_SHIELD("MMORPG_Guardian_Shield", "Diamond of Fortitude",
            "A brilliant diamond that bolsters your defense.",
            "brawler", "VIT"),

    ARCANE_SPELLBOOK("MMORPG_Arcane_Spellbook", "Emerald of Wisdom",
            "A shimmering emerald that channels devastating spells.",
            "wizard", "INT"),

    COMMANDERS_BANNER("MMORPG_Commanders_Banner", "Zephyr of Authority",
            "An ethereal gemstone that inspires allies and draws enemy aggro.",
            "protector", "PRE"),

    NATURES_ORB("MMORPG_Natures_Orb", "Gold Ore of Nature",
            "A chunk of mystical gold ore, attuned to nature's healing power.",
            "druid", "ARC");

    private final String gameItemId;
    private final String displayName;
    private final String description;
    private final String classId;      // primary class archetype (single-stat class id)
    private final String primaryStat;  // STR, DEX, VIT, INT, PRE, ARC

    OffhandItem(String gameItemId, String displayName, String description,
                String classId, String primaryStat) {
        this.gameItemId = gameItemId;
        this.displayName = displayName;
        this.description = description;
        this.classId = classId;
        this.primaryStat = primaryStat;
    }

    public String getGameItemId() { return gameItemId; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public String getClassId() { return classId; }
    public String getPrimaryStat() { return primaryStat; }

    /** Find an offhand item by 1-based list index. */
    public static OffhandItem findByIndex(int index) {
        OffhandItem[] items = values();
        if (index < 1 || index > items.length) return null;
        return items[index - 1];
    }

    /** Find an offhand item by its game item ID. */
    public static OffhandItem findById(String id) {
        for (OffhandItem item : values()) {
            if (item.gameItemId.equals(id)) return item;
        }
        return null;
    }

    /** Find the best-fit offhand item for a player's displayed class title. */
    public static OffhandItem findForClass(String displayedTitle) {
        if (displayedTitle == null || "Apprentice".equals(displayedTitle)) return null;
        int sep = displayedTitle.lastIndexOf('_');
        if (sep < 0) return null;
        String classId = displayedTitle.substring(0, sep);

        // Check single-stat classes first (exact match)
        for (OffhandItem item : values()) {
            if (item.classId.equals(classId)) return item;
        }

        // For dual-stat classes, match by primary stat of the class definition
        ClassDefinition classDef = ClassRegistry.get(classId);
        if (classDef != null && classDef.getPrimaryStats().length > 0) {
            String primaryStat = classDef.getPrimaryStats()[0];
            for (OffhandItem item : values()) {
                if (item.primaryStat.equals(primaryStat)) return item;
            }
        }

        return null;
    }
}
