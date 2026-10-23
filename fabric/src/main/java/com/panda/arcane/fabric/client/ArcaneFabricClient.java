package com.panda.arcane.fabric.client;

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
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/** The Fabric client entry point: hands the common client registrations to Fabric API's registries and events. */
public final class ArcaneFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(ModMenus.LECTERN.get(), ArcaneLecternScreen::new);
        MenuScreens.register(ModMenus.DECONSTRUCTION.get(), DeconstructionScreen::new);
        ArcaneRenderers.register(new ClientSinks.Renderers() {
            @Override
            public <E extends Entity> void register(EntityType<? extends E> type, EntityRendererProvider<E> provider) {
                EntityRendererRegistry.register(type, provider);
            }
        });
        ClientSinks.Layers layers = (layer, definition) -> ModelLayerRegistry.registerModelLayer(layer, definition::get);
        CreatureLayers.register(layers);
        MobsBClient.registerLayers(layers);
        ArchivistClient.registerLayers(layers);
        ArcaneParticles.register(new ClientSinks.Particles() {
            @Override
            public <T extends ParticleOptions> void sprites(ParticleType<T> type, ClientSinks.SpriteProvider<T> provider) {
                ParticleProviderRegistry.getInstance().register(type, provider::create);
            }
        });
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, ArcaneFeedback.STUDY_LAYER, ArcaneFeedback::studyBar);

        ClientTickEvents.END_CLIENT_TICK.register(mc -> ArcaneFeedback.onClientTick());
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> DevExit.onClientStopping());
        ItemTooltipCallback.EVENT.register((stack, context, flags, lines) -> ArcaneTooltips.onTooltip(stack, null, flags, lines));
        ArcaneMod.openCodex = () -> Minecraft.getInstance().gui.setScreen(new CodexScreen());
    }
}
