package com.panda.arcane.client.mobsb;

import com.mojang.blaze3d.vertex.PoseStack;
import com.panda.arcane.ArcaneMod;
import com.panda.arcane.entity.VariantMonster;
import com.panda.arcane.registry.ModEntities;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import com.panda.arcane.client.ClientSinks;

/** Client registration for the Creatures B models, plus the shared variant renderer and glow layer. */
public final class MobsBClient {
    public static final ModelLayerLocation PAGE_WRAITH = layer("page_wraith");
    public static final ModelLayerLocation TOME_MIMIC = layer("tome_mimic");
    public static final ModelLayerLocation BOOKWYRM = layer("bookwyrm");
    public static final ModelLayerLocation QUILL_STALKER = layer("quill_stalker");
    public static final ModelLayerLocation BOOKEND_GARGOYLE = layer("bookend_gargoyle");

    private MobsBClient() {}

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ArcaneMod.id(name), "main");
    }

    public static void registerLayers(ClientSinks.Layers event) {
        event.register(PAGE_WRAITH, MobsBLayers::pageWraith);
        event.register(TOME_MIMIC, MobsBLayers::tomeMimic);
        event.register(BOOKWYRM, MobsBLayers::bookwyrm);
        event.register(QUILL_STALKER, MobsBLayers::quillStalker);
        event.register(BOOKEND_GARGOYLE, MobsBLayers::gargoyle);
    }

    public static void registerRenderers(ClientSinks.Renderers event) {
        event.register(ModEntities.PAGE_WRAITH.get(), PageWraithRenderer::new);
        event.register(ModEntities.TOME_MIMIC.get(), TomeMimicRenderer::new);
        event.register(ModEntities.BOOKWYRM.get(), BookwyrmRenderer::new);
        event.register(ModEntities.QUILL_STALKER.get(), QuillStalkerRenderer::new);
        event.register(ModEntities.BOOKEND_GARGOYLE.get(), GargoyleRenderer::new);
    }

    /** Render state carrying the variant name every Creatures B renderer needs. */
    public static class VariantState extends LivingEntityRenderState {
        public String variant = "";
        public String glow = "";
        public float attack;
    }

    /** Picks textures/entity/<folder>/<variant>.png and its _glow mask from the synced variant. */
    public abstract static class VariantRenderer<T extends VariantMonster, S extends VariantState, M extends EntityModel<S>> extends MobRenderer<T, S, M> {
        private final String folder;

        protected VariantRenderer(EntityRendererProvider.Context ctx, M model, float shadow, String folder) {
            super(ctx, model, shadow);
            this.folder = folder;
            addLayer(new GlowLayer<>(this));
        }

        @Override
        public void extractRenderState(T entity, S state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.variant = entity.variantName();
            state.glow = entity.variantName();
            state.attack = entity.getSwingAnimation(partialTicks);
        }

        @Override
        public Identifier getTextureLocation(S state) {
            return ArcaneMod.id("textures/entity/" + folder + "/" + state.variant + ".png");
        }

        Identifier glowTexture(S state) {
            return ArcaneMod.id("textures/entity/" + folder + "/" + state.glow + "_glow.png");
        }
    }

    /** Full-bright glowing pixels (eyes, runes, cracks) drawn over the model from the variant's glow mask. */
    static final class GlowLayer<T extends VariantMonster, S extends VariantState, M extends EntityModel<S>> extends RenderLayer<S, M> {
        private final VariantRenderer<T, S, M> renderer;

        GlowLayer(VariantRenderer<T, S, M> renderer) {
            super(renderer);
            this.renderer = renderer;
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, S state, float yRot, float xRot) {
            if (state.isInvisible || state.glow.isEmpty()) return;
            collector.order(1).submitModel(getParentModel(), state, poseStack, RenderTypes.eyes(renderer.glowTexture(state)),
                    lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
    }
}
