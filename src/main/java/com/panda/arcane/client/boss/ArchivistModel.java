package com.panda.arcane.client.boss;

import com.panda.arcane.ArcaneMod;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The Archivist: a towering hooded keeper with a void for a face, a mantle, a robe that hides its feet, a giant quill
 * held like a staff, a grimoire that floats before its chest, a halo of orbiting pages and a stack of tomes bound to its
 * back. Vanilla model code; the clips are in {@link ArchivistAnimations}.
 *
 * <p>Texture offsets match the boss texture's layout: change both together.
 */
public class ArchivistModel extends EntityModel<ArchivistRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ArcaneMod.id("archivist"), "main");
    public static final int PAGES = 6;

    private final ModelPart head;
    private final ModelPart skirt;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart body;
    private final KeyframeAnimation idle;
    private final KeyframeAnimation storm;
    private final KeyframeAnimation erasure;
    private final KeyframeAnimation recall;
    private final KeyframeAnimation slam;
    private final KeyframeAnimation index;
    private final KeyframeAnimation transition;
    private final KeyframeAnimation stagger;

    public ArchivistModel(ModelPart root) {
        super(root);
        ModelPart main = root.getChild("root");
        this.body = main.getChild("body");
        this.head = body.getChild("head");
        this.skirt = main.getChild("skirt");
        this.rightArm = body.getChild("right_arm");
        this.leftArm = body.getChild("left_arm");
        this.idle = ArchivistAnimations.IDLE.bake(root);
        this.storm = ArchivistAnimations.STORM.bake(root);
        this.erasure = ArchivistAnimations.ERASURE.bake(root);
        this.recall = ArchivistAnimations.RECALL.bake(root);
        this.slam = ArchivistAnimations.SLAM.bake(root);
        this.index = ArchivistAnimations.INDEX.bake(root);
        this.transition = ArchivistAnimations.TRANSITION.bake(root);
        this.stagger = ArchivistAnimations.STAGGER.bake(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition top = mesh.getRoot();
        // everything hangs off "root", whose origin is the ground under the Archivist (model Y grows downward)
        PartDefinition root = top.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0, 24, 0));

        PartDefinition skirt = root.addOrReplaceChild("skirt", CubeListBuilder.create()
                .texOffs(0, 36).addBox(-6, 0, -4, 12, 11, 8), PartPose.offset(0, -12, 0));
        skirt.addOrReplaceChild("hem", CubeListBuilder.create()
                .texOffs(40, 36).addBox(-7, 8, -5, 14, 4, 10), PartPose.ZERO);

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-5, -12, -3, 10, 12, 6), PartPose.offset(0, -12, 0));
        body.addOrReplaceChild("mantle", CubeListBuilder.create()
                .texOffs(32, 16).addBox(-7, -12.5F, -4, 14, 5, 8), PartPose.ZERO);

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8)
                .texOffs(32, 0).addBox(-4, -8, -4, 8, 8, 8, new CubeDeformation(0.6F)), PartPose.offset(0, -12, 0));
        head.addOrReplaceChild("hood_tip", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-1.5F, -4, -1.5F, 3, 4, 3), PartPose.offsetAndRotation(0, -8.2F, 2.6F, -0.6F, 0, 0));

        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(80, 16).addBox(-2, -1, -2, 4, 13, 4), PartPose.offsetAndRotation(-7, -11, 0, -0.1F, 0, 0.12F));
        rightArm.addOrReplaceChild("right_cuff", CubeListBuilder.create()
                .texOffs(88, 36).addBox(-2.5F, 8, -2.5F, 5, 5, 5), PartPose.ZERO);
        rightArm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(112, 16).addBox(-1.5F, 12, -1.5F, 3, 3, 3), PartPose.ZERO);
        PartDefinition quill = rightArm.addOrReplaceChild("quill", CubeListBuilder.create()
                .texOffs(52, 56).addBox(-0.5F, -21, -0.5F, 1, 25, 1, new CubeDeformation(0.2F)),
                PartPose.offsetAndRotation(0, 13.5F, -0.5F, 0, 0, -0.12F));
        quill.addOrReplaceChild("vane", CubeListBuilder.create()
                .texOffs(56, 56).addBox(-3, -21, -0.5F, 6, 14, 1), PartPose.ZERO);

        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(96, 16).addBox(-2, -1, -2, 4, 13, 4), PartPose.offsetAndRotation(7, -11, 0, -0.75F, -0.25F, -0.1F));
        leftArm.addOrReplaceChild("left_cuff", CubeListBuilder.create()
                .texOffs(108, 36).addBox(-2.5F, 8, -2.5F, 5, 5, 5), PartPose.ZERO);
        leftArm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(112, 24).addBox(-1.5F, 12, -1.5F, 3, 3, 3), PartPose.ZERO);

        // the grimoire floats before the chest, open toward its reader; pages are on the cover's -Z side
        PartDefinition tome = body.addOrReplaceChild("tome", CubeListBuilder.create()
                .texOffs(80, 0).addBox(-1, -4.5F, -0.5F, 2, 9, 1), PartPose.offsetAndRotation(4.0F, -4.5F, -7.5F, -0.85F, Mth.PI + 0.4F, 0));
        PartDefinition coverL = tome.addOrReplaceChild("cover_left", CubeListBuilder.create()
                .texOffs(0, 56).addBox(-6, -4.5F, -0.5F, 6, 9, 1), PartPose.offsetAndRotation(-1, 0, 0, 0, -0.3F, 0));
        coverL.addOrReplaceChild("pages_left", CubeListBuilder.create()
                .texOffs(28, 56).addBox(-5.5F, -4, -1.5F, 5, 8, 1), PartPose.ZERO);
        PartDefinition coverR = tome.addOrReplaceChild("cover_right", CubeListBuilder.create()
                .texOffs(14, 56).addBox(0, -4.5F, -0.5F, 6, 9, 1), PartPose.offsetAndRotation(1, 0, 0, 0, 0.3F, 0));
        coverR.addOrReplaceChild("pages_right", CubeListBuilder.create()
                .texOffs(40, 56).addBox(0.5F, -4, -1.5F, 5, 8, 1), PartPose.ZERO);

        // a stack of tomes bound across the back
        PartDefinition books = body.addOrReplaceChild("back_books", CubeListBuilder.create(), PartPose.offset(0, -16, 4.5F));
        books.addOrReplaceChild("book_a", CubeListBuilder.create().texOffs(80, 56).addBox(-1.5F, -4.5F, 0, 3, 9, 6),
                PartPose.offsetAndRotation(-3, 0, 0, 0, 0, 0.1F));
        books.addOrReplaceChild("book_b", CubeListBuilder.create().texOffs(98, 56).addBox(-1.5F, -3.5F, 0.5F, 3, 7, 5),
                PartPose.offsetAndRotation(0.3F, 0.6F, 0, 0, 0, -0.04F));
        books.addOrReplaceChild("book_c", CubeListBuilder.create().texOffs(80, 72).addBox(-1, -4, 0, 2, 8, 5),
                PartPose.offsetAndRotation(3.2F, 0, 0.2F, 0, 0, -0.14F));

        // the halo of pages
        // the halo of pages turns above the hood, clear of the face
        PartDefinition ring = root.addOrReplaceChild("page_ring", CubeListBuilder.create(), PartPose.offset(0, -37, 0));
        for (int i = 0; i < PAGES; i++) {
            float a = i * Mth.TWO_PI / PAGES;
            ring.addOrReplaceChild("page_" + i, CubeListBuilder.create().texOffs(70, 56).addBox(-2, -2.5F, -0.5F, 4, 5, 1),
                    PartPose.offsetAndRotation(Mth.sin(a) * 7.5F, (i % 2) * 1.6F - 0.8F, Mth.cos(a) * 7.5F, -0.35F, a, 0.12F));
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(ArchivistRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        // walking: the robe sways and the sleeves swing a little (the Archivist has no visible legs)
        if (!state.hovering) {
            float swing = state.walkAnimationPos * 0.55F;
            float amount = Math.min(1F, state.walkAnimationSpeed);
            skirt.zRot += Mth.sin(swing) * 0.07F * amount;
            skirt.xRot += Math.abs(Mth.cos(swing)) * 0.05F * amount;
            body.yRot += Mth.sin(swing) * 0.06F * amount;
            if (!state.casting) {
                rightArm.xRot += Mth.cos(swing) * 0.35F * amount;
                leftArm.xRot -= Mth.cos(swing) * 0.15F * amount;
            }
        } else {
            // hovering: the robe flutters below
            skirt.xRot += 0.12F + Mth.sin(age * 0.21F) * 0.06F;
            skirt.zRot += Mth.cos(age * 0.17F) * 0.05F;
        }
        idle.apply(state.idle, age);
        storm.apply(state.storm, age);
        erasure.apply(state.erasure, age);
        recall.apply(state.recall, age);
        slam.apply(state.slam, age);
        index.apply(state.index, age);
        transition.apply(state.transition, age);
        stagger.apply(state.stagger, age);
        // the hood follows the target, less while casting so the pose reads
        float look = state.casting ? 0.4F : 1F;
        head.yRot += state.yRot * Mth.DEG_TO_RAD * look;
        head.xRot += state.xRot * Mth.DEG_TO_RAD * look;
    }
}
