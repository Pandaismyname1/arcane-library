package com.panda.arcane.registry;

import com.panda.arcane.ArcaneMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import com.panda.arcane.platform.registry.DeferredHolder;
import com.panda.arcane.platform.registry.DeferredRegister;

/**
 * The mod's own particles. Their providers are in client/particle/ArcaneParticles.
 */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, ArcaneMod.MODID);

    /** A warm, glowing speck of Lumen Dust that drifts up and fades. Velocity arguments are its start velocity. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LUMEN_MOTE =
            PARTICLES.register("lumen_mote", () -> new SimpleParticleType(false));
    /** A drop of ink: falls, then splats flat where it lands. Velocity arguments are its start velocity. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> INK_DROP =
            PARTICLES.register("ink_drop", () -> new SimpleParticleType(false));
    /**
     * A glowing cyan rune that flies from (x + dx, y + dy, z + dz) to (x, y, z), like the enchanting table's glyphs.
     * Spawn it at the destination and pass the start offset as the velocity arguments.
     */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PORTAL_GLYPH =
            PARTICLES.register("portal_glyph", () -> new SimpleParticleType(false));

    private ModParticles() {}

}
