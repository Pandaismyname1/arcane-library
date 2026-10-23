package com.panda.arcane.block;

import com.panda.arcane.lectern.ArcaneLecternMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** The Arcane Lectern: replaces the vanilla enchanting table. All state lives in the menu, so no block entity. */
public class ArcaneLecternBlock extends HorizontalDirectionalBlock {
    public static final Component TITLE = Component.translatable("container.arcane.lectern");
    private static final VoxelShape SHAPE = net.minecraft.world.level.block.Block.column(16.0, 0.0, 14.0);

    public ArcaneLecternBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
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
        return new SimpleMenuProvider((id, inv, p) -> new ArcaneLecternMenu(id, inv, ContainerLevelAccess.create(level, pos)), TITLE);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Art: lumen motes rise off the open book; nearby shelves send it glyphs, as bookshelves do an enchanting table
        double bx = pos.getX() + 0.5, by = pos.getY() + 0.95, bz = pos.getZ() + 0.5;
        if (random.nextInt(5) == 0) {
            level.addParticle(com.panda.arcane.registry.ModParticles.LUMEN_MOTE.get(), bx + (random.nextDouble() - 0.5) * 0.6,
                    by, bz + (random.nextDouble() - 0.5) * 0.6, 0, 0.015, 0);
        }
        for (BlockPos offset : net.minecraft.world.level.block.EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (random.nextInt(24) != 0) continue;
            BlockState shelf = level.getBlockState(pos.offset(offset));
            if (shelf.is(com.panda.arcane.registry.ModBlocks.KNOWLEDGE_SHELF.get()) || shelf.is(net.minecraft.tags.BlockTags.ENCHANTMENT_POWER_PROVIDER)) {
                level.addParticle(com.panda.arcane.registry.ModParticles.PORTAL_GLYPH.get(), bx, by + 0.3, bz,
                        offset.getX() + random.nextFloat() - 0.5, offset.getY() - random.nextFloat() - 0.2,
                        offset.getZ() + random.nextFloat() - 0.5);
            }
        }
    }
}
