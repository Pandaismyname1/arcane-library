package com.panda.arcane.registry;

import com.mojang.serialization.MapCodec;
import com.panda.arcane.ArcaneMod;
import com.panda.arcane.loot.RandomTomeFunction;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Loot functions: {@code arcane:random_tome} lets tomes teach enchantments from any mod. */
public final class ModLoot {
    private static final DeferredRegister<MapCodec<? extends LootItemFunction>> FUNCTIONS =
            DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, ArcaneMod.MODID);

    public static final Supplier<MapCodec<RandomTomeFunction>> RANDOM_TOME = FUNCTIONS.register("random_tome", () -> RandomTomeFunction.MAP_CODEC);

    private ModLoot() {}

    public static void register(IEventBus bus) {
        FUNCTIONS.register(bus);
    }
}
