package com.mmorpg.stats;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.*;
import java.util.logging.Logger;

/**
 * Evaluates a player's stat distribution against all class definitions
 * and grants/revokes titles accordingly. Also handles auto-select logic.
 */
public final class ClassEvaluator {

    private static final Logger LOGGER = Logger.getLogger("MMORPGStats");

    private ClassEvaluator() {}

    /**
     * Evaluate all class titles for a player based on their current stats.
     * Grants any newly qualified titles and recomputes cached passives.
     * Should be called after stat allocation, level-up, or respec.
     */
    public static void evaluate(Ref<EntityStore> ref, Store<EntityStore> store,
                                PlayerStatData stats) {
        ComponentType<EntityStore, PlayerClassData> classType =
                StatsPlugin.getInstance().getPlayerClassDataType();
        PlayerClassData classData = store.getComponent(ref, classType);
        if (classData == null) return;

        // No class titles before minimum level
        if (stats.getLevel() < StatConstants.CLASS_TITLE_MIN_LEVEL) return;

        List<String> newlyEarned = new ArrayList<>();

        for (ClassDefinition cls : ClassRegistry.getAll()) {
            for (ClassTier tier : cls.getTiers()) {
                String titleId = cls.getId() + "_" + tier.tier();
                if (classData.hasTitle(titleId)) continue;

                boolean qualifies;
                if (cls.isDualStat()) {
                    int stat1 = getStatValue(stats, cls.getPrimaryStats()[0]);
                    int stat2 = getStatValue(stats, cls.getPrimaryStats()[1]);
                    qualifies = stat1 >= tier.threshold() && stat2 >= tier.threshold();
                } else {
                    int stat = getStatValue(stats, cls.getPrimaryStats()[0]);
                    qualifies = stat >= tier.threshold();
                }

                if (qualifies) {
                    classData.addTitle(titleId);
                    newlyEarned.add(titleId);
                    LOGGER.info("[ClassEvaluator] Earned title: " + tier.titleName()
                            + " (" + titleId + ")");
                }
            }
        }

        if (!newlyEarned.isEmpty()) {
            classData.recomputePassives();

            // Apply updated stat bonuses
            StatEffectApplier.applyClassBonuses(ref, store, classData, stats);

            // Persist class data changes
            store.putComponent(ref, classType, classData);

            // Auto-update displayed title
            if (classData.isAutoUpdate()) {
                autoSelectTitle(classData);
            }

            // Notify player of new titles via chat
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef != null) {
                for (String titleId : newlyEarned) {
                    String displayName = getTitleDisplayName(titleId);
                    playerRef.sendMessage(
                            Message.raw("* Title earned: " + displayName
                                    + "! Use /class select " + titleId + " to equip it."));
                }
            }
        }
    }

    /**
     * Re-evaluate titles from scratch (used after respec).
     * Clears all titles first, then re-evaluates.
     */
    public static void reevaluateAfterRespec(Ref<EntityStore> ref, Store<EntityStore> store,
                                             PlayerStatData stats) {
        ComponentType<EntityStore, PlayerClassData> classType =
                StatsPlugin.getInstance().getPlayerClassDataType();
        PlayerClassData classData = store.getComponent(ref, classType);
        if (classData == null) return;

        // Remove all class stat modifiers
        StatEffectApplier.removeClassBonuses(ref, store);

        // Clear and re-evaluate
        classData.clearTitles();
        store.putComponent(ref, classType, classData);
        evaluate(ref, store, stats);
    }

    /**
     * Auto-select the best title: highest total point cost, then alphabetical.
     */
    static void autoSelectTitle(PlayerClassData classData) {
        String bestTitleId = null;
        int bestCost = -1;
        String bestName = null;

        for (String titleId : classData.getEarnedTitles()) {
            int sep = titleId.lastIndexOf('_');
            if (sep < 0) continue;
            String classId = titleId.substring(0, sep);
            int tier;
            try { tier = Integer.parseInt(titleId.substring(sep + 1)); }
            catch (NumberFormatException e) { continue; }

            ClassDefinition cls = ClassRegistry.get(classId);
            if (cls == null) continue;
            ClassTier t = cls.getTier(tier);
            if (t == null) continue;

            int cost = t.totalCost(cls.isDualStat());
            String name = t.titleName();

            if (cost > bestCost || (cost == bestCost && (bestName == null || name.compareTo(bestName) < 0))) {
                bestCost = cost;
                bestName = name;
                bestTitleId = titleId;
            }
        }

        if (bestTitleId != null) {
            classData.setDisplayedTitle(bestTitleId);
        }
    }

    /** Resolve a title ID like "knight_3" to its display name. */
    public static String getTitleDisplayName(String titleId) {
        if ("Apprentice".equals(titleId)) return "Apprentice";
        int sep = titleId.lastIndexOf('_');
        if (sep < 0) return titleId;
        String classId = titleId.substring(0, sep);
        int tier;
        try { tier = Integer.parseInt(titleId.substring(sep + 1)); }
        catch (NumberFormatException e) { return titleId; }
        ClassDefinition cls = ClassRegistry.get(classId);
        if (cls == null) return titleId;
        ClassTier t = cls.getTier(tier);
        return t != null ? t.titleName() : titleId;
    }

    /** Map stat abbreviation to the player's current value. */
    static int getStatValue(PlayerStatData stats, String statName) {
        return switch (statName) {
            case "STR" -> stats.getStrength();
            case "DEX" -> stats.getDexterity();
            case "VIT" -> stats.getVitality();
            case "INT" -> stats.getIntellect();
            case "PRE" -> stats.getPresence();
            case "ARC" -> stats.getArcana();
            default -> 0;
        };
    }
}
