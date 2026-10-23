package com.panda.arcane.client.mobsb;

import com.panda.arcane.entity.TomeMimic;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/** The Tome Mimic: a closed book that grows stubby legs, gapes its cover open and bites with page-teeth. */
public final class TomeMimicRenderer extends MobsBClient.VariantRenderer<TomeMimic, TomeMimicRenderer.State, TomeMimicRenderer.Model> {
    public TomeMimicRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(MobsBClient.TOME_MIMIC)), 0.45F, "tome_mimic");
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TomeMimic entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.open = entity.openAmount(partialTicks);
        state.awake = entity.awake();
        state.bite = entity.biteTicks > 0 ? (entity.biteTicks - partialTicks) / 10.0F : 0.0F;
        state.casting = entity.spell() != TomeMimic.SPELL_NONE;
        if (!entity.awake()) state.glow = ""; // closed eyes: no glow at all
    }

    @Override
    protected float getFlipDegrees() {
        return 180.0F;
    }

    public static final class State extends MobsBClient.VariantState {
        public float open, bite;
        public boolean awake, casting;
    }

    public static final class Model extends EntityModel<State> {
        private final ModelPart book, lid, tongue, tongueTip;
        private final ModelPart[] flips = new ModelPart[2];
        private final ModelPart[] legs = new ModelPart[4];

        public Model(ModelPart root) {
            super(root);
            Function<String, ModelPart> part = root.createPartLookup();
            book = part.apply("book");
            lid = part.apply("lid");
            tongue = part.apply("tongue");
            tongueTip = part.apply("tongue_tip");
            flips[0] = part.apply("flip_0");
            flips[1] = part.apply("flip_1");
            String[] l = {"leg_front_right", "leg_front_left", "leg_back_right", "leg_back_left"};
            for (int i = 0; i < 4; i++) legs[i] = part.apply(l[i]);
        }

        @Override
        public void setupAnim(State s) {
            super.setupAnim(s);
            float t = s.ageInTicks;
            float open = s.open;
            float legOut = Mth.clamp(open * 1.8F, 0.0F, 1.0F);
            float walk = s.walkAnimationSpeed * legOut, pos = s.walkAnimationPos;

            // legs unfold from under the cover and the book rises onto them
            for (int i = 0; i < 4; i++) {
                legs[i].visible = legOut > 0.02F;
                legs[i].yScale = Math.max(0.02F, legOut);
                float phase = (i == 0 || i == 3) ? 0.0F : Mth.PI;
                legs[i].xRot = Mth.cos(pos * 0.9F + phase) * 1.1F * walk;
            }
            float hop = Math.abs(Mth.sin(pos * 0.9F)) * 2.0F * walk;
            book.y -= 3.0F * legOut + hop;
            book.xRot = Mth.sin(pos * 0.9F) * 0.12F * walk;
            book.zRot = Mth.cos(pos * 0.45F) * 0.06F * walk;
            if (s.awake) book.yRot = s.yRot * Mth.DEG_TO_RAD * 0.35F;

            // the lid: chatters while awake, gapes for a cast, snaps on a bite; a faint twitch while disguised
            float chatter = s.awake ? (0.5F + 0.5F * Mth.sin(t * 0.35F)) * 0.22F : 0.0F;
            float lidOpen = open * 0.45F + chatter;
            if (s.casting) lidOpen = 1.15F + Mth.sin(t * 0.6F) * 0.08F;
            if (s.bite > 0) lidOpen += Mth.sin(s.bite * Mth.PI) * 0.9F;
            if (!s.awake && t % 140 < 5) lidOpen += 0.06F;
            lid.xRot = -lidOpen;

            // the bookmark ribbon hangs out of the pages; awake it lolls and wags like a tongue
            tongue.yRot = Mth.sin(t * 0.3F) * 0.45F * open;
            tongue.xRot = -0.25F * open;
            tongueTip.xRot = 0.1F + open * 0.5F + Mth.sin(t * 0.25F) * 0.25F * open;

            // a grimoire flips its pages while it casts
            for (int i = 0; i < 2; i++) {
                flips[i].visible = s.casting;
                flips[i].xRot = -((t * 0.22F + i * 1.6F) % Mth.PI);
            }
        }
    }
}
