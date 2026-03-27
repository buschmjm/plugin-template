package com.mmorpg.stats;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.InteractionType;

import javax.annotation.Nonnull;

/**
 * Custom interaction registered for Ability1/2/3 on MMORPG offhand items.
 * When the player presses Q/E/R, the game fires this interaction which
 * looks up the bound ability for the corresponding slot and casts it.
 */
public class AbilityCastInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<AbilityCastInteraction> CODEC = BuilderCodec.builder(
            AbilityCastInteraction.class, AbilityCastInteraction::new, SimpleInstantInteraction.CODEC
    ).build();

    @Override
    protected void firstRun(@Nonnull InteractionType interactionType,
                            @Nonnull InteractionContext ctx,
                            @Nonnull CooldownHandler cooldownHandler) {
        int slot;
        if (interactionType == InteractionType.Ability1) slot = 0;
        else if (interactionType == InteractionType.Ability2) slot = 1;
        else if (interactionType == InteractionType.Ability3) slot = 2;
        else return;

        Ref<EntityStore> ref = ctx.getEntity();
        if (ref == null || !ref.isValid()) return;

        Store<EntityStore> store = ref.getStore();
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return;

        try {
        PlayerAbilityData data = TransientDataStore.getAbilities(ref);
        String abilityId = data.getSlot(slot);
        if (abilityId == null) {
            playerRef.sendMessage(Message.raw("Ability slot " + (slot + 1)
                    + " is empty. Use /skill bind " + (slot + 1) + " <id>"));
            return;
        }

        AbilityCommand.castAbilityForPlayer(playerRef, ref, store, abilityId);
        } catch (Exception e) {
            playerRef.sendMessage(Message.raw("Ability error - please report this bug."));
        }
    }
}
