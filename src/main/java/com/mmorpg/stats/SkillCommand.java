package com.mmorpg.stats;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

/**
 * Player command to view profession skill levels and rewards.
 * /skill             - show all skills overview
 * /skill info <name> - show details for one skill
 */
public class SkillCommand extends AbstractAsyncCommand {

    // /skill - overview (default, no subcommand)
    @Nonnull
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
        Ref<EntityStore> ref = ctx.senderAsPlayerRef();
        if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);
        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        return CompletableFuture.runAsync(() -> {
            ComponentType<EntityStore, PlayerSkillData> skillType =
                    StatsPlugin.getInstance().getPlayerSkillDataType();
            PlayerSkillData data = store.getComponent(ref, skillType);
            if (data == null) {
                ctx.sendMessage(Message.raw("No skill data found."));
                return;
            }

            ctx.sendMessage(Message.raw("=== Profession Skills ==="));
            for (SkillType skill : SkillType.values()) {
                int level = data.getLevel(skill);
                int xp = data.getXp(skill);
                int needed = data.xpForNextLevel(skill);
                String progress = level >= StatConstants.MAX_SKILL_LEVEL
                        ? "MAX" : (xp + "/" + needed + " XP");
                ctx.sendMessage(Message.raw(
                        skill.getDisplayName() + ": Lv " + level + " (" + progress + ")"));
            }
            ctx.sendMessage(Message.raw("Chunks explored: " + data.getExploredCount()));
            ctx.sendMessage(Message.raw("Map radius: " + data.getMapRadius() + " chunks"));
        }, world);
    }

    // /skill info <name>
    private static class InfoCommand extends AbstractAsyncCommand {
        private final RequiredArg<String> nameArg;

        InfoCommand() {
            super("info", "Show details for a specific skill");
            this.nameArg = withRequiredArg("skill", "Skill name (e.g., mining, crafting)", ArgTypes.STRING);
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            String name = ctx.get(nameArg);
            SkillType skill = SkillType.fromName(name);
            if (skill == null) {
                ctx.sendMessage(Message.raw("Unknown skill: " + name));
                return CompletableFuture.completedFuture(null);
            }

            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);
            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();

            return CompletableFuture.runAsync(() -> {
                ComponentType<EntityStore, PlayerSkillData> skillType =
                        StatsPlugin.getInstance().getPlayerSkillDataType();
                PlayerSkillData data = store.getComponent(ref, skillType);
                if (data == null) {
                    ctx.sendMessage(Message.raw("No skill data found."));
                    return;
                }

                int level = data.getLevel(skill);
                int xp = data.getXp(skill);
                int needed = data.xpForNextLevel(skill);

                ctx.sendMessage(Message.raw("=== " + skill.getDisplayName() + " ==="));
                ctx.sendMessage(Message.raw(skill.getDescription()));
                ctx.sendMessage(Message.raw("Level: " + level + " / " + StatConstants.MAX_SKILL_LEVEL));

                if (level < StatConstants.MAX_SKILL_LEVEL) {
                    ctx.sendMessage(Message.raw("XP: " + xp + " / " + needed));
                } else {
                    ctx.sendMessage(Message.raw("XP: MAX LEVEL"));
                }

                switch (skill) {
                    case MINING, WOODCUTTING, FARMING, HERBALISM -> {
                        float speed = data.getToolSpeedBonus(skill) * 100;
                        float durSave = data.getDurabilitySaveChance(skill) * 100;
                        ctx.sendMessage(Message.raw(String.format(
                                "Speed bonus: +%.1f%%", speed)));
                        ctx.sendMessage(Message.raw(String.format(
                                "Durability save: %.1f%%", durSave)));
                    }
                    case CARPENTRY, MASONRY, SMITHING, PROCESSING -> {
                        float matSave = data.getMaterialSaveChance(skill) * 100;
                        float craftSpeed = data.getCraftingSpeedBonus(skill) * 100;
                        ctx.sendMessage(Message.raw(String.format(
                                "Material save chance: %.1f%%", matSave)));
                        ctx.sendMessage(Message.raw(String.format(
                                "Crafting speed bonus: +%.1f%%", craftSpeed)));
                    }
                    case EXPLORATION -> {
                        ctx.sendMessage(Message.raw("Chunks explored: " + data.getExploredCount()));
                        ctx.sendMessage(Message.raw("Map reveal radius: " + data.getMapRadius() + " chunks"));
                    }
                }
            }, world);
        }
    }

    public SkillCommand() {
        super("profession", "View your profession skill levels");
        addSubCommand(new InfoCommand());
    }

    @Override
    protected boolean canGeneratePermission() { return false; }
}
