package com.mmorpg.stats;

import java.util.*;

/**
 * Registry of all 21 class definitions.
 * Naming convention: Apprentice X, Acolyte X, X, Grand X, [Unique T5 Name].
 * Passives listed are the mechanically implementable subset; full descriptions are in passiveDesc.
 */
public final class ClassRegistry {

    private static final Map<String, ClassDefinition> classes = new LinkedHashMap<>();

    private ClassRegistry() {}

    public static ClassDefinition get(String id) { return classes.get(id); }
    public static Collection<ClassDefinition> getAll() { return Collections.unmodifiableCollection(classes.values()); }
    public static boolean exists(String id) { return classes.containsKey(id); }

    // -- Helpers --

    private static List<PassiveEffect> p(PassiveEffect... effects) { return List.of(effects); }
    private static PassiveEffect pe(PassiveType t, float v) { return new PassiveEffect(t, v); }

    private static ClassTier t(int tier, String className, String t5Name,
                               int threshold, String passiveDesc,
                               List<PassiveEffect> passives) {
        String name = switch (tier) {
            case 1 -> "Apprentice " + className;
            case 2 -> "Acolyte " + className;
            case 3 -> className;
            case 4 -> "Grand " + className;
            case 5 -> t5Name;
            default -> className;
        };
        return new ClassTier(tier, name, threshold, passiveDesc, passives);
    }

    private static void reg(ClassDefinition def) { classes.put(def.getId(), def); }

    // -- Static init --

    static {
        registerSingleStatClasses();
        registerDualStatClasses();
    }

    // ========================================================
    //  SINGLE-STAT CLASSES (6)
    // ========================================================

    private static void registerSingleStatClasses() {
        // -- Knight (STR) -- "I am an unstoppable melee force"
        // Cumulative T5: +65% melee, +20% crit multiplier, +5% DR
        reg(new ClassDefinition("knight", "Knight", "Combat",
                new String[]{"STR"}, false, List.of(
                t(1, "Knight", "Worldbreaker", 12,
                        "Melee damage +10%",
                        p(pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.10f))),
                t(2, "Knight", "Worldbreaker", 30,
                        "Melee damage +10%, Crit damage +20%",
                        p(pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.10f),
                          pe(PassiveType.CRIT_MULTIPLIER_PERCENT, 0.20f))),
                t(3, "Knight", "Worldbreaker", 60,
                        "Melee damage +15%. After knockback, next hit deals bonus damage",
                        p(pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.15f))),
                t(4, "Knight", "Worldbreaker", 120,
                        "Melee damage +10%, Damage resistance +5%",
                        p(pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.10f),
                          pe(PassiveType.DAMAGE_REDUCTION_PERCENT, 0.05f))),
                t(5, "Knight", "Worldbreaker", 150,
                        "Melee damage +20%. Knockback chains to nearby enemies",
                        p(pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.20f)))
        )));

        // -- Rogue (DEX) -- "I strike with lethal precision"
        // Cumulative T5: +60% crit multiplier, +20% melee
        reg(new ClassDefinition("rogue", "Rogue", "Combat",
                new String[]{"DEX"}, false, List.of(
                t(1, "Rogue", "Phantom", 12,
                        "Crit damage +10%, Melee damage +5%",
                        p(pe(PassiveType.CRIT_MULTIPLIER_PERCENT, 0.10f),
                          pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.05f))),
                t(2, "Rogue", "Phantom", 30,
                        "Crit damage +10%, Sneak attack damage +5%",
                        p(pe(PassiveType.CRIT_MULTIPLIER_PERCENT, 0.10f),
                          pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.05f))),
                t(3, "Rogue", "Phantom", 60,
                        "Crit damage +10%. Sneak detection radius reduced",
                        p(pe(PassiveType.CRIT_MULTIPLIER_PERCENT, 0.10f))),
                t(4, "Rogue", "Phantom", 120,
                        "Crit damage +16%, Melee damage +5%",
                        p(pe(PassiveType.CRIT_MULTIPLIER_PERCENT, 0.16f),
                          pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.05f))),
                t(5, "Rogue", "Phantom", 150,
                        "Crit damage +14%, Melee damage +5%",
                        p(pe(PassiveType.CRIT_MULTIPLIER_PERCENT, 0.14f),
                          pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.05f)))
        )));

        // -- Brawler (VIT) -- "I am an immovable wall"
        // Cumulative T5: +20% max HP, +16% DR, 8% negate, +8% regen, +25% low HP resist
        reg(new ClassDefinition("brawler", "Brawler", "Combat",
                new String[]{"VIT"}, false, List.of(
                t(1, "Brawler", "Golem", 12,
                        "Max HP +10%",
                        p(pe(PassiveType.MAX_HP_PERCENT, 0.10f))),
                t(2, "Brawler", "Golem", 30,
                        "Incoming damage reduced 5%",
                        p(pe(PassiveType.DAMAGE_REDUCTION_PERCENT, 0.05f))),
                t(3, "Brawler", "Golem", 60,
                        "8% chance to negate damage entirely, DR +3%",
                        p(pe(PassiveType.DAMAGE_NEGATE_CHANCE, 0.08f),
                          pe(PassiveType.DAMAGE_REDUCTION_PERCENT, 0.03f))),
                t(4, "Brawler", "Golem", 120,
                        "HP regen +8%, Max HP +10%",
                        p(pe(PassiveType.HP_REGEN_PERCENT, 0.08f),
                          pe(PassiveType.MAX_HP_PERCENT, 0.10f))),
                t(5, "Brawler", "Golem", 150,
                        "Below 25% HP: damage resistance +25%, DR +8%",
                        p(pe(PassiveType.LOW_HP_RESIST_PERCENT, 0.25f),
                          pe(PassiveType.DAMAGE_REDUCTION_PERCENT, 0.08f)))
        )));

        // -- Wizard (INT) -- "My spells devastate everything"
        // Cumulative T5: +75% spell, +8% mana, +10% mana regen
        reg(new ClassDefinition("wizard", "Wizard", "Magic",
                new String[]{"INT"}, false, List.of(
                t(1, "Wizard", "Sage", 12,
                        "Spell damage +10%",
                        p(pe(PassiveType.SPELL_DAMAGE_PERCENT, 0.10f))),
                t(2, "Wizard", "Sage", 30,
                        "Spell damage +10%, Mana pool +8%",
                        p(pe(PassiveType.SPELL_DAMAGE_PERCENT, 0.10f),
                          pe(PassiveType.MANA_POOL_PERCENT, 0.08f))),
                t(3, "Wizard", "Sage", 60,
                        "Spell damage +15%. 10% chance spells don't consume mana",
                        p(pe(PassiveType.SPELL_DAMAGE_PERCENT, 0.15f))),
                t(4, "Wizard", "Sage", 120,
                        "Spell damage +15%, Mana regen +10%",
                        p(pe(PassiveType.SPELL_DAMAGE_PERCENT, 0.15f),
                          pe(PassiveType.MANA_REGEN_PERCENT, 0.10f))),
                t(5, "Wizard", "Sage", 150,
                        "Spell damage +25%. Every 5th spell deals double damage",
                        p(pe(PassiveType.SPELL_DAMAGE_PERCENT, 0.25f)))
        )));

        // -- Protector (PRE) -- "I draw all threats and shield my allies"
        // Cumulative T5: +55% miss, +28% DR, +8% shop discount
        reg(new ClassDefinition("protector", "Protector", "Support",
                new String[]{"PRE"}, false, List.of(
                t(1, "Protector", "Sovereign", 12,
                        "Miss chance +20%",
                        p(pe(PassiveType.MISS_CHANCE_PERCENT, 0.20f))),
                t(2, "Protector", "Sovereign", 30,
                        "Miss chance +15%, Damage resistance +5%",
                        p(pe(PassiveType.MISS_CHANCE_PERCENT, 0.15f),
                          pe(PassiveType.DAMAGE_REDUCTION_PERCENT, 0.05f))),
                t(3, "Protector", "Sovereign", 60,
                        "Damage resistance +5%, Shop prices -8%",
                        p(pe(PassiveType.DAMAGE_REDUCTION_PERCENT, 0.05f),
                          pe(PassiveType.SHOP_DISCOUNT_PERCENT, 0.08f))),
                t(4, "Protector", "Sovereign", 120,
                        "Damage resistance +8%, Miss chance +10%",
                        p(pe(PassiveType.DAMAGE_REDUCTION_PERCENT, 0.08f),
                          pe(PassiveType.MISS_CHANCE_PERCENT, 0.10f))),
                t(5, "Protector", "Sovereign", 150,
                        "Damage resistance +10%, Miss chance +10%",
                        p(pe(PassiveType.DAMAGE_REDUCTION_PERCENT, 0.10f),
                          pe(PassiveType.MISS_CHANCE_PERCENT, 0.10f)))
        )));

        // -- Druid (ARC) -- "I command nature and heal all wounds"
        // Cumulative T5: +18% mana, +52% healing, +15% spell, +10% mana regen
        reg(new ClassDefinition("druid", "Druid", "Magic",
                new String[]{"ARC"}, false, List.of(
                t(1, "Druid", "World-Root", 12,
                        "Max mana +10%",
                        p(pe(PassiveType.MANA_POOL_PERCENT, 0.10f))),
                t(2, "Druid", "World-Root", 30,
                        "Healing output +10%. Animals passive toward you",
                        p(pe(PassiveType.HEALING_OUTPUT_PERCENT, 0.10f))),
                t(3, "Druid", "World-Root", 60,
                        "Healing output +12%, Max mana +8%",
                        p(pe(PassiveType.HEALING_OUTPUT_PERCENT, 0.12f),
                          pe(PassiveType.MANA_POOL_PERCENT, 0.08f))),
                t(4, "Druid", "World-Root", 120,
                        "Healing output +15%, Mana regen +10%",
                        p(pe(PassiveType.HEALING_OUTPUT_PERCENT, 0.15f),
                          pe(PassiveType.MANA_REGEN_PERCENT, 0.10f))),
                t(5, "Druid", "World-Root", 150,
                        "Spell damage +15%, Healing output +15%",
                        p(pe(PassiveType.SPELL_DAMAGE_PERCENT, 0.15f),
                          pe(PassiveType.HEALING_OUTPUT_PERCENT, 0.15f)))
        )));
    }

    // ========================================================
    //  DUAL-STAT CLASSES (15)
    // ========================================================

    private static void registerDualStatClasses() {
        // -- Duelist (STR + DEX) -- "Precision blade master"
        // Cumulative T5: +25% melee, +46% crit multiplier
        reg(new ClassDefinition("duelist", "Duelist", "Combat",
                new String[]{"STR", "DEX"}, true, List.of(
                t(1, "Duelist", "Bladelord", 6,
                        "Crit damage +10%, Melee damage +5%",
                        p(pe(PassiveType.CRIT_MULTIPLIER_PERCENT, 0.10f),
                          pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.05f))),
                t(2, "Duelist", "Bladelord", 15,
                        "Melee damage +5%. After crit, next attack +20% damage",
                        p(pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.05f))),
                t(3, "Duelist", "Bladelord", 30,
                        "Crit damage +10%, Melee damage +5%",
                        p(pe(PassiveType.CRIT_MULTIPLIER_PERCENT, 0.10f),
                          pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.05f))),
                t(4, "Duelist", "Bladelord", 60,
                        "Crit damage +10%, Melee damage +5%",
                        p(pe(PassiveType.CRIT_MULTIPLIER_PERCENT, 0.10f),
                          pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.05f))),
                t(5, "Duelist", "Bladelord", 75,
                        "Crit damage +16%, Melee damage +5%. 4 consecutive hits guarantees crit",
                        p(pe(PassiveType.CRIT_MULTIPLIER_PERCENT, 0.16f),
                          pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.05f)))
        )));

        // -- Warrior (STR + VIT) -- "Frontline bruiser"
        // Cumulative T5: +28% melee, +24% max HP, +15% DR
        reg(new ClassDefinition("warrior", "Warrior", "Combat",
                new String[]{"STR", "VIT"}, true, List.of(
                t(1, "Warrior", "Colossus", 6,
                        "Melee damage +8%",
                        p(pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.08f))),
                t(2, "Warrior", "Colossus", 15,
                        "Max HP +8%",
                        p(pe(PassiveType.MAX_HP_PERCENT, 0.08f))),
                t(3, "Warrior", "Colossus", 30,
                        "Melee damage +8%, Damage resistance +5%",
                        p(pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.08f),
                          pe(PassiveType.DAMAGE_REDUCTION_PERCENT, 0.05f))),
                t(4, "Warrior", "Colossus", 60,
                        "Damage resistance +5%, Max HP +8%",
                        p(pe(PassiveType.DAMAGE_REDUCTION_PERCENT, 0.05f),
                          pe(PassiveType.MAX_HP_PERCENT, 0.08f))),
                t(5, "Warrior", "Colossus", 75,
                        "Melee damage +12%, DR +5%, Max HP +8%",
                        p(pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.12f),
                          pe(PassiveType.DAMAGE_REDUCTION_PERCENT, 0.05f),
                          pe(PassiveType.MAX_HP_PERCENT, 0.08f)))
        )));

        // -- Warlock (STR + INT) -- "Melee/magic hybrid destroyer"
        // Cumulative T5: +43% melee, +33% spell, +8% mana
        reg(new ClassDefinition("warlock", "Warlock", "Hybrid",
                new String[]{"STR", "INT"}, true, List.of(
                t(1, "Warlock", "Runic Destroyer", 6,
                        "Melee damage +5%, Spell damage +5%",
                        p(pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.05f),
                          pe(PassiveType.SPELL_DAMAGE_PERCENT, 0.05f))),
                t(2, "Warlock", "Runic Destroyer", 15,
                        "Melee damage +8%, Spell damage +8%",
                        p(pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.08f),
                          pe(PassiveType.SPELL_DAMAGE_PERCENT, 0.08f))),
                t(3, "Warlock", "Runic Destroyer", 30,
                        "Melee damage +8%, Spell damage +8%",
                        p(pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.08f),
                          pe(PassiveType.SPELL_DAMAGE_PERCENT, 0.08f))),
                t(4, "Warlock", "Runic Destroyer", 60,
                        "Melee damage +10%, Mana pool +8%",
                        p(pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.10f),
                          pe(PassiveType.MANA_POOL_PERCENT, 0.08f))),
                t(5, "Warlock", "Runic Destroyer", 75,
                        "Melee damage +12%, Spell damage +12%",
                        p(pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.12f),
                          pe(PassiveType.SPELL_DAMAGE_PERCENT, 0.12f)))
        )));

        // -- Champion (STR + PRE) -- "Battlefield commander"
        // Cumulative T5: +46% melee, +35% miss, +10% DR
        reg(new ClassDefinition("champion", "Champion", "Combat",
                new String[]{"STR", "PRE"}, true, List.of(
                t(1, "Champion", "Legend", 6,
                        "Miss chance +20%, Melee damage +5%",
                        p(pe(PassiveType.MISS_CHANCE_PERCENT, 0.20f),
                          pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.05f))),
                t(2, "Champion", "Legend", 15,
                        "Melee damage +8%",
                        p(pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.08f))),
                t(3, "Champion", "Legend", 30,
                        "Melee damage +8%, Damage resistance +5%",
                        p(pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.08f),
                          pe(PassiveType.DAMAGE_REDUCTION_PERCENT, 0.05f))),
                t(4, "Champion", "Legend", 60,
                        "Melee damage +10%, Miss chance +15%",
                        p(pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.10f),
                          pe(PassiveType.MISS_CHANCE_PERCENT, 0.15f))),
                t(5, "Champion", "Legend", 75,
                        "Melee damage +15%, Damage resistance +5%",
                        p(pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.15f),
                          pe(PassiveType.DAMAGE_REDUCTION_PERCENT, 0.05f)))
        )));

        // -- Beastwarrior (STR + ARC) -- "Primal combatant"
        // Cumulative T5: +46% melee, +13% healing, +5% max HP, +8% spell
        reg(new ClassDefinition("beastwarrior", "Beastwarrior", "Hybrid",
                new String[]{"STR", "ARC"}, true, List.of(
                t(1, "Beastwarrior", "Primal Lord", 6,
                        "Melee damage +8%",
                        p(pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.08f))),
                t(2, "Beastwarrior", "Primal Lord", 15,
                        "Melee damage +8%, Healing output +5%",
                        p(pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.08f),
                          pe(PassiveType.HEALING_OUTPUT_PERCENT, 0.05f))),
                t(3, "Beastwarrior", "Primal Lord", 30,
                        "Melee damage +8%, Max HP +5%",
                        p(pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.08f),
                          pe(PassiveType.MAX_HP_PERCENT, 0.05f))),
                t(4, "Beastwarrior", "Primal Lord", 60,
                        "Melee damage +10%, Healing output +8%",
                        p(pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.10f),
                          pe(PassiveType.HEALING_OUTPUT_PERCENT, 0.08f))),
                t(5, "Beastwarrior", "Primal Lord", 75,
                        "Melee damage +12%, Spell damage +8%",
                        p(pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.12f),
                          pe(PassiveType.SPELL_DAMAGE_PERCENT, 0.08f)))
        )));

        // -- Pursuer (DEX + VIT) -- "Relentless hunter"
        // Cumulative T5: +32% crit multiplier, +10% stamina, +5% max HP, +8% regen, +8% DR
        reg(new ClassDefinition("pursuer", "Pursuer", "Combat",
                new String[]{"DEX", "VIT"}, true, List.of(
                t(1, "Pursuer", "Inevitable", 6,
                        "Crit damage +11%",
                        p(pe(PassiveType.CRIT_MULTIPLIER_PERCENT, 0.11f))),
                t(2, "Pursuer", "Inevitable", 15,
                        "Stamina pool +10%, Crit damage +3%",
                        p(pe(PassiveType.STAMINA_POOL_PERCENT, 0.10f),
                          pe(PassiveType.CRIT_MULTIPLIER_PERCENT, 0.03f))),
                t(3, "Pursuer", "Inevitable", 30,
                        "Crit damage +5%, Max HP +5%",
                        p(pe(PassiveType.CRIT_MULTIPLIER_PERCENT, 0.05f),
                          pe(PassiveType.MAX_HP_PERCENT, 0.05f))),
                t(4, "Pursuer", "Inevitable", 60,
                        "HP regen +8%, Damage resistance +3%",
                        p(pe(PassiveType.HP_REGEN_PERCENT, 0.08f),
                          pe(PassiveType.DAMAGE_REDUCTION_PERCENT, 0.03f))),
                t(5, "Pursuer", "Inevitable", 75,
                        "Crit damage +13%, DR +5%. Cannot be slowed below 80%",
                        p(pe(PassiveType.CRIT_MULTIPLIER_PERCENT, 0.13f),
                          pe(PassiveType.DAMAGE_REDUCTION_PERCENT, 0.05f)))
        )));

        // -- Trickster (DEX + INT) -- "Magical assassin"
        // Cumulative T5: +16% spell, +38% crit multiplier, +5% mana
        reg(new ClassDefinition("trickster", "Trickster", "Hybrid",
                new String[]{"DEX", "INT"}, true, List.of(
                t(1, "Trickster", "Mirage", 6,
                        "Crit damage +8%, Spell damage +3%",
                        p(pe(PassiveType.CRIT_MULTIPLIER_PERCENT, 0.08f),
                          pe(PassiveType.SPELL_DAMAGE_PERCENT, 0.03f))),
                t(2, "Trickster", "Mirage", 15,
                        "Crit damage +10%, Spell damage +5%",
                        p(pe(PassiveType.CRIT_MULTIPLIER_PERCENT, 0.10f),
                          pe(PassiveType.SPELL_DAMAGE_PERCENT, 0.05f))),
                t(3, "Trickster", "Mirage", 30,
                        "Spell damage +3%, Mana pool +5%",
                        p(pe(PassiveType.SPELL_DAMAGE_PERCENT, 0.03f),
                          pe(PassiveType.MANA_POOL_PERCENT, 0.05f))),
                t(4, "Trickster", "Mirage", 60,
                        "Crit damage +8%, Spell damage +3%",
                        p(pe(PassiveType.CRIT_MULTIPLIER_PERCENT, 0.08f),
                          pe(PassiveType.SPELL_DAMAGE_PERCENT, 0.03f))),
                t(5, "Trickster", "Mirage", 75,
                        "Crit damage +12%, Spell damage +2%. Every 4th spell is free",
                        p(pe(PassiveType.CRIT_MULTIPLIER_PERCENT, 0.12f),
                          pe(PassiveType.SPELL_DAMAGE_PERCENT, 0.02f)))
        )));

        // -- Swashbuckler (DEX + PRE) -- "Dashing duelist"
        // Cumulative T5: +30% crit multiplier, +19% melee, +25% miss
        reg(new ClassDefinition("swashbuckler", "Swashbuckler", "Combat",
                new String[]{"DEX", "PRE"}, true, List.of(
                t(1, "Swashbuckler", "Blade Saint", 6,
                        "Crit damage +6%, Melee damage +3%",
                        p(pe(PassiveType.CRIT_MULTIPLIER_PERCENT, 0.06f),
                          pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.03f))),
                t(2, "Swashbuckler", "Blade Saint", 15,
                        "Melee damage +5%, Miss chance +10%",
                        p(pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.05f),
                          pe(PassiveType.MISS_CHANCE_PERCENT, 0.10f))),
                t(3, "Swashbuckler", "Blade Saint", 30,
                        "Crit damage +8%, Melee damage +3%",
                        p(pe(PassiveType.CRIT_MULTIPLIER_PERCENT, 0.08f),
                          pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.03f))),
                t(4, "Swashbuckler", "Blade Saint", 60,
                        "Miss chance +15%, Melee damage +5%",
                        p(pe(PassiveType.MISS_CHANCE_PERCENT, 0.15f),
                          pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.05f))),
                t(5, "Swashbuckler", "Blade Saint", 75,
                        "Crit damage +16%, Melee +3%. Crits boost damage 15% for 3s",
                        p(pe(PassiveType.CRIT_MULTIPLIER_PERCENT, 0.16f),
                          pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.03f)))
        )));

        // -- Ranger (DEX + ARC) -- "Wilderness marksman"
        // Cumulative T5: +19% melee, +29% crit multiplier, +5% mana, +8% healing
        reg(new ClassDefinition("ranger", "Ranger", "Survival",
                new String[]{"DEX", "ARC"}, true, List.of(
                t(1, "Ranger", "Forestborn", 6,
                        "Crit damage +6%, Melee damage +3%",
                        p(pe(PassiveType.CRIT_MULTIPLIER_PERCENT, 0.06f),
                          pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.03f))),
                t(2, "Ranger", "Forestborn", 15,
                        "Melee damage +5%, Mana pool +5%. Animals passive toward you",
                        p(pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.05f),
                          pe(PassiveType.MANA_POOL_PERCENT, 0.05f))),
                t(3, "Ranger", "Forestborn", 30,
                        "Crit damage +10%, Melee damage +3%",
                        p(pe(PassiveType.CRIT_MULTIPLIER_PERCENT, 0.10f),
                          pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.03f))),
                t(4, "Ranger", "Forestborn", 60,
                        "Melee damage +5%, Healing output +8%",
                        p(pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.05f),
                          pe(PassiveType.HEALING_OUTPUT_PERCENT, 0.08f))),
                t(5, "Ranger", "Forestborn", 75,
                        "Crit damage +13%, Melee damage +3%. Headshots 15% chance to stun",
                        p(pe(PassiveType.CRIT_MULTIPLIER_PERCENT, 0.13f),
                          pe(PassiveType.MELEE_DAMAGE_PERCENT, 0.03f)))
        )));

        // -- Blightcaster (VIT + INT) -- "Tanky battle mage"
        // Cumulative T5: +15% DR, +43% spell, +10% max HP
        reg(new ClassDefinition("blightcaster", "Blightcaster", "Magic",
                new String[]{"VIT", "INT"}, true, List.of(
                t(1, "Blightcaster", "Ruinmage", 6,
                        "Damage resistance +5%, Spell damage +5%",
                        p(pe(PassiveType.DAMAGE_REDUCTION_PERCENT, 0.05f),
                          pe(PassiveType.SPELL_DAMAGE_PERCENT, 0.05f))),
                t(2, "Blightcaster", "Ruinmage", 15,
                        "Spell damage +8%. Spells 15% chance to apply status effect",
                        p(pe(PassiveType.SPELL_DAMAGE_PERCENT, 0.08f))),
                t(3, "Blightcaster", "Ruinmage", 30,
                        "Max HP +5%, Spell damage +8%",
                        p(pe(PassiveType.MAX_HP_PERCENT, 0.05f),
                          pe(PassiveType.SPELL_DAMAGE_PERCENT, 0.08f))),
                t(4, "Blightcaster", "Ruinmage", 60,
                        "Damage resistance +5%, Spell damage +10%",
                        p(pe(PassiveType.DAMAGE_REDUCTION_PERCENT, 0.05f),
                          pe(PassiveType.SPELL_DAMAGE_PERCENT, 0.10f))),
                t(5, "Blightcaster", "Ruinmage", 75,
                        "Spell damage +12%, DR +5%, Max HP +5%",
                        p(pe(PassiveType.SPELL_DAMAGE_PERCENT, 0.12f),
                          pe(PassiveType.DAMAGE_REDUCTION_PERCENT, 0.05f),
                          pe(PassiveType.MAX_HP_PERCENT, 0.05f)))
        )));

        // -- Sentinel (VIT + PRE) -- "Immovable wall"
        // Cumulative T5: +45% miss, +29% max HP, +21% DR
        reg(new ClassDefinition("sentinel", "Sentinel", "Support",
                new String[]{"VIT", "PRE"}, true, List.of(
                t(1, "Sentinel", "Bastion", 6,
                        "Miss chance +20%, Max HP +5%",
                        p(pe(PassiveType.MISS_CHANCE_PERCENT, 0.20f),
                          pe(PassiveType.MAX_HP_PERCENT, 0.05f))),
                t(2, "Sentinel", "Bastion", 15,
                        "Max HP +8%, Damage resistance +3%",
                        p(pe(PassiveType.MAX_HP_PERCENT, 0.08f),
                          pe(PassiveType.DAMAGE_REDUCTION_PERCENT, 0.03f))),
                t(3, "Sentinel", "Bastion", 30,
                        "Damage resistance +5%, Miss chance +10%",
                        p(pe(PassiveType.DAMAGE_REDUCTION_PERCENT, 0.05f),
                          pe(PassiveType.MISS_CHANCE_PERCENT, 0.10f))),
                t(4, "Sentinel", "Bastion", 60,
                        "Damage resistance +5%, Max HP +8%",
                        p(pe(PassiveType.DAMAGE_REDUCTION_PERCENT, 0.05f),
                          pe(PassiveType.MAX_HP_PERCENT, 0.08f))),
                t(5, "Sentinel", "Bastion", 75,
                        "DR +8%, Max HP +8%, Miss chance +15%",
                        p(pe(PassiveType.DAMAGE_REDUCTION_PERCENT, 0.08f),
                          pe(PassiveType.MAX_HP_PERCENT, 0.08f),
                          pe(PassiveType.MISS_CHANCE_PERCENT, 0.15f)))
        )));

        // -- Paladin (VIT + ARC) -- "Holy warrior-healer"
        // Cumulative T5: +50% healing, +13% max HP, +10% DR, +5% regen
        reg(new ClassDefinition("paladin", "Paladin", "Hybrid",
                new String[]{"VIT", "ARC"}, true, List.of(
                t(1, "Paladin", "Holy Avenger", 6,
                        "Healing output +10%",
                        p(pe(PassiveType.HEALING_OUTPUT_PERCENT, 0.10f))),
                t(2, "Paladin", "Holy Avenger", 15,
                        "Max HP +8%, Healing output +5%",
                        p(pe(PassiveType.MAX_HP_PERCENT, 0.08f),
                          pe(PassiveType.HEALING_OUTPUT_PERCENT, 0.05f))),
                t(3, "Paladin", "Holy Avenger", 30,
                        "Damage resistance +5%, Healing output +8%",
                        p(pe(PassiveType.DAMAGE_REDUCTION_PERCENT, 0.05f),
                          pe(PassiveType.HEALING_OUTPUT_PERCENT, 0.08f))),
                t(4, "Paladin", "Holy Avenger", 60,
                        "Healing output +12%, Max HP +5%",
                        p(pe(PassiveType.HEALING_OUTPUT_PERCENT, 0.12f),
                          pe(PassiveType.MAX_HP_PERCENT, 0.05f))),
                t(5, "Paladin", "Holy Avenger", 75,
                        "Healing output +15%, DR +5%, HP regen +5%",
                        p(pe(PassiveType.HEALING_OUTPUT_PERCENT, 0.15f),
                          pe(PassiveType.DAMAGE_REDUCTION_PERCENT, 0.05f),
                          pe(PassiveType.HP_REGEN_PERCENT, 0.05f)))
        )));

        // -- Scholar (INT + PRE) -- "Tactical spellweaver"
        // Cumulative T5: +43% spell, +45% miss, +3% DR
        reg(new ClassDefinition("scholar", "Scholar", "Support",
                new String[]{"INT", "PRE"}, true, List.of(
                t(1, "Scholar", "Mastermind", 6,
                        "Spell damage +5%, Miss chance +10%",
                        p(pe(PassiveType.SPELL_DAMAGE_PERCENT, 0.05f),
                          pe(PassiveType.MISS_CHANCE_PERCENT, 0.10f))),
                t(2, "Scholar", "Mastermind", 15,
                        "Miss chance +10%, Spell damage +8%",
                        p(pe(PassiveType.MISS_CHANCE_PERCENT, 0.10f),
                          pe(PassiveType.SPELL_DAMAGE_PERCENT, 0.08f))),
                t(3, "Scholar", "Mastermind", 30,
                        "Spell damage +8%, Damage resistance +3%",
                        p(pe(PassiveType.SPELL_DAMAGE_PERCENT, 0.08f),
                          pe(PassiveType.DAMAGE_REDUCTION_PERCENT, 0.03f))),
                t(4, "Scholar", "Mastermind", 60,
                        "Spell damage +10%, Miss chance +10%",
                        p(pe(PassiveType.SPELL_DAMAGE_PERCENT, 0.10f),
                          pe(PassiveType.MISS_CHANCE_PERCENT, 0.10f))),
                t(5, "Scholar", "Mastermind", 75,
                        "Spell damage +12%, Miss chance +15%. Kills reduce ally cooldowns by 1s",
                        p(pe(PassiveType.SPELL_DAMAGE_PERCENT, 0.12f),
                          pe(PassiveType.MISS_CHANCE_PERCENT, 0.15f)))
        )));

        // -- Arcanist (INT + ARC) -- "Pure arcane power"
        // Cumulative T5: +41% spell, +18% mana, +18% mana regen
        reg(new ClassDefinition("arcanist", "Arcanist", "Magic",
                new String[]{"INT", "ARC"}, true, List.of(
                t(1, "Arcanist", "Primordial", 6,
                        "Spell damage +8%",
                        p(pe(PassiveType.SPELL_DAMAGE_PERCENT, 0.08f))),
                t(2, "Arcanist", "Primordial", 15,
                        "Mana pool +10%",
                        p(pe(PassiveType.MANA_POOL_PERCENT, 0.10f))),
                t(3, "Arcanist", "Primordial", 30,
                        "Mana regen +10%, Spell damage +8%",
                        p(pe(PassiveType.MANA_REGEN_PERCENT, 0.10f),
                          pe(PassiveType.SPELL_DAMAGE_PERCENT, 0.08f))),
                t(4, "Arcanist", "Primordial", 60,
                        "Spell damage +10%, Mana pool +8%",
                        p(pe(PassiveType.SPELL_DAMAGE_PERCENT, 0.10f),
                          pe(PassiveType.MANA_POOL_PERCENT, 0.08f))),
                t(5, "Arcanist", "Primordial", 75,
                        "Spell damage +15%, Mana regen +8%",
                        p(pe(PassiveType.SPELL_DAMAGE_PERCENT, 0.15f),
                          pe(PassiveType.MANA_REGEN_PERCENT, 0.08f)))
        )));

        // -- Hexguard (PRE + ARC) -- "Cursing protector-healer"
        // Cumulative T5: +35% healing, +35% miss, +10% DR, +8% mana, +5% mana regen
        reg(new ClassDefinition("hexguard", "Hexguard", "Hybrid",
                new String[]{"PRE", "ARC"}, true, List.of(
                t(1, "Hexguard", "Hexsovereign", 6,
                        "Healing output +5%, Miss chance +10%",
                        p(pe(PassiveType.HEALING_OUTPUT_PERCENT, 0.05f),
                          pe(PassiveType.MISS_CHANCE_PERCENT, 0.10f))),
                t(2, "Hexguard", "Hexsovereign", 15,
                        "Mana pool +8%, Miss chance +10%",
                        p(pe(PassiveType.MANA_POOL_PERCENT, 0.08f),
                          pe(PassiveType.MISS_CHANCE_PERCENT, 0.10f))),
                t(3, "Hexguard", "Hexsovereign", 30,
                        "Healing output +10%, Damage resistance +5%",
                        p(pe(PassiveType.HEALING_OUTPUT_PERCENT, 0.10f),
                          pe(PassiveType.DAMAGE_REDUCTION_PERCENT, 0.05f))),
                t(4, "Hexguard", "Hexsovereign", 60,
                        "Healing output +8%, Mana regen +5%",
                        p(pe(PassiveType.HEALING_OUTPUT_PERCENT, 0.08f),
                          pe(PassiveType.MANA_REGEN_PERCENT, 0.05f))),
                t(5, "Hexguard", "Hexsovereign", 75,
                        "Healing +12%, DR +5%, Miss chance +15%",
                        p(pe(PassiveType.HEALING_OUTPUT_PERCENT, 0.12f),
                          pe(PassiveType.DAMAGE_REDUCTION_PERCENT, 0.05f),
                          pe(PassiveType.MISS_CHANCE_PERCENT, 0.15f)))
        )));
    }
}
