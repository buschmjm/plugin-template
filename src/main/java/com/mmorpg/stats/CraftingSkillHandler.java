package com.mmorpg.stats;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.ecs.CraftRecipeEvent;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.hypixel.hytale.protocol.BenchRequirement;
import com.hypixel.hytale.protocol.BenchType;

import java.util.logging.Logger;

/**
 * Listens for CraftRecipeEvent.Post to award Crafting or Processing XP.
 * Skill is determined by the bench type used:
 *   - Processing bench -> Processing skill
 *   - All others (Crafting, DiagramCrafting, StructuralCrafting) -> Crafting skill
 */
public class CraftingSkillHandler extends EntityEventSystem<EntityStore, CraftRecipeEvent.Post> {

    private static final Logger LOGGER = Logger.getLogger("MMORPGStats");
    private static final Query<EntityStore> QUERY = Query.any();

    private final ComponentType<EntityStore, PlayerSkillData> skillType;

    public CraftingSkillHandler() {
        super(CraftRecipeEvent.Post.class);
        this.skillType = StatsPlugin.getInstance().getPlayerSkillDataType();
    }

    @Override
    public Query<EntityStore> getQuery() { return QUERY; }

    @Override
    public void onSystemRegistered() {
        LOGGER.info("[CraftingSkillHandler] System registered");
    }

    @Override
    public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk,
                       Store<EntityStore> store, CommandBuffer<EntityStore> cmdBuf,
                       CraftRecipeEvent.Post event) {

        Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
        if (ref == null || !ref.isValid()) return;

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return;

        PlayerSkillData skills = store.getComponent(ref, skillType);
        if (skills == null) return;

        // Determine skill from bench type
        SkillType skill = classifyCraft(event.getCraftedRecipe());

        // Award XP (quantity-scaled)
        int xpAmount = Math.max(1, event.getQuantity());
        int levelsGained = skills.addXp(skill, xpAmount);

        // Quest tracking: craft + profession XP
        QuestTracker.record(ref, store, QuestObjective.CRAFT_ITEMS, xpAmount);
        QuestTracker.record(ref, store, QuestObjective.EARN_PROFESSION_XP, xpAmount);

        if (levelsGained > 0) {
            NotificationUtil.sendNotification(playerRef.getPacketHandler(),
                    Message.raw(skill.getDisplayName() + " level " + skills.getLevel(skill) + "!"));
        }
    }

    /** Classify a craft into the appropriate skill based on bench requirements. */
    static SkillType classifyCraft(CraftingRecipe recipe) {
        if (recipe == null) return SkillType.CARPENTRY;
        BenchRequirement[] reqs = recipe.getBenchRequirement();
        if (reqs != null) {
            for (BenchRequirement req : reqs) {
                if (req.type == BenchType.Processing) {
                    return SkillType.PROCESSING;
                }
                if (req.type == BenchType.DiagramCrafting) {
                    return SkillType.SMITHING;
                }
                if (req.type == BenchType.StructuralCrafting) {
                    return SkillType.MASONRY;
                }
                // Match cooking and alchemy benches by name for forward compatibility
                String typeName = req.type.name();
                if (typeName.contains("Cook")) {
                    return SkillType.COOKING;
                }
                if (typeName.contains("Alch") || typeName.contains("Brew")) {
                    return SkillType.ALCHEMY;
                }
                // Match Ancient Constructs workbench
                String benchId = req.id;
                if (benchId != null && benchId.contains("Construct")) {
                    return SkillType.TINKERING;
                }
            }
        }
        return SkillType.CARPENTRY;
    }
}
