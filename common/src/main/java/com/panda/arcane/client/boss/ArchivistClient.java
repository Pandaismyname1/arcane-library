package com.panda.arcane.client.boss;

import com.panda.arcane.entity.Archivist;
import com.panda.arcane.registry.ModEntities;
import com.panda.arcane.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.Music;
import com.panda.arcane.client.ClientSinks;

/** Client wiring for the boss: model layer, renderer and the fight music. Called once from the client mod class. */
public final class ArchivistClient {
    private ArchivistClient() {}

    public static void registerLayers(ClientSinks.Layers layers) {
        layers.register(ArchivistModel.LAYER, ArchivistModel::createLayer);
    }

    public static void registerRenderers(ClientSinks.Renderers renderers) {
        renderers.register(ModEntities.ARCHIVIST.get(), ArchivistRenderer::new);
    }

    /** While an Archivist nearby is fighting, its own track replaces whatever was playing: that track, else null. */
    public static Music music() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return null;
        boolean fight = !mc.level.getEntitiesOfClass(Archivist.class, mc.player.getBoundingBox().inflate(40), Archivist::isFighting).isEmpty();
        return fight ? new Music(ModSounds.ARCHIVIST_MUSIC.holder(), 20, 60, true) : null;
    }
}
