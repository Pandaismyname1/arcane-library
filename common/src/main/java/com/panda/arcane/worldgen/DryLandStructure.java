package com.panda.arcane.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panda.arcane.registry.ModWorldgen;
import java.util.Optional;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

/**
 * A jigsaw structure that only generates on dry land. The biome check alone looks at one column, and the start is
 * projected onto the water surface, so a land biome at the edge of an ocean, river or lake could put the tower in the
 * water. This samples a grid over the footprint (the template can be rotated around its start corner, so the grid
 * covers {@code footprint} blocks on every side) and gives up wherever water lies on the ground.
 */
public final class DryLandStructure extends Structure {
    public static final MapCodec<DryLandStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            JigsawStructure.CODEC.codec().fieldOf("jigsaw").forGetter(s -> s.jigsaw),
            com.mojang.serialization.Codec.intRange(0, 64).optionalFieldOf("footprint", 16).forGetter(s -> s.footprint)
    ).apply(i, DryLandStructure::new));

    /** Grid spacing of the dry check, in blocks. */
    private static final int STEP = 8;

    private final JigsawStructure jigsaw;
    private final int footprint;

    private DryLandStructure(JigsawStructure jigsaw, int footprint) {
        super(new StructureSettings(jigsaw.biomes(), jigsaw.spawnOverrides(), jigsaw.step(), jigsaw.terrainAdaptation()));
        this.jigsaw = jigsaw;
        this.footprint = footprint;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x0 = context.chunkPos().getMinBlockX();
        int z0 = context.chunkPos().getMinBlockZ();
        for (int dx = -footprint; dx <= footprint; dx += STEP) {
            for (int dz = -footprint; dz <= footprint; dz += STEP) {
                if (wet(context, x0 + dx, z0 + dz)) return Optional.empty();
            }
        }
        return jigsaw.findGenerationPoint(context);
    }

    /** Water above the ground: the first free block above everything differs from the first above solid ground. */
    static boolean wet(GenerationContext context, int x, int z) {
        var generator = context.chunkGenerator();
        int surface = generator.getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        int ground = generator.getFirstFreeHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
        return surface != ground;
    }

    @Override
    public StructureType<?> type() {
        return ModWorldgen.DRY_LAND.get();
    }
}
