package com.mmorpg.stats;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * ECS component storing a player's land claim data.
 * Each player can claim up to MAX_CLAIMS chunks. Chunks are stored as "x,z" strings.
 * Trusted players (by UUID string) can interact with the claim owner's land.
 * Claim settings are stored as a bitmask for granular permission control.
 */
public class ClaimData implements Component<EntityStore> {

    /** Default settings: everything protected (no flags set = all blocked for non-owners) */
    public static final int FLAG_BREAK_BLOCKS   = 1;       // Allow visitors to break blocks
    public static final int FLAG_PLACE_BLOCKS   = 1 << 1;  // Allow visitors to place blocks
    public static final int FLAG_OPEN_DOORS     = 1 << 2;  // Allow visitors to open doors
    public static final int FLAG_OPEN_CONTAINERS = 1 << 3; // Allow visitors to open containers
    public static final int FLAG_PVP            = 1 << 4;  // Allow PvP on claimed land
    public static final int FLAG_MOB_DAMAGE     = 1 << 5;  // Allow mobs to damage blocks

    /** Flag names for display and command parsing */
    private static final Map<String, Integer> FLAG_MAP = new LinkedHashMap<>();
    static {
        FLAG_MAP.put("break", FLAG_BREAK_BLOCKS);
        FLAG_MAP.put("place", FLAG_PLACE_BLOCKS);
        FLAG_MAP.put("doors", FLAG_OPEN_DOORS);
        FLAG_MAP.put("containers", FLAG_OPEN_CONTAINERS);
        FLAG_MAP.put("pvp", FLAG_PVP);
        FLAG_MAP.put("mobdamage", FLAG_MOB_DAMAGE);
    }

    /** Preset claim overlay colors players can choose from */
    private static final Map<String, int[]> COLOR_PRESETS = new LinkedHashMap<>();
    static {
        COLOR_PRESETS.put("blue",    new int[]{70, 130, 230});
        COLOR_PRESETS.put("red",     new int[]{220, 60, 60});
        COLOR_PRESETS.put("green",   new int[]{60, 190, 80});
        COLOR_PRESETS.put("yellow",  new int[]{230, 210, 50});
        COLOR_PRESETS.put("purple",  new int[]{160, 70, 210});
        COLOR_PRESETS.put("orange",  new int[]{230, 140, 40});
        COLOR_PRESETS.put("cyan",    new int[]{40, 200, 210});
        COLOR_PRESETS.put("pink",    new int[]{230, 100, 170});
        COLOR_PRESETS.put("white",   new int[]{220, 220, 220});
        COLOR_PRESETS.put("lime",    new int[]{140, 230, 50});
    }

    /** Get color preset RGB by name, or null if not found */
    public static int[] getColorPreset(String name) {
        return COLOR_PRESETS.get(name.toLowerCase());
    }

    /** Get all available color names */
    public static Set<String> getColorNames() {
        return COLOR_PRESETS.keySet();
    }

    private String claimedChunks; // comma-delimited "x:z" pairs
    private String trustedPlayers; // comma-delimited UUID strings
    private int settings; // bitmask of FLAGS
    private String claimColor; // color name for map overlay (e.g. "blue")

    public static final BuilderCodec<ClaimData> CODEC =
            BuilderCodec.<ClaimData>builder(ClaimData.class, ClaimData::new)
                    .addField(new KeyedCodec<>("ClaimedChunks", Codec.STRING),
                            (data, value) -> data.claimedChunks = value,
                            data -> data.claimedChunks)
                    .addField(new KeyedCodec<>("TrustedPlayers", Codec.STRING),
                            (data, value) -> data.trustedPlayers = value,
                            data -> data.trustedPlayers)
                    .addField(new KeyedCodec<>("Settings", Codec.INTEGER),
                            (data, value) -> data.settings = value,
                            data -> data.settings)
                    .addField(new KeyedCodec<>("ClaimColor", Codec.STRING),
                            (data, value) -> data.claimColor = value,
                            data -> data.claimColor)
                    .build();

    public ClaimData() {
        this.claimedChunks = "";
        this.trustedPlayers = "";
        this.settings = 0; // all flags off = fully protected
        this.claimColor = "blue"; // default overlay color
    }

    public ClaimData(ClaimData clone) {
        this.claimedChunks = clone.claimedChunks;
        this.trustedPlayers = clone.trustedPlayers;
        this.settings = clone.settings;
        this.claimColor = clone.claimColor;
    }

    // -- Chunk management --

    /** Get set of claimed chunk keys ("x:z") */
    public Set<String> getClaimedChunkSet() {
        if (claimedChunks == null || claimedChunks.isEmpty()) return new HashSet<>();
        Set<String> set = new HashSet<>();
        for (String chunk : claimedChunks.split(",")) {
            String trimmed = chunk.trim();
            if (!trimmed.isEmpty()) set.add(trimmed);
        }
        return set;
    }

    /** Get number of claimed chunks */
    public int getClaimCount() {
        return getClaimedChunkSet().size();
    }

    /** Check if a chunk is claimed by this player */
    public boolean hasClaim(int chunkX, int chunkZ) {
        return getClaimedChunkSet().contains(chunkX + ":" + chunkZ);
    }

    /** Add a chunk claim. Returns true if added, false if already claimed. */
    public boolean addClaim(int chunkX, int chunkZ) {
        Set<String> chunks = getClaimedChunkSet();
        String key = chunkX + ":" + chunkZ;
        if (chunks.contains(key)) return false;
        chunks.add(key);
        this.claimedChunks = String.join(",", chunks);
        return true;
    }

    /** Remove a chunk claim. Returns true if removed, false if not found. */
    public boolean removeClaim(int chunkX, int chunkZ) {
        Set<String> chunks = getClaimedChunkSet();
        String key = chunkX + ":" + chunkZ;
        if (!chunks.remove(key)) return false;
        this.claimedChunks = String.join(",", chunks);
        return true;
    }

    // -- Trusted players --

    /** Get set of trusted player UUIDs */
    public Set<String> getTrustedPlayerSet() {
        if (trustedPlayers == null || trustedPlayers.isEmpty()) return new HashSet<>();
        Set<String> set = new HashSet<>();
        for (String uuid : trustedPlayers.split(",")) {
            String trimmed = uuid.trim();
            if (!trimmed.isEmpty()) set.add(trimmed);
        }
        return set;
    }

    /** Check if a player UUID is trusted */
    public boolean isTrusted(String uuid) {
        return getTrustedPlayerSet().contains(uuid);
    }

    /** Add a trusted player. Returns true if added. */
    public boolean addTrusted(String uuid) {
        Set<String> trusted = getTrustedPlayerSet();
        if (trusted.contains(uuid)) return false;
        trusted.add(uuid);
        this.trustedPlayers = String.join(",", trusted);
        return true;
    }

    /** Remove a trusted player. Returns true if removed. */
    public boolean removeTrusted(String uuid) {
        Set<String> trusted = getTrustedPlayerSet();
        if (!trusted.remove(uuid)) return false;
        this.trustedPlayers = String.join(",", trusted);
        return true;
    }

    // -- Settings flags --

    public int getSettings() { return settings; }

    /** Check if a specific flag is enabled (allowed for visitors) */
    public boolean hasFlag(int flag) { return (settings & flag) != 0; }

    // -- Claim color --

    /** Get the claim overlay color name */
    public String getClaimColor() {
        return claimColor != null && !claimColor.isEmpty() ? claimColor : "blue";
    }

    /** Set the claim overlay color name */
    public void setClaimColor(String color) {
        this.claimColor = color;
    }

    /** Get the RGB values for this player's claim color */
    public int[] getClaimColorRGB() {
        int[] rgb = getColorPreset(getClaimColor());
        return rgb != null ? rgb : new int[]{70, 130, 230}; // default blue
    }

    /** Set a flag on or off */
    public void setFlag(int flag, boolean enabled) {
        if (enabled) {
            settings |= flag;
        } else {
            settings &= ~flag;
        }
    }

    /** Toggle a flag. Returns the new state. */
    public boolean toggleFlag(int flag) {
        settings ^= flag;
        return (settings & flag) != 0;
    }

    /** Get flag integer by name, or -1 if not found */
    public static int getFlagByName(String name) {
        Integer flag = FLAG_MAP.get(name.toLowerCase());
        return flag != null ? flag : -1;
    }

    /** Get all flag names */
    public static Map<String, Integer> getAllFlags() {
        return Collections.unmodifiableMap(FLAG_MAP);
    }

    /** Convert chunk block coordinates to chunk coordinates */
    public static int blockToChunk(int blockCoord) {
        return Math.floorDiv(blockCoord, 16);
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new ClaimData(this);
    }
}
