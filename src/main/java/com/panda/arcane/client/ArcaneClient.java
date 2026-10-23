package com.panda.arcane.client;

import com.panda.arcane.ArcaneMod;
import com.panda.arcane.registry.ModMenus;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = ArcaneMod.MODID, dist = Dist.CLIENT)
public final class ArcaneClient {
    public ArcaneClient(IEventBus modBus, ModContainer container) {
        modBus.addListener((RegisterMenuScreensEvent e) -> {
            e.register(ModMenus.LECTERN.get(), ArcaneLecternScreen::new);
            e.register(ModMenus.DECONSTRUCTION.get(), DeconstructionScreen::new);
        });
        modBus.addListener(ArcaneRenderers::register);
        modBus.addListener(com.panda.arcane.client.particle.ArcaneParticles::register); // Art: particle providers
        modBus.addListener(com.panda.arcane.client.creatures.CreatureLayers::register); // Creatures A
        com.panda.arcane.client.boss.ArchivistClient.init(modBus); // Boss: Archivist model, renderer, music
        ArcaneTooltips.register();
        ArcaneFeedback.register(modBus);
        modBus.addListener(com.panda.arcane.client.mobsb.MobsBClient::registerLayers); // Creatures B model layers
        ArcaneMod.openCodex = () -> Minecraft.getInstance().gui.setScreen(new CodexScreen());
    }
}
