package com.mmorpg.stats;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemTool;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemToolSpec;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;

import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * Listens for BreakBlockEvent to award Mining, Woodcutting, and Farming XP.
 * Also applies durability-save rolls based on skill level.
 *
 * Skill detection logic:
 * - Mining: tool gather type contains "pick"
 * - Woodcutting: tool gather type contains "axe"
 * - Farming: block has farming data (isHarvestable), or block type id contains
 *   common crop/plant keywords
 */
public class GatheringSkillHandler extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    private static final Logger LOGGER = Logger.getLogger("MMORPGStats");
    private static final Query<EntityStore> QUERY = Query.any();

    private final ComponentType<EntityStore, PlayerSkillData> skillType;

    public GatheringSkillHandler() {
        super(BreakBlockEvent.class);
        this.skillType = StatsPlugin.getInstance().getPlayerSkillDataType();
    }

    @Override
    public Query<EntityStore> getQuery() { return QUERY; }

    @Override
    public void onSystemRegistered() {
        LOGGER.info("[GatheringSkillHandler] System registered");
    }

    @Override
    public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk,
                       Store<EntityStore> store, CommandBuffer<EntityStore> cmdBuf,
                       BreakBlockEvent event) {

        Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
        if (ref == null || !ref.isValid()) return;

        // Only players earn skill XP
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return;

        PlayerSkillData skills = store.getComponent(ref, skillType);
        if (skills == null) return;

        // Determine which skill this block break maps to
        SkillType skill = classifyBreak(event);
        if (skill == null) return;

        // Award XP based on block value (ore > normal blocks)
        int xpAmount = getBlockXpValue(event.getBlockType());
        int levelsGained = skills.addXp(skill, xpAmount);

        // Quest tracking: gather + profession XP
        QuestTracker.record(ref, store, QuestObjective.GATHER_BLOCKS);
        QuestTracker.record(ref, store, QuestObjective.EARN_PROFESSION_XP);

        // Notify on level up
        if (levelsGained > 0) {
            NotificationUtil.sendNotification(playerRef.getPacketHandler(),
                    Message.raw(skill.getDisplayName() + " level " + skills.getLevel(skill) + "!"));
        }

        // Durability save roll
        float saveChance = skills.getDurabilitySaveChance(skill);
        if (saveChance > 0 && ThreadLocalRandom.current().nextFloat() < saveChance) {
            ItemStack tool = event.getItemInHand();
            if (tool != null && tool.getMaxDurability() > 0) {
                // Restore 1 durability to negate the loss that just happened
                event.getItemInHand().withIncreasedDurability(1);
            }
        }
    }

    /**
     * Determine XP value based on block type.
     * Rare ores give 10 XP, common ores give 5 XP, normal blocks give 1 XP.
     */
    static int getBlockXpValue(BlockType blockType) {
        if (blockType == null) return StatConstants.NORMAL_BLOCK_XP;
        String id = String.valueOf(blockType.getId()).toLowerCase();

        // Rare ores
        if (id.contains("diamond") || id.contains("emerald") || id.contains("gold")
                || id.contains("mythril") || id.contains("adamant") || id.contains("ancient")) {
            return StatConstants.RARE_ORE_BLOCK_XP;
        }
        // Common ores
        if (id.contains("ore") || id.contains("coal") || id.contains("iron")
                || id.contains("copper") || id.contains("tin") || id.contains("silver")
                || id.contains("crystal")) {
            return StatConstants.ORE_BLOCK_XP;
        }
        return StatConstants.NORMAL_BLOCK_XP;
    }

    /**
     * Classify a block break into a skill type based on tool and block data.
     * Returns null if no skill applies.
     */
    static SkillType classifyBreak(BreakBlockEvent event) {
        BlockType blockType = event.getBlockType();

        // Check tool gather type first - hoe/sickle = Farming (tilling/watering)
        ItemStack handItem = event.getItemInHand();
        String toolGatherType = null;
        if (handItem != null) {
            Item item = handItem.getItem();
            if (item != null) {
                ItemTool tool = item.getTool();
                if (tool != null) {
                    ItemToolSpec[] specs = tool.getSpecs();
                    if (specs != null) {
                        for (ItemToolSpec spec : specs) {
                            String gt = spec.getGatherType();
                            if (gt != null) {
                                toolGatherType = gt.toLowerCase();
                                break;
                            }
                        }
                    }
                }
            }
        }

        // Hoe/sickle tool -> Farming (tilling, watering can usage)
        if (toolGatherType != null && (toolGatherType.contains("hoe") || toolGatherType.contains("sickle"))) {
            return SkillType.FARMING;
        }

        // Harvestable blocks (crops, plants) -> Herbalism
        if (blockType != null) {
            if (blockType.getFarming() != null) {
                return SkillType.HERBALISM;
            }
            var gathering = blockType.getGathering();
            if (gathering != null && gathering.isHarvestable()) {
                return SkillType.HERBALISM;
            }
        }

        // Other tool types
        if (toolGatherType != null) {
            if (toolGatherType.contains("pick")) return SkillType.MINING;
            if (toolGatherType.contains("axe")) return SkillType.WOODCUTTING;
        }

        return null;
    }
}
