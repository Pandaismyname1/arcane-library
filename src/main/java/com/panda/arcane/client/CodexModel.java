package com.panda.arcane.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Filtering, sorting and paging for the Codex. Plain Java (no Minecraft types) so it is unit tested. */
public final class CodexModel {
    /** One learned enchantment: a stable key, its display name and the mastery tier (1..3). */
    public record Entry(String key, String name, int tier) {}

    private final int pageSize;
    private List<Entry> all = List.of();
    private String query = "";
    private int tierFilter; // 0 = all
    private int page;
    private String selected;

    public CodexModel(int pageSize) {
        this.pageSize = Math.max(1, pageSize);
    }

    /** Replaces the data, keeping the page and the selection when they still make sense (live sync). */
    public void setEntries(List<Entry> entries) {
        List<Entry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparingInt((Entry e) -> -e.tier()).thenComparing(e -> e.name().toLowerCase(Locale.ROOT)).thenComparing(Entry::key));
        all = List.copyOf(sorted);
        clamp();
    }

    public void setQuery(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.equals(this.query)) return;
        this.query = q;
        page = 0;
        clamp();
    }

    public void setTierFilter(int tier) {
        if (tier == tierFilter) return;
        tierFilter = Math.max(0, Math.min(3, tier));
        page = 0;
        clamp();
    }

    public int tierFilter() {
        return tierFilter;
    }

    public List<Entry> all() {
        return all;
    }

    public List<Entry> filtered() {
        List<Entry> out = new ArrayList<>();
        for (Entry e : all) {
            if (tierFilter != 0 && e.tier() != tierFilter) continue;
            if (!query.isEmpty() && !e.name().toLowerCase(Locale.ROOT).contains(query) && !e.key().toLowerCase(Locale.ROOT).contains(query)) continue;
            out.add(e);
        }
        return out;
    }

    public int pages() {
        return Math.max(1, (filtered().size() + pageSize - 1) / pageSize);
    }

    public int page() {
        return page;
    }

    /** Moves by {@code delta} pages; returns true when the page changed. */
    public boolean turn(int delta) {
        int next = Math.max(0, Math.min(pages() - 1, page + delta));
        if (next == page) return false;
        page = next;
        return true;
    }

    public List<Entry> pageEntries() {
        List<Entry> f = filtered();
        int from = Math.min(f.size(), page * pageSize);
        return f.subList(from, Math.min(f.size(), from + pageSize));
    }

    public Entry selected() {
        for (Entry e : all) if (e.key().equals(selected)) return e;
        return null;
    }

    public void select(String key) {
        selected = key;
    }

    /** Moves the selection through the filtered list, turning pages as needed. */
    public void step(int delta) {
        List<Entry> f = filtered();
        if (f.isEmpty()) return;
        int i = -1;
        for (int k = 0; k < f.size(); k++) if (f.get(k).key().equals(selected)) i = k;
        int next = i < 0 ? 0 : Math.max(0, Math.min(f.size() - 1, i + delta));
        selected = f.get(next).key();
        page = next / pageSize;
    }

    private void clamp() {
        page = Math.max(0, Math.min(pages() - 1, page));
        List<Entry> f = filtered();
        boolean visible = false;
        for (Entry e : f) if (e.key().equals(selected)) visible = true;
        if (!visible && !f.isEmpty() && (selected == null || selected() == null)) selected = f.get(0).key();
    }
}
