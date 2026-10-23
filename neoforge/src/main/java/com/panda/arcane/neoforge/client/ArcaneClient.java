package com.panda.arcane.neoforge.client;

import com.panda.arcane.ArcaneMod;
import com.panda.arcane.client.ArcaneFeedback;
import com.panda.arcane.client.ArcaneLecternScreen;
import com.panda.arcane.client.ArcaneRenderers;
import com.panda.arcane.client.ArcaneTooltips;
import com.panda.arcane.client.ClientSinks;
import com.panda.arcane.client.CodexScreen;
import com.panda.arcane.client.DeconstructionScreen;
import com.panda.arcane.client.DevExit;
import com.panda.arcane.client.boss.ArchivistClient;
import com.panda.arcane.client.creatures.CreatureLayers;
import com.panda.arcane.client.mobsb.MobsBClient;
import com.panda.arcane.client.particle.ArcaneParticles;
import com.panda.arcane.registry.ModMenus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.sounds.Music;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** The NeoForge client entry point: hands the common client registrations NeoForge's events. */
@Mod(value = ArcaneMod.MODID, dist = Dist.CLIENT)
public final class ArcaneClient {
    public ArcaneClient(IEventBus modBus, ModContainer container) {
        modBus.addListener((RegisterMenuScreensEvent e) -> {
            e.register(ModMenus.LECTERN.get(), ArcaneLecternScreen::new);
            e.register(ModMenus.DECONSTRUCTION.get(), DeconstructionScreen::new);
        });
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> ArcaneRenderers.register(new ClientSinks.Renderers() {
            @Override
            public <E extends Entity> void register(EntityType<? extends E> type, EntityRendererProvider<E> provider) {
                e.registerEntityRenderer(type, provider);
            }
        }));
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions e) -> {
            CreatureLayers.register(e::registerLayerDefinition);
            MobsBClient.registerLayers(e::registerLayerDefinition);
            ArchivistClient.registerLayers(e::registerLayerDefinition);
        });
        modBus.addListener((RegisterParticleProvidersEvent e) -> ArcaneParticles.register(new ClientSinks.Particles() {
            @Override
            public <T extends ParticleOptions> void sprites(ParticleType<T> type, ClientSinks.SpriteProvider<T> provider) {
                e.registerSpriteSet(type, provider::create);
            }
        }));
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAbove(VanillaGuiLayers.CROSSHAIR, ArcaneFeedback.STUDY_LAYER, ArcaneFeedback::studyBar));

        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> ArcaneFeedback.onClientTick());
        NeoForge.EVENT_BUS.addListener((GameShuttingDownEvent e) -> DevExit.onClientStopping());
        NeoForge.EVENT_BUS.addListener((ItemTooltipEvent e) -> ArcaneTooltips.onTooltip(e.getItemStack(), e.getEntity(), e.getFlags(), e.getToolTip()));
        NeoForge.EVENT_BUS.addListener((SelectMusicEvent e) -> {
            Music fight = ArchivistClient.music();
            if (fight != null) e.overrideMusic(fight);
        });
        ArcaneMod.openCodex = () -> Minecraft.getInstance().gui.setScreen(new CodexScreen());
    }
}
