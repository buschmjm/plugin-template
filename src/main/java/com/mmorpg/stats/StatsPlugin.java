package com.mmorpg.stats;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

/**
 * Main plugin entry point for the MMORPG Stats mod.
 * Registers the persistent stat component, the /stats command, and
 * applies stat effects when players join.
 */
public class StatsPlugin extends JavaPlugin {

    private static final Logger LOGGER = Logger.getLogger("MMORPGStats");
    private static StatsPlugin instance;
    private ComponentType<EntityStore, PlayerStatData> playerStatDataType;
    private ComponentType<EntityStore, PlayerClassData> playerClassDataType;
    private ComponentType<EntityStore, PlayerSkillData> playerSkillDataType;
    private ComponentType<EntityStore, PlayerQuestData> playerQuestDataType;
    private ComponentType<EntityStore, PlayerGoldData> playerGoldDataType;
    private ComponentType<EntityStore, PlayerEquipmentData> playerEquipmentDataType;
    private ComponentType<EntityStore, ClaimData> claimDataType;
    private ComponentType<EntityStore, GuildData> guildDataType;
    private ComponentType<EntityStore, PlayerAbilityBindData> abilityBindDataType;
    private volatile boolean claimMarkersRegistered = false;

    public StatsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        // Register persistent ECS component for player stat data
        this.playerStatDataType = this.getEntityStoreRegistry().registerComponent(
                PlayerStatData.class,
                "MMORPGPlayerStatData",
                PlayerStatData.CODEC
        );

        // Register class title data component (persisted)
        this.playerClassDataType = this.getEntityStoreRegistry().registerComponent(
                PlayerClassData.class,
                "MMORPGPlayerClassData",
                PlayerClassData.CODEC
        );

        // Register skill/profession data component (persisted)
        this.playerSkillDataType = this.getEntityStoreRegistry().registerComponent(
                PlayerSkillData.class,
                "MMORPGPlayerSkillData",
                PlayerSkillData.CODEC
        );

        // Register quest data component (persisted)
        this.playerQuestDataType = this.getEntityStoreRegistry().registerComponent(
                PlayerQuestData.class,
                "MMORPGPlayerQuestData",
                PlayerQuestData.CODEC
        );

        // Register gold data component (persisted)
        this.playerGoldDataType = this.getEntityStoreRegistry().registerComponent(
                PlayerGoldData.class,
                "MMORPGPlayerGoldData",
                PlayerGoldData.CODEC
        );

        // Register equipment data component (persisted)
        this.playerEquipmentDataType = this.getEntityStoreRegistry().registerComponent(
                PlayerEquipmentData.class,
                "MMORPGPlayerEquipmentData",
                PlayerEquipmentData.CODEC
        );

        // Register claim data component (persisted)
        this.claimDataType = this.getEntityStoreRegistry().registerComponent(
                ClaimData.class,
                "MMORPGClaimData",
                ClaimData.CODEC
        );

        // Register guild data component (persisted)
        this.guildDataType = this.getEntityStoreRegistry().registerComponent(
                GuildData.class,
                "MMORPGGuildData",
                GuildData.CODEC
        );

        // Register ability bind data component (persisted)
        this.abilityBindDataType = this.getEntityStoreRegistry().registerComponent(
                PlayerAbilityBindData.class,
                "MMORPGAbilityBindData",
                PlayerAbilityBindData.CODEC
        );

        // Buff and ability cooldown data are stored in TransientDataStore (not ECS)
        // to reduce ECS component count and memory overhead

        // Register commands
        this.getCommandRegistry().registerCommand(new StatsCommand());
        this.getCommandRegistry().registerCommand(new AdminCommand());
        this.getCommandRegistry().registerCommand(new AbilityCommand());
        this.getCommandRegistry().registerCommand(new ClassCommand());
        this.getCommandRegistry().registerCommand(new SkillCommand());
        this.getCommandRegistry().registerCommand(new HelpCommand());
        this.getCommandRegistry().registerCommand(new QuestCommand());
        this.getCommandRegistry().registerCommand(new GoldCommand());
        this.getCommandRegistry().registerCommand(new ShopCommand());
        this.getCommandRegistry().registerCommand(new SpawnShopCommand());
        this.getCommandRegistry().registerCommand(new LeaderboardCommand());
        this.getCommandRegistry().registerCommand(new TradeCommand());
        this.getCommandRegistry().registerCommand(new AllocateCommand());
        this.getCommandRegistry().registerCommand(new ClaimCommand());
        this.getCommandRegistry().registerCommand(new GuildCommand());
        this.getCommandRegistry().registerCommand(new OnlineCommand());
        this.getCommandRegistry().registerCommand(new GoldLogCommand());
        this.getCommandRegistry().registerCommand(new MenuCommand());
        this.getCommandRegistry().registerCommand(new CommandsCommand());

        // Register systems
        this.getEntityStoreRegistry().registerSystem(new DamageHandler());
        this.getEntityStoreRegistry().registerSystem(new KillXpHandler());
        this.getEntityStoreRegistry().registerSystem(new DeathPenaltyHandler());
        this.getEntityStoreRegistry().registerSystem(new BuffTickSystem());
        this.getEntityStoreRegistry().registerSystem(new GatheringSkillHandler());
        this.getEntityStoreRegistry().registerSystem(new CraftingSkillHandler());
        this.getEntityStoreRegistry().registerSystem(new KillSkillHandler());
        this.getEntityStoreRegistry().registerSystem(new ClaimProtectionSystems.BreakProtection());
        this.getEntityStoreRegistry().registerSystem(new ClaimProtectionSystems.PlaceProtection());
        this.getEntityStoreRegistry().registerSystem(new ClaimProtectionSystems.DamageBlockProtection());
        this.getEntityStoreRegistry().registerSystem(new ClaimProtectionSystems.MmorpgItemPlaceGuard());

        // Prune old gold ledger entries on startup
        GoldLedger.prune();

        // Load persisted daily rewards
        SessionTracker.loadDailyRewards();

        // Register custom interaction for ability keybinds (Q/E/R)
        this.getCodecRegistry(Interaction.CODEC).register(
                "mmorpg_ability_cast", AbilityCastInteraction.class, AbilityCastInteraction.CODEC);

        // Register starter abilities
        registerAbilities();

        // When a player is ready, ensure their components exist, apply effects, and show HUD
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, event -> {
            var player = event.getPlayer();
            var ref = player.getReference();
            if (ref == null || !ref.isValid()) return;

            var store = ref.getStore();
            var world = store.getExternalData().getWorld();
            world.execute(() -> {
                try {
                    PlayerStatData stats = store.ensureAndGetComponent(ref, playerStatDataType);

                    // Debug mode: grant high level (points to spend) but don't auto-allocate stats
                    if (StatConstants.DEBUG_MAX_STATS) {
                        stats.setLevel(StatConstants.DEBUG_LEVEL);
                    }

                    store.putComponent(ref, playerStatDataType, stats);
                    StatEffectApplier.applyStats(ref, store, stats);

                    // Ensure class and skill data exists
                    store.ensureAndGetComponent(ref, playerClassDataType);
                    PlayerSkillData skillData = store.ensureAndGetComponent(ref, playerSkillDataType);

                    // Debug mode: start with elevated skill levels for testing
                    if (StatConstants.DEBUG_MAX_STATS) {
                        for (SkillType skill : SkillType.values()) {
                            skillData.setLevel(skill, StatConstants.DEBUG_SKILL_LEVEL);
                        }
                    }

                    ClassEvaluator.reevaluateAfterRespec(ref, store, stats);

                    // Ensure quest data exists and is current week
                    PlayerQuestData questData = store.ensureAndGetComponent(ref, playerQuestDataType);
                    questData.ensureCurrentWeek();

                    // Ensure gold data exists; give starting gold to new players
                    PlayerGoldData goldData = store.ensureAndGetComponent(ref, playerGoldDataType);

                    // Ensure equipment data exists
                    PlayerEquipmentData equipData = store.ensureAndGetComponent(ref, playerEquipmentDataType);

                    // Ensure claim data exists and rebuild claim registry
                    ClaimData claimData = store.ensureAndGetComponent(ref, claimDataType);
                    PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());

                    if (goldData.getGold() == 0 && StatConstants.STARTING_GOLD > 0) {
                        goldData.setGold(StatConstants.STARTING_GOLD);
                        GoldLedger.log(pRef != null ? pRef.getUsername() : "unknown",
                                "STARTING", StatConstants.STARTING_GOLD,
                                goldData.getGold(), "new player");
                    }

                    if (pRef != null) {
                        String uuid = pRef.getUuid().toString();
                        for (String chunkKey : claimData.getClaimedChunkSet()) {
                            String[] parts = chunkKey.split(":");
                            if (parts.length == 2) {
                                ClaimRegistry.register(
                                        Integer.parseInt(parts[0]),
                                        Integer.parseInt(parts[1]), uuid);
                            }
                        }

                        // Cache claim color and player name for map markers
                        ClaimRegistry.setOwnerColor(uuid, claimData.getClaimColorRGB());
                        ClaimRegistry.setOwnerName(uuid, pRef.getUsername());

                        // Ensure guild data exists and rebuild guild registry
                        GuildData gData = store.ensureAndGetComponent(ref, guildDataType);
                        if (gData.isInGuild()) {
                            String guildName = gData.getGuildName();
                            if (!GuildRegistry.guildExists(guildName)) {
                                GuildRegistry.createGuild(guildName, uuid);
                            } else {
                                GuildRegistry.addMember(guildName, uuid);
                            }
                            ClaimRegistry.setPlayerGuild(uuid, guildName);
                        }
                    }

                    // Load persisted ability binds into transient store
                    PlayerAbilityBindData bindData = store.ensureAndGetComponent(ref, abilityBindDataType);
                    PlayerAbilityData abilityData = TransientDataStore.getAbilities(ref);
                    bindData.loadInto(abilityData);

                    // Register claim map markers (once, on first player join)
                    if (!claimMarkersRegistered) {
                        claimMarkersRegistered = true;
                        world.getWorldMapManager().addMarkerProvider(
                                "mmorpg_claims", ClaimMarkerProvider.INSTANCE);
                    }

                    // Apply exploration-based map view radius
                    com.hypixel.hytale.server.core.entity.entities.Player playerEntity =
                            store.getComponent(ref, com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
                    if (playerEntity != null && playerEntity.getWorldMapTracker() != null) {
                        playerEntity.getWorldMapTracker().setViewRadiusOverride(skillData.getMapRadius());

                        // Re-send previously explored chunk images so the map persists
                        var wmm = world.getWorldMapManager();
                        var mapPR = store.getComponent(ref, PlayerRef.getComponentType());
                        var mapPacketHandler = mapPR != null ? mapPR.getPacketHandler() : null;
                        for (long packed : skillData.getExploredChunks()) {
                            int cx = PlayerSkillData.unpackChunkX(packed);
                            int cz = PlayerSkillData.unpackChunkZ(packed);
                            wmm.getImageAsync(cx, cz).thenAccept(img -> {
                                if (img != null && mapPacketHandler != null) {
                                    var chunk = new com.hypixel.hytale.protocol.packets.worldmap.MapChunk(cx, cz, img);
                                    var pkt = new com.hypixel.hytale.protocol.packets.worldmap.UpdateWorldMap(
                                            new com.hypixel.hytale.protocol.packets.worldmap.MapChunk[]{chunk},
                                            null, null);
                                    mapPacketHandler.write(pkt);
                                }
                            });
                        }
                    }

                    // Restore offhand item if owned - place in utility (offhand) slot
                    if (equipData.hasOffhand()) {
                        com.hypixel.hytale.server.core.entity.entities.Player pEntity =
                                store.getComponent(ref, com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
                        if (pEntity != null) {
                            ItemStack offhandStack = new ItemStack(equipData.getOwnedOffhand());
                            var utilityContainer = pEntity.getInventory().getUtility();
                            utilityContainer.removeItemStackFromSlot((short) 0);
                            utilityContainer.setItemStackForSlot((short) 0, offhandStack);
                            pEntity.getInventory().setActiveUtilitySlot(ref, (byte) 0, store);
                        }
                    }

                    // Set up persistent on-screen HUD
                    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                    if (playerRef != null) {

                        // Enable HUD
                        playerEntity.getHudManager().setCustomHud(playerRef, new CombatHud(playerRef));

                        String uuid = playerRef.getUuid().toString();

                        // Track session for /online playtime
                        SessionTracker.recordLogin(uuid);

                        // Daily login reward
                        if (SessionTracker.claimDailyReward(uuid)) {
                            goldData.addGold(SessionTracker.DAILY_REWARD_GOLD);
                            playerRef.sendMessage(Message.raw(
                                    "* Daily login reward: +" + SessionTracker.DAILY_REWARD_GOLD + " gold!"));
                            NotificationUtil.sendNotification(
                                    playerRef.getPacketHandler(),
                                    Message.raw("Daily Login Reward!"),
                                    Message.raw("+" + SessionTracker.DAILY_REWARD_GOLD + " gold added to your balance"),
                                    NotificationStyle.Success);
                            GoldLedger.log(playerRef.getUsername(), "DAILY_LOGIN",
                                    SessionTracker.DAILY_REWARD_GOLD, goldData.getGold(),
                                    "daily reward");
                        }

                        // Set player nametag showing class and guild
                        updatePlayerNametag(ref, store, playerRef);

                        // Send welcome message with available commands
                        sendWelcomeMessage(playerRef);
                    }
                } catch (Exception e) {
                    LOGGER.severe("[MMORPG] Error initializing player: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        });

        // Clean up transient data when a player disconnects (prevents OOM leak)
        this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, event -> {
            PlayerRef dRef = event.getPlayerRef();
            if (dRef == null) return;

            String uuid = dRef.getUuid().toString();
            SessionTracker.recordLogout(uuid);
            TradeCommand.removeFor(uuid);

            Ref<EntityStore> entityRef = dRef.getReference();
            if (entityRef != null && entityRef.isValid()) {
                TransientDataStore.remove(entityRef);
            }

            TradeCommand.cleanupExpired();
        });

        // Ability keybinds: Q/E/R are handled by AbilityCastInteraction
        // registered above via the interaction system (no event handler needed).
    }

    /** Update a player's nameplate to show class and guild. */
    void updatePlayerNametag(Ref<EntityStore> ref, Store<EntityStore> store,
                             PlayerRef playerRef) {
        try {
            Nameplate nameplate = store.ensureAndGetComponent(
                    ref, Nameplate.getComponentType());

            String name = playerRef.getUsername();

            // Build display: [Class] Name <Guild>
            StringBuilder display = new StringBuilder();

            PlayerClassData classData = store.getComponent(ref, playerClassDataType);
            if (classData != null) {
                String title = classData.getDisplayedTitleName();
                if (title != null && !title.isEmpty()) {
                    display.append("[").append(title).append("] ");
                }
            }

            display.append(name);

            GuildData guildData = store.getComponent(ref, guildDataType);
            if (guildData != null && guildData.isInGuild()) {
                display.append(" <").append(guildData.getGuildName()).append(">");
            }

            nameplate.setText(display.toString());
        } catch (Exception e) {
            LOGGER.warning("[MMORPG] Failed to set player nametag: " + e.getMessage());
        }
    }

    /** Send welcome message listing all available commands. */
    private void sendWelcomeMessage(PlayerRef playerRef) {
        playerRef.sendMessage(Message.raw("==============================="));
        playerRef.sendMessage(Message.raw("  Welcome to Hytale MMORPG!"));
        playerRef.sendMessage(Message.raw("  Use /menu to open the main UI."));
        playerRef.sendMessage(Message.raw("  Use /commands to view all server commands."));
        playerRef.sendMessage(Message.raw("==============================="));
    }
    /** Register all built-in abilities. 4 per stat, unlocked at 5/15/30/50 points.
     *  Each class: Damage, Movement, Utility, Class Wildcard. */
    private void registerAbilities() {
        // == STR Abilities (Knight - raw power) ==
        // Damage: big damage buff
        AbilityRegistry.register(
                AbilityDefinition.builder("war_cry", "War Cry")
                        .description("+35% damage for 12s")
                        .cooldown(40)
                        .requires("STR", 5)
                        .selfBuff(BuffInstance.builder("war_cry", "War Cry", BuffType.STAT_MODIFIER)
                                .duration(12).bonusDamage(0.35f).build())
                        .build());
        // Empowered strikes: strength + toughness combo
        AbilityRegistry.register(
                AbilityDefinition.builder("iron_fist", "Iron Fist")
                        .description("+20% damage, +30 HP for 12s")
                        .cooldown(30)
                        .requires("STR", 15)
                        .selfBuff(BuffInstance.builder("iron_fist", "Iron Fist", BuffType.STAT_MODIFIER)
                                .duration(12).bonusDamage(0.20f)
                                .statModifier(DefaultEntityStatTypes.getHealth(), 30f).build())
                        .build());
        // Utility: defensive HP shield
        AbilityRegistry.register(
                AbilityDefinition.builder("iron_skin", "Iron Skin")
                        .description("+50 max HP for 20s")
                        .cooldown(50)
                        .requires("STR", 30)
                        .selfBuff(BuffInstance.builder("iron_skin", "Iron Skin", BuffType.STAT_MODIFIER)
                                .duration(20).statModifier(DefaultEntityStatTypes.getHealth(), 50f).build())
                        .build());
        // Wildcard (damage): sacrifice HP for massive damage
        AbilityRegistry.register(
                AbilityDefinition.builder("berserker_rage", "Berserker Rage")
                        .description("Sacrifice 20 HP, +50% damage for 8s")
                        .cooldown(45).selfDamage(20)
                        .requires("STR", 50)
                        .selfBuff(BuffInstance.builder("berserker_rage", "Berserker Rage", BuffType.STAT_MODIFIER)
                                .duration(8).bonusDamage(0.50f).build())
                        .build());

        // == DEX Abilities (Rogue - speed & precision) ==
        // Damage: quick burst damage
        AbilityRegistry.register(
                AbilityDefinition.builder("swift_strikes", "Swift Strikes")
                        .description("+15% damage for 10s")
                        .cooldown(20)
                        .requires("DEX", 5)
                        .selfBuff(BuffInstance.builder("swift_strikes", "Swift Strikes", BuffType.STAT_MODIFIER)
                                .duration(10).bonusDamage(0.15f).build())
                        .build());
        // Movement: shadow teleport stamina
        AbilityRegistry.register(
                AbilityDefinition.builder("shadow_step", "Shadow Step")
                        .description("+100 stamina for 12s")
                        .cooldown(35)
                        .requires("DEX", 15)
                        .selfBuff(BuffInstance.builder("shadow_step", "Shadow Step", BuffType.STAT_MODIFIER)
                                .duration(12).statModifier(DefaultEntityStatTypes.getStamina(), 100f).build())
                        .build());
        // Utility: evasion blend of offense/defense
        AbilityRegistry.register(
                AbilityDefinition.builder("evasion", "Evasion")
                        .description("+50 stamina, +15% damage for 10s")
                        .cooldown(35)
                        .requires("DEX", 30)
                        .selfBuff(BuffInstance.builder("evasion", "Evasion", BuffType.STAT_MODIFIER)
                                .duration(10).statModifier(DefaultEntityStatTypes.getStamina(), 50f)
                                .bonusDamage(0.15f).build())
                        .build());
        // Wildcard (movement): ultimate sprint with damage
        AbilityRegistry.register(
                AbilityDefinition.builder("phantom_dash", "Phantom Dash")
                        .description("+120 stamina, +20% damage for 10s")
                        .cooldown(55)
                        .requires("DEX", 50)
                        .selfBuff(BuffInstance.builder("phantom_dash", "Phantom Dash", BuffType.STAT_MODIFIER)
                                .duration(10).statModifier(DefaultEntityStatTypes.getStamina(), 120f)
                                .bonusDamage(0.20f).build())
                        .build());

        // == VIT Abilities (Brawler - endurance & survival) ==
        // Stamina surge: endurance-driven charge
        AbilityRegistry.register(
                AbilityDefinition.builder("bull_rush", "Bull Rush")
                        .description("+80 stamina for 12s")
                        .cooldown(30)
                        .requires("VIT", 5)
                        .selfBuff(BuffInstance.builder("bull_rush", "Bull Rush", BuffType.STAT_MODIFIER)
                                .duration(12).statModifier(DefaultEntityStatTypes.getStamina(), 80f).build())
                        .build());
        // Movement: endurance stamina surge
        AbilityRegistry.register(
                AbilityDefinition.builder("endurance", "Endurance")
                        .description("+80 stamina for 15s")
                        .cooldown(35)
                        .requires("VIT", 15)
                        .selfBuff(BuffInstance.builder("endurance", "Endurance", BuffType.STAT_MODIFIER)
                                .duration(15).statModifier(DefaultEntityStatTypes.getStamina(), 80f).build())
                        .build());
        // Utility: heal over time
        AbilityRegistry.register(
                AbilityDefinition.builder("second_wind", "Second Wind")
                        .description("Heal 5 HP/s for 10s")
                        .cooldown(45)
                        .requires("VIT", 30)
                        .selfBuff(BuffInstance.builder("second_wind", "Second Wind", BuffType.HEAL_OVER_TIME)
                                .duration(10).tickInterval(1).tickAmount(5).build())
                        .build());
        // Wildcard (utility): ultimate tank - massive HP + regen
        AbilityRegistry.register(
                AbilityDefinition.builder("living_bastion", "Living Bastion")
                        .description("+100 HP, heal 3 HP/s for 12s")
                        .cooldown(60)
                        .requires("VIT", 50)
                        .selfBuff(BuffInstance.builder("living_bastion", "Living Bastion", BuffType.HEAL_OVER_TIME)
                                .duration(12).tickInterval(1).tickAmount(3)
                                .statModifier(DefaultEntityStatTypes.getHealth(), 100f).build())
                        .build());

        // == INT Abilities (Wizard - arcane power) ==
        // Damage: magic damage spike
        AbilityRegistry.register(
                AbilityDefinition.builder("mind_spike", "Mind Spike")
                        .description("+25% damage for 10s")
                        .cooldown(30).manaCost(20)
                        .requires("INT", 5)
                        .selfBuff(BuffInstance.builder("mind_spike", "Mind Spike", BuffType.STAT_MODIFIER)
                                .duration(10).bonusDamage(0.25f).build())
                        .build());
        // Movement: arcane-powered sprint
        AbilityRegistry.register(
                AbilityDefinition.builder("arcane_step", "Arcane Step")
                        .description("+80 stamina, +40 mana for 12s")
                        .cooldown(35).manaCost(10)
                        .requires("INT", 15)
                        .selfBuff(BuffInstance.builder("arcane_step", "Arcane Step", BuffType.STAT_MODIFIER)
                                .duration(12).statModifier(DefaultEntityStatTypes.getStamina(), 80f)
                                .statModifier(DefaultEntityStatTypes.getMana(), 40f).build())
                        .build());
        // Utility: mana + HP shield
        AbilityRegistry.register(
                AbilityDefinition.builder("mana_shield", "Mana Shield")
                        .description("+60 mana, +30 HP for 12s")
                        .cooldown(45).manaCost(20)
                        .requires("INT", 30)
                        .selfBuff(BuffInstance.builder("mana_shield", "Mana Shield", BuffType.STAT_MODIFIER)
                                .duration(12).statModifier(DefaultEntityStatTypes.getMana(), 60f)
                                .statModifier(DefaultEntityStatTypes.getHealth(), 30f).build())
                        .build());
        // Wildcard (damage): ultimate spell power
        AbilityRegistry.register(
                AbilityDefinition.builder("spell_surge", "Spell Surge")
                        .description("+45% damage, +80 mana for 10s")
                        .cooldown(55).manaCost(30)
                        .requires("INT", 50)
                        .selfBuff(BuffInstance.builder("spell_surge", "Spell Surge", BuffType.STAT_MODIFIER)
                                .duration(10).bonusDamage(0.45f)
                                .statModifier(DefaultEntityStatTypes.getMana(), 80f).build())
                        .build());

        // == PRE Abilities (Commander - leadership & support) ==
        // Damage: inspired attack
        AbilityRegistry.register(
                AbilityDefinition.builder("inspire", "Inspire")
                        .description("Heal 20 HP, +20% damage for 12s")
                        .cooldown(40)
                        .requires("PRE", 5)
                        .instantHeal(20)
                        .selfBuff(BuffInstance.builder("inspire", "Inspire", BuffType.STAT_MODIFIER)
                                .duration(12).bonusDamage(0.20f).build())
                        .build());
        // Movement: rallying charge
        AbilityRegistry.register(
                AbilityDefinition.builder("rally", "Rally")
                        .description("Heal 15 HP, +80 stamina for 15s")
                        .cooldown(40)
                        .requires("PRE", 15)
                        .instantHeal(15)
                        .selfBuff(BuffInstance.builder("rally", "Rally", BuffType.STAT_MODIFIER)
                                .duration(15).statModifier(DefaultEntityStatTypes.getStamina(), 80f).build())
                        .build());
        // Utility: defensive banner
        AbilityRegistry.register(
                AbilityDefinition.builder("battle_standard", "Battle Standard")
                        .description("+25% damage, +40 HP for 15s")
                        .cooldown(50)
                        .requires("PRE", 30)
                        .selfBuff(BuffInstance.builder("battle_standard", "Battle Standard", BuffType.STAT_MODIFIER)
                                .duration(15).bonusDamage(0.25f)
                                .statModifier(DefaultEntityStatTypes.getHealth(), 40f).build())
                        .build());
        // Wildcard (utility): ultimate commander buff
        AbilityRegistry.register(
                AbilityDefinition.builder("commanders_aura", "Commander's Aura")
                        .description("Heal 20 HP, +20% damage, +40 stamina for 12s")
                        .cooldown(60)
                        .requires("PRE", 50)
                        .instantHeal(20)
                        .selfBuff(BuffInstance.builder("commanders_aura", "Commander's Aura", BuffType.STAT_MODIFIER)
                                .duration(12).bonusDamage(0.20f)
                                .statModifier(DefaultEntityStatTypes.getStamina(), 40f).build())
                        .build());

        // == ARC Abilities (Druid - nature & healing) ==
        // Damage: sacrifice HP for mana (offensive resource)
        AbilityRegistry.register(
                AbilityDefinition.builder("life_tap", "Life Tap")
                        .description("+40 mana for 8s")
                        .cooldown(25)
                        .requires("ARC", 5)
                        .selfBuff(BuffInstance.builder("life_tap", "Life Tap", BuffType.STAT_MODIFIER)
                                .duration(8).statModifier(DefaultEntityStatTypes.getMana(), 40f).build())
                        .build());
        // Movement: nature sprint
        AbilityRegistry.register(
                AbilityDefinition.builder("natures_sprint", "Nature's Sprint")
                        .description("+80 stamina for 12s")
                        .cooldown(30).manaCost(15)
                        .requires("ARC", 15)
                        .selfBuff(BuffInstance.builder("natures_sprint", "Nature's Sprint", BuffType.STAT_MODIFIER)
                                .duration(12).statModifier(DefaultEntityStatTypes.getStamina(), 80f).build())
                        .build());
        // Utility: instant burst heal
        AbilityRegistry.register(
                AbilityDefinition.builder("quick_recovery", "Quick Recovery")
                        .description("Instantly restore 30 HP")
                        .cooldown(20).manaCost(20)
                        .requires("ARC", 30)
                        .instantHeal(30)
                        .build());
        // Wildcard (utility): ultimate heal + mana restore
        AbilityRegistry.register(
                AbilityDefinition.builder("natures_blessing", "Nature's Blessing")
                        .description("Heal 40 HP, +60 mana for 15s")
                        .cooldown(55).manaCost(25)
                        .requires("ARC", 50)
                        .instantHeal(40)
                        .selfBuff(BuffInstance.builder("natures_blessing", "Nature's Blessing", BuffType.STAT_MODIFIER)
                                .duration(15).statModifier(DefaultEntityStatTypes.getMana(), 60f).build())
                        .build());
    }

    public static StatsPlugin getInstance() {
        return instance;
    }

    public ComponentType<EntityStore, PlayerStatData> getPlayerStatDataType() {
        return playerStatDataType;
    }

    public ComponentType<EntityStore, PlayerClassData> getPlayerClassDataType() {
        return playerClassDataType;
    }

    public ComponentType<EntityStore, PlayerSkillData> getPlayerSkillDataType() {
        return playerSkillDataType;
    }

    public ComponentType<EntityStore, PlayerQuestData> getPlayerQuestDataType() {
        return playerQuestDataType;
    }

    public ComponentType<EntityStore, PlayerGoldData> getPlayerGoldDataType() {
        return playerGoldDataType;
    }

    public ComponentType<EntityStore, PlayerEquipmentData> getPlayerEquipmentDataType() {
        return playerEquipmentDataType;
    }

    public ComponentType<EntityStore, ClaimData> getClaimDataType() {
        return claimDataType;
    }

    public ComponentType<EntityStore, GuildData> getGuildDataType() {
        return guildDataType;
    }

    public ComponentType<EntityStore, PlayerAbilityBindData> getAbilityBindDataType() {
        return abilityBindDataType;
    }

    /** Persist current ability binds from transient data to ECS. */
    public static void persistAbilityBinds(Ref<EntityStore> ref, Store<EntityStore> store) {
        PlayerAbilityData abilityData = TransientDataStore.getAbilities(ref);
        var bindType = getInstance().getAbilityBindDataType();
        PlayerAbilityBindData bindData = store.ensureAndGetComponent(ref, bindType);
        bindData.copyFrom(abilityData);
        store.putComponent(ref, bindType, bindData);
    }

    /**
     * Check if a player just reached 5 points in any stat and grant them
     * the corresponding offhand gemstone for free (one-time per stat).
     * Call after stat allocation.
     */
    public static void checkFirstClassUnlock(Ref<EntityStore> ref, Store<EntityStore> store,
                                              PlayerStatData stats) {
        var equipType = getInstance().getPlayerEquipmentDataType();
        PlayerEquipmentData equipData = store.ensureAndGetComponent(ref, equipType);

        // Only grant if the player doesn't already own an offhand
        if (equipData.hasOffhand()) return;

        // Check each stat for the 5-point threshold
        String matchedStat = null;
        if (stats.getStrength() >= 5) matchedStat = "STR";
        else if (stats.getDexterity() >= 5) matchedStat = "DEX";
        else if (stats.getVitality() >= 5) matchedStat = "VIT";
        else if (stats.getIntellect() >= 5) matchedStat = "INT";
        else if (stats.getPresence() >= 5) matchedStat = "PRE";
        else if (stats.getArcana() >= 5) matchedStat = "ARC";

        if (matchedStat == null) return;

        // Find the matching offhand item
        OffhandItem offhand = null;
        for (OffhandItem item : OffhandItem.values()) {
            if (item.getPrimaryStat().equals(matchedStat)) {
                offhand = item;
                break;
            }
        }
        if (offhand == null) return;

        // Grant it
        equipData.setOwnedOffhand(offhand.getGameItemId());
        store.putComponent(ref, equipType, equipData);

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            ItemStack stack = new ItemStack(offhand.getGameItemId());
            var utilityContainer = player.getInventory().getUtility();
            utilityContainer.removeItemStackFromSlot((short) 0);
            utilityContainer.setItemStackForSlot((short) 0, stack);
            player.getInventory().setActiveUtilitySlot(ref, (byte) 0, store);
        }

        PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (pRef != null) {
            pRef.sendMessage(Message.raw("* Class unlocked! You received a free "
                    + offhand.getDisplayName() + "!"));
            pRef.sendMessage(Message.raw("It's in your offhand slot. Use /skill to bind abilities."));
            GoldLedger.log(pRef.getUsername(), "CLASS_UNLOCK_REWARD",
                    0, 0, offhand.getDisplayName());
        }
    }
}
