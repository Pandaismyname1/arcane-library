package com.panda.arcane.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.panda.arcane.entity.ArchivistPattern.Move;
import com.panda.arcane.worldgen.LibraryLayout;
import com.panda.arcane.worldgen.Mat;
import org.junit.jupiter.api.Test;

/** Boss: the Archivist's numbers and geometry, and the Sanctum the fight needs. */
class ArchivistPatternTest {
    @Test
    void phasesOnlyMoveForward() {
        assertEquals(1, ArchivistPattern.phaseFor(1.0F, 1));
        assertEquals(2, ArchivistPattern.phaseFor(0.6F, 1));
        assertEquals(3, ArchivistPattern.phaseFor(0.2F, 1));
        assertEquals(3, ArchivistPattern.phaseFor(0.9F, 3), "healing never returns to an earlier phase");
        assertEquals(2, ArchivistPattern.phaseFor(1.0F, 2));
    }

    @Test
    void everyAttackHasAReadableTelegraph() {
        for (Move m : new Move[] {Move.STORM, Move.ERASURE, Move.RECALL, Move.SLAM, Move.INDEX}) {
            for (int phase = 1; phase <= 3; phase++) {
                assertTrue(ArchivistPattern.windup(m, phase) >= 20, m + " telegraph in phase " + phase + " is under a second");
            }
        }
    }

    @Test
    void phasesChangeThePatternNotJustTheNumbers() {
        assertEquals(1, ArchivistPattern.erasureYaws(1, 0).length);
        assertEquals(2, ArchivistPattern.erasureYaws(2, 0).length);
        assertEquals(4, ArchivistPattern.erasureYaws(3, 0).length);
        assertTrue(ArchivistPattern.active(Move.STORM, 3) > ArchivistPattern.active(Move.STORM, 2), "phase 3 storm is a sustained spiral");
        assertTrue(ArchivistPattern.wardsFor(3) > ArchivistPattern.wardsFor(2));
    }

    @Test
    void erasureBandCanBeSteppedOutOf() {
        // a line heading +Z (yaw 0) from the origin
        assertTrue(ArchivistPattern.inBand(0, 0, 0F, 0.0, 6.0));
        assertTrue(ArchivistPattern.inBand(0, 0, 0F, 0.9, 6.0));
        assertFalse(ArchivistPattern.inBand(0, 0, 0F, 2.0, 6.0), "two blocks to the side is safe");
        assertFalse(ArchivistPattern.inBand(0, 0, 0F, 0.0, -3.0), "behind the Archivist is safe");
        assertFalse(ArchivistPattern.inBand(0, 0, 0F, 0.0, ArchivistPattern.ERASE_LENGTH + 2), "past the end is safe");
        // yawTo and the band agree on direction
        float yaw = ArchivistPattern.yawTo(0, 0, 5, 5);
        assertTrue(ArchivistPattern.inBand(0, 0, yaw, 5, 5));
        assertFalse(ArchivistPattern.inBand(0, 0, yaw, -5, -5));
    }

    @Test
    void sanctumSupportsTheMechanics() {
        LibraryLayout l = new LibraryLayout(12345L);
        int cx = 5, cz = 0; // an arena cell (isArena)
        assertEquals(LibraryLayout.Kind.ARENA, l.cell(cx, cz).kind());
        int ox = cx * LibraryLayout.CELL + LibraryLayout.STREET, oz = cz * LibraryLayout.CELL + LibraryLayout.STREET;
        int x0 = ox + 8, z0 = oz + 8, y = LibraryLayout.FLOOR + 1;
        // the dais is clear floor, the slam ring is marked, the ward sockets carry candles
        assertEquals(Mat.AIR, l.blockAt(x0, y, z0));
        assertEquals(Mat.CHISEL, l.blockAt(x0, LibraryLayout.FLOOR, z0));
        assertEquals(Mat.TRIM, l.blockAt(x0 + 5, LibraryLayout.FLOOR, z0));
        for (int[] s : new int[][] {{5, 5}, {-5, 5}, {5, -5}, {-5, -5}}) {
            assertEquals(Mat.CANDLE, l.blockAt(x0 + s[0], y, z0 + s[1]));
            assertTrue(l.blockAt(x0 + s[0], y + 1, z0 + s[1]).passable(), "a ward can appear over each socket");
        }
        // pillars give cover at least three blocks high
        for (int h = 0; h < 3; h++) assertFalse(l.blockAt(x0 + 6, y + h, z0 + 3).passable());
        // nothing slows a dodge between the dais and the pillars
        for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) {
            Mat m = l.blockAt(x0 + dx, y, z0 + dz);
            assertTrue(m == Mat.AIR || m == Mat.CANDLE, "obstacle in the fighting floor at " + dx + "," + dz + ": " + m);
        }
    }
}
