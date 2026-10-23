package com.panda.arcane;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ArcaneConfig {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue DISABLE_VANILLA_TABLE = B
            .comment("Replace the vanilla enchanting table with the Arcane Lectern (the table shows a hint instead of opening).")
            .define("disableVanillaEnchantingTable", true);
    public static final ModConfigSpec.IntValue INK_BOUND_INTERVAL = B
            .comment("Ticks between Ink Bound repair pulses.")
            .defineInRange("inkBoundIntervalTicks", 100, 20, 12000);
    public static final ModConfigSpec.IntValue PORTAL_DELAY = B
            .comment("Ticks an entity must stand in a Library portal before travelling.")
            .defineInRange("portalDelayTicks", 60, 1, 400);

    public static final ModConfigSpec SPEC = B.build();

    private ArcaneConfig() {}

    public static boolean disableVanillaTable() {
        return !SPEC.isLoaded() || DISABLE_VANILLA_TABLE.get();
    }

    public static int inkBoundInterval() {
        return SPEC.isLoaded() ? INK_BOUND_INTERVAL.get() : 100;
    }

    public static int portalDelay() {
        return SPEC.isLoaded() ? PORTAL_DELAY.get() : 60;
    }
}
