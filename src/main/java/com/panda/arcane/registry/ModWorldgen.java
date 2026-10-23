package com.panda.arcane.registry;

import com.mojang.serialization.MapCodec;
import com.panda.arcane.ArcaneMod;
import com.panda.arcane.worldgen.LibraryChunkGenerator;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModWorldgen {
    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, ArcaneMod.MODID);

    public static final Supplier<MapCodec<LibraryChunkGenerator>> LIBRARY = GENERATORS.register("library", () -> LibraryChunkGenerator.CODEC);

    private ModWorldgen() {}

    public static void register(IEventBus modBus) {
        GENERATORS.register(modBus);
    }
}
