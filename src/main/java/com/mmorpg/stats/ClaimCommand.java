package com.mmorpg.stats;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Land claiming commands:
 *   /claim            - claim the chunk you're standing in
 *   /unclaim          - unclaim the chunk you're standing in
 *   /claim info       - show info about the current chunk
 *   /claim list       - list all your claimed chunks
 *   /claim trust      - trust a player on your claims
 *   /claim untrust    - remove a player's trust
 *   /claim setting    - toggle a claim setting flag
 *   /claim settings   - view all current settings
 */
public class ClaimCommand extends AbstractAsyncCommand {

    /** Maximum chunks a player can claim (increases with guild level) */
    public static final int BASE_MAX_CLAIMS = 9;
    public static final int CLAIMS_PER_GUILD_LEVEL = 2;

    public ClaimCommand() {
        super("claim", "Claim the chunk you're standing in");
        addSubCommand(new UnclaimSubcommand());
        addSubCommand(new InfoSubcommand());
        addSubCommand(new ListSubcommand());
        addSubCommand(new TrustSubcommand());
        addSubCommand(new UntrustSubcommand());
        addSubCommand(new SettingSubcommand());
        addSubCommand(new SettingsSubcommand());
        addSubCommand(new ColorSubcommand());
    }

    @Override
    protected boolean canGeneratePermission() { return false; }

    @Nonnull
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
        Ref<EntityStore> ref = ctx.senderAsPlayerRef();
        if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);
        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        return CompletableFuture.runAsync(() -> {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) return;
            String uuid = playerRef.getUuid().toString();

            // Get player position to determine chunk
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null) {
                ctx.sendMessage(Message.raw("Could not determine your position."));
                return;
            }

            var pos = transform.getPosition();
            int chunkX = ClaimData.blockToChunk((int) pos.getX());
            int chunkZ = ClaimData.blockToChunk((int) pos.getZ());

            // Check if already claimed
            String existingOwner = ClaimRegistry.getOwner(chunkX, chunkZ);
            if (existingOwner != null) {
                if (existingOwner.equals(uuid)) {
                    ctx.sendMessage(Message.raw("You already own this chunk!"));
                } else {
                    ctx.sendMessage(Message.raw("This chunk is already claimed by another player."));
                }
                return;
            }

            // Check claim limit
            var claimType = StatsPlugin.getInstance().getClaimDataType();
            ClaimData claimData = store.getComponent(ref, claimType);
            if (claimData == null) {
                ctx.sendMessage(Message.raw("Claim data not available."));
                return;
            }

            int maxClaims = getMaxClaims(ref, store);
            if (claimData.getClaimCount() >= maxClaims) {
                ctx.sendMessage(Message.raw("You've reached your claim limit (" + maxClaims
                        + " chunks). Unclaim some land or level up your guild for more."));
                return;
            }

            // Claim the chunk
            claimData.addClaim(chunkX, chunkZ);
            ClaimRegistry.register(chunkX, chunkZ, uuid);
            store.putComponent(ref, claimType, claimData);

            ctx.sendMessage(Message.raw("Claimed chunk [" + chunkX + ", " + chunkZ + "]! ("
                    + claimData.getClaimCount() + "/" + maxClaims + " claims used)"));
        }, world);
    }

    /** Calculate max claims based on guild level */
    static int getMaxClaims(Ref<EntityStore> ref, Store<EntityStore> store) {
        int max = BASE_MAX_CLAIMS;
        var guildType = StatsPlugin.getInstance().getGuildDataType();
        if (guildType != null) {
            GuildData guildData = store.getComponent(ref, guildType);
            if (guildData != null && !guildData.getGuildName().isEmpty()) {
                int guildLevel = GuildRegistry.getGuildLevel(guildData.getGuildName());
                max += guildLevel * CLAIMS_PER_GUILD_LEVEL;
            }
        }
        return max;
    }

    // -- /unclaim --
    private static class UnclaimSubcommand extends AbstractAsyncCommand {
        UnclaimSubcommand() { super("unclaim", "Unclaim the chunk you're standing in"); }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);
            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();

            return CompletableFuture.runAsync(() -> {
                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef == null) return;
                String uuid = playerRef.getUuid().toString();

                TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
                if (transform == null) return;

                var pos = transform.getPosition();
                int chunkX = ClaimData.blockToChunk((int) pos.getX());
                int chunkZ = ClaimData.blockToChunk((int) pos.getZ());

                if (!ClaimRegistry.isOwnedBy(chunkX, chunkZ, uuid)) {
                    ctx.sendMessage(Message.raw("You don't own this chunk."));
                    return;
                }

                var claimType = StatsPlugin.getInstance().getClaimDataType();
                ClaimData claimData = store.getComponent(ref, claimType);
                if (claimData != null) {
                    claimData.removeClaim(chunkX, chunkZ);
                    store.putComponent(ref, claimType, claimData);
                }
                ClaimRegistry.unregister(chunkX, chunkZ);
                ClaimMarkerProvider.removeTint(chunkX, chunkZ, world.getWorldMapManager());

                ctx.sendMessage(Message.raw("Unclaimed chunk [" + chunkX + ", " + chunkZ + "]."));
            }, world);
        }
    }

    // -- /claim info --
    private static class InfoSubcommand extends AbstractAsyncCommand {
        InfoSubcommand() { super("info", "Show info about the current chunk"); }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);
            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();

            return CompletableFuture.runAsync(() -> {
                TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
                if (transform == null) return;

                var pos = transform.getPosition();
                int chunkX = ClaimData.blockToChunk((int) pos.getX());
                int chunkZ = ClaimData.blockToChunk((int) pos.getZ());

                String owner = ClaimRegistry.getOwner(chunkX, chunkZ);
                if (owner == null) {
                    ctx.sendMessage(Message.raw("Chunk [" + chunkX + ", " + chunkZ + "] - Unclaimed"));
                } else {
                    // Try to find owner name
                    String ownerName = findPlayerName(store, owner);
                    String guild = ClaimRegistry.getPlayerGuild(owner);
                    ctx.sendMessage(Message.raw("Chunk [" + chunkX + ", " + chunkZ + "] - Owned by: "
                            + ownerName + (guild != null ? " [" + guild + "]" : "")));
                }
            }, world);
        }
    }

    // -- /claim list --
    private static class ListSubcommand extends AbstractAsyncCommand {
        ListSubcommand() { super("list", "List all your claimed chunks"); }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);
            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();

            return CompletableFuture.runAsync(() -> {
                var claimType = StatsPlugin.getInstance().getClaimDataType();
                ClaimData claimData = store.getComponent(ref, claimType);
                if (claimData == null) return;

                Set<String> chunks = claimData.getClaimedChunkSet();
                int maxClaims = getMaxClaims(ref, store);

                ctx.sendMessage(Message.raw("=== Your Claims (" + chunks.size() + "/" + maxClaims + ") ==="));
                if (chunks.isEmpty()) {
                    ctx.sendMessage(Message.raw("No chunks claimed. Use /claim to claim land."));
                } else {
                    for (String chunk : chunks) {
                        ctx.sendMessage(Message.raw("  Chunk [" + chunk.replace(":", ", ") + "]"));
                    }
                }
            }, world);
        }
    }

    // -- /claim trust <player> --
    private static class TrustSubcommand extends AbstractAsyncCommand {
        private final RequiredArg<String> playerArg;

        TrustSubcommand() {
            super("trust", "Trust a player on your claims");
            this.playerArg = withRequiredArg("player", "Player to trust", ArgTypes.STRING);
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);
            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();
            String targetName = ctx.get(playerArg);

            return CompletableFuture.runAsync(() -> {
                PlayerRef targetPlayerRef = findPlayer(world, targetName);
                if (targetPlayerRef == null) {
                    ctx.sendMessage(Message.raw("Player not found: " + targetName));
                    return;
                }

                String targetUuid = targetPlayerRef.getUuid().toString();
                var claimType = StatsPlugin.getInstance().getClaimDataType();
                ClaimData claimData = store.getComponent(ref, claimType);
                if (claimData == null) return;

                if (claimData.addTrusted(targetUuid)) {
                    ctx.sendMessage(Message.raw("Trusted " + targetName + " on all your claims."));
                } else {
                    ctx.sendMessage(Message.raw(targetName + " is already trusted."));
                }
            }, world);
        }
    }

    // -- /claim untrust <player> --
    private static class UntrustSubcommand extends AbstractAsyncCommand {
        private final RequiredArg<String> playerArg;

        UntrustSubcommand() {
            super("untrust", "Remove trust from a player");
            this.playerArg = withRequiredArg("player", "Player to untrust", ArgTypes.STRING);
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);
            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();
            String targetName = ctx.get(playerArg);

            return CompletableFuture.runAsync(() -> {
                PlayerRef targetPlayerRef = findPlayer(world, targetName);
                if (targetPlayerRef == null) {
                    ctx.sendMessage(Message.raw("Player not found: " + targetName));
                    return;
                }

                String targetUuid = targetPlayerRef.getUuid().toString();
                var claimType = StatsPlugin.getInstance().getClaimDataType();
                ClaimData claimData = store.getComponent(ref, claimType);
                if (claimData == null) return;

                if (claimData.removeTrusted(targetUuid)) {
                    ctx.sendMessage(Message.raw("Removed trust for " + targetName + "."));
                } else {
                    ctx.sendMessage(Message.raw(targetName + " was not trusted."));
                }
            }, world);
        }
    }

    // -- /claim setting <flag> --
    private static class SettingSubcommand extends AbstractAsyncCommand {
        private final RequiredArg<String> flagArg;

        SettingSubcommand() {
            super("setting", "Toggle a claim setting (break/place/doors/containers/pvp/mobdamage)");
            this.flagArg = withRequiredArg("flag", "Setting to toggle", ArgTypes.STRING);
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);
            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();
            String flagName = ctx.get(flagArg);

            return CompletableFuture.runAsync(() -> {
                int flag = ClaimData.getFlagByName(flagName);
                if (flag == -1) {
                    ctx.sendMessage(Message.raw("Unknown setting: " + flagName));
                    ctx.sendMessage(Message.raw("Valid settings: break, place, doors, containers, pvp, mobdamage"));
                    return;
                }

                var claimType = StatsPlugin.getInstance().getClaimDataType();
                ClaimData claimData = store.getComponent(ref, claimType);
                if (claimData == null) return;

                boolean newState = claimData.toggleFlag(flag);
                ctx.sendMessage(Message.raw("Claim setting '" + flagName + "' is now "
                        + (newState ? "ALLOWED" : "BLOCKED") + " for visitors."));
            }, world);
        }
    }

    // -- /claim settings --
    private static class SettingsSubcommand extends AbstractAsyncCommand {
        SettingsSubcommand() { super("settings", "View all claim settings"); }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);
            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();

            return CompletableFuture.runAsync(() -> {
                var claimType = StatsPlugin.getInstance().getClaimDataType();
                ClaimData claimData = store.getComponent(ref, claimType);
                if (claimData == null) return;

                ctx.sendMessage(Message.raw("=== Claim Settings ==="));
                for (Map.Entry<String, Integer> entry : ClaimData.getAllFlags().entrySet()) {
                    boolean allowed = claimData.hasFlag(entry.getValue());
                    ctx.sendMessage(Message.raw("  " + entry.getKey() + ": "
                            + (allowed ? "ALLOWED" : "BLOCKED")));
                }
                ctx.sendMessage(Message.raw("Use /claim setting <name> to toggle."));
            }, world);
        }
    }

    // -- /claim color <color> --
    private static class ColorSubcommand extends AbstractAsyncCommand {
        private final RequiredArg<String> colorArg;

        ColorSubcommand() {
            super("color", "Set your claim map overlay color");
            this.colorArg = withRequiredArg("color",
                    "Color name: " + String.join(", ", ClaimData.getColorNames()),
                    ArgTypes.STRING);
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);
            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();
            String colorName = ctx.get(colorArg);

            return CompletableFuture.runAsync(() -> {
                int[] rgb = ClaimData.getColorPreset(colorName);
                if (rgb == null) {
                    ctx.sendMessage(Message.raw("Unknown color: " + colorName));
                    ctx.sendMessage(Message.raw("Available colors: "
                            + String.join(", ", ClaimData.getColorNames())));
                    return;
                }

                var claimType = StatsPlugin.getInstance().getClaimDataType();
                ClaimData claimData = store.getComponent(ref, claimType);
                if (claimData == null) return;

                claimData.setClaimColor(colorName.toLowerCase());
                store.putComponent(ref, claimType, claimData);

                // Update the color cache so markers update immediately
                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef != null) {
                    String uuid = playerRef.getUuid().toString();
                    ClaimRegistry.setOwnerColor(uuid, rgb);
                    ClaimMarkerProvider.invalidatePlayerTints(uuid);
                }

                ctx.sendMessage(Message.raw("Claim map color set to: " + colorName));
            }, world);
        }
    }

    // -- Utility --

    /** Find a player by name in the world */
    private static PlayerRef findPlayer(World world, String name) {
        for (PlayerRef pr : world.getPlayerRefs()) {
            if (pr.getUsername().equalsIgnoreCase(name)) return pr;
        }
        return null;
    }

    /** Find a player's name by UUID */
    static String findPlayerName(Store<EntityStore> store, String uuid) {
        World world = store.getExternalData().getWorld();
        for (PlayerRef pr : world.getPlayerRefs()) {
            if (pr.getUuid().toString().equals(uuid)) return pr.getUsername();
        }
        return "Unknown";
    }
}
