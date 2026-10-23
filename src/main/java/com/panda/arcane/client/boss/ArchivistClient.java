package com.panda.arcane.client.boss;

import com.panda.arcane.entity.Archivist;
import com.panda.arcane.registry.ModEntities;
import com.panda.arcane.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.Music;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client wiring for the boss: model layer, renderer and the fight music. Called once from the client mod class. */
public final class ArchivistClient {
    private ArchivistClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions e) ->
                e.registerLayerDefinition(ArchivistModel.LAYER, ArchivistModel::createLayer));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) ->
                e.registerEntityRenderer(ModEntities.ARCHIVIST.get(), ArchivistRenderer::new));
        NeoForge.EVENT_BUS.addListener(ArchivistClient::music);
    }

    /** While an Archivist nearby is fighting, its own track replaces whatever was playing. */
    private static void music(SelectMusicEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        boolean fight = !mc.level.getEntitiesOfClass(Archivist.class, mc.player.getBoundingBox().inflate(40), Archivist::isFighting).isEmpty();
        if (fight) event.overrideMusic(new Music(ModSounds.ARCHIVIST_MUSIC, 20, 60, true));
    }
}
