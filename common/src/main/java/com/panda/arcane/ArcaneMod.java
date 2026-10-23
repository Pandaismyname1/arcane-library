package com.panda.arcane;

import com.mojang.logging.LogUtils;
import com.panda.arcane.entity.MobsB;
import com.panda.arcane.registry.ModBlocks;
import com.panda.arcane.registry.ModComponents;
import com.panda.arcane.registry.ModEntities;
import com.panda.arcane.registry.ModItems;
import com.panda.arcane.registry.ModLoot;
import com.panda.arcane.registry.ModMenus;
import com.panda.arcane.registry.ModParticles;
import com.panda.arcane.registry.ModSounds;
import com.panda.arcane.registry.ModTabs;
import com.panda.arcane.registry.ModTriggers;
import com.panda.arcane.registry.ModWorldgen;
import java.util.List;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

/** The mod's common side. Each loader module has its own entry point, which calls {@link #init()} first. */
public final class ArcaneMod {
    public static final String MODID = "arcane";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** Set by the client entry class; lets common item code open a screen without naming client classes. */
    public static Runnable openCodex = () -> {};

    /** The classes whose static DeferredRegisters hold the mod's content (Creatures B keeps its sounds in MobsB). */
    private static final List<Class<?>> CONTENT = List.of(ModBlocks.class, ModItems.class, ModComponents.class, ModMenus.class,
            ModSounds.class, ModTriggers.class, ModTabs.class, ModWorldgen.class, ModEntities.class, ModParticles.class, ModLoot.class,
            MobsB.class);

    private ArcaneMod() {}

    /** Loads the content classes so every registration is queued before the loader registers them. */
    public static void init() {
        for (Class<?> c : CONTENT) {
            try {
                Class.forName(c.getName(), true, c.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
        LOGGER.info("Arcane Library booting on {}", com.panda.arcane.platform.Services.PLATFORM.loaderName());
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
