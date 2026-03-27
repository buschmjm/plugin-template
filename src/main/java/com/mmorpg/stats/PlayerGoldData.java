package com.mmorpg.stats;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * ECS component storing a player's gold balance.
 * Persisted via BSON codec serialization.
 */
public class PlayerGoldData implements Component<EntityStore> {

    private long gold;

    public static final BuilderCodec<PlayerGoldData> CODEC =
            BuilderCodec.<PlayerGoldData>builder(PlayerGoldData.class, PlayerGoldData::new)
                    .addField(new KeyedCodec<>("Gold", Codec.LONG),
                            (data, value) -> data.gold = value,
                            data -> data.gold)
                    .build();

    public PlayerGoldData() {
        this.gold = 0;
    }

    public PlayerGoldData(PlayerGoldData clone) {
        this.gold = clone.gold;
    }

    public long getGold() { return gold; }

    public void setGold(long gold) { this.gold = Math.max(0, gold); }

    /**
     * Add gold to the balance. Returns the new total.
     */
    public long addGold(long amount) {
        this.gold = Math.max(0, this.gold + amount);
        return this.gold;
    }

    /**
     * Try to spend gold. Returns true if the player had enough and gold was deducted.
     */
    public boolean spend(long amount) {
        if (amount <= 0) return false;
        if (this.gold < amount) return false;
        this.gold -= amount;
        return true;
    }

    /**
     * Check if the player can afford a cost.
     */
    public boolean canAfford(long cost) {
        return this.gold >= cost;
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new PlayerGoldData(this);
    }
}
