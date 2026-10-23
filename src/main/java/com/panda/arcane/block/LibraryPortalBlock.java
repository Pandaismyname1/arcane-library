package com.panda.arcane.block;

import com.panda.arcane.ArcaneConfig;
import com.panda.arcane.portal.ArcanePortalShape;
import com.panda.arcane.portal.PortalTravel;
import com.panda.arcane.registry.ModSounds;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** The shimmering membrane inside a lit Library portal frame. */
public class LibraryPortalBlock extends Block implements Portal {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final Map<Direction.Axis, VoxelShape> SHAPES = Shapes.rotateHorizontalAxis(Block.column(4.0, 16.0, 0.0, 16.0));

    public LibraryPortalBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(AXIS));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        Direction.Axis axis = state.getValue(AXIS);
        boolean wrongAxis = axis != dir.getAxis() && dir.getAxis().isHorizontal();
        if (!wrongAxis && !neighbourState.is(this) && ArcanePortalShape.find(level, pos, axis).isEmpty()) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, dir, neighbourPos, neighbourState, random);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean precise) {
        if (entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        if (entity instanceof Player player) return player.getAbilities().invulnerable ? 1 : ArcaneConfig.portalDelay();
        return 0;
    }

    @Override
    public @Nullable TeleportTransition getPortalDestination(ServerLevel current, Entity entity, BlockPos entry) {
        return PortalTravel.destination(current, entity, entry);
    }

    @Override
    public Portal.Transition getLocalTransition() {
        return Portal.Transition.CONFUSION;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(100) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.PORTAL_AMBIENT.get(),
                    SoundSource.BLOCKS, 0.5F, random.nextFloat() * 0.4F + 0.8F, false);
        }
        // Art: cyan glyphs drift in from either side and sink into the membrane; a lumen mote now and then
        boolean alongX = state.getValue(AXIS) == Direction.Axis.X;
        for (int i = 0; i < 2; i++) {
            if (random.nextInt(3) != 0) continue;
            double x = alongX ? pos.getX() + random.nextDouble() : pos.getX() + 0.5;
            double y = pos.getY() + random.nextDouble();
            double z = alongX ? pos.getZ() + 0.5 : pos.getZ() + random.nextDouble();
            double out = (random.nextBoolean() ? 1 : -1) * (0.7 + random.nextDouble() * 0.9);
            double side = (random.nextDouble() - 0.5) * 0.8;
            level.addParticle(com.panda.arcane.registry.ModParticles.PORTAL_GLYPH.get(), x, y, z,
                    alongX ? side : out, (random.nextDouble() - 0.3) * 0.9, alongX ? out : side);
        }
        if (random.nextInt(10) == 0) {
            double out = (random.nextBoolean() ? 1 : -1) * 0.02;
            level.addParticle(com.panda.arcane.registry.ModParticles.LUMEN_MOTE.get(),
                    pos.getX() + (alongX ? random.nextDouble() : 0.5), pos.getY() + random.nextDouble(),
                    pos.getZ() + (alongX ? 0.5 : random.nextDouble()), alongX ? 0 : out, 0.01, alongX ? out : 0);
        }
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return ItemStack.EMPTY;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }
}
