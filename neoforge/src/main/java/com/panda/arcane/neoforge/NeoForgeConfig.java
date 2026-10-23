package com.panda.arcane.neoforge;

import com.panda.arcane.ArcaneConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/** The mod's settings as a NeoForge config (synced from server to clients). */
public final class NeoForgeConfig implements ArcaneConfig.Source {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue DISABLE_VANILLA_TABLE = B
            .comment("Replace the vanilla enchanting table with the Arcane Lectern (the table shows a hint instead of opening).")
            .define("disableVanillaEnchantingTable", true);
    private static final ModConfigSpec.IntValue INK_BOUND_INTERVAL = B
            .comment("Ticks between Ink Bound repair pulses.")
            .defineInRange("inkBoundIntervalTicks", 100, 20, 12000);
    private static final ModConfigSpec.IntValue PORTAL_DELAY = B
            .comment("Ticks an entity must stand in a Library portal before travelling.")
            .defineInRange("portalDelayTicks", 60, 1, 400);

    public static final ModConfigSpec SPEC = B.build();

    @Override
    public boolean disableVanillaTable() {
        return !SPEC.isLoaded() ? ArcaneConfig.DEFAULTS.disableVanillaTable() : DISABLE_VANILLA_TABLE.get();
    }

    @Override
    public int inkBoundInterval() {
        return SPEC.isLoaded() ? INK_BOUND_INTERVAL.get() : ArcaneConfig.DEFAULTS.inkBoundInterval();
    }

    @Override
    public int portalDelay() {
        return SPEC.isLoaded() ? PORTAL_DELAY.get() : ArcaneConfig.DEFAULTS.portalDelay();
    }
}
