package com.mmorpg.stats;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Singleton registry holding all ability definitions.
 * Abilities are registered during plugin setup and looked up by ID.
 */
public final class AbilityRegistry {

    private static final Map<String, AbilityDefinition> abilities = new LinkedHashMap<>();

    private AbilityRegistry() {}

    public static void register(AbilityDefinition ability) {
        abilities.put(ability.getId(), ability);
    }

    public static AbilityDefinition get(String id) {
        return abilities.get(id);
    }

    public static Collection<AbilityDefinition> getAll() {
        return Collections.unmodifiableCollection(abilities.values());
    }

    public static boolean exists(String id) {
        return abilities.containsKey(id);
    }

    public static void clear() {
        abilities.clear();
    }
}
