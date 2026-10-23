package com.panda.arcane.fabric;

import com.panda.arcane.ArcaneConfig;
import com.panda.arcane.ArcaneMod;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

/**
 * The mod's settings on Fabric: config/arcane.properties, written with the defaults on first start. Unlike NeoForge's
 * synced config it is read once at startup, on each side from its own file.
 */
final class FabricConfig implements ArcaneConfig.Source {
    private final boolean disableVanillaTable;
    private final int inkBoundInterval;
    private final int portalDelay;

    private FabricConfig(Properties p) {
        ArcaneConfig.Source d = ArcaneConfig.DEFAULTS;
        disableVanillaTable = Boolean.parseBoolean(p.getProperty("disableVanillaEnchantingTable", String.valueOf(d.disableVanillaTable())));
        inkBoundInterval = clamp(p.getProperty("inkBoundIntervalTicks"), d.inkBoundInterval(), 20, 12000);
        portalDelay = clamp(p.getProperty("portalDelayTicks"), d.portalDelay(), 1, 400);
    }

    static FabricConfig load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("arcane.properties");
        Properties p = new Properties();
        if (Files.exists(file)) {
            try (Reader in = Files.newBufferedReader(file)) {
                p.load(in);
            } catch (IOException e) {
                ArcaneMod.LOGGER.warn("Couldn't read {}, using the defaults", file, e);
            }
        } else {
            ArcaneConfig.Source d = ArcaneConfig.DEFAULTS;
            p.setProperty("disableVanillaEnchantingTable", String.valueOf(d.disableVanillaTable()));
            p.setProperty("inkBoundIntervalTicks", String.valueOf(d.inkBoundInterval()));
            p.setProperty("portalDelayTicks", String.valueOf(d.portalDelay()));
            try (Writer out = Files.newBufferedWriter(file)) {
                p.store(out, """
                         Arcane Library
                         disableVanillaEnchantingTable: replace the vanilla enchanting table with the Arcane Lectern (true/false)
                         inkBoundIntervalTicks: ticks between Ink Bound repair pulses (20-12000)
                         portalDelayTicks: ticks an entity must stand in a Library portal before travelling (1-400)""");
            } catch (IOException e) {
                ArcaneMod.LOGGER.warn("Couldn't write {}", file, e);
            }
        }
        return new FabricConfig(p);
    }

    private static int clamp(String value, int fallback, int min, int max) {
        if (value == null) return fallback;
        try {
            return Math.clamp(Integer.parseInt(value.trim()), min, max);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public boolean disableVanillaTable() {
        return disableVanillaTable;
    }

    @Override
    public int inkBoundInterval() {
        return inkBoundInterval;
    }

    @Override
    public int portalDelay() {
        return portalDelay;
    }
}
