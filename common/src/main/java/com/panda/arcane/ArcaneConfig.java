package com.panda.arcane;

/** The mod's settings. Each loader keeps them in its own config system and plugs it in with {@link #use}. */
public final class ArcaneConfig {
    /** Where the values come from. The defaults apply until the loader's config is loaded. */
    public interface Source {
        /** Replace the vanilla enchanting table with the Arcane Lectern (the table shows a hint instead of opening). */
        boolean disableVanillaTable();

        /** Ticks between Ink Bound repair pulses. */
        int inkBoundInterval();

        /** Ticks an entity must stand in a Library portal before travelling. */
        int portalDelay();
    }

    public static final Source DEFAULTS = new Source() {
        @Override public boolean disableVanillaTable() { return true; }
        @Override public int inkBoundInterval() { return 100; }
        @Override public int portalDelay() { return 60; }
    };

    private static Source source = DEFAULTS;

    private ArcaneConfig() {}

    public static void use(Source loaderConfig) {
        source = loaderConfig;
    }

    public static boolean disableVanillaTable() {
        return source.disableVanillaTable();
    }

    public static int inkBoundInterval() {
        return source.inkBoundInterval();
    }

    public static int portalDelay() {
        return source.portalDelay();
    }
}
