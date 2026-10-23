package com.panda.arcane.logic;

/** How well a player understands one enchantment. Pure logic, no Minecraft types. */
public enum Mastery {
    NONE(0, 0, 0),
    GLIMPSED(1, 1, 0),
    STUDIED(2, 2, 8),
    MASTERED(3, 3, 16);

    public static final int MAX_TIER = 3;

    private final int tier;
    private final int refinements;
    private final int rollBonus;

    Mastery(int tier, int refinements, int rollBonus) {
        this.tier = tier;
        this.refinements = refinements;
        this.rollBonus = rollBonus;
    }

    public int tier() { return tier; }
    /** How many times the roll can be refined (rerolled keeping the better result). */
    public int refinements() { return refinements; }
    /** Flat bonus added to the d100 before it is mapped to a power level. */
    public int rollBonus() { return rollBonus; }
    public boolean known() { return this != NONE; }

    public static Mastery ofTier(int tier) {
        for (Mastery m : values()) {
            if (m.tier == tier) return m;
        }
        return tier <= 0 ? NONE : MASTERED;
    }

    public Mastery next() {
        return ofTier(Math.min(MAX_TIER, tier + 1));
    }
}
