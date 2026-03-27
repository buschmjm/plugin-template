package com.mmorpg.stats;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class GuildPage extends InteractiveCustomUIPage<GuildPage.GuildAction> {

    private static final int MAX_INVITE_SLOTS = 4;
    // Cache invitable player names so button clicks can reference them
    private final List<String> invitableNames = new ArrayList<>();

    public GuildPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, GuildAction.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("GuildPage.ui");

        GuildData guildData = store.getComponent(ref,
                StatsPlugin.getInstance().getGuildDataType());

        if (guildData == null || !guildData.isInGuild()) {
            cmd.set("#GuildInfo.TextSpans", Message.raw(
                    "Not in a guild. Use the button below or /guild create <name>"));
            cmd.set("#MemberHeader.TextSpans", Message.raw(""));
            cmd.set("#GuildLevel.TextSpans", Message.raw(""));
            cmd.set("#GuildBonus.TextSpans", Message.raw(""));
            cmd.set("#PersonalStats.TextSpans", Message.raw(""));
            // Show create button, hide leave button & confirm row
            cmd.set("#BtnLeave.Visible", false);
            cmd.set("#BtnCreate.Visible", true);
            cmd.set("#ConfirmLabel.Visible", false);
            cmd.set("#BtnConfirmYes.Visible", false);
            cmd.set("#BtnConfirmNo.Visible", false);
            // Hide invite section when not in guild
            cmd.set("#InviteHeader.TextSpans", Message.raw(""));
            hideInviteSlots(cmd);
        } else {
            String guildName = guildData.getGuildName();
            GuildRegistry.GuildInfo info = GuildRegistry.getGuild(guildName);

            cmd.set("#GuildInfo.TextSpans", Message.raw(
                    "Guild: " + guildName + " - Rank: " + guildData.getRankTitle()));

            if (info != null) {
                cmd.set("#GuildLevel.TextSpans", Message.raw(
                        "Level " + info.getLevel() + " - "
                                + info.getMemberCount() + " members - "
                                + info.getTotalXp() + " XP"));

                float bonus = GuildRegistry.getSharedXpBonus(guildName) * 100;
                cmd.set("#GuildBonus.TextSpans", Message.raw(
                        "Shared XP bonus: +" + String.format("%.0f%%", bonus)));

                // List members
                List<String> members = new ArrayList<>(info.getMemberUuids());
                for (int i = 0; i < 8; i++) {
                    String label = "#Member" + (i + 1);
                    if (i < members.size()) {
                        String uuid = members.get(i);
                        String name = ClaimRegistry.getOwnerName(uuid);
                        if (name == null) name = uuid.substring(0, 8) + "...";
                        cmd.set(label + ".TextSpans", Message.raw("- " + name));
                    } else {
                        cmd.set(label + ".TextSpans", Message.raw(""));
                    }
                }
            }

            cmd.set("#PersonalStats.TextSpans", Message.raw(
                    "Your donations: " + guildData.getGoldDonated() + "g - "
                            + "Quests done: " + guildData.getQuestsCompleted()));
            // Show leave button, hide create button & confirm row
            cmd.set("#BtnLeave.Visible", true);
            cmd.set("#BtnCreate.Visible", false);
            cmd.set("#ConfirmLabel.Visible", false);
            cmd.set("#BtnConfirmYes.Visible", false);
            cmd.set("#BtnConfirmNo.Visible", false);

            // Invite section - show online players not in this guild (officers+ only)
            if (guildData.isOfficerOrAbove()) {
                cmd.set("#InviteHeader.TextSpans", Message.raw("Invite Players:"));
                populateInviteList(cmd, store, guildData.getGuildName(), ref);
            } else {
                cmd.set("#InviteHeader.TextSpans", Message.raw(""));
                hideInviteSlots(cmd);
            }
        }

        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnLeave", EventData.of("Action", "LEAVE"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnBack", EventData.of("Action", "BACK"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnCreate", EventData.of("Action", "CREATE"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnConfirmYes", EventData.of("Action", "CONFIRM_CREATE"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnConfirmNo", EventData.of("Action", "CANCEL_CREATE"), false);
        for (int i = 1; i <= MAX_INVITE_SLOTS; i++) {
            events.addEventBinding(CustomUIEventBindingType.Activating,
                    "#BtnInvite" + i, EventData.of("Action", "INVITE_" + i), false);
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store, @Nonnull GuildAction data) {
        super.handleDataEvent(ref, store, data);
        if (data.action == null) return;

        switch (data.action) {
            case "LEAVE" -> {
                GuildData guildData = store.getComponent(ref,
                        StatsPlugin.getInstance().getGuildDataType());
                PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (guildData == null || pRef == null || !guildData.isInGuild()) return;

                if (guildData.isLeader()) {
                    pRef.sendMessage(Message.raw("Leaders must use /guild disband to disband."));
                    return;
                }

                String guildName = guildData.getGuildName();
                String uuid = pRef.getUuid().toString();
                GuildRegistry.removeMember(guildName, uuid);
                ClaimRegistry.setPlayerGuild(uuid, "");
                guildData.leaveGuild();
                store.putComponent(ref, StatsPlugin.getInstance().getGuildDataType(), guildData);
                StatsPlugin.getInstance().updatePlayerNametag(ref, store, pRef);

                pRef.sendMessage(Message.raw("You left " + guildName + "."));

                // Refresh page
                UICommandBuilder cmd = new UICommandBuilder();
                cmd.set("#GuildInfo.TextSpans", Message.raw("Not in a guild."));
                cmd.set("#GuildLevel.TextSpans", Message.raw(""));
                cmd.set("#GuildBonus.TextSpans", Message.raw(""));
                cmd.set("#MemberHeader.TextSpans", Message.raw(""));
                cmd.set("#PersonalStats.TextSpans", Message.raw(""));
                for (int i = 1; i <= 8; i++) {
                    cmd.set("#Member" + i + ".TextSpans", Message.raw(""));
                }
                cmd.set("#BtnLeave.Visible", false);
                cmd.set("#BtnCreate.Visible", true);
                this.sendUpdate(cmd, new UIEventBuilder(), false);
            }
            case "BACK" -> {
                Player player = store.getComponent(ref, Player.getComponentType());
                PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (player != null && pRef != null) {
                    player.getPageManager().openCustomPage(ref, store, new MainMenuPage(pRef));
                }
            }
            case "CREATE" -> {
                // Show confirmation prompt
                PlayerGoldData goldData = store.getComponent(ref,
                        StatsPlugin.getInstance().getPlayerGoldDataType());
                long gold = goldData != null ? goldData.getGold() : 0;
                UICommandBuilder cmd = new UICommandBuilder();
                cmd.set("#ConfirmLabel.Visible", true);
                cmd.set("#ConfirmLabel.TextSpans", Message.raw(
                        "Create a guild for " + GuildRegistry.GUILD_CREATION_COST
                        + " gold? (You have " + gold + "g)"));
                cmd.set("#BtnConfirmYes.Visible", true);
                cmd.set("#BtnConfirmNo.Visible", true);
                cmd.set("#BtnCreate.Visible", false);
                this.sendUpdate(cmd, new UIEventBuilder(), false);
            }
            case "CANCEL_CREATE" -> {
                UICommandBuilder cmd = new UICommandBuilder();
                cmd.set("#ConfirmLabel.Visible", false);
                cmd.set("#BtnConfirmYes.Visible", false);
                cmd.set("#BtnConfirmNo.Visible", false);
                cmd.set("#BtnCreate.Visible", true);
                this.sendUpdate(cmd, new UIEventBuilder(), false);
            }
            case "INVITE_1", "INVITE_2", "INVITE_3", "INVITE_4" -> {
                int inviteIndex = Integer.parseInt(data.action.substring("INVITE_".length())) - 1;
                if (inviteIndex < 0 || inviteIndex >= invitableNames.size()) return;

                String targetName = invitableNames.get(inviteIndex);
                PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (pRef == null) return;

                GuildData guildData = store.getComponent(ref,
                        StatsPlugin.getInstance().getGuildDataType());
                if (guildData == null || !guildData.isInGuild() || !guildData.isOfficerOrAbove()) {
                    setInviteStatus("You cannot invite players.");
                    return;
                }

                World world = store.getExternalData().getWorld();
                PlayerRef targetPlayerRef = findPlayer(world, targetName);
                if (targetPlayerRef == null) {
                    setInviteStatus(targetName + " is no longer online.");
                    return;
                }

                Ref<EntityStore> targetRef = targetPlayerRef.getReference();
                if (targetRef == null || !targetRef.isValid()) {
                    setInviteStatus(targetName + " is no longer available.");
                    return;
                }

                GuildData targetGuildData = store.getComponent(targetRef,
                        StatsPlugin.getInstance().getGuildDataType());
                if (targetGuildData != null && targetGuildData.isInGuild()) {
                    setInviteStatus(targetName + " is already in a guild.");
                    return;
                }
                if (targetGuildData == null) {
                    targetGuildData = targetRef.getStore().ensureAndGetComponent(targetRef,
                            StatsPlugin.getInstance().getGuildDataType());
                }

                String guildName = guildData.getGuildName();
                String targetUuid = targetPlayerRef.getUuid().toString();

                targetGuildData.joinGuild(guildName);
                GuildRegistry.addMember(guildName, targetUuid);
                ClaimRegistry.setPlayerGuild(targetUuid, guildName);

                pRef.sendMessage(Message.raw(targetName + " has joined " + guildName + "!"));
                targetPlayerRef.sendMessage(Message.raw("You have been added to guild: " + guildName));

                // Refresh the page to update member list and invite list
                Player player = store.getComponent(ref, Player.getComponentType());
                if (player != null) {
                    player.getPageManager().openCustomPage(ref, store, new GuildPage(pRef));
                }
            }
            case "CONFIRM_CREATE" -> {
                PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (pRef == null) return;

                GuildData guildData = store.getComponent(ref,
                        StatsPlugin.getInstance().getGuildDataType());
                if (guildData != null && guildData.isInGuild()) {
                    pRef.sendMessage(Message.raw("You are already in a guild."));
                    return;
                }

                PlayerGoldData goldData = store.getComponent(ref,
                        StatsPlugin.getInstance().getPlayerGoldDataType());
                if (goldData == null || !goldData.canAfford(GuildRegistry.GUILD_CREATION_COST)) {
                    pRef.sendMessage(Message.raw("Not enough gold. Need "
                            + GuildRegistry.GUILD_CREATION_COST + "g."));
                    return;
                }

                // Generate a guild name from player name
                String guildName = pRef.getUsername() + "'s Guild";
                String uuid = pRef.getUuid().toString();

                GuildRegistry.GuildInfo info = GuildRegistry.createGuild(guildName, uuid);
                if (info == null) {
                    // Name collision - append number
                    for (int i = 2; i <= 99; i++) {
                        guildName = pRef.getUsername() + "'s Guild " + i;
                        info = GuildRegistry.createGuild(guildName, uuid);
                        if (info != null) break;
                    }
                    if (info == null) {
                        pRef.sendMessage(Message.raw("Could not create guild. Try /guild create <name>."));
                        return;
                    }
                }

                goldData.spend(GuildRegistry.GUILD_CREATION_COST);
                if (guildData == null) guildData = store.ensureAndGetComponent(ref,
                        StatsPlugin.getInstance().getGuildDataType());
                guildData.createGuild(guildName);
                ClaimRegistry.setPlayerGuild(uuid, guildName);
                store.putComponent(ref, StatsPlugin.getInstance().getGuildDataType(), guildData);
                store.putComponent(ref, StatsPlugin.getInstance().getPlayerGoldDataType(), goldData);
                StatsPlugin.getInstance().updatePlayerNametag(ref, store, pRef);

                GoldLedger.log(pRef.getUsername(), "GUILD_CREATE",
                        -GuildRegistry.GUILD_CREATION_COST, goldData.getGold(), guildName);

                pRef.sendMessage(Message.raw("Guild '" + guildName + "' created!"));

                // Reopen the page to show guild info
                Player player = store.getComponent(ref, Player.getComponentType());
                if (player != null) {
                    player.getPageManager().openCustomPage(ref, store, new GuildPage(pRef));
                }
            }
        }
    }

    private void populateInviteList(UICommandBuilder cmd, Store<EntityStore> store,
                                     String guildName, Ref<EntityStore> ref) {
        invitableNames.clear();
        World world = store.getExternalData().getWorld();
        var guildType = StatsPlugin.getInstance().getGuildDataType();

        for (PlayerRef pr : world.getPlayerRefs()) {
            if (invitableNames.size() >= MAX_INVITE_SLOTS) break;
            Ref<EntityStore> prRef = pr.getReference();
            if (prRef == null || !prRef.isValid() || prRef.equals(ref)) continue;
            GuildData prGuild = store.getComponent(prRef, guildType);
            if (prGuild != null && prGuild.isInGuild()) continue;
            invitableNames.add(pr.getUsername());
        }

        for (int i = 1; i <= MAX_INVITE_SLOTS; i++) {
            if (i <= invitableNames.size()) {
                cmd.set("#InviteName" + i + ".TextSpans", Message.raw(invitableNames.get(i - 1)));
                cmd.set("#BtnInvite" + i + ".Visible", true);
                cmd.set("#InviteName" + i + ".Visible", true);
            } else {
                cmd.set("#InviteName" + i + ".TextSpans", Message.raw(""));
                cmd.set("#BtnInvite" + i + ".Visible", false);
                cmd.set("#InviteName" + i + ".Visible", false);
            }
        }
        cmd.set("#InviteStatus.TextSpans", Message.raw(""));
    }

    private void hideInviteSlots(UICommandBuilder cmd) {
        for (int i = 1; i <= MAX_INVITE_SLOTS; i++) {
            cmd.set("#InviteName" + i + ".TextSpans", Message.raw(""));
            cmd.set("#BtnInvite" + i + ".Visible", false);
            cmd.set("#InviteName" + i + ".Visible", false);
        }
        cmd.set("#InviteStatus.TextSpans", Message.raw(""));
    }

    private void setInviteStatus(String message) {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#InviteStatus.TextSpans", Message.raw(message));
        this.sendUpdate(cmd, new UIEventBuilder(), false);
    }

    private static PlayerRef findPlayer(World world, String name) {
        for (PlayerRef pr : world.getPlayerRefs()) {
            if (pr.getUsername().equalsIgnoreCase(name)) return pr;
        }
        return null;
    }

    public static class GuildAction {
        public static final BuilderCodec<GuildAction> CODEC =
                BuilderCodec.<GuildAction>builder(GuildAction.class, GuildAction::new)
                        .addField(new KeyedCodec<>("Action", Codec.STRING),
                                (d, v) -> d.action = v, d -> d.action)
                        .build();
        private String action;
    }
}
