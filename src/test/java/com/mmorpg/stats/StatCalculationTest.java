package com.mmorpg.stats;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for stat calculation logic and PlayerStatData budget system.
 * Tests the math only — no Hytale dependencies.
 */
class StatCalculationTest {

    private static final float DELTA = 0.001f;

    // ══════════════════════════════════════
    //  PlayerStatData — level & points
    // ══════════════════════════════════════

    @Test
    void newPlayer_startsAtLevel1() {
        PlayerStatData data = new PlayerStatData();
        assertEquals(1, data.getLevel());
    }

    @Test
    void newPlayer_hasCorrectTotalPoints() {
        PlayerStatData data = new PlayerStatData();
        assertEquals(StatConstants.POINTS_PER_LEVEL, data.getTotalPoints());
    }

    @Test
    void newPlayer_allPointsAvailable() {
        PlayerStatData data = new PlayerStatData();
        assertEquals(StatConstants.POINTS_PER_LEVEL, data.getAvailablePoints());
        assertEquals(0, data.getSpentPoints());
    }

    @Test
    void spentPoints_tracksAllStats() {
        PlayerStatData data = new PlayerStatData();
        data.setStrength(3);
        data.setDexterity(2);
        data.setVitality(1);
        assertEquals(6, data.getSpentPoints());
    }

    @Test
    void availablePoints_decreasesAsPointsSpent() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(10);
        int total = data.getTotalPoints();
        data.setStrength(5);
        data.setDexterity(5);
        assertEquals(total - 10, data.getAvailablePoints());
    }

    @Test
    void setters_clampNegativeToZero() {
        PlayerStatData data = new PlayerStatData();
        data.setStrength(-5);
        assertEquals(0, data.getStrength());
    }

    @Test
    void setters_clampAboveMax() {
        PlayerStatData data = new PlayerStatData();
        data.setStrength(StatConstants.MAX_STAT_POINTS + 50);
        assertEquals(StatConstants.MAX_STAT_POINTS, data.getStrength());
    }

    @Test
    void setLevel_clampsMinimumToOne() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(0);
        assertEquals(1, data.getLevel());
        data.setLevel(-10);
        assertEquals(1, data.getLevel());
    }

    @Test
    void clone_copiesAllFields() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(15);
        data.setStrength(10);
        data.setDexterity(8);
        data.setVitality(12);
        data.setIntellect(5);
        data.setPresence(3);
        data.setArcana(7);

        PlayerStatData copy = new PlayerStatData(data);
        assertEquals(15, copy.getLevel());
        assertEquals(10, copy.getStrength());
        assertEquals(8, copy.getDexterity());
        assertEquals(12, copy.getVitality());
        assertEquals(5, copy.getIntellect());
        assertEquals(3, copy.getPresence());
        assertEquals(7, copy.getArcana());
    }

    // ══════════════════════════════════════
    //  Max HP (VIT)
    // ══════════════════════════════════════

    @Test
    void maxHp_atZeroVit_returnsBaseHp() {
        assertEquals(StatConstants.BASE_HP, StatCalculation.calculateMaxHp(0), DELTA);
    }

    @Test
    void maxHp_scales_withVitality() {
        float expected = StatConstants.BASE_HP + 10 * StatConstants.HP_PER_VIT;
        assertEquals(expected, StatCalculation.calculateMaxHp(10), DELTA);
    }

    @Test
    void maxHp_atHighVit_scalesLinearly() {
        float expected = StatConstants.BASE_HP + 100 * StatConstants.HP_PER_VIT;
        assertEquals(expected, StatCalculation.calculateMaxHp(100), DELTA);
    }

    // ══════════════════════════════════════
    //  Max Stamina (DEX)
    // ══════════════════════════════════════

    @Test
    void maxStamina_atZeroDex_returnsBaseStamina() {
        assertEquals(StatConstants.BASE_STAMINA, StatCalculation.calculateMaxStamina(0), DELTA);
    }

    @Test
    void maxStamina_scales_withDexterity() {
        float expected = StatConstants.BASE_STAMINA + 15 * StatConstants.STAMINA_PER_DEX;
        assertEquals(expected, StatCalculation.calculateMaxStamina(15), DELTA);
    }

    // ══════════════════════════════════════
    //  Max Mana (ARC)
    // ══════════════════════════════════════

    @Test
    void maxMana_atZeroArc_returnsBaseMana() {
        assertEquals(StatConstants.BASE_MANA, StatCalculation.calculateMaxMana(0), DELTA);
    }

    @Test
    void maxMana_scales_withArcana() {
        float expected = StatConstants.BASE_MANA + 20 * StatConstants.MANA_PER_ARC;
        assertEquals(expected, StatCalculation.calculateMaxMana(20), DELTA);
    }

    // ══════════════════════════════════════
    //  Max Oxygen (VIT)
    // ══════════════════════════════════════

    @Test
    void maxOxygen_atZeroVit_returnsBaseOxygen() {
        assertEquals(StatConstants.BASE_OXYGEN, StatCalculation.calculateMaxOxygen(0), DELTA);
    }

    @Test
    void maxOxygen_scales_withVitality() {
        float expected = StatConstants.BASE_OXYGEN + 5 * StatConstants.OXYGEN_PER_VIT;
        assertEquals(expected, StatCalculation.calculateMaxOxygen(5), DELTA);
    }

    // ══════════════════════════════════════
    //  Mana Regen (INT)
    // ══════════════════════════════════════

    @Test
    void manaRegen_atZeroInt_returnsBaseRegen() {
        assertEquals(StatConstants.BASE_MANA_REGEN, StatCalculation.calculateManaRegenRate(0), DELTA);
    }

    @Test
    void manaRegen_scales_withIntellect() {
        float expected = StatConstants.BASE_MANA_REGEN + 8 * StatConstants.MANA_REGEN_PER_INT;
        assertEquals(expected, StatCalculation.calculateManaRegenRate(8), DELTA);
    }

    // ══════════════════════════════════════
    //  Melee Damage Multiplier (STR)
    // ══════════════════════════════════════

    @Test
    void meleeDamage_atZeroStr_returnsBaseMultiplier() {
        assertEquals(StatConstants.BASE_MELEE_DAMAGE_MULTIPLIER,
                StatCalculation.calculateMeleeDamageMultiplier(0), DELTA);
    }

    @Test
    void meleeDamage_scales_withStrength() {
        float expected = StatConstants.BASE_MELEE_DAMAGE_MULTIPLIER + 20 * StatConstants.MELEE_DAMAGE_PER_STR;
        assertEquals(expected, StatCalculation.calculateMeleeDamageMultiplier(20), DELTA);
    }

    // ══════════════════════════════════════
    //  Miss Chance (PRE)
    // ══════════════════════════════════════

    @Test
    void missChance_atZeroPre_returnsZero() {
        assertEquals(0.0f, StatCalculation.calculateMissChance(0), DELTA);
    }

    @Test
    void missChance_clamps_atFiftyPercent() {
        assertEquals(0.50f, StatCalculation.calculateMissChance(500), DELTA);
    }

    @Test
    void missChance_normal_value() {
        float expected = 25 * StatConstants.MISS_CHANCE_PER_PRE;
        assertEquals(expected, StatCalculation.calculateMissChance(25), DELTA);
    }

    // ══════════════════════════════════════
    //  Damage Resistance (VIT)
    // ══════════════════════════════════════

    @Test
    void damageResistance_atZeroVit_returnsZero() {
        assertEquals(0.0f, StatCalculation.calculateDamageResistance(0), DELTA);
    }

    @Test
    void damageResistance_scales_withVitality() {
        float expected = 10 * StatConstants.DAMAGE_RESISTANCE_PER_VIT;
        assertEquals(expected, StatCalculation.calculateDamageResistance(10), DELTA);
    }

    // ══════════════════════════════════════
    //  Vendor Discount (PRE)
    // ══════════════════════════════════════

    @Test
    void vendorDiscount_atZeroPre_returnsZero() {
        assertEquals(0.0f, StatCalculation.calculateVendorDiscount(0), DELTA);
    }

    @Test
    void vendorDiscount_clamps_atOne() {
        assertEquals(1.0f, StatCalculation.calculateVendorDiscount(1000), DELTA);
    }

    @Test
    void vendorDiscount_normal_value() {
        float expected = 10 * StatConstants.VENDOR_DISCOUNT_PER_PRE;
        assertEquals(expected, StatCalculation.calculateVendorDiscount(10), DELTA);
    }

    // ══════════════════════════════════════
    //  Other multipliers
    // ══════════════════════════════════════

    @Test
    void fallDamageReduction_scales_withDexterity() {
        float expected = 10 * StatConstants.FALL_DMG_REDUCTION_PER_DEX;
        assertEquals(expected, StatCalculation.calculateFallDamageReduction(10), DELTA);
    }

    @Test
    void lightWeaponDamage_scales_withDexterity() {
        float expected = 1.0f + 10 * StatConstants.LIGHT_WEAPON_DAMAGE_PER_DEX;
        assertEquals(expected, StatCalculation.calculateLightWeaponDamageMultiplier(10), DELTA);
    }

    @Test
    void spellDamage_scales_withIntellect() {
        float expected = 1.0f + 10 * StatConstants.SPELL_DAMAGE_PER_INT;
        assertEquals(expected, StatCalculation.calculateSpellDamageMultiplier(10), DELTA);
    }

    @Test
    void healingMultiplier_scales_withArcana() {
        float expected = 1.0f + 10 * StatConstants.HEALING_PER_ARC;
        assertEquals(expected, StatCalculation.calculateHealingMultiplier(10), DELTA);
    }

    // ══════════════════════════════════════
    //  XP Calculations
    // ══════════════════════════════════════

    @Test
    void xpForLevel_level1_returnsZero() {
        assertEquals(0, StatCalculation.xpForLevel(1));
    }

    @Test
    void xpForLevel_level2_returnsBase() {
        assertEquals(StatConstants.BASE_XP_PER_LEVEL, StatCalculation.xpForLevel(2));
    }

    @Test
    void xpForLevel_scales_exponentially() {
        int level3 = StatCalculation.xpForLevel(3);
        int level2 = StatCalculation.xpForLevel(2);
        assertTrue(level3 > level2, "Level 3 should require more XP than level 2");
    }

    @Test
    void totalXpForLevel_level1_isZero() {
        assertEquals(0, StatCalculation.totalXpForLevel(1));
    }

    @Test
    void totalXpForLevel_level2_equalsBase() {
        assertEquals(StatConstants.BASE_XP_PER_LEVEL, StatCalculation.totalXpForLevel(2));
    }

    @Test
    void totalXpForLevel_accumulates() {
        int expected = StatCalculation.xpForLevel(2) + StatCalculation.xpForLevel(3);
        assertEquals(expected, StatCalculation.totalXpForLevel(3));
    }

    // ══════════════════════════════════════
    //  PlayerStatData — XP & Level-up
    // ══════════════════════════════════════

    @Test
    void newPlayer_hasZeroXp() {
        PlayerStatData data = new PlayerStatData();
        assertEquals(0, data.getXp());
    }

    @Test
    void addXp_belowThreshold_noLevelUp() {
        PlayerStatData data = new PlayerStatData();
        int needed = data.getXpForNextLevel();
        int gained = data.addXp(needed - 1);
        assertEquals(0, gained);
        assertEquals(1, data.getLevel());
        assertEquals(needed - 1, data.getXp());
    }

    @Test
    void addXp_exactThreshold_levelsUp() {
        PlayerStatData data = new PlayerStatData();
        int needed = data.getXpForNextLevel();
        int gained = data.addXp(needed);
        assertEquals(1, gained);
        assertEquals(2, data.getLevel());
        assertEquals(0, data.getXp());
    }

    @Test
    void addXp_overflowCarries_toNextLevel() {
        PlayerStatData data = new PlayerStatData();
        int level2 = data.getXpForNextLevel();
        data.addXp(level2); // now level 2
        int level3needed = data.getXpForNextLevel();
        int gained = data.addXp(level3needed + 10);
        assertEquals(1, gained);
        assertEquals(3, data.getLevel());
        assertEquals(10, data.getXp());
    }

    @Test
    void addXp_multipleeLevelUps() {
        PlayerStatData data = new PlayerStatData();
        // Give enough XP for multiple levels
        int bigXp = StatCalculation.totalXpForLevel(5);
        int gained = data.addXp(bigXp);
        assertEquals(4, gained); // level 1 → 5
        assertEquals(5, data.getLevel());
    }

    @Test
    void addXp_atMaxLevel_noEffect() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(StatConstants.MAX_LEVEL);
        int gained = data.addXp(99999);
        assertEquals(0, gained);
        assertEquals(StatConstants.MAX_LEVEL, data.getLevel());
    }

    @Test
    void setLevel_clampsAtMaxLevel() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(StatConstants.MAX_LEVEL + 10);
        assertEquals(StatConstants.MAX_LEVEL, data.getLevel());
    }

    @Test
    void clone_copiesXp() {
        PlayerStatData data = new PlayerStatData();
        data.addXp(42);
        PlayerStatData copy = new PlayerStatData(data);
        assertEquals(42, copy.getXp());
    }

    @Test
    void getXpForNextLevel_atMaxLevel_returnsPositive() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(StatConstants.MAX_LEVEL);
        // Should still return a valid number (even though we can't level past max)
        assertTrue(data.getXpForNextLevel() > 0);
    }

    // ══════════════════════════════════════
    //  Kill XP Scaling (Phase 4)
    // ══════════════════════════════════════

    @Test
    void killXp_atReferenceHp_returnsBaseXp() {
        assertEquals(StatConstants.BASE_KILL_XP,
                StatCalculation.calculateKillXp(StatConstants.XP_REFERENCE_HP));
    }

    @Test
    void killXp_doubleHp_givesDoubleXp() {
        int xp = StatCalculation.calculateKillXp(StatConstants.XP_REFERENCE_HP * 2);
        assertEquals(StatConstants.BASE_KILL_XP * 2, xp);
    }

    @Test
    void killXp_halfHp_givesHalfXp() {
        int xp = StatCalculation.calculateKillXp(StatConstants.XP_REFERENCE_HP / 2);
        assertEquals(StatConstants.BASE_KILL_XP / 2, xp);
    }

    @Test
    void killXp_veryWeakMob_clampsToMinimum() {
        int xp = StatCalculation.calculateKillXp(1.0f);
        assertEquals(StatConstants.MIN_KILL_XP, xp);
    }

    @Test
    void killXp_zeroHp_clampsToMinimum() {
        assertEquals(StatConstants.MIN_KILL_XP, StatCalculation.calculateKillXp(0.0f));
    }

    @Test
    void killXp_negativeHp_clampsToMinimum() {
        assertEquals(StatConstants.MIN_KILL_XP, StatCalculation.calculateKillXp(-50.0f));
    }

    @Test
    void killXp_bossHp_clampsToMaximum() {
        int xp = StatCalculation.calculateKillXp(100000.0f);
        assertEquals(StatConstants.MAX_KILL_XP, xp);
    }

    @Test
    void killXp_scalingIsProportional() {
        int xp100 = StatCalculation.calculateKillXp(100.0f);
        int xp200 = StatCalculation.calculateKillXp(200.0f);
        int xp400 = StatCalculation.calculateKillXp(400.0f);
        // Higher HP should always give more XP
        assertTrue(xp200 > xp100, "200 HP mob should give more XP than 100 HP mob");
        assertTrue(xp400 > xp200, "400 HP mob should give more XP than 200 HP mob");
        // Ratio should be approximately linear (within int truncation)
        assertEquals(xp100 * 2, xp200, "200 HP should give ~2x XP of 100 HP");
    }

    // ══════════════════════════════════════
    //  Death XP Penalty (Phase 4)
    // ══════════════════════════════════════

    @Test
    void deathPenalty_atLevel1_is10PercentOfLevelXp() {
        int xpNeeded = StatCalculation.xpForLevel(2);
        int penalty = StatCalculation.calculateDeathXpPenalty(1, xpNeeded);
        int expected = (int) (xpNeeded * StatConstants.DEATH_XP_PENALTY);
        assertEquals(expected, penalty);
    }

    @Test
    void deathPenalty_neverExceedsCurrentXp() {
        int penalty = StatCalculation.calculateDeathXpPenalty(10, 5);
        assertTrue(penalty <= 5, "Penalty should not exceed current XP");
    }

    @Test
    void deathPenalty_atZeroXp_isZero() {
        int penalty = StatCalculation.calculateDeathXpPenalty(5, 0);
        assertEquals(0, penalty);
    }

    @Test
    void deathPenalty_scalesWithLevel() {
        int penaltyLow = StatCalculation.calculateDeathXpPenalty(2, 9999);
        int penaltyHigh = StatCalculation.calculateDeathXpPenalty(20, 9999);
        assertTrue(penaltyHigh > penaltyLow,
                "Higher level should have larger penalty");
    }

    // ══════════════════════════════════════
    //  PlayerStatData — Respec / Death
    // ══════════════════════════════════════

    @Test
    void respec_resetsAllStatsToZero() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(10);
        data.setStrength(10);
        data.setDexterity(8);
        data.setVitality(5);
        data.setIntellect(3);
        data.setPresence(2);
        data.setArcana(7);

        // Simulate respec
        data.setStrength(0);
        data.setDexterity(0);
        data.setVitality(0);
        data.setIntellect(0);
        data.setPresence(0);
        data.setArcana(0);

        assertEquals(0, data.getSpentPoints());
        assertEquals(data.getTotalPoints(), data.getAvailablePoints());
    }

    @Test
    void setXp_clampsBelowZero() {
        PlayerStatData data = new PlayerStatData();
        data.setXp(-100);
        assertEquals(0, data.getXp());
    }

    @Test
    void deathPenalty_appliedToStatData_reducesXp() {
        PlayerStatData data = new PlayerStatData();
        data.addXp(80); // add some XP
        int before = data.getXp();
        int penalty = StatCalculation.calculateDeathXpPenalty(data.getLevel(), data.getXp());
        data.setXp(data.getXp() - penalty);
        assertTrue(data.getXp() < before, "XP should decrease after death penalty");
        assertTrue(data.getXp() >= 0, "XP should never go negative");
    }

    // ══════════════════════════════════════
    //  Critical hit calculations
    // ══════════════════════════════════════

    @Test
    void critChance_zeroDex_returnsZero() {
        assertEquals(0f, StatCalculation.calculateCritChance(0), DELTA);
    }

    @Test
    void critChance_scalesWithDex() {
        float chance = StatCalculation.calculateCritChance(20);
        assertEquals(20 * StatConstants.CRIT_CHANCE_PER_DEX, chance, DELTA);
    }

    @Test
    void critChance_cappedAtMax() {
        float chance = StatCalculation.calculateCritChance(1000);
        assertEquals(StatConstants.MAX_CRIT_CHANCE, chance, DELTA);
    }

    @Test
    void critMultiplier_zeroStr_returnsBase() {
        float mult = StatCalculation.calculateCritMultiplier(0);
        assertEquals(StatConstants.BASE_CRIT_MULTIPLIER, mult, DELTA);
    }

    @Test
    void critMultiplier_scalesWithStr() {
        float mult = StatCalculation.calculateCritMultiplier(25);
        float expected = StatConstants.BASE_CRIT_MULTIPLIER
                + 25 * StatConstants.CRIT_DAMAGE_PER_STR;
        assertEquals(expected, mult, DELTA);
    }

    @Test
    void critMultiplier_highStr_noUpperCap() {
        float mult = StatCalculation.calculateCritMultiplier(100);
        assertTrue(mult > StatConstants.BASE_CRIT_MULTIPLIER,
                "Crit multiplier should exceed base with high STR");
    }

    // ══════════════════════════════════════
    //  Buff system — BuffInstance
    // ══════════════════════════════════════

    @Test
    void buffInstance_initialState() {
        BuffInstance buff = BuffInstance.builder("test", "Test", BuffType.STAT_MODIFIER)
                .duration(10)
                .build();
        assertEquals("test", buff.getId());
        assertEquals("Test", buff.getDisplayName());
        assertEquals(BuffType.STAT_MODIFIER, buff.getType());
        assertEquals(10f, buff.getDuration(), DELTA);
        assertFalse(buff.isPermanent());
        assertFalse(buff.isExpired());
        assertFalse(buff.isDebuff());
    }

    @Test
    void buffInstance_permanentBuff() {
        BuffInstance buff = BuffInstance.builder("perm", "Permanent", BuffType.STAT_MODIFIER)
                .build(); // default duration is -1
        assertTrue(buff.isPermanent());
        assertFalse(buff.isExpired());
    }

    @Test
    void buffInstance_debuffFlag() {
        BuffInstance buff = BuffInstance.builder("poison", "Poison", BuffType.DAMAGE_OVER_TIME)
                .duration(5)
                .debuff()
                .build();
        assertTrue(buff.isDebuff());
    }

    @Test
    void buffInstance_tickDecrementsDuration() {
        BuffInstance buff = BuffInstance.builder("test", "Test", BuffType.STAT_MODIFIER)
                .duration(5)
                .build();
        buff.tick(2f);
        assertEquals(3f, buff.getDuration(), DELTA);
        assertFalse(buff.isExpired());
    }

    @Test
    void buffInstance_tickExpires() {
        BuffInstance buff = BuffInstance.builder("test", "Test", BuffType.STAT_MODIFIER)
                .duration(1)
                .build();
        buff.tick(1.5f);
        assertTrue(buff.isExpired());
    }

    @Test
    void buffInstance_permanentNeverExpires() {
        BuffInstance buff = BuffInstance.builder("perm", "Permanent", BuffType.STAT_MODIFIER)
                .build();
        buff.tick(1000f);
        assertFalse(buff.isExpired());
    }

    @Test
    void buffInstance_dotTickFires() {
        BuffInstance buff = BuffInstance.builder("dot", "DoT", BuffType.DAMAGE_OVER_TIME)
                .duration(10)
                .tickInterval(2)
                .tickAmount(5)
                .build();
        // First tick at 1s — not enough time
        assertFalse(buff.tick(1f));
        // Second tick at 2s total — should fire
        assertTrue(buff.tick(1f));
    }

    @Test
    void buffInstance_hotTickFires() {
        BuffInstance buff = BuffInstance.builder("hot", "HoT", BuffType.HEAL_OVER_TIME)
                .duration(10)
                .tickInterval(1)
                .tickAmount(3)
                .build();
        assertTrue(buff.tick(1f));
    }

    @Test
    void buffInstance_statModifierDotTickDoesNotFire() {
        BuffInstance buff = BuffInstance.builder("stat", "Stat", BuffType.STAT_MODIFIER)
                .duration(10)
                .build();
        assertFalse(buff.tick(5f));
    }

    @Test
    void buffInstance_copy_isIndependent() {
        BuffInstance original = BuffInstance.builder("test", "Test", BuffType.STAT_MODIFIER)
                .duration(10)
                .bonusDamage(0.5f)
                .build();
        BuffInstance copy = original.copy();

        copy.tick(5f);
        assertEquals(10f, original.getDuration(), DELTA); // original unchanged
        assertEquals(5f, copy.getDuration(), DELTA);
    }

    // ══════════════════════════════════════
    //  Buff system — BuffComponent
    // ══════════════════════════════════════

    @Test
    void buffComponent_initiallyEmpty() {
        BuffComponent comp = new BuffComponent();
        assertTrue(comp.getBuffs().isEmpty());
    }

    @Test
    void buffComponent_addAndGet() {
        BuffComponent comp = new BuffComponent();
        BuffInstance buff = BuffInstance.builder("test", "Test", BuffType.STAT_MODIFIER)
                .duration(10)
                .build();
        comp.addBuff(buff);
        assertTrue(comp.hasBuff("test"));
        assertNotNull(comp.getBuff("test"));
    }

    @Test
    void buffComponent_remove() {
        BuffComponent comp = new BuffComponent();
        comp.addBuff(BuffInstance.builder("test", "Test", BuffType.STAT_MODIFIER)
                .duration(10).build());
        assertTrue(comp.removeBuff("test"));
        assertFalse(comp.hasBuff("test"));
    }

    @Test
    void buffComponent_clearRemovesAll() {
        BuffComponent comp = new BuffComponent();
        comp.addBuff(BuffInstance.builder("a", "A", BuffType.STAT_MODIFIER).duration(5).build());
        comp.addBuff(BuffInstance.builder("b", "B", BuffType.HEAL_OVER_TIME).duration(5).build());
        comp.clear();
        assertTrue(comp.getBuffs().isEmpty());
    }

    // ══════════════════════════════════════
    //  Ability system — AbilityDefinition
    // ══════════════════════════════════════

    @Test
    void abilityDefinition_basicProperties() {
        AbilityDefinition ability = AbilityDefinition.builder("fireball", "Fireball")
                .description("Shoots fire")
                .cooldown(5)
                .manaCost(20)
                .build();
        assertEquals("fireball", ability.getId());
        assertEquals("Fireball", ability.getName());
        assertEquals("Shoots fire", ability.getDescription());
        assertEquals(5f, ability.getCooldownSeconds(), DELTA);
        assertEquals(20f, ability.getManaCost(), DELTA);
        assertEquals(0f, ability.getStaminaCost(), DELTA);
    }

    @Test
    void abilityDefinition_withSelfBuff() {
        BuffInstance buff = BuffInstance.builder("rage", "Rage", BuffType.STAT_MODIFIER)
                .duration(10).bonusDamage(0.5f).build();
        AbilityDefinition ability = AbilityDefinition.builder("rage", "Rage")
                .selfBuff(buff)
                .build();
        assertTrue(ability.hasSelfBuff());
        assertNotNull(ability.getSelfBuff());
    }

    @Test
    void abilityDefinition_withInstantHeal() {
        AbilityDefinition ability = AbilityDefinition.builder("heal", "Heal")
                .instantHeal(50)
                .build();
        assertTrue(ability.hasInstantHeal());
        assertEquals(50f, ability.getInstantHeal(), DELTA);
    }

    @Test
    void abilityDefinition_noEffects() {
        AbilityDefinition ability = AbilityDefinition.builder("empty", "Empty").build();
        assertFalse(ability.hasSelfBuff());
        assertFalse(ability.hasInstantHeal());
        assertFalse(ability.hasSelfDamage());
    }

    // ══════════════════════════════════════
    //  Ability system — AbilityRegistry
    // ══════════════════════════════════════

    @Test
    void abilityRegistry_registerAndGet() {
        AbilityRegistry.clear();
        AbilityDefinition ability = AbilityDefinition.builder("test_spell", "Test").build();
        AbilityRegistry.register(ability);
        assertTrue(AbilityRegistry.exists("test_spell"));
        assertEquals("Test", AbilityRegistry.get("test_spell").getName());
        AbilityRegistry.clear();
    }

    @Test
    void abilityRegistry_unknownReturnsNull() {
        AbilityRegistry.clear();
        assertNull(AbilityRegistry.get("nonexistent"));
        assertFalse(AbilityRegistry.exists("nonexistent"));
        AbilityRegistry.clear();
    }

    @Test
    void abilityRegistry_getAllReturnsAll() {
        AbilityRegistry.clear();
        AbilityRegistry.register(AbilityDefinition.builder("a", "A").build());
        AbilityRegistry.register(AbilityDefinition.builder("b", "B").build());
        assertEquals(2, AbilityRegistry.getAll().size());
        AbilityRegistry.clear();
    }

    // ══════════════════════════════════════
    //  Ability system — PlayerAbilityData
    // ══════════════════════════════════════

    @Test
    void playerAbilityData_notOnCooldownInitially() {
        PlayerAbilityData data = new PlayerAbilityData();
        assertFalse(data.isOnCooldown("any_ability"));
        assertEquals(0f, data.getRemainingCooldown("any_ability"), DELTA);
    }

    @Test
    void playerAbilityData_cooldownTracking() {
        PlayerAbilityData data = new PlayerAbilityData();
        data.startCooldown("fireball", 10);
        assertTrue(data.isOnCooldown("fireball"));
        assertTrue(data.getRemainingCooldown("fireball") > 0);
    }

    @Test
    void playerAbilityData_zeroCooldownNotTracked() {
        PlayerAbilityData data = new PlayerAbilityData();
        data.startCooldown("instant", 0);
        assertFalse(data.isOnCooldown("instant"));
    }

    @Test
    void playerAbilityData_clearCooldowns() {
        PlayerAbilityData data = new PlayerAbilityData();
        data.startCooldown("a", 100);
        data.startCooldown("b", 100);
        data.clearCooldowns();
        assertFalse(data.isOnCooldown("a"));
        assertFalse(data.isOnCooldown("b"));
    }

    // ══════════════════════════════════════
    //  Class system — ClassRegistry
    // ══════════════════════════════════════

    @Test
    void classRegistry_has21Classes() {
        assertEquals(21, ClassRegistry.getAll().size());
    }

    @Test
    void classRegistry_singleStatClasses() {
        assertNotNull(ClassRegistry.get("knight"));
        assertNotNull(ClassRegistry.get("rogue"));
        assertNotNull(ClassRegistry.get("brawler"));
        assertNotNull(ClassRegistry.get("wizard"));
        assertNotNull(ClassRegistry.get("protector"));
        assertNotNull(ClassRegistry.get("druid"));
    }

    @Test
    void classRegistry_dualStatClasses() {
        assertNotNull(ClassRegistry.get("duelist"));
        assertNotNull(ClassRegistry.get("warrior"));
        assertNotNull(ClassRegistry.get("warlock"));
        assertNotNull(ClassRegistry.get("champion"));
        assertNotNull(ClassRegistry.get("beastwarrior"));
        assertNotNull(ClassRegistry.get("pursuer"));
        assertNotNull(ClassRegistry.get("trickster"));
        assertNotNull(ClassRegistry.get("swashbuckler"));
        assertNotNull(ClassRegistry.get("ranger"));
        assertNotNull(ClassRegistry.get("blightcaster"));
        assertNotNull(ClassRegistry.get("sentinel"));
        assertNotNull(ClassRegistry.get("paladin"));
        assertNotNull(ClassRegistry.get("scholar"));
        assertNotNull(ClassRegistry.get("arcanist"));
        assertNotNull(ClassRegistry.get("hexguard"));
    }

    @Test
    void classRegistry_knightHas5Tiers() {
        ClassDefinition knight = ClassRegistry.get("knight");
        assertEquals(5, knight.getTiers().size());
        assertFalse(knight.isDualStat());
        assertArrayEquals(new String[]{"STR"}, knight.getPrimaryStats());
    }

    @Test
    void classRegistry_duelistIsDualStat() {
        ClassDefinition duelist = ClassRegistry.get("duelist");
        assertTrue(duelist.isDualStat());
        assertArrayEquals(new String[]{"STR", "DEX"}, duelist.getPrimaryStats());
    }

    // ── Naming convention tests ──

    @Test
    void classRegistry_tierNamingConvention_singleStat() {
        ClassDefinition knight = ClassRegistry.get("knight");
        assertEquals("Apprentice Knight", knight.getTier(1).titleName());
        assertEquals("Acolyte Knight", knight.getTier(2).titleName());
        assertEquals("Knight", knight.getTier(3).titleName());
        assertEquals("Grand Knight", knight.getTier(4).titleName());
        assertEquals("Worldbreaker", knight.getTier(5).titleName());
    }

    @Test
    void classRegistry_tierNamingConvention_wizard() {
        ClassDefinition wizard = ClassRegistry.get("wizard");
        assertEquals("Apprentice Wizard", wizard.getTier(1).titleName());
        assertEquals("Acolyte Wizard", wizard.getTier(2).titleName());
        assertEquals("Wizard", wizard.getTier(3).titleName());
        assertEquals("Grand Wizard", wizard.getTier(4).titleName());
        assertEquals("Sage", wizard.getTier(5).titleName());
    }

    @Test
    void classRegistry_tierNamingConvention_dualStat() {
        ClassDefinition duelist = ClassRegistry.get("duelist");
        assertEquals("Apprentice Duelist", duelist.getTier(1).titleName());
        assertEquals("Acolyte Duelist", duelist.getTier(2).titleName());
        assertEquals("Duelist", duelist.getTier(3).titleName());
        assertEquals("Grand Duelist", duelist.getTier(4).titleName());
        assertEquals("Bladelord", duelist.getTier(5).titleName());
    }

    @Test
    void classRegistry_singleStatThresholds() {
        ClassDefinition knight = ClassRegistry.get("knight");
        assertEquals(12, knight.getTier(1).threshold());
        assertEquals(30, knight.getTier(2).threshold());
        assertEquals(60, knight.getTier(3).threshold());
        assertEquals(120, knight.getTier(4).threshold());
        assertEquals(150, knight.getTier(5).threshold());
    }

    @Test
    void classRegistry_dualStatThresholds() {
        ClassDefinition duelist = ClassRegistry.get("duelist");
        assertEquals(6, duelist.getTier(1).threshold());
        assertEquals(15, duelist.getTier(2).threshold());
        assertEquals(30, duelist.getTier(3).threshold());
        assertEquals(60, duelist.getTier(4).threshold());
        assertEquals(75, duelist.getTier(5).threshold());
    }

    @Test
    void classRegistry_t5TotalCost_singleEqualsDoubleDual() {
        ClassDefinition knight = ClassRegistry.get("knight");
        ClassDefinition duelist = ClassRegistry.get("duelist");
        // Single T5: 150, Dual T5: 75 * 2 = 150 → equal
        assertEquals(knight.getTier(5).totalCost(false),
                     duelist.getTier(5).totalCost(true));
    }

    @Test
    void classRegistry_knightT1HasPassive() {
        ClassDefinition knight = ClassRegistry.get("knight");
        var passives = knight.getTier(1).passives();
        assertEquals(1, passives.size());
        assertEquals(PassiveType.MELEE_DAMAGE_PERCENT, passives.get(0).type());
        assertEquals(0.10f, passives.get(0).value(), DELTA);
    }

    // ══════════════════════════════════════
    //  Class system — PlayerClassData
    // ══════════════════════════════════════

    @Test
    void playerClassData_defaultState() {
        PlayerClassData data = new PlayerClassData();
        assertTrue(data.getEarnedTitles().isEmpty());
        assertEquals("Apprentice", data.getDisplayedTitle());
        assertTrue(data.isAutoUpdate());
    }

    @Test
    void playerClassData_addAndCheckTitle() {
        PlayerClassData data = new PlayerClassData();
        data.addTitle("knight_1");
        assertTrue(data.hasTitle("knight_1"));
        assertFalse(data.hasTitle("knight_2"));
    }

    @Test
    void playerClassData_clearTitles() {
        PlayerClassData data = new PlayerClassData();
        data.addTitle("knight_1");
        data.addTitle("brawler_2");
        data.clearTitles();
        assertTrue(data.getEarnedTitles().isEmpty());
        assertEquals("Apprentice", data.getDisplayedTitle());
    }

    @Test
    void playerClassData_recomputePassives_aggregates() {
        PlayerClassData data = new PlayerClassData();
        // Knight T1: melee +10%, Warrior T1: melee +8%, Brawler T1: HP +10%
        data.addTitle("knight_1");
        data.addTitle("warrior_1");
        data.addTitle("brawler_1");
        data.recomputePassives();

        assertEquals(0.18f, data.getCachedMeleeDamageBonus(), DELTA); // 10% + 8%
        assertEquals(0.10f, data.getCachedMaxHpPercent(), DELTA);
    }

    @Test
    void playerClassData_recomputePassives_critMultAndDamageReduction() {
        PlayerClassData data = new PlayerClassData();
        // Duelist T1: crit damage +5%, Swashbuckler T1: crit damage +5%
        // Brawler T2: damage reduction 5%
        data.addTitle("duelist_1");
        data.addTitle("swashbuckler_1");
        data.addTitle("brawler_2");
        data.recomputePassives();

        assertEquals(0.10f, data.getCachedCritMultiplierBonus(), DELTA);
        assertEquals(0.05f, data.getCachedDamageReductionPercent(), DELTA);
    }

    // ══════════════════════════════════════
    //  Class system — ClassEvaluator (pure logic)
    // ══════════════════════════════════════

    @Test
    void classEvaluator_getStatValue_mapsCorrectly() {
        PlayerStatData stats = new PlayerStatData();
        stats.setStrength(10);
        stats.setDexterity(20);
        stats.setVitality(30);
        stats.setIntellect(40);
        stats.setPresence(50);
        stats.setArcana(60);

        assertEquals(10, ClassEvaluator.getStatValue(stats, "STR"));
        assertEquals(20, ClassEvaluator.getStatValue(stats, "DEX"));
        assertEquals(30, ClassEvaluator.getStatValue(stats, "VIT"));
        assertEquals(40, ClassEvaluator.getStatValue(stats, "INT"));
        assertEquals(50, ClassEvaluator.getStatValue(stats, "PRE"));
        assertEquals(60, ClassEvaluator.getStatValue(stats, "ARC"));
        assertEquals(0, ClassEvaluator.getStatValue(stats, "INVALID"));
    }

    @Test
    void classEvaluator_getTitleDisplayName() {
        assertEquals("Apprentice Knight", ClassEvaluator.getTitleDisplayName("knight_1"));
        assertEquals("Knight", ClassEvaluator.getTitleDisplayName("knight_3"));
        assertEquals("Worldbreaker", ClassEvaluator.getTitleDisplayName("knight_5"));
        assertEquals("Apprentice", ClassEvaluator.getTitleDisplayName("Apprentice"));
    }

    @Test
    void classEvaluator_autoSelect_picksHighestCost() {
        PlayerClassData data = new PlayerClassData();
        data.addTitle("knight_1");  // cost: 12
        data.addTitle("knight_3");  // cost: 60
        data.addTitle("duelist_2"); // cost: 15 * 2 = 30
        ClassEvaluator.autoSelectTitle(data);
        assertEquals("knight_3", data.getDisplayedTitle()); // 60 is highest
    }

    @Test
    void classEvaluator_autoSelect_tiesBreakAlphabetically() {
        PlayerClassData data = new PlayerClassData();
        data.addTitle("knight_1");  // cost: 12, name: "Apprentice Knight"
        data.addTitle("duelist_1"); // cost: 6 * 2 = 12, name: "Apprentice Duelist"
        ClassEvaluator.autoSelectTitle(data);
        // "Apprentice Duelist" < "Apprentice Knight" alphabetically
        assertEquals("duelist_1", data.getDisplayedTitle());
    }

    // ══════════════════════════════════════
    //  Updated constants
    // ══════════════════════════════════════

    @Test
    void constants_maxLevel100() {
        assertEquals(100, StatConstants.MAX_LEVEL);
    }

    @Test
    void constants_maxStatPoints500() {
        assertEquals(500, StatConstants.MAX_STAT_POINTS);
    }

    @Test
    void constants_classTitleMinLevel5() {
        assertEquals(5, StatConstants.CLASS_TITLE_MIN_LEVEL);
    }

    @Test
    void constants_singleStatThresholds() {
        assertArrayEquals(new int[]{12, 30, 60, 120, 150}, StatConstants.SINGLE_STAT_THRESHOLDS);
    }

    @Test
    void constants_dualStatThresholds() {
        assertArrayEquals(new int[]{6, 15, 30, 60, 75}, StatConstants.DUAL_STAT_THRESHOLDS);
    }

    @Test
    void constants_threeT5ClassesRequire450Points() {
        // Three T5 classes should require 450 total points, leaving ~50 flex at level 100
        int singleT5Cost = 150;
        int dualT5Cost = 75 * 2;
        assertEquals(singleT5Cost, dualT5Cost); // Both tracks equal
        assertEquals(450, singleT5Cost * 3);
        int totalPointsAtMaxLevel = StatConstants.POINTS_PER_LEVEL * StatConstants.MAX_LEVEL;
        assertEquals(500, totalPointsAtMaxLevel);
        assertEquals(50, totalPointsAtMaxLevel - 450); // 50 flex points
    }

    // ══════════════════════════════════════
    //  All T5 fancy names are unique
    // ══════════════════════════════════════

    @Test
    void classRegistry_allT5NamesAreUnique() {
        var t5Names = new java.util.HashSet<String>();
        for (ClassDefinition cls : ClassRegistry.getAll()) {
            String t5Name = cls.getTier(5).titleName();
            assertTrue(t5Names.add(t5Name), "Duplicate T5 name: " + t5Name);
        }
    }

    @Test
    void classRegistry_allClassesCoverAllStatCombinations() {
        // 6 single-stat + C(6,2) = 15 dual-stat = 21 total
        assertEquals(21, ClassRegistry.getAll().size());
    }

    // ══════════════════════════════════════
    //  Skill system — SkillType
    // ══════════════════════════════════════

    @Test
    void skillType_has9Skills() {
        assertEquals(9, SkillType.values().length);
    }

    @Test
    void skillType_fromName_caseInsensitive() {
        assertEquals(SkillType.MINING, SkillType.fromName("mining"));
        assertEquals(SkillType.MINING, SkillType.fromName("Mining"));
        assertEquals(SkillType.MINING, SkillType.fromName("MINING"));
        assertNull(SkillType.fromName("nonexistent"));
    }

    // ══════════════════════════════════════
    //  Skill system — PlayerSkillData
    // ══════════════════════════════════════

    @Test
    void playerSkillData_defaultState() {
        PlayerSkillData data = new PlayerSkillData();
        for (SkillType skill : SkillType.values()) {
            assertEquals(1, data.getLevel(skill));
            assertEquals(0, data.getXp(skill));
        }
    }

    @Test
    void playerSkillData_addXp_noLevelUp() {
        PlayerSkillData data = new PlayerSkillData();
        int gained = data.addXp(SkillType.MINING, 10);
        assertEquals(0, gained);
        assertEquals(1, data.getLevel(SkillType.MINING));
        assertEquals(10, data.getXp(SkillType.MINING));
    }

    @Test
    void playerSkillData_addXp_levelUp() {
        PlayerSkillData data = new PlayerSkillData();
        // Level 1 needs SKILL_BASE_XP (150) to reach level 2
        int gained = data.addXp(SkillType.MINING, 150);
        assertEquals(1, gained);
        assertEquals(2, data.getLevel(SkillType.MINING));
        assertEquals(0, data.getXp(SkillType.MINING));
    }

    @Test
    void playerSkillData_addXp_multiLevelUp() {
        PlayerSkillData data = new PlayerSkillData();
        // Level 1→2 costs 150, level 2→3 costs 172 → total 322 for 2 levels
        int gained = data.addXp(SkillType.MINING, 322);
        assertEquals(2, gained);
        assertEquals(3, data.getLevel(SkillType.MINING));
    }

    @Test
    void playerSkillData_addXp_capsAtMaxLevel() {
        PlayerSkillData data = new PlayerSkillData();
        data.setLevel(SkillType.MINING, StatConstants.MAX_SKILL_LEVEL);
        int gained = data.addXp(SkillType.MINING, 1000);
        assertEquals(0, gained);
        assertEquals(StatConstants.MAX_SKILL_LEVEL, data.getLevel(SkillType.MINING));
    }

    @Test
    void playerSkillData_xpForNextLevel() {
        PlayerSkillData data = new PlayerSkillData();
        assertEquals(StatConstants.SKILL_BASE_XP, data.xpForNextLevel(SkillType.MINING));
        data.setLevel(SkillType.MINING, 2);
        assertEquals((int)(StatConstants.SKILL_BASE_XP * StatConstants.SKILL_XP_SCALING),
                data.xpForNextLevel(SkillType.MINING));
    }

    // ── Exploration chunk tracking ──

    @Test
    void playerSkillData_chunkTracking() {
        PlayerSkillData data = new PlayerSkillData();
        assertFalse(data.isExplored(0, 0));
        assertTrue(data.markExplored(0, 0));
        assertTrue(data.isExplored(0, 0));
        assertFalse(data.markExplored(0, 0)); // already explored
        assertEquals(1, data.getExploredCount());
    }

    @Test
    void playerSkillData_chunkTracking_negativeCoords() {
        PlayerSkillData data = new PlayerSkillData();
        assertTrue(data.markExplored(-5, -10));
        assertTrue(data.isExplored(-5, -10));
        assertFalse(data.isExplored(5, 10)); // different chunk
    }

    @Test
    void playerSkillData_mapRadius() {
        PlayerSkillData data = new PlayerSkillData();
        // Level 1: base radius only (1/5 = 0)
        assertEquals(StatConstants.BASE_MAP_RADIUS, data.getMapRadius());
        // Level 5: base + 1
        data.setLevel(SkillType.EXPLORATION, 5);
        assertEquals(StatConstants.BASE_MAP_RADIUS + 1, data.getMapRadius());
        // Level 10: base + 2
        data.setLevel(SkillType.EXPLORATION, 10);
        assertEquals(StatConstants.BASE_MAP_RADIUS + 2, data.getMapRadius());
        // Level 50: base + 10
        data.setLevel(SkillType.EXPLORATION, 50);
        assertEquals(StatConstants.BASE_MAP_RADIUS + 10, data.getMapRadius());
        // Level 100: base + 20
        data.setLevel(SkillType.EXPLORATION, 100);
        assertEquals(StatConstants.BASE_MAP_RADIUS + 20, data.getMapRadius());
    }

    // ── Reward calculations ──

    @Test
    void playerSkillData_toolSpeedBonus() {
        PlayerSkillData data = new PlayerSkillData();
        // Level 1: no bonus
        assertEquals(0f, data.getToolSpeedBonus(SkillType.MINING), DELTA);
        // Level 100: max bonus
        data.setLevel(SkillType.MINING, 100);
        float expected = 99 * StatConstants.TOOL_SPEED_PER_LEVEL;
        assertEquals(expected, data.getToolSpeedBonus(SkillType.MINING), DELTA);
        // Verify max is ~14.85% (15% minus one level)
        assertTrue(expected > 0.14f && expected < 0.16f);
    }

    @Test
    void playerSkillData_durabilitySaveChance() {
        PlayerSkillData data = new PlayerSkillData();
        assertEquals(0f, data.getDurabilitySaveChance(SkillType.MINING), DELTA);
        data.setLevel(SkillType.MINING, 100);
        float expected = 99 * StatConstants.DURABILITY_SAVE_PER_LEVEL;
        assertEquals(expected, data.getDurabilitySaveChance(SkillType.MINING), DELTA);
        // Verify max is ~74.25% (75% minus one level)
        assertTrue(expected > 0.73f && expected < 0.76f);
    }

    @Test
    void playerSkillData_materialSaveChance() {
        PlayerSkillData data = new PlayerSkillData();
        assertEquals(0f, data.getMaterialSaveChance(SkillType.CARPENTRY), DELTA);
        data.setLevel(SkillType.CARPENTRY, 100);
        float expected = 99 * StatConstants.MATERIAL_SAVE_PER_LEVEL;
        assertEquals(expected, data.getMaterialSaveChance(SkillType.CARPENTRY), DELTA);
        // Verify max is ~39.6% (40% minus one level)
        assertTrue(expected > 0.38f && expected < 0.41f);
    }

    @Test
    void playerSkillData_craftingSpeedBonus() {
        PlayerSkillData data = new PlayerSkillData();
        assertEquals(0f, data.getCraftingSpeedBonus(SkillType.CARPENTRY), DELTA);
        data.setLevel(SkillType.CARPENTRY, 100);
        float expected = 99 * StatConstants.CRAFTING_SPEED_PER_LEVEL;
        assertEquals(expected, data.getCraftingSpeedBonus(SkillType.CARPENTRY), DELTA);
        // Same scaling as durability save: ~74.25% at max
        assertTrue(expected > 0.73f && expected < 0.76f);
    }

    @Test
    void playerSkillData_craftingSpeedMatchesDurabilitySave() {
        // Crafting speed and durability save use the same per-level rate
        assertEquals(StatConstants.CRAFTING_SPEED_PER_LEVEL,
                StatConstants.DURABILITY_SAVE_PER_LEVEL, DELTA);
    }

    // ── Debug constants ──

    @Test
    void debugSkillLevel_is10() {
        assertEquals(10, StatConstants.DEBUG_SKILL_LEVEL);
    }

    // ── Skill constants ──

    @Test
    void skillConstants_maxSkillLevel() {
        assertEquals(100, StatConstants.MAX_SKILL_LEVEL);
    }

    @Test
    void skillConstants_explorationXpPerChunk() {
        assertEquals(1, StatConstants.EXPLORATION_XP_PER_CHUNK);
    }

    @Test
    void skillConstants_baseMapRadius() {
        assertEquals(1, StatConstants.BASE_MAP_RADIUS);
    }

    @Test
    void skillConstants_levelsPerMapRadius() {
        assertEquals(5, StatConstants.LEVELS_PER_MAP_RADIUS);
    }

    // ── Stat point spending ──

    @Test
    void statPointSpending_newPlayerHasPointsAtLevel() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(10);
        assertEquals(10 * StatConstants.POINTS_PER_LEVEL, data.getTotalPoints());
        assertEquals(data.getTotalPoints(), data.getAvailablePoints());
        assertEquals(0, data.getSpentPoints());
    }

    @Test
    void statPointSpending_spendReducesAvailable() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(5);
        int total = data.getTotalPoints();
        data.setStrength(10);
        assertEquals(10, data.getSpentPoints());
        assertEquals(total - 10, data.getAvailablePoints());
    }

    @Test
    void statPointSpending_cannotOverspend() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(1); // 5 points total
        data.setStrength(3);
        data.setDexterity(2);
        assertEquals(0, data.getAvailablePoints());
        // Setting more than available still works at the data level (command enforces the limit)
        // but available shows 0 via Math.max(0, ...)
        data.setVitality(5);
        assertEquals(0, data.getAvailablePoints());
    }

    @Test
    void statPointSpending_levelUpGivesMorePoints() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(1);
        data.setStrength(5); // spend all 5 points
        assertEquals(0, data.getAvailablePoints());
        // Level up — now have more points
        data.setLevel(2);
        assertEquals(StatConstants.POINTS_PER_LEVEL, data.getAvailablePoints());
    }

    @Test
    void statPointSpending_debugLevel() {
        assertEquals(20, StatConstants.DEBUG_LEVEL);
        PlayerStatData data = new PlayerStatData();
        data.setLevel(StatConstants.DEBUG_LEVEL);
        assertEquals(20 * StatConstants.POINTS_PER_LEVEL, data.getAvailablePoints());
    }

    // ══════════════════════════════════════
    //  PlayerGoldData — gold balance
    // ══════════════════════════════════════

    @Test
    void goldData_startsAtZero() {
        PlayerGoldData gold = new PlayerGoldData();
        assertEquals(0, gold.getGold());
    }

    @Test
    void goldData_setGold() {
        PlayerGoldData gold = new PlayerGoldData();
        gold.setGold(500);
        assertEquals(500, gold.getGold());
    }

    @Test
    void goldData_setGold_negativeClampsToZero() {
        PlayerGoldData gold = new PlayerGoldData();
        gold.setGold(-100);
        assertEquals(0, gold.getGold());
    }

    @Test
    void goldData_addGold() {
        PlayerGoldData gold = new PlayerGoldData();
        gold.setGold(100);
        long result = gold.addGold(50);
        assertEquals(150, result);
        assertEquals(150, gold.getGold());
    }

    @Test
    void goldData_addGold_negativeSubtracts() {
        PlayerGoldData gold = new PlayerGoldData();
        gold.setGold(100);
        gold.addGold(-30);
        assertEquals(70, gold.getGold());
    }

    @Test
    void goldData_addGold_cannotGoBelowZero() {
        PlayerGoldData gold = new PlayerGoldData();
        gold.setGold(10);
        gold.addGold(-999);
        assertEquals(0, gold.getGold());
    }

    @Test
    void goldData_spend_deductsWhenAffordable() {
        PlayerGoldData gold = new PlayerGoldData();
        gold.setGold(100);
        assertTrue(gold.spend(40));
        assertEquals(60, gold.getGold());
    }

    @Test
    void goldData_spend_failsWhenTooExpensive() {
        PlayerGoldData gold = new PlayerGoldData();
        gold.setGold(10);
        assertFalse(gold.spend(50));
        assertEquals(10, gold.getGold());
    }

    @Test
    void goldData_spend_rejectsZeroOrNegative() {
        PlayerGoldData gold = new PlayerGoldData();
        gold.setGold(100);
        assertFalse(gold.spend(0));
        assertFalse(gold.spend(-5));
        assertEquals(100, gold.getGold());
    }

    @Test
    void goldData_canAfford() {
        PlayerGoldData gold = new PlayerGoldData();
        gold.setGold(100);
        assertTrue(gold.canAfford(100));
        assertTrue(gold.canAfford(50));
        assertFalse(gold.canAfford(101));
    }

    @Test
    void goldData_clone_independentCopy() {
        PlayerGoldData gold = new PlayerGoldData();
        gold.setGold(250);
        PlayerGoldData copy = new PlayerGoldData(gold);
        assertEquals(250, copy.getGold());
        copy.addGold(100);
        assertEquals(250, gold.getGold());
        assertEquals(350, copy.getGold());
    }

    // ══════════════════════════════════════
    //  Kill gold calculation
    // ══════════════════════════════════════

    @Test
    void calculateKillGold_referenceHp_givesBaseGold() {
        assertEquals(StatConstants.BASE_KILL_GOLD,
                StatCalculation.calculateKillGold(StatConstants.GOLD_REFERENCE_HP));
    }

    @Test
    void calculateKillGold_zeroHp_givesMinGold() {
        assertEquals(StatConstants.MIN_KILL_GOLD, StatCalculation.calculateKillGold(0));
    }

    @Test
    void calculateKillGold_negativeHp_givesMinGold() {
        assertEquals(StatConstants.MIN_KILL_GOLD, StatCalculation.calculateKillGold(-50));
    }

    @Test
    void calculateKillGold_highHp_cappedAtMax() {
        assertEquals(StatConstants.MAX_KILL_GOLD, StatCalculation.calculateKillGold(99999));
    }

    @Test
    void calculateKillGold_halfHp_givesHalfGold() {
        int gold = StatCalculation.calculateKillGold(StatConstants.GOLD_REFERENCE_HP / 2);
        assertEquals(StatConstants.BASE_KILL_GOLD / 2, gold);
    }

    @Test
    void calculateKillGold_doubleHp_givesDoubleGold() {
        int gold = StatCalculation.calculateKillGold(StatConstants.GOLD_REFERENCE_HP * 2);
        assertEquals(StatConstants.BASE_KILL_GOLD * 2, gold);
    }

    // ══════════════════════════════════════
    //  Quest gold rewards
    // ══════════════════════════════════════

    @Test
    void questDefinition_goldReward_defaultsToZero() {
        QuestDefinition q = new QuestDefinition("test", "Test", "desc",
                QuestObjective.KILL_MOBS, 10, 100);
        assertEquals(0, q.getGoldReward());
    }

    @Test
    void questDefinition_goldReward_setViaConstructor() {
        QuestDefinition q = new QuestDefinition("test", "Test", "desc",
                QuestObjective.KILL_MOBS, 10, 100, 50);
        assertEquals(50, q.getGoldReward());
        assertEquals(100, q.getXpReward());
    }

    @Test
    void weeklyQuests_allHaveGoldRewards() {
        var quests = WeeklyQuestPool.getActiveQuests();
        for (QuestDefinition q : quests) {
            assertTrue(q.getGoldReward() > 0,
                    "Quest '" + q.getName() + "' should have a gold reward");
        }
    }

    // ══════════════════════════════════════
    //  Shop items
    // ══════════════════════════════════════

    @Test
    void shopItem_findById() {
        ShopItem item = ShopItem.findById("anduril");
        assertNotNull(item);
        assertEquals("Andúril, Flame of the West", item.getDisplayName());
        assertEquals(5000, item.getPrice());
    }

    @Test
    void shopItem_findById_caseInsensitive() {
        assertNotNull(ShopItem.findById("ANDURIL"));
        assertNotNull(ShopItem.findById("Anduril"));
    }

    @Test
    void shopItem_findById_invalidReturnsNull() {
        assertNull(ShopItem.findById("nonexistent"));
    }

    @Test
    void shopItem_findByIndex_valid() {
        ShopItem first = ShopItem.findByIndex(1);
        assertNotNull(first);
        assertEquals(ShopItem.values()[0], first);
    }

    @Test
    void shopItem_findByIndex_invalidReturnsNull() {
        assertNull(ShopItem.findByIndex(0));
        assertNull(ShopItem.findByIndex(999));
    }

    @Test
    void shopItem_allHavePositivePrice() {
        for (ShopItem item : ShopItem.values()) {
            assertTrue(item.getPrice() > 0,
                    "Item '" + item.getDisplayName() + "' should have positive price");
        }
    }

    @Test
    void shopItem_allHavePositiveDamage() {
        for (ShopItem item : ShopItem.values()) {
            assertTrue(item.getBonusDamageMultiplier() > 0,
                    "Item '" + item.getDisplayName() + "' should have positive damage");
        }
    }

    // ══════════════════════════════════════
    //  Player equipment data
    // ══════════════════════════════════════

    @Test
    void equipmentData_startsEmpty() {
        PlayerEquipmentData data = new PlayerEquipmentData();
        assertTrue(data.getOwnedItems().isEmpty());
        assertEquals("", data.getEquippedItem());
        assertNull(data.getEquippedShopItem());
    }

    @Test
    void equipmentData_addAndOwn() {
        PlayerEquipmentData data = new PlayerEquipmentData();
        data.addOwnedItem("anduril");
        assertTrue(data.ownsItem("anduril"));
        assertFalse(data.ownsItem("stormbreaker"));
    }

    @Test
    void equipmentData_noDuplicates() {
        PlayerEquipmentData data = new PlayerEquipmentData();
        data.addOwnedItem("anduril");
        data.addOwnedItem("anduril");
        assertEquals(1, data.getOwnedItems().size());
    }

    @Test
    void equipmentData_equipAndGet() {
        PlayerEquipmentData data = new PlayerEquipmentData();
        data.addOwnedItem("anduril");
        data.setEquippedItem("anduril");
        assertEquals("anduril", data.getEquippedItem());
        ShopItem equipped = data.getEquippedShopItem();
        assertNotNull(equipped);
        assertEquals(ShopItem.ANDURIL, equipped);
    }

    @Test
    void equipmentData_unequip() {
        PlayerEquipmentData data = new PlayerEquipmentData();
        data.setEquippedItem("anduril");
        data.setEquippedItem("");
        assertEquals("", data.getEquippedItem());
        assertNull(data.getEquippedShopItem());
    }

    @Test
    void equipmentData_clone_independent() {
        PlayerEquipmentData data = new PlayerEquipmentData();
        data.addOwnedItem("anduril");
        data.setEquippedItem("anduril");
        PlayerEquipmentData copy = new PlayerEquipmentData(data);
        assertEquals("anduril", copy.getEquippedItem());
        assertTrue(copy.ownsItem("anduril"));
        // Mutations are independent
        copy.addOwnedItem("stormbreaker");
        assertFalse(data.ownsItem("stormbreaker"));
    }

    @Test
    void formatPercent_calculation() {
        // Test percentage formatting logic (same as ShopCommand.formatPercent)
        assertEquals("150%", (int) (1.5f * 100) + "%");
        assertEquals("85%", (int) (0.85f * 100) + "%");
        assertEquals("250%", (int) (2.5f * 100) + "%");
    }

    // ══════════════════════════════════════
    //  PlayerStatData — Kill Counter
    // ══════════════════════════════════════

    @Test
    void kills_startsAtZero() {
        PlayerStatData data = new PlayerStatData();
        assertEquals(0, data.getKills());
    }

    @Test
    void kills_addKillIncrements() {
        PlayerStatData data = new PlayerStatData();
        data.addKill();
        assertEquals(1, data.getKills());
        data.addKill();
        data.addKill();
        assertEquals(3, data.getKills());
    }

    @Test
    void kills_setKills() {
        PlayerStatData data = new PlayerStatData();
        data.setKills(42);
        assertEquals(42, data.getKills());
    }

    @Test
    void kills_setKills_negativeClampsToZero() {
        PlayerStatData data = new PlayerStatData();
        data.setKills(-5);
        assertEquals(0, data.getKills());
    }

    @Test
    void kills_clone_copiesKills() {
        PlayerStatData data = new PlayerStatData();
        data.addKill();
        data.addKill();
        data.addKill();
        PlayerStatData copy = new PlayerStatData(data);
        assertEquals(3, copy.getKills());
    }

    // ══════════════════════════════════════
    //  Stat allocation — point spending logic
    // ══════════════════════════════════════

    @Test
    void allocate_spendSinglePoint() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(5); // 25 points
        int before = data.getAvailablePoints();
        data.setStrength(data.getStrength() + 1);
        assertEquals(before - 1, data.getAvailablePoints());
    }

    @Test
    void allocate_spendMultiplePoints() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(10); // 50 points
        data.setDexterity(data.getDexterity() + 10);
        assertEquals(40, data.getAvailablePoints());
    }

    @Test
    void allocate_capAtAvailable() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(1); // 5 points
        int available = data.getAvailablePoints();
        int toSpend = Math.min(10, available);
        data.setStrength(data.getStrength() + toSpend);
        assertEquals(0, data.getAvailablePoints());
        assertEquals(5, data.getStrength());
    }

    @Test
    void allocate_allSixStats() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(10); // 50 points
        data.setStrength(8);
        data.setDexterity(8);
        data.setVitality(8);
        data.setIntellect(8);
        data.setPresence(8);
        data.setArcana(8);
        assertEquals(50 - 48, data.getAvailablePoints());
        assertEquals(48, data.getSpentPoints());
    }

    @Test
    void allocate_cannotGoNegativePoints() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(1); // 5 points
        data.setStrength(5);
        // Available is 0, no more can be spent
        assertEquals(0, data.getAvailablePoints());
    }

    // ══════════════════════════════════════
    //  Gold trading — pure data logic
    // ══════════════════════════════════════

    @Test
    void trade_senderLosesGold() {
        PlayerGoldData sender = new PlayerGoldData();
        sender.setGold(500);
        PlayerGoldData receiver = new PlayerGoldData();
        receiver.setGold(100);

        int amount = 200;
        assertTrue(sender.canAfford(amount));
        sender.spend(amount);
        receiver.addGold(amount);

        assertEquals(300, sender.getGold());
        assertEquals(300, receiver.getGold());
    }

    @Test
    void trade_insufficientGold_fails() {
        PlayerGoldData sender = new PlayerGoldData();
        sender.setGold(50);

        assertFalse(sender.canAfford(100));
        assertFalse(sender.spend(100));
        assertEquals(50, sender.getGold()); // unchanged
    }

    @Test
    void trade_exactAmount() {
        PlayerGoldData sender = new PlayerGoldData();
        sender.setGold(100);
        PlayerGoldData receiver = new PlayerGoldData();
        receiver.setGold(0);

        assertTrue(sender.spend(100));
        receiver.addGold(100);

        assertEquals(0, sender.getGold());
        assertEquals(100, receiver.getGold());
    }

    @Test
    void trade_zeroAmount_rejected() {
        PlayerGoldData sender = new PlayerGoldData();
        sender.setGold(100);
        assertFalse(sender.spend(0));
    }

    @Test
    void trade_negativeAmount_rejected() {
        PlayerGoldData sender = new PlayerGoldData();
        sender.setGold(100);
        assertFalse(sender.spend(-10));
    }

    @Test
    void trade_multipleTradesChained() {
        PlayerGoldData a = new PlayerGoldData();
        a.setGold(1000);
        PlayerGoldData b = new PlayerGoldData();
        b.setGold(500);

        // A sends 300 to B
        a.spend(300);
        b.addGold(300);
        assertEquals(700, a.getGold());
        assertEquals(800, b.getGold());

        // B sends 400 back to A
        b.spend(400);
        a.addGold(400);
        assertEquals(1100, a.getGold());
        assertEquals(400, b.getGold());
    }

    // ══════════════════════════════════════
    //  Mob level — distance-based calculation
    // ══════════════════════════════════════

    @Test
    void mobLevel_atSpawn_isLevel1() {
        assertEquals(1, StatCalculation.calculateMobLevel(0, 0));
    }

    @Test
    void mobLevel_nearSpawn_isLevel1() {
        // Within first 100 blocks = level 1
        assertEquals(1, StatCalculation.calculateMobLevel(50, 0));
        assertEquals(1, StatCalculation.calculateMobLevel(0, 50));
    }

    @Test
    void mobLevel_at100Blocks_isLevel2() {
        assertEquals(2, StatCalculation.calculateMobLevel(100, 0));
    }

    @Test
    void mobLevel_at200Blocks_isLevel3() {
        assertEquals(3, StatCalculation.calculateMobLevel(200, 0));
    }

    @Test
    void mobLevel_diagonalDistance() {
        // 141 blocks diagonal ≈ sqrt(100^2 + 100^2) = 141.4
        int level = StatCalculation.calculateMobLevel(100, 100);
        assertEquals(2, level); // distance ~141, / 100 = 1.41 → floor + 1 = 2
    }

    @Test
    void mobLevel_at3000Blocks_isLevel31() {
        // 3000 / 100 = 30 → level 31
        assertEquals(31, StatCalculation.calculateMobLevel(3000, 0));
    }

    @Test
    void mobLevel_negativeCoords() {
        // Distance is absolute
        assertEquals(StatCalculation.calculateMobLevel(500, 0),
                StatCalculation.calculateMobLevel(-500, 0));
    }

    @Test
    void mobLevel_cappedAtMax() {
        int level = StatCalculation.calculateMobLevel(999999, 0);
        assertEquals(StatConstants.MAX_MOB_LEVEL, level);
    }

    @Test
    void mobLevel_progressionToLevel30() {
        // Level 30 should be reached at ~2900 blocks
        int level = StatCalculation.calculateMobLevel(2900, 0);
        assertEquals(30, level);
    }

    // ══════════════════════════════════════
    //  Mob HP multiplier
    // ══════════════════════════════════════

    @Test
    void mobHpMult_level1_is1x() {
        assertEquals(1.0f, StatCalculation.calculateMobHpMultiplier(1), DELTA);
    }

    @Test
    void mobHpMult_level10_scales() {
        float expected = 1.0f + 9 * StatConstants.MOB_HP_SCALE_PER_LEVEL;
        assertEquals(expected, StatCalculation.calculateMobHpMultiplier(10), DELTA);
    }

    @Test
    void mobHpMult_level50_scales() {
        float expected = 1.0f + 49 * StatConstants.MOB_HP_SCALE_PER_LEVEL;
        assertEquals(expected, StatCalculation.calculateMobHpMultiplier(50), DELTA);
    }

    @Test
    void mobHpMult_alwaysPositive() {
        for (int level = 1; level <= StatConstants.MAX_MOB_LEVEL; level++) {
            assertTrue(StatCalculation.calculateMobHpMultiplier(level) >= 1.0f);
        }
    }

    // ══════════════════════════════════════
    //  Mob damage multiplier
    // ══════════════════════════════════════

    @Test
    void mobDmgMult_level1_is1x() {
        assertEquals(1.0f, StatCalculation.calculateMobDamageMultiplier(1), DELTA);
    }

    @Test
    void mobDmgMult_level10_scales() {
        float expected = 1.0f + 9 * StatConstants.MOB_DAMAGE_SCALE_PER_LEVEL;
        assertEquals(expected, StatCalculation.calculateMobDamageMultiplier(10), DELTA);
    }

    @Test
    void mobDmgMult_level50_scales() {
        float expected = 1.0f + 49 * StatConstants.MOB_DAMAGE_SCALE_PER_LEVEL;
        assertEquals(expected, StatCalculation.calculateMobDamageMultiplier(50), DELTA);
    }

    @Test
    void mobDmgMult_alwaysPositive() {
        for (int level = 1; level <= StatConstants.MAX_MOB_LEVEL; level++) {
            assertTrue(StatCalculation.calculateMobDamageMultiplier(level) >= 1.0f);
        }
    }

    // ══════════════════════════════════════
    //  XP scaling by level difference
    // ══════════════════════════════════════

    @Test
    void xpLevelMult_sameLevel_is1x() {
        assertEquals(1.0f, StatCalculation.calculateLevelDifferenceXpMultiplier(10, 10), DELTA);
    }

    @Test
    void xpLevelMult_mobHigher_givesBonus() {
        float mult = StatCalculation.calculateLevelDifferenceXpMultiplier(15, 10);
        assertTrue(mult > 1.0f, "Higher level mob should give bonus XP");
    }

    @Test
    void xpLevelMult_mobHigher_cappedAtMax() {
        // Exactly at cap (+10 levels)
        float multAtCap = StatCalculation.calculateLevelDifferenceXpMultiplier(20, 10);
        // Well beyond cap (+20 levels)
        float multBeyond = StatCalculation.calculateLevelDifferenceXpMultiplier(30, 10);
        // Both should cap at 1.0 + XP_HIGH_LEVEL_BONUS
        assertEquals(1.0f + StatConstants.XP_HIGH_LEVEL_BONUS, multAtCap, DELTA);
        assertEquals(1.0f + StatConstants.XP_HIGH_LEVEL_BONUS, multBeyond, DELTA);
    }

    @Test
    void xpLevelMult_mobSlightlyLower_nopenalty() {
        // Within penalty start threshold (3 levels below = no penalty)
        float mult = StatCalculation.calculateLevelDifferenceXpMultiplier(8, 10);
        assertEquals(1.0f, mult, DELTA);
    }

    @Test
    void xpLevelMult_mob3Below_noPenalty() {
        // Exactly at penalty start boundary
        float mult = StatCalculation.calculateLevelDifferenceXpMultiplier(7, 10);
        assertEquals(1.0f, mult, DELTA);
    }

    @Test
    void xpLevelMult_mob4Below_startsPenalty() {
        // 4 levels below: just past the penalty_start of 3
        float mult = StatCalculation.calculateLevelDifferenceXpMultiplier(6, 10);
        assertTrue(mult < 1.0f, "Mob 4 levels below should get XP penalty");
        assertTrue(mult > StatConstants.XP_LOW_LEVEL_FLOOR,
                "Should not hit floor yet at 4 levels below");
    }

    @Test
    void xpLevelMult_veryLowMob_hitsFloor() {
        // 15+ levels below should be at floor
        float mult = StatCalculation.calculateLevelDifferenceXpMultiplier(1, 50);
        assertEquals(StatConstants.XP_LOW_LEVEL_FLOOR, mult, DELTA);
    }

    @Test
    void xpLevelMult_penaltyIsGradual() {
        float mult5 = StatCalculation.calculateLevelDifferenceXpMultiplier(5, 10);
        float mult2 = StatCalculation.calculateLevelDifferenceXpMultiplier(2, 10);
        float mult1 = StatCalculation.calculateLevelDifferenceXpMultiplier(1, 50);
        // As gap grows, multiplier should decrease
        assertTrue(mult5 > mult2, "Closer mob should give more XP");
        assertTrue(mult2 > mult1, "Mid-gap should give more XP than large gap");
    }

    @Test
    void xpLevelMult_mob1Above_givesSmallBonus() {
        float mult = StatCalculation.calculateLevelDifferenceXpMultiplier(11, 10);
        assertTrue(mult > 1.0f);
        assertTrue(mult < 1.0f + StatConstants.XP_HIGH_LEVEL_BONUS);
    }

    @Test
    void xpLevelMult_symmetry_bonusAndPenalty() {
        // Bonus at +10 cap
        float bonus = StatCalculation.calculateLevelDifferenceXpMultiplier(20, 10);
        // Penalty way below
        float penalty = StatCalculation.calculateLevelDifferenceXpMultiplier(1, 50);
        assertTrue(bonus > 1.0f);
        assertTrue(penalty < 1.0f);
        // The bonus caps at 50%, the penalty floors at 10%
        assertEquals(1.0f + StatConstants.XP_HIGH_LEVEL_BONUS, bonus, DELTA);
        assertEquals(StatConstants.XP_LOW_LEVEL_FLOOR, penalty, DELTA);
    }

    // ══════════════════════════════════════
    //  Mob level constants
    // ══════════════════════════════════════

    @Test
    void mobConstants_blocksPerLevel() {
        assertEquals(100.0f, StatConstants.BLOCKS_PER_MOB_LEVEL, DELTA);
    }

    @Test
    void mobConstants_maxMobLevel() {
        assertEquals(100, StatConstants.MAX_MOB_LEVEL);
    }

    @Test
    void mobConstants_hpScalePerLevel() {
        assertEquals(0.08f, StatConstants.MOB_HP_SCALE_PER_LEVEL, DELTA);
    }

    @Test
    void mobConstants_damageScalePerLevel() {
        assertEquals(0.05f, StatConstants.MOB_DAMAGE_SCALE_PER_LEVEL, DELTA);
    }

    @Test
    void mobConstants_xpHighLevelBonus() {
        assertEquals(0.50f, StatConstants.XP_HIGH_LEVEL_BONUS, DELTA);
    }

    @Test
    void mobConstants_xpLowLevelFloor() {
        assertEquals(0.10f, StatConstants.XP_LOW_LEVEL_FLOOR, DELTA);
    }

    // ════════════════════════════════════════════════════════════════
    //  CLAIM DATA TESTS
    // ════════════════════════════════════════════════════════════════

    @Test
    void claimData_defaultState() {
        ClaimData data = new ClaimData();
        assertEquals(0, data.getClaimCount());
        assertTrue(data.getClaimedChunkSet().isEmpty());
        assertTrue(data.getTrustedPlayerSet().isEmpty());
        assertEquals(0, data.getSettings());
    }

    @Test
    void claimData_addAndRemoveClaim() {
        ClaimData data = new ClaimData();
        assertTrue(data.addClaim(5, 10));
        assertTrue(data.hasClaim(5, 10));
        assertEquals(1, data.getClaimCount());

        // Can't add duplicate
        assertFalse(data.addClaim(5, 10));
        assertEquals(1, data.getClaimCount());

        // Add second chunk
        assertTrue(data.addClaim(6, 10));
        assertEquals(2, data.getClaimCount());

        // Remove
        assertTrue(data.removeClaim(5, 10));
        assertFalse(data.hasClaim(5, 10));
        assertEquals(1, data.getClaimCount());

        // Can't remove non-existent
        assertFalse(data.removeClaim(5, 10));
    }

    @Test
    void claimData_negativeChunkCoords() {
        ClaimData data = new ClaimData();
        assertTrue(data.addClaim(-3, -7));
        assertTrue(data.hasClaim(-3, -7));
        assertFalse(data.hasClaim(3, 7));
    }

    @Test
    void claimData_trustedPlayers() {
        ClaimData data = new ClaimData();
        String uuid1 = "uuid-1111";
        String uuid2 = "uuid-2222";

        assertFalse(data.isTrusted(uuid1));
        assertTrue(data.addTrusted(uuid1));
        assertTrue(data.isTrusted(uuid1));
        assertFalse(data.isTrusted(uuid2));

        // Can't add duplicate
        assertFalse(data.addTrusted(uuid1));

        // Add second
        assertTrue(data.addTrusted(uuid2));
        assertTrue(data.isTrusted(uuid2));

        // Remove
        assertTrue(data.removeTrusted(uuid1));
        assertFalse(data.isTrusted(uuid1));
        assertFalse(data.removeTrusted(uuid1)); // already removed
    }

    @Test
    void claimData_settingsFlags() {
        ClaimData data = new ClaimData();

        // All flags off by default
        assertFalse(data.hasFlag(ClaimData.FLAG_BREAK_BLOCKS));
        assertFalse(data.hasFlag(ClaimData.FLAG_PLACE_BLOCKS));
        assertFalse(data.hasFlag(ClaimData.FLAG_OPEN_DOORS));
        assertFalse(data.hasFlag(ClaimData.FLAG_PVP));

        // Set flags
        data.setFlag(ClaimData.FLAG_BREAK_BLOCKS, true);
        assertTrue(data.hasFlag(ClaimData.FLAG_BREAK_BLOCKS));
        assertFalse(data.hasFlag(ClaimData.FLAG_PLACE_BLOCKS));

        // Toggle
        boolean newState = data.toggleFlag(ClaimData.FLAG_BREAK_BLOCKS);
        assertFalse(newState); // was on, now off
        assertFalse(data.hasFlag(ClaimData.FLAG_BREAK_BLOCKS));

        newState = data.toggleFlag(ClaimData.FLAG_OPEN_DOORS);
        assertTrue(newState); // was off, now on
        assertTrue(data.hasFlag(ClaimData.FLAG_OPEN_DOORS));
    }

    @Test
    void claimData_getFlagByName() {
        assertEquals(ClaimData.FLAG_BREAK_BLOCKS, ClaimData.getFlagByName("break"));
        assertEquals(ClaimData.FLAG_PLACE_BLOCKS, ClaimData.getFlagByName("place"));
        assertEquals(ClaimData.FLAG_OPEN_DOORS, ClaimData.getFlagByName("doors"));
        assertEquals(ClaimData.FLAG_OPEN_CONTAINERS, ClaimData.getFlagByName("containers"));
        assertEquals(ClaimData.FLAG_PVP, ClaimData.getFlagByName("pvp"));
        assertEquals(ClaimData.FLAG_MOB_DAMAGE, ClaimData.getFlagByName("mobdamage"));
        assertEquals(-1, ClaimData.getFlagByName("nonexistent"));
        assertEquals(ClaimData.FLAG_BREAK_BLOCKS, ClaimData.getFlagByName("BREAK")); // case insensitive
    }

    @Test
    void claimData_blockToChunk() {
        assertEquals(0, ClaimData.blockToChunk(0));
        assertEquals(0, ClaimData.blockToChunk(15));
        assertEquals(1, ClaimData.blockToChunk(16));
        assertEquals(1, ClaimData.blockToChunk(31));
        assertEquals(2, ClaimData.blockToChunk(32));
        assertEquals(-1, ClaimData.blockToChunk(-1));
        assertEquals(-1, ClaimData.blockToChunk(-16));
        assertEquals(-2, ClaimData.blockToChunk(-17));
    }

    @Test
    void claimData_clone() {
        ClaimData data = new ClaimData();
        data.addClaim(1, 2);
        data.addTrusted("test-uuid");
        data.setFlag(ClaimData.FLAG_PVP, true);

        ClaimData clone = (ClaimData) data.clone();
        assertTrue(clone.hasClaim(1, 2));
        assertTrue(clone.isTrusted("test-uuid"));
        assertTrue(clone.hasFlag(ClaimData.FLAG_PVP));
    }

    @Test
    void claimData_multipleFlagsIndependent() {
        ClaimData data = new ClaimData();
        data.setFlag(ClaimData.FLAG_BREAK_BLOCKS, true);
        data.setFlag(ClaimData.FLAG_OPEN_DOORS, true);

        assertTrue(data.hasFlag(ClaimData.FLAG_BREAK_BLOCKS));
        assertTrue(data.hasFlag(ClaimData.FLAG_OPEN_DOORS));
        assertFalse(data.hasFlag(ClaimData.FLAG_PLACE_BLOCKS));

        data.setFlag(ClaimData.FLAG_BREAK_BLOCKS, false);
        assertFalse(data.hasFlag(ClaimData.FLAG_BREAK_BLOCKS));
        assertTrue(data.hasFlag(ClaimData.FLAG_OPEN_DOORS)); // still on
    }

    // ════════════════════════════════════════════════════════════════
    //  CLAIM REGISTRY TESTS
    // ════════════════════════════════════════════════════════════════

    @Test
    void claimRegistry_registerAndLookup() {
        ClaimRegistry.clearAll();
        assertFalse(ClaimRegistry.isClaimed(0, 0));
        assertNull(ClaimRegistry.getOwner(0, 0));

        ClaimRegistry.register(0, 0, "owner-uuid");
        assertTrue(ClaimRegistry.isClaimed(0, 0));
        assertEquals("owner-uuid", ClaimRegistry.getOwner(0, 0));
        assertTrue(ClaimRegistry.isOwnedBy(0, 0, "owner-uuid"));
        assertFalse(ClaimRegistry.isOwnedBy(0, 0, "other-uuid"));
    }

    @Test
    void claimRegistry_unregister() {
        ClaimRegistry.clearAll();
        ClaimRegistry.register(5, 5, "owner");
        ClaimRegistry.unregister(5, 5);
        assertFalse(ClaimRegistry.isClaimed(5, 5));
    }

    @Test
    void claimRegistry_getChunksOwnedBy() {
        ClaimRegistry.clearAll();
        ClaimRegistry.register(1, 1, "player1");
        ClaimRegistry.register(2, 2, "player1");
        ClaimRegistry.register(3, 3, "player2");

        var chunks = ClaimRegistry.getChunksOwnedBy("player1");
        assertEquals(2, chunks.size());
        assertTrue(chunks.contains("1:1"));
        assertTrue(chunks.contains("2:2"));
    }

    @Test
    void claimRegistry_guildCache() {
        ClaimRegistry.clearAll();
        ClaimRegistry.setPlayerGuild("p1", "TestGuild");
        ClaimRegistry.setPlayerGuild("p2", "TestGuild");
        ClaimRegistry.setPlayerGuild("p3", "OtherGuild");

        assertTrue(ClaimRegistry.areInSameGuild("p1", "p2"));
        assertFalse(ClaimRegistry.areInSameGuild("p1", "p3"));
        assertFalse(ClaimRegistry.areInSameGuild("p1", "p4"));
        assertEquals("TestGuild", ClaimRegistry.getPlayerGuild("p1"));

        ClaimRegistry.setPlayerGuild("p1", null);
        assertNull(ClaimRegistry.getPlayerGuild("p1"));
        assertFalse(ClaimRegistry.areInSameGuild("p1", "p2"));
    }

    @Test
    void claimRegistry_totalClaims() {
        ClaimRegistry.clearAll();
        assertEquals(0, ClaimRegistry.getTotalClaims());
        ClaimRegistry.register(0, 0, "a");
        ClaimRegistry.register(1, 1, "b");
        assertEquals(2, ClaimRegistry.getTotalClaims());
    }

    // ════════════════════════════════════════════════════════════════
    //  GUILD DATA TESTS
    // ════════════════════════════════════════════════════════════════

    @Test
    void guildData_defaultState() {
        GuildData data = new GuildData();
        assertFalse(data.isInGuild());
        assertEquals("", data.getGuildName());
        assertEquals("", data.getGuildRank());
        assertEquals(0, data.getGoldDonated());
        assertEquals(0, data.getQuestsCompleted());
    }

    @Test
    void guildData_createGuild() {
        GuildData data = new GuildData();
        data.createGuild("TestGuild");
        assertTrue(data.isInGuild());
        assertEquals("TestGuild", data.getGuildName());
        assertTrue(data.isLeader());
        assertTrue(data.isOfficerOrAbove());
    }

    @Test
    void guildData_joinGuild() {
        GuildData data = new GuildData();
        data.joinGuild("TestGuild");
        assertTrue(data.isInGuild());
        assertEquals("TestGuild", data.getGuildName());
        assertEquals("member", data.getGuildRank());
        assertFalse(data.isLeader());
        assertFalse(data.isOfficerOrAbove());
    }

    @Test
    void guildData_leaveGuild() {
        GuildData data = new GuildData();
        data.createGuild("TestGuild");
        data.addGoldDonated(100);
        data.incrementQuestsCompleted();
        data.leaveGuild();

        assertFalse(data.isInGuild());
        assertEquals("", data.getGuildName());
        assertEquals(0, data.getGoldDonated());
        assertEquals(0, data.getQuestsCompleted());
    }

    @Test
    void guildData_ranks() {
        GuildData data = new GuildData();
        data.joinGuild("G");

        // Default member
        assertFalse(data.isLeader());
        assertFalse(data.isOfficerOrAbove());

        // Promote to officer
        data.setGuildRank("officer");
        assertFalse(data.isLeader());
        assertTrue(data.isOfficerOrAbove());

        // Promote to leader
        data.setGuildRank("leader");
        assertTrue(data.isLeader());
        assertTrue(data.isOfficerOrAbove());
    }

    @Test
    void guildData_rankTitles() {
        GuildData data = new GuildData();
        assertEquals("None", data.getRankTitle());

        data.createGuild("G");
        assertEquals("Guild Leader", data.getRankTitle());

        data.setGuildRank("officer");
        assertEquals("Officer", data.getRankTitle());

        data.setGuildRank("member");
        assertEquals("Recruit", data.getRankTitle()); // 0 quests

        data.setQuestsCompleted(5);
        assertEquals("Initiate", data.getRankTitle());

        data.setQuestsCompleted(20);
        assertEquals("Regular", data.getRankTitle());

        data.setQuestsCompleted(50);
        assertEquals("Veteran", data.getRankTitle());

        data.setQuestsCompleted(100);
        assertEquals("Champion", data.getRankTitle());
    }

    @Test
    void guildData_donations() {
        GuildData data = new GuildData();
        data.joinGuild("G");
        assertEquals(0, data.getGoldDonated());
        data.addGoldDonated(50);
        assertEquals(50, data.getGoldDonated());
        data.addGoldDonated(30);
        assertEquals(80, data.getGoldDonated());
    }

    @Test
    void guildData_clone() {
        GuildData data = new GuildData();
        data.createGuild("TestGuild");
        data.addGoldDonated(200);
        data.setQuestsCompleted(10);

        GuildData clone = (GuildData) data.clone();
        assertEquals("TestGuild", clone.getGuildName());
        assertTrue(clone.isLeader());
        assertEquals(200, clone.getGoldDonated());
        assertEquals(10, clone.getQuestsCompleted());
    }

    // ════════════════════════════════════════════════════════════════
    //  GUILD REGISTRY TESTS
    // ════════════════════════════════════════════════════════════════

    @Test
    void guildRegistry_createGuild() {
        GuildRegistry.clearAll();
        GuildRegistry.GuildInfo info = GuildRegistry.createGuild("TestGuild", "leader-uuid");
        assertNotNull(info);
        assertEquals("TestGuild", info.getName());
        assertEquals(1, info.getLevel());
        assertEquals(1, info.getMemberCount());

        // Can't create duplicate
        assertNull(GuildRegistry.createGuild("TestGuild", "other-uuid"));
        // Case insensitive
        assertNull(GuildRegistry.createGuild("testguild", "other-uuid"));
    }

    @Test
    void guildRegistry_memberManagement() {
        GuildRegistry.clearAll();
        GuildRegistry.createGuild("G", "leader");
        GuildRegistry.addMember("G", "member1");
        GuildRegistry.addMember("G", "member2");

        GuildRegistry.GuildInfo info = GuildRegistry.getGuild("G");
        assertEquals(3, info.getMemberCount());

        GuildRegistry.removeMember("G", "member1");
        assertEquals(2, info.getMemberCount());
    }

    @Test
    void guildRegistry_guildLevel_fromXp() {
        // Level 1 at 0 XP
        assertEquals(1, GuildRegistry.calculateLevel(0));
        // Level 2 at 1000 XP (exactly GUILD_BASE_XP)
        assertEquals(2, GuildRegistry.calculateLevel(GuildRegistry.GUILD_BASE_XP));
        // Level 1 at 999 XP
        assertEquals(1, GuildRegistry.calculateLevel(999));
    }

    @Test
    void guildRegistry_donateAndLevel() {
        GuildRegistry.clearAll();
        GuildRegistry.createGuild("G", "leader");
        assertEquals(1, GuildRegistry.getGuildLevel("G"));

        // Donate enough for level 2
        GuildRegistry.donateGold("G", GuildRegistry.GUILD_BASE_XP);
        assertEquals(2, GuildRegistry.getGuildLevel("G"));
    }

    @Test
    void guildRegistry_sharedXpBonus() {
        GuildRegistry.clearAll();
        assertEquals(0.0f, GuildRegistry.getSharedXpBonus("nonexistent"), DELTA);

        GuildRegistry.createGuild("G", "leader");
        assertEquals(GuildRegistry.SHARED_XP_BONUS_PER_LEVEL, GuildRegistry.getSharedXpBonus("G"), DELTA);

        // Donate to reach level 2
        GuildRegistry.donateGold("G", GuildRegistry.GUILD_BASE_XP);
        assertEquals(2 * GuildRegistry.SHARED_XP_BONUS_PER_LEVEL, GuildRegistry.getSharedXpBonus("G"), DELTA);
    }

    @Test
    void guildRegistry_disbandGuild() {
        GuildRegistry.clearAll();
        GuildRegistry.createGuild("G", "leader");
        assertTrue(GuildRegistry.guildExists("G"));

        GuildRegistry.disbandGuild("G");
        assertFalse(GuildRegistry.guildExists("G"));
        assertEquals(0, GuildRegistry.getGuildLevel("G"));
    }

    @Test
    void guildRegistry_getAllGuildNames() {
        GuildRegistry.clearAll();
        GuildRegistry.createGuild("Alpha", "p1");
        GuildRegistry.createGuild("Beta", "p2");

        var names = GuildRegistry.getAllGuildNames();
        assertEquals(2, names.size());
        assertTrue(names.contains("Alpha"));
        assertTrue(names.contains("Beta"));
    }

    @Test
    void guildRegistry_guildInfoXpTracking() {
        GuildRegistry.clearAll();
        GuildRegistry.createGuild("G", "leader");
        GuildRegistry.GuildInfo info = GuildRegistry.getGuild("G");

        assertEquals(0, info.getTotalXp());
        info.addXp(500);
        assertEquals(500, info.getTotalXp());
        assertEquals(500, info.getXpInCurrentLevel());
        assertEquals(GuildRegistry.GUILD_BASE_XP, info.getXpForNextLevel());
    }

    @Test
    void guildRegistry_maxLevel() {
        // Very high XP should cap at MAX_GUILD_LEVEL
        int level = GuildRegistry.calculateLevel(999_999_999);
        assertEquals(GuildRegistry.MAX_GUILD_LEVEL, level);
    }

    @Test
    void guildRegistry_xpForNextLevelAtMax() {
        GuildRegistry.clearAll();
        GuildRegistry.createGuild("G", "leader");
        GuildRegistry.GuildInfo info = GuildRegistry.getGuild("G");
        info.addXp(999_999_999); // way past max
        assertEquals(0, info.getXpForNextLevel()); // at max, no next level
    }

    // ════════════════════════════════════════════════════════════════
    //  CLAIM COMMAND CONSTANTS
    // ════════════════════════════════════════════════════════════════

    @Test
    void claimCommand_maxClaimsConstant() {
        assertTrue(ClaimCommand.BASE_MAX_CLAIMS > 0);
        assertTrue(ClaimCommand.CLAIMS_PER_GUILD_LEVEL > 0);
    }

    @Test
    void guildRegistry_creationCost() {
        assertTrue(GuildRegistry.GUILD_CREATION_COST > 0);
    }

    @Test
    void guildRegistry_sharedXpBonusCapped() {
        // Actual bonus at max level should be capped
        GuildRegistry.clearAll();
        GuildRegistry.createGuild("CappedTest", "leader-cap");
        GuildRegistry.getGuild("CappedTest").addXp(999_999_999);
        float bonus = GuildRegistry.getSharedXpBonus("CappedTest");
        assertTrue(bonus <= GuildRegistry.MAX_SHARED_XP_BONUS + DELTA,
                "bonus=" + bonus + " should <= " + (GuildRegistry.MAX_SHARED_XP_BONUS + DELTA));
        assertTrue(bonus > 0, "bonus should be positive at max level");
    }

    @Test
    void claimData_allFlagNames() {
        var flags = ClaimData.getAllFlags();
        assertTrue(flags.containsKey("break"));
        assertTrue(flags.containsKey("place"));
        assertTrue(flags.containsKey("doors"));
        assertTrue(flags.containsKey("containers"));
        assertTrue(flags.containsKey("pvp"));
        assertTrue(flags.containsKey("mobdamage"));
        assertEquals(6, flags.size());
    }

    @Test
    void claimData_flagBitsAreDistinct() {
        // Each flag should be a unique power of 2
        int[] flags = {
            ClaimData.FLAG_BREAK_BLOCKS,
            ClaimData.FLAG_PLACE_BLOCKS,
            ClaimData.FLAG_OPEN_DOORS,
            ClaimData.FLAG_OPEN_CONTAINERS,
            ClaimData.FLAG_PVP,
            ClaimData.FLAG_MOB_DAMAGE
        };
        for (int i = 0; i < flags.length; i++) {
            for (int j = i + 1; j < flags.length; j++) {
                assertNotEquals(flags[i], flags[j], "Flags " + i + " and " + j + " collide");
                assertEquals(0, flags[i] & flags[j], "Flags " + i + " and " + j + " overlap bits");
            }
        }
    }
}
