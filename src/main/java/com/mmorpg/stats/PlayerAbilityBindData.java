package com.mmorpg.stats;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Persisted ECS component storing a player's ability slot bindings.
 * Slots 1-3 each map to an ability ID string (or empty if unset).
 */
public class PlayerAbilityBindData implements Component<EntityStore> {

    private String slot1;
    private String slot2;
    private String slot3;

    public static final BuilderCodec<PlayerAbilityBindData> CODEC =
            BuilderCodec.<PlayerAbilityBindData>builder(PlayerAbilityBindData.class, PlayerAbilityBindData::new)
                    .addField(new KeyedCodec<>("Slot1", Codec.STRING),
                            (data, value) -> data.slot1 = value != null ? value : "",
                            data -> data.slot1)
                    .addField(new KeyedCodec<>("Slot2", Codec.STRING),
                            (data, value) -> data.slot2 = value != null ? value : "",
                            data -> data.slot2)
                    .addField(new KeyedCodec<>("Slot3", Codec.STRING),
                            (data, value) -> data.slot3 = value != null ? value : "",
                            data -> data.slot3)
                    .build();

    public PlayerAbilityBindData() {
        this.slot1 = "";
        this.slot2 = "";
        this.slot3 = "";
    }

    public PlayerAbilityBindData(PlayerAbilityBindData clone) {
        this.slot1 = clone.slot1;
        this.slot2 = clone.slot2;
        this.slot3 = clone.slot3;
    }

    public String getSlot(int slot) {
        return switch (slot) {
            case 0 -> slot1.isEmpty() ? null : slot1;
            case 1 -> slot2.isEmpty() ? null : slot2;
            case 2 -> slot3.isEmpty() ? null : slot3;
            default -> null;
        };
    }

    public void setSlot(int slot, String abilityId) {
        String val = abilityId != null ? abilityId : "";
        switch (slot) {
            case 0 -> slot1 = val;
            case 1 -> slot2 = val;
            case 2 -> slot3 = val;
        }
    }

    /** Copy all slots into a transient PlayerAbilityData. */
    public void loadInto(PlayerAbilityData target) {
        for (int i = 0; i < 3; i++) {
            String id = getSlot(i);
            if (id != null) {
                target.bindSlot(i, id);
            }
        }
    }

    /** Copy current binds from a transient PlayerAbilityData. */
    public void copyFrom(PlayerAbilityData source) {
        for (int i = 0; i < 3; i++) {
            setSlot(i, source.getSlot(i));
        }
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new PlayerAbilityBindData(this);
    }
}
