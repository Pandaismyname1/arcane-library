package com.panda.arcane.client.boss;

import com.panda.arcane.ArcaneMod;
import com.panda.arcane.entity.Archivist;
import com.panda.arcane.entity.ArchivistPattern;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Renders the Archivist with its own model, plus a glow layer: gold normally, blue while warded, crimson in phase 3. */
public class ArchivistRenderer extends MobRenderer<Archivist, ArchivistRenderState, ArchivistModel> {
    private static final Identifier TEXTURE = tex("archivist");
    private static final Identifier GLOW = tex("archivist_glow");
    private static final Identifier GLOW_WARDED = tex("archivist_glow_warded");
    private static final Identifier GLOW_FINAL = tex("archivist_glow_final");

    private static Identifier tex(String name) {
        return ArcaneMod.id("textures/entity/archivist/" + name + ".png");
    }

    public ArchivistRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new ArchivistModel(ctx.bakeLayer(ArchivistModel.LAYER)), 0.9F);
        addLayer(new LivingEntityEmissiveLayer<>(this,
                s -> s.warded ? GLOW_WARDED : s.phase >= 3 ? GLOW_FINAL : GLOW,
                (s, age) -> s.casting || s.warded ? 1.0F : 0.75F + 0.25F * Mth.sin(age * 0.12F),
                new ArchivistModel(ctx.bakeLayer(ArchivistModel.LAYER)), RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public ArchivistRenderState createRenderState() {
        return new ArchivistRenderState();
    }

    @Override
    public void extractRenderState(Archivist entity, ArchivistRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.idle.copyFrom(entity.idleAnimation);
        state.storm.copyFrom(entity.stormAnimation);
        state.erasure.copyFrom(entity.erasureAnimation);
        state.recall.copyFrom(entity.recallAnimation);
        state.slam.copyFrom(entity.slamAnimation);
        state.index.copyFrom(entity.indexAnimation);
        state.transition.copyFrom(entity.transitionAnimation);
        state.stagger.copyFrom(entity.staggerAnimation);
        state.phase = entity.phase();
        state.warded = entity.isWarded();
        state.hovering = entity.isHovering();
        state.casting = entity.move() != ArchivistPattern.Move.NONE;
    }

    @Override
    public Identifier getTextureLocation(ArchivistRenderState state) {
        return TEXTURE;
    }
}
