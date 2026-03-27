package com.mmorpg.stats;

/**
 * A single passive effect granted by a class tier.
 * Value is a fraction: 0.05 means +5%, 0.03 means 3% chance, etc.
 */
public record PassiveEffect(PassiveType type, float value) {}
