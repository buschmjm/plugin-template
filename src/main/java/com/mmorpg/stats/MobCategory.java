package com.mmorpg.stats;

import java.util.Set;

/**
 * Classifies mobs into combat skill categories based on their nameplate text.
 * Each category maps to a combat SkillType that provides bonus damage.
 */
public enum MobCategory {
    ANIMAL,
    BEAST,
    VOID,
    UNDEAD,
    UNKNOWN;

    private static final Set<String> ANIMAL_KEYWORDS = Set.of(
            "deer", "sheep", "rabbit", "chicken", "cow", "pig", "horse", "ram",
            "pigeon", "bird", "fish", "frog", "duck", "goat", "turkey", "hen",
            "rooster", "lamb", "calf", "foal", "doe", "stag", "kweebec",
            "rambird", "parrot", "cat", "dog", "donkey", "squirrel", "owl"
    );

    private static final Set<String> BEAST_KEYWORDS = Set.of(
            "wolf", "bear", "yeti", "sabertooth", "spider", "scorpion", "worm",
            "snake", "boar", "lion", "tiger", "panther", "raptor", "drake",
            "bat", "rat", "hyena", "crab", "beetle", "wasp", "hornet",
            "trork", "scout", "warrior", "archer", "brute", "bandit", "pirate",
            "goblin", "ogre", "troll", "raider", "marauder"
    );

    private static final Set<String> VOID_KEYWORDS = Set.of(
            "void", "corrupt", "shadow", "abyssal", "eldritch",
            "aberration", "rift", "dimensional", "warp", "chaos",
            "malformed", "twisted", "tainted", "blight"
    );

    private static final Set<String> UNDEAD_KEYWORDS = Set.of(
            "skeleton", "zombie", "ghost", "wraith", "specter", "phantom",
            "lich", "necro", "undead", "revenant", "wight", "banshee",
            "ghoul", "bones", "skeletal", "risen", "cursed", "haunted"
    );

    /**
     * Classify a mob from its nameplate text. Strips any "[LvX] " prefix first.
     * Returns UNKNOWN if no keywords match.
     */
    public static MobCategory classify(String nameplateText) {
        if (nameplateText == null || nameplateText.isEmpty()) return UNKNOWN;

        // Strip [LvX] prefix set by DamageHandler
        String name = nameplateText;
        if (name.startsWith("[Lv")) {
            int end = name.indexOf(']');
            if (end >= 0 && end + 2 <= name.length()) {
                name = name.substring(end + 1).trim();
            }
        }
        String lower = name.toLowerCase();

        // Check in priority order: void > undead > beast > animal
        for (String kw : VOID_KEYWORDS) {
            if (lower.contains(kw)) return VOID;
        }
        for (String kw : UNDEAD_KEYWORDS) {
            if (lower.contains(kw)) return UNDEAD;
        }
        for (String kw : BEAST_KEYWORDS) {
            if (lower.contains(kw)) return BEAST;
        }
        for (String kw : ANIMAL_KEYWORDS) {
            if (lower.contains(kw)) return ANIMAL;
        }
        return UNKNOWN;
    }

    /** Get the combat skill type associated with this mob category. Null for UNKNOWN. */
    public SkillType getSkillType() {
        return switch (this) {
            case ANIMAL -> SkillType.HUNTING;
            case BEAST -> SkillType.SLAYING;
            case VOID -> SkillType.VOIDSLAYING;
            case UNDEAD -> SkillType.WARDING;
            default -> null;
        };
    }
}
