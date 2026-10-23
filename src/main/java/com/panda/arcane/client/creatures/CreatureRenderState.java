package com.panda.arcane.client.creatures;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Render state shared by the Creatures A mobs: the variant plus the few values their animations need. */
public class CreatureRenderState extends LivingEntityRenderState {
    public int variant;
    public String variantName = "";
    /** 0..1 progress of the variant's special move (tell, wind-up, swell). */
    public float action;
    /** 0..1 progress of the attack swing (1 at the start of the swing). */
    public float attack;
    public boolean flag;
}
