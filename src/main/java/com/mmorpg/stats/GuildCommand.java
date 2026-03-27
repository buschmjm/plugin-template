package com.mmorpg.stats;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

/**
 * Guild commands:
 *   /guild              - show your guild info
 *   /guild create <name> - create a new guild (costs gold)
 *   /guild invite <player> - invite a player to your guild
 *   /guild join <name>   - join a guild you've been invited to
 *   /guild leave         - leave your current guild
 *   /guild donate <amount> - donate gold to level up the guild
 *   /guild promote <player> - promote a member to officer
 *   /guild demote <player>  - demote an officer to member
 *   /guild kick <player> - kick a member from the guild
 *   /guild disband       - disband the guild (leader only)
 *   /guild info <name>   - view info about any guild
 */
public class GuildCommand extends AbstractAsyncCommand {

    public GuildCommand() {
        super("guild", "View your guild info");
        addSubCommand(new CreateSubcommand());
        addSubCommand(new InviteSubcommand());
        addSubCommand(new JoinSubcommand());
        addSubCommand(new LeaveSubcommand());
        addSubCommand(new DonateSubcommand());
        addSubCommand(new PromoteSubcommand());
        addSubCommand(new DemoteSubcommand());
        addSubCommand(new KickSubcommand());
        addSubCommand(new DisbandSubcommand());
        addSubCommand(new GuildInfoSubcommand());
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
            var guildType = StatsPlugin.getInstance().getGuildDataType();
            GuildData guildData = store.getComponent(ref, guildType);
            if (guildData == null || !guildData.isInGuild()) {
                ctx.sendMessage(Message.raw("You are not in a guild. Use /guild create <name> or /guild join <name>."));
                return;
            }

            GuildRegistry.GuildInfo guildInfo = GuildRegistry.getGuild(guildData.getGuildName());
            if (guildInfo == null) {
                ctx.sendMessage(Message.raw("Guild data not found. Try leaving and rejoining."));
                return;
            }

            ctx.sendMessage(Message.raw("=== " + guildInfo.getName() + " ==="));
            ctx.sendMessage(Message.raw("Level: " + guildInfo.getLevel()
                    + " | XP: " + guildInfo.getXpInCurrentLevel() + "/" + guildInfo.getXpForNextLevel()));
            ctx.sendMessage(Message.raw("Members: " + guildInfo.getMemberCount()));
            ctx.sendMessage(Message.raw("Your Rank: " + guildData.getRankTitle()));
            ctx.sendMessage(Message.raw("Your Donations: " + guildData.getGoldDonated() + " gold"));
            ctx.sendMessage(Message.raw("Quests Completed: " + guildData.getQuestsCompleted()));

            float xpBonus = GuildRegistry.getSharedXpBonus(guildData.getGuildName());
            if (xpBonus > 0) {
                ctx.sendMessage(Message.raw("Guild XP Bonus: +" + (int)(xpBonus * 100) + "%"));
            }

            int extraClaims = guildInfo.getLevel() * ClaimCommand.CLAIMS_PER_GUILD_LEVEL;
            if (extraClaims > 0) {
                ctx.sendMessage(Message.raw("Bonus Claim Chunks: +" + extraClaims));
            }
        }, world);
    }

    // -- /guild create <name> --
    private static class CreateSubcommand extends AbstractAsyncCommand {
        private final RequiredArg<String> nameArg;

        CreateSubcommand() {
            super("create", "Create a new guild");
            this.nameArg = withRequiredArg("name", "Guild name", ArgTypes.STRING);
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
            String guildName = ctx.get(nameArg);

            return CompletableFuture.runAsync(() -> {
                // Validate name
                if (guildName.length() < 3 || guildName.length() > 20) {
                    ctx.sendMessage(Message.raw("Guild name must be 3-20 characters."));
                    return;
                }
                if (!guildName.matches("[a-zA-Z0-9_ ]+")) {
                    ctx.sendMessage(Message.raw("Guild name can only contain letters, numbers, spaces, and underscores."));
                    return;
                }

                var guildType = StatsPlugin.getInstance().getGuildDataType();
                GuildData guildData = store.getComponent(ref, guildType);
                if (guildData == null) return;

                if (guildData.isInGuild()) {
                    ctx.sendMessage(Message.raw("You must leave your current guild first."));
                    return;
                }

                // Check gold cost
                var goldType = StatsPlugin.getInstance().getPlayerGoldDataType();
                PlayerGoldData goldData = store.getComponent(ref, goldType);
                if (goldData == null || !goldData.canAfford(GuildRegistry.GUILD_CREATION_COST)) {
                    ctx.sendMessage(Message.raw("Creating a guild costs " + GuildRegistry.GUILD_CREATION_COST
                            + " gold. You don't have enough."));
                    return;
                }

                // Create the guild
                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef == null) return;
                String uuid = playerRef.getUuid().toString();

                GuildRegistry.GuildInfo info = GuildRegistry.createGuild(guildName, uuid);
                if (info == null) {
                    ctx.sendMessage(Message.raw("A guild with that name already exists."));
                    return;
                }

                goldData.spend(GuildRegistry.GUILD_CREATION_COST);
                guildData.createGuild(guildName);
                ClaimRegistry.setPlayerGuild(uuid, guildName);

                GoldLedger.log(playerRef.getUsername(), "GUILD_CREATE",
                        -GuildRegistry.GUILD_CREATION_COST, goldData.getGold(), guildName);

                ctx.sendMessage(Message.raw("Guild '" + guildName + "' created! Cost: "
                        + GuildRegistry.GUILD_CREATION_COST + " gold."));
                ctx.sendMessage(Message.raw("Invite players with /guild invite <player>."));
            }, world);
        }
    }

    // -- /guild invite <player> --
    private static class InviteSubcommand extends AbstractAsyncCommand {
        private final RequiredArg<String> playerArg;

        InviteSubcommand() {
            super("invite", "Invite a player to your guild");
            this.playerArg = withRequiredArg("player", "Player to invite", ArgTypes.STRING);
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
                var guildType = StatsPlugin.getInstance().getGuildDataType();
                GuildData guildData = store.getComponent(ref, guildType);
                if (guildData == null || !guildData.isInGuild()) {
                    ctx.sendMessage(Message.raw("You are not in a guild."));
                    return;
                }
                if (!guildData.isOfficerOrAbove()) {
                    ctx.sendMessage(Message.raw("Only officers and leaders can invite players."));
                    return;
                }

                PlayerRef targetPlayerRef = findPlayer(world, targetName);
                if (targetPlayerRef == null) {
                    ctx.sendMessage(Message.raw("Player not found: " + targetName));
                    return;
                }

                Ref<EntityStore> targetRef = targetPlayerRef.getReference();
                if (targetRef == null || !targetRef.isValid()) return;

                GuildData targetGuildData = store.getComponent(targetRef, guildType);
                if (targetGuildData == null) return;

                if (targetGuildData.isInGuild()) {
                    ctx.sendMessage(Message.raw(targetName + " is already in a guild."));
                    return;
                }

                // Direct join (simplified - no pending invite system for now)
                String guildName = guildData.getGuildName();
                String targetUuid = targetPlayerRef.getUuid().toString();

                targetGuildData.joinGuild(guildName);
                GuildRegistry.addMember(guildName, targetUuid);
                ClaimRegistry.setPlayerGuild(targetUuid, guildName);

                ctx.sendMessage(Message.raw(targetName + " has joined " + guildName + "!"));
                targetPlayerRef.sendMessage(Message.raw("You have been added to guild: " + guildName));
            }, world);
        }
    }

    // -- /guild join <name> --
    private static class JoinSubcommand extends AbstractAsyncCommand {
        private final RequiredArg<String> nameArg;

        JoinSubcommand() {
            super("join", "Join a guild");
            this.nameArg = withRequiredArg("name", "Guild name", ArgTypes.STRING);
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
            String guildName = ctx.get(nameArg);

            return CompletableFuture.runAsync(() -> {
                var guildType = StatsPlugin.getInstance().getGuildDataType();
                GuildData guildData = store.getComponent(ref, guildType);
                if (guildData == null) return;

                if (guildData.isInGuild()) {
                    ctx.sendMessage(Message.raw("You must leave your current guild first."));
                    return;
                }

                if (!GuildRegistry.guildExists(guildName)) {
                    ctx.sendMessage(Message.raw("Guild not found: " + guildName));
                    return;
                }

                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef == null) return;
                String uuid = playerRef.getUuid().toString();

                guildData.joinGuild(guildName);
                GuildRegistry.addMember(guildName, uuid);
                ClaimRegistry.setPlayerGuild(uuid, guildName);

                ctx.sendMessage(Message.raw("You joined guild: " + guildName + "!"));
            }, world);
        }
    }

    // -- /guild leave --
    private static class LeaveSubcommand extends AbstractAsyncCommand {
        LeaveSubcommand() { super("leave", "Leave your current guild"); }

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
                var guildType = StatsPlugin.getInstance().getGuildDataType();
                GuildData guildData = store.getComponent(ref, guildType);
                if (guildData == null || !guildData.isInGuild()) {
                    ctx.sendMessage(Message.raw("You are not in a guild."));
                    return;
                }

                if (guildData.isLeader()) {
                    ctx.sendMessage(Message.raw("Leaders must /guild disband or promote someone else first."));
                    return;
                }

                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef == null) return;
                String uuid = playerRef.getUuid().toString();
                String guildName = guildData.getGuildName();

                GuildRegistry.removeMember(guildName, uuid);
                ClaimRegistry.setPlayerGuild(uuid, null);
                guildData.leaveGuild();

                ctx.sendMessage(Message.raw("You left the guild."));
            }, world);
        }
    }

    // -- /guild donate <amount> --
    private static class DonateSubcommand extends AbstractAsyncCommand {
        private final RequiredArg<Integer> amountArg;

        DonateSubcommand() {
            super("donate", "Donate gold to level up your guild");
            this.amountArg = withRequiredArg("amount", "Gold amount to donate", ArgTypes.INTEGER);
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
            int amount = ctx.get(amountArg);

            return CompletableFuture.runAsync(() -> {
                if (amount <= 0) {
                    ctx.sendMessage(Message.raw("Amount must be positive."));
                    return;
                }

                var guildType = StatsPlugin.getInstance().getGuildDataType();
                GuildData guildData = store.getComponent(ref, guildType);
                if (guildData == null || !guildData.isInGuild()) {
                    ctx.sendMessage(Message.raw("You are not in a guild."));
                    return;
                }

                var goldType = StatsPlugin.getInstance().getPlayerGoldDataType();
                PlayerGoldData goldData = store.getComponent(ref, goldType);
                if (goldData == null || !goldData.canAfford(amount)) {
                    ctx.sendMessage(Message.raw("Not enough gold! You have "
                            + (goldData != null ? goldData.getGold() : 0) + " gold."));
                    return;
                }

                String guildName = guildData.getGuildName();
                int oldLevel = GuildRegistry.getGuildLevel(guildName);

                goldData.spend(amount);
                guildData.addGoldDonated(amount);
                GuildRegistry.donateGold(guildName, amount);

                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef != null) {
                    GoldLedger.log(playerRef.getUsername(), "GUILD_DONATE",
                            -amount, goldData.getGold(), guildName);
                }

                int newLevel = GuildRegistry.getGuildLevel(guildName);

                ctx.sendMessage(Message.raw("Donated " + amount + " gold to " + guildName
                        + "! (Total donated: " + guildData.getGoldDonated() + ")"));

                if (newLevel > oldLevel) {
                    // Announce level up to all guild members online
                    for (PlayerRef pr : world.getPlayerRefs()) {
                        Ref<EntityStore> prRef = pr.getReference();
                        if (prRef == null || !prRef.isValid()) continue;
                        GuildData prGuild = store.getComponent(prRef, guildType);
                        if (prGuild != null && guildName.equals(prGuild.getGuildName())) {
                            pr.sendMessage(Message.raw("[" + guildName + "] Guild leveled up to " + newLevel + "!"));
                        }
                    }
                }
            }, world);
        }
    }

    // -- /guild promote <player> --
    private static class PromoteSubcommand extends AbstractAsyncCommand {
        private final RequiredArg<String> playerArg;

        PromoteSubcommand() {
            super("promote", "Promote a member to officer");
            this.playerArg = withRequiredArg("player", "Player to promote", ArgTypes.STRING);
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
                var guildType = StatsPlugin.getInstance().getGuildDataType();
                GuildData guildData = store.getComponent(ref, guildType);
                if (guildData == null || !guildData.isLeader()) {
                    ctx.sendMessage(Message.raw("Only the guild leader can promote."));
                    return;
                }

                PlayerRef targetPlayerRef = findPlayer(world, targetName);
                if (targetPlayerRef == null) {
                    ctx.sendMessage(Message.raw("Player not found: " + targetName));
                    return;
                }
                Ref<EntityStore> targetRef = targetPlayerRef.getReference();
                if (targetRef == null || !targetRef.isValid()) return;

                GuildData targetGuild = store.getComponent(targetRef, guildType);
                if (targetGuild == null || !guildData.getGuildName().equals(targetGuild.getGuildName())) {
                    ctx.sendMessage(Message.raw(targetName + " is not in your guild."));
                    return;
                }

                if (targetGuild.isOfficerOrAbove()) {
                    ctx.sendMessage(Message.raw(targetName + " is already an officer or higher."));
                    return;
                }

                targetGuild.setGuildRank("officer");
                ctx.sendMessage(Message.raw(targetName + " promoted to Officer!"));
                targetPlayerRef.sendMessage(Message.raw("You have been promoted to Officer in " + guildData.getGuildName() + "!"));
            }, world);
        }
    }

    // -- /guild demote <player> --
    private static class DemoteSubcommand extends AbstractAsyncCommand {
        private final RequiredArg<String> playerArg;

        DemoteSubcommand() {
            super("demote", "Demote an officer to member");
            this.playerArg = withRequiredArg("player", "Player to demote", ArgTypes.STRING);
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
                var guildType = StatsPlugin.getInstance().getGuildDataType();
                GuildData guildData = store.getComponent(ref, guildType);
                if (guildData == null || !guildData.isLeader()) {
                    ctx.sendMessage(Message.raw("Only the guild leader can demote."));
                    return;
                }

                PlayerRef targetPlayerRef = findPlayer(world, targetName);
                if (targetPlayerRef == null) {
                    ctx.sendMessage(Message.raw("Player not found: " + targetName));
                    return;
                }
                Ref<EntityStore> targetRef = targetPlayerRef.getReference();
                if (targetRef == null || !targetRef.isValid()) return;

                GuildData targetGuild = store.getComponent(targetRef, guildType);
                if (targetGuild == null || !guildData.getGuildName().equals(targetGuild.getGuildName())) {
                    ctx.sendMessage(Message.raw(targetName + " is not in your guild."));
                    return;
                }

                if (!"officer".equals(targetGuild.getGuildRank())) {
                    ctx.sendMessage(Message.raw(targetName + " is not an officer."));
                    return;
                }

                targetGuild.setGuildRank("member");
                ctx.sendMessage(Message.raw(targetName + " demoted to Member."));
                targetPlayerRef.sendMessage(Message.raw("You have been demoted to Member in " + guildData.getGuildName() + "."));
            }, world);
        }
    }

    // -- /guild kick <player> --
    private static class KickSubcommand extends AbstractAsyncCommand {
        private final RequiredArg<String> playerArg;

        KickSubcommand() {
            super("kick", "Kick a member from your guild");
            this.playerArg = withRequiredArg("player", "Player to kick", ArgTypes.STRING);
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
                var guildType = StatsPlugin.getInstance().getGuildDataType();
                GuildData guildData = store.getComponent(ref, guildType);
                if (guildData == null || !guildData.isOfficerOrAbove()) {
                    ctx.sendMessage(Message.raw("Only officers and leaders can kick members."));
                    return;
                }

                PlayerRef targetPlayerRef = findPlayer(world, targetName);
                if (targetPlayerRef == null) {
                    ctx.sendMessage(Message.raw("Player not found: " + targetName));
                    return;
                }
                Ref<EntityStore> targetRef = targetPlayerRef.getReference();
                if (targetRef == null || !targetRef.isValid()) return;

                GuildData targetGuild = store.getComponent(targetRef, guildType);
                if (targetGuild == null || !guildData.getGuildName().equals(targetGuild.getGuildName())) {
                    ctx.sendMessage(Message.raw(targetName + " is not in your guild."));
                    return;
                }

                // Can't kick the leader
                if (targetGuild.isLeader()) {
                    ctx.sendMessage(Message.raw("You cannot kick the guild leader."));
                    return;
                }

                // Officers can't kick other officers
                if ("officer".equals(guildData.getGuildRank()) && targetGuild.isOfficerOrAbove()) {
                    ctx.sendMessage(Message.raw("Officers cannot kick other officers."));
                    return;
                }

                String guildName = guildData.getGuildName();
                String targetUuid = targetPlayerRef.getUuid().toString();

                GuildRegistry.removeMember(guildName, targetUuid);
                ClaimRegistry.setPlayerGuild(targetUuid, null);
                targetGuild.leaveGuild();

                ctx.sendMessage(Message.raw(targetName + " has been kicked from the guild."));
                targetPlayerRef.sendMessage(Message.raw("You have been kicked from " + guildName + "."));
            }, world);
        }
    }

    // -- /guild disband --
    private static class DisbandSubcommand extends AbstractAsyncCommand {
        DisbandSubcommand() { super("disband", "Disband your guild (leader only)"); }

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
                var guildType = StatsPlugin.getInstance().getGuildDataType();
                GuildData guildData = store.getComponent(ref, guildType);
                if (guildData == null || !guildData.isLeader()) {
                    ctx.sendMessage(Message.raw("Only the guild leader can disband the guild."));
                    return;
                }

                String guildName = guildData.getGuildName();

                // Remove all members
                for (PlayerRef pr : world.getPlayerRefs()) {
                    Ref<EntityStore> prRef = pr.getReference();
                    if (prRef == null || !prRef.isValid()) continue;
                    GuildData prGuild = store.getComponent(prRef, guildType);
                    if (prGuild != null && guildName.equals(prGuild.getGuildName())) {
                        ClaimRegistry.setPlayerGuild(pr.getUuid().toString(), null);
                        prGuild.leaveGuild();
                        if (!pr.getUuid().toString().equals(
                                store.getComponent(ref, PlayerRef.getComponentType()).getUuid().toString())) {
                            pr.sendMessage(Message.raw("Guild '" + guildName + "' has been disbanded."));
                        }
                    }
                }

                GuildRegistry.disbandGuild(guildName);
                ctx.sendMessage(Message.raw("Guild '" + guildName + "' has been disbanded."));
            }, world);
        }
    }

    // -- /guild info <name> --
    private static class GuildInfoSubcommand extends AbstractAsyncCommand {
        private final RequiredArg<String> nameArg;

        GuildInfoSubcommand() {
            super("info", "View info about a guild");
            this.nameArg = withRequiredArg("name", "Guild name to look up", ArgTypes.STRING);
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            String guildName = ctx.get(nameArg);

            return CompletableFuture.runAsync(() -> {
                GuildRegistry.GuildInfo info = GuildRegistry.getGuild(guildName);
                if (info == null) {
                    ctx.sendMessage(Message.raw("Guild not found: " + guildName));
                    return;
                }

                ctx.sendMessage(Message.raw("=== " + info.getName() + " ==="));
                ctx.sendMessage(Message.raw("Level: " + info.getLevel()
                        + " | XP: " + info.getXpInCurrentLevel() + "/" + info.getXpForNextLevel()));
                ctx.sendMessage(Message.raw("Members: " + info.getMemberCount()));
                float xpBonus = GuildRegistry.getSharedXpBonus(info.getName());
                if (xpBonus > 0) {
                    ctx.sendMessage(Message.raw("Shared XP Bonus: +" + (int)(xpBonus * 100) + "%"));
                }
            });
        }
    }

    // -- Utility --
    private static PlayerRef findPlayer(World world, String name) {
        for (PlayerRef pr : world.getPlayerRefs()) {
            if (pr.getUsername().equalsIgnoreCase(name)) return pr;
        }
        return null;
    }
}
