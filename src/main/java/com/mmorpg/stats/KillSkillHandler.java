package com.mmorpg.stats;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.damage.event.KillFeedEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;

import java.util.logging.Logger;

/**
 * Listens for KillFeedEvent.KillerMessage to award combat skill XP
 * (Hunting, Slaying, Void Slaying, Warding) based on the killed mob's category.
 */
public class KillSkillHandler extends EntityEventSystem<EntityStore, KillFeedEvent.KillerMessage> {

    private static final Logger LOGGER = Logger.getLogger("MMORPGStats");
    private static final Query<EntityStore> QUERY = Query.any();

    private final ComponentType<EntityStore, PlayerSkillData> skillType;

    public KillSkillHandler() {
        super(KillFeedEvent.KillerMessage.class);
        this.skillType = StatsPlugin.getInstance().getPlayerSkillDataType();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public void onSystemRegistered() {
        LOGGER.info("[KillSkillHandler] System registered");
    }

    @Override
    public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk,
                       Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer,
                       KillFeedEvent.KillerMessage event) {
        Ref<EntityStore> killerRef = chunk.getReferenceTo(entityIndex);
        if (killerRef == null || !killerRef.isValid()) return;

        // Only award skill XP to players
        PlayerSkillData skills = store.getComponent(killerRef, skillType);
        if (skills == null) return;

        // Classify the killed mob by its nameplate text
        Ref<EntityStore> targetRef = event.getTargetRef();
        if (targetRef == null || !targetRef.isValid()) return;

        Nameplate nameplate = store.getComponent(targetRef, Nameplate.getComponentType());
        if (nameplate == null || nameplate.getText() == null) return;

        MobCategory category = MobCategory.classify(nameplate.getText());
        SkillType combatSkill = category.getSkillType();
        if (combatSkill == null) return; // UNKNOWN category

        int levelsGained = skills.addXp(combatSkill, StatConstants.COMBAT_SKILL_XP_PER_KILL);

        // Notify on level up
        if (levelsGained > 0) {
            PlayerRef playerRef = store.getComponent(killerRef, PlayerRef.getComponentType());
            if (playerRef != null) {
                NotificationUtil.sendNotification(playerRef.getPacketHandler(),
                        Message.raw(combatSkill.getDisplayName() + " level "
                                + skills.getLevel(combatSkill) + "!"));
            }
        }
    }
}
