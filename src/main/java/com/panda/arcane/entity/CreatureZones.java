package com.panda.arcane.entity;

import com.panda.arcane.portal.PortalTravel;
import com.panda.arcane.worldgen.LibraryLayout;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;

/**
 * Where in the Library a creature is spawning (Creatures A). Variants are chosen from this: the Restricted Section
 * gets the nastier ones, and a few variants belong to one kind of room (the Warden of Keys guards vaults).
 *
 * @param library   inside the Library dimension
 * @param restricted inside the Restricted Section (cells {@link LibraryLayout#RESTRICTED_FROM} or more from the origin)
 * @param kind      the room kind of the cell, or null in a street or outside the Library
 * @param u         x inside the room block (0..16), or -1 in a street
 * @param v         z inside the room block (0..16), or -1 in a street
 */
public record CreatureZones(boolean library, boolean restricted, LibraryLayout.@Nullable Kind kind, int u, int v) {
    public static final CreatureZones OUTSIDE = new CreatureZones(false, false, null, -1, -1);

    public static CreatureZones at(ServerLevelAccessor level, BlockPos pos) {
        if (!level.getLevel().dimension().equals(PortalTravel.LIBRARY)) return OUTSIDE;
        return of(level.getLevel().getSeed(), pos.getX(), pos.getZ());
    }

    /** Pure function of the seed and the column, so tests can use it without a world. */
    public static CreatureZones of(long seed, int x, int z) {
        int cx = Math.floorDiv(x, LibraryLayout.CELL), cz = Math.floorDiv(z, LibraryLayout.CELL);
        LibraryLayout.Cell cell = new LibraryLayout(seed).cell(cx, cz);
        int u = x - cell.originX(), v = z - cell.originZ();
        boolean inRoom = u >= 0 && v >= 0 && u < LibraryLayout.BLOCK && v < LibraryLayout.BLOCK;
        return new CreatureZones(true, cell.restricted(), inRoom ? cell.kind() : null, inRoom ? u : -1, inRoom ? v : -1);
    }

    public boolean in(LibraryLayout.Kind k) {
        return kind == k;
    }

    /** Near the middle of the room block (vaults keep their warden there). */
    public boolean central() {
        return u >= 7 && u <= 9 && v >= 7 && v <= 9;
    }

    /** Weighted pick among enum values; a weight of zero never picks. Falls back to the first value. */
    public static <E extends Enum<E>> E pick(E[] values, ToIntFunction<E> weight, RandomSource random) {
        int total = 0;
        for (E e : values) total += Math.max(0, weight.applyAsInt(e));
        if (total <= 0) return values[0];
        int roll = random.nextInt(total);
        for (E e : values) {
            roll -= Math.max(0, weight.applyAsInt(e));
            if (roll < 0) return e;
        }
        return values[0];
    }

    /** Variant ids are saved by name; unknown names fall back to the first value. */
    public static <E extends Enum<E>> E byName(E[] values, String name) {
        for (E e : values) if (e.name().equalsIgnoreCase(name)) return e;
        return values[0];
    }
}
