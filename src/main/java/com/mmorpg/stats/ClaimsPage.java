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
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ClaimsPage extends InteractiveCustomUIPage<ClaimsPage.ClaimAction> {

    private static final List<String> COLOR_LIST = new ArrayList<>(ClaimData.getColorNames());

    public ClaimsPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, ClaimAction.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("ClaimsPage.ui");

        ClaimData claimData = store.getComponent(ref,
                StatsPlugin.getInstance().getClaimDataType());

        if (claimData == null) {
            cmd.set("#ClaimCount.TextSpans", Message.raw("Claims: 0 / " + ClaimCommand.BASE_MAX_CLAIMS));
            bindAllEvents(events);
            return;
        }

        populateDisplay(cmd, claimData);
        bindAllEvents(events);
    }

    private void populateDisplay(UICommandBuilder cmd, ClaimData claimData) {
        Set<String> chunks = claimData.getClaimedChunkSet();
        cmd.set("#ClaimCount.TextSpans", Message.raw(
                "Claims: " + chunks.size() + " / " + ClaimCommand.BASE_MAX_CLAIMS));

        // Color display
        cmd.set("#ColorName.TextSpans", Message.raw(claimData.getClaimColor()));

        // Flag toggle buttons - show ON/OFF state
        setFlagButton(cmd, "#BtnBreak", claimData.hasFlag(ClaimData.FLAG_BREAK_BLOCKS));
        setFlagButton(cmd, "#BtnPlace", claimData.hasFlag(ClaimData.FLAG_PLACE_BLOCKS));
        setFlagButton(cmd, "#BtnDoors", claimData.hasFlag(ClaimData.FLAG_OPEN_DOORS));
        setFlagButton(cmd, "#BtnContainers", claimData.hasFlag(ClaimData.FLAG_OPEN_CONTAINERS));
        setFlagButton(cmd, "#BtnPvp", claimData.hasFlag(ClaimData.FLAG_PVP));
        setFlagButton(cmd, "#BtnMobs", claimData.hasFlag(ClaimData.FLAG_MOB_DAMAGE));

        // List claimed chunks (5 visible)
        List<String> chunkList = new ArrayList<>(chunks);
        for (int i = 0; i < 5; i++) {
            String label = "#Claim" + (i + 1);
            if (i < chunkList.size()) {
                String chunk = chunkList.get(i);
                String[] parts = chunk.split(":");
                if (parts.length == 2) {
                    int cx = Integer.parseInt(parts[0]);
                    int cz = Integer.parseInt(parts[1]);
                    cmd.set(label + ".TextSpans", Message.raw(
                            (i + 1) + ". Chunk [" + cx + ", " + cz + "]"
                                    + " (blocks " + (cx * 16) + ", " + (cz * 16) + ")"));
                } else {
                    cmd.set(label + ".TextSpans", Message.raw((i + 1) + ". " + chunk));
                }
            } else {
                cmd.set(label + ".TextSpans", Message.raw(""));
            }
        }
    }

    private void setFlagButton(UICommandBuilder cmd, String buttonId, boolean enabled) {
        cmd.set(buttonId + ".TextSpans", Message.raw(enabled ? "ON" : "OFF"));
    }

    private void bindAllEvents(UIEventBuilder events) {
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnBack", EventData.of("Action", "BACK"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnColorPrev", EventData.of("Action", "COLOR_PREV"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnColorNext", EventData.of("Action", "COLOR_NEXT"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnBreak", EventData.of("Action", "TOGGLE_BREAK"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnPlace", EventData.of("Action", "TOGGLE_PLACE"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnDoors", EventData.of("Action", "TOGGLE_DOORS"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnContainers", EventData.of("Action", "TOGGLE_CONTAINERS"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnPvp", EventData.of("Action", "TOGGLE_PVP"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnMobs", EventData.of("Action", "TOGGLE_MOBS"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BtnClaimChunk", EventData.of("Action", "CLAIM_CHUNK"), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store, @Nonnull ClaimAction data) {
        super.handleDataEvent(ref, store, data);
        if (data.action == null) return;

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());

        if ("BACK".equals(data.action)) {
            if (player != null && pRef != null) {
                player.getPageManager().openCustomPage(ref, store, new MainMenuPage(pRef));
            }
            return;
        }

        ClaimData claimData = store.getComponent(ref,
                StatsPlugin.getInstance().getClaimDataType());
        if (claimData == null) return;

        switch (data.action) {
            case "COLOR_PREV" -> cycleColor(claimData, -1, pRef);
            case "COLOR_NEXT" -> cycleColor(claimData, 1, pRef);
            case "TOGGLE_BREAK" -> claimData.toggleFlag(ClaimData.FLAG_BREAK_BLOCKS);
            case "TOGGLE_PLACE" -> claimData.toggleFlag(ClaimData.FLAG_PLACE_BLOCKS);
            case "TOGGLE_DOORS" -> claimData.toggleFlag(ClaimData.FLAG_OPEN_DOORS);
            case "TOGGLE_CONTAINERS" -> claimData.toggleFlag(ClaimData.FLAG_OPEN_CONTAINERS);
            case "TOGGLE_PVP" -> claimData.toggleFlag(ClaimData.FLAG_PVP);
            case "TOGGLE_MOBS" -> claimData.toggleFlag(ClaimData.FLAG_MOB_DAMAGE);
            case "CLAIM_CHUNK" -> {
                if (pRef == null) return;
                String uuid = pRef.getUuid().toString();

                TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
                if (transform == null) {
                    setStatus("Could not determine position.");
                    return;
                }

                var pos = transform.getPosition();
                int chunkX = ClaimData.blockToChunk((int) pos.getX());
                int chunkZ = ClaimData.blockToChunk((int) pos.getZ());

                String existingOwner = ClaimRegistry.getOwner(chunkX, chunkZ);
                if (existingOwner != null) {
                    if (existingOwner.equals(uuid)) {
                        setStatus("You already own this chunk.");
                    } else {
                        setStatus("This chunk is already claimed.");
                    }
                    return;
                }

                int maxClaims = ClaimCommand.getMaxClaims(ref, store);
                if (claimData.getClaimCount() >= maxClaims) {
                    setStatus("Claim limit reached (" + maxClaims + ").");
                    return;
                }

                claimData.addClaim(chunkX, chunkZ);
                ClaimRegistry.register(chunkX, chunkZ, uuid);
                store.putComponent(ref, StatsPlugin.getInstance().getClaimDataType(), claimData);

                setStatus("Claimed chunk [" + chunkX + ", " + chunkZ + "]!");

                // Refresh the display
                UICommandBuilder cmd = new UICommandBuilder();
                populateDisplay(cmd, claimData);
                this.sendUpdate(cmd, new UIEventBuilder(), false);
                return;
            }
            default -> { return; }
        }

        // Persist changes
        store.putComponent(ref, StatsPlugin.getInstance().getClaimDataType(), claimData);

        // Refresh the UI
        UICommandBuilder cmd = new UICommandBuilder();
        populateDisplay(cmd, claimData);
        this.sendUpdate(cmd, new UIEventBuilder(), false);
    }

    private void setStatus(String text) {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#ClaimStatus.TextSpans", Message.raw(text));
        this.sendUpdate(cmd, new UIEventBuilder(), false);
    }

    private void cycleColor(ClaimData claimData, int direction, PlayerRef pRef) {
        String current = claimData.getClaimColor();
        int idx = COLOR_LIST.indexOf(current);
        if (idx < 0) idx = 0;
        idx = (idx + direction + COLOR_LIST.size()) % COLOR_LIST.size();
        String newColor = COLOR_LIST.get(idx);
        claimData.setClaimColor(newColor);

        // Update the color cache in ClaimRegistry for map markers
        if (pRef != null) {
            ClaimRegistry.setOwnerColor(pRef.getUuid().toString(), claimData.getClaimColorRGB());
        }
    }

    public static class ClaimAction {
        public static final BuilderCodec<ClaimAction> CODEC =
                BuilderCodec.<ClaimAction>builder(ClaimAction.class, ClaimAction::new)
                        .addField(new KeyedCodec<>("Action", Codec.STRING),
                                (d, v) -> d.action = v, d -> d.action)
                        .build();
        private String action;
    }
}
