package com.panda.arcane.client;

import com.panda.arcane.ArcaneMod;
import com.panda.arcane.entity.ArcaneBolt;
import com.panda.arcane.registry.ModEntities;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Entity renderers. The Creatures A mobs have their own models and renderer in client/creatures. */
public final class ArcaneRenderers {
    private ArcaneRenderers() {}

    private static Identifier tex(String name) {
        return ArcaneMod.id("textures/entity/" + name + ".png");
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        // Boss: the Archivist's renderer is registered by client.boss.ArchivistClient (own model and layers)
        // ---- Creatures A: moth, librarian, ink golem, vault ward, inkling (custom models in client/creatures) ----
        com.panda.arcane.client.creatures.CreatureRenderer.register(event);
        // ---- end Creatures A ----
        event.registerEntityRenderer(ModEntities.ARCANE_BOLT.get(), ctx -> new ThrownItemRenderer<ArcaneBolt>(ctx, 1.4F, true));
        // ---- Creatures B: custom models in client/mobsb
        com.panda.arcane.client.mobsb.MobsBClient.registerRenderers(event);
    }

    static final class Humanoid<T extends Mob> extends HumanoidMobRenderer<T, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
        private final Identifier texture;

        Humanoid(EntityRendererProvider.Context ctx, Identifier texture, float shadow) {
            super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE_BABY)), shadow);
            this.texture = texture;
        }

        @Override
        public HumanoidRenderState createRenderState() {
            return new HumanoidRenderState();
        }

        @Override
        public Identifier getTextureLocation(HumanoidRenderState state) {
            return texture;
        }
    }
}
