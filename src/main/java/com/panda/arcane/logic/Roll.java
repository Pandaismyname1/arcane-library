package com.panda.arcane.logic;

import java.util.function.IntUnaryOperator;

/** The roll that decides how powerful an inscribed enchantment becomes. */
public final class Roll {
    private Roll() {}

    /** Rolls a 1..100 die. */
    public static int d100(IntUnaryOperator nextInt) {
        return nextInt.applyAsInt(100) + 1;
    }

    /** Maps a die result plus mastery bonus onto a power level in 1..maxLevel, uniformly across levels. */
    public static int power(int die, Mastery mastery, int quillBonus, int maxLevel) {
        if (maxLevel <= 1) return 1;
        int score = Math.max(1, Math.min(100, die + mastery.rollBonus() + quillBonus));
        int level = (score * maxLevel + 99) / 100; // ceil(score * maxLevel / 100)
        return Math.max(1, Math.min(maxLevel, level));
    }

    /** First roll of an inscription. */
    public static int inscribe(IntUnaryOperator random, Mastery mastery, int quillBonus, int maxLevel) {
        return power(d100(random), mastery, quillBonus, maxLevel);
    }

    /** A refinement rerolls and keeps whichever result is better. */
    public static int refine(IntUnaryOperator random, int current, Mastery mastery, int quillBonus, int maxLevel) {
        return Math.max(current, inscribe(random, mastery, quillBonus, maxLevel));
    }

    /** Probability of reaching at least {@code level} in one roll. Used by the UI and the tests. */
    public static double chanceAtLeast(int level, Mastery mastery, int quillBonus, int maxLevel) {
        if (maxLevel <= 1 || level <= 1) return 1.0;
        int hits = 0;
        for (int die = 1; die <= 100; die++) {
            if (power(die, mastery, quillBonus, maxLevel) >= level) hits++;
        }
        return hits / 100.0;
    }

    /** Probability of reaching at least {@code level} after {@code rolls} total rolls (inscribe + refinements). */
    public static double chanceAtLeast(int level, Mastery mastery, int quillBonus, int maxLevel, int rolls) {
        double single = chanceAtLeast(level, mastery, quillBonus, maxLevel);
        return 1.0 - Math.pow(1.0 - single, Math.max(1, rolls));
    }
}
