package com.panda.arcane.block;

import com.panda.arcane.ArcaneMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

/** A dusty bookshelf that hides a book. Brush it (vanilla brush) to reveal the loot; it then becomes a Library Bookshelf. */
public class SuspiciousBookshelfBlock extends BrushableBlock {
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, ArcaneMod.id("brushing/suspicious_bookshelf"));

    public SuspiciousBookshelfBlock(BlockBehaviour.Properties properties) {
        super(com.panda.arcane.registry.ModBlocks.LIBRARY_BOOKSHELF.get(), SoundEvents.BRUSH_GRAVEL, SoundEvents.BRUSH_GRAVEL_COMPLETED, properties);
    }

    /** Every new shelf comes with its loot table; a saved one overrides it when it loads. */
    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        BrushableBlockEntity entity = new BrushableBlockEntity(pos, state);
        entity.setLootTable(LOOT, pos.asLong() * 31L + 7L);
        return entity;
    }

    /** A shelf is attached to the wall: it never falls like suspicious sand. */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof BrushableBlockEntity entity) entity.checkReset(level);
    }
}
