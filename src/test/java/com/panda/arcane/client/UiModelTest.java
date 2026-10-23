package com.panda.arcane.client;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

/** The pure parts of the UI: the roll reveal timeline and the Codex's filter and paging. */
class UiModelTest {
    @Test
    void revealEndsOnTheServersResult() {
        for (int max = 1; max <= 10; max++) {
            for (int target = 1; target <= max; target++) {
                RollReveal r = new RollReveal(0, target, max, 42L);
                assertEquals(0, r.litPips(0F));
                assertTrue(r.spinning(0F));
                assertEquals(target, r.litPips(r.revealEnd()), "reveal must land on the synced power");
                assertEquals(target, r.litPips(r.end() + 100F));
                assertTrue(r.done(r.end()));
                for (float t = 0F; t < r.end(); t += 0.25F) {
                    assertTrue(r.litPips(t) <= target, "never shows more than the result");
                    int spin = r.spinValue(t);
                    assertTrue(spin >= 1 && spin <= max, "spin stays in range");
                }
            }
        }
    }

    @Test
    void revealLightsPipsOneByOne() {
        RollReveal r = new RollReveal(2, 4, 5, 7L);
        int last = 0;
        for (float t = r.revealStart(); t <= r.revealEnd(); t += 0.5F) {
            int lit = r.litPips(t);
            assertTrue(lit >= last && lit - last <= 1, "pips light one at a time");
            last = lit;
        }
        assertTrue(r.refine());
        assertTrue(r.improved());
        assertFalse(new RollReveal(4, 4, 5, 7L).improved());
    }

    @Test
    void glowFadesAfterTheReveal() {
        RollReveal r = new RollReveal(0, 3, 5, 1L);
        assertEquals(0F, r.glow(r.revealEnd() - 0.1F));
        assertTrue(r.glow(r.revealEnd()) > 0.9F);
        assertTrue(r.glow(r.revealEnd() + RollReveal.SETTLE_TICKS / 2F) < r.glow(r.revealEnd()));
        assertEquals(0F, r.glow(r.end()));
    }

    private static CodexModel model() {
        CodexModel m = new CodexModel(3);
        m.setEntries(List.of(
                new CodexModel.Entry("minecraft:sharpness", "Sharpness", 2),
                new CodexModel.Entry("minecraft:mending", "Mending", 3),
                new CodexModel.Entry("minecraft:looting", "Looting", 2),
                new CodexModel.Entry("minecraft:smite", "Smite", 1),
                new CodexModel.Entry("arcane:ink_bound", "Ink Bound", 1),
                new CodexModel.Entry("minecraft:unbreaking", "Unbreaking", 1),
                new CodexModel.Entry("minecraft:efficiency", "Efficiency", 3)));
        return m;
    }

    @Test
    void codexSortsByTierThenName() {
        List<String> names = model().all().stream().map(CodexModel.Entry::name).toList();
        assertEquals(List.of("Efficiency", "Mending", "Looting", "Sharpness", "Ink Bound", "Smite", "Unbreaking"), names);
    }

    @Test
    void codexPagesAndTurns() {
        CodexModel m = model();
        assertEquals(3, m.pages());
        assertEquals(3, m.pageEntries().size());
        assertFalse(m.turn(-1), "no page before the first");
        assertTrue(m.turn(1));
        assertTrue(m.turn(1));
        assertEquals(1, m.pageEntries().size());
        assertFalse(m.turn(1), "no page after the last");
    }

    @Test
    void codexFiltersByTierAndSearch() {
        CodexModel m = model();
        m.setTierFilter(1);
        assertEquals(3, m.filtered().size());
        assertEquals(0, m.page());
        m.setQuery("ING");
        assertEquals(List.of("Unbreaking"), m.filtered().stream().map(CodexModel.Entry::name).toList());
        m.setTierFilter(0);
        assertEquals(List.of("Mending", "Looting", "Unbreaking"), m.filtered().stream().map(CodexModel.Entry::name).toList());
        m.setQuery("arcane:");
        assertEquals(List.of("Ink Bound"), m.filtered().stream().map(CodexModel.Entry::name).toList(), "search matches ids too");
        m.setQuery("zzz");
        assertTrue(m.pageEntries().isEmpty());
        assertEquals(1, m.pages());
    }

    @Test
    void codexSelectionSurvivesResyncAndSteps() {
        CodexModel m = model();
        assertEquals("minecraft:efficiency", m.selected().key(), "first entry selected by default");
        m.select("minecraft:smite");
        m.setEntries(model().all()); // a live resync must not move the selection
        assertEquals("minecraft:smite", m.selected().key());
        m.step(1);
        assertEquals("minecraft:unbreaking", m.selected().key());
        assertEquals(2, m.page(), "stepping turns to the page of the selection");
        m.step(10);
        assertEquals("minecraft:unbreaking", m.selected().key(), "clamped at the end");
    }
}
