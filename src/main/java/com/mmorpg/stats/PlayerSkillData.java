package com.mmorpg.stats;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Persistent ECS component storing all profession skill levels, XP, and explored chunk data.
 * Serialised via BSON - each skill's level and XP are stored as separate int fields.
 * Explored chunks are tracked in a session-local set (not persisted) to prevent
 * unbounded memory growth. Only the total explored count is persisted.
 */
public class PlayerSkillData implements Component<EntityStore> {

    // -- Codec (BSON persistence) --

    public static final BuilderCodec<PlayerSkillData> CODEC =
            BuilderCodec.<PlayerSkillData>builder(PlayerSkillData.class, PlayerSkillData::new)
                    .addField(new KeyedCodec<>("MiningLevel", Codec.INTEGER),
                            (d, v) -> d.setLevel(SkillType.MINING, v),
                            d -> d.getLevel(SkillType.MINING))
                    .addField(new KeyedCodec<>("MiningXp", Codec.INTEGER),
                            (d, v) -> d.setXp(SkillType.MINING, v),
                            d -> d.getXp(SkillType.MINING))
                    .addField(new KeyedCodec<>("WoodcuttingLevel", Codec.INTEGER),
                            (d, v) -> d.setLevel(SkillType.WOODCUTTING, v),
                            d -> d.getLevel(SkillType.WOODCUTTING))
                    .addField(new KeyedCodec<>("WoodcuttingXp", Codec.INTEGER),
                            (d, v) -> d.setXp(SkillType.WOODCUTTING, v),
                            d -> d.getXp(SkillType.WOODCUTTING))
                    .addField(new KeyedCodec<>("FarmingLevel", Codec.INTEGER),
                            (d, v) -> d.setLevel(SkillType.FARMING, v),
                            d -> d.getLevel(SkillType.FARMING))
                    .addField(new KeyedCodec<>("FarmingXp", Codec.INTEGER),
                            (d, v) -> d.setXp(SkillType.FARMING, v),
                            d -> d.getXp(SkillType.FARMING))
                    .addField(new KeyedCodec<>("HerbalismLevel", Codec.INTEGER),
                            (d, v) -> d.setLevel(SkillType.HERBALISM, v),
                            d -> d.getLevel(SkillType.HERBALISM))
                    .addField(new KeyedCodec<>("HerbalismXp", Codec.INTEGER),
                            (d, v) -> d.setXp(SkillType.HERBALISM, v),
                            d -> d.getXp(SkillType.HERBALISM))
                    .addField(new KeyedCodec<>("CraftingLevel", Codec.INTEGER),
                            (d, v) -> d.setLevel(SkillType.CARPENTRY, v),
                            d -> d.getLevel(SkillType.CARPENTRY))
                    .addField(new KeyedCodec<>("CraftingXp", Codec.INTEGER),
                            (d, v) -> d.setXp(SkillType.CARPENTRY, v),
                            d -> d.getXp(SkillType.CARPENTRY))
                    .addField(new KeyedCodec<>("MasonryLevel", Codec.INTEGER),
                            (d, v) -> d.setLevel(SkillType.MASONRY, v),
                            d -> d.getLevel(SkillType.MASONRY))
                    .addField(new KeyedCodec<>("MasonryXp", Codec.INTEGER),
                            (d, v) -> d.setXp(SkillType.MASONRY, v),
                            d -> d.getXp(SkillType.MASONRY))
                    .addField(new KeyedCodec<>("SmithingLevel", Codec.INTEGER),
                            (d, v) -> d.setLevel(SkillType.SMITHING, v),
                            d -> d.getLevel(SkillType.SMITHING))
                    .addField(new KeyedCodec<>("SmithingXp", Codec.INTEGER),
                            (d, v) -> d.setXp(SkillType.SMITHING, v),
                            d -> d.getXp(SkillType.SMITHING))
                    .addField(new KeyedCodec<>("ProcessingLevel", Codec.INTEGER),
                            (d, v) -> d.setLevel(SkillType.PROCESSING, v),
                            d -> d.getLevel(SkillType.PROCESSING))
                    .addField(new KeyedCodec<>("ProcessingXp", Codec.INTEGER),
                            (d, v) -> d.setXp(SkillType.PROCESSING, v),
                            d -> d.getXp(SkillType.PROCESSING))
                    .addField(new KeyedCodec<>("ExplorationLevel", Codec.INTEGER),
                            (d, v) -> d.setLevel(SkillType.EXPLORATION, v),
                            d -> d.getLevel(SkillType.EXPLORATION))
                    .addField(new KeyedCodec<>("ExplorationXp", Codec.INTEGER),
                            (d, v) -> d.setXp(SkillType.EXPLORATION, v),
                            d -> d.getXp(SkillType.EXPLORATION))
                    .addField(new KeyedCodec<>("ExploredChunkCount", Codec.INTEGER),
                            (d, v) -> d.exploredChunkCount = v,
                            d -> d.exploredChunkCount)
                    .addField(new KeyedCodec<>("ExploredChunks", Codec.LONG_ARRAY),
                            (d, v) -> { if (v != null) for (long l : v) d.exploredChunks.add(l); },
                            d -> d.exploredChunks.stream().mapToLong(Long::longValue).toArray())
                    .addField(new KeyedCodec<>("CookingLevel", Codec.INTEGER),
                            (d, v) -> d.setLevel(SkillType.COOKING, v),
                            d -> d.getLevel(SkillType.COOKING))
                    .addField(new KeyedCodec<>("CookingXp", Codec.INTEGER),
                            (d, v) -> d.setXp(SkillType.COOKING, v),
                            d -> d.getXp(SkillType.COOKING))
                    .addField(new KeyedCodec<>("AlchemyLevel", Codec.INTEGER),
                            (d, v) -> d.setLevel(SkillType.ALCHEMY, v),
                            d -> d.getLevel(SkillType.ALCHEMY))
                    .addField(new KeyedCodec<>("AlchemyXp", Codec.INTEGER),
                            (d, v) -> d.setXp(SkillType.ALCHEMY, v),
                            d -> d.getXp(SkillType.ALCHEMY))
                    .addField(new KeyedCodec<>("TinkeringLevel", Codec.INTEGER),
                            (d, v) -> d.setLevel(SkillType.TINKERING, v),
                            d -> d.getLevel(SkillType.TINKERING))
                    .addField(new KeyedCodec<>("TinkeringXp", Codec.INTEGER),
                            (d, v) -> d.setXp(SkillType.TINKERING, v),
                            d -> d.getXp(SkillType.TINKERING))
                    .addField(new KeyedCodec<>("HuntingLevel", Codec.INTEGER),
                            (d, v) -> d.setLevel(SkillType.HUNTING, v),
                            d -> d.getLevel(SkillType.HUNTING))
                    .addField(new KeyedCodec<>("HuntingXp", Codec.INTEGER),
                            (d, v) -> d.setXp(SkillType.HUNTING, v),
                            d -> d.getXp(SkillType.HUNTING))
                    .addField(new KeyedCodec<>("SlayingLevel", Codec.INTEGER),
                            (d, v) -> d.setLevel(SkillType.SLAYING, v),
                            d -> d.getLevel(SkillType.SLAYING))
                    .addField(new KeyedCodec<>("SlayingXp", Codec.INTEGER),
                            (d, v) -> d.setXp(SkillType.SLAYING, v),
                            d -> d.getXp(SkillType.SLAYING))
                    .addField(new KeyedCodec<>("VoidslayingLevel", Codec.INTEGER),
                            (d, v) -> d.setLevel(SkillType.VOIDSLAYING, v),
                            d -> d.getLevel(SkillType.VOIDSLAYING))
                    .addField(new KeyedCodec<>("VoidslayingXp", Codec.INTEGER),
                            (d, v) -> d.setXp(SkillType.VOIDSLAYING, v),
                            d -> d.getXp(SkillType.VOIDSLAYING))
                    .addField(new KeyedCodec<>("WardingLevel", Codec.INTEGER),
                            (d, v) -> d.setLevel(SkillType.WARDING, v),
                            d -> d.getLevel(SkillType.WARDING))
                    .addField(new KeyedCodec<>("WardingXp", Codec.INTEGER),
                            (d, v) -> d.setXp(SkillType.WARDING, v),
                            d -> d.getXp(SkillType.WARDING))
                    .build();

    // -- State --

    private final EnumMap<SkillType, Integer> levels = new EnumMap<>(SkillType.class);
    private final EnumMap<SkillType, Integer> xp = new EnumMap<>(SkillType.class);

    /** Persisted total count of unique chunks ever explored. */
    private int exploredChunkCount;

    /** Persisted set of all explored chunk coordinates as packed longs. */
    private final Set<Long> exploredChunks = new HashSet<>();

    /** Last chunk position for exploration dedup. */
    private int lastChunkX = Integer.MIN_VALUE;
    private int lastChunkZ = Integer.MIN_VALUE;

    public PlayerSkillData() {
        for (SkillType skill : SkillType.values()) {
            levels.put(skill, 1);
            xp.put(skill, 0);
        }
    }

    // -- Level / XP accessors --

    public int getLevel(SkillType skill) {
        return levels.getOrDefault(skill, 1);
    }

    public void setLevel(SkillType skill, int level) {
        levels.put(skill, Math.max(1, Math.min(level, StatConstants.MAX_SKILL_LEVEL)));
    }

    public int getXp(SkillType skill) {
        return xp.getOrDefault(skill, 0);
    }

    public void setXp(SkillType skill, int value) {
        xp.put(skill, Math.max(0, value));
    }

    /** XP required to reach the next level for this skill. */
    public int xpForNextLevel(SkillType skill) {
        int lvl = getLevel(skill);
        return (int)(StatConstants.SKILL_BASE_XP *
                Math.pow(StatConstants.SKILL_XP_SCALING, lvl - 1));
    }

    /**
     * Award XP to a skill. Returns the number of levels gained (0 if none).
     */
    public int addXp(SkillType skill, int amount) {
        if (getLevel(skill) >= StatConstants.MAX_SKILL_LEVEL) return 0;
        int gained = 0;
        int current = getXp(skill) + amount;
        while (current >= xpForNextLevel(skill) && getLevel(skill) < StatConstants.MAX_SKILL_LEVEL) {
            current -= xpForNextLevel(skill);
            setLevel(skill, getLevel(skill) + 1);
            gained++;
        }
        setXp(skill, current);
        return gained;
    }

    // -- Exploration chunk tracking --

    /** Returns true if this chunk was already the last explored chunk. */
    public boolean isExplored(int chunkX, int chunkZ) {
        return chunkX == lastChunkX && chunkZ == lastChunkZ;
    }

    /**
     * Mark a chunk as explored. Returns true if it's a new chunk (not previously explored).
     * Tracks coordinates persistently so the map remembers explored areas.
     */
    public boolean markExplored(int chunkX, int chunkZ) {
        if (chunkX == lastChunkX && chunkZ == lastChunkZ) return false;
        lastChunkX = chunkX;
        lastChunkZ = chunkZ;
        long packed = packChunk(chunkX, chunkZ);
        if (!exploredChunks.add(packed)) return false;
        exploredChunkCount = exploredChunks.size();
        return true;
    }

    /** Get all explored chunk coordinates as packed longs. */
    public Set<Long> getExploredChunks() {
        return exploredChunks;
    }

    /** Pack two chunk coords into a single long. */
    public static long packChunk(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    /** Unpack chunk X from a packed long. */
    public static int unpackChunkX(long packed) {
        return (int) (packed >> 32);
    }

    /** Unpack chunk Z from a packed long. */
    public static int unpackChunkZ(long packed) {
        return (int) packed;
    }

    /** Total unique chunks explored (persisted across sessions). */
    public int getExploredCount() {
        return exploredChunkCount;
    }

    /** Current map reveal radius in chunks based on exploration level. */
    public int getMapRadius() {
        int level = getLevel(SkillType.EXPLORATION);
        return StatConstants.BASE_MAP_RADIUS +
                level / StatConstants.LEVELS_PER_MAP_RADIUS;
    }

    // -- Reward calculators (pure math - usable from tests) --

    /** Tool gathering speed multiplier bonus for a skill (e.g. 0.15 = +15%). */
    public float getToolSpeedBonus(SkillType skill) {
        return (getLevel(skill) - 1) * StatConstants.TOOL_SPEED_PER_LEVEL;
    }

    /** Chance to not consume tool durability (0.0–0.75). */
    public float getDurabilitySaveChance(SkillType skill) {
        return (getLevel(skill) - 1) * StatConstants.DURABILITY_SAVE_PER_LEVEL;
    }

    /** Chance to save one input material on crafting (0.0–0.40). */
    public float getMaterialSaveChance(SkillType skill) {
        return (getLevel(skill) - 1) * StatConstants.MATERIAL_SAVE_PER_LEVEL;
    }

    /** Crafting speed bonus (0.0–0.75). Same scaling as durability save. */
    public float getCraftingSpeedBonus(SkillType skill) {
        return (getLevel(skill) - 1) * StatConstants.CRAFTING_SPEED_PER_LEVEL;
    }

    /** Combat skill damage bonus for a given skill (0.0–0.50). */
    public float getCombatDamageBonus(SkillType skill) {
        return (getLevel(skill) - 1) * StatConstants.COMBAT_SKILL_DAMAGE_PER_LEVEL;
    }

    @Override
    public Component<EntityStore> clone() {
        PlayerSkillData copy = new PlayerSkillData();
        copy.levels.putAll(this.levels);
        copy.xp.putAll(this.xp);
        copy.exploredChunkCount = this.exploredChunkCount;
        copy.exploredChunks.addAll(this.exploredChunks);
        copy.lastChunkX = this.lastChunkX;
        copy.lastChunkZ = this.lastChunkZ;
        return copy;
    }
}
