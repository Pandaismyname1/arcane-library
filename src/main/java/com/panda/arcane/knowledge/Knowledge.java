package com.panda.arcane.knowledge;

import com.mojang.serialization.Codec;
import com.panda.arcane.logic.Mastery;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/** Immutable per-player map of enchantment id to mastery tier (1..3). */
public record Knowledge(Map<Identifier, Integer> tiers) {
    public static final Knowledge EMPTY = new Knowledge(Map.of());
    public static final int MAX_ENTRIES = 8192;

    public static final Codec<Knowledge> CODEC = Codec.unboundedMap(Identifier.CODEC, Codec.intRange(1, Mastery.MAX_TIER))
            .xmap(Knowledge::new, Knowledge::tiers);
    public static final StreamCodec<RegistryFriendlyByteBuf, Knowledge> STREAM_CODEC = ByteBufCodecs
            .map(LinkedHashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.VAR_INT, MAX_ENTRIES)
            .map(Knowledge::new, k -> new LinkedHashMap<>(k.tiers)).cast();

    public Knowledge {
        tiers = Collections.unmodifiableMap(new LinkedHashMap<>(tiers));
    }

    public Mastery mastery(Identifier enchantment) {
        return Mastery.ofTier(tiers.getOrDefault(enchantment, 0));
    }

    /** Returns a copy where the enchantment is known at least at {@code mastery}; same instance if nothing changes. */
    public Knowledge with(Identifier enchantment, Mastery mastery) {
        if (mastery.tier() <= mastery(enchantment).tier()) return this;
        Map<Identifier, Integer> copy = new LinkedHashMap<>(tiers);
        copy.put(enchantment, mastery.tier());
        return new Knowledge(copy);
    }

    public int size() {
        return tiers.size();
    }
}
