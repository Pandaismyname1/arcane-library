package com.panda.arcane.entity;

/**
 * What the Creatures A renderers read from an entity (all of it synced data or entity events, so it is correct on
 * the client). Kept in common code with no client types so dedicated servers load it.
 */
public interface CreatureVisuals {
    /** Index of the variant in the entity's Variant enum (the texture is chosen from it). */
    int variantIndex();

    /** 0..1 progress of the variant's special move (a tell, a wind-up, a swell); 0 when idle. */
    default float actionProgress(float partialTick) {
        return 0.0F;
    }

    /** 0..1 progress of the ordinary attack swing; 0 when not attacking. */
    default float attackProgress(float partialTick) {
        return 0.0F;
    }

    /** A per-mob visual flag (angry, carrying loot, small). */
    default boolean visualFlag() {
        return false;
    }
}
