package com.mmorpg.stats;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * ECS component tracking a player's purchased and equipped shop items.
 * Persisted via BSON codec serialization.
 */
public class PlayerEquipmentData implements Component<EntityStore> {

    /** Set of owned item IDs (ShopItem.id values) */
    private List<String> ownedItems;

    /** Currently equipped item ID, or empty string if none */
    private String equippedItem;

    /** Owned offhand item ID (OffhandItem.gameItemId), or empty string if none */
    private String ownedOffhand;

    public static final BuilderCodec<PlayerEquipmentData> CODEC =
            BuilderCodec.<PlayerEquipmentData>builder(PlayerEquipmentData.class, PlayerEquipmentData::new)
                    .addField(new KeyedCodec<>("OwnedItems", Codec.STRING),
                            PlayerEquipmentData::parseOwned,
                            PlayerEquipmentData::serializeOwned)
                    .addField(new KeyedCodec<>("EquippedItem", Codec.STRING),
                            (data, value) -> data.equippedItem = value,
                            data -> data.equippedItem)
                    .addField(new KeyedCodec<>("OwnedOffhand", Codec.STRING),
                            (data, value) -> data.ownedOffhand = (value != null) ? value : "",
                            data -> data.ownedOffhand)
                    .build();

    public PlayerEquipmentData() {
        this.ownedItems = new ArrayList<>();
        this.equippedItem = "";
        this.ownedOffhand = "";
    }

    public PlayerEquipmentData(PlayerEquipmentData clone) {
        this.ownedItems = new ArrayList<>(clone.ownedItems);
        this.equippedItem = clone.equippedItem;
        this.ownedOffhand = clone.ownedOffhand;
    }

    public boolean ownsItem(String itemId) {
        return ownedItems.contains(itemId);
    }

    public void addOwnedItem(String itemId) {
        if (!ownedItems.contains(itemId)) {
            ownedItems.add(itemId);
        }
    }

    public List<String> getOwnedItems() {
        return Collections.unmodifiableList(ownedItems);
    }

    public String getEquippedItem() { return equippedItem; }

    public void setEquippedItem(String itemId) {
        this.equippedItem = (itemId != null) ? itemId : "";
    }

    /** Get the currently equipped ShopItem, or null if none. */
    public ShopItem getEquippedShopItem() {
        if (equippedItem.isEmpty()) return null;
        return ShopItem.findById(equippedItem);
    }

    // -- Offhand item --

    public String getOwnedOffhand() { return ownedOffhand; }

    public void setOwnedOffhand(String itemId) {
        this.ownedOffhand = (itemId != null) ? itemId : "";
    }

    public boolean hasOffhand() { return !ownedOffhand.isEmpty(); }

    /** Get the owned OffhandItem, or null if none. */
    public OffhandItem getOwnedOffhandItem() {
        if (ownedOffhand.isEmpty()) return null;
        return OffhandItem.findById(ownedOffhand);
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new PlayerEquipmentData(this);
    }

    // -- Serialization helpers (comma-delimited string ↔ list) --

    private static void parseOwned(PlayerEquipmentData data, String raw) {
        data.ownedItems.clear();
        if (raw != null && !raw.isEmpty()) {
            for (String s : raw.split(",")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) data.ownedItems.add(trimmed);
            }
        }
    }

    private static String serializeOwned(PlayerEquipmentData data) {
        return String.join(",", data.ownedItems);
    }
}
