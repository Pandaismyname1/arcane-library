package com.panda.arcane.client.mobsb;

import com.panda.arcane.entity.QuillStalker;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/** The Quill Stalker: an inkwell abdomen bristling with quills, a nibbed head and eight two-jointed quill legs. */
public final class QuillStalkerRenderer extends MobsBClient.VariantRenderer<QuillStalker, QuillStalkerRenderer.State, QuillStalkerRenderer.Model> {
    public QuillStalkerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(MobsBClient.QUILL_STALKER)), 0.7F, "quill_stalker");
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(QuillStalker entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.rear = entity.rearAmount(partialTicks);
        state.writing = entity.action() == QuillStalker.ACTION_WRITE;
    }

    @Override
    protected float getFlipDegrees() {
        return 180.0F;
    }

    public static final class State extends MobsBClient.VariantState {
        public float rear;
        public boolean writing;
    }

    public static final class Model extends EntityModel<State> {
        private final ModelPart body, head, nibRight, nibLeft, abdomen;
        private final ModelPart[] quills = new ModelPart[3];
        private final ModelPart[] legsRight = new ModelPart[4], legsLeft = new ModelPart[4];
        private final ModelPart[] tipsRight = new ModelPart[4], tipsLeft = new ModelPart[4];

        public Model(ModelPart root) {
            super(root);
            Function<String, ModelPart> part = root.createPartLookup();
            body = part.apply("body");
            head = part.apply("head");
            nibRight = part.apply("nib_right");
            nibLeft = part.apply("nib_left");
            abdomen = part.apply("abdomen");
            for (int i = 0; i < 3; i++) quills[i] = part.apply("back_quill_" + i);
            for (int i = 0; i < 4; i++) {
                legsRight[i] = part.apply("leg_right_" + i);
                legsLeft[i] = part.apply("leg_left_" + i);
                tipsRight[i] = part.apply("leg_right_" + i + "_tip");
                tipsLeft[i] = part.apply("leg_left_" + i + "_tip");
            }
        }

        @Override
        public void setupAnim(State s) {
            super.setupAnim(s);
            float t = s.ageInTicks;
            float speed = Math.min(1.0F, s.walkAnimationSpeed * 1.4F);
            float pos = s.walkAnimationPos * 1.3F;
            float rear = Math.max(0.0F, s.rear), hunch = Math.max(0.0F, -s.rear);

            body.y += Mth.sin(t * 0.18F) * 0.25F - Math.abs(Mth.sin(pos)) * 0.6F * speed - rear * 1.5F;
            body.xRot = -rear * 0.35F + hunch * 0.3F;
            head.yRot = s.yRot * Mth.DEG_TO_RAD * 0.7F;
            head.xRot = s.xRot * Mth.DEG_TO_RAD * 0.5F + hunch * 0.5F;
            // nibs scissor: idly, then wide on a bite, and scratch back and forth while writing a rune
            float snap = s.attack > 0 ? Mth.sin(s.attack * Mth.PI) * 0.5F : 0.0F;
            float scratch = s.writing ? Mth.sin(t * 1.4F) * 0.25F : 0.0F;
            float nib = 0.08F * Mth.sin(t * 0.25F) + snap + scratch;
            nibRight.yRot += nib;
            nibLeft.yRot -= nib;
            nibRight.xRot = scratch * 0.8F;
            nibLeft.xRot = -scratch * 0.8F;
            // the inkwell lifts and the quills fan out when it aims a volley
            abdomen.xRot += -rear * 0.55F + Mth.sin(t * 0.12F) * 0.04F;
            for (int i = 0; i < 3; i++) {
                quills[i].xRot -= rear * 0.35F;
                quills[i].zRot *= 1.0F + rear * 1.2F;
                quills[i].zRot += Mth.sin(t * 0.2F + i) * 0.03F;
            }
            // alternating tetrapod gait: legs 0/2 of one side step with legs 1/3 of the other
            for (int i = 0; i < 4; i++) {
                float phR = (i % 2 == 0) ? 0.0F : Mth.PI;
                float phL = phR + Mth.PI;
                float swingR = Mth.cos(pos + phR) * 0.38F * speed;
                float swingL = Mth.cos(pos + phL) * 0.38F * speed;
                float liftR = Math.max(0.0F, Mth.sin(pos + phR)) * 0.45F * speed;
                float liftL = Math.max(0.0F, Mth.sin(pos + phL)) * 0.45F * speed;
                legsRight[i].yRot += swingR;
                legsLeft[i].yRot -= swingL;
                legsRight[i].zRot += liftR + Mth.sin(t * 0.21F + i) * 0.02F;
                legsLeft[i].zRot -= liftL + Mth.sin(t * 0.21F + i + 2) * 0.02F;
                tipsRight[i].zRot -= liftR * 0.4F;
                tipsLeft[i].zRot += liftL * 0.4F;
            }
            // front legs rise and spread when it rears to shoot
            legsRight[0].zRot += rear * 0.7F;
            legsLeft[0].zRot -= rear * 0.7F;
            tipsRight[0].zRot += rear * 0.5F;
            tipsLeft[0].zRot -= rear * 0.5F;
        }
    }
}
