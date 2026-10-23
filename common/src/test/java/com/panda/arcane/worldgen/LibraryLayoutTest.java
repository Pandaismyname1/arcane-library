package com.panda.arcane.worldgen;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LibraryLayoutTest {
    private static final LibraryLayout L = new LibraryLayout(12345L);
    private static final int Y = LibraryLayout.FLOOR + 1;

    private static boolean clear(int x, int y, int z) {
        return L.blockAt(x, y, z).passable();
    }

    private static boolean standable(int x, int z) {
        return L.blockAt(x, LibraryLayout.FLOOR, z) != Mat.AIR && clear(x, Y, z) && clear(x, Y + 1, z);
    }

    /** A step up onto a 1-high block (dais, sill) is allowed. */
    private static boolean stepUp(int x, int z) {
        return !L.blockAt(x, Y, z).passable() && L.blockAt(x, Y, z) != Mat.AIR
                && clear(x, Y + 1, z) && clear(x, Y + 2, z) && clear(x, Y + 3, z);
    }

    private static Set<Long> reachable(int fromX, int fromZ, int radius) {
        Set<Long> seen = new HashSet<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[] {fromX, fromZ});
        seen.add(key(fromX, fromZ));
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            for (int[] d : dirs) {
                int nx = p[0] + d[0], nz = p[1] + d[1];
                if (Math.abs(nx - fromX) > radius || Math.abs(nz - fromZ) > radius) continue;
                if (seen.contains(key(nx, nz))) continue;
                if (standable(nx, nz) || stepUp(nx, nz)) {
                    seen.add(key(nx, nz));
                    queue.add(new int[] {nx, nz});
                }
            }
        }
        return seen;
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    @Test
    void deterministic() {
        LibraryLayout a = new LibraryLayout(7), b = new LibraryLayout(7), c = new LibraryLayout(8);
        int diff = 0;
        for (int x = -50; x < 50; x++) for (int z = -50; z < 50; z++) {
            assertEquals(a.blockAt(x, 10, z), b.blockAt(x, 10, z));
            if (a.blockAt(x, 12, z) != c.blockAt(x, 12, z)) diff++;
        }
        assertTrue(diff > 100, "different seeds should give different libraries");
    }

    @Test
    void streetsAreOpenAndFloored() {
        for (int x = -100; x < 100; x++) {
            int z = 3; // an east-west street (lv < 7)
            assertNotEquals(Mat.AIR, L.blockAt(x, LibraryLayout.FLOOR, z));
            for (int h = 0; h < 2; h++) {
                Mat m = L.blockAt(x, Y + h, z);
                assertTrue(m.passable(), "street blocked at x=" + x + " h=" + h + ": " + m);
            }
        }
    }

    @Test
    void floorEverywhereAndCeilingSealsTheWorld() {
        for (int x = -60; x < 60; x += 3) for (int z = -60; z < 60; z += 3) {
            assertNotEquals(Mat.AIR, L.blockAt(x, LibraryLayout.FLOOR, z));
            assertNotEquals(Mat.AIR, L.blockAt(x, LibraryLayout.MAX_Y, z), "open sky at " + x + "," + z);
        }
    }

    @Test
    void bedrockFloorAndRoof() {
        for (int x = -80; x < 80; x += 7) for (int z = -80; z < 80; z += 5) {
            assertEquals(Mat.BEDROCK, L.blockAt(x, 0, z), "no bedrock floor at " + x + "," + z);
            assertEquals(Mat.BEDROCK, L.blockAt(x, LibraryLayout.ROOF, z), "no bedrock roof at " + x + "," + z);
            assertEquals(Mat.AIR, L.blockAt(x, LibraryLayout.ROOF + 1, z), "the roof must be the top of the Library");
            for (int y = 1; y < LibraryLayout.ROOF; y++) assertNotEquals(Mat.BEDROCK, L.blockAt(x, y, z), "stray bedrock at y " + y);
        }
    }

    @Test
    void everythingImportantIsReachableFromTheGate() {
        int[] start = LibraryLayout.gateArrival(0, 0);
        assertTrue(standable(start[0], start[2]), "arrival point must be standable");
        Set<Long> reach = reachable(start[0], start[2], 220);
        int chests = 0, missing = 0;
        StringBuilder report = new StringBuilder();
        for (int cx = -8; cx <= 8; cx++) for (int cz = -8; cz <= 8; cz++) {
            LibraryLayout.Cell cell = L.cell(cx, cz);
            for (LibraryLayout.Feature f : L.features(cell)) {
                if (f.type() != LibraryLayout.FeatureType.CHEST) continue;
                chests++;
                boolean ok = false;
                for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    if (reach.contains(key(f.x() + d[0], f.z() + d[1]))) ok = true;
                }
                if (!ok) {
                    missing++;
                    report.append(cell.kind()).append(" chest at ").append(f.x()).append(',').append(f.z()).append('\n');
                }
            }
        }
        assertTrue(chests > 20, "expected plenty of chests, got " + chests);
        assertEquals(0, missing, "unreachable chests:\n" + report);
    }

    @Test
    void everyCreatureStartsInOpenAir() {
        int checked = 0;
        for (int cx = -9; cx <= 9; cx++) for (int cz = -9; cz <= 9; cz++) {
            for (LibraryLayout.Feature f : L.features(L.cell(cx, cz))) {
                if (f.type() != LibraryLayout.FeatureType.MOB) continue;
                checked++;
                assertTrue(L.blockAt(f.x(), f.y(), f.z()).passable() && L.blockAt(f.x(), f.y() + 1, f.z()).passable(),
                        f.data() + " would spawn inside a block at " + f.x() + "," + f.y() + "," + f.z() + ": " + L.blockAt(f.x(), f.y(), f.z()));
            }
        }
        assertTrue(checked > 30);
    }

    @Test
    void arenaAndLecternsExist() {
        int arenas = 0, lecterns = 0, vaults = 0, gates = 0;
        for (int cx = -12; cx <= 12; cx++) for (int cz = -12; cz <= 12; cz++) {
            switch (L.cell(cx, cz).kind()) {
                case ARENA -> arenas++;
                case READING_ROOM -> lecterns++;
                case VAULT -> vaults++;
                case GATE_HALL -> gates++;
                default -> { }
            }
        }
        assertTrue(arenas >= 2 && lecterns >= 10 && vaults >= 5 && gates >= 4,
                "arenas=" + arenas + " reading=" + lecterns + " vaults=" + vaults + " gates=" + gates);
    }

    @Test
    void everyRoomKindAppears() {
        java.util.EnumMap<LibraryLayout.Kind, Integer> seen = new java.util.EnumMap<>(LibraryLayout.Kind.class);
        for (int cx = -14; cx <= 14; cx++) for (int cz = -14; cz <= 14; cz++) {
            LibraryLayout.Cell c = L.cell(cx, cz);
            seen.merge(c.kind(), 1, Integer::sum);
            if (c.kind() == LibraryLayout.Kind.SEALED_ARCHIVE) assertTrue(c.restricted(), "a sealed archive outside the Restricted Section");
        }
        for (LibraryLayout.Kind k : LibraryLayout.Kind.values()) assertTrue(seen.getOrDefault(k, 0) >= 2, "too few " + k + ": " + seen);
    }

    @Test
    void returnPortalsAreCommon() {
        int gates = 0, wayside = 0;
        for (int cx = -12; cx <= 12; cx++) for (int cz = -12; cz <= 12; cz++) {
            if (L.cell(cx, cz).kind() != LibraryLayout.Kind.GATE_HALL) continue;
            gates++;
            if (!LibraryLayout.isGate(cx, cz)) wayside++;
        }
        assertTrue(wayside >= 15 && gates >= 24, "gate halls " + gates + ", wayside " + wayside + " in 625 cells");
    }

    @Test
    void dumpSlices() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int y : new int[] {Y, Y + 1, Y + 4}) {
            sb.append("== y=").append(y).append(" (x -30..130, z -30..80)\n");
            for (int z = -30; z < 80; z++) {
                for (int x = -30; x < 130; x++) sb.append(glyph(L.blockAt(x, y, z)));
                sb.append('\n');
            }
        }
        Path out = Path.of("build", "library_layout.txt");
        Files.createDirectories(out.getParent());
        Files.writeString(out, sb.toString());
    }

    private static char glyph(Mat m) {
        return switch (m) {
            case AIR -> ' ';
            case BASE -> '#';
            case SHELF, SHELF_KNOW, SHELF_CHISEL -> 'b';
            case TRIM, CHISEL -> 'T';
            case PILLAR, PILLAR_RUIN -> 'O';
            case CARPET, CARPET_DARK -> '.';
            case LANTERN -> '*';
            case CHAIN -> '|';
            case ARCANE_LECTERN -> 'L';
            case PORTAL_X, PORTAL_Z -> 'P';
            case BARS -> '=';
            case TABLE_LEG, TABLE_TOP -> 't';
            case LECTERN_N, LECTERN_S, LECTERN_E, LECTERN_W -> 'l';
            case BARREL -> 'B';
            case COBWEB -> '~';
            case LOG, LEAVES, LEAVES_PLAIN -> 'Y';
            case DECON_TABLE -> 'D';
            default -> '?';
        };
    }
}
