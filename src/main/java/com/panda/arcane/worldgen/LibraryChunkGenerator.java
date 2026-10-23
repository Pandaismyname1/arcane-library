package com.panda.arcane.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panda.arcane.ArcaneMod;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

/** Generates the Arcane Library from {@link LibraryLayout}: a pure, seed-deterministic function of position. */
public class LibraryChunkGenerator extends ChunkGenerator {
    public static final MapCodec<LibraryChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)
    ).apply(i, i.stable(LibraryChunkGenerator::new)));

    private static volatile Map<Mat, BlockState> states;
    private volatile LibraryLayout layout;
    private volatile long layoutSeed = Long.MIN_VALUE;

    public LibraryChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    private LibraryLayout layout(long seed) {
        LibraryLayout l = layout;
        if (l == null || layoutSeed != seed) {
            l = new LibraryLayout(seed);
            layout = l;
            layoutSeed = seed;
        }
        return l;
    }

    /** Layout for tests and runtime lookups that have no RandomState (uses the last seed seen). */
    public LibraryLayout currentLayout() {
        return layout == null ? layout(0L) : layout;
    }

    private static Map<Mat, BlockState> states() {
        Map<Mat, BlockState> m = states;
        if (m == null) {
            m = new EnumMap<>(Mat.class);
            for (Mat mat : Mat.values()) {
                BlockState parsed;
                try {
                    parsed = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, mat.spec(), false).blockState();
                } catch (Exception e) {
                    ArcaneMod.LOGGER.error("Library material {} has a bad block spec '{}'", mat, mat.spec(), e);
                    parsed = Blocks.AIR.defaultBlockState();
                }
                m.put(mat, parsed);
            }
            states = m;
        }
        return m;
    }

    @Override
    public CompletableFuture<ChunkAccess> buildTerrain(ChunkAccess chunk, Blender blender, RandomState randomState,
                                                       StructureManager structureManager, BiomeManager biomeManager,
                                                       @Nullable WorldGenRegion carverBiomeRegion, Set<Holder<Biome>> possibleBiomes) {
        LibraryLayout l = layout(randomState.seed());
        Map<Mat, BlockState> table = states();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        int minX = chunk.getPos().getMinBlockX(), minZ = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinY(), maxY = Math.min(chunk.getMaxY(), LibraryLayout.ROOF);
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                for (int y = minY; y <= maxY; y++) {
                    Mat mat = l.blockAt(minX + lx, y, minZ + lz);
                    if (mat == Mat.AIR) continue;
                    BlockState state = table.get(mat);
                    chunk.setBlockState(pos.set(lx, y, lz), state);
                    oceanFloor.update(lx, y, lz, state);
                    worldSurface.update(lx, y, lz, state);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    /** Chests with loot, and the creatures that live in each room. Runs once per chunk, after terrain. */
    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        super.applyBiomeDecoration(level, chunk, structureManager);
        LibraryLayout l = layout(level.getSeed());
        int minX = chunk.getPos().getMinBlockX(), minZ = chunk.getPos().getMinBlockZ();
        int cx0 = Math.floorDiv(minX, LibraryLayout.CELL) - 0, cx1 = Math.floorDiv(minX + 15, LibraryLayout.CELL);
        int cz0 = Math.floorDiv(minZ, LibraryLayout.CELL), cz1 = Math.floorDiv(minZ + 15, LibraryLayout.CELL);
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                for (LibraryLayout.Feature f : l.features(l.cell(cx, cz))) {
                    if (f.x() < minX || f.x() > minX + 15 || f.z() < minZ || f.z() > minZ + 15) continue;
                    place(level, f);
                }
            }
        }
    }

    private static void place(WorldGenLevel level, LibraryLayout.Feature f) {
        BlockPos pos = new BlockPos(f.x(), f.y(), f.z());
        if (f.type() == LibraryLayout.FeatureType.CHEST) {
            Direction facing = Direction.byName(f.facing());
            BlockState chest = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing == null ? Direction.NORTH : facing);
            level.setBlock(pos, chest, 2);
            ResourceKey<LootTable> table = ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(f.data()));
            RandomizableContainer.setBlockEntityLootTable(level, level.getRandom(), pos, table);
            return;
        }
        BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(f.data())).ifPresentOrElse(holder -> {
            EntityType<?> type = holder.value();
            var entity = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
            if (entity == null) return;
            entity.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360F, 0F);
            if (entity instanceof Mob mob) {
                mob.setPersistenceRequired();
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
            }
            level.addFreshEntityWithPassengers(entity);
        }, () -> ArcaneMod.LOGGER.warn("Library feature names unknown entity {}", f.data()));
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    @Override
    public int getGenDepth() {
        return 80;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor, RandomState randomState) {
        return LibraryLayout.FLOOR + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
        LibraryLayout l = layout(randomState.seed());
        Map<Mat, BlockState> table = states();
        int min = heightAccessor.getMinY();
        int height = Math.min(heightAccessor.getHeight(), LibraryLayout.ROOF + 1 - min);
        BlockState[] column = new BlockState[height];
        for (int i = 0; i < height; i++) column[i] = table.get(l.blockAt(x, min + i, z));
        return new NoiseColumn(min, column);
    }

    @Override
    public void addDebugScreenInfo(List<String> result, RandomState randomState, BlockPos feetPos, SamplerContext samplerContext) {
    }
}
