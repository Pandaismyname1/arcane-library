package com.panda.arcane.entity;

import com.panda.arcane.registry.ModEntities;
import com.panda.arcane.registry.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A bolt of condensed lumen. Deals magic damage and can carry a status effect. Never sets anything on fire.
 * Bolts fade after {@link #lifetime(int)} ticks so a missed volley does not fly across the Library.
 */
public class ArcaneBolt extends Fireball {
    private float damage = 4.0F;
    private @Nullable Holder<MobEffect> effect;
    private int effectTicks;
    private int lifetime = 100;

    public ArcaneBolt(EntityType<? extends ArcaneBolt> type, Level level) {
        super(type, level);
        setItem(sprite());
    }

    public ArcaneBolt(Level level, LivingEntity owner, Vec3 direction) {
        super(ModEntities.ARCANE_BOLT.get(), owner, direction, level);
        setItem(sprite());
    }

    /** What the bolt renders as: a glowing glyph sprite (assets/arcane/items/arcane_bolt.json), not the dust item. */
    private static ItemStack sprite() {
        ItemStack stack = new ItemStack(ModItems.LUMEN_DUST.get());
        stack.set(net.minecraft.core.component.DataComponents.ITEM_MODEL, com.panda.arcane.ArcaneMod.id("arcane_bolt"));
        return stack;
    }

    public ArcaneBolt damage(float damage) {
        this.damage = damage;
        return this;
    }

    public ArcaneBolt effect(Holder<MobEffect> effect, int ticks) {
        this.effect = effect;
        this.effectTicks = ticks;
        return this;
    }

    /**
     * Sets a steady speed in blocks per tick: the bolt starts at that speed and keeps it, instead of a fireball's slow
     * start and fast finish. Slow bolts are easy to read and to step around.
     */
    public ArcaneBolt speed(double blocksPerTick, Vec3 direction) {
        // a hurting projectile settles at accel * inertia / (1 - inertia) = accel * 19
        this.accelerationPower = blocksPerTick / 19.0;
        setDeltaMovement(direction.normalize().scale(blocksPerTick));
        return this;
    }

    public ArcaneBolt lifetime(int ticks) {
        this.lifetime = ticks;
        return this;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount > lifetime) erase();
    }

    /** Removes the bolt with a puff of ink, as when the Archivist's Erasure crosses it. */
    public void erase() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SQUID_INK, getX(), getY(), getZ(), 6, 0.1, 0.1, 0.1, 0.02);
        }
        discard();
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return !(entity instanceof LibraryDweller) && super.canHitEntity(entity);
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected @Nullable ParticleOptions getTrailParticle() {
        return ParticleTypes.END_ROD; // a bright streak: the bolt's path reads even in the dark stacks
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (level() instanceof ServerLevel server) {
            Entity target = hit.getEntity();
            Entity owner = getOwner();
            if (target.hurtServer(server, damageSources().indirectMagic(this, owner), damage) && effect != null && target instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(effect, effectTicks), owner);
            }
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 10, 0.15, 0.15, 0.15, 0.2);
            discard();
        }
    }
}
