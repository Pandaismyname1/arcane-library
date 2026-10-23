package com.panda.arcane.client.mobsb;

import com.panda.arcane.entity.BookendGargoyle;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/**
 * The Bookend Gargoyle: a horned stone gargoyle on a carved bookend slab. It blends between a crouched statue pose
 * and a hunched prowl; while frozen (Granite, watched) its animation clock stops, so it freezes mid-step.
 */
public final class GargoyleRenderer extends MobsBClient.VariantRenderer<BookendGargoyle, GargoyleRenderer.State, GargoyleRenderer.Model> {
    public GargoyleRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(MobsBClient.BOOKEND_GARGOYLE)), 0.5F, "bookend_gargoyle");
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BookendGargoyle entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        boolean frozen = entity.isFrozen();
        state.animAge = frozen ? entity.animAge : Mth.lerp(partialTicks, entity.animAgeO, entity.animAge);
        state.limbPos = entity.limbPos;
        state.limbSpeed = entity.limbSpeed;
        state.statue = entity.statueAmount(partialTicks);
        state.frozen = frozen;
        state.pounce = entity.pounce();
        if (frozen) {
            state.walkAnimationSpeed = 0;
            state.attack = 0;
        }
    }

    public static final class State extends MobsBClient.VariantState {
        public float animAge, limbPos, limbSpeed, statue;
        public boolean frozen;
        public int pounce;
    }

    public static final class Model extends EntityModel<State> {
        private static final float CROUCH_THIGH = 1.35F;
        private final ModelPart waist, torso, head, jaw;
        private final ModelPart armRight, armLeft, foreRight, foreLeft, wingRight, wingLeft;
        private final ModelPart legRight, legLeft, shinRight, shinLeft;

        public Model(ModelPart root) {
            super(root);
            Function<String, ModelPart> part = root.createPartLookup();
            waist = part.apply("waist");
            torso = part.apply("torso");
            head = part.apply("head");
            jaw = part.apply("jaw");
            armRight = part.apply("arm_right");
            armLeft = part.apply("arm_left");
            foreRight = part.apply("forearm_right");
            foreLeft = part.apply("forearm_left");
            wingRight = part.apply("wing_right");
            wingLeft = part.apply("wing_left");
            legRight = part.apply("leg_right");
            legLeft = part.apply("leg_left");
            shinRight = part.apply("shin_right");
            shinLeft = part.apply("shin_left");
        }

        @Override
        public void setupAnim(State s) {
            super.setupAnim(s);
            float t = s.animAge;
            float pos = s.limbPos, speed = s.frozen ? s.limbSpeed : Math.min(1.0F, s.limbSpeed);
            boolean tell = s.pounce == BookendGargoyle.POUNCE_TELL, air = s.pounce == BookendGargoyle.POUNCE_AIR,
                    recover = s.pounce == BookendGargoyle.POUNCE_RECOVER;
            float crouch = Math.max(s.statue, tell ? 1.0F : recover ? 0.55F : 0.0F);
            float stand = 1.0F - crouch;

            // ---- legs: walking swing when standing, folded under the body when crouched
            float swing = Mth.cos(pos * 0.6662F) * 0.9F * speed;
            // even standing it keeps its knees bent, a predator's crouch rather than a golem's stance
            float bend = CROUCH_THIGH * crouch + 0.4F * stand;
            legRight.xRot = -bend + swing * stand;
            legLeft.xRot = -bend - swing * stand;
            shinRight.xRot = bend * 1.1F + Math.max(0.0F, -swing) * 0.8F * stand;
            shinLeft.xRot = bend * 1.1F + Math.max(0.0F, swing) * 0.8F * stand;
            waist.y += 6.0F * (1.0F - Mth.cos(bend)) + 5.0F * (1.0F - Mth.cos(bend * 0.1F)) + (tell ? 1.0F : 0.0F);
            if (air) {
                legRight.xRot = 0.6F;
                legLeft.xRot = 0.5F;
                shinRight.xRot = 0.6F;
                shinLeft.xRot = 0.7F;
            }

            // ---- torso and head: hunched prowl, deep lean as a statue, coiled on the tell
            float breathe = s.statue > 0.5F || s.frozen ? 0.0F : Mth.sin(t * 0.08F) * 0.03F;
            torso.xRot = 0.5F * stand + 0.55F * crouch + breathe + (tell ? 0.25F : 0.0F) - (air ? 0.3F : 0.0F);
            torso.yRot = Mth.cos(pos * 0.6662F) * 0.12F * speed * stand;
            head.yRot = s.statue > 0.5F ? 0.0F : s.yRot * Mth.DEG_TO_RAD;
            head.xRot = (s.statue > 0.5F ? -0.35F : s.xRot * Mth.DEG_TO_RAD) - torso.xRot * 0.6F;
            jaw.xRot = air || tell ? 0.45F : s.attack > 0 ? Mth.sin(s.attack * Mth.PI) * 0.5F : 0.05F * (1 + Mth.sin(t * 0.1F)) * stand;

            // ---- arms: swing while walking, rest on the knees as a statue, reach forward in the air
            float armSwing = Mth.cos(pos * 0.6662F + Mth.PI) * 0.7F * speed * stand;
            // arms hang forward from the hunched shoulders, claws curled, ready to grab
            armRight.xRot = armSwing - 0.3F * stand - 0.85F * crouch;
            armLeft.xRot = -armSwing - 0.3F * stand - 0.85F * crouch;
            armRight.zRot = 0.2F * stand + 0.12F * crouch;
            armLeft.zRot = -0.2F * stand - 0.12F * crouch;
            foreRight.xRot = -0.7F * stand - 0.65F * crouch;
            foreLeft.xRot = -0.7F * stand - 0.65F * crouch;
            if (s.attack > 0) {
                float a = Mth.sin(s.attack * Mth.PI);
                armRight.xRot -= a * 1.6F;
                armLeft.xRot -= a * 1.3F;
            }
            if (air) {
                armRight.xRot = -2.3F;
                armLeft.xRot = -2.3F;
                foreRight.xRot = -0.2F;
                foreLeft.xRot = -0.2F;
            }

            // ---- wings: folded behind the slab as a statue, half open on the prowl, flared to pounce
            float flare = air ? 1.0F : tell ? 0.7F : stand * 0.35F;
            float flap = s.frozen || s.statue > 0.5F ? 0.0F : Mth.sin(t * 0.15F) * 0.05F;
            wingRight.yRot -= flare * 1.0F;
            wingLeft.yRot += flare * 1.0F;
            wingRight.zRot = flare * 0.5F + flap - 0.1F * crouch;
            wingLeft.zRot = -flare * 0.5F - flap + 0.1F * crouch;
            wingRight.xRot += flare * -0.2F;
            wingLeft.xRot += flare * -0.2F;
        }
    }
}
