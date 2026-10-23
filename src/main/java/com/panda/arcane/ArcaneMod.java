package com.panda.arcane;

import com.mojang.logging.LogUtils;
import com.panda.arcane.registry.ModAttachments;
import com.panda.arcane.registry.ModBlocks;
import com.panda.arcane.registry.ModComponents;
import com.panda.arcane.registry.ModItems;
import com.panda.arcane.registry.ModMenus;
import com.panda.arcane.registry.ModSounds;
import com.panda.arcane.registry.ModTabs;
import com.panda.arcane.registry.ModTriggers;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ArcaneMod.MODID)
public final class ArcaneMod {
    public static final String MODID = "arcane";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** Set by the client entry class; lets common item code open a screen without naming client classes. */
    public static Runnable openCodex = () -> {};

    public ArcaneMod(IEventBus modBus, ModContainer container) {
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModComponents.register(modBus);
        ModAttachments.register(modBus);
        ModMenus.register(modBus);
        ModSounds.register(modBus);
        ModTriggers.register(modBus);
        ModTabs.register(modBus);
        com.panda.arcane.registry.ModWorldgen.register(modBus);
        com.panda.arcane.registry.ModEntities.register(modBus);
        com.panda.arcane.registry.ModParticles.register(modBus); // Art: lumen motes, ink drops, portal glyphs
        com.panda.arcane.registry.ModLoot.register(modBus); // random_tome: tomes for any mod's enchantments
        modBus.addListener((net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent e) ->
                e.modify(net.minecraft.world.level.block.entity.BlockEntityTypes.BRUSHABLE_BLOCK, com.panda.arcane.registry.ModBlocks.SUSPICIOUS_BOOKSHELF.get()));
        if (System.getProperty("arcane.compatTest") != null) {
            // test runs only: a built-in pack with enchantments shaped like another mod's (datapacks/compat_test)
            modBus.addListener((net.neoforged.neoforge.event.AddPackFindersEvent e) -> e.addPackFinders(id("datapacks/compat_test"),
                    net.minecraft.server.packs.PackType.SERVER_DATA, net.minecraft.network.chat.Component.literal("Arcane compat test"),
                    net.minecraft.server.packs.repository.PackSource.BUILT_IN, true, net.minecraft.server.packs.repository.Pack.Position.TOP));
        }
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SYNCED, ArcaneConfig.SPEC);
        com.panda.arcane.event.ArcaneEvents.register();
        com.panda.arcane.gametest.ArcaneGameTests.register(modBus);
        com.panda.arcane.entity.CreaturesA.register(); // Creatures A
        com.panda.arcane.gametest.CreaturesAGameTests.register(modBus); // Creatures A
        LOGGER.info("Arcane Library booting");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
