package com.panda.arcane.entity;

import com.panda.arcane.registry.ModEntities;
import com.panda.arcane.registry.ModSounds;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A hulking construct of slate and ink with an inkwell on its back (Creatures A). Variants:
 * <ul>
 *   <li>Ink: leaves pools of slowing ink where it walks; its blows slow.</li>
 *   <li>Iron-bound (more common deep in): banded in iron, cannot be knocked back. It rears up with both fists over its
 *       head for most of a second, then slams the floor: heavy damage and a launch for anyone within reach. Step out.</li>
 *   <li>Blotted: its blows blind, and when it falls it bursts into a brood of Inklings.</li>
 * </ul>
 */
public class InkGolem extends Monster implements LibraryDweller, CreatureVisuals {
    public enum Variant {
        INK(70, 30), IRONBOUND(10, 35), BLOTTED(20, 35);

        final int normalWeight, restrictedWeight;

        Variant(int normalWeight, int restrictedWeight) {
            this.normalWeight = normalWeight;
            this.restrictedWeight = restrictedWeight;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** The slam's wind-up: fists overhead for this many ticks before they come down (the model tracks it exactly). */
    public static final int SLAM_TELL = 18;
    public static final double SLAM_RADIUS = 3.5;
    public static final int BROOD = 3;
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(InkGolem.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SLAM = SynchedEntityData.defineId(InkGolem.class, EntityDataSerializers.INT);

    private int attackAnimationTick;
    private int slamCooldown = 60;
    private int puddleCooldown;

    public InkGolem(EntityType<? extends InkGolem> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 70.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    // ---- variant ------------------------------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, 0);
        builder.define(DATA_SLAM, 0);
    }

    public Variant variant() {
        return Variant.values()[Math.floorMod(entityData.get(DATA_VARIANT), Variant.values().length)];
    }

    public void setVariant(Variant variant) {
        entityData.set(DATA_VARIANT, variant.ordinal());
        boolean iron = variant == Variant.IRONBOUND;
        getAttribute(Attributes.ARMOR).setBaseValue(iron ? 12.0 : 4.0);
        getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(iron ? 1.0 : 0.8);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(iron ? 0.2 : 0.22);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(iron ? 90.0 : variant == Variant.BLOTTED ? 60.0 : 70.0);
        if (getHealth() > getMaxHealth() || tickCount == 0) setHealth(getMaxHealth());
    }

    @Override
    public int variantIndex() {
        return variant().ordinal();
    }

    @Override
    public float actionProgress(float partialTick) {
        int slam = entityData.get(DATA_SLAM);
        return slam <= 0 ? 0.0F : Math.min(1.0F, 1.0F - (slam - partialTick) / SLAM_TELL);
    }

    @Override
    public float attackProgress(float partialTick) {
        return attackAnimationTick <= 0 ? 0.0F : Math.max(0.0F, attackAnimationTick - partialTick) / 10.0F;
    }

    public static Variant pickVariant(CreatureZones zone, RandomSource random) {
        return CreatureZones.pick(Variant.values(), v -> zone.restricted() ? v.restrictedWeight : v.normalWeight, random);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
                                                  @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        setVariant(pickVariant(CreatureZones.at(level, blockPosition()), random));
        setHealth(getMaxHealth());
        return result;
    }

    @Override
    protected Component getTypeName() {
        return Component.translatable(getType().getDescriptionId() + "." + variant().id());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("Variant", variant().id());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(DATA_VARIANT, CreatureZones.byName(Variant.values(), input.getStringOr("Variant", "ink")).ordinal());
        entityData.set(DATA_SLAM, 0);
    }

    // ---- behaviour ----------------------------------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true) {
            @Override
            public boolean canUse() {
                return entityData.get(DATA_SLAM) <= 0 && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return entityData.get(DATA_SLAM) <= 0 && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public int getAttackAnimationTick() {
        return attackAnimationTick;
    }

    public boolean isSlamming() {
        return entityData.get(DATA_SLAM) > 0;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (attackAnimationTick > 0) attackAnimationTick--;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        LivingEntity target = getTarget();
        if (slamCooldown > 0) slamCooldown--;
        int slam = entityData.get(DATA_SLAM);
        if (slam > 0) {
            getNavigation().stop();
            if (target != null) getLookControl().setLookAt(target, 30.0F, 30.0F);
            entityData.set(DATA_SLAM, --slam);
            if (slam == 0) slam(level);
            return;
        }
        if (variant() == Variant.IRONBOUND && target != null && slamCooldown <= 0 && onGround() && distanceToSqr(target) < 4.5 * 4.5) {
            startSlam();
        }
        if (variant() == Variant.INK && target != null && --puddleCooldown <= 0 && getDeltaMovement().horizontalDistanceSqr() > 1.0E-4) {
            puddleCooldown = 30;
            dropPuddle(level);
        }
    }

    /** Ink: a pool of slowing ink at its feet. Public as a test hook. */
    public AreaEffectCloud dropPuddle(ServerLevel level) {
        AreaEffectCloud cloud = new AreaEffectCloud(level, getX(), getY(), getZ());
        cloud.setOwner(this);
        cloud.setRadius(1.6F);
        cloud.setDuration(140);
        cloud.setWaitTime(5);
        cloud.setRadiusPerTick(-0.004F);
        cloud.setCustomParticle(ParticleTypes.SQUID_INK);
        cloud.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 1));
        level.addFreshEntity(cloud);
        return cloud;
    }

    /** Iron-bound: starts the slam's wind-up. Public as a test hook. */
    public void startSlam() {
        entityData.set(DATA_SLAM, SLAM_TELL);
        slamCooldown = 110;
        playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.2F, 0.5F);
    }

    /** Holds the wind-up at a given number of ticks left (screenshots and tests; the AI counts it down normally). */
    public void poseSlam(int ticksLeft) {
        entityData.set(DATA_SLAM, Math.max(0, Math.min(SLAM_TELL, ticksLeft)));
    }

    /** The slam lands: everything near takes a heavy blow and is thrown up and away. Returns how many it hit. */
    public int slam(ServerLevel level) {
        int hit = 0;
        attackAnimationTick = 10;
        level.broadcastEntityEvent(this, (byte) 4);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(SLAM_RADIUS, 1.0, SLAM_RADIUS))) {
            if (victim == this || victim instanceof LibraryDweller || victim.distanceToSqr(this) > (SLAM_RADIUS + 1) * (SLAM_RADIUS + 1)) continue;
            if (victim instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
            if (victim.hurtServer(level, damageSources().mobAttack(this), 8.0F)) hit++;
            Vec3 away = victim.position().subtract(position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : away.normalize();
            victim.setDeltaMovement(away.x * 0.9, 0.7, away.z * 0.9);
            victim.needsSync = true;
            if (victim instanceof ServerPlayer sp) sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
        }
        BlockState floor = level.getBlockState(blockPosition().below());
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor), getX(), getY() + 0.1, getZ(), 60, 1.6, 0.1, 1.6, 0.2);
        level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.3, getZ(), 2, 0.8, 0.1, 0.8, 0.0);
        level.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, getSoundSource(), 1.2F, 0.5F);
        level.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), getSoundSource(), 0.6F, 0.8F);
        return hit;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        attackAnimationTick = 10;
        level.broadcastEntityEvent(this, (byte) 4);
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1), this);
            if (variant() == Variant.BLOTTED) living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0), this);
        }
        return hit;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) attackAnimationTick = 10;
        else super.handleEntityEvent(id);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (variant() == Variant.BLOTTED && level() instanceof ServerLevel level) spawnBrood(level, source);
    }

    /** Blotted: bursts into Inklings that take up its fight. Returns how many it made. */
    public int spawnBrood(ServerLevel level, @Nullable DamageSource source) {
        int made = 0;
        LivingEntity grudge = source != null && source.getEntity() instanceof LivingEntity l ? l : getTarget();
        for (int i = 0; i < BROOD; i++) {
            Inkling inkling = ModEntities.INKLING.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            if (inkling == null) continue;
            double a = i * Math.PI * 2 / BROOD + random.nextDouble();
            inkling.snapTo(getX() + Math.cos(a) * 0.8, getY() + 0.5, getZ() + Math.sin(a) * 0.8, random.nextFloat() * 360F, 0F);
            inkling.setVariant(i == 0 ? Inkling.Variant.BLUE : Inkling.Variant.BLACK);
            inkling.setDeltaMovement(Math.cos(a) * 0.3, 0.4, Math.sin(a) * 0.3);
            if (grudge != null && !(grudge instanceof LibraryDweller)) inkling.setTarget(grudge);
            level.addFreshEntity(inkling);
            made++;
        }
        level.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1.2, getZ(), 50, 0.8, 0.8, 0.8, 0.08);
        level.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, getSoundSource(), 1.5F, 0.5F);
        return made;
    }

    @Override
    public boolean canUsePortal(boolean ignorePassenger) {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(ModSounds.GOLEM_STEP.get(), 0.8F, variant() == Variant.IRONBOUND ? 0.55F : 0.7F);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return variant() == Variant.BLOTTED ? SoundEvents.SLIME_HURT : SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof LibraryDweller) && super.canAttack(target);
    }
}
