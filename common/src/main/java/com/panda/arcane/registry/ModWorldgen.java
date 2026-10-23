package com.panda.arcane.registry;

import com.mojang.serialization.MapCodec;
import com.panda.arcane.ArcaneMod;
import com.panda.arcane.worldgen.DryLandStructure;
import com.panda.arcane.worldgen.LibraryChunkGenerator;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.StructureType;
import com.panda.arcane.platform.registry.DeferredRegister;

public final class ModWorldgen {
    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, ArcaneMod.MODID);

    public static final Supplier<MapCodec<LibraryChunkGenerator>> LIBRARY = GENERATORS.register("library", () -> LibraryChunkGenerator.CODEC);

    private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, ArcaneMod.MODID);

    /** A jigsaw structure that never generates in water (the Ruined Librarian Tower). */
    public static final Supplier<StructureType<DryLandStructure>> DRY_LAND = STRUCTURE_TYPES.register("dry_land", () -> () -> DryLandStructure.CODEC);

    private ModWorldgen() {}

}
