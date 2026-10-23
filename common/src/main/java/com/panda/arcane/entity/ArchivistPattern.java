package com.panda.arcane.entity;

/**
 * Pure numbers and geometry for the Archivist's fight, kept free of Minecraft classes so JUnit can check them.
 *
 * <p>Every telegraph length here is also the length of the matching wind-up clip in
 * {@code client.boss.ArchivistAnimations}: change both together.
 */
public final class ArchivistPattern {
    private ArchivistPattern() {}

    /** The Archivist's attacks. Ids are synced to the client, which plays the matching clip. */
    public enum Move {
        NONE, STORM, ERASURE, RECALL, SLAM, INDEX, TRANSITION, STAGGER;

        public static Move byId(int id) {
            Move[] all = values();
            return id >= 0 && id < all.length ? all[id] : NONE;
        }
    }

    /** Health fractions at which phases 2 and 3 begin. */
    public static final float PHASE_2_AT = 0.66F;
    public static final float PHASE_3_AT = 0.33F;

    /** Erasure: half the width of the band the wave clears, in blocks. Step more than this off the line. */
    public static final double ERASE_HALF_WIDTH = 1.1;
    /** Erasure: how far the line runs from the Archivist. */
    public static final double ERASE_LENGTH = 17.0;
    /** Erasure: the wave covers this many blocks per tick once released. */
    public static final double ERASE_SPEED = 2.2;
    /** Page Slam: radius of the shockwave. The floor ring in the arena marks it. */
    public static final double SLAM_RADIUS = 5.0;
    /** Recall: the tether snaps if the target is farther than this. */
    public static final double RECALL_RANGE = 20.0;
    /** Recall: ticks the pull takes. */
    public static final int PULL_TICKS = 6;
    /** Storm: the aim follows the target until this many ticks before release, then locks. */
    public static final int STORM_LOCK_BEFORE = 10;

    /** The phase for a health fraction. Phases only move forward: healing never returns the fight to an earlier one. */
    public static int phaseFor(float healthFraction, int current) {
        int want = healthFraction <= PHASE_3_AT ? 3 : healthFraction <= PHASE_2_AT ? 2 : 1;
        return Math.max(Math.max(1, current), want);
    }

    /** Ticks of telegraph before the move lands. */
    public static int windup(Move move, int phase) {
        return switch (move) {
            case STORM -> 30;
            case ERASURE -> phase >= 3 ? 34 : 40;
            case RECALL -> phase >= 3 ? 26 : 30;
            case SLAM -> 24;
            case INDEX -> 40;
            case TRANSITION -> 50;
            case STAGGER -> 50;
            case NONE -> 0;
        };
    }

    /** Ticks the move keeps acting after its wind-up (volleys in flight, the wave crossing the room, the pull). */
    public static int active(Move move, int phase) {
        return switch (move) {
            case STORM -> phase >= 3 ? 40 : phase == 2 ? 12 : 1;
            case ERASURE -> (int) Math.ceil(ERASE_LENGTH / ERASE_SPEED) + 1;
            case RECALL -> PULL_TICKS;
            case SLAM -> 1;
            case INDEX -> 1;
            default -> 0;
        };
    }

    /** Ticks after the move before the Archivist acts again (a short opening to punish). */
    public static int recover(Move move, int phase) {
        return switch (move) {
            case STORM -> 14;
            case ERASURE -> 12;
            case RECALL -> 0; // flows straight into the Page Slam
            case SLAM -> phase >= 3 ? 14 : 22;
            case INDEX -> 10;
            default -> 0;
        };
    }

    /** Pause between moves. Shrinks as the fight goes on. */
    public static int globalCooldown(int phase) {
        return phase >= 3 ? 22 : phase == 2 ? 32 : 44;
    }

    /** Per-move cooldown once used. */
    public static int cooldown(Move move, int phase) {
        return switch (move) {
            case STORM -> phase >= 3 ? 120 : 110;
            case ERASURE -> phase >= 3 ? 120 : 150;
            case RECALL -> phase >= 3 ? 170 : 210;
            case SLAM -> phase >= 3 ? 90 : 120;
            case INDEX -> phase >= 3 ? 560 : 480;
            default -> 0;
        };
    }

    /** How many Vault Wards the Index calls. */
    public static int wardsFor(int phase) {
        return phase >= 3 ? 3 : 2;
    }

    /**
     * Yaws (degrees, Minecraft convention) of the Erasure lines for a phase, given the yaw from the Archivist to its
     * target. Phase 1: one line through the target. Phase 2: a V either side of it, so standing still between the arms
     * is safe but the arms close the flanks. Phase 3: a cross through the target.
     */
    public static float[] erasureYaws(int phase, float aimYaw) {
        if (phase >= 3) return new float[] {aimYaw, aimYaw + 90F, aimYaw + 180F, aimYaw + 270F};
        if (phase == 2) return new float[] {aimYaw - 24F, aimYaw + 24F};
        return new float[] {aimYaw};
    }

    /** Yaw offsets (degrees) of the aimed fan of bolts. */
    public static float[] fanOffsets(int phase) {
        return phase >= 2 ? new float[] {-16F, 0F, 16F} : new float[] {-11F, 0F, 11F};
    }

    /** Number of bolts in the Phase 2 ring. Hiding straight behind a pillar is the counter. */
    public static final int RING_BOLTS = 20;

    /**
     * Distance from point (px, pz) to the segment that starts at (ox, oz), heads along yaw for {@code length}, and
     * also the position along it. Returns {distance, along}. Minecraft yaw: 0 faces +Z, 90 faces -X.
     */
    public static double[] toLine(double ox, double oz, float yawDeg, double length, double px, double pz) {
        double yaw = Math.toRadians(yawDeg);
        double dx = -Math.sin(yaw), dz = Math.cos(yaw);
        double rx = px - ox, rz = pz - oz;
        double along = rx * dx + rz * dz;
        double clamped = Math.max(0, Math.min(length, along));
        double cx = ox + dx * clamped - px, cz = oz + dz * clamped - pz;
        return new double[] {Math.sqrt(cx * cx + cz * cz), along};
    }

    /** True when (px, pz) lies inside the band an Erasure line clears. */
    public static boolean inBand(double ox, double oz, float yawDeg, double px, double pz) {
        double[] d = toLine(ox, oz, yawDeg, ERASE_LENGTH, px, pz);
        return d[1] >= -0.5 && d[1] <= ERASE_LENGTH && d[0] <= ERASE_HALF_WIDTH;
    }

    /** Minecraft yaw that faces from (ax, az) toward (bx, bz). */
    public static float yawTo(double ax, double az, double bx, double bz) {
        return (float) Math.toDegrees(Math.atan2(-(bx - ax), bz - az));
    }
}
