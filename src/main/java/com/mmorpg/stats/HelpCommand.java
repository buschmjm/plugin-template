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
 * Help command with subcommands for different topics.
 *   /help           - Quick overview with topic list
 *   /help commands   - All available commands
 *   /help guide      - Game systems explained
 *   /help stats      - Stat system details
 *   /help classes    - Class title system
 *   /help skills     - Skills (abilities) info
 *   /help professions - Profession system
 */
public class HelpCommand extends AbstractAsyncCommand {

    private static void send(CommandContext ctx, String text) {
        ctx.sendMessage(Message.raw(text));
    }

    // /help commands
    private static class CommandsHelp extends AbstractAsyncCommand {
        CommandsHelp() { super("commands", "List all available commands"); }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            send(ctx, "=== Commands ===");
            send(ctx, "--- Character ---");
            send(ctx, "/stats - Open stat allocation UI");
            send(ctx, "/allocate <stat> [amount] - Quick stat allocation");
            send(ctx, "/class list - View earned class titles & passives");
            send(ctx, "/class select <titleId> - Equip a class title (e.g. knight_3)");
            send(ctx, "/class auto <on|off> - Auto-equip your highest earned title");
            send(ctx, "/class info <classId> - View tiers & passives for a class");
            send(ctx, "--- Skills & Abilities ---");
            send(ctx, "/skill use <id> - Use a skill");
            send(ctx, "/skill list - List all skills with cooldowns");
            send(ctx, "/skill bind <1-3> <id> - Bind a skill to a quick slot");
            send(ctx, "/skill binds - View your current binds");
            send(ctx, "/skill 1|2|3 - Quick-cast a bound skill");
            send(ctx, "Equip a class offhand (/shop offhand) to use ability keybinds (Q/E/R)");
            send(ctx, "--- Professions ---");
            send(ctx, "/profession - View all profession levels & progress");
            send(ctx, "/profession info <name> - Details & bonuses for a profession");
            send(ctx, "--- Economy ---");
            send(ctx, "/gold - Check your gold balance");
            send(ctx, "/shop - Browse weapons by category (Swords/Axes/Daggers/Staves/Maces/Bows/Shields/Wands/Premium)");
            send(ctx, "/shop buy <item> - Buy a weapon");
            send(ctx, "/shop offhand - Browse class offhand items (25g)");
            send(ctx, "/shop offhand buy <#> - Get a class offhand item");
            send(ctx, "  (Shop also carries Armor - Helmets, Chestplates, Gloves, Leggings)");
            send(ctx, "/trade <player> <amount> - Send gold to a player");
            send(ctx, "/goldlog - View your recent gold transactions");
            send(ctx, "/online - See all online players");
            send(ctx, "--- Quests & Progress ---");
            send(ctx, "/quest - View weekly quests and progress");
            send(ctx, "/lb [level|gold|kills] - View the leaderboard");
            send(ctx, "--- Mounts & Pets ---");
            send(ctx, "Access Mounts and Pets from /menu (top-right buttons)");
            send(ctx, "Available mounts: Horse, Nightmare, Kirin, Pegasus, Chocobo (Rare - tame via Chocobo Tales)");
            send(ctx, "Floating pets follow you and provide passive bonuses");
            send(ctx, "--- Land & Guilds ---");
            send(ctx, "/claim - Claim the chunk you're standing in");
            send(ctx, "/claim unclaim - Unclaim the current chunk");
            send(ctx, "/claim info - See who owns the current chunk");
            send(ctx, "/claim list - List all your claimed chunks");
            send(ctx, "/claim trust <player> - Trust a player on your land");
            send(ctx, "/claim untrust <player> - Remove trust from a player");
            send(ctx, "/claim setting <flag> - Toggle a protection flag");
            send(ctx, "/claim settings - View all protection settings");
            send(ctx, "/guild - View your guild info");
            send(ctx, "/guild create <name> - Create a guild (500 gold)");
            send(ctx, "/guild invite <player> - Invite a player");
            send(ctx, "/guild join <name> - Join a guild");
            send(ctx, "/guild leave - Leave your guild");
            send(ctx, "/guild donate <amount> - Donate gold to level up guild");
            send(ctx, "/guild promote/demote/kick <player> - Manage members");
            send(ctx, "/guild disband - Disband the guild (leader only)");
            send(ctx, "/guild info <name> - View info about any guild");
            send(ctx, "--- Other ---");
            send(ctx, "/menu - Open the main MMORPG hub");
            send(ctx, "/rpghelp [commands|guide|stats|classes|skills|professions] - Help topics");

            // Show admin commands if applicable
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref != null && ref.isValid()) {
                PlayerRef pr = ref.getStore().getComponent(ref, PlayerRef.getComponentType());
                if (AdminCommand.isAdmin(pr)) {
                    send(ctx, "--- Admin ---");
                    send(ctx, "/rpgadmin setlevel <lvl> - Set your level");
                    send(ctx, "/rpgadmin setstat <stat> <val> - Set a stat");
                    send(ctx, "/rpgadmin reset - Reset all stats to zero");
                    send(ctx, "/rpgadmin respec - Refund spent stat points");
                    send(ctx, "/rpgadmin tp <player> - Teleport to player");
                    send(ctx, "/rpgadmin tphere <player> - Bring player to you");
                }
            }
            send(ctx, "================");
            return CompletableFuture.completedFuture(null);
        }
    }

    // /help guide
    private static class GuideHelp extends AbstractAsyncCommand {
        GuideHelp() { super("guide", "Overview of all game systems"); }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            send(ctx, "=== MMORPG Guide ===");
            send(ctx, "This mod adds RPG progression to Hytale:");
            send(ctx, "");
            send(ctx, "STATS - You have 6 stats (STR, DEX, VIT, INT, PRE, ARC).");
            send(ctx, "  Each level grants 5 points to spend via /stats.");
            send(ctx, "  Stats boost HP, mana, damage, crit, dodge, and more.");
            send(ctx, "");
            send(ctx, "CLASSES - Investing in stats unlocks class titles.");
            send(ctx, "  21 titles across 5 tiers. Equip one for passive bonuses.");
            send(ctx, "  Single-stat tracks: Knight, Rogue, Brawler, Wizard, Protector, Druid.");
            send(ctx, "  Dual-stat hybrids: Duelist, Warrior, Warlock, Paladin, etc.");
            send(ctx, "");
            send(ctx, "SKILLS - Castable abilities with cooldowns and costs.");
            send(ctx, "  Use /skill list to see what's available.");
            send(ctx, "  Skills cost mana or stamina and go on cooldown.");
            send(ctx, "");
            send(ctx, "PROFESSIONS - Level up by gathering, crafting, exploring.");
            send(ctx, "  9 professions: Mining, Woodcutting, Farming, Herbalism,");
            send(ctx, "  Carpentry, Masonry, Smithing, Processing, and Exploration.");
            send(ctx, "  Higher levels give speed bonuses and material savings.");
            send(ctx, "");
            send(ctx, "COMBAT - Damage scales with STR/DEX/INT.");
            send(ctx, "  DEX adds crit chance, STR adds crit damage.");
            send(ctx, "  VIT gives damage resistance and HP.");
            send(ctx, "  XP is earned from kills; dying costs 10% of level XP.");
            send(ctx, "");
            send(ctx, "QUESTS - Weekly quests rotate every Sunday at noon UTC.");
            send(ctx, "  Complete quests for bonus XP rewards.");
            send(ctx, "  Use /quest to view active quests and progress.");
            send(ctx, "  Story quests are coming soon!");
            send(ctx, "");
            send(ctx, "Use /rpghelp <topic> for details: stats, classes, skills, professions, quests");
            send(ctx, "====================");
            return CompletableFuture.completedFuture(null);
        }
    }

    // /help stats
    private static class StatsHelp extends AbstractAsyncCommand {
        StatsHelp() { super("stats", "Stat system details"); }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            send(ctx, "=== Stats ===");
            send(ctx, "6 stats you allocate with /stats:");
            send(ctx, "");
            send(ctx, "STR (Strength)");
            send(ctx, "  +5% melee damage per point");
            send(ctx, "  +2% crit damage per point");
            send(ctx, "");
            send(ctx, "DEX (Dexterity)");
            send(ctx, "  +5 stamina per point");
            send(ctx, "  +4% light weapon / bow damage per point");
            send(ctx, "  +0.5% crit chance per point (max 75%)");
            send(ctx, "  +0.2% fall damage reduction per point");
            send(ctx, "");
            send(ctx, "VIT (Vitality)");
            send(ctx, "  +10 HP per point");
            send(ctx, "  +5 oxygen per point");
            send(ctx, "  +0.5 damage resistance per point");
            send(ctx, "");
            send(ctx, "INT (Intellect)");
            send(ctx, "  +5% spell damage per point");
            send(ctx, "  +0.5 mana regen per point");
            send(ctx, "");
            send(ctx, "PRE (Presence)");
            send(ctx, "  +0.198% vendor discount per point (99% at 500)");
            send(ctx, "  +0.1% enemy miss chance per point (max 50%)");
            send(ctx, "");
            send(ctx, "ARC (Arcana)");
            send(ctx, "  +8 mana per point");
            send(ctx, "  +4% healing output per point");
            send(ctx, "");
            send(ctx, "You get 5 points per level, max level 100 (500 points total).");
            send(ctx, "=============");
            return CompletableFuture.completedFuture(null);
        }
    }

    // /help classes
    private static class ClassesHelp extends AbstractAsyncCommand {
        ClassesHelp() { super("classes", "Class title system"); }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            send(ctx, "=== Class Titles ===");
            send(ctx, "Investing in stats automatically unlocks class titles.");
            send(ctx, "Each title gives passive bonuses when equipped.");
            send(ctx, "Use /class list to see earned titles, /class select to equip.");
            send(ctx, "");
            send(ctx, "Single-stat tracks (Apprentice > Acolyte > Base > Grand > T5):");
            send(ctx, "  STR Knight - T5: Worldbreaker");
            send(ctx, "  DEX Rogue - T5: Phantom");
            send(ctx, "  VIT Brawler - T5: Golem");
            send(ctx, "  INT Wizard - T5: Sage");
            send(ctx, "  PRE Protector - T5: Sovereign");
            send(ctx, "  ARC Druid - T5: World-Root");
            send(ctx, "");
            send(ctx, "Thresholds: 12 / 30 / 60 / 120 / 150 points in one stat.");
            send(ctx, "");
            send(ctx, "Dual-stat hybrid tracks (15 total):");
            send(ctx, "  STR+DEX Duelist, STR+VIT Warrior, STR+INT Warlock,");
            send(ctx, "  STR+PRE Champion, STR+ARC Beastwarrior, DEX+VIT Pursuer,");
            send(ctx, "  DEX+INT Trickster, DEX+PRE Swashbuckler, DEX+ARC Ranger,");
            send(ctx, "  VIT+INT Blightcaster, VIT+PRE Sentinel, VIT+ARC Paladin,");
            send(ctx, "  INT+PRE Scholar, INT+ARC Arcanist, PRE+ARC Hexguard");
            send(ctx, "Thresholds: 6/15/30/60/75 in EACH of the two stats.");
            send(ctx, "===================");
            return CompletableFuture.completedFuture(null);
        }
    }

    // /help skills
    private static class SkillsHelp extends AbstractAsyncCommand {
        SkillsHelp() { super("skills", "Skills (abilities) info"); }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            send(ctx, "=== Skills ===");
            send(ctx, "Use /skill use <id> to activate. /skill list for full details.");
            send(ctx, "Bind up to 3 skills: /skill bind <1-3> <id>");
            send(ctx, "Quick-cast: /skill 1, /skill 2, /skill 3");
            send(ctx, "Or equip a class offhand (/shop offhand) and use Q/E/R keybinds!");
            send(ctx, "");
            send(ctx, "--- STR Skills ---");
            send(ctx, "War Cry (war_cry) - +25% damage for 15s | 20 mana | 30s CD");
            send(ctx, "Berserker Rage (berserker_rage) - -20 HP, +50% damage 8s | 45s CD");
            send(ctx, "--- DEX Skills ---");
            send(ctx, "Shadow Step (shadow_step) - +100 stamina for 12s | 15 stamina | 35s CD");
            send(ctx, "Evasion (evasion) - +50 stamina, +15% damage 10s | 10 stamina | 30s CD");
            send(ctx, "--- VIT Skills ---");
            send(ctx, "Iron Skin (iron_skin) - +50 max HP for 20s | 25 stamina | 60s CD");
            send(ctx, "Fortify (fortify) - +80 max HP for 15s | 20 stamina | 50s CD");
            send(ctx, "Second Wind (second_wind) - Heal 5 HP/s for 10s | 30 mana | 45s CD");
            send(ctx, "Quick Recovery (quick_recovery) - Instant 30 HP | 25 mana | 20s CD");
            send(ctx, "--- INT Skills ---");
            send(ctx, "Mind Spike (mind_spike) - +35% damage for 10s | 35 mana | 40s CD");
            send(ctx, "Arcane Focus (arcane_focus) - +40 mana for 30s | 10 mana | 45s CD");
            send(ctx, "Mana Shield (mana_shield) - +60 mana, +30 HP 12s | 20 mana | 40s CD");
            send(ctx, "--- PRE Skills ---");
            send(ctx, "Rally (rally) - Heal 20 HP, +60 stamina for 15s | 20 stamina | 50s CD");
            send(ctx, "Inspire (inspire) - Heal 20 HP, +20% damage 12s | 15 stamina | 40s CD");
            send(ctx, "--- ARC Skills ---");
            send(ctx, "Life Tap (life_tap) - Trade 15 HP for +40 mana 8s | no cost | 25s CD");
            send(ctx, "Soul Drain (soul_drain) - -25 HP, heal 8 HP/s for 6s | 35s CD");
            send(ctx, "");
            send(ctx, "Skills cost mana or stamina and go on cooldown after use.");
            send(ctx, "==============");
            return CompletableFuture.completedFuture(null);
        }
    }

    // /help professions
    private static class ProfessionsHelp extends AbstractAsyncCommand {
        ProfessionsHelp() { super("professions", "Profession system"); }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            send(ctx, "=== Professions ===");
            send(ctx, "Level up professions by performing related activities.");
            send(ctx, "Use /profession to see levels, /profession info <name> for details.");
            send(ctx, "");
            send(ctx, "Mining - Mine blocks for XP. Boosts pickaxe speed & durability.");
            send(ctx, "Woodcutting - Chop trees for XP. Boosts axe speed & durability.");
            send(ctx, "Farming - Use hoe/watering can for XP. Boosts tool speed & durability.");
            send(ctx, "Herbalism - Harvest plants/crops for XP. Boosts gather speed & durability.");
            send(ctx, "Carpentry - Craft at workbench for XP. Chance to save materials.");
            send(ctx, "Masonry - Craft structural items for XP. Chance to save materials.");
            send(ctx, "Smithing - Craft from diagrams for XP. Chance to save materials.");
            send(ctx, "Processing - Smelt/refine for XP. Chance to save materials.");
            send(ctx, "Exploration - Discover new chunks. Increases map reveal radius.");
            send(ctx, "");
            send(ctx, "Max level: 100. Higher levels = bigger bonuses.");
            send(ctx, "===================");
            return CompletableFuture.completedFuture(null);
        }
    }

    // /help quests
    private static class QuestsHelp extends AbstractAsyncCommand {
        QuestsHelp() { super("quests", "Weekly quest system"); }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            send(ctx, "=== Quests ===");
            send(ctx, "Weekly quests rotate every Sunday at noon UTC.");
            send(ctx, "Complete quests to earn bonus XP rewards.");
            send(ctx, "Use /quest to see active quests and your progress.");
            send(ctx, "");
            send(ctx, "Quest types include:");
            send(ctx, "  Combat - Slay mobs, deal damage, survive hits");
            send(ctx, "  Gathering - Mine, chop, harvest blocks");
            send(ctx, "  Crafting - Craft and process items");
            send(ctx, "  Exploration - Discover new chunks");
            send(ctx, "  Skills - Use your combat skills");
            send(ctx, "  Progression - Earn XP, spend stat points");
            send(ctx, "");
            send(ctx, "5 quests are active each week.");
            send(ctx, "Story quests are coming soon!");
            send(ctx, "===============");
            return CompletableFuture.completedFuture(null);
        }
    }

    // /help economy
    private static class EconomyHelp extends AbstractAsyncCommand {
        EconomyHelp() { super("economy", "Gold, shops, and trading"); }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            send(ctx, "=== Economy ===");
            send(ctx, "Earn gold by killing mobs and completing quests.");
            send(ctx, "Gold scales with mob level and your level difference.");
            send(ctx, "New players start with " + StatConstants.STARTING_GOLD + " gold.");
            send(ctx, "");
            send(ctx, "/gold - Check your balance");
            send(ctx, "/shop - Browse available weapons");
            send(ctx, "/shop buy <item> - Purchase a weapon");
            send(ctx, "/trade <player> <amount> - Send gold to another player");
            send(ctx, "");
            send(ctx, "Weapons give bonus damage multipliers and stat bonuses.");
            send(ctx, "Higher-tier weapons cost more but are much more powerful.");
            send(ctx, "===============");
            return CompletableFuture.completedFuture(null);
        }
    }

    // /help claims
    private static class ClaimsHelp extends AbstractAsyncCommand {
        ClaimsHelp() { super("claims", "Land claiming & protection"); }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            send(ctx, "=== Land Claims ===");
            send(ctx, "Claim 16x16 chunks to protect your builds.");
            send(ctx, "Base limit: " + ClaimCommand.BASE_MAX_CLAIMS + " chunks (more with guild levels).");
            send(ctx, "");
            send(ctx, "/claim - Claim the chunk you're in");
            send(ctx, "/claim unclaim - Give up the current chunk");
            send(ctx, "/claim info - See who owns the current chunk");
            send(ctx, "/claim list - List all your claims");
            send(ctx, "/claim trust <player> - Let someone build on your land");
            send(ctx, "/claim untrust <player> - Revoke access");
            send(ctx, "");
            send(ctx, "Protection settings (toggle with /claim setting <flag>):");
            send(ctx, "  break - Block breaking");
            send(ctx, "  place - Block placing");
            send(ctx, "  doors - Opening doors");
            send(ctx, "  containers - Opening containers");
            send(ctx, "  pvp - Player vs player combat");
            send(ctx, "  mobdamage - Mob block damage");
            send(ctx, "");
            send(ctx, "By default, all actions are BLOCKED for visitors.");
            send(ctx, "Guild members automatically have access to guildmates' claims.");
            send(ctx, "===================");
            return CompletableFuture.completedFuture(null);
        }
    }

    // /help guilds
    private static class GuildsHelp extends AbstractAsyncCommand {
        GuildsHelp() { super("guilds", "Guild system & perks"); }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            send(ctx, "=== Guilds ===");
            send(ctx, "Create or join a guild for shared perks and land access.");
            send(ctx, "Creating a guild costs " + GuildRegistry.GUILD_CREATION_COST + " gold.");
            send(ctx, "");
            send(ctx, "/guild - View your guild info");
            send(ctx, "/guild create <name> - Create a guild");
            send(ctx, "/guild invite <player> - Invite someone");
            send(ctx, "/guild join <name> - Join a guild");
            send(ctx, "/guild leave - Leave your guild");
            send(ctx, "/guild donate <amount> - Donate gold for guild XP");
            send(ctx, "/guild promote/demote/kick <player> - Manage members");
            send(ctx, "/guild disband - Disband (leader only)");
            send(ctx, "/guild info <name> - View any guild's info");
            send(ctx, "");
            send(ctx, "Guild Perks (by level):");
            send(ctx, "  Shared XP bonus: +1% per guild level (max 20%)");
            send(ctx, "  Extra claim chunks: +" + ClaimCommand.CLAIMS_PER_GUILD_LEVEL + " per guild level");
            send(ctx, "  Guild members share land access automatically");
            send(ctx, "");
            send(ctx, "Ranks: Recruit > Initiate > Regular > Veteran > Champion");
            send(ctx, "  (based on quests completed while in the guild)");
            send(ctx, "  Officers can invite and kick. Leaders can promote/demote/disband.");
            send(ctx, "==============");
            return CompletableFuture.completedFuture(null);
        }
    }

    public HelpCommand() {
        super("rpghelp", "Get help on game systems and commands");
        addSubCommand(new CommandsHelp());
        addSubCommand(new GuideHelp());
        addSubCommand(new StatsHelp());
        addSubCommand(new ClassesHelp());
        addSubCommand(new SkillsHelp());
        addSubCommand(new ProfessionsHelp());
        addSubCommand(new QuestsHelp());
        addSubCommand(new EconomyHelp());
        addSubCommand(new ClaimsHelp());
        addSubCommand(new GuildsHelp());
    }

    @Override
    protected boolean canGeneratePermission() { return false; }

    @Nonnull
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
        send(ctx, "=== MMORPG Help ===");
        send(ctx, "Use /rpghelp <topic> for details:");
        send(ctx, "");
        send(ctx, "/rpghelp guide       - How the game systems work");
        send(ctx, "/rpghelp commands    - List all commands");
        send(ctx, "/rpghelp stats       - Stat details & scaling");
        send(ctx, "/rpghelp classes     - Class title unlocks & tiers");
        send(ctx, "/rpghelp skills      - Available skills & cooldowns");
        send(ctx, "/rpghelp professions - Profession leveling & bonuses");
        send(ctx, "/rpghelp quests     - Weekly quest system");
        send(ctx, "/rpghelp economy    - Gold, shops, and trading");
        send(ctx, "/rpghelp claims     - Land claiming & protection");
        send(ctx, "/rpghelp guilds     - Guild system & perks");
        send(ctx, "==================");
        return CompletableFuture.completedFuture(null);
    }
}
