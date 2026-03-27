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
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class AbilitiesPage extends InteractiveCustomUIPage<AbilitiesPage.AbilityAction> {

    private static final int PAGE_SIZE = 4;
    private static final String[] STAT_PAGES = {"STR", "DEX", "VIT", "INT", "PRE", "ARC"};
    private static final String[] STAT_LABELS = {
            "Strength", "Dexterity", "Vitality", "Intellect", "Presence", "Arcana"
    };
    private int selectedIndex = -1;  // -1 = nothing selected
    private int page = 0;
    private final List<AbilityDefinition> pageAbilities = new ArrayList<>();

    public AbilitiesPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, AbilityAction.CODEC);
    }

    private void refreshPageAbilities() {
        pageAbilities.clear();
        String stat = STAT_PAGES[page];
        for (AbilityDefinition def : AbilityRegistry.getAll()) {
            if (stat.equals(def.getRequiredStat())) {
                pageAbilities.add(def);
            }
        }
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("AbilitiesPage.ui");

        PlayerAbilityData abilityData = TransientDataStore.getAbilities(ref);
        refreshPageAbilities();

        var statType = StatsPlugin.getInstance().getPlayerStatDataType();
        PlayerStatData stats = store.getComponent(ref, statType);

        updateSlotButtons(cmd, abilityData);
        populateResources(cmd, ref, store);
        populateAbilityList(cmd, stats);
        updatePageLabel(cmd);
        updateAbilityHeader(cmd);

        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#SlotBtn1", EventData.of("Action", "SLOT1"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#SlotBtn2", EventData.of("Action", "SLOT2"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#SlotBtn3", EventData.of("Action", "SLOT3"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnBack", EventData.of("Action", "BACK"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnPagePrev", EventData.of("Action", "PAGE_PREV"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnPageNext", EventData.of("Action", "PAGE_NEXT"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#AbilityBtn1", EventData.of("Action", "ABILITY1"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#AbilityBtn2", EventData.of("Action", "ABILITY2"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#AbilityBtn3", EventData.of("Action", "ABILITY3"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#AbilityBtn4", EventData.of("Action", "ABILITY4"), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store, @Nonnull AbilityAction data) {
        super.handleDataEvent(ref, store, data);
        if (data.action == null) return;

        var statType = StatsPlugin.getInstance().getPlayerStatDataType();
        PlayerStatData stats = store.getComponent(ref, statType);

        switch (data.action) {
            case "ABILITY1", "ABILITY2", "ABILITY3", "ABILITY4" -> {
                int idx = data.action.charAt(7) - '1';
                selectedIndex = (idx >= 0 && idx < pageAbilities.size()) ? idx : -1;
                UICommandBuilder cmd = new UICommandBuilder();
                populateAbilityList(cmd, stats);
                updateAbilityHeader(cmd);
                this.sendUpdate(cmd, new UIEventBuilder(), false);
            }
            case "PAGE_PREV" -> {
                page = (page - 1 + STAT_PAGES.length) % STAT_PAGES.length;
                refreshPageAbilities();
                selectedIndex = -1;
                UICommandBuilder cmd = new UICommandBuilder();
                populateAbilityList(cmd, stats);
                updateAbilityHeader(cmd);
                updatePageLabel(cmd);
                this.sendUpdate(cmd, new UIEventBuilder(), false);
            }
            case "PAGE_NEXT" -> {
                page = (page + 1) % STAT_PAGES.length;
                refreshPageAbilities();
                selectedIndex = -1;
                UICommandBuilder cmd = new UICommandBuilder();
                populateAbilityList(cmd, stats);
                updateAbilityHeader(cmd);
                updatePageLabel(cmd);
                this.sendUpdate(cmd, new UIEventBuilder(), false);
            }
            case "SLOT1", "SLOT2", "SLOT3" -> {
                int slot = data.action.charAt(4) - '1';
                PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());

                if (selectedIndex < 0 || selectedIndex >= pageAbilities.size()) {
                    if (pRef != null) {
                        pRef.sendMessage(Message.raw("Select an ability from the list first."));
                    }
                    return;
                }

                AbilityDefinition selectedAbility = pageAbilities.get(selectedIndex);

                if (selectedAbility.hasRequirement() && (stats == null || !selectedAbility.isUnlockedBy(stats))) {
                    if (pRef != null) {
                        pRef.sendMessage(Message.raw(selectedAbility.getName()
                                + " is locked! Requires " + selectedAbility.getRequirementText() + " points."));
                    }
                    return;
                }

                PlayerAbilityData abilityData = TransientDataStore.getAbilities(ref);
                abilityData.bindSlot(slot, selectedAbility.getId());
                StatsPlugin.persistAbilityBinds(ref, store);

                if (pRef != null) {
                    String keyName = slot == 0 ? "Q" : slot == 1 ? "E" : "R";
                    pRef.sendMessage(Message.raw("Bound " + selectedAbility.getName()
                            + " to Slot " + (slot + 1) + " [" + keyName + "]"));
                }

                UICommandBuilder cmd = new UICommandBuilder();
                updateSlotButtons(cmd, abilityData);
                this.sendUpdate(cmd, new UIEventBuilder(), false);
            }
            case "BACK" -> {
                Player player = store.getComponent(ref, Player.getComponentType());
                PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (player != null && pRef != null) {
                    player.getPageManager().openCustomPage(ref, store, new MainMenuPage(pRef));
                }
            }
        }
    }

    private void updateSlotButtons(UICommandBuilder cmd, PlayerAbilityData abilityData) {
        String[] keys = {"Q", "E", "R"};
        for (int s = 0; s < 3; s++) {
            String bound = abilityData.getSlot(s);
            String abilityName;
            if (bound != null) {
                AbilityDefinition def = AbilityRegistry.get(bound);
                abilityName = def != null ? def.getName() : bound;
            } else {
                abilityName = "(empty)";
            }
            cmd.set("#SlotBtn" + (s + 1) + ".Text",
                    "[ " + keys[s] + " ]  Slot " + (s + 1) + ": " + abilityName);
        }
    }

    private void updateAbilityHeader(UICommandBuilder cmd) {
        if (selectedIndex >= 0 && selectedIndex < pageAbilities.size()) {
            AbilityDefinition sel = pageAbilities.get(selectedIndex);
            cmd.set("#AbilityHeader.TextSpans", Message.raw(
                    "Selected: " + sel.getName() + "  \u2014  Click a slot above [Q / E / R] to bind:"));
        } else {
            cmd.set("#AbilityHeader.TextSpans", Message.raw(
                    "Select an ability, then click a slot above to bind it:"));
        }
    }

    private void populateAbilityList(UICommandBuilder cmd, PlayerStatData stats) {
        for (int i = 0; i < PAGE_SIZE; i++) {
            String text = "";
            if (i < pageAbilities.size()) {
                AbilityDefinition def = pageAbilities.get(i);
                boolean unlocked = stats != null && (!def.hasRequirement() || def.isUnlockedBy(stats));
                StringBuilder sb = new StringBuilder();
                if (i == selectedIndex) sb.append("> ");
                if (!unlocked) sb.append("[LOCKED] ");
                sb.append(def.getName());
                if (def.hasRequirement()) {
                    sb.append(" (").append(statFullName(def.getRequiredStat())).append(": ").append(def.getRequiredPoints()).append(")");
                }
                sb.append(" - ").append(def.getDescription());
                sb.append("  |  ").append((int) def.getCooldownSeconds()).append("s CD");
                if (def.getManaCost() > 0) sb.append(" | ").append((int) def.getManaCost()).append(" mana");
                text = sb.toString();
            }
            cmd.set("#AbilityBtn" + (i + 1) + ".Text", text);
        }
    }

    private void updatePageLabel(UICommandBuilder cmd) {
        cmd.set("#PageLabel.TextSpans", Message.raw(STAT_LABELS[page]));
    }

    private static String statFullName(String statCode) {
        return switch (statCode) {
            case "STR" -> "Strength";
            case "DEX" -> "Dexterity";
            case "VIT" -> "Vitality";
            case "INT" -> "Intellect";
            case "PRE" -> "Presence";
            case "ARC" -> "Arcana";
            default -> statCode;
        };
    }

    private void populateResources(UICommandBuilder cmd, Ref<EntityStore> ref, Store<EntityStore> store) {
        EntityStatMap statMap = (EntityStatMap) store.getComponent(ref,
                EntityStatMap.getComponentType());
        if (statMap == null) return;

        EntityStatValue mana = statMap.get(DefaultEntityStatTypes.getMana());
        if (mana != null) {
            cmd.set("#ManaLabel.TextSpans", Message.raw(
                    "Mana: " + (int) mana.get() + " / " + (int) mana.getMax()));
        }
        EntityStatValue stamina = statMap.get(DefaultEntityStatTypes.getStamina());
        if (stamina != null) {
            cmd.set("#StaminaLabel.TextSpans", Message.raw(
                    "Stamina: " + (int) stamina.get() + " / " + (int) stamina.getMax()));
        }
    }

    public static class AbilityAction {
        public static final BuilderCodec<AbilityAction> CODEC =
                BuilderCodec.<AbilityAction>builder(AbilityAction.class, AbilityAction::new)
                        .addField(new KeyedCodec<>("Action", Codec.STRING),
                                (d, v) -> d.action = v, d -> d.action)
                        .build();
        private String action;
    }
}
