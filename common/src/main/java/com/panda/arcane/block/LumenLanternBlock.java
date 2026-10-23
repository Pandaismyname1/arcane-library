package com.panda.arcane.block;

import com.panda.arcane.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** A full-block lamp of caged Lumen; now and then a mote escapes through one of its open faces. */
public class LumenLanternBlock extends Block {
    public LumenLanternBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) != 0) return;
        Direction dir = Direction.getRandom(random);
        BlockPos next = pos.relative(dir);
        if (level.getBlockState(next).isSolidRender()) return;
        double x = pos.getX() + 0.5 + dir.getStepX() * 0.55 + (dir.getStepX() == 0 ? random.nextDouble() - 0.5 : 0) * 0.8;
        double y = pos.getY() + 0.5 + dir.getStepY() * 0.55 + (dir.getStepY() == 0 ? random.nextDouble() - 0.5 : 0) * 0.8;
        double z = pos.getZ() + 0.5 + dir.getStepZ() * 0.55 + (dir.getStepZ() == 0 ? random.nextDouble() - 0.5 : 0) * 0.8;
        level.addParticle(ModParticles.LUMEN_MOTE.get(), x, y, z, dir.getStepX() * 0.01, 0.01, dir.getStepZ() * 0.01);
    }
}
