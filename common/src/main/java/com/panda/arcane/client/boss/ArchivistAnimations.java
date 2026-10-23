package com.panda.arcane.client.boss;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

/**
 * The Archivist's clips. Rotations are offsets in degrees added to the rest pose; positions are pixels (+Y up).
 *
 * <p>Each attack clip holds its wind-up pose for exactly the telegraph in {@code ArchivistPattern.windup}
 * (ticks / 20 = seconds) and strikes on that frame:
 * Storm 30 t = 1.5 s, Erasure 40 t = 2.0 s (34 t = 1.7 s in phase 3: the clip strikes at 2.0 s and is cut short, which
 * reads as a faster slash), Recall 30 t = 1.5 s, Slam 24 t = 1.2 s, Index 40 t = 2.0 s, Transition 50 t = 2.5 s,
 * Stagger 50 t = 2.5 s.
 */
final class ArchivistAnimations {
    private ArchivistAnimations() {}

    private static final AnimationChannel.Interpolation L = AnimationChannel.Interpolations.LINEAR;
    private static final AnimationChannel.Interpolation C = AnimationChannel.Interpolations.CATMULLROM;

    /** Rotation keys: t, x, y, z, t, x, y, z, ... */
    private static AnimationChannel rot(AnimationChannel.Interpolation ip, float... k) {
        Keyframe[] out = new Keyframe[k.length / 4];
        for (int i = 0; i < out.length; i++) out[i] = new Keyframe(k[i * 4], KeyframeAnimations.degreeVec(k[i * 4 + 1], k[i * 4 + 2], k[i * 4 + 3]), ip);
        return new AnimationChannel(AnimationChannel.Targets.ROTATION, out);
    }

    private static AnimationChannel rot(float... k) {
        return rot(L, k);
    }

    private static AnimationChannel pos(float... k) {
        Keyframe[] out = new Keyframe[k.length / 4];
        for (int i = 0; i < out.length; i++) out[i] = new Keyframe(k[i * 4], KeyframeAnimations.posVec(k[i * 4 + 1], k[i * 4 + 2], k[i * 4 + 3]), L);
        return new AnimationChannel(AnimationChannel.Targets.POSITION, out);
    }

    /** Uniform scale keys: t, s, t, s, ... */
    private static AnimationChannel scale(float... k) {
        Keyframe[] out = new Keyframe[k.length / 2];
        for (int i = 0; i < out.length; i++) out[i] = new Keyframe(k[i * 2], KeyframeAnimations.scaleVec(k[i * 2 + 1], k[i * 2 + 1], k[i * 2 + 1]), L);
        return new AnimationChannel(AnimationChannel.Targets.SCALE, out);
    }

    // ---- idle: breathing, a bobbing grimoire, the halo of pages turning ----------------------------------------------
    static final AnimationDefinition IDLE = AnimationDefinition.Builder.withLength(4.0F).looping()
            .addAnimation("body", rot(C, 0, 0, 0, 0, 2, 2.5F, 0, 0, 4, 0, 0, 0))
            .addAnimation("head", rot(C, 0, 0, 0, 0, 1.2F, 3, 4, 0, 2.6F, -2, -3, 0, 4, 0, 0, 0))
            .addAnimation("tome", pos(0, 0, 0, 0, 1, 0, 0.8F, 0, 2, 0, 0, 0, 3, 0, -0.6F, 0, 4, 0, 0, 0))
            .addAnimation("tome", rot(C, 0, 0, 0, 0, 2, 4, 6, 0, 4, 0, 0, 0))
            .addAnimation("cover_left", rot(C, 0, 0, 0, 0, 2, 0, -5, 0, 4, 0, 0, 0))
            .addAnimation("cover_right", rot(C, 0, 0, 0, 0, 2, 0, 5, 0, 4, 0, 0, 0))
            .addAnimation("page_ring", rot(0, 0, 0, 0, 4, 0, 360, 0))
            .addAnimation("page_ring", pos(0, 0, 0, 0, 2, 0, 1.0F, 0, 4, 0, 0, 0))
            .addAnimation("back_books", pos(0, 0, 0, 0, 1.5F, 0, 0.6F, 0, 3, 0, -0.3F, 0, 4, 0, 0, 0))
            .addAnimation("right_arm", rot(C, 0, 0, 0, 0, 2, -3, 0, 2, 4, 0, 0, 0))
            .addAnimation("left_arm", rot(C, 0, 0, 0, 0, 2, 4, 0, 0, 4, 0, 0, 0))
            .build();

    // ---- Page Storm: both arms rise, the grimoire lifts overhead and opens flat, the halo spins up; release at 1.5 s
    static final AnimationDefinition STORM = AnimationDefinition.Builder.withLength(2.1F)
            .addAnimation("right_arm", rot(0, 0, 0, 0, 0.35F, -165, 0, 22, 0.9F, -160, 0, 26, 1.4F, -168, 0, 20, 1.5F, -95, 0, 6, 1.7F, -95, 0, 6, 2.1F, 0, 0, 0))
            .addAnimation("left_arm", rot(0, 0, 0, 0, 0.35F, -120, 14, -22, 0.9F, -115, 14, -26, 1.4F, -123, 14, -20, 1.5F, -50, 14, -6, 1.7F, -50, 14, -6, 2.1F, 0, 0, 0))
            .addAnimation("tome", pos(0, 0, 0, 0, 0.35F, -4, 11, 3, 1.4F, -4, 12, 3, 1.5F, -4, 3, -7, 1.7F, -4, 3, -7, 2.1F, 0, 0, 0))
            .addAnimation("tome", rot(0, 0, 0, 0, 0.35F, -41, -203, 0, 1.4F, -41, -203, 0, 1.5F, 49, -203, 0, 1.7F, 49, -203, 0, 2.1F, 0, 0, 0))
            .addAnimation("cover_left", rot(0, 0, 0, 0, 0.35F, 0, 17, 0, 1.7F, 0, 17, 0, 2.1F, 0, 0, 0))
            .addAnimation("cover_right", rot(0, 0, 0, 0, 0.35F, 0, -17, 0, 1.7F, 0, -17, 0, 2.1F, 0, 0, 0))
            .addAnimation("page_ring", rot(0, 0, 0, 0, 1.5F, 0, 900, 0, 2.1F, 0, 1000, 0))
            .addAnimation("page_ring", scale(0, 1, 0.35F, 1.5F, 1.4F, 1.6F, 1.5F, 1.9F, 2.1F, 1))
            .addAnimation("page_ring", pos(0, 0, 0, 0, 0.35F, 0, 6, 0, 1.4F, 0, 7, 0, 1.5F, 0, 2, 0, 2.1F, 0, 0, 0))
            .addAnimation("head", rot(0, 0, 0, 0, 0.35F, -22, 0, 0, 1.4F, -24, 0, 0, 1.5F, 12, 0, 0, 2.1F, 0, 0, 0))
            .addAnimation("body", rot(0, 0, 0, 0, 0.35F, -9, 0, 0, 1.4F, -10, 0, 0, 1.5F, 14, 0, 0, 1.7F, 14, 0, 0, 2.1F, 0, 0, 0))
            .build();

    // ---- Erasure: the quill is raised high and trembles, then slashes down across the body at 2.0 s -----------------
    static final AnimationDefinition ERASURE = AnimationDefinition.Builder.withLength(2.6F)
            .addAnimation("right_arm", rot(0, 0, 0, 0, 0.5F, -172, 0, 10, 0.8F, -168, 0, 12, 1.1F, -174, 0, 9, 1.4F, -168, 0, 12, 1.7F, -175, 0, 8,
                    1.95F, -172, 0, 10, 2.1F, -28, 0, -30, 2.3F, -28, 0, -30, 2.6F, 0, 0, 0))
            .addAnimation("quill", rot(0, 0, 0, 0, 0.5F, -15, 0, 0, 1.95F, -15, 0, 0, 2.1F, 20, 0, 0, 2.6F, 0, 0, 0))
            .addAnimation("left_arm", rot(0, 0, 0, 0, 0.5F, 30, 0, -45, 1.95F, 30, 0, -45, 2.1F, 15, 0, -20, 2.6F, 0, 0, 0))
            .addAnimation("body", rot(0, 0, 0, 0, 0.5F, -7, 16, 0, 1.95F, -8, 18, 0, 2.1F, 24, -22, 0, 2.3F, 24, -22, 0, 2.6F, 0, 0, 0))
            .addAnimation("head", rot(0, 0, 0, 0, 0.5F, -10, -10, 0, 1.95F, -10, -12, 0, 2.1F, 18, 10, 0, 2.6F, 0, 0, 0))
            .addAnimation("tome", pos(0, 0, 0, 0, 0.5F, 4, 2, 5, 2.3F, 4, 2, 5, 2.6F, 0, 0, 0))
            .addAnimation("tome", rot(0, 0, 0, 0, 0.5F, 0, 30, 0, 2.3F, 0, 30, 0, 2.6F, 0, 0, 0))
            .addAnimation("page_ring", scale(0, 1, 0.5F, 1.2F, 1.95F, 1.25F, 2.1F, 0.8F, 2.6F, 1))
            .build();

    // ---- Recall: the grimoire is held out toward the target, opened wide; at 1.5 s it snaps shut and is yanked back ---
    static final AnimationDefinition RECALL = AnimationDefinition.Builder.withLength(1.9F)
            .addAnimation("left_arm", rot(0, 0, 0, 0, 0.4F, -48, 16, 0, 1.45F, -50, 16, 0, 1.6F, 32, 10, -20, 1.9F, 30, 10, -20))
            .addAnimation("right_arm", rot(0, 0, 0, 0, 0.4F, -55, -10, 0, 1.45F, -58, -10, 0, 1.6F, -20, 0, 10, 1.9F, -20, 0, 10))
            .addAnimation("tome", pos(0, 0, 0, 0, 0.4F, 0, 3, -6, 1.45F, 0, 3, -7, 1.6F, -3, 0, 1, 1.9F, -3, 0, 1))
            .addAnimation("tome", rot(0, 0, 0, 0, 0.4F, 49, -203, 0, 1.45F, 49, -203, 0, 1.6F, 0, -23, 0, 1.9F, 0, -23, 0))
            .addAnimation("cover_left", rot(0, 0, 0, 0, 0.4F, 0, 22, 0, 1.45F, 0, 22, 0, 1.55F, 0, -55, 0, 1.9F, 0, -50, 0))
            .addAnimation("cover_right", rot(0, 0, 0, 0, 0.4F, 0, -22, 0, 1.45F, 0, -22, 0, 1.55F, 0, 55, 0, 1.9F, 0, 50, 0))
            .addAnimation("body", rot(0, 0, 0, 0, 0.4F, -6, -8, 0, 1.45F, -7, -8, 0, 1.6F, -16, 6, 0, 1.9F, -12, 0, 0))
            .addAnimation("head", rot(0, 0, 0, 0, 0.4F, 6, 0, 0, 1.45F, 6, 0, 0, 1.6F, -10, 0, 0, 1.9F, -6, 0, 0))
            .addAnimation("page_ring", scale(0, 1, 0.4F, 0.8F, 1.45F, 0.75F, 1.6F, 1.4F, 1.9F, 1.2F))
            .build();

    // ---- Page Slam: the closed grimoire is lifted overhead in both hands and brought down at 1.2 s -------------------
    static final AnimationDefinition SLAM = AnimationDefinition.Builder.withLength(2.0F)
            .addAnimation("right_arm", rot(0, 0, 0, 0, 0.4F, -172, 0, -12, 1.15F, -176, 0, -10, 1.3F, -40, 0, -4, 1.6F, -40, 0, -4, 2.0F, 0, 0, 0))
            .addAnimation("left_arm", rot(0, 0, 0, 0, 0.4F, -130, 14, 12, 1.15F, -134, 14, 10, 1.3F, 2, 14, 4, 1.6F, 2, 14, 4, 2.0F, 0, 0, 0))
            .addAnimation("quill", rot(0, 0, 0, 0, 0.4F, 0, 0, 40, 1.6F, 0, 0, 40, 2.0F, 0, 0, 0))
            .addAnimation("tome", pos(0, 0, 0, 0, 0.4F, -4, 17, 5, 1.15F, -4, 18, 5, 1.3F, -4, -5, -10, 1.6F, -4, -5, -10, 2.0F, 0, 0, 0))
            .addAnimation("tome", rot(0, 0, 0, 0, 0.4F, -25, -23, 0, 1.15F, -25, -23, 0, 1.3F, 95, -23, 0, 1.6F, 95, -23, 0, 2.0F, 0, 0, 0))
            .addAnimation("cover_left", rot(0, 0, 0, 0, 0.3F, 0, -52, 0, 1.6F, 0, -52, 0, 2.0F, 0, 0, 0))
            .addAnimation("cover_right", rot(0, 0, 0, 0, 0.3F, 0, 52, 0, 1.6F, 0, 52, 0, 2.0F, 0, 0, 0))
            .addAnimation("body", rot(0, 0, 0, 0, 0.4F, -13, 0, 0, 1.15F, -15, 0, 0, 1.3F, 30, 0, 0, 1.6F, 28, 0, 0, 2.0F, 0, 0, 0))
            .addAnimation("head", rot(0, 0, 0, 0, 0.4F, -20, 0, 0, 1.15F, -22, 0, 0, 1.3F, 22, 0, 0, 2.0F, 0, 0, 0))
            .addAnimation("skirt", rot(0, 0, 0, 0, 1.15F, 0, 0, 0, 1.3F, 8, 0, 0, 2.0F, 0, 0, 0))
            .addAnimation("page_ring", pos(0, 0, 0, 0, 0.4F, 0, 4, 0, 1.15F, 0, 5, 0, 1.3F, 0, -14, 0, 1.6F, 0, -14, 0, 2.0F, 0, 0, 0))
            .addAnimation("page_ring", scale(0, 1, 1.15F, 1, 1.3F, 1.8F, 1.6F, 1.8F, 2.0F, 1))
            .build();

    // ---- Index: arms spread wide, head thrown back, the halo widens; the wards appear at 2.0 s ------------------------
    static final AnimationDefinition INDEX = AnimationDefinition.Builder.withLength(2.5F)
            .addAnimation("right_arm", rot(0, 0, 0, 0, 0.5F, -8, 0, 82, 1.95F, -12, 0, 86, 2.15F, -70, 0, 20, 2.5F, 0, 0, 0))
            .addAnimation("left_arm", rot(0, 0, 0, 0, 0.5F, 35, 14, -82, 1.95F, 33, 14, -86, 2.15F, -25, 14, -20, 2.5F, 0, 0, 0))
            .addAnimation("head", rot(0, 0, 0, 0, 0.5F, -28, 0, 0, 1.95F, -30, 0, 0, 2.15F, 10, 0, 0, 2.5F, 0, 0, 0))
            .addAnimation("body", rot(0, 0, 0, 0, 0.5F, -7, 0, 0, 1.95F, -8, 0, 0, 2.15F, 8, 0, 0, 2.5F, 0, 0, 0))
            .addAnimation("tome", pos(0, 0, 0, 0, 0.5F, -4, 12, 2, 1.95F, -4, 13, 2, 2.5F, 0, 0, 0))
            .addAnimation("tome", rot(0, 0, 0, 0, 0.5F, -41, -203, 0, 1.95F, -41, -203, 0, 2.5F, 0, 0, 0))
            .addAnimation("cover_left", rot(0, 0, 0, 0, 0.5F, 0, 17, 0, 1.95F, 0, 17, 0, 2.5F, 0, 0, 0))
            .addAnimation("cover_right", rot(0, 0, 0, 0, 0.5F, 0, -17, 0, 1.95F, 0, -17, 0, 2.5F, 0, 0, 0))
            .addAnimation("page_ring", rot(0, 0, 0, 0, 2.0F, 0, 720, 0, 2.5F, 0, 760, 0))
            .addAnimation("page_ring", scale(0, 1, 0.5F, 1.9F, 1.95F, 2.0F, 2.15F, 1.2F, 2.5F, 1))
            .build();

    // ---- Transition: doubled over, clutching the hood, shaking; at 1.6 s it rises and flings its arms out -------------
    static final AnimationDefinition TRANSITION = AnimationDefinition.Builder.withLength(2.5F)
            .addAnimation("body", rot(0, 0, 0, 0, 0.4F, 30, 0, 0, 0.6F, 31, 0, 5, 0.8F, 30, 0, -5, 1.0F, 31, 0, 5, 1.2F, 30, 0, -5, 1.4F, 30, 0, 0,
                    1.7F, -16, 0, 0, 2.1F, -14, 0, 0, 2.5F, 0, 0, 0))
            .addAnimation("head", rot(0, 0, 0, 0, 0.4F, 25, 0, 0, 1.4F, 25, 0, 0, 1.7F, -32, 0, 0, 2.1F, -30, 0, 0, 2.5F, 0, 0, 0))
            .addAnimation("right_arm", rot(0, 0, 0, 0, 0.4F, -150, 0, -32, 1.4F, -150, 0, -32, 1.7F, -30, 0, 72, 2.1F, -30, 0, 70, 2.5F, 0, 0, 0))
            .addAnimation("left_arm", rot(0, 0, 0, 0, 0.4F, -108, 14, 32, 1.4F, -108, 14, 32, 1.7F, 13, 14, -72, 2.1F, 13, 14, -70, 2.5F, 0, 0, 0))
            .addAnimation("tome", pos(0, 0, 0, 0, 0.4F, -4, -7, -2, 1.4F, -4, -7, -2, 1.7F, -4, 13, 0, 2.1F, -4, 13, 0, 2.5F, 0, 0, 0))
            .addAnimation("tome", rot(0, 0, 0, 0, 0.4F, 65, -23, 0, 1.4F, 65, -23, 0, 1.7F, -41, -203, 0, 2.1F, -41, -203, 0, 2.5F, 0, 0, 0))
            .addAnimation("cover_left", rot(0, 0, 0, 0, 0.4F, 0, -52, 0, 1.4F, 0, -52, 0, 1.7F, 0, 17, 0, 2.5F, 0, 0, 0))
            .addAnimation("cover_right", rot(0, 0, 0, 0, 0.4F, 0, 52, 0, 1.4F, 0, 52, 0, 1.7F, 0, -17, 0, 2.5F, 0, 0, 0))
            .addAnimation("page_ring", scale(0, 1, 0.4F, 0.55F, 1.4F, 0.55F, 1.7F, 2.0F, 2.1F, 2.0F, 2.5F, 1))
            .addAnimation("page_ring", rot(0, 0, 0, 0, 1.4F, 0, -60, 0, 2.5F, 0, 600, 0))
            .build();

    // ---- Stagger: the tether snapped; slumped, limp, the grimoire sinks and the halo collapses ------------------------
    static final AnimationDefinition STAGGER = AnimationDefinition.Builder.withLength(2.5F)
            .addAnimation("body", rot(0, 0, 0, 0, 0.3F, 34, 0, 6, 0.8F, 36, 0, -6, 1.3F, 34, 0, 6, 1.8F, 36, 0, -6, 2.2F, 34, 0, 0, 2.5F, 0, 0, 0))
            .addAnimation("head", rot(0, 0, 0, 0, 0.3F, 30, 0, 15, 1.3F, 28, 0, -12, 2.2F, 30, 0, 10, 2.5F, 0, 0, 0))
            .addAnimation("right_arm", rot(0, 0, 0, 0, 0.3F, 12, 0, -6, 2.2F, 12, 0, -6, 2.5F, 0, 0, 0))
            .addAnimation("left_arm", rot(0, 0, 0, 0, 0.3F, 45, 14, 6, 2.2F, 45, 14, 6, 2.5F, 0, 0, 0))
            .addAnimation("tome", pos(0, 0, 0, 0, 0.3F, 0, -9, -1, 2.2F, 0, -9, -1, 2.5F, 0, 0, 0))
            .addAnimation("tome", rot(0, 0, 0, 0, 0.3F, 60, 0, 20, 2.2F, 60, 0, 20, 2.5F, 0, 0, 0))
            .addAnimation("cover_left", rot(0, 0, 0, 0, 0.3F, 0, -50, 0, 2.2F, 0, -50, 0, 2.5F, 0, 0, 0))
            .addAnimation("cover_right", rot(0, 0, 0, 0, 0.3F, 0, 50, 0, 2.2F, 0, 50, 0, 2.5F, 0, 0, 0))
            .addAnimation("page_ring", pos(0, 0, 0, 0, 0.3F, 0, -12, 0, 2.2F, 0, -12, 0, 2.5F, 0, 0, 0))
            .addAnimation("page_ring", scale(0, 1, 0.3F, 0.6F, 2.2F, 0.6F, 2.5F, 1))
            .build();
}
