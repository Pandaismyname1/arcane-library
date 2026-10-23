package com.panda.arcane.logic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RollTest {
    @Test
    void powerStaysInRange() {
        for (Mastery m : Mastery.values()) {
            for (int max = 1; max <= 10; max++) {
                for (int die = 1; die <= 100; die++) {
                    int p = Roll.power(die, m, 0, max);
                    assertTrue(p >= 1 && p <= max, "die=" + die + " max=" + max + " m=" + m + " p=" + p);
                }
            }
        }
    }

    @Test
    void lowestAndHighestDie() {
        assertEquals(1, Roll.power(1, Mastery.NONE, 0, 5));
        assertEquals(5, Roll.power(100, Mastery.NONE, 0, 5));
        assertEquals(1, Roll.power(1, Mastery.GLIMPSED, 0, 1));
    }

    @Test
    void levelsAreUniformWithoutBonus() {
        int[] counts = new int[6];
        for (int die = 1; die <= 100; die++) counts[Roll.power(die, Mastery.NONE, 0, 5)]++;
        for (int level = 1; level <= 5; level++) assertEquals(20, counts[level]);
    }

    @Test
    void masteryNeverLowersTheOdds() {
        for (int level = 1; level <= 5; level++) {
            double a = Roll.chanceAtLeast(level, Mastery.GLIMPSED, 0, 5);
            double b = Roll.chanceAtLeast(level, Mastery.STUDIED, 0, 5);
            double c = Roll.chanceAtLeast(level, Mastery.MASTERED, 0, 5);
            assertTrue(a <= b && b <= c);
        }
        assertTrue(Roll.chanceAtLeast(5, Mastery.MASTERED, 0, 5) > Roll.chanceAtLeast(5, Mastery.GLIMPSED, 0, 5));
    }

    @Test
    void refineNeverLowersResult() {
        java.util.Random rng = new java.util.Random(42);
        for (int i = 0; i < 2000; i++) {
            int current = 1 + rng.nextInt(5);
            assertTrue(Roll.refine(rng::nextInt, current, Mastery.STUDIED, 0, 5) >= current);
        }
    }

    @Test
    void moreRollsMoreLikelyToHitTheTop() {
        double one = Roll.chanceAtLeast(5, Mastery.GLIMPSED, 0, 5, 1);
        double two = Roll.chanceAtLeast(5, Mastery.GLIMPSED, 0, 5, 2);
        double four = Roll.chanceAtLeast(5, Mastery.GLIMPSED, 0, 5, 4);
        assertTrue(one < two && two < four);
        assertEquals(0.2, one, 1e-9);
    }

    @Test
    void costs() {
        assertEquals(4, Costs.inscribe(4, 10));
        assertEquals(6, Costs.inscribe(4, 1));
        assertEquals(2, Costs.refine(0));
        assertEquals(4, Costs.refine(2));
        assertEquals(3, Costs.salvage(0));
        assertEquals(21, Costs.salvage(7));
    }

    @Test
    void masteryTiers() {
        assertEquals(Mastery.GLIMPSED, Mastery.ofTier(1));
        assertEquals(Mastery.MASTERED, Mastery.ofTier(99));
        assertEquals(Mastery.NONE, Mastery.ofTier(-1));
        assertEquals(Mastery.STUDIED, Mastery.GLIMPSED.next());
        assertEquals(Mastery.MASTERED, Mastery.MASTERED.next());
    }
}
