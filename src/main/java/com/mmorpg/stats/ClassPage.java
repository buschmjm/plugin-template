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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class title browser with two views:
 *   LIST  - paginated grid of all 21 classes showing earned tier counts.
 *   TREE  - 5-tier progression ladder for a single selected class with
 *            passive descriptions and equip buttons.
 */
public class ClassPage extends InteractiveCustomUIPage<ClassPage.ClassAction> {

    private enum Mode { LIST, TREE }

    private static final int PER_PAGE = 7;

    private Mode mode = Mode.LIST;
    private int classPage = 0;
    private String selectedClassId = null;

    // Ordered list of all class IDs, populated once at construction time.
    private final List<String> allClassIds = new ArrayList<>();

    public ClassPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, ClassAction.CODEC);
        for (ClassDefinition cls : ClassRegistry.getAll()) {
            allClassIds.add(cls.getId());
        }
    }

    // -- Build (initial render) ---------------------------------------------

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("ClassPage.ui");

        PlayerClassData classData = store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerClassDataType());

        populateView(cmd, classData);

        // Register all possible actions once - mode switching is handled in Java.
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnBack", EventData.of("Action", "BACK"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnPrev", EventData.of("Action", "PREV"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnNext", EventData.of("Action", "NEXT"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnAuto", EventData.of("Action", "AUTO"), false);
        for (int i = 1; i <= PER_PAGE; i++) {
            events.addEventBinding(CustomUIEventBindingType.Activating,
                    "#RowBtn" + i, EventData.of("Action", "ROW_" + i), false);
        }
    }

    // -- Event handling -----------------------------------------------------

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store, @Nonnull ClassAction data) {
        super.handleDataEvent(ref, store, data);
        if (data.action == null) return;

        PlayerClassData classData = store.getComponent(ref,
                StatsPlugin.getInstance().getPlayerClassDataType());

        switch (data.action) {

            case "BACK" -> {
                if (mode == Mode.TREE) {
                    // Return to class list, not main menu.
                    mode = Mode.LIST;
                    UICommandBuilder cmd = new UICommandBuilder();
                    populateView(cmd, classData);
                    this.sendUpdate(cmd, new UIEventBuilder(), false);
                } else {
                    Player player = store.getComponent(ref, Player.getComponentType());
                    PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());
                    if (player != null && pRef != null) {
                        player.getPageManager().openCustomPage(ref, store, new MainMenuPage(pRef));
                    }
                }
            }

            case "PREV" -> {
                if (mode == Mode.LIST && classPage > 0) {
                    classPage--;
                    UICommandBuilder cmd = new UICommandBuilder();
                    populateView(cmd, classData);
                    this.sendUpdate(cmd, new UIEventBuilder(), false);
                }
            }

            case "NEXT" -> {
                if (mode == Mode.LIST) {
                    int totalPages = totalPages();
                    if (classPage < totalPages - 1) {
                        classPage++;
                        UICommandBuilder cmd = new UICommandBuilder();
                        populateView(cmd, classData);
                        this.sendUpdate(cmd, new UIEventBuilder(), false);
                    }
                }
            }

            case "AUTO" -> {
                if (classData == null) return;
                boolean nowAuto = !classData.isAutoUpdate();
                classData.setAutoUpdate(nowAuto);
                if (nowAuto) {
                    ClassEvaluator.autoSelectTitle(classData);
                    classData.recomputePassives();
                    PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());
                    if (pRef != null) StatsPlugin.getInstance().updatePlayerNametag(ref, store, pRef);
                }
                store.putComponent(ref, StatsPlugin.getInstance().getPlayerClassDataType(), classData);
                UICommandBuilder cmd = new UICommandBuilder();
                populateView(cmd, classData);
                this.sendUpdate(cmd, new UIEventBuilder(), false);
            }

            default -> {
                if (!data.action.startsWith("ROW_")) return;
                int row;
                try { row = Integer.parseInt(data.action.substring(4)); }
                catch (NumberFormatException e) { return; }

                if (mode == Mode.LIST) {
                    int idx = classPage * PER_PAGE + (row - 1);
                    if (idx < allClassIds.size()) {
                        selectedClassId = allClassIds.get(idx);
                        mode = Mode.TREE;
                        UICommandBuilder cmd = new UICommandBuilder();
                        populateView(cmd, classData);
                        this.sendUpdate(cmd, new UIEventBuilder(), false);
                    }
                } else {
                    // TREE mode: equip the tier at this row (rows 1-5 = tiers 1-5).
                    if (row < 1 || row > 5 || classData == null || selectedClassId == null) return;
                    String titleId = selectedClassId + "_" + row;
                    if (!classData.hasTitle(titleId)) return;

                    classData.setDisplayedTitle(titleId);
                    classData.setAutoUpdate(false);
                    classData.recomputePassives();
                    store.putComponent(ref, StatsPlugin.getInstance().getPlayerClassDataType(), classData);

                    PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());
                    if (pRef != null) {
                        StatsPlugin.getInstance().updatePlayerNametag(ref, store, pRef);
                        pRef.sendMessage(Message.raw("Title set to: "
                                + ClassEvaluator.getTitleDisplayName(titleId)));
                    }

                    UICommandBuilder cmd = new UICommandBuilder();
                    populateView(cmd, classData);
                    this.sendUpdate(cmd, new UIEventBuilder(), false);
                }
            }
        }
    }

    // -- View population helpers --------------------------------------------

    private void populateView(UICommandBuilder cmd, PlayerClassData classData) {
        String activeName = classData != null ? classData.getDisplayedTitleName() : "Apprentice";
        cmd.set("#InfoLine1.TextSpans", Message.raw("Active Title: " + activeName));
        cmd.set("#BtnAuto.TextSpans", Message.raw(
                "Auto: " + (classData != null && classData.isAutoUpdate() ? "ON" : "OFF")));

        if (mode == Mode.LIST) {
            populateListView(cmd, classData);
        } else {
            populateTreeView(cmd, classData);
        }
    }

    private void populateListView(UICommandBuilder cmd, PlayerClassData classData) {
        int total = totalPages();
        cmd.set("#PageTitle.TextSpans", Message.raw("Class Selection"));
        cmd.set("#InfoLine2.TextSpans", Message.raw(
                "Click a class to view its progression tree"));
        cmd.set("#NavLabel.TextSpans", Message.raw("Page " + (classPage + 1) + " / " + total));
        cmd.set("#BtnPrev.TextSpans", Message.raw("< Prev"));
        cmd.set("#BtnNext.TextSpans", Message.raw("Next >"));

        int start = classPage * PER_PAGE;
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = start + i;
            int row = i + 1;
            if (idx < allClassIds.size()) {
                String classId = allClassIds.get(idx);
                ClassDefinition cls = ClassRegistry.get(classId);
                int earned = countEarnedTiers(classData, classId);
                String statStr = String.join("/", cls.getPrimaryStats());
                String earnedStr = earned == 5 ? "[5/5 *]" : (earned > 0 ? "[" + earned + "/5]" : "[0/5]");
                cmd.set("#RowLabel" + row + ".TextSpans", Message.raw(
                        cls.getName() + "  (" + statStr + ")" + "  " + earnedStr
                        + "  |  " + cls.getCategory()));
                cmd.set("#RowBtn" + row + ".TextSpans", Message.raw("View"));
            } else {
                cmd.set("#RowLabel" + row + ".TextSpans", Message.raw(""));
                cmd.set("#RowBtn" + row + ".TextSpans", Message.raw(""));
            }
        }

        // Clear passive section in list mode
        cmd.set("#PassiveHeader.TextSpans", Message.raw(""));
        cmd.set("#PassiveLine1.TextSpans", Message.raw(""));
        cmd.set("#PassiveLine2.TextSpans", Message.raw(""));
        cmd.set("#PassiveLine3.TextSpans", Message.raw(""));
    }

    private void populateTreeView(UICommandBuilder cmd, PlayerClassData classData) {
        ClassDefinition cls = ClassRegistry.get(selectedClassId);
        if (cls == null) {
            mode = Mode.LIST;
            populateListView(cmd, classData);
            return;
        }

        String statStr = String.join("/", cls.getPrimaryStats());
        cmd.set("#PageTitle.TextSpans", Message.raw(cls.getName() + "  (" + statStr + ")"));
        cmd.set("#InfoLine2.TextSpans", Message.raw(
                cls.getCategory() + (cls.isDualStat() ? " - Dual-stat" : " - Single-stat")
                + "  |  Back goes to class list"));
        cmd.set("#NavLabel.TextSpans", Message.raw("Tier Progression"));
        cmd.set("#BtnPrev.TextSpans", Message.raw(""));
        cmd.set("#BtnNext.TextSpans", Message.raw(""));

        String activeTitle = classData != null ? classData.getDisplayedTitle() : "Apprentice";

        for (int tier = 1; tier <= 5; tier++) {
            ClassTier t = cls.getTier(tier);
            if (t == null) continue;
            String titleId = selectedClassId + "_" + tier;
            boolean earned = classData != null && classData.hasTitle(titleId);
            boolean active = titleId.equals(activeTitle);

            String threshStr = cls.isDualStat()
                    ? t.threshold() + " " + statStr + " each"
                    : t.threshold() + " " + statStr;

            String prefix = active ? "* " : (earned ? "[done] " : "[lock] ");
            String rowText = prefix + "T" + tier + ": " + t.titleName()
                    + "  (" + threshStr + ")  |  " + t.passiveDesc();
            String btnText = active ? "Active" : (earned ? "Equip" : "Locked");

            cmd.set("#RowLabel" + tier + ".TextSpans", Message.raw(rowText));
            cmd.set("#RowBtn" + tier + ".TextSpans", Message.raw(btnText));
        }

        // Clear unused rows 6 and 7.
        for (int i = 6; i <= PER_PAGE; i++) {
            cmd.set("#RowLabel" + i + ".TextSpans", Message.raw(""));
            cmd.set("#RowBtn" + i + ".TextSpans", Message.raw(""));
        }

        // Populate cumulative passive bonuses for this class
        int earnedCount = countEarnedTiers(classData, selectedClassId);
        if (earnedCount > 0) {
            cmd.set("#PassiveHeader.TextSpans", Message.raw(
                    "-- " + cls.getName() + " Passive Bonuses (" + earnedCount + " tier"
                    + (earnedCount > 1 ? "s" : "") + " earned) --"));
            List<String> lines = computeCumulativePassiveLines(cls, earnedCount);
            cmd.set("#PassiveLine1.TextSpans", Message.raw(lines.size() > 0 ? lines.get(0) : ""));
            cmd.set("#PassiveLine2.TextSpans", Message.raw(lines.size() > 1 ? lines.get(1) : ""));
            cmd.set("#PassiveLine3.TextSpans", Message.raw(lines.size() > 2 ? lines.get(2) : ""));
        } else {
            cmd.set("#PassiveHeader.TextSpans", Message.raw(
                    "-- No tiers earned yet --"));
            String statReq = cls.isDualStat()
                    ? "Allocate " + String.join(" and ", cls.getPrimaryStats()) + " to unlock"
                    : "Allocate " + cls.getPrimaryStats()[0] + " to unlock";
            cmd.set("#PassiveLine1.TextSpans", Message.raw(statReq));
            cmd.set("#PassiveLine2.TextSpans", Message.raw(""));
            cmd.set("#PassiveLine3.TextSpans", Message.raw(""));
        }
    }

    // -- Passive display helpers -------------------------------------------

    /** Compute cumulative passive effects for tiers 1..upToTier and split into display lines. */
    private List<String> computeCumulativePassiveLines(ClassDefinition cls, int upToTier) {
        Map<PassiveType, Float> totals = new LinkedHashMap<>();
        for (ClassTier t : cls.getTiers()) {
            if (t.tier() > upToTier) break;
            for (PassiveEffect pe : t.passives()) {
                totals.merge(pe.type(), pe.value(), Float::sum);
            }
        }
        if (totals.isEmpty()) return List.of("No mechanical bonuses");

        List<String> parts = new ArrayList<>();
        for (var entry : totals.entrySet()) {
            parts.add(formatPassiveType(entry.getKey(), entry.getValue()));
        }

        // Split into lines of ~3 effects each
        List<String> lines = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String part : parts) {
            if (count > 0) sb.append("  |  ");
            sb.append(part);
            count++;
            if (count >= 3) {
                lines.add(sb.toString());
                sb.setLength(0);
                count = 0;
            }
        }
        if (sb.length() > 0) lines.add(sb.toString());
        return lines;
    }

    private static String formatPassiveType(PassiveType type, float value) {
        String pct = String.format("%.0f%%", value * 100);
        return switch (type) {
            case MELEE_DAMAGE_PERCENT -> "Melee Dmg +" + pct;
            case SPELL_DAMAGE_PERCENT -> "Spell Dmg +" + pct;
            case MISS_CHANCE_PERCENT -> "Miss Chance +" + pct;
            case DAMAGE_REDUCTION_PERCENT -> "Dmg Resist +" + pct;
            case DAMAGE_NEGATE_CHANCE -> "Negate Chance " + pct;
            case LOW_HP_RESIST_PERCENT -> "Low HP Resist +" + pct;
            case MAX_HP_PERCENT -> "Max HP +" + pct;
            case MANA_POOL_PERCENT -> "Mana Pool +" + pct;
            case STAMINA_POOL_PERCENT -> "Stamina +" + pct;
            case HEALING_OUTPUT_PERCENT -> "Healing +" + pct;
            case CRIT_MULTIPLIER_PERCENT -> "Crit Dmg +" + pct;
            case HP_REGEN_PERCENT -> "HP Regen +" + pct;
            case MANA_REGEN_PERCENT -> "Mana Regen +" + pct;
            case FALL_DAMAGE_REDUCTION -> "Fall Dmg Resist +" + pct;
            case SHOP_DISCOUNT_PERCENT -> "Shop Discount " + pct;
        };
    }

    // -- Utilities ----------------------------------------------------------

    private int totalPages() {
        return (allClassIds.size() + PER_PAGE - 1) / PER_PAGE;
    }

    private static int countEarnedTiers(PlayerClassData classData, String classId) {
        if (classData == null) return 0;
        int count = 0;
        for (int t = 1; t <= 5; t++) {
            if (classData.hasTitle(classId + "_" + t)) count++;
        }
        return count;
    }

    // -- Action codec -------------------------------------------------------

    public static class ClassAction {
        public static final BuilderCodec<ClassAction> CODEC =
                BuilderCodec.<ClassAction>builder(ClassAction.class, ClassAction::new)
                        .addField(new KeyedCodec<>("Action", Codec.STRING),
                                (d, v) -> d.action = v, d -> d.action)
                        .build();
        private String action;
    }
}
