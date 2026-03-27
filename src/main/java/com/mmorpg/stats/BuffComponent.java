package com.mmorpg.stats;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the list of active buffs/debuffs on an entity.
 * Stored in TransientDataStore (not the ECS) to reduce memory overhead.
 * Buffs are transient and lost on server restart by design.
 */
public class BuffComponent {

    private final List<BuffInstance> buffs = new ArrayList<>();

    public BuffComponent() {}

    public List<BuffInstance> getBuffs() { return buffs; }

    public void addBuff(BuffInstance buff) {
        buffs.add(buff);
    }

    public boolean removeBuff(String buffId) {
        return buffs.removeIf(b -> b.getId().equals(buffId));
    }

    public BuffInstance getBuff(String buffId) {
        for (BuffInstance b : buffs) {
            if (b.getId().equals(buffId)) return b;
        }
        return null;
    }

    public boolean hasBuff(String buffId) {
        return getBuff(buffId) != null;
    }

    public void clear() {
        buffs.clear();
    }
}
