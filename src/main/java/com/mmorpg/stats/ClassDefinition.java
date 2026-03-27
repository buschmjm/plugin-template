package com.mmorpg.stats;

import java.util.List;

/**
 * Defines a complete class with its 5 tiers and primary stats.
 */
public class ClassDefinition {

    private final String id;
    private final String name;
    private final String category;
    private final String[] primaryStats;
    private final boolean dualStat;
    private final List<ClassTier> tiers;

    public ClassDefinition(String id, String name, String category,
                           String[] primaryStats, boolean dualStat,
                           List<ClassTier> tiers) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.primaryStats = primaryStats;
        this.dualStat = dualStat;
        this.tiers = List.copyOf(tiers);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String[] getPrimaryStats() { return primaryStats; }
    public boolean isDualStat() { return dualStat; }
    public List<ClassTier> getTiers() { return tiers; }

    /** Get the tier definition for a specific tier number (1-5), or null. */
    public ClassTier getTier(int tierNum) {
        for (ClassTier t : tiers) {
            if (t.tier() == tierNum) return t;
        }
        return null;
    }
}
