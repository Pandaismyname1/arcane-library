package com.panda.arcane.knowledge;

import com.panda.arcane.logic.Mastery;
import com.panda.arcane.platform.Services;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;

/** Reads and writes what a player has learned. The server is the only writer. */
public final class KnowledgeManager {
    private KnowledgeManager() {}

    public static Knowledge get(Player player) {
        return Services.PLATFORM.knowledge(player);
    }

    public static Mastery mastery(Player player, Identifier enchantment) {
        return get(player).mastery(enchantment);
    }

    /** Raises mastery to at least {@code mastery}. Returns true when something actually changed. */
    public static boolean learn(Player player, Identifier enchantment, Mastery mastery) {
        Knowledge before = get(player);
        Knowledge after = before.with(enchantment, mastery);
        if (after == before) return false;
        Services.PLATFORM.setKnowledge(player, after);
        return true;
    }

    public static boolean exists(Player player, Identifier enchantment) {
        return player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(enchantment).isPresent();
    }

    public static Identifier idOf(Holder<Enchantment> holder) {
        return holder.unwrapKey().orElseThrow().identifier();
    }
}
