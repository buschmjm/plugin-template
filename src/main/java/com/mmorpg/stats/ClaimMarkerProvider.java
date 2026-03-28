package com.mmorpg.stats;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.packets.worldmap.MapImage;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager;
import com.hypixel.hytale.server.core.universe.world.worldmap.markers.MarkersCollector;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * WorldMap overlay provider that tints claimed chunks on the map.
 *
 * COORDINATE SYSTEMS:
 *   Claim chunks use 16-block tiles (blockCoord >> 4).
 *   Map images use 32-block tiles (blockCoord >> 5).
 *   Conversion: mapChunk = claimChunk >> 1
 *
 * Strategy: mutate the server-side cached MapImage pixel data in place, then
 * clear those map chunks from the player's WorldMapTracker so they get re-sent
 * (via getImageAsync, which returns the cached tinted image) on the next tick.
 *
 * Runs on the WorldMap thread - must NOT access the ECS store.
 */
public class ClaimMarkerProvider implements WorldMapManager.MarkerProvider {

    public static final ClaimMarkerProvider INSTANCE = new ClaimMarkerProvider();

    private static final Logger LOG = Logger.getLogger("ClaimOverlay");
    private static final float TINT_ALPHA = 0.35f;

    /** Tracks which map chunks have been tinted and with what color (keyed by "mapX:mapZ"). */
    private static final ConcurrentHashMap<String, int[]> tintedColor = new ConcurrentHashMap<>();

    /** Original untinted palette data, keyed by "mapX:mapZ". */
    private static final ConcurrentHashMap<String, int[]> originalPalettes = new ConcurrentHashMap<>();

    /**
     * Per-player set of map chunk keys already force-cleared for re-send.
     * Prevents clearing every tick (which would cause flicker).
     */
    private static final ConcurrentHashMap<String, Set<String>> playerCleared = new ConcurrentHashMap<>();

    /** One-time flag to log the first overlay attempt for debugging. */
    private static volatile boolean firstLog = true;

    private ClaimMarkerProvider() {}

    /** Convert a 16-block claim chunk coord to a 32-block map chunk coord. */
    private static int claimToMap(int claimChunk) {
        return claimChunk >> 1;
    }

    @Override
    public void update(World world, Player player, MarkersCollector collector) {
        Map<String, String> allClaims = ClaimRegistry.getAllClaims();
        if (allClaims.isEmpty()) return;

        WorldMapManager wmm = world.getWorldMapManager();

        String viewerUuid = "";
        var playerEntityRef = player.getReference();
        if (playerEntityRef != null && playerEntityRef.isValid()) {
            PlayerRef playerRef = playerEntityRef.getStore()
                    .getComponent(playerEntityRef, PlayerRef.getComponentType());
            if (playerRef != null) {
                viewerUuid = playerRef.getUuid().toString();
            }
        }

        Set<String> cleared = playerCleared.computeIfAbsent(viewerUuid,
                k -> ConcurrentHashMap.newKeySet());

        LongOpenHashSet toResend = new LongOpenHashSet();

        for (Map.Entry<String, String> entry : allClaims.entrySet()) {
            String chunkKey = entry.getKey();
            String ownerUuid = entry.getValue();

            String[] parts = chunkKey.split(":");
            if (parts.length != 2) continue;

            int claimX, claimZ;
            try {
                claimX = Integer.parseInt(parts[0]);
                claimZ = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                continue;
            }

            // World position of claim chunk center (for markers and view distance)
            double worldX = claimX * 16.0 + 8.0;
            double worldZ = claimZ * 16.0 + 8.0;

            if (!collector.isInViewDistance(worldX, worldZ)) continue;

            // Convert to 32-block map chunk coords
            int mapX = claimToMap(claimX);
            int mapZ = claimToMap(claimZ);
            String mapKey = mapX + ":" + mapZ;

            // Tint the server-side cached map image (in place)
            int[] rgb = ClaimRegistry.getOwnerColor(ownerUuid);
            MapImage image = wmm.getImageIfInMemory(mapX, mapZ);

            if (firstLog) {
                firstLog = false;
                LOG.info("[MMORPG] Overlay: claim(" + claimX + "," + claimZ
                        + ") -> map(" + mapX + "," + mapZ + ") image="
                        + (image != null ? image.width + "x" + image.height
                                + " palette=" + (image.palette != null ? image.palette.length : 0) : "null")
                        + " rgb=[" + rgb[0] + "," + rgb[1] + "," + rgb[2] + "]");
            }

            if (image != null && image.palette != null && image.palette.length > 0) {
                boolean justTinted = applyTint(mapKey, image, rgb);
                if (justTinted || !cleared.contains(mapKey)) {
                    toResend.add(ChunkUtil.indexChunk(mapX, mapZ));
                    cleared.add(mapKey);
                }
            }

            // Overlay-only - no map markers
        }

        // Force tracker to re-send tinted map chunks
        if (!toResend.isEmpty()) {
            var tracker = player.getWorldMapTracker();
            if (tracker != null) {
                tracker.clearChunks(toResend);
            }
        }
    }

    /**
     * Tint the cached image in place. Returns true if newly tinted or re-tinted.
     */
    private boolean applyTint(String mapKey, MapImage image, int[] rgb) {
        int[] prevColor = tintedColor.get(mapKey);
        if (prevColor != null && Arrays.equals(prevColor, rgb)) {
            return false;
        }

        int[] originalPalette;
        if (prevColor == null) {
            originalPalette = image.palette.clone();
            originalPalettes.put(mapKey, originalPalette);
        } else {
            originalPalette = originalPalettes.get(mapKey);
            if (originalPalette != null) {
                System.arraycopy(originalPalette, 0, image.palette, 0, originalPalette.length);
            } else {
                originalPalette = image.palette.clone();
                originalPalettes.put(mapKey, originalPalette);
            }
        }

        float tR = rgb[0], tG = rgb[1], tB = rgb[2];
        float inv = 1.0f - TINT_ALPHA;
        for (int i = 0; i < image.palette.length; i++) {
            int pixel = originalPalette[i];
            int a = (pixel >> 24) & 0xFF;
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;

            r = Math.min(255, (int)(r * inv + tR * TINT_ALPHA));
            g = Math.min(255, (int)(g * inv + tG * TINT_ALPHA));
            b = Math.min(255, (int)(b * inv + tB * TINT_ALPHA));

            image.palette[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }

        tintedColor.put(mapKey, rgb.clone());
        return true;
    }

    /**
     * Restore original pixels for an unclaimed chunk.
     * Takes claim chunk coords (16-block) - converts to map coords internally.
     */
    public static void removeTint(int claimX, int claimZ, WorldMapManager wmm) {
        int mapX = claimX >> 1;
        int mapZ = claimZ >> 1;
        String mapKey = mapX + ":" + mapZ;
        tintedColor.remove(mapKey);
        int[] originalPalette = originalPalettes.remove(mapKey);
        if (originalPalette != null) {
            MapImage image = wmm.getImageIfInMemory(mapX, mapZ);
            if (image != null && image.palette != null) {
                System.arraycopy(originalPalette, 0, image.palette, 0,
                        Math.min(originalPalette.length, image.palette.length));
            }
        }
        for (Set<String> s : playerCleared.values()) {
            s.remove(mapKey);
        }
    }

    /**
     * Force re-tint of all chunks owned by a player (color change).
     */
    public static void invalidatePlayerTints(String ownerUuid) {
        Map<String, String> allClaims = ClaimRegistry.getAllClaims();
        for (Map.Entry<String, String> entry : allClaims.entrySet()) {
            if (entry.getValue().equals(ownerUuid)) {
                String[] parts = entry.getKey().split(":");
                if (parts.length != 2) continue;
                try {
                    int claimX = Integer.parseInt(parts[0]);
                    int claimZ = Integer.parseInt(parts[1]);
                    String mapKey = (claimX >> 1) + ":" + (claimZ >> 1);
                    tintedColor.remove(mapKey);
                    for (Set<String> s : playerCleared.values()) {
                        s.remove(mapKey);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    /** Clean up per-player tracking on disconnect. */
    public static void removePlayer(String uuid) {
        playerCleared.remove(uuid);
    }
}
