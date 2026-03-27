package com.mmorpg.stats;

/**
 * All profession/gathering skills that can be leveled independently of combat level.
 */
public enum SkillType {
    MINING("Mining", "Pickaxe gathering speed and durability preservation"),
    WOODCUTTING("Woodcutting", "Axe gathering speed and durability preservation"),
    FARMING("Farming", "Hoe and watering can speed and durability preservation"),
    HERBALISM("Herbalism", "Plant and crop harvest speed and durability preservation"),
    CARPENTRY("Carpentry", "Material save chance at the crafting bench"),
    MASONRY("Masonry", "Material save chance at the structural bench"),
    SMITHING("Smithing", "Material save chance at the diagram bench"),
    PROCESSING("Processing", "Material save chance when smelting/refining"),
    EXPLORATION("Exploration", "Map reveal radius from explored chunks"),
    COOKING("Cooking", "Material save chance at the cooking bench"),
    ALCHEMY("Alchemy", "Material save chance at the alchemy bench"),
    HUNTING("Hunting", "Bonus damage vs animals"),
    SLAYING("Slaying", "Bonus damage vs beasts and hostiles"),
    VOIDSLAYING("Void Slaying", "Bonus damage vs void creatures"),
    WARDING("Warding", "Bonus damage vs undead"),
    TINKERING("Tinkering", "Material save chance and faster crafting at construct workbenches");

    private final String displayName;
    private final String description;

    SkillType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }

    /** Look up by case-insensitive name, returns null if not found. */
    public static SkillType fromName(String name) {
        for (SkillType s : values()) {
            if (s.name().equalsIgnoreCase(name) || s.displayName.equalsIgnoreCase(name)) {
                return s;
            }
        }
        return null;
    }
}
