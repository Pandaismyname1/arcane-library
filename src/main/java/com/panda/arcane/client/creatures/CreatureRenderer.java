package com.panda.arcane.client.creatures;

import com.mojang.blaze3d.vertex.PoseStack;
import com.panda.arcane.ArcaneMod;
import com.panda.arcane.entity.CreatureVisuals;
import com.panda.arcane.entity.InkGolem;
import com.panda.arcane.entity.Inkling;
import com.panda.arcane.entity.LumenMoth;
import com.panda.arcane.entity.SilentLibrarian;
import com.panda.arcane.entity.VaultWard;
import com.panda.arcane.registry.ModEntities;
import java.util.Arrays;
import java.util.function.Function;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * One renderer for all Creatures A mobs: the variant picks textures/entity/&lt;mob&gt;/&lt;variant&gt;.png, and an
 * emissive layer draws &lt;variant&gt;_glow.png (eyes, runes, lanterns) at full brightness.
 */
public class CreatureRenderer<T extends Mob & CreatureVisuals> extends MobRenderer<T, CreatureRenderState, CreatureModel> {
    private final Identifier[] textures, glows;
    private final String[] names;
    private final float modelScale;
    private boolean flashOnAction;

    public CreatureRenderer(EntityRendererProvider.Context ctx, CreatureModel model, String mob, String[] variants, float shadow, float modelScale) {
        super(ctx, model, shadow);
        this.names = variants;
        this.modelScale = modelScale;
        this.textures = Arrays.stream(variants).map(v -> ArcaneMod.id("textures/entity/" + mob + "/" + v + ".png")).toArray(Identifier[]::new);
        this.glows = Arrays.stream(variants).map(v -> ArcaneMod.id("textures/entity/" + mob + "/" + v + "_glow.png")).toArray(Identifier[]::new);
        addLayer(new Glow(this));
    }

    /** Flash white while the special move builds (the Red Inkling's swell, like a creeper's fuse). */
    public CreatureRenderer<T> flashOnAction() {
        this.flashOnAction = true;
        return this;
    }

    @Override
    public CreatureRenderState createRenderState() {
        return new CreatureRenderState();
    }

    @Override
    public void extractRenderState(T entity, CreatureRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.variant = Mth.clamp(entity.variantIndex(), 0, names.length - 1);
        state.variantName = names[state.variant];
        state.action = entity.actionProgress(partialTicks);
        state.attack = entity.attackProgress(partialTicks);
        state.flag = entity.visualFlag();
    }

    @Override
    public Identifier getTextureLocation(CreatureRenderState state) {
        return textures[state.variant];
    }

    @Override
    protected void scale(CreatureRenderState state, PoseStack poseStack) {
        if (modelScale != 1.0F) poseStack.scale(modelScale, modelScale, modelScale);
    }

    @Override
    protected float getWhiteOverlayProgress(CreatureRenderState state) {
        if (!flashOnAction || state.action <= 0) return 0.0F;
        // creeper-style flashes over a steady paling, so every frame of the tell reads as "about to pop"
        float flash = (int) (state.action * 10.0F) % 2 == 0 ? 0.0F : Mth.clamp(state.action, 0.5F, 1.0F);
        return Math.max(flash, 0.35F * state.action);
    }

    private final class Glow extends RenderLayer<CreatureRenderState, CreatureModel> {
        Glow(CreatureRenderer<T> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, CreatureRenderState state, float yRot, float xRot) {
            if (state.isInvisible) return;
            collector.order(1).submitModel(getParentModel(), state, poseStack, RenderTypes.eyes(glows[state.variant]), lightCoords,
                    OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
    }

    // ---- registration ------------------------------------------------------------------------------------------

    private static String[] ids(Enum<?>[] values) {
        return Arrays.stream(values).map(v -> v.name().toLowerCase(java.util.Locale.ROOT)).toArray(String[]::new);
    }

    private static <T extends Mob & CreatureVisuals> EntityRendererProvider<T> provider(ModelLayerLocation layer, Function<ModelPart, CreatureModel> model,
                                                                                        String mob, Enum<?>[] variants, float shadow, float scale, boolean flash) {
        return ctx -> {
            CreatureRenderer<T> r = new CreatureRenderer<>(ctx, model.apply(ctx.bakeLayer(layer)), mob, ids(variants), shadow, scale);
            return flash ? r.flashOnAction() : r;
        };
    }

    /** Called from ArcaneRenderers (Creatures A block). */
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.LUMEN_MOTH.get(),
                provider(CreatureLayers.LUMENMOTH, CreatureModels.Moth::new, "lumen_moth", LumenMoth.Variant.values(), 0.25F, 1.0F, false));
        event.registerEntityRenderer(ModEntities.SILENT_LIBRARIAN.get(),
                provider(CreatureLayers.SILENTLIBRARIAN, CreatureModels.Librarian::new, "silent_librarian", SilentLibrarian.Variant.values(), 0.5F, 1.0F, false));
        event.registerEntityRenderer(ModEntities.INK_GOLEM.get(),
                provider(CreatureLayers.INKGOLEM, CreatureModels.Golem::new, "ink_golem", InkGolem.Variant.values(), 1.0F, 1.25F, false));
        event.registerEntityRenderer(ModEntities.VAULT_WARD.get(),
                provider(CreatureLayers.VAULTWARD, CreatureModels.Ward::new, "vault_ward", VaultWard.Variant.values(), 0.35F, 1.0F, false));
        event.registerEntityRenderer(ModEntities.INKLING.get(),
                provider(CreatureLayers.INKLING, CreatureModels.InklingModel::new, "inkling", Inkling.Variant.values(), 0.35F, 0.75F, true));
    }
}
