package com.panda.arcane.client.boss;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

/** What the Archivist model needs from the entity each frame. */
public class ArchivistRenderState extends LivingEntityRenderState {
    public final AnimationState idle = new AnimationState();
    public final AnimationState storm = new AnimationState();
    public final AnimationState erasure = new AnimationState();
    public final AnimationState recall = new AnimationState();
    public final AnimationState slam = new AnimationState();
    public final AnimationState index = new AnimationState();
    public final AnimationState transition = new AnimationState();
    public final AnimationState stagger = new AnimationState();
    public int phase = 1;
    public boolean warded;
    public boolean hovering;
    public boolean casting;
}
