package com.panda.arcane.client.mobsb;

import com.panda.arcane.entity.PageWraith;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/** The Page Wraith: a hooded sheaf of pages with page-fan wings, trailing strips and three pages in orbit. */
public final class PageWraithRenderer extends MobsBClient.VariantRenderer<PageWraith, PageWraithRenderer.State, PageWraithRenderer.Model> {
    public PageWraithRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(MobsBClient.PAGE_WRAITH)), 0.3F, "page_wraith");
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PageWraith entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.dive = Mth.lerp(partialTicks, entity.diveAmountO, entity.diveAmount);
        state.ward = entity.wardFlash;
        if (entity.variant() == PageWraith.BLANK && entity.copied()) state.glow = "blank_copied";
    }

    public static final class State extends MobsBClient.VariantState {
        public float dive;
        public float ward;
    }

    public static final class Model extends EntityModel<State> {
        private final ModelPart body, head, wingRight, wingLeft;
        private final ModelPart[] wingPagesRight = new ModelPart[3], wingPagesLeft = new ModelPart[3];
        private final ModelPart[] tails = new ModelPart[3], tailEnds = new ModelPart[3];
        private final ModelPart[] orbit = new ModelPart[3];

        public Model(ModelPart root) {
            super(root, RenderTypes::entityTranslucent);
            Function<String, ModelPart> part = root.createPartLookup();
            body = part.apply("body");
            head = part.apply("head");
            wingRight = part.apply("wing_right");
            wingLeft = part.apply("wing_left");
            for (int i = 0; i < 3; i++) {
                wingPagesRight[i] = part.apply("wing_right_" + i);
                wingPagesLeft[i] = part.apply("wing_left_" + i);
                orbit[i] = part.apply("orbit_" + i);
            }
            String[] t = {"tail_mid", "tail_right", "tail_left"};
            for (int i = 0; i < 3; i++) {
                tails[i] = part.apply(t[i]);
                tailEnds[i] = part.apply(t[i] + "_end");
            }
        }

        @Override
        public void setupAnim(State s) {
            super.setupAnim(s);
            float t = s.ageInTicks;
            float dive = Math.max(0, s.dive), tell = Math.max(0, -s.dive);
            // hover: a slow bob and sway; the tell rears back, the dive pitches forward
            body.y += Mth.sin(t * 0.09F) * 1.2F - tell * 1.5F;
            body.xRot = 0.08F + dive * 1.0F - tell * 0.45F;
            body.zRot = Mth.sin(t * 0.05F) * 0.06F;
            head.yRot = s.yRot * Mth.DEG_TO_RAD;
            head.xRot = s.xRot * Mth.DEG_TO_RAD * 0.6F - dive * 0.6F;
            // wings: a fan of pages that beats slowly, flares on the tell and sweeps back in a dive
            float beat = Mth.sin(t * 0.28F);
            float spread = 0.25F - tell * 0.35F + dive * 1.1F;
            wingRight.yRot = spread;
            wingRight.zRot = beat * 0.35F + tell * 0.5F;
            wingLeft.yRot = -spread;
            wingLeft.zRot = -beat * 0.35F - tell * 0.5F;
            for (int i = 0; i < 3; i++) {
                float ripple = Mth.sin(t * 0.28F - i * 0.7F) * 0.14F;
                wingPagesRight[i].zRot += ripple;
                wingPagesLeft[i].zRot -= ripple;
                wingPagesRight[i].xRot = Mth.sin(t * 0.2F + i) * 0.1F;
                wingPagesLeft[i].xRot = Mth.sin(t * 0.2F + i + 1.5F) * 0.1F;
            }
            // trailing strips stream behind, more so in a dive
            for (int i = 0; i < 3; i++) {
                float ph = i * 1.9F;
                tails[i].xRot += Mth.sin(t * 0.16F + ph) * 0.18F + dive * 0.7F;
                tails[i].zRot += Mth.sin(t * 0.11F + ph) * 0.1F;
                tailEnds[i].xRot += Mth.sin(t * 0.16F + ph - 0.9F) * 0.25F + dive * 0.4F;
            }
            // three loose pages orbit the swarm, pulled in tight while it dives
            float r = 8.5F - dive * 4.0F + tell * 1.5F;
            for (int i = 0; i < 3; i++) {
                float a = t * 0.07F + i * Mth.TWO_PI / 3.0F;
                ModelPart o = orbit[i];
                o.x = Mth.cos(a) * r;
                o.z = Mth.sin(a) * r;
                o.y = 9.0F + Mth.sin(t * 0.13F + i * 1.7F) * 2.5F;
                o.yRot = -a + Mth.HALF_PI;
                o.zRot = Mth.sin(t * 0.4F + i) * 0.35F;
                o.xRot = 0.2F + Mth.sin(t * 0.3F + i * 2) * 0.2F;
            }
            if (s.ward > 0) {
                float k = 1.0F + s.ward * 0.15F;
                wingRight.xScale = wingRight.yScale = wingRight.zScale = k;
                wingLeft.xScale = wingLeft.yScale = wingLeft.zScale = k;
            }
        }
    }
}
