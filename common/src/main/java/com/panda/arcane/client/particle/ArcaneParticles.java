package com.panda.arcane.client.particle;

import com.panda.arcane.registry.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.FlyTowardsPositionParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import com.panda.arcane.client.ClientSinks;

/** Client providers for {@link ModParticles}. Registered from each loader's client entry. */
public final class ArcaneParticles {
    private ArcaneParticles() {}

    public static void register(ClientSinks.Particles event) {
        event.sprites(ModParticles.LUMEN_MOTE.get(), sprites -> (options, level, x, y, z, dx, dy, dz, random) ->
                new LumenMote(level, x, y, z, dx, dy, dz, sprites));
        event.sprites(ModParticles.INK_DROP.get(), sprites -> (options, level, x, y, z, dx, dy, dz, random) ->
                new InkDrop(level, x, y, z, dx, dy, dz, sprites));
        event.sprites(ModParticles.PORTAL_GLYPH.get(), sprites -> (options, level, x, y, z, dx, dy, dz, random) -> {
            FlyTowardsPositionParticle glyph = new FlyTowardsPositionParticle(level, x, y, z, dx, dy, dz, true,
                    new Particle.LifetimeAlpha(0.0F, 1.0F, 0.0F, 0.25F), sprites.get(random));
            float shade = 0.8F + random.nextFloat() * 0.2F;
            glyph.setColor(0.55F * shade, 0.95F * shade, 1.0F * shade);
            glyph.scale(1.6F);
            return glyph;
        });
    }

    /** Lumen Dust: a glowing mote that twinkles down its sprite set, wobbles upward and fades out. */
    static final class LumenMote extends SingleQuadParticle {
        private final SpriteSet sprites;
        private final float phase;

        LumenMote(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, sprites.first());
            this.sprites = sprites;
            this.xd = dx;
            this.yd = dy;
            this.zd = dz;
            this.friction = 0.94F;
            this.gravity = -0.02F;
            this.hasPhysics = false;
            this.lifetime = 28 + this.random.nextInt(28);
            this.quadSize = 0.07F + this.random.nextFloat() * 0.05F;
            this.phase = this.random.nextFloat() * Mth.TWO_PI;
            float warm = this.random.nextFloat() * 0.15F;
            this.setColor(1.0F, 0.9F + warm * 0.5F, 0.75F + warm);
            this.setSpriteFromAge(sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (this.removed) return;
            this.setSpriteFromAge(this.sprites);
            this.xd += Mth.sin(this.age * 0.35F + this.phase) * 0.0025;
            this.zd += Mth.cos(this.age * 0.31F + this.phase) * 0.0025;
            float t = (float) this.age / this.lifetime;
            this.setAlpha(t < 0.6F ? 1.0F : Math.max(0.0F, (1.0F - t) / 0.4F));
        }

        @Override
        protected Layer getLayer() {
            return Layer.TRANSLUCENT;
        }

        @Override
        public int getLightCoords(float partialTick) {
            return LightCoordsUtil.FULL_BRIGHT;
        }
    }

    /** Ink: a falling drop that turns into a flat splat when it lands, then fades. */
    static final class InkDrop extends SingleQuadParticle {
        private final SpriteSet sprites;
        private int landedAt = -1;

        InkDrop(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, sprites.first());
            this.sprites = sprites;
            this.xd = dx;
            this.yd = dy;
            this.zd = dz;
            this.gravity = 0.9F;
            this.friction = 0.98F;
            this.lifetime = 60;
            this.quadSize = 0.06F + this.random.nextFloat() * 0.03F;
            this.setSize(0.02F, 0.02F);
        }

        @Override
        public void tick() {
            super.tick();
            if (this.removed) return;
            if (this.onGround && this.landedAt < 0) {
                this.landedAt = this.age;
                this.setSprite(this.sprites.get(1, 1));
                this.quadSize *= 1.6F;
                this.xd = 0;
                this.zd = 0;
                this.gravity = 0;
                this.lifetime = Math.min(this.lifetime, this.age + 24);
            }
            if (this.landedAt >= 0) {
                this.setAlpha(Math.max(0.0F, 1.0F - (float) (this.age - this.landedAt) / 24.0F));
            }
        }

        @Override
        protected Layer getLayer() {
            return Layer.TRANSLUCENT;
        }
    }
}
