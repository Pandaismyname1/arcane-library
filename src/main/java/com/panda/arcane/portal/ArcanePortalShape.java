package com.panda.arcane.portal;

import com.panda.arcane.registry.ModBlocks;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * A rectangular frame of inscribed bricks around an air interior, 2..21 wide and 3..21 tall, vertical, on either
 * horizontal axis. Mirrors the shape rules of the vanilla nether portal.
 */
public record ArcanePortalShape(BlockPos bottomLeft, Direction.Axis axis, int width, int height) {
    public static final int MIN_W = 2, MAX_W = 21, MIN_H = 3, MAX_H = 21;

    public static boolean isFrame(BlockState state) {
        return state.is(ModBlocks.INSCRIBED_BRICKS.get()) || state.is(ModBlocks.CHISELED_INSCRIBED_BRICKS.get());
    }

    private static boolean isEmpty(BlockState state) {
        return state.isAir() || state.is(ModBlocks.LIBRARY_PORTAL.get());
    }

    private static Direction positive(Direction.Axis axis) {
        return axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
    }

    /** Finds the frame containing {@code start} (an interior block) for the given axis, if one is complete. */
    public static Optional<ArcanePortalShape> find(BlockGetter level, BlockPos start, Direction.Axis axis) {
        Direction pos = positive(axis), neg = pos.getOpposite();
        if (!isEmpty(level.getBlockState(start))) return Optional.empty();
        BlockPos p = start;
        int guard = 0;
        while (isEmpty(level.getBlockState(p.below())) && guard++ < MAX_H) p = p.below();
        if (!isFrame(level.getBlockState(p.below()))) return Optional.empty();
        // walk to the left edge along the bottom row
        BlockPos left = p;
        guard = 0;
        while (isEmpty(level.getBlockState(left.relative(neg))) && isFrame(level.getBlockState(left.relative(neg).below())) && guard++ < MAX_W) {
            left = left.relative(neg);
        }
        if (!isFrame(level.getBlockState(left.relative(neg)))) return Optional.empty();
        int width = 0;
        BlockPos q = left;
        while (isEmpty(level.getBlockState(q)) && isFrame(level.getBlockState(q.below())) && width <= MAX_W) {
            q = q.relative(pos);
            width++;
        }
        if (width < MIN_W || width > MAX_W || !isFrame(level.getBlockState(q))) return Optional.empty();
        for (int h = 0; h <= MAX_H + 1; h++) {
            BlockPos row = left.above(h);
            if (allFrame(level, row, pos, width)) {
                return h >= MIN_H ? Optional.of(new ArcanePortalShape(left, axis, width, h)) : Optional.empty();
            }
            for (int i = 0; i < width; i++) {
                if (!isEmpty(level.getBlockState(row.relative(pos, i)))) return Optional.empty();
            }
            if (!isFrame(level.getBlockState(row.relative(neg))) || !isFrame(level.getBlockState(row.relative(pos, width)))) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static boolean allFrame(BlockGetter level, BlockPos row, Direction pos, int width) {
        for (int i = 0; i < width; i++) {
            if (!isFrame(level.getBlockState(row.relative(pos, i)))) return false;
        }
        return true;
    }

    /** Fills the interior with portal blocks. */
    public void light(LevelAccessor level) {
        Direction pos = positive(axis);
        BlockState portal = ModBlocks.LIBRARY_PORTAL.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_AXIS, axis);
        for (int i = 0; i < width; i++) {
            for (int h = 0; h < height; h++) {
                level.setBlock(bottomLeft.relative(pos, i).above(h), portal, 18);
            }
        }
    }

    public boolean isComplete(BlockGetter level) {
        return find(level, bottomLeft, axis).map(s -> s.width == width && s.height == height).orElse(false);
    }
}
