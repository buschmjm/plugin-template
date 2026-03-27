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
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

/**
 * Interactive UI page for viewing and modifying the six base stats.
 * Opened via the /stats command.
 */
public class StatsUI extends InteractiveCustomUIPage<StatsUI.UIData> {

    private static final Logger LOGGER = Logger.getLogger("MMORPGStats");
    private boolean awaitingConfirm = false;

    // Pending (unsaved) additions per stat - index 0=STR,1=DEX,2=VIT,3=INT,4=PRE,5=ARC
    private final int[] pending = new int[6];
    // Snapshot of committed stats when page opened
    private final int[] committed = new int[6];

    public StatsUI(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, UIData.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder,
                      @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("StatsScreen.ui");

        // Load current stat data
        PlayerStatData stats = store.ensureAndGetComponent(ref,
                StatsPlugin.getInstance().getPlayerStatDataType());

        // Set initial stat values in UI
        snapshotCommitted(stats);
        java.util.Arrays.fill(pending, 0);
        updateStatDisplay(uiCommandBuilder, stats);

        // Register button event bindings for each stat +/- button
        String[] statNames = {"STR", "DEX", "VIT", "INT", "PRE", "ARC"};
        for (String stat : statNames) {
            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                    "#" + stat + "Plus", EventData.of("Action", stat + "_PLUS"), false);
            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                    "#" + stat + "Minus", EventData.of("Action", stat + "_MINUS"), false);
        }

        // Back button
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnBack", EventData.of("Action", "BACK"), false);

        // Save button
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnSave", EventData.of("Action", "SAVE"), false);

        // Undo button
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnUndo", EventData.of("Action", "UNDO"), false);

        // Reset stats button
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnReset", EventData.of("Action", "RESET"), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull UIData data) {
        super.handleDataEvent(ref, store, data);
        LOGGER.info("[StatsUI] handleDataEvent received action: " + data.action);
        if (data.action == null) {
            return;
        }

        PlayerStatData stats = store.ensureAndGetComponent(ref,
                StatsPlugin.getInstance().getPlayerStatDataType());

        int totalPending = pending[0] + pending[1] + pending[2] + pending[3] + pending[4] + pending[5];
        boolean canSpend = stats.getAvailablePoints() - totalPending > 0;

        switch (data.action) {
            case "BACK" -> {
                Player p = store.getComponent(ref, Player.getComponentType());
                PlayerRef pr = store.getComponent(ref, PlayerRef.getComponentType());
                if (p != null && pr != null) {
                    p.getPageManager().openCustomPage(ref, store, new MainMenuPage(pr));
                }
                return;
            }
            case "STR_PLUS" -> { if (canSpend && committed[0] + pending[0] < StatConstants.MAX_STAT_POINTS) pending[0]++; }
            case "STR_MINUS" -> { if (pending[0] > 0) pending[0]--; }
            case "DEX_PLUS" -> { if (canSpend && committed[1] + pending[1] < StatConstants.MAX_STAT_POINTS) pending[1]++; }
            case "DEX_MINUS" -> { if (pending[1] > 0) pending[1]--; }
            case "VIT_PLUS" -> { if (canSpend && committed[2] + pending[2] < StatConstants.MAX_STAT_POINTS) pending[2]++; }
            case "VIT_MINUS" -> { if (pending[2] > 0) pending[2]--; }
            case "INT_PLUS" -> { if (canSpend && committed[3] + pending[3] < StatConstants.MAX_STAT_POINTS) pending[3]++; }
            case "INT_MINUS" -> { if (pending[3] > 0) pending[3]--; }
            case "PRE_PLUS" -> { if (canSpend && committed[4] + pending[4] < StatConstants.MAX_STAT_POINTS) pending[4]++; }
            case "PRE_MINUS" -> { if (pending[4] > 0) pending[4]--; }
            case "ARC_PLUS" -> { if (canSpend && committed[5] + pending[5] < StatConstants.MAX_STAT_POINTS) pending[5]++; }
            case "ARC_MINUS" -> { if (pending[5] > 0) pending[5]--; }
            case "SAVE" -> {
                if (totalPending == 0) return;

                stats.setStrength(committed[0] + pending[0]);
                stats.setDexterity(committed[1] + pending[1]);
                stats.setVitality(committed[2] + pending[2]);
                stats.setIntellect(committed[3] + pending[3]);
                stats.setPresence(committed[4] + pending[4]);
                stats.setArcana(committed[5] + pending[5]);

                store.putComponent(ref, StatsPlugin.getInstance().getPlayerStatDataType(), stats);
                StatEffectApplier.applyStats(ref, store, stats);
                QuestTracker.record(ref, store, QuestObjective.SPEND_STAT_POINTS);
                ClassEvaluator.evaluate(ref, store, stats);
                StatsPlugin.checkFirstClassUnlock(ref, store, stats);

                Player player = store.getComponent(ref, Player.getComponentType());
                if (player != null) {
                    var customHud = player.getHudManager().getCustomHud();
                    if (customHud instanceof MmorpgHud hud) hud.forceRefresh(ref, store);
                    else if (customHud instanceof CombatHud hud) hud.forceRefresh(ref, store);
                }

                PlayerRef pr = store.getComponent(ref, PlayerRef.getComponentType());
                if (pr != null) pr.sendMessage(Message.raw("Stat points saved! (" + totalPending + " points allocated)"));

                snapshotCommitted(stats);
                java.util.Arrays.fill(pending, 0);

                UICommandBuilder cmd = new UICommandBuilder();
                updateStatDisplay(cmd, stats);
                this.sendUpdate(cmd, new UIEventBuilder(), false);
                return;
            }
            case "UNDO" -> {
                java.util.Arrays.fill(pending, 0);

                UICommandBuilder cmd = new UICommandBuilder();
                updateStatDisplayWithPending(cmd, stats);
                this.sendUpdate(cmd, new UIEventBuilder(), false);
                return;
            }
            case "RESET" -> {
                if (awaitingConfirm) {
                    // Second press = confirm
                    var goldType = StatsPlugin.getInstance().getPlayerGoldDataType();
                    PlayerGoldData goldData = store.ensureAndGetComponent(ref, goldType);

                    int refunded = stats.getSpentPoints();
                    if (refunded == 0) {
                        awaitingConfirm = false;
                        UICommandBuilder cmd = new UICommandBuilder();
                        cmd.set("#ResetStatus.TextSpans", Message.raw("No points to reset!"));
                        showResetButton(cmd);
                        UIEventBuilder evt = new UIEventBuilder();
                        evt.addEventBinding(CustomUIEventBindingType.Activating,
                                "#BtnReset", EventData.of("Action", "RESET"), false);
                        this.sendUpdate(cmd, evt, false);
                        return;
                    }

                    long resetCost = (long) refunded * StatConstants.RESET_COST_PER_POINT;
                    if (!goldData.canAfford(resetCost)) {
                        PlayerRef pr = store.getComponent(ref, PlayerRef.getComponentType());
                        if (pr != null) pr.sendMessage(Message.raw("Not enough gold! Need " + resetCost + "g."));
                        awaitingConfirm = false;
                        UICommandBuilder cmd = new UICommandBuilder();
                        cmd.set("#ResetStatus.TextSpans", Message.raw("Not enough gold!"));
                        showResetButton(cmd);
                        UIEventBuilder evt = new UIEventBuilder();
                        evt.addEventBinding(CustomUIEventBindingType.Activating,
                                "#BtnReset", EventData.of("Action", "RESET"), false);
                        this.sendUpdate(cmd, evt, false);
                        return;
                    }

                    goldData.spend(resetCost);
                    stats.setStrength(0);
                    stats.setDexterity(0);
                    stats.setVitality(0);
                    stats.setIntellect(0);
                    stats.setPresence(0);
                    stats.setArcana(0);

                    store.putComponent(ref, StatsPlugin.getInstance().getPlayerStatDataType(), stats);
                    store.putComponent(ref, goldType, goldData);
                    StatEffectApplier.applyStats(ref, store, stats);
                    ClassEvaluator.evaluate(ref, store, stats);

                    PlayerRef pr = store.getComponent(ref, PlayerRef.getComponentType());
                    if (pr != null) {
                        GoldLedger.log(pr.getUsername(), "SHOP_BUY",
                                -resetCost, goldData.getGold(), "stat reset (UI)");
                        pr.sendMessage(Message.raw("Stats reset! " + refunded + " points refunded."));
                    }

                    awaitingConfirm = false;
                    UICommandBuilder cmd = new UICommandBuilder();
                    updateStatDisplay(cmd, stats);
                    snapshotCommitted(stats);
                    java.util.Arrays.fill(pending, 0);
                    cmd.set("#ResetStatus.TextSpans", Message.raw("Reset complete!"));
                    showResetButton(cmd);
                    UIEventBuilder evt = new UIEventBuilder();
                    evt.addEventBinding(CustomUIEventBindingType.Activating,
                            "#BtnReset", EventData.of("Action", "RESET"), false);
                    this.sendUpdate(cmd, evt, false);
                    return;
                } else {
                    // First press = show confirm prompt
                    awaitingConfirm = true;
                    UICommandBuilder cmd = new UICommandBuilder();
                    long previewCost = (long) stats.getSpentPoints() * StatConstants.RESET_COST_PER_POINT;
                    cmd.set("#ResetStatus.TextSpans", Message.raw("Press again to confirm (" + previewCost + "g)"));
                    hideResetButton(cmd);
                    UIEventBuilder evt = new UIEventBuilder();
                    evt.addEventBinding(CustomUIEventBindingType.Activating,
                            "#BtnReset", EventData.of("Action", "RESET"), false);
                    this.sendUpdate(cmd, evt, false);
                    return;
                }
            }
            default -> { return; }
        }

        // For +/- actions: just update the preview display (no persist)
        UICommandBuilder commandBuilder = new UICommandBuilder();
        updateStatDisplayWithPending(commandBuilder, stats);
        this.sendUpdate(commandBuilder, new UIEventBuilder(), false);
    }

    private void snapshotCommitted(PlayerStatData stats) {
        committed[0] = stats.getStrength();
        committed[1] = stats.getDexterity();
        committed[2] = stats.getVitality();
        committed[3] = stats.getIntellect();
        committed[4] = stats.getPresence();
        committed[5] = stats.getArcana();
    }

    private void updateStatDisplayWithPending(UICommandBuilder cmd, PlayerStatData stats) {
        int totalPending = pending[0] + pending[1] + pending[2] + pending[3] + pending[4] + pending[5];
        int available = stats.getAvailablePoints() - totalPending;

        cmd.set("#LevelValue.TextSpans", Message.raw(String.valueOf(stats.getLevel())));
        cmd.set("#PointsValue.TextSpans", Message.raw(
                available + " / " + stats.getTotalPoints()
                + (totalPending > 0 ? " (" + totalPending + " unsaved)" : "")));

        // Show committed + pending for each stat
        String[] labels = {"#STRValue", "#DEXValue", "#VITValue", "#INTValue", "#PREValue", "#ARCValue"};
        for (int i = 0; i < 6; i++) {
            int total = committed[i] + pending[i];
            String display = pending[i] > 0 ? total + " (+" + pending[i] + ")" : String.valueOf(total);
            cmd.set(labels[i] + ".TextSpans", Message.raw(display));
        }

        // Derived stats preview (using committed + pending)
        int str = committed[0] + pending[0];
        int dex = committed[1] + pending[1];
        int vit = committed[2] + pending[2];
        int intel = committed[3] + pending[3];
        int pre = committed[4] + pending[4];
        int arc = committed[5] + pending[5];

        cmd.set("#MaxHPValue.TextSpans", Message.raw(
                String.format("%.0f", StatCalculation.calculateMaxHp(vit))));
        cmd.set("#MaxStaminaValue.TextSpans", Message.raw(
                String.format("%.0f", StatCalculation.calculateMaxStamina(dex))));
        cmd.set("#MaxManaValue.TextSpans", Message.raw(
                String.format("%.0f", StatCalculation.calculateMaxMana(arc))));
        cmd.set("#MaxOxygenValue.TextSpans", Message.raw(
                String.format("%.0f", StatCalculation.calculateMaxOxygen(vit))));
        cmd.set("#ManaRegenValue.TextSpans", Message.raw(
                String.format("%.1f/s", StatCalculation.calculateManaRegenRate(intel))));
        cmd.set("#MeleeDmgValue.TextSpans", Message.raw(
                String.format("%.0f%%", StatCalculation.calculateMeleeDamageMultiplier(str) * 100)));
        cmd.set("#MissValue.TextSpans", Message.raw(
                String.format("%.1f%%", StatCalculation.calculateMissChance(pre) * 100)));
        cmd.set("#ResistValue.TextSpans", Message.raw(
                String.format("%.1f", StatCalculation.calculateDamageResistance(vit))));
        cmd.set("#CritChanceValue.TextSpans", Message.raw(
                String.format("%.1f%%", StatCalculation.calculateCritChance(dex) * 100)));
        cmd.set("#CritDmgValue.TextSpans", Message.raw(
                String.format("%.0f%%", StatCalculation.calculateCritMultiplier(str) * 100)));
        cmd.set("#SpellDmgValue.TextSpans", Message.raw(
                String.format("%.0f%%", StatCalculation.calculateSpellDamageMultiplier(intel) * 100)));
        cmd.set("#RangedDmgValue.TextSpans", Message.raw(
                String.format("%.0f%%", StatCalculation.calculateLightWeaponDamageMultiplier(dex) * 100)));
        cmd.set("#FallDRValue.TextSpans", Message.raw(
                String.format("%.0f%%", StatCalculation.calculateFallDamageReduction(dex) * 100)));
        cmd.set("#HealingValue.TextSpans", Message.raw(
                String.format("%.0f%%", StatCalculation.calculateHealingMultiplier(arc) * 100)));
        cmd.set("#DiscountValue.TextSpans", Message.raw(
                String.format("%.1f%%", StatCalculation.calculateVendorDiscount(pre) * 100)));
    }

    private void updateStatDisplay(UICommandBuilder cmd, PlayerStatData stats) {
        // Level and point budget
        cmd.set("#LevelValue.TextSpans", Message.raw(String.valueOf(stats.getLevel())));
        cmd.set("#PointsValue.TextSpans", Message.raw(
                stats.getAvailablePoints() + " / " + stats.getTotalPoints()));

        // Base stat values
        cmd.set("#STRValue.TextSpans", Message.raw(String.valueOf(stats.getStrength())));
        cmd.set("#DEXValue.TextSpans", Message.raw(String.valueOf(stats.getDexterity())));
        cmd.set("#VITValue.TextSpans", Message.raw(String.valueOf(stats.getVitality())));
        cmd.set("#INTValue.TextSpans", Message.raw(String.valueOf(stats.getIntellect())));
        cmd.set("#PREValue.TextSpans", Message.raw(String.valueOf(stats.getPresence())));
        cmd.set("#ARCValue.TextSpans", Message.raw(String.valueOf(stats.getArcana())));

        // Derived stat values
        cmd.set("#MaxHPValue.TextSpans", Message.raw(
                String.format("%.0f", StatCalculation.calculateMaxHp(stats.getVitality()))));
        cmd.set("#MaxStaminaValue.TextSpans", Message.raw(
                String.format("%.0f", StatCalculation.calculateMaxStamina(stats.getDexterity()))));
        cmd.set("#MaxManaValue.TextSpans", Message.raw(
                String.format("%.0f", StatCalculation.calculateMaxMana(stats.getArcana()))));
        cmd.set("#MaxOxygenValue.TextSpans", Message.raw(
                String.format("%.0f", StatCalculation.calculateMaxOxygen(stats.getVitality()))));
        cmd.set("#ManaRegenValue.TextSpans", Message.raw(
                String.format("%.1f/s", StatCalculation.calculateManaRegenRate(stats.getIntellect()))));
        cmd.set("#MeleeDmgValue.TextSpans", Message.raw(
                String.format("%.0f%%", StatCalculation.calculateMeleeDamageMultiplier(stats.getStrength()) * 100)));
        cmd.set("#MissValue.TextSpans", Message.raw(
                String.format("%.1f%%", StatCalculation.calculateMissChance(stats.getPresence()) * 100)));
        cmd.set("#ResistValue.TextSpans", Message.raw(
                String.format("%.1f", StatCalculation.calculateDamageResistance(stats.getVitality()))));
        cmd.set("#CritChanceValue.TextSpans", Message.raw(
                String.format("%.1f%%", StatCalculation.calculateCritChance(stats.getDexterity()) * 100)));
        cmd.set("#CritDmgValue.TextSpans", Message.raw(
                String.format("%.0f%%", StatCalculation.calculateCritMultiplier(stats.getStrength()) * 100)));
        cmd.set("#SpellDmgValue.TextSpans", Message.raw(
                String.format("%.0f%%", StatCalculation.calculateSpellDamageMultiplier(stats.getIntellect()) * 100)));
        cmd.set("#RangedDmgValue.TextSpans", Message.raw(
                String.format("%.0f%%", StatCalculation.calculateLightWeaponDamageMultiplier(stats.getDexterity()) * 100)));
        cmd.set("#FallDRValue.TextSpans", Message.raw(
                String.format("%.0f%%", StatCalculation.calculateFallDamageReduction(stats.getDexterity()) * 100)));
        cmd.set("#HealingValue.TextSpans", Message.raw(
                String.format("%.0f%%", StatCalculation.calculateHealingMultiplier(stats.getArcana()) * 100)));
        cmd.set("#DiscountValue.TextSpans", Message.raw(
                String.format("%.1f%%", StatCalculation.calculateVendorDiscount(stats.getPresence()) * 100)));
    }

    private void showResetButton(UICommandBuilder cmd) {
        cmd.remove("#BtnReset");
        cmd.appendInline("#ResetRow",
                "TextButton #BtnReset { Anchor: (Width: 200, Height: 34); Text: \"Reset Stats\"; "
                + "Background: #5A2A2A(0.9); OutlineColor: #7A4A4A; OutlineSize: 1; "
                + "Style: (Default: (LabelStyle: (HorizontalAlignment: Center, VerticalAlignment: Center))); }");
    }

    private void hideResetButton(UICommandBuilder cmd) {
        cmd.remove("#BtnReset");
        cmd.appendInline("#ResetRow",
                "TextButton #BtnReset { Anchor: (Width: 200, Height: 34); Text: \"CONFIRM RESET\"; "
                + "Background: #8A2A2A(0.9); OutlineColor: #AA4A4A; OutlineSize: 1; "
                + "Style: (Default: (LabelStyle: (HorizontalAlignment: Center, VerticalAlignment: Center))); }");
    }

    /**
     * Data class for receiving UI button events.
     */
    public static class UIData {
        public static final BuilderCodec<UIData> CODEC =
                BuilderCodec.<UIData>builder(UIData.class, UIData::new)
                        .addField(new KeyedCodec<>("Action", Codec.STRING),
                                (d, v) -> d.action = v, d -> d.action)
                        .build();

        private String action;
    }
}
