package com.panda.arcane.logic;

/** Lumen Dust prices at the Arcane Lectern. */
public final class Costs {
    private Costs() {}

    /** Dust to start an inscription. Rare enchantments (low vanilla weight) cost extra. */
    public static int inscribe(int maxLevel, int weight) {
        int base = 2 + Math.max(1, maxLevel) / 2;
        return weight <= 2 ? base + 2 : base;
    }

    /** Dust for the next refinement, given how many have been used already. */
    public static int refine(int refinesUsed) {
        return 2 + Math.max(0, refinesUsed);
    }

    /** Dust returned for deconstructing an item: three per enchantment level. */
    public static int salvage(int totalLevels) {
        return 3 * Math.max(1, Math.min(12, totalLevels));
    }
}
