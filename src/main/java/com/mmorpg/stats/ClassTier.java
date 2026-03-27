package com.mmorpg.stats;

import java.util.List;

/**
 * Definition of a single tier within a class.
 *
 * @param tier           Tier number (1-5)
 * @param titleName      Display name following naming convention
 * @param threshold      Points required in the primary stat(s) to qualify
 * @param passiveDesc    Human-readable passive description from design doc
 * @param passives       Mechanically implemented passive effects for this tier
 */
public record ClassTier(
        int tier,
        String titleName,
        int threshold,
        String passiveDesc,
        List<PassiveEffect> passives
) {
    /** Total point cost for this tier (single stat = threshold, dual = threshold * 2). */
    public int totalCost(boolean dualStat) {
        return dualStat ? threshold * 2 : threshold;
    }
}
