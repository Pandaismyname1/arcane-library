package com.panda.arcane.client.creatures;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** Animation for the five Creatures A models. Rest poses live in the rigs; everything here is added on top. */
public final class CreatureModels {
    private CreatureModels() {}

    // ================================================================================================ Lumen Moth
    public static final class Moth extends CreatureModel {
        private final ModelPart body, head, abdomen, wingR, wingL, antR, antL;

        public Moth(ModelPart root) {
            super(root, CreatureLayers.LUMENMOTH_ONLY);
            body = part("body");
            head = part("head");
            abdomen = part("abdomen");
            wingR = part("wing_right");
            wingL = part("wing_left");
            antR = part("antenna_right");
            antL = part("antenna_left");
        }

        @Override
        protected void animate(CreatureRenderState s, float t, float pos, float speed) {
            float beat = Mth.sin(t * 1.35F);
            // wings beat around a raised V so they never sit edge-on for long
            float amp = 0.55F + 0.18F * speed;
            wingR.zRot += beat * amp + 0.42F;
            wingL.zRot -= beat * amp + 0.42F;
            wingR.yRot += 0.14F * Mth.cos(t * 1.35F);
            wingL.yRot -= 0.14F * Mth.cos(t * 1.35F);
            body.y += Mth.sin(t * 0.22F) * 1.3F - beat * 0.35F;
            body.xRot += 0.18F * speed + Mth.sin(t * 0.22F + 1.0F) * 0.06F;
            abdomen.xRot += Mth.sin(t * 0.22F + 0.6F) * 0.14F;
            head.yRot = Mth.clamp(s.yRot * DEG, -0.7F, 0.7F);
            head.xRot += Mth.clamp(s.xRot * DEG, -0.5F, 0.5F) * 0.5F;
            antR.xRot += Mth.sin(t * 0.31F) * 0.12F;
            antL.xRot += Mth.sin(t * 0.29F + 1.3F) * 0.12F;
            antR.zRot -= Mth.sin(t * 0.47F) * 0.06F;
            antL.zRot += Mth.sin(t * 0.43F) * 0.06F;
        }
    }

    // ================================================================================================ Silent Librarian
    public static final class Librarian extends CreatureModel {
        private final ModelPart skirt, torso, head, hoodTip, hoodPoint, armR, armL, book, pageR, pageL;

        public Librarian(ModelPart root) {
            super(root, CreatureLayers.SILENTLIBRARIAN_ONLY);
            skirt = part("skirt");
            torso = part("torso");
            head = part("head");
            hoodTip = part("hood_tip");
            hoodPoint = part("hood_point");
            armR = part("arm_right");
            armL = part("arm_left");
            book = part("book");
            pageR = part("page_right");
            pageL = part("page_left");
        }

        @Override
        protected void animate(CreatureRenderState s, float t, float pos, float speed) {
            // they glide: a slow hover, and the robe sways instead of legs stepping
            float bob = Mth.sin(t * 0.09F) * 0.7F;
            skirt.y += bob;
            torso.y += bob;
            float sway = Mth.sin(pos * 0.6F);
            skirt.zRot += sway * 0.07F * speed;
            skirt.yRot += sway * 0.09F * speed;
            skirt.xRot += 0.1F * speed;
            torso.yRot += sway * 0.06F * speed;
            torso.xRot += Mth.sin(t * 0.09F + 1.0F) * 0.03F + 0.06F * speed;
            head.yRot = Mth.clamp(s.yRot * DEG, -1.0F, 1.0F);
            head.xRot += Mth.clamp(s.xRot * DEG, -0.6F, 0.6F) * 0.6F;
            hoodTip.xRot += Mth.sin(t * 0.07F) * 0.06F - 0.12F * speed;
            hoodPoint.xRot += Mth.sin(t * 0.07F + 0.8F) * 0.1F - 0.15F * speed;
            armR.xRot += Mth.sin(t * 0.09F) * 0.04F;
            armL.xRot += Mth.sin(t * 0.09F + 0.5F) * 0.04F;
            pageR.zRot += Mth.sin(t * 0.21F) * 0.05F;
            pageL.zRot -= Mth.sin(t * 0.23F + 1.0F) * 0.05F;
            // a page turns now and then
            float turn = (t % 90.0F) / 12.0F;
            if (turn < 1.0F) pageR.zRot -= Mth.sin(turn * Mth.PI) * 0.9F;

            if (s.flag) { // a Clerk clutching its loot: hunched, arms pulled in
                torso.xRot += 0.22F;
                armR.xRot -= 0.25F;
                armL.xRot -= 0.25F;
                armR.zRot += 0.25F;
                armL.zRot -= 0.25F;
            }
            float k = s.attack;
            if (k > 0) { // a hex: the book is thrust forward
                float push = Mth.sin(k * Mth.PI);
                armR.xRot -= 0.55F * push;
                armL.xRot -= 0.55F * push;
                book.z -= 2.0F * push;
                book.xRot -= 0.35F * push;
            }
            float a = smooth(s.action);
            if (a > 0) { // the Shusher's tell: finger to the lips
                armR.xRot = Mth.lerp(a, armR.xRot, -2.25F);
                armR.zRot = Mth.lerp(a, armR.zRot, 0.55F);
                armR.yRot = Mth.lerp(a, armR.yRot, 0.2F);
                head.xRot += 0.18F * a;
                hoodPoint.xRot += 0.3F * a * Mth.sin(t * 0.8F);
            }
        }
    }

    // ================================================================================================ Ink Golem
    public static final class Golem extends CreatureModel {
        private final ModelPart legR, legL, waist, chest, head, quill, armR, armL, fistR, fistL;

        public Golem(ModelPart root) {
            super(root, CreatureLayers.INKGOLEM_ONLY);
            legR = part("right_leg");
            legL = part("left_leg");
            waist = part("waist");
            chest = part("chest");
            head = part("head");
            quill = part("quill");
            armR = part("right_arm");
            armL = part("left_arm");
            fistR = part("right_fist");
            fistL = part("left_fist");
        }

        @Override
        protected void animate(CreatureRenderState s, float t, float pos, float speed) {
            float swing = Mth.triangleWave(pos, 13.0F) * speed;
            float stomp = Mth.abs(Mth.sin(pos * 0.4833F)) * speed;
            legR.xRot = -0.9F * swing;
            legL.xRot = 0.9F * swing;
            chest.zRot += Mth.sin(pos * 0.4833F) * 0.07F * speed;
            chest.y += stomp * 0.9F;
            waist.y += stomp * 0.6F;
            chest.xRot += Mth.sin(t * 0.05F) * 0.03F;
            armR.xRot += 0.55F * swing;
            armL.xRot -= 0.55F * swing;
            fistR.xRot -= 0.25F * Math.max(0.0F, swing);
            fistL.xRot -= 0.25F * Math.max(0.0F, -swing);
            quill.zRot += Mth.sin(t * 0.08F) * 0.07F + swing * 0.12F;
            quill.xRot += Mth.cos(t * 0.06F) * 0.05F;
            head.yRot = Mth.clamp(s.yRot * DEG, -0.8F, 0.8F) * 0.8F;
            head.xRot += Mth.clamp(s.xRot * DEG, -0.5F, 0.5F) * 0.5F;

            float a = smooth(s.action);
            if (a > 0) { // the Iron-bound wind-up: rear back, both fists overhead
                chest.xRot -= 0.45F * a;
                head.xRot -= 0.25F * a;
                armR.xRot = Mth.lerp(a, armR.xRot, -3.0F);
                armL.xRot = Mth.lerp(a, armL.xRot, -3.0F);
                armR.zRot = Mth.lerp(a, armR.zRot, 0.25F);
                armL.zRot = Mth.lerp(a, armL.zRot, -0.25F);
                fistR.xRot -= 0.4F * a;
                fistL.xRot -= 0.4F * a;
            }
            float k = s.attack;
            if (k > 0 && a <= 0) { // the blow: fists come down from overhead
                float lift = k * k;
                armR.xRot -= 2.6F * lift;
                armL.xRot -= 2.6F * lift;
                chest.xRot += 0.25F * (1.0F - lift) * k * 4.0F * (1.0F - k);
            }
        }
    }

    // ================================================================================================ Vault Ward
    public static final class Ward extends CreatureModel {
        private final ModelPart helm, core, orbit, chain;
        private final ModelPart[] plates = new ModelPart[4], shields = new ModelPart[4];

        public Ward(ModelPart root) {
            super(root, CreatureLayers.VAULTWARD_ONLY);
            helm = part("helm");
            core = part("core");
            orbit = part("orbit");
            chain = part("chain");
            for (int i = 0; i < 4; i++) {
                plates[i] = part("plate_" + i);
                shields[i] = part("shield_" + i);
            }
        }

        @Override
        protected void animate(CreatureRenderState s, float t, float pos, float speed) {
            float bob = Mth.sin(t * 0.1F) * 1.1F;
            helm.y += bob;
            core.y += bob * 0.85F;
            orbit.y += bob * 0.85F;
            chain.y += bob * 0.85F;
            helm.yRot = s.yRot * DEG;
            helm.xRot = Mth.clamp(s.xRot * DEG, -0.5F, 0.5F) * 0.5F;
            core.yRot += t * 0.07F;
            float spin = switch (s.variantName) {
                case "bulwark" -> 0.022F;
                case "seeker" -> 0.09F;
                case "keys" -> 0.035F;
                default -> 0.05F;
            };
            if (s.flag) spin *= 2.2F;
            orbit.yRot = t * spin;
            for (int i = 0; i < 4; i++) {
                float wob = Mth.sin(t * 0.15F + i * 1.5708F);
                plates[i].y += wob * 0.7F;
                shields[i].y += wob * 0.4F;
                plates[i].xRot += wob * 0.08F;
            }
            chain.zRot = Mth.sin(t * 0.06F) * 0.16F;
            chain.xRot = Mth.cos(t * 0.05F) * 0.12F;
            float k = s.attack;
            if (k > 0) {
                float pulse = 1.0F + 0.55F * Mth.sin(k * Mth.PI);
                core.xScale = core.yScale = core.zScale = pulse;
                helm.xRot -= 0.25F * Mth.sin(k * Mth.PI);
            }
        }
    }

    // ================================================================================================ Inkling
    public static final class InklingModel extends CreatureModel {
        private final ModelPart body, tip, armR, armL, eyeR, eyeL;

        public InklingModel(ModelPart root) {
            super(root, CreatureLayers.INKLING_ONLY);
            body = part("body");
            tip = part("tip");
            armR = part("arm_right");
            armL = part("arm_left");
            eyeR = part("eye_right");
            eyeL = part("eye_left");
        }

        @Override
        protected void animate(CreatureRenderState s, float t, float pos, float speed) {
            // it hops: up in a stretched drop, down in a squashed splat
            float phase = pos * 0.75F;
            float hop = Mth.abs(Mth.sin(phase)) * speed;
            body.y -= hop * 3.2F;
            float stretch = 1.0F + (hop - 0.4F * speed) * 0.42F + Mth.sin(t * 0.2F) * 0.035F;
            float a = smooth(s.action);
            body.yScale = stretch * (1.0F + 0.3F * a);
            float wide = (1.0F / Mth.sqrt(stretch)) * (1.0F + 0.6F * a);
            body.xScale = wide;
            body.zScale = wide;
            if (a > 0) body.x += Mth.sin(t * 2.7F) * 0.35F * a;
            tip.xRot += Mth.sin(t * 0.15F) * 0.16F - hop * 0.5F;
            tip.zRot += Mth.sin(t * 0.11F) * 0.08F;
            armR.zRot += Mth.sin(t * 0.25F) * 0.15F + hop * 0.9F + a * 0.8F;
            armL.zRot -= Mth.sin(t * 0.25F + 0.4F) * 0.15F + hop * 0.9F + a * 0.8F;
            float look = Mth.clamp(s.yRot * DEG, -0.5F, 0.5F);
            eyeR.x += look * 0.6F;
            eyeL.x += look * 0.6F;
            float blink = (t % 70.0F) < 3.0F ? 0.15F : 1.0F;
            eyeR.yScale = blink;
            eyeL.yScale = blink;
            float k = s.attack;
            if (k > 0) body.xRot += 0.4F * Mth.sin(k * Mth.PI);
        }
    }
}
