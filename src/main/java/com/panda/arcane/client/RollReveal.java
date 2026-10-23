package com.panda.arcane.client;

/**
 * Timeline of the lectern's roll animation. Pure maths on a time in ticks since the roll arrived, so it is unit
 * tested and never decides anything: the result was already chosen by the server and synced before this starts.
 *
 * <pre>
 * 0 ........ SPIN_TICKS            the pips flicker through random powers, slowing down
 * SPIN ..... SPIN + target*STEP     the real pips light one by one (a chime each)
 * ...        + SETTLE_TICKS         a glow pulses over the result, then the screen is idle again
 * </pre>
 */
public record RollReveal(int previous, int target, int max, long seed) {
    public static final int SPIN_TICKS = 14;
    public static final int STEP_TICKS = 2;
    public static final int SETTLE_TICKS = 14;
    /** Spin frames: the flicker changes on these ticks, further apart as it slows. */
    private static final int[] SPIN_CHANGES = {0, 1, 2, 3, 4, 6, 8, 10, 13};

    public float revealStart() {
        return SPIN_TICKS;
    }

    public float revealEnd() {
        return SPIN_TICKS + target * STEP_TICKS;
    }

    public float end() {
        return revealEnd() + SETTLE_TICKS;
    }

    public boolean spinning(float t) {
        return t < SPIN_TICKS;
    }

    public boolean revealed(float t) {
        return t >= revealEnd();
    }

    public boolean done(float t) {
        return t >= end();
    }

    /** Pips lit at time t during the reveal (0 while spinning, target once revealed). */
    public int litPips(float t) {
        if (t < SPIN_TICKS) return 0;
        int n = 1 + (int) ((t - SPIN_TICKS) / STEP_TICKS);
        return Math.max(0, Math.min(target, n));
    }

    /** The random-looking power shown while spinning; deterministic in (seed, frame) so it never flickers per frame. */
    public int spinValue(float t) {
        int frame = 0;
        for (int i = 0; i < SPIN_CHANGES.length; i++) if (t >= SPIN_CHANGES[i]) frame = i;
        long h = seed * 0x9E3779B97F4A7C15L + frame * 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 31);
        int value = 1 + (int) Math.floorMod(h, (long) Math.max(1, max));
        return Math.max(1, Math.min(max, value));
    }

    /** Glow strength 0..1 after the reveal: a quick flash that fades. */
    public float glow(float t) {
        if (t < revealEnd() || t >= end()) return 0F;
        float k = (t - revealEnd()) / SETTLE_TICKS;
        return (1F - k) * (1F - k);
    }

    public boolean improved() {
        return target > previous;
    }

    public boolean refine() {
        return previous > 0;
    }
}
