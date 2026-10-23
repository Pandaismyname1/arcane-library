package com.panda.arcane.client.creatures;

import com.panda.arcane.ArcaneMod;
import java.util.Map;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The creature model layers: one mesh per creature, in the creature texture's UV layout. */
@SuppressWarnings("unused")
public final class CreatureLayers {
    public static final ModelLayerLocation INKLING = new ModelLayerLocation(ArcaneMod.id("inkling"), "main");
    public static final ModelLayerLocation LUMENMOTH = new ModelLayerLocation(ArcaneMod.id("lumen_moth"), "main");
    public static final ModelLayerLocation SILENTLIBRARIAN = new ModelLayerLocation(ArcaneMod.id("silent_librarian"), "main");
    public static final ModelLayerLocation INKGOLEM = new ModelLayerLocation(ArcaneMod.id("ink_golem"), "main");
    public static final ModelLayerLocation VAULTWARD = new ModelLayerLocation(ArcaneMod.id("vault_ward"), "main");

    private CreatureLayers() {}

    public static void register(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(INKLING, CreatureLayers::inkling);
        event.registerLayerDefinition(LUMENMOTH, CreatureLayers::lumenMoth);
        event.registerLayerDefinition(SILENTLIBRARIAN, CreatureLayers::silentLibrarian);
        event.registerLayerDefinition(INKGOLEM, CreatureLayers::inkGolem);
        event.registerLayerDefinition(VAULTWARD, CreatureLayers::vaultWard);
    }

    public static LayerDefinition inkling() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(28, 14).addBox(-5.0F, -1.0F, -4.0F, 10.0F, 1.0F, 8.0F).texOffs(0, 0).addBox(-4.0F, -1.0F, -5.0F, 8.0F, 1.0F, 10.0F, new CubeDeformation(-0.15F)).texOffs(0, 11).addBox(-4.0F, -7.0F, -3.0F, 8.0F, 6.0F, 6.0F).texOffs(36, 0).addBox(-3.0F, -7.0F, -4.0F, 6.0F, 6.0F, 8.0F, new CubeDeformation(-0.15F)).texOffs(0, 23).addBox(-3.0F, -8.0F, -3.0F, 6.0F, 1.0F, 6.0F).texOffs(44, 23).addBox(-2.0F, -10.0F, -2.0F, 4.0F, 2.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_body.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(24, 29).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -10.0F, 0.5F, -0.5236F, 0.0F, 0.0F));
        p_body.addOrReplaceChild("eye_right", CubeListBuilder.create().texOffs(32, 29).addBox(-1.5F, -1.5F, -0.6F, 3.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(-1.7F, -5.0F, -4.0F, 0.0F, 0.0F, 0.0F));
        p_body.addOrReplaceChild("eye_left", CubeListBuilder.create().texOffs(40, 29).addBox(-1.5F, -1.5F, -0.6F, 3.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(1.7F, -5.0F, -4.0F, 0.0F, 0.0F, 0.0F));
        p_body.addOrReplaceChild("mouth", CubeListBuilder.create().texOffs(28, 11).addBox(-1.0F, -0.5F, -0.3F, 2.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -2.5F, -4.0F, 0.0F, 0.0F, 0.0F));
        p_body.addOrReplaceChild("arm_right", CubeListBuilder.create().texOffs(48, 29).addBox(-1.0F, -0.5F, -1.0F, 1.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(-4.0F, -4.0F, 0.0F, 0.0F, 0.0F, 0.4363F));
        p_body.addOrReplaceChild("arm_left", CubeListBuilder.create().texOffs(54, 29).addBox(0.0F, -0.5F, -1.0F, 1.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(4.0F, -4.0F, 0.0F, 0.0F, 0.0F, -0.4363F));
        p_body.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(24, 23).addBox(-2.5F, -1.0F, -2.5F, 5.0F, 1.0F, 5.0F, new CubeDeformation(0.05F)).texOffs(60, 23).addBox(-2.5F, -2.0F, -2.5F, 1.0F, 1.0F, 1.0F).texOffs(60, 25).addBox(1.5F, -2.0F, -2.5F, 1.0F, 1.0F, 1.0F).texOffs(60, 27).addBox(-2.5F, -2.0F, 1.5F, 1.0F, 1.0F, 1.0F).texOffs(60, 29).addBox(1.5F, -2.0F, 1.5F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -10.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
    /** Parts shown only for some variants of inkling. */
    public static final Map<String, String[]> INKLING_ONLY = Map.ofEntries(
            Map.entry("crown", new String[] {"gold"}));

    public static LayerDefinition lumenMoth() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(40, 0).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 5.0F).texOffs(40, 9).addBox(-2.5F, -2.5F, -3.5F, 5.0F, 5.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 17.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_head = p_body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(54, 9).addBox(-1.5F, -1.5F, -2.0F, 3.0F, 3.0F, 2.0F).texOffs(40, 22).addBox(-2.0F, -1.0F, -1.6F, 4.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -0.5F, -3.5F, 0.0F, 0.0F, 0.0F));
        p_head.addOrReplaceChild("antenna_right", CubeListBuilder.create().texOffs(58, 0).addBox(-1.5F, -5.0F, 0.0F, 3.0F, 5.0F, 0.0F), PartPose.offsetAndRotation(-0.6F, -1.4F, -1.2F, -0.6981F, 0.0F, -0.4189F));
        p_head.addOrReplaceChild("antenna_left", CubeListBuilder.create().texOffs(34, 18).addBox(-1.5F, -5.0F, 0.0F, 3.0F, 5.0F, 0.0F), PartPose.offsetAndRotation(0.6F, -1.4F, -1.2F, -0.6981F, 0.0F, 0.4189F));
        p_body.addOrReplaceChild("abdomen", CubeListBuilder.create().texOffs(40, 16).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 3.0F).texOffs(54, 14).addBox(-1.0F, -1.0F, 3.0F, 2.0F, 2.0F, 3.0F).texOffs(52, 19).addBox(-1.0F, -1.0F, 5.6F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.15F)), PartPose.offsetAndRotation(0.0F, 0.2F, 2.8F, -0.3142F, 0.0F, 0.0F));
        p_body.addOrReplaceChild("wing_right", CubeListBuilder.create().texOffs(0, 0).addBox(-11.0F, 0.0F, -3.0F, 11.0F, 0.0F, 9.0F).texOffs(0, 18).addBox(-9.0F, 0.25F, 0.0F, 9.0F, 0.0F, 8.0F), PartPose.offsetAndRotation(-1.8F, -1.6F, -0.5F, 0.0F, 0.0F, 0.4189F));
        p_body.addOrReplaceChild("wing_left", CubeListBuilder.create().texOffs(0, 9).addBox(0.0F, 0.0F, -3.0F, 11.0F, 0.0F, 9.0F).texOffs(0, 26).addBox(0.0F, 0.25F, 0.0F, 9.0F, 0.0F, 8.0F), PartPose.offsetAndRotation(1.8F, -1.6F, -0.5F, 0.0F, 0.0F, -0.4189F));
        p_body.addOrReplaceChild("leg_right_0", CubeListBuilder.create().texOffs(58, 5).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 2.0F, 0.0F), PartPose.offsetAndRotation(-1.5F, 1.8F, -1.5F, 0.0F, 0.0F, 0.8727F));
        p_body.addOrReplaceChild("leg_left_0", CubeListBuilder.create().texOffs(60, 5).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 2.0F, 0.0F), PartPose.offsetAndRotation(1.5F, 1.8F, -1.5F, 0.0F, 0.0F, -0.8727F));
        p_body.addOrReplaceChild("leg_right_1", CubeListBuilder.create().texOffs(62, 5).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 2.0F, 0.0F), PartPose.offsetAndRotation(-1.5F, 1.8F, 0.5F, 0.0F, 0.0F, 0.8727F));
        p_body.addOrReplaceChild("leg_left_1", CubeListBuilder.create().texOffs(58, 7).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 2.0F, 0.0F), PartPose.offsetAndRotation(1.5F, 1.8F, 0.5F, 0.0F, 0.0F, -0.8727F));
        p_body.addOrReplaceChild("leg_right_2", CubeListBuilder.create().texOffs(60, 7).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 2.0F, 0.0F), PartPose.offsetAndRotation(-1.5F, 1.8F, 2.5F, 0.0F, 0.0F, 0.8727F));
        p_body.addOrReplaceChild("leg_left_2", CubeListBuilder.create().texOffs(62, 7).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 2.0F, 0.0F), PartPose.offsetAndRotation(1.5F, 1.8F, 2.5F, 0.0F, 0.0F, -0.8727F));
        return LayerDefinition.create(mesh, 64, 64);
    }
    public static final Map<String, String[]> LUMENMOTH_ONLY = Map.of();

    public static LayerDefinition silentLibrarian() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(38, 15).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 5.0F, 6.0F).texOffs(94, 11).addBox(-4.5F, 4.5F, -3.5F, 9.0F, 4.0F, 7.0F).texOffs(0, 0).addBox(-5.5F, 8.0F, -4.5F, 11.0F, 3.0F, 9.0F), PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_torso = root.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(68, 0).addBox(-4.0F, -11.0F, -2.5F, 8.0F, 11.0F, 5.0F).texOffs(94, 0).addBox(-5.0F, -11.5F, -3.5F, 10.0F, 4.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.2443F, 0.0F, 0.0F));
        p_torso.addOrReplaceChild("stole", CubeListBuilder.create().texOffs(0, 12).addBox(-5.5F, -0.5F, -4.0F, 11.0F, 2.0F, 8.0F), PartPose.offsetAndRotation(0.0F, -11.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_torso.addOrReplaceChild("satchel", CubeListBuilder.create().texOffs(20, 34).addBox(-2.0F, -1.0F, -2.0F, 2.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(-4.5F, -4.0F, 0.0F, 0.0F, 0.0F, 0.1396F));
        p_torso.addOrReplaceChild("sash", CubeListBuilder.create().texOffs(94, 22).addBox(-4.5F, -1.0F, -3.0F, 9.0F, 2.0F, 6.0F), PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_head = p_torso.addOrReplaceChild("head", CubeListBuilder.create().texOffs(40, 0).addBox(-3.5F, -8.0F, -4.0F, 7.0F, 8.0F, 7.0F).texOffs(54, 26).addBox(-2.5F, -6.5F, -4.6F, 5.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -11.0F, -0.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_hood_tip = p_head.addOrReplaceChild("hood_tip", CubeListBuilder.create().texOffs(0, 31).addBox(-2.5F, -3.0F, -2.0F, 5.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, -8.0F, 1.0F, -0.5236F, 0.0F, 0.0F));
        p_hood_tip.addOrReplaceChild("hood_point", CubeListBuilder.create().texOffs(32, 34).addBox(-1.5F, -4.0F, -1.0F, 3.0F, 4.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -3.0F, 0.5F, -0.6109F, 0.0F, 0.0F));
        p_head.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(0, 22).addBox(-4.0F, -1.0F, -3.5F, 8.0F, 2.0F, 7.0F, new CubeDeformation(0.1F)).texOffs(30, 22).addBox(-3.5F, -3.0F, -4.0F, 1.0F, 2.0F, 1.0F).texOffs(34, 22).addBox(-0.5F, -3.0F, -4.0F, 1.0F, 2.0F, 1.0F).texOffs(124, 22).addBox(2.5F, -3.0F, -4.0F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -8.0F, -0.5F, 0.0F, 0.0F, 0.0F));
        p_head.addOrReplaceChild("blindfold", CubeListBuilder.create().texOffs(66, 16).addBox(-3.7F, -1.0F, -4.4F, 7.0F, 2.0F, 7.0F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0.0F, -4.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_arm_right = p_torso.addOrReplaceChild("arm_right", CubeListBuilder.create().texOffs(90, 30).addBox(-1.5F, -1.0F, -2.0F, 3.0F, 8.0F, 4.0F).texOffs(54, 33).addBox(-2.0F, 6.0F, -2.5F, 4.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(-4.6F, -9.6F, 0.2F, -0.8378F, 0.0F, 0.1047F));
        p_arm_right.addOrReplaceChild("hand_right", CubeListBuilder.create().texOffs(118, 30).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 9.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_arm_left = p_torso.addOrReplaceChild("arm_left", CubeListBuilder.create().texOffs(104, 30).addBox(-1.5F, -1.0F, -2.0F, 3.0F, 8.0F, 4.0F).texOffs(72, 33).addBox(-2.0F, 6.0F, -2.5F, 4.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(4.6F, -9.6F, 0.2F, -0.8378F, 0.0F, -0.1047F));
        p_arm_left.addOrReplaceChild("hand_left", CubeListBuilder.create().texOffs(44, 34).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 9.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_book = p_torso.addOrReplaceChild("book", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -2.8F, -8.8F, 0.6981F, 0.0F, 0.0F));
        p_book.addOrReplaceChild("page_right", CubeListBuilder.create().texOffs(66, 25).addBox(-5.0F, -1.0F, -3.5F, 5.0F, 1.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.2094F));
        p_book.addOrReplaceChild("page_left", CubeListBuilder.create().texOffs(30, 26).addBox(0.0F, -1.0F, -3.5F, 5.0F, 1.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.2094F));
        return LayerDefinition.create(mesh, 128, 64);
    }
    /** Parts shown only for some variants of silent_librarian. */
    public static final Map<String, String[]> SILENTLIBRARIAN_ONLY = Map.ofEntries(
            Map.entry("blindfold", new String[] {"censor"}),
            Map.entry("crown", new String[] {"head"}),
            Map.entry("sash", new String[] {"censor"}),
            Map.entry("satchel", new String[] {"clerk"}),
            Map.entry("stole", new String[] {"head"}));

    public static LayerDefinition inkGolem() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(104, 44).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 9.0F, 6.0F).texOffs(0, 59).addBox(-3.5F, 8.0F, -4.0F, 7.0F, 4.0F, 7.0F), PartPose.offsetAndRotation(-4.5F, 12.0F, 1.5F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(98, 59).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 9.0F, 6.0F).texOffs(62, 72).addBox(-3.5F, 8.0F, -4.0F, 7.0F, 4.0F, 7.0F), PartPose.offsetAndRotation(4.5F, 12.0F, 1.5F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("waist", CubeListBuilder.create().texOffs(32, 58).addBox(-5.5F, -5.0F, -3.5F, 11.0F, 6.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 12.0F, 1.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_chest = root.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -16.0F, -5.5F, 16.0F, 16.0F, 11.0F).texOffs(0, 71).addBox(-6.0F, -18.0F, -3.0F, 12.0F, 2.0F, 8.0F), PartPose.offsetAndRotation(0.0F, 8.0F, 1.5F, 0.384F, 0.0F, 0.0F));
        p_chest.addOrReplaceChild("bands", CubeListBuilder.create().texOffs(54, 0).addBox(-8.0F, -12.0F, -5.5F, 16.0F, 2.0F, 11.0F, new CubeDeformation(0.4F)).texOffs(54, 13).addBox(-8.0F, -5.0F, -5.5F, 16.0F, 2.0F, 11.0F, new CubeDeformation(0.4F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_chest.addOrReplaceChild("head", CubeListBuilder.create().texOffs(68, 58).addBox(-4.0F, -4.0F, -6.0F, 8.0F, 7.0F, 7.0F).texOffs(90, 74).addBox(-4.5F, -5.0F, -4.0F, 9.0F, 2.0F, 6.0F), PartPose.offsetAndRotation(0.0F, -12.5F, -5.5F, -0.3491F, 0.0F, 0.0F));
        PartDefinition p_inkwell = p_chest.addOrReplaceChild("inkwell", CubeListBuilder.create().texOffs(62, 83).addBox(-2.5F, -5.0F, -2.5F, 5.0F, 5.0F, 5.0F).texOffs(26, 87).addBox(-2.0F, -6.0F, -2.0F, 4.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -16.0F, 3.5F, -0.4887F, 0.0F, 0.0F));
        p_inkwell.addOrReplaceChild("quill", CubeListBuilder.create().texOffs(108, 0).addBox(0.0F, -17.0F, -3.0F, 0.0F, 17.0F, 7.0F), PartPose.offsetAndRotation(0.6F, -5.5F, 0.0F, -0.4538F, 0.0F, 0.3491F));
        PartDefinition p_right_arm = p_chest.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 27).addBox(-6.0F, -3.5F, -4.5F, 7.0F, 7.0F, 9.0F).texOffs(32, 27).addBox(-4.5F, 3.0F, -3.0F, 5.0F, 10.0F, 6.0F), PartPose.offsetAndRotation(-9.5F, -12.5F, 0.0F, -0.384F, 0.0F, 0.192F));
        p_right_arm.addOrReplaceChild("right_arm_bands", CubeListBuilder.create().texOffs(0, 81).addBox(-5.0F, 6.0F, -3.5F, 6.0F, 2.0F, 7.0F, new CubeDeformation(0.3F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_right_fist = p_right_arm.addOrReplaceChild("right_fist", CubeListBuilder.create().texOffs(54, 26).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 10.0F, 8.0F).texOffs(32, 44).addBox(-4.5F, 6.0F, -4.5F, 9.0F, 5.0F, 9.0F), PartPose.offsetAndRotation(-2.0F, 13.0F, 0.0F, 0.0698F, 0.0F, 0.0F));
        p_right_fist.addOrReplaceChild("right_drip", CubeListBuilder.create().texOffs(26, 81).addBox(-3.0F, 0.0F, -2.5F, 6.0F, 4.0F, 0.0F).texOffs(118, 24).addBox(-2.0F, 0.0F, 2.5F, 4.0F, 3.0F, 0.0F), PartPose.offsetAndRotation(0.0F, 11.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_left_arm = p_chest.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 43).addBox(-1.0F, -3.5F, -4.5F, 7.0F, 7.0F, 9.0F).texOffs(40, 71).addBox(-0.5F, 3.0F, -3.0F, 5.0F, 10.0F, 6.0F), PartPose.offsetAndRotation(9.5F, -12.5F, 0.0F, -0.384F, 0.0F, -0.192F));
        p_left_arm.addOrReplaceChild("left_arm_bands", CubeListBuilder.create().texOffs(90, 82).addBox(-1.0F, 6.0F, -3.5F, 6.0F, 2.0F, 7.0F, new CubeDeformation(0.3F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_left_fist = p_left_arm.addOrReplaceChild("left_fist", CubeListBuilder.create().texOffs(86, 26).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 10.0F, 8.0F).texOffs(68, 44).addBox(-4.5F, 6.0F, -4.5F, 9.0F, 5.0F, 9.0F), PartPose.offsetAndRotation(2.0F, 13.0F, 0.0F, 0.0698F, 0.0F, 0.0F));
        p_left_fist.addOrReplaceChild("left_drip", CubeListBuilder.create().texOffs(116, 82).addBox(-3.0F, 0.0F, -2.5F, 6.0F, 4.0F, 0.0F).texOffs(118, 27).addBox(-2.0F, 0.0F, 2.5F, 4.0F, 3.0F, 0.0F), PartPose.offsetAndRotation(0.0F, 11.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }
    /** Parts shown only for some variants of ink_golem. */
    public static final Map<String, String[]> INKGOLEM_ONLY = Map.ofEntries(
            Map.entry("bands", new String[] {"ironbound"}),
            Map.entry("left_arm_bands", new String[] {"ironbound"}),
            Map.entry("right_arm_bands", new String[] {"ironbound"}));

    public static LayerDefinition vaultWard() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_helm = root.addOrReplaceChild("helm", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -6.0F, -4.0F, 8.0F, 8.0F, 8.0F).texOffs(0, 16).addBox(-4.5F, 1.0F, -4.5F, 9.0F, 2.0F, 9.0F), PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_helm.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(32, 0).addBox(-1.0F, -3.0F, -4.5F, 2.0F, 3.0F, 9.0F), PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_helm.addOrReplaceChild("key_crown", CubeListBuilder.create().texOffs(48, 29).addBox(-3.0F, -3.0F, -3.0F, 2.0F, 3.0F, 2.0F).texOffs(56, 29).addBox(1.0F, -3.0F, -3.0F, 2.0F, 3.0F, 2.0F).texOffs(48, 34).addBox(-3.0F, -3.0F, 1.0F, 2.0F, 3.0F, 2.0F).texOffs(56, 34).addBox(1.0F, -3.0F, 1.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(36, 12).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 12.5F, 0.0F, 0.7854F, 0.0F, 0.7854F));
        PartDefinition p_orbit = root.addOrReplaceChild("orbit", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 12.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_orbit.addOrReplaceChild("plate_0", CubeListBuilder.create().texOffs(54, 0).addBox(-2.0F, -3.0F, -0.5F, 4.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(-0.0F, 0.0F, -6.5F, 0.0F, 0.0F, 0.0F));
        p_orbit.addOrReplaceChild("shield_0", CubeListBuilder.create().texOffs(36, 20).addBox(-3.0F, -4.0F, -0.5F, 6.0F, 8.0F, 1.0F), PartPose.offsetAndRotation(-0.0F, 0.0F, -7.15F, 0.0F, 0.0F, 0.0F));
        p_orbit.addOrReplaceChild("plate_1", CubeListBuilder.create().texOffs(54, 7).addBox(-2.0F, -3.0F, -0.5F, 4.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(-6.5F, 0.0F, -0.0F, 0.0F, 1.5708F, 0.0F));
        p_orbit.addOrReplaceChild("shield_1", CubeListBuilder.create().texOffs(50, 20).addBox(-3.0F, -4.0F, -0.5F, 6.0F, 8.0F, 1.0F), PartPose.offsetAndRotation(-7.15F, 0.0F, -0.0F, 0.0F, 1.5708F, 0.0F));
        p_orbit.addOrReplaceChild("plate_2", CubeListBuilder.create().texOffs(28, 29).addBox(-2.0F, -3.0F, -0.5F, 4.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(-0.0F, 0.0F, 6.5F, 0.0F, 3.1416F, 0.0F));
        p_orbit.addOrReplaceChild("shield_2", CubeListBuilder.create().texOffs(0, 27).addBox(-3.0F, -4.0F, -0.5F, 6.0F, 8.0F, 1.0F), PartPose.offsetAndRotation(-0.0F, 0.0F, 7.15F, 0.0F, 3.1416F, 0.0F));
        p_orbit.addOrReplaceChild("plate_3", CubeListBuilder.create().texOffs(38, 29).addBox(-2.0F, -3.0F, -0.5F, 4.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(6.5F, 0.0F, 0.0F, 0.0F, 4.7124F, 0.0F));
        p_orbit.addOrReplaceChild("shield_3", CubeListBuilder.create().texOffs(14, 27).addBox(-3.0F, -4.0F, -0.5F, 6.0F, 8.0F, 1.0F), PartPose.offsetAndRotation(7.15F, 0.0F, 0.0F, 0.0F, 4.7124F, 0.0F));
        PartDefinition p_chain = root.addOrReplaceChild("chain", CubeListBuilder.create().texOffs(32, 12).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 15.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_chain.addOrReplaceChild("spike", CubeListBuilder.create().texOffs(52, 14).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 2.0F, 3.0F).texOffs(8, 36).addBox(-0.5F, 2.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 3.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_chain.addOrReplaceChild("key", CubeListBuilder.create().texOffs(0, 36).addBox(-1.5F, 0.0F, -0.5F, 3.0F, 3.0F, 1.0F).texOffs(12, 36).addBox(-0.5F, 3.0F, -0.5F, 1.0F, 3.0F, 1.0F).texOffs(28, 27).addBox(0.5F, 4.5F, -0.5F, 2.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 3.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
    /** Parts shown only for some variants of vault_ward. */
    public static final Map<String, String[]> VAULTWARD_ONLY = Map.ofEntries(
            Map.entry("crest", new String[] {"sentinel", "seeker", "bulwark"}),
            Map.entry("key", new String[] {"keys"}),
            Map.entry("key_crown", new String[] {"keys"}),
            Map.entry("plate_0", new String[] {"sentinel", "seeker", "keys"}),
            Map.entry("plate_1", new String[] {"sentinel", "seeker", "keys"}),
            Map.entry("plate_2", new String[] {"sentinel", "seeker", "keys"}),
            Map.entry("plate_3", new String[] {"sentinel", "seeker", "keys"}),
            Map.entry("shield_0", new String[] {"bulwark"}),
            Map.entry("shield_1", new String[] {"bulwark"}),
            Map.entry("shield_2", new String[] {"bulwark"}),
            Map.entry("shield_3", new String[] {"bulwark"}),
            Map.entry("spike", new String[] {"sentinel", "seeker", "bulwark"}));
}
