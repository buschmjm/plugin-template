package com.mmorpg.stats;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

/**
 * /commands - Lists all available server commands.
 * Available to every player.
 */
public class CommandsCommand extends AbstractAsyncCommand {

    public CommandsCommand() { super("commands", "List all available server commands"); }

    @Override
    protected boolean canGeneratePermission() { return false; }

    @Nonnull @Override
    protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
        send(ctx, "===============================");
        send(ctx, "  Server Commands");
        send(ctx, "===============================");

        send(ctx, "-- Core --------------------------------");
        send(ctx, "/menu              Open the main MMORPG UI hub");
        send(ctx, "/stats             Allocate stat points (STR/DEX/VIT/INT/PRE/ARC)");
        send(ctx, "/allocate <stat> [amount]  Quick stat allocation");

        send(ctx, "-- Classes -----------------------------");
        send(ctx, "/class list        View all earned class titles & passives");
        send(ctx, "/class select <id> Equip a class title (e.g. knight_3)");
        send(ctx, "/class auto on|off Auto-equip your highest earned title");
        send(ctx, "/class info <id>   View tier tree & passives for a class");

        send(ctx, "-- Skills & Abilities ------------------");
        send(ctx, "/skill list        List all skills with cooldowns");
        send(ctx, "/skill bind <1-3> <id>  Bind skill to quick-cast slot");
        send(ctx, "/skill binds       View your current binds");
        send(ctx, "/skill 1|2|3       Quick-cast a bound skill");
        send(ctx, "Tip: Equip class offhand (/shop offhand) to use Q/E/R keys");

        send(ctx, "-- Professions -------------------------");
        send(ctx, "/profession        View all profession levels & XP progress");
        send(ctx, "/profession info <name>  Details & bonuses for one profession");

        send(ctx, "-- Economy & Shop ----------------------");
        send(ctx, "/gold              Check your current gold balance");
        send(ctx, "/shop              Browse weapons (Swords/Axes/Daggers/Staves/Maces/Bows/Shields/Wands/Premium)");
        send(ctx, "/shop offhand      Browse class offhand items - 25g each");
        send(ctx, "/shop supplies     Browse potions, food, blocks & crafting materials");
        send(ctx, "  Shop also sells Armor: Helmets, Chestplates, Gloves, Leggings");
        send(ctx, "/trade <player> <amount>  Send gold to another player");
        send(ctx, "/goldlog           View recent gold transaction history");

        send(ctx, "-- Quests & Progress -------------------");
        send(ctx, "/quest             View weekly quests and progress");
        send(ctx, "/lb [level|gold|kills]  View the leaderboard");

        send(ctx, "-- Mounts & Pets -----------------------");
        send(ctx, "Use the Mounts and Pets buttons in /menu");
        send(ctx, "Mounts: Horse, Nightmare, Kirin, Pegasus, Chocobo + more");
        send(ctx, "Pets: floating companions with passive bonuses");

        send(ctx, "-- Land & Guilds ------------------------");
        send(ctx, "/claim             Claim the chunk you're standing in");
        send(ctx, "/claim unclaim     Release your claim");
        send(ctx, "/claim trust <player>   Trust a player on your land");
        send(ctx, "/claim info|list|settings  Manage claim settings");
        send(ctx, "/guild             View your guild");
        send(ctx, "/guild create <name>   Create a guild (500 gold)");
        send(ctx, "/guild invite|join|leave|donate|promote|demote|kick|disband");

        send(ctx, "-- Other --------------------------------");
        send(ctx, "/online            See all currently online players");
        send(ctx, "/commands          Show this list");
        send(ctx, "===============================");

        // Show admin commands if applicable
        Ref<EntityStore> ref = ctx.senderAsPlayerRef();
        if (ref != null && ref.isValid()) {
            PlayerRef pr = ref.getStore().getComponent(ref, PlayerRef.getComponentType());
            if (AdminCommand.isAdmin(pr)) {
                send(ctx, "-- Admin -----------------------------");
                send(ctx, "/rpgadmin setlevel <lvl>    Set your level");
                send(ctx, "/rpgadmin setstat <stat> <val>  Set a stat value");
                send(ctx, "/rpgadmin reset             Reset all stats to zero");
                send(ctx, "/rpgadmin respec            Refund spent stat points");
                send(ctx, "/rpgadmin tp <player>       Teleport to player");
                send(ctx, "/rpgadmin tphere <player>   Bring player to you");
                send(ctx, "-------------------------------------");
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    private static void send(CommandContext ctx, String text) {
        ctx.sendMessage(Message.raw(text));
    }
}
