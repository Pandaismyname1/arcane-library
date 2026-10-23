package com.panda.arcane.portal;

import com.panda.arcane.ArcaneMod;
import com.panda.arcane.platform.Services;
import com.panda.arcane.registry.ModBlocks;
import com.panda.arcane.worldgen.LibraryLayout;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Where a Library portal sends you, and how the way back is found or built. */
public final class PortalTravel {
    public static final ResourceKey<Level> LIBRARY = ResourceKey.create(Registries.DIMENSION, ArcaneMod.id("library"));
    private static final int SEARCH_H = 12, SEARCH_V = 8;

    private PortalTravel() {}

    public static @Nullable TeleportTransition destination(ServerLevel current, Entity entity, BlockPos entry) {
        MinecraftServer server = current.getServer();
        if (current.dimension().equals(LIBRARY)) return leave(server, current, entity);
        ServerLevel library = server.getLevel(LIBRARY);
        if (library == null) return null;
        Services.PLATFORM.setPortalReturn(entity, Optional.of(GlobalPos.of(current.dimension(), entry)));
        int[] gate = LibraryLayout.nearestGate(entity.getBlockX(), entity.getBlockZ());
        int[] arrival = LibraryLayout.gateArrival(gate[0], gate[1]);
        Vec3 target = new Vec3(arrival[0] + 0.5, arrival[1], arrival[2] + 0.5);
        return transition(library, entity, target);
    }

    /**
     * Any portal in the Library leads back to where you came in: the portal you entered by (rebuilt in place if it is
     * gone, even underground or in the Nether). With no record of that, home is your respawn point, else world spawn.
     * Never a position mapped from Library coordinates.
     */
    public static @Nullable TeleportTransition leave(MinecraftServer server, ServerLevel current, Entity entity) {
        Optional<GlobalPos> ret = Services.PLATFORM.portalReturn(entity);
        // Only players may have a way home found for them: items and arrows with no stored origin simply do not travel.
        if (ret.isEmpty() && !(entity instanceof net.minecraft.world.entity.player.Player)) return null;
        GlobalPos home = ret.filter(g -> server.getLevel(g.dimension()) != null).orElseGet(() -> homeOf(server, entity));
        ServerLevel target = server.getLevel(home.dimension());
        if (target == null) target = server.overworld();
        boolean exact = ret.isPresent(); // a portal you used stands (or stood) exactly there
        BlockPos near = home.pos();
        ArcanePortalShape shape = findNear(target, near);
        if (shape == null) shape = exact || target.dimensionType().hasCeiling() ? PortalBuilder.buildAt(target, near) : PortalBuilder.build(target, near);
        if (shape == null) return null;
        return transition(target, entity, bottomCentre(shape));
    }

    /** Where a player with no recorded entry portal goes home to: their respawn point outside the Library, else world spawn. */
    static GlobalPos homeOf(MinecraftServer server, Entity entity) {
        if (entity instanceof net.minecraft.server.level.ServerPlayer sp && sp.getRespawnConfig() != null) {
            GlobalPos bed = sp.getRespawnConfig().respawnData().globalPos();
            if (!bed.dimension().equals(LIBRARY) && server.getLevel(bed.dimension()) != null) return bed;
        }
        GlobalPos spawn = server.getRespawnData().globalPos();
        return spawn.dimension().equals(LIBRARY) ? GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO) : spawn;
    }

    public static Vec3 bottomCentre(ArcanePortalShape shape) {
        BlockPos bl = shape.bottomLeft();
        return shape.axis() == Direction.Axis.X
                ? new Vec3(bl.getX() + shape.width() / 2.0, bl.getY(), bl.getZ() + 0.5)
                : new Vec3(bl.getX() + 0.5, bl.getY(), bl.getZ() + shape.width() / 2.0);
    }

    private static TeleportTransition transition(ServerLevel level, Entity entity, Vec3 target) {
        Vec3 free = PortalShape.findCollisionFreePosition(target, level, entity, entity.getDimensions(entity.getPose()));
        return new TeleportTransition(level, free, Vec3.ZERO, 0.0F, 0.0F, Relative.union(Relative.DELTA, Relative.ROTATION),
                TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET));
    }

    /** The closest lit portal within reach of {@code near}, as a complete shape. */
    public static @Nullable ArcanePortalShape findNear(ServerLevel level, BlockPos near) {
        ArcanePortalShape best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(near.offset(-SEARCH_H, -SEARCH_V, -SEARCH_H), near.offset(SEARCH_H, SEARCH_V, SEARCH_H))) {
            BlockState state = level.getBlockState(p);
            if (!state.is(ModBlocks.LIBRARY_PORTAL.get())) continue;
            double d = p.distSqr(near);
            if (d >= bestDist) continue;
            Direction.Axis axis = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_AXIS);
            Optional<ArcanePortalShape> shape = ArcanePortalShape.find(level, p, axis);
            if (shape.isPresent()) {
                best = shape.get();
                bestDist = d;
            }
        }
        return best;
    }

    /** Builds a fresh frame (2 wide, 3 tall inside) on the ground near a position and lights it. */
    public static final class PortalBuilder {
        private PortalBuilder() {}

        /** On the surface above {@code near}. Not in a roofed dimension, whose "surface" is the ceiling. */
        public static @Nullable ArcanePortalShape build(ServerLevel level, BlockPos near) {
            if (level.dimensionType().hasCeiling()) return null;
            int x = near.getX(), z = near.getZ();
            level.getChunk(x >> 4, z >> 4);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            return buildAt(level, new BlockPos(x, y, z));
        }

        /** At {@code at} itself (dropping up to 3 blocks onto a floor): where an old portal stood, underground or not. */
        public static ArcanePortalShape buildAt(ServerLevel level, BlockPos at) {
            int x = at.getX(), z = at.getZ();
            level.getChunk(x >> 4, z >> 4);
            int y = at.getY();
            for (int i = 0; i < 3 && level.getBlockState(new BlockPos(x, y - 1, z)).isAir(); i++) y--;
            y = Math.max(level.getMinY() + 2, Math.min(y, level.getMaxY() - 6));
            BlockState brick = ModBlocks.INSCRIBED_BRICKS.get().defaultBlockState();
            BlockState chiseled = ModBlocks.CHISELED_INSCRIBED_BRICKS.get().defaultBlockState();
            // clear a pocket and lay a floor so the portal is reachable and not buried
            for (int dx = -2; dx <= 3; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int dy = 0; dy <= 4; dy++) {
                        BlockPos p = new BlockPos(x + dx, y + dy, z + dz);
                        if (dz == 0 && dx >= -1 && dx <= 2 && dy <= 3) continue;
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 18);
                    }
                    BlockPos floor = new BlockPos(x + dx, y - 1, z + dz);
                    if (!level.getBlockState(floor).isSolid() || Math.abs(dz) == 2) level.setBlock(floor, brick, 18);
                }
            }
            for (int i = -1; i <= 2; i++) {
                for (int h = -1; h <= 3; h++) {
                    boolean edge = i == -1 || i == 2 || h == -1 || h == 3;
                    if (edge) level.setBlock(new BlockPos(x + i, y + h, z), (i + h) % 2 == 0 ? chiseled : brick, 18);
                }
            }
            ArcanePortalShape shape = new ArcanePortalShape(new BlockPos(x, y, z), Direction.Axis.X, 2, 3);
            shape.light(level);
            return shape;
        }
    }
}
