package com.panda.arcane.client.mobsb;

import java.util.EnumSet;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.Direction;

/** Geometry and UV layout of the second creature set's models; the textures are painted on exactly this layout. */
public final class MobsBLayers {
    private MobsBLayers() {}

    public static LayerDefinition pageWraith() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(30, 0).addBox(-3.5F, -6.0F, -2.0F, 7.0F, 6.0F, 4.0F).texOffs(26, 32).addBox(-2.5F, 0.0F, -1.5F, 5.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 11.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 23).addBox(-3.5F, -7.0F, -4.0F, 7.0F, 1.0F, 7.0F).texOffs(0, 0).addBox(-3.5F, -6.0F, -3.0F, 1.0F, 6.0F, 6.0F).texOffs(15, 0).addBox(2.5F, -6.0F, -3.0F, 1.0F, 6.0F, 6.0F).texOffs(0, 32).addBox(-2.5F, -6.0F, 2.0F, 5.0F, 6.0F, 1.0F).texOffs(13, 32).addBox(-2.5F, -6.0F, -1.5F, 5.0F, 6.0F, 1.0F).texOffs(39, 40).addBox(-1.5F, -8.0F, 0.0F, 3.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -6.0F, -0.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition wing_right = body.addOrReplaceChild("wing_right", CubeListBuilder.create(), PartPose.offsetAndRotation(-3.0F, -5.0F, 1.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition wing_left = body.addOrReplaceChild("wing_left", CubeListBuilder.create(), PartPose.offsetAndRotation(3.0F, -5.0F, 1.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition wing_right_0 = wing_right.addOrReplaceChild("wing_right_0", CubeListBuilder.create().texOffs(0, 13).addBox(-7.0F, -5.0F, 0.0F, 7.0F, 9.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.15F, -0.55F));
        PartDefinition wing_left_0 = wing_left.addOrReplaceChild("wing_left_0", CubeListBuilder.create().texOffs(15, 13).addBox(0.0F, -5.0F, 0.0F, 7.0F, 9.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.15F, 0.55F));
        PartDefinition wing_right_1 = wing_right.addOrReplaceChild("wing_right_1", CubeListBuilder.create().texOffs(30, 13).addBox(-7.0F, -5.0F, 0.0F, 7.0F, 9.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.4F, 0.0F, 0.3F, 0.05F));
        PartDefinition wing_left_1 = wing_left.addOrReplaceChild("wing_left_1", CubeListBuilder.create().texOffs(45, 13).addBox(0.0F, -5.0F, 0.0F, 7.0F, 9.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.4F, 0.0F, -0.3F, -0.05F));
        PartDefinition wing_right_2 = wing_right.addOrReplaceChild("wing_right_2", CubeListBuilder.create().texOffs(29, 23).addBox(-6.0F, -4.0F, 0.0F, 6.0F, 8.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.8F, 0.0F, 0.45F, 0.6F));
        PartDefinition wing_left_2 = wing_left.addOrReplaceChild("wing_left_2", CubeListBuilder.create().texOffs(42, 23).addBox(0.0F, -4.0F, 0.0F, 6.0F, 8.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.8F, 0.0F, -0.45F, -0.6F));
        PartDefinition tail_mid = body.addOrReplaceChild("tail_mid", CubeListBuilder.create().texOffs(43, 32).addBox(-2.0F, 0.0F, 0.0F, 4.0F, 5.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)), PartPose.offsetAndRotation(0.0F, 3.0F, 0.0F, 0.35F, 0.0F, 0.0F));
        PartDefinition tail_mid_end = tail_mid.addOrReplaceChild("tail_mid_end", CubeListBuilder.create().texOffs(18, 40).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 5.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)), PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, 0.25F, 0.0F, 0.0F));
        PartDefinition tail_right = body.addOrReplaceChild("tail_right", CubeListBuilder.create().texOffs(25, 40).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 5.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)), PartPose.offsetAndRotation(-2.0F, 3.0F, 0.5F, 0.35F, 0.0F, 0.25F));
        PartDefinition tail_right_end = tail_right.addOrReplaceChild("tail_right_end", CubeListBuilder.create().texOffs(52, 40).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 4.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)), PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, 0.25F, 0.0F, 0.0F));
        PartDefinition tail_left = body.addOrReplaceChild("tail_left", CubeListBuilder.create().texOffs(32, 40).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 5.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)), PartPose.offsetAndRotation(2.0F, 3.0F, 0.5F, 0.35F, 0.0F, -0.25F));
        PartDefinition tail_left_end = tail_left.addOrReplaceChild("tail_left_end", CubeListBuilder.create().texOffs(57, 40).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 4.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)), PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, 0.25F, 0.0F, 0.0F));
        PartDefinition orbit_0 = root.addOrReplaceChild("orbit_0", CubeListBuilder.create().texOffs(52, 32).addBox(-2.0F, -2.5F, 0.0F, 4.0F, 5.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)), PartPose.offsetAndRotation(0.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition orbit_1 = root.addOrReplaceChild("orbit_1", CubeListBuilder.create().texOffs(0, 40).addBox(-2.0F, -2.5F, 0.0F, 4.0F, 5.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)), PartPose.offsetAndRotation(0.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition orbit_2 = root.addOrReplaceChild("orbit_2", CubeListBuilder.create().texOffs(9, 40).addBox(-2.0F, -2.5F, 0.0F, 4.0F, 5.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)), PartPose.offsetAndRotation(0.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition tomeMimic() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition book = root.addOrReplaceChild("book", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition lower = book.addOrReplaceChild("lower", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -1.0F, -5.0F, 12.0F, 1.0F, 10.0F).texOffs(0, 12).addBox(-5.5F, -3.0F, -4.5F, 11.0F, 2.0F, 9.0F).texOffs(39, 24).addBox(-6.0F, -6.0F, 5.0F, 12.0F, 6.0F, 1.0F).texOffs(0, 34).addBox(-4.0F, -4.5F, -4.25F, 1.0F, 2.0F, 1.0F).texOffs(5, 34).addBox(-1.0F, -4.5F, -4.25F, 1.0F, 2.0F, 1.0F).texOffs(10, 34).addBox(2.5F, -4.5F, -4.25F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition lid = book.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(45, 0).addBox(-6.0F, -1.0F, -10.0F, 12.0F, 1.0F, 10.0F).texOffs(41, 12).addBox(-5.5F, 0.0F, -9.5F, 11.0F, 2.0F, 9.0F).texOffs(15, 34).addBox(-5.0F, 1.5F, -9.25F, 1.0F, 2.0F, 1.0F).texOffs(20, 34).addBox(-2.5F, 1.5F, -9.25F, 1.0F, 2.0F, 1.0F).texOffs(25, 34).addBox(0.5F, 1.5F, -9.25F, 1.0F, 2.0F, 1.0F).texOffs(30, 34).addBox(3.5F, 1.5F, -9.25F, 1.0F, 2.0F, 1.0F).texOffs(109, 24).addBox(-4.5F, -2.0F, -8.5F, 2.0F, 1.0F, 2.0F).texOffs(118, 24).addBox(2.5F, -2.0F, -8.5F, 2.0F, 1.0F, 2.0F).texOffs(102, 24).addBox(-1.0F, -0.5F, -10.25F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -5.0F, 5.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition tongue = lower.addOrReplaceChild("tongue", CubeListBuilder.create().texOffs(40, 34).addBox(-1.0F, 0.0F, -2.0F, 2.0F, 0.0F, 2.0F, EnumSet.of(Direction.DOWN, Direction.UP)), PartPose.offsetAndRotation(0.5F, -3.0F, -4.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition tongue_tip = tongue.addOrReplaceChild("tongue_tip", CubeListBuilder.create().texOffs(35, 34).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)), PartPose.offsetAndRotation(0.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition flip_0 = lower.addOrReplaceChild("flip_0", CubeListBuilder.create().texOffs(82, 12).addBox(-5.0F, 0.0F, -9.0F, 10.0F, 0.0F, 9.0F, EnumSet.of(Direction.DOWN, Direction.UP)), PartPose.offsetAndRotation(0.0F, -3.2F, 4.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition flip_1 = lower.addOrReplaceChild("flip_1", CubeListBuilder.create().texOffs(0, 24).addBox(-5.0F, 0.0F, -9.0F, 10.0F, 0.0F, 9.0F, EnumSet.of(Direction.DOWN, Direction.UP)), PartPose.offsetAndRotation(0.0F, -3.4F, 4.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition leg_front_right = root.addOrReplaceChild("leg_front_right", CubeListBuilder.create().texOffs(66, 24).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(-4.0F, 21.0F, -3.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition leg_front_left = root.addOrReplaceChild("leg_front_left", CubeListBuilder.create().texOffs(75, 24).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(4.0F, 21.0F, -3.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition leg_back_right = root.addOrReplaceChild("leg_back_right", CubeListBuilder.create().texOffs(84, 24).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(-4.0F, 21.0F, 3.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition leg_back_left = root.addOrReplaceChild("leg_back_left", CubeListBuilder.create().texOffs(93, 24).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(4.0F, 21.0F, 3.0F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    public static LayerDefinition bookwyrm() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition seg0 = root.addOrReplaceChild("seg0", CubeListBuilder.create().texOffs(35, 0).addBox(-3.5F, -3.0F, 0.0F, 7.0F, 6.0F, 5.0F).texOffs(17, 26).addBox(0.0F, -6.0F, 1.0F, 0.0F, 3.0F, 4.0F, EnumSet.of(Direction.WEST, Direction.EAST)), PartPose.offsetAndRotation(0.0F, 21.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition seg1 = seg0.addOrReplaceChild("seg1", CubeListBuilder.create().texOffs(60, 0).addBox(-3.5F, -3.0F, 0.0F, 7.0F, 6.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 5.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition seg2 = seg1.addOrReplaceChild("seg2", CubeListBuilder.create().texOffs(85, 0).addBox(-3.0F, -2.0F, 0.0F, 6.0F, 5.0F, 5.0F).texOffs(26, 26).addBox(0.0F, -5.0F, 1.0F, 0.0F, 3.0F, 4.0F, EnumSet.of(Direction.WEST, Direction.EAST)), PartPose.offsetAndRotation(0.0F, 0.0F, 5.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition seg3 = seg2.addOrReplaceChild("seg3", CubeListBuilder.create().texOffs(0, 15).addBox(-3.0F, -2.0F, 0.0F, 6.0F, 5.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 5.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition seg4 = seg3.addOrReplaceChild("seg4", CubeListBuilder.create().texOffs(65, 15).addBox(-2.5F, -1.0F, 0.0F, 5.0F, 4.0F, 4.0F).texOffs(35, 26).addBox(0.0F, -4.0F, 1.0F, 0.0F, 3.0F, 3.0F, EnumSet.of(Direction.WEST, Direction.EAST)), PartPose.offsetAndRotation(0.0F, 0.0F, 5.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition seg5 = seg4.addOrReplaceChild("seg5", CubeListBuilder.create().texOffs(84, 15).addBox(-2.0F, 0.0F, 0.0F, 4.0F, 3.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 4.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition tail = seg5.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(101, 15).addBox(-1.5F, 1.0F, 0.0F, 3.0F, 2.0F, 5.0F).texOffs(23, 15).addBox(0.0F, -3.0F, 1.0F, 0.0F, 4.0F, 6.0F, EnumSet.of(Direction.WEST, Direction.EAST)), PartPose.offsetAndRotation(0.0F, 0.0F, 4.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition head = seg0.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -9.0F, 8.0F, 5.0F, 9.0F).texOffs(42, 26).addBox(-4.5F, -3.0F, -7.0F, 1.0F, 2.0F, 2.0F).texOffs(49, 26).addBox(3.5F, -3.0F, -7.0F, 1.0F, 2.0F, 2.0F).texOffs(0, 26).addBox(-1.0F, -5.0F, -7.0F, 2.0F, 1.0F, 6.0F).texOffs(56, 26).addBox(-3.5F, 1.0F, -8.75F, 7.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -4.0F, 1.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(36, 15).addBox(-3.5F, 0.0F, -7.5F, 7.0F, 2.0F, 7.0F).texOffs(73, 26).addBox(-3.0F, -1.0F, -7.25F, 6.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 1.0F, -1.0F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    public static LayerDefinition quillStalker() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(33, 0).addBox(-3.0F, -2.5F, -3.0F, 6.0F, 5.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 17.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(10, 17).addBox(-2.5F, -2.5F, -4.0F, 5.0F, 5.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition nib_right = head.addOrReplaceChild("nib_right", CubeListBuilder.create().texOffs(0, 29).addBox(-0.5F, -0.5F, -4.0F, 1.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(-1.5F, 1.5F, -4.0F, 0.0F, 0.3F, 0.0F));
        PartDefinition nib_left = head.addOrReplaceChild("nib_left", CubeListBuilder.create().texOffs(11, 29).addBox(-0.5F, -0.5F, -4.0F, 1.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(1.5F, 1.5F, -4.0F, 0.0F, -0.3F, 0.0F));
        PartDefinition abdomen = body.addOrReplaceChild("abdomen", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 8.0F).texOffs(50, 17).addBox(-1.5F, -6.0F, 2.5F, 3.0F, 2.0F, 3.0F).texOffs(30, 39).addBox(-1.0F, -7.0F, 3.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -1.0F, 3.0F, -0.3F, 0.0F, 0.0F));
        PartDefinition back_quill_0 = abdomen.addOrReplaceChild("back_quill_0", CubeListBuilder.create().texOffs(58, 0).addBox(-0.5F, -10.0F, -0.5F, 1.0F, 10.0F, 1.0F).texOffs(29, 17).addBox(-1.5F, -10.0F, 0.0F, 3.0F, 7.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)), PartPose.offsetAndRotation(-2.5F, -4.0F, 4.0F, -0.75F, 0.0F, -0.35F));
        PartDefinition back_quill_1 = abdomen.addOrReplaceChild("back_quill_1", CubeListBuilder.create().texOffs(0, 17).addBox(-0.5F, -10.0F, -0.5F, 1.0F, 10.0F, 1.0F).texOffs(36, 17).addBox(-1.5F, -10.0F, 0.0F, 3.0F, 7.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)), PartPose.offsetAndRotation(0.0F, -4.0F, 5.5F, -0.85F, 0.0F, 0.0F));
        PartDefinition back_quill_2 = abdomen.addOrReplaceChild("back_quill_2", CubeListBuilder.create().texOffs(5, 17).addBox(-0.5F, -10.0F, -0.5F, 1.0F, 10.0F, 1.0F).texOffs(43, 17).addBox(-1.5F, -10.0F, 0.0F, 3.0F, 7.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)), PartPose.offsetAndRotation(2.5F, -4.0F, 4.0F, -0.75F, 0.0F, 0.35F));
        PartDefinition leg_right_0 = root.addOrReplaceChild("leg_right_0", CubeListBuilder.create().texOffs(25, 52).addBox(-7.0F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-2.5F, 17.0F, -2.25F, 0.0F, -0.75F, 0.65F));
        PartDefinition leg_right_0_tip = leg_right_0.addOrReplaceChild("leg_right_0_tip", CubeListBuilder.create().texOffs(39, 39).addBox(-11.0F, -0.5F, -0.5F, 11.0F, 1.0F, 1.0F).texOffs(22, 29).addBox(-9.0F, -1.5F, 0.0F, 7.0F, 3.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)), PartPose.offsetAndRotation(-7.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.95F));
        PartDefinition leg_right_1 = root.addOrReplaceChild("leg_right_1", CubeListBuilder.create().texOffs(42, 52).addBox(-7.0F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-2.5F, 17.0F, -0.75F, 0.0F, -0.25F, 0.65F));
        PartDefinition leg_right_1_tip = leg_right_1.addOrReplaceChild("leg_right_1_tip", CubeListBuilder.create().texOffs(0, 43).addBox(-11.0F, -0.5F, -0.5F, 11.0F, 1.0F, 1.0F).texOffs(37, 29).addBox(-9.0F, -1.5F, 0.0F, 7.0F, 3.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)), PartPose.offsetAndRotation(-7.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.95F));
        PartDefinition leg_right_2 = root.addOrReplaceChild("leg_right_2", CubeListBuilder.create().texOffs(0, 55).addBox(-7.0F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-2.5F, 17.0F, 0.75F, 0.0F, 0.25F, 0.65F));
        PartDefinition leg_right_2_tip = leg_right_2.addOrReplaceChild("leg_right_2_tip", CubeListBuilder.create().texOffs(25, 43).addBox(-11.0F, -0.5F, -0.5F, 11.0F, 1.0F, 1.0F).texOffs(0, 35).addBox(-9.0F, -1.5F, 0.0F, 7.0F, 3.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)), PartPose.offsetAndRotation(-7.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.95F));
        PartDefinition leg_right_3 = root.addOrReplaceChild("leg_right_3", CubeListBuilder.create().texOffs(17, 55).addBox(-7.0F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-2.5F, 17.0F, 2.25F, 0.0F, 0.75F, 0.65F));
        PartDefinition leg_right_3_tip = leg_right_3.addOrReplaceChild("leg_right_3_tip", CubeListBuilder.create().texOffs(0, 46).addBox(-11.0F, -0.5F, -0.5F, 11.0F, 1.0F, 1.0F).texOffs(15, 35).addBox(-9.0F, -1.5F, 0.0F, 7.0F, 3.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)), PartPose.offsetAndRotation(-7.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.95F));
        PartDefinition leg_left_0 = root.addOrReplaceChild("leg_left_0", CubeListBuilder.create().texOffs(34, 55).addBox(0.0F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(2.5F, 17.0F, -2.25F, 0.0F, 0.75F, -0.65F));
        PartDefinition leg_left_0_tip = leg_left_0.addOrReplaceChild("leg_left_0_tip", CubeListBuilder.create().texOffs(25, 46).addBox(0.0F, -0.5F, -0.5F, 11.0F, 1.0F, 1.0F).texOffs(30, 35).addBox(2.0F, -1.5F, 0.0F, 7.0F, 3.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)), PartPose.offsetAndRotation(7.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.95F));
        PartDefinition leg_left_1 = root.addOrReplaceChild("leg_left_1", CubeListBuilder.create().texOffs(0, 58).addBox(0.0F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(2.5F, 17.0F, -0.75F, 0.0F, 0.25F, -0.65F));
        PartDefinition leg_left_1_tip = leg_left_1.addOrReplaceChild("leg_left_1_tip", CubeListBuilder.create().texOffs(0, 49).addBox(0.0F, -0.5F, -0.5F, 11.0F, 1.0F, 1.0F).texOffs(45, 35).addBox(2.0F, -1.5F, 0.0F, 7.0F, 3.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)), PartPose.offsetAndRotation(7.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.95F));
        PartDefinition leg_left_2 = root.addOrReplaceChild("leg_left_2", CubeListBuilder.create().texOffs(17, 58).addBox(0.0F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(2.5F, 17.0F, 0.75F, 0.0F, -0.25F, -0.65F));
        PartDefinition leg_left_2_tip = leg_left_2.addOrReplaceChild("leg_left_2_tip", CubeListBuilder.create().texOffs(25, 49).addBox(0.0F, -0.5F, -0.5F, 11.0F, 1.0F, 1.0F).texOffs(0, 39).addBox(2.0F, -1.5F, 0.0F, 7.0F, 3.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)), PartPose.offsetAndRotation(7.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.95F));
        PartDefinition leg_left_3 = root.addOrReplaceChild("leg_left_3", CubeListBuilder.create().texOffs(34, 58).addBox(0.0F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(2.5F, 17.0F, 2.25F, 0.0F, -0.75F, -0.65F));
        PartDefinition leg_left_3_tip = leg_left_3.addOrReplaceChild("leg_left_3_tip", CubeListBuilder.create().texOffs(0, 52).addBox(0.0F, -0.5F, -0.5F, 11.0F, 1.0F, 1.0F).texOffs(15, 39).addBox(2.0F, -1.5F, 0.0F, 7.0F, 3.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)), PartPose.offsetAndRotation(7.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.95F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition gargoyle() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition waist = root.addOrReplaceChild("waist", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition torso = waist.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(73, 0).addBox(-4.5F, -8.0F, -3.0F, 9.0F, 3.0F, 6.0F).texOffs(25, 0).addBox(-3.5F, -5.0F, -2.5F, 7.0F, 5.0F, 5.0F).texOffs(0, 0).addBox(-5.0F, -10.0F, 3.2F, 10.0F, 12.0F, 2.0F).texOffs(73, 25).addBox(-5.5F, -11.0F, 2.5F, 11.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition head = torso.addOrReplaceChild("head", CubeListBuilder.create().texOffs(50, 0).addBox(-3.0F, -5.0F, -4.0F, 6.0F, 5.0F, 5.0F).texOffs(102, 25).addBox(-2.0F, -3.0F, -6.0F, 4.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -8.0F, -1.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition horn_right = head.addOrReplaceChild("horn_right", CubeListBuilder.create().texOffs(115, 25).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(-2.0F, -5.0F, -1.0F, -0.5F, 0.0F, -0.35F));
        PartDefinition horn_right_tip = horn_right.addOrReplaceChild("horn_right_tip", CubeListBuilder.create().texOffs(0, 31).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, -0.7F, 0.0F, 0.0F));
        PartDefinition horn_left = head.addOrReplaceChild("horn_left", CubeListBuilder.create().texOffs(120, 25).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(2.0F, -5.0F, -1.0F, -0.5F, 0.0F, 0.35F));
        PartDefinition horn_left_tip = horn_left.addOrReplaceChild("horn_left_tip", CubeListBuilder.create().texOffs(5, 31).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, -0.7F, 0.0F, 0.0F));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(0, 25).addBox(-2.0F, -0.5F, -4.5F, 4.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition arm_right = torso.addOrReplaceChild("arm_right", CubeListBuilder.create().texOffs(104, 0).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 6.0F, 3.0F), PartPose.offsetAndRotation(-5.0F, -7.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition forearm_right = arm_right.addOrReplaceChild("forearm_right", CubeListBuilder.create().texOffs(103, 15).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F).texOffs(47, 25).addBox(-1.5F, 4.0F, -1.5F, 3.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition wing_right = torso.addOrReplaceChild("wing_right", CubeListBuilder.create().texOffs(10, 31).addBox(-9.0F, -1.0F, -0.5F, 9.0F, 1.0F, 1.0F).texOffs(39, 15).addBox(-9.0F, 0.0F, 0.0F, 9.0F, 8.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)), PartPose.offsetAndRotation(-3.0F, -8.0F, 5.5F, 0.2F, 1.25F, 0.0F));
        PartDefinition leg_right = waist.addOrReplaceChild("leg_right", CubeListBuilder.create().texOffs(0, 15).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F), PartPose.offsetAndRotation(-2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition shin_right = leg_right.addOrReplaceChild("shin_right", CubeListBuilder.create().texOffs(77, 15).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F).texOffs(17, 25).addBox(-1.5F, 5.0F, -3.5F, 3.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition arm_left = torso.addOrReplaceChild("arm_left", CubeListBuilder.create().texOffs(13, 15).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 6.0F, 3.0F), PartPose.offsetAndRotation(5.0F, -7.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition forearm_left = arm_left.addOrReplaceChild("forearm_left", CubeListBuilder.create().texOffs(116, 15).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F).texOffs(60, 25).addBox(-1.5F, 4.0F, -1.5F, 3.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition wing_left = torso.addOrReplaceChild("wing_left", CubeListBuilder.create().texOffs(31, 31).addBox(0.0F, -1.0F, -0.5F, 9.0F, 1.0F, 1.0F).texOffs(58, 15).addBox(0.0F, 0.0F, 0.0F, 9.0F, 8.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)), PartPose.offsetAndRotation(3.0F, -8.0F, 5.5F, 0.2F, -1.25F, 0.0F));
        PartDefinition leg_left = waist.addOrReplaceChild("leg_left", CubeListBuilder.create().texOffs(26, 15).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F), PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition shin_left = leg_left.addOrReplaceChild("shin_left", CubeListBuilder.create().texOffs(90, 15).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F).texOffs(32, 25).addBox(-1.5F, 5.0F, -3.5F, 3.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

}
