package com.panda.arcane.worldgen;

import java.util.ArrayList;
import java.util.List;

/**
 * The shape of the Arcane Library, as a pure function of world coordinates and the world seed.
 *
 * <p>The world is a grid of 24x24 cells. Each cell has a 7-wide street along its north and west edges and a 17x17
 * block to the south-east. Streets are always open and lit. A block is one room-type chosen from the cell hash.
 * {@code h} is the height above the floor's walking surface (0 = the first air layer).
 */
public final class LibraryLayout {
    public static final int CELL = 24;
    public static final int STREET = 7;
    public static final int BLOCK = 17;
    /** Y of the floor surface block. The first air layer is FLOOR + 1. */
    public static final int FLOOR = 8;
    public static final int STREET_H = 14;
    public static final int MAX_Y = 63;
    /** The bedrock roof. The world floor (y 0) is bedrock too; above the roof is empty, and nothing spawns there. */
    public static final int ROOF = 64;
    /** Cells at least this far from the origin (Chebyshev) belong to the Restricted Section. */
    public static final int RESTRICTED_FROM = 4;
    /** Percent of ordinary cells that are extra gate halls (return portals), on top of the fixed 8-cell grid. */
    public static final int WAYGATE_PERCENT = 5;

    public enum Kind {
        STACKS, READING_ROOM, STUDY, SCRIPTORIUM, ATRIUM, VAULT, GATE_HALL, ARENA,
        /** Two storeys of shelves around an open well, a balcony reached by ladders. */
        GALLERY,
        /** The card catalogue: rows of drawer cabinets that index the stacks. */
        CATALOGUE,
        /** Where books are sewn, pressed and bound: benches, presses, vats, drying lines. */
        BINDERY,
        /** An observatory: an armillary sphere hangs under a starry vault. */
        ORRERY,
        /** Restricted Section only: forbidden books chained to their shelves, one caged chest at the heart. */
        SEALED_ARCHIVE
    }

    public enum Face { WEST, EAST, NORTH, SOUTH }

    public record Cell(int cx, int cz, Kind kind, boolean restricted, long hash) {
        public int originX() { return cx * CELL + STREET; }
        public int originZ() { return cz * CELL + STREET; }
    }

    public enum FeatureType { CHEST, MOB }

    /** A thing that needs a block entity or an entity, placed after terrain. {@code data} is a loot table or an entity id. */
    public record Feature(FeatureType type, int x, int y, int z, String data, String facing) {}

    private final long seed;

    public LibraryLayout(long seed) {
        this.seed = seed;
    }

    // ---- hashing -----------------------------------------------------------------------------------------------

    static long mix(long a) {
        a += 0x9E3779B97F4A7C15L;
        a = (a ^ (a >>> 30)) * 0xBF58476D1CE4E5B9L;
        a = (a ^ (a >>> 27)) * 0x94D049BB133111EBL;
        return a ^ (a >>> 31);
    }

    long hash(int a, int b, int salt) {
        return mix(seed ^ mix(a * 0x1F1F1F1FL + mix(b * 0x3C3C3C3DL + salt)));
    }

    /** Uniform in [0, 1). */
    double unit(int a, int b, int salt) {
        return (hash(a, b, salt) >>> 11) * 0x1.0p-53;
    }

    // ---- cells -------------------------------------------------------------------------------------------------

    public static boolean isGate(int cx, int cz) {
        return Math.floorMod(cx, 8) == 0 && Math.floorMod(cz, 8) == 0;
    }

    public static boolean isArena(int cx, int cz) {
        return Math.max(Math.abs(cx), Math.abs(cz)) >= 5 && Math.floorMod(cx, 6) == 5 && Math.floorMod(cz, 6) == 0;
    }

    public Cell cell(int cx, int cz) {
        boolean restricted = Math.max(Math.abs(cx), Math.abs(cz)) >= RESTRICTED_FROM;
        long h = hash(cx, cz, 1);
        Kind kind;
        if (isGate(cx, cz)) {
            kind = Kind.GATE_HALL;
        } else if (isArena(cx, cz)) {
            kind = Kind.ARENA;
        } else if (Math.floorMod(hash(cx, cz, 2), 100L) < WAYGATE_PERCENT) {
            kind = Kind.GATE_HALL; // a way home between the fixed gates: lost players should not wander far to find one
        } else {
            int roll = (int) Math.floorMod(h, 100L);
            if (!restricted) {
                kind = pick(roll, NORMAL_ROLLS);
            } else {
                kind = pick(roll, RESTRICTED_ROLLS);
            }
        }
        return new Cell(cx, cz, kind, restricted, h);
    }

    // cumulative percent thresholds (GATE_HALL and ARENA are placed, never rolled)
    private static final Object[][] NORMAL_ROLLS = {
            {Kind.STACKS, 34}, {Kind.READING_ROOM, 43}, {Kind.STUDY, 55}, {Kind.SCRIPTORIUM, 63}, {Kind.ATRIUM, 70},
            {Kind.VAULT, 76}, {Kind.GALLERY, 84}, {Kind.CATALOGUE, 91}, {Kind.BINDERY, 96}, {Kind.ORRERY, 100}};
    private static final Object[][] RESTRICTED_ROLLS = {
            {Kind.STACKS, 30}, {Kind.READING_ROOM, 33}, {Kind.STUDY, 40}, {Kind.SCRIPTORIUM, 46}, {Kind.ATRIUM, 51},
            {Kind.VAULT, 79}, {Kind.GALLERY, 84}, {Kind.CATALOGUE, 88}, {Kind.BINDERY, 90}, {Kind.ORRERY, 92},
            {Kind.SEALED_ARCHIVE, 100}};

    private static Kind pick(int roll, Object[][] table) {
        for (Object[] row : table) if (roll < (int) row[1]) return (Kind) row[0];
        return Kind.STACKS;
    }

    static int topHeight(Kind kind) {
        return switch (kind) {
            case STACKS -> 10;
            case STUDY -> 8;
            case VAULT -> 12;
            case READING_ROOM -> 20;
            case SCRIPTORIUM -> 14;
            case ATRIUM -> 36;
            case GATE_HALL -> 20;
            case ARENA -> 38;
            case GALLERY -> 16;
            case CATALOGUE -> 10;
            case BINDERY -> 10;
            case ORRERY -> 22;
            case SEALED_ARCHIVE -> 12;
        };
    }

    /** Which faces of a block have a doorway into the street. */
    public boolean doorOpen(Cell cell, Face face) {
        return switch (cell.kind()) {
            case READING_ROOM, SCRIPTORIUM, ATRIUM, GATE_HALL, ARENA, GALLERY, CATALOGUE, BINDERY, ORRERY -> true;
            case VAULT -> false;
            case SEALED_ARCHIVE -> face == vaultFace(cell); // one way in
            case STACKS -> {
                int mask = (int) (Math.floorMod(cell.hash() >>> 8, 15L)) + 1; // 1..15, never zero
                yield (mask & (1 << face.ordinal())) != 0;
            }
            case STUDY -> {
                int mask = (int) (Math.floorMod(cell.hash() >>> 16, 4L)); // 0..3 chooses one or two faces
                yield face.ordinal() == mask || face.ordinal() == (mask + 2) % 4 && (cell.hash() & 1) == 0;
            }
        };
    }

    /** The one face a vault's hidden corridor (or a sealed archive's only door) opens on. */
    public Face vaultFace(Cell cell) {
        return Face.values()[(int) Math.floorMod(cell.hash() >>> 24, 4L)];
    }

    // ---- blocks ------------------------------------------------------------------------------------------------

    public Mat blockAt(int x, int y, int z) {
        if (y <= 0 || y == ROOF) return Mat.BEDROCK;
        if (y > ROOF) return Mat.AIR;
        if (y < FLOOR) return Mat.BASE;
        if (y > MAX_Y) return Mat.BASE;
        int cx = Math.floorDiv(x, CELL), cz = Math.floorDiv(z, CELL);
        int lu = Math.floorMod(x, CELL), lv = Math.floorMod(z, CELL);
        int h = y - FLOOR - 1;
        boolean streetX = lu < STREET; // part of a north-south street
        boolean streetZ = lv < STREET; // part of an east-west street
        if (streetX || streetZ) return street(x, z, h, lu, lv, cx, cz, streetX, streetZ);
        return inBlock(cell(cx, cz), lu - STREET, lv - STREET, h, x, z);
    }

    private Mat street(int x, int z, int h, int lu, int lv, int cx, int cz, boolean streetX, boolean streetZ) {
        boolean restricted = Math.max(Math.abs(cx), Math.abs(cz)) >= RESTRICTED_FROM;
        if (h == -1) return streetFloor(x, z, lu, lv, streetX, streetZ, restricted);
        if (h >= STREET_H) return h == STREET_H ? (restricted ? Mat.CEILING_RUIN : Mat.CEILING) : Mat.BASE;
        boolean plaza = streetX && streetZ;
        int ax = Math.floorMod(x, 8), az = Math.floorMod(z, 8);
        if (plaza) {
            boolean corner = (lu == 0 || lu == 6) && (lv == 0 || lv == 6);
            if (corner) return restricted ? Mat.PILLAR_RUIN : Mat.PILLAR;
            if (lu == 3 && lv == 3) { // chandelier over the plaza centre
                if (h >= STREET_H - 3) return Mat.CHAIN;
                if (h == STREET_H - 4) return Mat.LANTERN;
            }
            if (h == 0 && !restricted && lu >= 2 && lu <= 4 && lv >= 2 && lv <= 4 && !(lu == 3 && lv == 3)) return Mat.CARPET;
            return Mat.AIR;
        }
        if (streetX) { // runs along z; across = lu
            int across = lu;
            if ((across == 0 || across == 6) && az == 3) return restricted ? Mat.PILLAR_RUIN : Mat.PILLAR;
            if (az == 3 && h == STREET_H - 1) return Mat.BEAM_X;
            if (across == 3 && (az == 1 || az == 5)) {
                if (restricted && Math.floorMod(Math.floorDiv(z, 8), 3) != 0) return Mat.AIR;
                if (h >= STREET_H - 2) return Mat.CHAIN;
                if (h == STREET_H - 3) return Mat.LANTERN;
            }
            if (h == 0 && across >= 2 && across <= 4) return restricted ? (unit(x, z, 7) < 0.45 ? Mat.CARPET_DARK : Mat.AIR) : Mat.CARPET;
            if (restricted && h == 9 && unit(x, z, 9) < 0.01) return Mat.COBWEB;
            return Mat.AIR;
        }
        int across = lv; // runs along x
        if ((across == 0 || across == 6) && ax == 3) return restricted ? Mat.PILLAR_RUIN : Mat.PILLAR;
        if (ax == 3 && h == STREET_H - 1) return Mat.BEAM_Z;
        if (across == 3 && (ax == 1 || ax == 5)) {
            if (restricted && Math.floorMod(Math.floorDiv(x, 8), 3) != 0) return Mat.AIR;
            if (h >= STREET_H - 2) return Mat.CHAIN;
            if (h == STREET_H - 3) return Mat.LANTERN;
        }
        if (h == 0 && across >= 2 && across <= 4) return restricted ? (unit(x, z, 8) < 0.45 ? Mat.CARPET_DARK : Mat.AIR) : Mat.CARPET;
        if (restricted && h == 9 && unit(x, z, 10) < 0.01) return Mat.COBWEB;
        return Mat.AIR;
    }

    private Mat streetFloor(int x, int z, int lu, int lv, boolean streetX, boolean streetZ, boolean restricted) {
        if (streetX && streetZ) {
            if (lu == 3 && lv == 3) return Mat.CHISEL;
            int d = Math.max(Math.abs(lu - 3), Math.abs(lv - 3));
            if (d == 2) return Mat.TRIM;
        }
        boolean parity = ((Math.floorDiv(x, 2) + Math.floorDiv(z, 2)) & 1) == 0;
        if (restricted) return unit(x, z, 21) < 0.2 ? Mat.FLOOR_RUIN_B : parity ? Mat.FLOOR_RUIN_A : Mat.FLOOR_RUIN_B;
        return parity ? Mat.FLOOR_A : Mat.FLOOR_B;
    }

    private Mat blockFloor(Cell cell, int bu, int bv, int x, int z) {
        if (cell.kind() == Kind.GATE_HALL && bv == 3 && bu >= 6 && bu <= 9) return Mat.TRIM; // portal sill
        if (cell.kind() == Kind.ARENA) { // Boss: the Sanctum floor (see arena())
            int du = bu - 8, dv = bv - 8;
            int d2 = du * du + dv * dv;
            if (Math.max(Math.abs(du), Math.abs(dv)) <= 1) return Mat.CHISEL; // the dais: Recall pulls you here
            if (d2 >= 21 && d2 <= 29) return Mat.TRIM;                         // ring at the Page Slam's reach (5 blocks)
            if (Math.abs(du) == 5 && Math.abs(dv) == 5) return Mat.CHISEL;     // ward sockets: the Index calls wards here
        }
        if (cell.kind() == Kind.ATRIUM && unit(x, z, 31) < 0.55) return Mat.MOSS;
        if (cell.kind() == Kind.ORRERY) { // a compass rose in dark stone under the sphere
            int du = bu - 8, dv = bv - 8;
            int d2 = du * du + dv * dv;
            if (Math.max(Math.abs(du), Math.abs(dv)) <= 1) return Mat.CHISEL;
            if (d2 >= 30 && d2 <= 42) return (du == 0 || dv == 0) ? Mat.CHISEL : Mat.TRIM;
            if ((du == 0 || dv == 0) && d2 < 30) return Mat.TRIM;
            return Mat.FLOOR_RUIN_A;
        }
        if (cell.kind() == Kind.SEALED_ARCHIVE && Math.max(Math.abs(bu - 8), Math.abs(bv - 8)) <= 1) return Mat.CRYING;
        boolean parity = ((x + z) & 1) == 0;
        if (cell.restricted()) return unit(x, z, 22) < 0.25 ? Mat.FLOOR_RUIN_B : parity ? Mat.FLOOR_RUIN_A : Mat.FLOOR_RUIN_B;
        return parity ? Mat.FLOOR_A : Mat.FLOOR_B;
    }

    private Mat inBlock(Cell cell, int bu, int bv, int h, int x, int z) {
        if (h == -1) return blockFloor(cell, bu, bv, x, z);
        int top = topHeight(cell.kind());
        if (h == top && cell.kind() == Kind.ORRERY) return unit(x, z, 101) < 0.07 ? Mat.STAR : Mat.NIGHT;
        if (h >= top) return h == top ? (cell.restricted() ? Mat.CEILING_RUIN : Mat.CEILING) : Mat.BASE;
        boolean ring = bu == 0 || bu == BLOCK - 1 || bv == 0 || bv == BLOCK - 1;
        if (ring) return wall(cell, bu, bv, h, top, x, z);
        return switch (cell.kind()) {
            case STACKS -> stacksAny(cell, bu, bv, h, top, x, z);
            case STUDY -> study(cell, bu, bv, h, top);
            case VAULT -> vault(cell, bu, bv, h, top);
            case READING_ROOM -> reading(cell, bu, bv, h, top);
            case SCRIPTORIUM -> scriptorium(cell, bu, bv, h, top);
            case ATRIUM -> atrium(cell, bu, bv, h, top, x, z);
            case GATE_HALL -> gate(cell, bu, bv, h, top);
            case ARENA -> arena(cell, bu, bv, h, top);
            case GALLERY -> gallery(cell, bu, bv, h, top);
            case CATALOGUE -> catalogueAny(cell, bu, bv, h, top, x, z);
            case BINDERY -> bindery(cell, bu, bv, h, top, x, z);
            case ORRERY -> orrery(cell, bu, bv, h, top);
            case SEALED_ARCHIVE -> sealed(cell, bu, bv, h, top, x, z);
        };
    }

    // ---- walls -------------------------------------------------------------------------------------------------

    private Mat wall(Cell cell, int bu, int bv, int h, int top, int x, int z) {
        boolean cornerCol = (bu == 0 || bu == BLOCK - 1) && (bv == 0 || bv == BLOCK - 1);
        if (cornerCol) return Mat.TRIM;
        Face face = bu == 0 ? Face.WEST : bu == BLOCK - 1 ? Face.EAST : bv == 0 ? Face.NORTH : Face.SOUTH;
        int along = (face == Face.WEST || face == Face.EAST) ? bv : bu;
        boolean doorZone = along >= 7 && along <= 9;
        if (doorZone && doorOpen(cell, face)) {
            if (h < 4) return Mat.AIR;
            if (h == 4) return Mat.CHISEL;
        }
        if (doorZone && cell.kind() == Kind.VAULT && vaultFace(cell) == face && along == 8) {
            if (h < 3) return Mat.AIR;
        }
        boolean tall = top > 14;
        if (tall) {
            int shelvesTo = cell.kind() == Kind.GALLERY ? top - 1 : 8; // the gallery's walls are shelved up both storeys
            if (h < shelvesTo) return h % 4 == 3 ? Mat.TRIM : (unit(x, z + h, 41) < 0.08 ? Mat.SHELF_KNOW : Mat.SHELF);
            return h % 6 == 3 ? Mat.CHISEL : Mat.TRIM;
        }
        if (h % 4 == 3 || h >= top - 1) return Mat.TRIM;
        if (cell.restricted() && cell.kind() != Kind.VAULT && unit(x + h * 31, z, 42) < 0.18 && h < top - 2) return Mat.COBWEB;
        double r = unit(x, z + h, 43);
        return r < 0.025 ? Mat.SHELF_SUS : r < 0.07 ? Mat.SHELF_KNOW : Mat.SHELF;
    }

    private static Mat carpet(Cell cell) {
        return cell.restricted() ? Mat.CARPET_DARK : Mat.CARPET;
    }

    // ---- interiors ---------------------------------------------------------------------------------------------

    private Mat stacks(Cell cell, int bu, int bv, int h, int top, int x, int z) {
        boolean shelfCol = bu % 3 == 1 && bv >= 1 && bv <= 15 && !(bv >= 7 && bv <= 9);
        if (shelfCol && h <= 6) {
            if (cell.restricted() && unit(x, z + h * 17, 51) < 0.22) return Mat.AIR;
            double r = unit(x, z + h, 52);
            return r < 0.025 ? Mat.SHELF_SUS : r < 0.06 ? Mat.SHELF_KNOW : Mat.SHELF;
        }
        if (h == top - 1 && bu % 3 == 0 && bv % 4 == 2) {
            if (cell.restricted() && unit(x, z, 53) < 0.6) return Mat.AIR;
            return Mat.LANTERN;
        }
        if (h == 0 && bv >= 7 && bv <= 9 && bu % 3 != 1) return carpet(cell);
        if (cell.restricted() && h == 5 && unit(x, z, 54) < 0.03) return Mat.COBWEB;
        return Mat.AIR;
    }

    /** Half the stacks run their rows east-west instead of north-south. */
    private Mat stacksAny(Cell cell, int bu, int bv, int h, int top, int x, int z) {
        return ((cell.hash() >>> 33) & 1) == 0 ? stacks(cell, bu, bv, h, top, x, z) : stacks(cell, bv, bu, h, top, x, z);
    }

    private Mat study(Cell cell, int bu, int bv, int h, int top) {
        if (h == 0 && bu >= 6 && bu <= 10 && bv >= 6 && bv <= 10) return carpet(cell);
        if (bu == 4 && bv == 4) return h == 0 ? Mat.TABLE_LEG : h == 1 ? Mat.TABLE_TOP : Mat.AIR;
        if (bu == 12 && bv == 12) return h == 0 ? Mat.LECTERN_N : Mat.AIR;
        if (bu == 3 && bv == 12 && h <= 5) return Mat.SHELF;
        if (bu == 13 && bv == 3 && h <= 5) return Mat.SHELF;
        if (h == top - 1 && bu == 8 && bv == 8) return Mat.LANTERN;
        return Mat.AIR;
    }

    private Mat vault(Cell cell, int bu, int bv, int h, int top) {
        int du = bu - 8, dv = bv - 8;
        int ring = Math.max(Math.abs(du), Math.abs(dv));
        Face face = vaultFace(cell);
        if (ring <= 3) { // chamber interior
            if (h == 0 && ring <= 2) return carpet(cell);
            if (h == top - 1 && du == 0 && dv == 0) return Mat.LANTERN;
            if (h <= 1 && ((Math.abs(du) == 3 && Math.abs(dv) == 3))) return Mat.BARREL;
            return h < 8 ? Mat.AIR : Mat.TRIM;
        }
        if (ring == 4) { // chamber wall
            boolean door = switch (face) {
                case NORTH -> dv == -4 && du == 0;
                case SOUTH -> dv == 4 && du == 0;
                case WEST -> du == -4 && dv == 0;
                case EAST -> du == 4 && dv == 0;
            };
            if (door && h < 3) return Mat.AIR;
            if (h >= 2 && h <= 3 && (Math.abs(du) + Math.abs(dv)) % 2 == 1 && !door) return Mat.BARS;
            return h % 4 == 3 ? Mat.CHISEL : Mat.TRIM;
        }
        // hidden corridor: one block wide, from the entry face to the chamber door
        boolean corridor = switch (face) {
            case NORTH -> du == 0 && dv < -4;
            case SOUTH -> du == 0 && dv > 4;
            case WEST -> dv == 0 && du < -4;
            case EAST -> dv == 0 && du > 4;
        };
        if (corridor) {
            if (h == 0) return carpet(cell);
            if (h < 3) return Mat.AIR;
            if (h == 3 && Math.abs(Math.max(Math.abs(du), Math.abs(dv))) % 3 == 0) return Mat.LANTERN;
            return Mat.TRIM;
        }
        return h % 4 == 3 ? Mat.TRIM : Mat.SHELF;
    }

    private Mat reading(Cell cell, int bu, int bv, int h, int top) {
        int du = bu - 8, dv = bv - 8;
        int cheb = Math.max(Math.abs(du), Math.abs(dv));
        if (cheb <= 1) { // dais with the lectern
            if (h == 0) return Mat.TRIM;
            if (h == 1 && du == 0 && dv == 0) return Mat.ARCANE_LECTERN;
            if (h == 1 && Math.abs(du) + Math.abs(dv) == 2) return Mat.CANDLE;
            return Mat.AIR;
        }
        boolean pillar = (Math.abs(du) == 4 && Math.abs(dv) == 4);
        if (pillar) {
            if (h <= top - 3) return cell.restricted() ? Mat.PILLAR_RUIN : Mat.PILLAR;
            if (h == top - 2) return Mat.LANTERN;
            return Mat.AIR;
        }
        if (h == 0 && (cheb == 3 || cheb == 4) && !cell.restricted()) return Mat.CARPET;
        if ((du == -5 && dv == 0 || du == 5 && dv == 0 || dv == -5 && du == 0 || dv == 5 && du == 0)) {
            return h == 0 ? Mat.TABLE_LEG : h == 1 ? Mat.TABLE_TOP : Mat.AIR;
        }
        // chandelier: four lanterns hanging on chains around the centre
        if ((Math.abs(du) == 2 && Math.abs(dv) == 2)) {
            if (h >= top - 6) return Mat.CHAIN;
            if (h == top - 7) return Mat.LANTERN;
        }
        if (du == 0 && dv == 0 && h >= top - 8) return h == top - 9 ? Mat.LANTERN : Mat.CHAIN;
        return Mat.AIR;
    }

    private Mat scriptorium(Cell cell, int bu, int bv, int h, int top) {
        boolean desk = (bu == 4 || bu == 8 || bu == 12) && (bv == 4 || bv == 12) ;
        if (desk) return h == 0 ? (bu == 8 ? Mat.LECTERN_S : Mat.LECTERN_N) : Mat.AIR;
        if ((bu == 3 || bu == 13) && (bv == 8)) return h == 0 ? Mat.LOOM : Mat.AIR;
        if ((bu == 2 && bv == 2) || (bu == 14 && bv == 14)) return h <= 1 ? Mat.BARREL : Mat.AIR;
        if (h == 0 && bu >= 7 && bu <= 9) return carpet(cell);
        if (h == top - 1 && ((bu == 6 || bu == 10) && (bv == 6 || bv == 10))) return Mat.LANTERN;
        return Mat.AIR;
    }

    private Mat atrium(Cell cell, int bu, int bv, int h, int top, int x, int z) {
        int du = bu - 8, dv = bv - 8;
        if (du == 0 && dv == 0 && h <= 9) return Mat.LOG;
        double d = Math.sqrt(du * du + dv * dv + (h - 11) * (h - 11) * 0.6);
        if (h >= 8 && h <= 14 && d < 3.6 && !(du == 0 && dv == 0 && h <= 9)) return unit(x, z + h, 61) < 0.25 ? Mat.LEAVES : Mat.LEAVES_PLAIN;
        if (h == 0 && Math.abs(du) == 3 && Math.abs(dv) <= 1) return Mat.MOSS_CARPET;
        if (Math.abs(du) == 4 && Math.abs(dv) == 4) {
            if (h >= top - 8) return Mat.CHAIN;
            if (h == top - 9) return Mat.LANTERN;
        }
        if (h == 1 && (Math.abs(du) == 5 && Math.abs(dv) == 0 || Math.abs(dv) == 5 && Math.abs(du) == 0)) return Mat.LANTERN;
        return Mat.AIR;
    }

    private Mat gate(Cell cell, int bu, int bv, int h, int top) {
        // portal frame on the north side: sill y=FLOOR (blockFloor), posts, lintel
        if (bv == 3) {
            if (bu == 6 || bu == 9) return h <= 3 ? Mat.CHISEL : Mat.AIR;
            if ((bu == 7 || bu == 8) && h <= 2) return Mat.PORTAL_X;
            if ((bu == 7 || bu == 8) && h == 3) return Mat.CHISEL;
        }
        int du = bu - 8, dv = bv - 8;
        if (Math.abs(du) == 5 && Math.abs(dv) == 5 && h <= top - 3) return Mat.PILLAR;
        if (Math.abs(du) == 5 && Math.abs(dv) == 5 && h == top - 2) return Mat.LANTERN;
        if (h == 0 && Math.max(Math.abs(du), Math.abs(dv)) <= 3 && bv > 4) return Mat.CARPET;
        if (du == 0 && dv == 3 && h == 0) return Mat.DECON_TABLE;
        if (h == top - 1 && du == 0 && dv == 0) return Mat.LANTERN;
        return Mat.AIR;
    }

    // ---- the newer rooms ----------------------------------------------------------------------------------------

    /** Gallery: a balcony (three wide, at h 6) rings an open well; ladders in two corners; carrels under the balcony. */
    private Mat gallery(Cell cell, int bu, int bv, int h, int top) {
        int du = bu - 8, dv = bv - 8;
        int cheb = Math.max(Math.abs(du), Math.abs(dv));
        boolean flip = ((cell.hash() >>> 5) & 1) == 0;
        // ladders against the wall, up through the balcony
        if (!flip && (bu == 1 && bv == 3 || bu == 15 && bv == 13) || flip && (bv == 1 && bu == 13 || bv == 15 && bu == 3)) {
            if (h > 7) return Mat.AIR;
            if (!flip) return bu == 1 ? Mat.LADDER_E : Mat.LADDER_W;
            return bv == 1 ? Mat.LADDER_S : Mat.LADDER_N;
        }
        boolean newel = Math.abs(du) == 5 && Math.abs(dv) == 5; // corner posts rise through the balcony, a lantern on top
        if (newel && h <= 7) return cell.restricted() ? Mat.PILLAR_RUIN : Mat.PILLAR;
        if (newel && h == 8) return Mat.LANTERN;
        if (cheb >= 5 && h == 6) return Mat.TABLE_TOP;                       // the balcony floor
        if (cheb == 5 && h == 7) return Math.abs(du) == 5 ? Mat.RAIL_Z : Mat.RAIL_X; // its railing
        boolean post = cheb == 5 && (Math.abs(du) == 2 || Math.abs(dv) == 2);
        if (post && h < 6) return cell.restricted() ? Mat.PILLAR_RUIN : Mat.PILLAR;
        if (cheb >= 6 && h < 6) {
            boolean corner = Math.abs(du) >= 6 && Math.abs(dv) >= 6;
            int along = Math.abs(du) >= 6 ? dv : du;
            boolean carrel = !corner && (Math.abs(along) == 3 || Math.abs(along) == 6);
            if (carrel && h <= 3) return h == 3 ? Mat.TRIM : Mat.SHELF;     // shelf partitions between reading nooks
            if (h == 5 && !corner && Math.abs(along) == 4) return Mat.LANTERN; // lit under the balcony
            return Mat.AIR;
        }
        // the well floor: a long reading table under a tall chandelier
        if (dv == 0 && Math.abs(du) <= 2) {
            if (h == 0) return Math.abs(du) == 2 ? Mat.TABLE_LEG : Mat.AIR;
            if (h == 1) return Mat.TABLE_TOP;
            if (h == 2 && du == 0) return Mat.CANDLE;
        }
        if (h == 0 && cheb <= 3) return carpet(cell);
        if (du == 0 && dv == 0 && h >= 9) return h == 9 ? Mat.LANTERN : Mat.CHAIN;
        if (Math.abs(du) == 3 && Math.abs(dv) == 3 && h >= 11) return h == 11 ? Mat.LANTERN : Mat.CHAIN;
        return Mat.AIR;
    }

    /** The catalogue's cabinet rows run east-west or (transposed) north-south. */
    private Mat catalogueAny(Cell cell, int bu, int bv, int h, int top, int x, int z) {
        if (((cell.hash() >>> 35) & 1) == 0) return catalogue(cell, bu, bv, h, top, x, z);
        return transpose(catalogue(cell, bv, bu, h, top, z, x));
    }

    /** Mirror a facing across the u = v diagonal, for a layout drawn with its axes swapped. */
    private static Mat transpose(Mat m) {
        return switch (m) {
            case DRAWER_N -> Mat.DRAWER_W;
            case DRAWER_S -> Mat.DRAWER_E;
            case LECTERN_N -> Mat.LECTERN_W;
            case LECTERN_S -> Mat.LECTERN_E;
            case LECTERN_W -> Mat.LECTERN_N;
            case LECTERN_E -> Mat.LECTERN_S;
            default -> m;
        };
    }

    /** Card catalogue: back-to-back drawer cabinets with a slab top and the odd candle, a desk at each aisle end. */
    private Mat catalogue(Cell cell, int bu, int bv, int h, int top, int x, int z) {
        boolean crossAisle = bu >= 7 && bu <= 9;
        boolean cabinetRow = bv == 3 || bv == 4 || bv == 12 || bv == 13;
        if (cabinetRow && bu >= 2 && bu <= 14 && !crossAisle) {
            boolean endCap = bu == 2 || bu == 14 || bu == 6 || bu == 10;
            if (endCap) return h <= 2 ? (h == 2 ? Mat.TRIM : Mat.SHELF) : Mat.AIR;
            boolean front = bv == 3 || bv == 12; // the drawers face out into the aisles
            if (h <= 1) return front ? Mat.DRAWER_N : Mat.DRAWER_S;
            if (h == 2) return Mat.TABLE_TOP;
            if (h == 3 && unit(x, z, 111) < 0.12) return Mat.CANDLE_WHITE;
            return Mat.AIR;
        }
        if (bv == 8 && (bu == 3 || bu == 13)) return h == 0 ? (bu == 3 ? Mat.LECTERN_E : Mat.LECTERN_W) : Mat.AIR;
        if (h == 0 && bv >= 7 && bv <= 9) return carpet(cell);
        if (h == top - 1 && (bv == 1 || bv == 8 || bv == 15) && bu % 4 == 0) {
            if (cell.restricted() && unit(x, z, 112) < 0.5) return Mat.AIR;
            return Mat.LANTERN;
        }
        if (cell.restricted() && h == top - 2 && unit(x, z, 113) < 0.04) return Mat.COBWEB;
        return Mat.AIR;
    }

    /** Bindery: benches along the walls, a long binding table, ink vats in the corners, drying lines overhead. */
    private Mat bindery(Cell cell, int bu, int bv, int h, int top, int x, int z) {
        int du = bu - 8, dv = bv - 8;
        int cheb = Math.max(Math.abs(du), Math.abs(dv));
        if (cheb == 7 && Math.abs(du) != Math.abs(dv)) {
            int along = Math.abs(du) == 7 ? dv : du;
            if (Math.abs(along) <= 1 || h > 0) return Mat.AIR; // nothing in front of the doors
            return switch (Math.floorMod(along + (Math.abs(du) == 7 ? 0 : 2), 5)) {
                case 0 -> Mat.LOOM;
                case 1 -> Mat.WORKBENCH;
                case 2 -> Mat.PRESS;
                case 3 -> Mat.CARTOGRAPHY;
                default -> Mat.BARREL;
            };
        }
        if (Math.abs(du) == 5 && Math.abs(dv) == 5) return h == 0 ? Mat.VAT : Mat.AIR;
        // the binding table: 7 x 3, slab top on fence legs, paper and a candle on it
        if (Math.abs(du) <= 3 && Math.abs(dv) <= 1) {
            if (h == 0) return Math.abs(du) == 3 && Math.abs(dv) == 1 ? Mat.TABLE_LEG : Mat.AIR;
            if (h == 1) return Mat.TABLE_TOP;
            if (h == 2) {
                if (du == 0 && dv == 0) return Mat.CANDLE_WHITE;
                return unit(x, z, 121) < 0.4 ? Mat.PAPER : Mat.AIR;
            }
            return Mat.AIR;
        }
        if (Math.abs(dv) == 4 && Math.abs(du) == 2 && h == 0) return dv < 0 ? Mat.LECTERN_S : Mat.LECTERN_N; // copy stands
        if (h == top - 2 && Math.abs(dv) == 3 && Math.abs(du) <= 5) return Mat.CHAIN_X; // drying lines
        if (h == top - 1 && (Math.abs(du) == 4 && dv == 0 || du == 0 && Math.abs(dv) == 5)) return Mat.LANTERN;
        if (h == 0 && cheb <= 6 && Math.abs(dv) >= 2 && Math.abs(dv) <= 3) return carpet(cell);
        if (cell.restricted() && h == top - 3 && unit(x, z, 122) < 0.04) return Mat.COBWEB;
        return Mat.AIR;
    }

    /**
     * Orrery: a sun on a chain at h 11 inside three rings (one level, two tilted), planets riding the level ring,
     * crystal pedestals at the diagonals. The ceiling is a night sky (see inBlock) over a compass-rose floor.
     */
    private Mat orrery(Cell cell, int bu, int bv, int h, int top) {
        int du = bu - 8, dv = bv - 8;
        int y = h - 11;
        if (du == 0 && dv == 0) {
            if (y == 0) return Mat.SUN;
            if (y > 0) return Mat.CHAIN;
            return h == 0 ? Mat.CANDLE : Mat.AIR;
        }
        double r3 = Math.sqrt(du * du + dv * dv + y * y);
        if (Math.abs(r3 - 4.6) < 0.55) {
            if (y == 0) { // the level ring, with three planets on it
                double a = Math.toDegrees(Math.atan2(dv, du));
                if (Math.abs(a - 20) < 9) return Mat.PLANET_A;
                if (Math.abs(a + 110) < 9) return Mat.PLANET_B;
                if (Math.abs(Math.abs(a) - 160) < 8) return Mat.PLANET_A;
                return Mat.RING;
            }
            // two rings tilted 50 degrees, about the east-west and the north-south axes
            double c = Math.cos(Math.toRadians(50)), s = Math.sin(Math.toRadians(50));
            if (Math.abs(y * c - dv * s) < 0.5) return Mat.RING_GOLD;
            if (Math.abs(y * c - du * s) < 0.5) return Mat.RING_GOLD;
        }
        if (Math.abs(du) == 6 && Math.abs(dv) == 6) {
            if (h == 0) return Mat.CHISEL;
            if (h == 1) return Mat.CRYSTAL;
            if (h >= top - 6) return Mat.CHAIN;
            if (h == top - 7) return Mat.LANTERN;
        }
        return Mat.AIR;
    }

    /**
     * Sealed archive: shelf rows chained to the vault above and strung with chains along their faces, soul lanterns,
     * and at the heart a barred cage on crying obsidian around the chest. The one way in is vaultFace().
     */
    private Mat sealed(Cell cell, int bu, int bv, int h, int top, int x, int z) {
        int du = bu - 8, dv = bv - 8;
        int cheb = Math.max(Math.abs(du), Math.abs(dv));
        boolean crossAisle = Math.abs(du) <= 1 || Math.abs(dv) <= 1;
        if (cheb == 2) { // the cage, opening toward the door
            boolean gap = switch (vaultFace(cell)) {
                case NORTH -> du == 0 && dv == -2;
                case SOUTH -> du == 0 && dv == 2;
                case WEST -> dv == 0 && du == -2;
                case EAST -> dv == 0 && du == 2;
            };
            if (h <= 2) return gap ? Mat.AIR : Mat.BARS;
            return h == 3 ? Mat.CHISEL : Mat.AIR;
        }
        if (cheb < 2) {
            if (h == 0 && Math.abs(du) == 1 && Math.abs(dv) == 1) return Mat.CANDLE_BLACK;
            if (du == 0 && dv == 0 && h >= 3) return h == 3 ? Mat.SOUL_LANTERN : Mat.CHAIN;
            return Mat.AIR;
        }
        boolean shelfRow = bu % 4 == 2 && bv >= 2 && bv <= 14 && !crossAisle;
        if (shelfRow) {
            if (h <= 4) {
                if (unit(x, z + h * 13, 131) < 0.1) return Mat.SHELF_KNOW;
                return unit(x, z + h, 132) < 0.03 ? Mat.SHELF_SUS : Mat.SHELF;
            }
            return Mat.CHAIN; // chained to the vault above
        }
        // chains strung along the shelf faces, just over head height
        if ((bu % 4 == 1 || bu % 4 == 3) && bv >= 2 && bv <= 14 && !crossAisle && h == 2) return Mat.CHAIN_Z;
        if (h == top - 1 && bu % 4 == 0 && (bv == 4 || bv == 12)) return Mat.SOUL_LANTERN;
        if (h == 0 && crossAisle && cheb <= 6) return Mat.CARPET_DARK;
        if (h >= 3 && h <= top - 2 && unit(x + h * 7, z, 133) < 0.03) return Mat.COBWEB;
        return Mat.AIR;
    }


    // ---- Boss: the Archivist's Sanctum -------------------------------------------------------------------------
    // A clear floor for the Erasure lines and the slam ring; eight pillars at the edge to hide behind (they block bolts
    // and break the Recall tether); four candle-lit ward sockets for the Index; lanterns low enough to light the fight.
    private Mat arena(Cell cell, int bu, int bv, int h, int top) {
        int du = bu - 8, dv = bv - 8;
        double r = Math.sqrt(du * du + dv * dv);
        int[][] spots = {{6, 3}, {6, -3}, {-6, 3}, {-6, -3}, {3, 6}, {-3, 6}, {3, -6}, {-3, -6}};
        for (int[] s : spots) {
            if (du == s[0] && dv == s[1]) {
                if (h <= 6) return h == 3 ? Mat.CHISEL : Mat.PILLAR_RUIN;
                if (h == 7) return Mat.LANTERN;
                return Mat.AIR;
            }
        }
        if (Math.abs(du) == 5 && Math.abs(dv) == 5 && h == 0) return Mat.CANDLE;
        // a chandelier over the dais
        if (du == 0 && dv == 0 && h >= 11) return h == 11 ? Mat.LANTERN : Mat.CHAIN;
        if (Math.abs(du) == 2 && Math.abs(dv) == 2 && h >= 14) return h == 14 ? Mat.LANTERN : Mat.CHAIN;
        if (h == 0 && r >= 8.6 && unit(bu, bv, 71) < 0.35) return Mat.COBWEB; // only in the far corners
        return Mat.AIR;
    }

    // ---- features ----------------------------------------------------------------------------------------------

    /** Chests and mobs for one cell, in world coordinates. */
    public List<Feature> features(Cell cell) {
        List<Feature> out = new ArrayList<>();
        int ox = cell.originX(), oz = cell.originZ(), y = FLOOR + 1;
        switch (cell.kind()) {
            case STACKS -> {
                if (unit(cell.cx(), cell.cz(), 81) < 0.45) out.add(chest(ox + 3, y, oz + 15, "arcane:chests/stacks", "north"));
                if (cell.restricted() && unit(cell.cx(), cell.cz(), 82) < 0.5) out.add(mob(ox + 8, y, oz + 8, "arcane:silent_librarian"));
                if (!cell.restricted() && unit(cell.cx(), cell.cz(), 83) < 0.3) out.add(mob(ox + 8, y, oz + 8, "arcane:silent_librarian"));
            }
            case STUDY -> {
                out.add(chest(ox + 3, y, oz + 3, cell.restricted() ? "arcane:chests/restricted_study" : "arcane:chests/study", "south"));
                if (unit(cell.cx(), cell.cz(), 84) < 0.6) out.add(mob(ox + 8, y, oz + 10, "arcane:silent_librarian"));
            }
            case VAULT -> {
                String loot = cell.restricted() ? "arcane:chests/restricted_vault" : "arcane:chests/vault";
                out.add(chest(ox + 6, y, oz + 6, loot, "south"));
                out.add(chest(ox + 10, y, oz + 10, loot, "north"));
                out.add(mob(ox + 6, y, oz + 10, "arcane:vault_ward"));
                out.add(mob(ox + 10, y, oz + 6, "arcane:vault_ward"));
                if (cell.restricted()) out.add(mob(ox + 8, y, oz + 8, "arcane:vault_ward"));
            }
            case SCRIPTORIUM -> {
                out.add(chest(ox + 15, y, oz + 2, "arcane:chests/scriptorium", "west"));
                out.add(chest(ox + 2, y, oz + 15, "arcane:chests/scriptorium", "east"));
                out.add(mob(ox + 6, y, oz + 6, "arcane:ink_golem"));
                out.add(mob(ox + 10, y, oz + 10, "arcane:ink_golem"));
            }
            case ATRIUM -> {
                out.add(mob(ox + 6, y + 6, oz + 8, "arcane:lumen_moth"));
                out.add(mob(ox + 13, y + 6, oz + 8, "arcane:lumen_moth"));
                out.add(mob(ox + 8, y + 5, oz + 11, "arcane:lumen_moth"));
                if (unit(cell.cx(), cell.cz(), 85) < 0.5) out.add(chest(ox + 3, y, oz + 3, "arcane:chests/atrium", "south"));
            }
            case READING_ROOM -> {
                if (unit(cell.cx(), cell.cz(), 86) < 0.5) out.add(mob(ox + 11, y, oz + 12, "arcane:silent_librarian"));
                out.add(chest(ox + 2, y, oz + 2, "arcane:chests/study", "south"));
            }
            case GALLERY -> {
                out.add(chest(ox + 15, y + 7, oz + 15, "arcane:chests/study", "north")); // in a balcony corner
                if (unit(cell.cx(), cell.cz(), 87) < 0.6) out.add(mob(ox + 8, y, oz + 5, "arcane:silent_librarian"));
                if (unit(cell.cx(), cell.cz(), 88) < 0.5) out.add(mob(ox + 8, y + 4, oz + 8, "arcane:page_wraith"));
                if (unit(cell.cx(), cell.cz(), 89) < 0.35) out.add(mob(ox + 4, y, oz + 8, "arcane:bookwyrm"));
            }
            case CATALOGUE -> {
                out.add(chest(ox + 1, y, oz + 1, cell.restricted() ? "arcane:chests/restricted_study" : "arcane:chests/study", "south"));
                out.add(mob(ox + 8, y, oz + 8, "arcane:silent_librarian"));
                if (unit(cell.cx(), cell.cz(), 90) < 0.4) out.add(mob(ox + 15, y, oz + 15, "arcane:tome_mimic"));
            }
            case BINDERY -> {
                out.add(chest(ox + 2, y, oz + 2, "arcane:chests/scriptorium", "south"));
                out.add(mob(ox + 8, y, oz + 12, "arcane:ink_golem"));
                out.add(mob(ox + 8, y, oz + 4, "arcane:inkling"));
                if (unit(cell.cx(), cell.cz(), 94) < 0.5) out.add(mob(ox + 12, y, oz + 8, "arcane:inkling"));
            }
            case ORRERY -> {
                out.add(chest(ox + 1, y, oz + 13, "arcane:chests/atrium", "east"));
                out.add(mob(ox + 8, y + 6, oz + 14, "arcane:lumen_moth")); // drawn to the sun
                out.add(mob(ox + 2, y + 9, oz + 8, "arcane:lumen_moth"));
                if (unit(cell.cx(), cell.cz(), 95) < 0.4) out.add(mob(ox + 12, y + 2, oz + 12, "arcane:page_wraith"));
            }
            case SEALED_ARCHIVE -> {
                out.add(chest(ox + 8, y, oz + 8, "arcane:chests/restricted_vault", "south")); // in the cage
                out.add(mob(ox + 8, y, oz + 5, "arcane:vault_ward"));
                if (unit(cell.cx(), cell.cz(), 96) < 0.6) out.add(mob(ox + 4, y, oz + 8, "arcane:quill_stalker"));
                if (unit(cell.cx(), cell.cz(), 97) < 0.5) out.add(mob(ox + 12, y + 3, oz + 12, "arcane:page_wraith"));
                if (unit(cell.cx(), cell.cz(), 98) < 0.5) out.add(mob(ox + 8, y, oz + 12, "arcane:bookend_gargoyle"));
            }
            case ARENA -> out.add(mob(ox + 8, y, oz + 8, "arcane:archivist")); // Boss: on the dais
            case GATE_HALL -> { }
        }
        creaturesA(cell, out, ox, y, oz);
        creaturesB(cell, out);
        return out;
    }

    /**
     * Creatures A additions. Variants are chosen at spawn from the position (see entity/CreatureZones): the ward at a
     * vault's heart becomes its Warden of Keys, and the Restricted Section rolls the nastier variants.
     */
    private void creaturesA(Cell cell, List<Feature> out, int ox, int y, int oz) {
        switch (cell.kind()) {
            case SCRIPTORIUM -> {
                out.add(mob(ox + 8, y, oz + 8, "arcane:inkling"));
                if (unit(cell.cx(), cell.cz(), 91) < 0.6) out.add(mob(ox + 7, y, oz + 10, "arcane:inkling"));
            }
            case VAULT -> {
                if (!cell.restricted() && unit(cell.cx(), cell.cz(), 92) < 0.5) out.add(mob(ox + 8, y, oz + 8, "arcane:vault_ward"));
            }
            case STACKS -> {
                if (cell.restricted() && unit(cell.cx(), cell.cz(), 93) < 0.35) out.add(mob(ox + 8, y, oz + 9, "arcane:inkling"));
            }
            case ATRIUM -> {
                if (cell.restricted()) out.add(mob(ox + 10, y + 7, oz + 6, "arcane:lumen_moth"));
            }
            default -> { }
        }
    }

    // ---- Creatures B: where the Page Wraiths, Tome Mimics, Bookwyrms, Quill Stalkers and Gargoyles live ----------
    // Each creature picks its variant from the cell it spawns in (the Restricted Section gets the nasty ones).
    private void creaturesB(Cell cell, List<Feature> out) {
        int ox = cell.originX(), oz = cell.originZ(), y = FLOOR + 1;
        int cx = cell.cx(), cz = cell.cz();
        boolean r = cell.restricted();
        switch (cell.kind()) {
            case STACKS -> {
                if (unit(cx, cz, 191) < (r ? 0.45 : 0.3)) out.add(mob(ox + 5, y, oz + 8, "arcane:bookwyrm"));
                if (unit(cx, cz, 192) < (r ? 0.35 : 0.15)) out.add(mob(ox + 11, y, oz + 3, "arcane:tome_mimic"));
            }
            case STUDY -> {
                if (unit(cx, cz, 193) < 0.5) out.add(mob(ox + 12, y, oz + 5, "arcane:tome_mimic"));
            }
            case READING_ROOM -> {
                if (unit(cx, cz, 194) < 0.6) {
                    out.add(mob(ox + 5, y + 3, oz + 12, "arcane:page_wraith"));
                    out.add(mob(ox + 12, y + 4, oz + 5, "arcane:page_wraith"));
                }
            }
            case SCRIPTORIUM -> {
                if (unit(cx, cz, 195) < 0.6) out.add(mob(ox + 10, y, oz + 6, "arcane:quill_stalker"));
                if (r && unit(cx, cz, 199) < 0.4) out.add(mob(ox + 6, y, oz + 12, "arcane:quill_stalker"));
            }
            case VAULT -> {
                if (unit(cx, cz, 196) < 0.5) out.add(mob(ox + 8, y, oz + 6, "arcane:bookend_gargoyle"));
                if (unit(cx, cz, 200) < 0.3) out.add(mob(ox + 10, y, oz + 8, "arcane:tome_mimic"));
            }
            case ATRIUM -> {
                if (unit(cx, cz, 197) < 0.5) out.add(mob(ox + 11, y + 3, oz + 5, "arcane:page_wraith"));
            }
            default -> { }
        }
        // a gargoyle perched by the pillar at the corner of the plaza, watching the crossing
        if (cell.kind() != Kind.GATE_HALL && cell.kind() != Kind.ARENA && unit(cx, cz, 198) < (r ? 0.25 : 0.1)) {
            out.add(mob(cx * CELL + 1, y, cz * CELL + 1, "arcane:bookend_gargoyle"));
        }
    }
    // ---- end Creatures B ---------------------------------------------------------------------------------------

    private static Feature chest(int x, int y, int z, String loot, String facing) {
        return new Feature(FeatureType.CHEST, x, y, z, loot, facing);
    }

    private static Feature mob(int x, int y, int z, String id) {
        return new Feature(FeatureType.MOB, x, y, z, id, "north");
    }

    /** Where an entity arriving at this gate hall should stand (in front of the portal). */
    public static int[] gateArrival(int cx, int cz) {
        return new int[] {cx * CELL + STREET + 8, FLOOR + 1, cz * CELL + STREET + 6};
    }

    /** The gate hall nearest to a world position. */
    public static int[] nearestGate(int x, int z) {
        int gcx = (int) Math.round(Math.floorDiv(x, CELL) / 8.0) * 8;
        int gcz = (int) Math.round(Math.floorDiv(z, CELL) / 8.0) * 8;
        return new int[] {gcx, gcz};
    }
}
