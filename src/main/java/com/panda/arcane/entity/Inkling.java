package com.panda.arcane.entity;

import com.panda.arcane.registry.ModEntities;
import com.panda.arcane.registry.ModItems;
import java.util.EnumSet;
import java.util.Locale;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A droplet of living ink with big eyes, spilled from the scriptorium presses (Creatures A). Variants:
 * <ul>
 *   <li>Black: splits into two droplets when it pops.</li>
 *   <li>Blue: its touch is cold and heavy (Slowness II).</li>
 *   <li>Red (more common deep in): creeps close, swells and flashes for a second and a half, then bursts in a spray
 *       of ink that blinds and hurts. Back off and it calms.</li>
 *   <li>Gold: rare and shy. It runs from players and leaves Lumen Dust when caught.</li>
 * </ul>
 */
public class Inkling extends Monster implements LibraryDweller, CreatureVisuals {
    public enum Variant {
        BLACK(45, 30), BLUE(35, 25), RED(12, 35), GOLD(8, 10);

        final int normalWeight, restrictedWeight;

        Variant(int normalWeight, int restrictedWeight) {
            this.normalWeight = normalWeight;
            this.restrictedWeight = restrictedWeight;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** Red: how long the swell takes before the burst, in ticks (the model inflates and flashes over it). */
    public static final int SWELL_TICKS = 30;
    public static final double BURST_RADIUS = 3.0;
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(Inkling.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SWELL = SynchedEntityData.defineId(Inkling.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SMALL = SynchedEntityData.defineId(Inkling.class, EntityDataSerializers.BOOLEAN);

    private int oldSwell;
    private int attackAnim;

    public Inkling(EntityType<? extends Inkling> type, Level level) {
        super(type, level);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    // ---- variant ------------------------------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, 0);
        builder.define(DATA_SWELL, 0);
        builder.define(DATA_SMALL, false);
    }

    public Variant variant() {
        return Variant.values()[Math.floorMod(entityData.get(DATA_VARIANT), Variant.values().length)];
    }

    public Inkling setVariant(Variant variant) {
        entityData.set(DATA_VARIANT, variant.ordinal());
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(variant == Variant.GOLD ? 0.34 : variant == Variant.RED ? 0.27 : 0.3);
        return this;
    }

    public boolean isSmall() {
        return entityData.get(DATA_SMALL);
    }

    /** A droplet split from a Black Inkling: smaller, weaker, and it does not split again. */
    public Inkling makeSmall() {
        entityData.set(DATA_SMALL, true);
        getAttribute(Attributes.SCALE).setBaseValue(0.6);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(4.0);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(1.0);
        setHealth(4.0F);
        return this;
    }

    @Override
    public int variantIndex() {
        return variant().ordinal();
    }

    @Override
    public float actionProgress(float partialTick) {
        float s = oldSwell + (entityData.get(DATA_SWELL) - oldSwell) * partialTick;
        return Math.max(0.0F, Math.min(1.0F, s / SWELL_TICKS));
    }

    @Override
    public float attackProgress(float partialTick) {
        return attackAnim <= 0 ? 0.0F : Math.max(0.0F, attackAnim - partialTick) / 8.0F;
    }

    @Override
    public boolean visualFlag() {
        return isSmall();
    }

    public static Variant pickVariant(CreatureZones zone, RandomSource random) {
        return CreatureZones.pick(Variant.values(), v -> zone.restricted() ? v.restrictedWeight : v.normalWeight, random);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
                                                  @Nullable SpawnGroupData data) {
        setVariant(pickVariant(CreatureZones.at(level, blockPosition()), random));
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    protected Component getTypeName() {
        return Component.translatable(getType().getDescriptionId() + "." + variant().id());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("Variant", variant().id());
        output.putBoolean("Small", isSmall());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(DATA_VARIANT, CreatureZones.byName(Variant.values(), input.getStringOr("Variant", "black")).ordinal());
        entityData.set(DATA_SMALL, input.getBooleanOr("Small", false));
    }

    // ---- behaviour ----------------------------------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SwellGoal());
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 8.0F, 1.1, 1.45) {
            @Override
            public boolean canUse() {
                return variant() == Variant.GOLD && super.canUse();
            }
        });
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, false) {
            @Override
            public boolean canUse() {
                return variant() != Variant.GOLD && super.canUse();
            }
        });
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, (target, level) -> variant() != Variant.GOLD));
    }

    @Override
    public void tick() {
        oldSwell = entityData.get(DATA_SWELL);
        super.tick();
        if (level().isClientSide()) {
            if (variant() == Variant.GOLD && random.nextInt(5) == 0) {
                level().addParticle(ParticleTypes.WAX_ON, getRandomX(0.5), getY() + random.nextDouble() * 0.6, getRandomZ(0.5), 0, 0.03, 0);
            }
            if (onGround() && getDeltaMovement().horizontalDistanceSqr() > 0.002 && random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.SQUID_INK, getRandomX(0.4), getY() + 0.05, getRandomZ(0.4), 0, 0.0, 0);
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (attackAnim > 0) attackAnim--;
    }

    public int swell() {
        return entityData.get(DATA_SWELL);
    }

    /** Red: one tick of swelling (positive) or calming (negative). Bursts when full. Public as a test hook. */
    public void swellBy(int amount) {
        int s = Math.max(0, Math.min(SWELL_TICKS, swell() + amount));
        if (s > 0 && swell() == 0) playSound(SoundEvents.CREEPER_PRIMED, 0.8F, 1.6F);
        entityData.set(DATA_SWELL, s);
        if (s >= SWELL_TICKS && level() instanceof ServerLevel server) burst(server);
    }

    /** The burst: blinding, stinging ink for everyone close, and the inkling is gone. Returns how many it hit. */
    public int burst(ServerLevel level) {
        int hit = 0;
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(BURST_RADIUS))) {
            if (victim == this || victim instanceof LibraryDweller || victim.distanceToSqr(this) > BURST_RADIUS * BURST_RADIUS) continue;
            if (victim.hurtServer(level, damageSources().mobAttack(this), 5.0F)) hit++;
            victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0), this);
            victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0), this);
        }
        level.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 0.3, getZ(), 70, 1.0, 0.5, 1.0, 0.15);
        level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.3, getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, blockPosition(), SoundEvents.SLIME_DEATH, getSoundSource(), 1.2F, 0.6F);
        level.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), getSoundSource(), 0.4F, 1.6F);
        discard();
        return hit;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        attackAnim = 8;
        level.broadcastEntityEvent(this, (byte) 4);
        boolean hit = super.doHurtTarget(level, target);
        if (hit && variant() == Variant.BLUE && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), this);
        }
        return hit;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) attackAnim = 8;
        else super.handleEntityEvent(id);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (variant() == Variant.BLACK && !isSmall() && level() instanceof ServerLevel level) split(level);
    }

    /** Black: pops into two small droplets. Returns how many it made. */
    public int split(ServerLevel level) {
        int made = 0;
        for (int i = 0; i < 2; i++) {
            Inkling drop = ModEntities.INKLING.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            if (drop == null) continue;
            drop.setVariant(Variant.BLACK).makeSmall();
            double a = random.nextDouble() * Math.PI * 2;
            drop.snapTo(getX() + Math.cos(a) * 0.3, getY() + 0.2, getZ() + Math.sin(a) * 0.3, random.nextFloat() * 360F, 0F);
            drop.setDeltaMovement(Math.cos(a) * 0.25, 0.3, Math.sin(a) * 0.25);
            if (getTarget() != null) drop.setTarget(getTarget());
            level.addFreshEntity(drop);
            made++;
        }
        level.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 0.3, getZ(), 16, 0.3, 0.2, 0.3, 0.05);
        return made;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        if (variant() == Variant.GOLD) spawnAtLocation(level, new ItemStack(ModItems.LUMEN_DUST.get(), 6 + random.nextInt(7)));
    }

    @Override
    public boolean canUsePortal(boolean ignorePassenger) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return random.nextInt(3) == 0 ? SoundEvents.SLIME_SQUISH_SMALL : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_HURT_SMALL;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH_SMALL;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (isSmall() ? 1.5F : 1.15F);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof LibraryDweller) && super.canAttack(target);
    }

    /** Red: swell while a target is within reach and visible, calm down otherwise. */
    private final class SwellGoal extends Goal {
        SwellGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return variant() == Variant.RED && (swell() > 0 || target != null && distanceToSqr(target) < 2.5 * 2.5);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            boolean close = target != null && target.isAlive() && distanceToSqr(target) < 5.0 * 5.0 && getSensing().hasLineOfSight(target);
            swellBy(close ? 1 : -1);
        }
    }
}
