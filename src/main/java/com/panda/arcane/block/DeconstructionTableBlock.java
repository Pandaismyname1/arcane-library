package com.panda.arcane.block;

import com.panda.arcane.lectern.DeconstructionMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** Destroys enchanted gear to learn what is on it and recover Lumen Dust. */
public class DeconstructionTableBlock extends Block {
    public static final Component TITLE = Component.translatable("container.arcane.deconstruction");

    public DeconstructionTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        // Art: ink from the stain on the west edge drips to the floor; the wheel sheds a lumen mote now and then
        if (random.nextInt(14) == 0) {
            level.addParticle(com.panda.arcane.registry.ModParticles.INK_DROP.get(), pos.getX() - 0.03, pos.getY() + 0.6,
                    pos.getZ() + 0.62 + random.nextDouble() * 0.16, 0, 0, 0);
        }
        if (random.nextInt(18) == 0) {
            level.addParticle(com.panda.arcane.registry.ModParticles.LUMEN_MOTE.get(), pos.getX() + 0.5,
                    pos.getY() + 1.14, pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.01, 0);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            player.openMenu(state.getMenuProvider(level, pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected @Nullable MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inv, p) -> new DeconstructionMenu(id, inv, ContainerLevelAccess.create(level, pos)), TITLE);
    }
}
