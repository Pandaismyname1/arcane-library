package com.panda.arcane.client.mobsb;

import com.panda.arcane.entity.Bookwyrm;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/**
 * The Bookwyrm: a book-headed worm of six bundles of books and a tail. Each segment follows the path the head took
 * (a yaw history sampled by distance travelled), with a slither wave on top while it moves.
 */
public final class BookwyrmRenderer extends MobsBClient.VariantRenderer<Bookwyrm, BookwyrmRenderer.State, BookwyrmRenderer.Model> {
    public BookwyrmRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(MobsBClient.BOOKWYRM)), 0.55F, "bookwyrm");
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(Bookwyrm entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        for (int i = 0; i < Bookwyrm.SEGMENTS; i++) state.segYaw[i] = entity.segmentYaw(i);
        state.jaw = Mth.lerp(partialTicks, entity.jawO, entity.jaw);
        state.phase = entity.phase();
    }

    @Override
    protected float getFlipDegrees() {
        return 180.0F;
    }

    public static final class State extends MobsBClient.VariantState {
        public final float[] segYaw = new float[Bookwyrm.SEGMENTS];
        public float jaw;
        public Bookwyrm.Phase phase = Bookwyrm.Phase.NONE;
    }

    public static final class Model extends EntityModel<State> {
        private final ModelPart head, jaw;
        private final ModelPart[] segs = new ModelPart[Bookwyrm.SEGMENTS];

        public Model(ModelPart root) {
            super(root);
            Function<String, ModelPart> part = root.createPartLookup();
            head = part.apply("head");
            jaw = part.apply("jaw");
            for (int i = 0; i < 6; i++) segs[i] = part.apply("seg" + i);
            segs[6] = part.apply("tail");
        }

        @Override
        public void setupAnim(State s) {
            super.setupAnim(s);
            float t = s.ageInTicks;
            float speed = Math.min(1.0F, s.walkAnimationSpeed * 1.5F);
            float pos = s.walkAnimationPos;
            for (int i = 1; i < segs.length; i++) {
                float follow = s.segYaw[i - 1] * Mth.DEG_TO_RAD;
                float wave = Mth.sin(pos * 0.8F - i * 1.1F) * 0.32F * speed + Mth.sin(t * 0.08F - i * 0.9F) * 0.05F;
                segs[i].yRot = follow + wave;
                segs[i].y += Math.max(0.0F, Mth.sin(pos * 0.8F - i * 1.1F + 1.2F)) * -0.6F * speed;
            }
            segs[0].yRot = Mth.sin(pos * 0.8F) * 0.12F * speed;
            head.yRot = Mth.clamp(s.yRot, -50.0F, 50.0F) * Mth.DEG_TO_RAD - segs[0].yRot;
            head.xRot = s.xRot * Mth.DEG_TO_RAD * 0.5F + Mth.sin(t * 0.1F) * 0.03F;
            switch (s.phase) {
                case DIVE -> head.xRot += 0.9F;
                case EMERGE -> head.xRot -= 0.55F;
                case CHEW -> head.xRot += 0.25F + Mth.sin(t * 0.9F) * 0.12F;
                default -> { }
            }
            head.xRot -= s.attack > 0 ? Mth.sin(s.attack * Mth.PI) * 0.5F : 0.0F;
            jaw.xRot = s.jaw * 0.75F + (s.attack > 0 ? Mth.sin(s.attack * Mth.PI) * 0.4F : 0.0F);
        }
    }
}
