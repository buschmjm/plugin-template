package com.mmorpg.stats;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

/**
 * Player command for using skills (abilities).
 * Usage:
 *   /skill use <id>       - Use a skill by ID
 *   /skill list            - List all available skills
 *   /skill bind <slot> <id> - Bind a skill to slot 1, 2, or 3
 *   /skill binds            - Show your current binds
 *   /skill 1|2|3            - Quick-cast the skill in that slot
 */
public class AbilityCommand extends AbstractAsyncCommand {

    /** Cast a skill on behalf of a player (used by chat shortcuts !1 !2 !3). */
    static void castAbilityForPlayer(PlayerRef playerRef, Ref<EntityStore> ref,
                                     Store<EntityStore> store, String abilityId) {
        castAbilityInternal(playerRef, ref, store, abilityId);
    }

    /** Shared cast logic used by /skill use and /skill 1|2|3. */
    static void castAbility(CommandContext ctx, Ref<EntityStore> ref,
                            Store<EntityStore> store, String abilityId) {
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef != null) {
            castAbilityInternal(playerRef, ref, store, abilityId);
        }
    }

    /** Core ability cast logic - sends feedback via PlayerRef. */
    private static void castAbilityInternal(PlayerRef playerRef, Ref<EntityStore> ref,
                                            Store<EntityStore> store, String abilityId) {
        AbilityDefinition ability = AbilityRegistry.get(abilityId);
        if (ability == null) {
            playerRef.sendMessage(Message.raw("Unknown skill: " + abilityId));
            return;
        }

        // Check stat requirements
        if (ability.hasRequirement()) {
            var statType = StatsPlugin.getInstance().getPlayerStatDataType();
            PlayerStatData stats = store.getComponent(ref, statType);
            if (stats == null || !ability.isUnlockedBy(stats)) {
                playerRef.sendMessage(Message.raw(ability.getName()
                        + " is locked! Requires " + ability.getRequirementText() + " points."));
                return;
            }
        }

        PlayerAbilityData abilityData = TransientDataStore.getAbilities(ref);

        if (abilityData.isOnCooldown(abilityId)) {
            float remaining = abilityData.getRemainingCooldown(abilityId);
            playerRef.sendMessage(Message.raw(ability.getName()
                    + " on cooldown (" + String.format("%.1f", remaining) + "s)"));
            return;
        }

        EntityStatMap statMap = (EntityStatMap) store.getComponent(ref,
                EntityStatMap.getComponentType());
        if (statMap == null) return;

        if (ability.getManaCost() > 0) {
            EntityStatValue mana = statMap.get(DefaultEntityStatTypes.getMana());
            if (mana == null || mana.get() < ability.getManaCost()) {
                int cur = mana != null ? (int) mana.get() : 0;
                playerRef.sendMessage(Message.raw("Not enough mana! Have "
                        + cur + " / need " + (int) ability.getManaCost()));
                return;
            }
        }

        if (ability.getStaminaCost() > 0) {
            EntityStatValue stamina = statMap.get(DefaultEntityStatTypes.getStamina());
            if (stamina == null || stamina.get() < ability.getStaminaCost()) {
                int cur = stamina != null ? (int) stamina.get() : 0;
                playerRef.sendMessage(Message.raw("Not enough stamina! Have "
                        + cur + " / need " + (int) ability.getStaminaCost()));
                return;
            }
        }

        // Deduct resources
        if (ability.getManaCost() > 0) {
            statMap.subtractStatValue(DefaultEntityStatTypes.getMana(),
                    ability.getManaCost());
        }
        if (ability.getStaminaCost() > 0) {
            statMap.subtractStatValue(DefaultEntityStatTypes.getStamina(),
                    ability.getStaminaCost());
        }

        abilityData.startCooldown(abilityId, ability.getCooldownSeconds());
        QuestTracker.record(ref, store, QuestObjective.USE_SKILL);

        if (ability.hasSelfBuff()) {
            BuffManager.addBuff(ref, store, ability.getSelfBuff());
        }
        if (ability.hasInstantHeal()) {
            float healAmount = ability.getInstantHeal();
            // Scale by ARC healing multiplier
            PlayerStatData pStats = store.getComponent(ref,
                    StatsPlugin.getInstance().getPlayerStatDataType());
            if (pStats != null) {
                healAmount *= StatCalculation.calculateHealingMultiplier(pStats.getArcana());
            }
            // Scale by class healing output passive
            PlayerClassData classData = store.getComponent(ref,
                    StatsPlugin.getInstance().getPlayerClassDataType());
            if (classData != null && classData.getCachedHealingOutputPercent() > 0) {
                healAmount *= (1.0f + classData.getCachedHealingOutputPercent());
            }
            statMap.addStatValue(DefaultEntityStatTypes.getHealth(), healAmount);
        }
        if (ability.hasSelfDamage()) {
            statMap.subtractStatValue(DefaultEntityStatTypes.getHealth(),
                    ability.getSelfDamage());
        }

        playerRef.sendMessage(Message.raw("Used " + ability.getName() + "!"
                + (ability.getManaCost() > 0 ? " [Mana: " + (int) statMap.get(DefaultEntityStatTypes.getMana()).get()
                        + "/" + (int) statMap.get(DefaultEntityStatTypes.getMana()).getMax() + "]" : "")
                + (ability.getStaminaCost() > 0 ? " [Stam: " + (int) statMap.get(DefaultEntityStatTypes.getStamina()).get()
                        + "/" + (int) statMap.get(DefaultEntityStatTypes.getStamina()).getMax() + "]" : "")));

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            var customHud = player.getHudManager().getCustomHud();
            if (customHud instanceof MmorpgHud mHud) mHud.forceRefresh(ref, store);
            else if (customHud instanceof CombatHud hud) hud.forceRefresh(ref, store);
        }
    }

    // Subcommand: /skill use <id>
    private static class UseCommand extends AbstractAsyncCommand {
        private final RequiredArg<String> abilityArg;

        UseCommand() {
            super("use", "Use a skill");
            this.abilityArg = withRequiredArg("skill", "Skill ID to use", ArgTypes.STRING);
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);

            Store<EntityStore> store = ref.getStore();
            var world = store.getExternalData().getWorld();
            String abilityId = ctx.get(abilityArg);

            return CompletableFuture.runAsync(() -> castAbility(ctx, ref, store, abilityId), world);
        }
    }

    // Subcommand: /skill bind <slot> <id>
    private static class BindCommand extends AbstractAsyncCommand {
        private final RequiredArg<Integer> slotArg;
        private final RequiredArg<String> abilityArg;

        BindCommand() {
            super("bind", "Bind a skill to slot 1, 2, or 3");
            this.slotArg = withRequiredArg("slot", "Slot number (1-3)", ArgTypes.INTEGER);
            this.abilityArg = withRequiredArg("skill", "Skill ID to bind", ArgTypes.STRING);
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);

            int slot = ctx.get(slotArg);
            String abilityId = ctx.get(abilityArg);

            if (slot < 1 || slot > 3) {
                ctx.sendMessage(Message.raw("Slot must be 1, 2, or 3."));
                return CompletableFuture.completedFuture(null);
            }

            AbilityDefinition ability = AbilityRegistry.get(abilityId);
            if (ability == null) {
                ctx.sendMessage(Message.raw("Unknown skill: " + abilityId
                        + ". Use /skill list to see available skills."));
                return CompletableFuture.completedFuture(null);
            }

            // Block binding locked abilities
            if (ability.hasRequirement()) {
                Store<EntityStore> store = ref.getStore();
                var statType = StatsPlugin.getInstance().getPlayerStatDataType();
                PlayerStatData stats = store.getComponent(ref, statType);
                if (stats == null || !ability.isUnlockedBy(stats)) {
                    ctx.sendMessage(Message.raw(ability.getName()
                            + " is locked! Requires " + ability.getRequirementText() + " points."));
                    return CompletableFuture.completedFuture(null);
                }
            }

            PlayerAbilityData data = TransientDataStore.getAbilities(ref);
            data.bindSlot(slot - 1, abilityId);

            Store<EntityStore> store = ref.getStore();
            StatsPlugin.persistAbilityBinds(ref, store);

            ctx.sendMessage(Message.raw("* Slot " + slot + " bound to "
                    + ability.getName() + " [" + abilityId + "]"));
            ctx.sendMessage(Message.raw("Use /skill " + slot + " to quick-cast."));
            return CompletableFuture.completedFuture(null);
        }
    }

    // Subcommand: /skill binds - show current binds
    private static class BindsCommand extends AbstractAsyncCommand {
        BindsCommand() {
            super("binds", "Show your skill binds");
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);

            PlayerAbilityData data = TransientDataStore.getAbilities(ref);
            ctx.sendMessage(Message.raw("=== Skill Binds ==="));
            for (int i = 0; i < 3; i++) {
                String id = data.getSlot(i);
                if (id != null) {
                    AbilityDefinition def = AbilityRegistry.get(id);
                    String name = def != null ? def.getName() : id;
                    String cooldown = "";
                    if (data.isOnCooldown(id)) {
                        cooldown = " (CD: " + String.format("%.1f",
                                data.getRemainingCooldown(id)) + "s)";
                    }
                    ctx.sendMessage(Message.raw("  Slot " + (i + 1) + ": "
                            + name + " [" + id + "]" + cooldown));
                } else {
                    ctx.sendMessage(Message.raw("  Slot " + (i + 1) + ": (empty)"));
                }
            }
            ctx.sendMessage(Message.raw("Use /skill bind <slot> <id> to set a bind."));
            return CompletableFuture.completedFuture(null);
        }
    }

    // Quick-cast subcommand factory for /skill 1, /skill 2, /skill 3
    private static class SlotCastCommand extends AbstractAsyncCommand {
        private final int slot; // 0-indexed

        SlotCastCommand(int slotNum) {
            super(String.valueOf(slotNum), "Quick-cast skill in slot " + slotNum);
            this.slot = slotNum - 1;
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);

            Store<EntityStore> store = ref.getStore();
            var world = store.getExternalData().getWorld();

            PlayerAbilityData data = TransientDataStore.getAbilities(ref);
            String abilityId = data.getSlot(slot);

            if (abilityId == null) {
                ctx.sendMessage(Message.raw("Slot " + (slot + 1)
                        + " is empty. Use /skill bind " + (slot + 1) + " <id> to set it."));
                return CompletableFuture.completedFuture(null);
            }

            return CompletableFuture.runAsync(
                    () -> castAbility(ctx, ref, store, abilityId), world);
        }
    }

    // Subcommand: /skill list
    private static class ListCommand extends AbstractAsyncCommand {
        ListCommand() {
            super("list", "List all available skills");
        }

        @Override
        protected boolean canGeneratePermission() { return false; }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);

            Store<EntityStore> store = ref.getStore();

            var abilities = AbilityRegistry.getAll();
            if (abilities.isEmpty()) {
                ctx.sendMessage(Message.raw("No skills registered."));
                return CompletableFuture.completedFuture(null);
            }

            PlayerAbilityData data = TransientDataStore.getAbilities(ref);

            var statType = StatsPlugin.getInstance().getPlayerStatDataType();
            PlayerStatData stats = store.getComponent(ref, statType);

            ctx.sendMessage(Message.raw("=== Skills ==="));
            for (AbilityDefinition ability : abilities) {
                boolean unlocked = stats != null && (!ability.hasRequirement() || ability.isUnlockedBy(stats));
                StringBuilder sb = new StringBuilder();
                sb.append(unlocked ? "\u2705 " : "\uD83D\uDD12 ");
                sb.append(ability.getName()).append(" [").append(ability.getId()).append("]");
                if (ability.hasRequirement()) {
                    sb.append(" (").append(ability.getRequirementText()).append(")");
                }
                if (!ability.getDescription().isEmpty()) {
                    sb.append(" - ").append(ability.getDescription());
                }
                sb.append(" | CD: ").append((int) ability.getCooldownSeconds()).append("s");
                if (ability.getManaCost() > 0) {
                    sb.append(" | Mana: ").append((int) ability.getManaCost());
                }
                if (ability.getStaminaCost() > 0) {
                    sb.append(" | Stamina: ").append((int) ability.getStaminaCost());
                }
                if (data != null && data.isOnCooldown(ability.getId())) {
                    sb.append(" (").append(String.format("%.1f",
                            data.getRemainingCooldown(ability.getId()))).append("s left)");
                }
                ctx.sendMessage(Message.raw(sb.toString()));
            }
            ctx.sendMessage(Message.raw("Use /skill bind <1-3> <id> to bind skills."));
            return CompletableFuture.completedFuture(null);
        }
    }

    public AbilityCommand() {
        super("skill", "Use and manage skills");
        addSubCommand(new UseCommand());
        addSubCommand(new ListCommand());
        addSubCommand(new BindCommand());
        addSubCommand(new BindsCommand());
        addSubCommand(new SlotCastCommand(1));
        addSubCommand(new SlotCastCommand(2));
        addSubCommand(new SlotCastCommand(3));
    }

    @Override
    protected boolean canGeneratePermission() { return false; }

    @Nonnull
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
        ctx.sendMessage(Message.raw("Usage: /skill use <id> | /skill list | /skill bind <1-3> <id> | /skill 1|2|3"));
        return CompletableFuture.completedFuture(null);
    }
}
