package com.panda.arcane.entity;

import com.panda.arcane.worldgen.LibraryLayout;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;

/**
 * A spider built of quills around an inkwell. It climbs the shelves like a spider.
 *
 * <ul>
 *   <li><b>Needle</b>: keeps its distance, rears up (the tell) and looses a fan of three quills.</li>
 *   <li><b>Inkfang</b>: leaps and bites; the ink poisons and splashes into your eyes (Poison, brief Blindness).</li>
 *   <li><b>Scribe</b> (Restricted Section): scribbles a rune under its prey. The rune glows for a second before it
 *       binds (heavy Slowness while you stand in it): walk off it.</li>
 * </ul>
 */
public class QuillStalker extends VariantMonster {
    public static final int NEEDLE = 0, INKFANG = 1, SCRIBE = 2;
    private static final String[] NAMES = {"needle", "inkfang", "scribe"};
    public static final int ACTION_NONE = 0, ACTION_AIM = 1, ACTION_WRITE = 2;
    public static final int AIM_TICKS = 14, WRITE_TICKS = 20;

    private static final EntityDataAccessor<Byte> DATA_CLIMBING = SynchedEntityData.defineId(QuillStalker.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_ACTION = SynchedEntityData.defineId(QuillStalker.class, EntityDataSerializers.INT);

    private int actionTicks;
    private int shootCooldown = 30;
    private int runeCooldown = 40;
    private Vec3 runeAt = Vec3.ZERO;

    public float rearO, rear;

    public QuillStalker(EntityType<? extends QuillStalker> type, Level level) {
        super(type, level);
        this.xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.SCALE, 1.3) // polish: read at a distance (renders and collides 30% larger)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    public String[] variantNames() {
        return NAMES;
    }

    @Override
    protected int chooseVariant(MobsB.Zone zone, RandomSource random) {
        if (zone.restricted()) return MobsB.weighted(random, 25, 35, 40);
        if (zone.is(LibraryLayout.Kind.SCRIPTORIUM)) return MobsB.weighted(random, 45, 45, 10);
        return MobsB.weighted(random, 60, 40, 0);
    }

    @Override
    protected void applyVariantStats(int v) {
        base(Attributes.MAX_HEALTH, switch (v) { case INKFANG -> 18.0; case SCRIBE -> 22.0; default -> 14.0; });
        base(Attributes.ATTACK_DAMAGE, switch (v) { case INKFANG -> 3.0; case SCRIBE -> 3.0; default -> 2.0; });
        base(Attributes.MOVEMENT_SPEED, v == INKFANG ? 0.33 : 0.29);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CLIMBING, (byte) 0);
        builder.define(DATA_ACTION, ACTION_NONE);
    }

    public int action() {
        return entityData.get(DATA_ACTION);
    }

    private void setAction(int a) {
        entityData.set(DATA_ACTION, a);
        actionTicks = 0;
    }

    public boolean isClimbing() {
        return entityData.get(DATA_CLIMBING) != 0;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setAction(ACTION_NONE);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WallClimberNavigation(this, level);
    }

    @Override
    public boolean onClimbable() {
        return isClimbing();
    }

    @Override
    public void makeStuckInBlock(BlockState state, Vec3 speedMultiplier) {
        if (!state.is(Blocks.COBWEB)) super.makeStuckInBlock(state, speedMultiplier);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new VolleyGoal());
        goalSelector.addGoal(2, new RuneGoal());
        goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F) {
            @Override
            public boolean canUse() {
                return variant() == INKFANG && super.canUse();
            }
        });
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, true) {
            @Override
            public boolean canUse() {
                return action() == ACTION_NONE && (variant() != NEEDLE || closeTarget()) && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return action() == ACTION_NONE && (variant() != NEEDLE || closeTarget()) && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    private boolean closeTarget() {
        LivingEntity t = getTarget();
        return t != null && distanceToSqr(t) < 6.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            entityData.set(DATA_CLIMBING, (byte) (horizontalCollision ? 1 : 0));
        } else {
            rearO = rear;
            float goal = action() == ACTION_AIM ? 1.0F : action() == ACTION_WRITE ? -0.5F : 0.0F;
            rear += (goal - rear) * 0.3F;
            if (variant() == INKFANG && random.nextInt(14) == 0) {
                level().addParticle(ParticleTypes.SQUID_INK, getX(), getY() + 0.4, getZ(), 0, -0.02, 0);
            }
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        actionTicks++;
        if (shootCooldown > 0) shootCooldown--;
        if (runeCooldown > 0) runeCooldown--;
        LivingEntity target = getTarget();
        if (action() == ACTION_AIM) {
            getNavigation().stop();
            if (target != null) getLookControl().setLookAt(target, 60, 60);
            if (actionTicks >= AIM_TICKS) {
                if (target != null && target.isAlive()) volley(level, target);
                setAction(ACTION_NONE);
                shootCooldown = 50 + random.nextInt(20);
            }
        } else if (action() == ACTION_WRITE) {
            getNavigation().stop();
            if (target != null) getLookControl().setLookAt(target, 60, 60);
            // a scribbled line of ink runs from its nibs to the rune
            Vec3 from = position().add(0, 0.3, 0);
            Vec3 p = from.lerp(runeAt.add(0, 0.1, 0), actionTicks / (double) WRITE_TICKS);
            level.sendParticles(ParticleTypes.ENCHANT, p.x, p.y, p.z, 3, 0.05, 0.05, 0.05, 0.0);
            if (actionTicks % 5 == 0) MobsB.play(this, MobsB.STALKER_SCRIBBLE.get(), 0.7F, 1.0F + random.nextFloat() * 0.3F);
            if (actionTicks >= WRITE_TICKS) {
                placeRune(level, runeAt);
                setAction(ACTION_NONE);
                runeCooldown = 120 + random.nextInt(40);
            }
        }
    }

    /** Needle: test hook and goal entry. */
    public void startAim() {
        setAction(ACTION_AIM);
        MobsB.layered(this, MobsB.STALKER_AMBIENT.get(), 1.0F, 1.6F, MobsB.STALKER_SCRIBBLE.get(), 0.6F, 0.7F);
    }

    private int quillsLoosed;

    /** Quills shot so far (for tests: a quill that hits is gone from the world). */
    public int quillsLoosed() {
        return quillsLoosed;
    }

    private void volley(ServerLevel level, LivingEntity target) {
        Vec3 eye = new Vec3(getX(), getY() + 0.7, getZ());
        for (int i = -1; i <= 1; i++) {
            Arrow quill = new Arrow(level, this, new ItemStack(Items.ARROW), null);
            quill.pickup = AbstractArrow.Pickup.DISALLOWED;
            quill.setPos(eye.x, eye.y, eye.z);
            quill.setBaseDamage(1.5);
            double dx = target.getX() - eye.x, dz = target.getZ() - eye.z;
            double dy = target.getY(0.4) - eye.y;
            double dist = Math.sqrt(dx * dx + dz * dz);
            Vec3 side = new Vec3(-dz, 0, dx).normalize().scale(i * 0.12 * dist);
            quill.shoot(dx + side.x, dy + dist * 0.18, dz + side.z, 1.5F, 4.0F);
            if (level.addFreshEntity(quill)) quillsLoosed++;
        }
        MobsB.layered(this, MobsB.STALKER_SHOOT.get(), 1.0F, 1.0F, MobsB.STALKER_SCRIBBLE.get(), 0.5F, 1.4F);
    }

    /** Scribe: test hook and goal entry. Scribble a rune where the target stands. */
    public void startRune(Vec3 at) {
        runeAt = at;
        setAction(ACTION_WRITE);
    }

    private void placeRune(ServerLevel level, Vec3 at) {
        AreaEffectCloud rune = new AreaEffectCloud(level, at.x, at.y, at.z);
        rune.setRadius(1.4F);
        rune.setDuration(160);
        rune.setWaitTime(20);
        rune.setRadiusPerTick(0.0F);
        rune.setRadiusOnUse(0.0F);
        rune.setDurationOnUse(0);
        rune.setCustomParticle(ParticleTypes.ENCHANT);
        rune.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 4));
        rune.setOwner(this);
        level.addFreshEntity(rune);
        level.sendParticles(ParticleTypes.GLOW, at.x, at.y + 0.1, at.z, 16, 0.7, 0.02, 0.7, 0.0);
        MobsB.play(this, MobsB.STALKER_SCRIBBLE.get(), 1.0F, 0.6F);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            MobsB.play(this, MobsB.STALKER_BITE.get(), 0.8F, 1.0F);
            if (variant() == INKFANG && target instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0), this);
                living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0), this);
                level.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getEyeY(), target.getZ(), 12, 0.25, 0.25, 0.25, 0.05);
            }
        }
        return hit;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && action() == ACTION_AIM && damage >= 3.0F) { // knocked out of its aim
            setAction(ACTION_NONE);
            shootCooldown = 40;
        }
        return hurt;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.is(MobEffects.POISON) && super.canBeAffected(effect);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return MobsB.STALKER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return MobsB.STALKER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return MobsB.STALKER_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(net.minecraft.sounds.SoundEvents.SPIDER_STEP, 0.15F, 1.4F);
    }

    // ---- goals ---------------------------------------------------------------------------------------------------

    /** Needle: hold 6-12 blocks off, then rear up and fire a fan of quills. */
    private final class VolleyGoal extends Goal {
        private int repath;

        VolleyGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return variant() == NEEDLE && getTarget() != null && getTarget().isAlive() && !closeTarget();
        }

        @Override
        public boolean canContinueToUse() {
            return action() == ACTION_AIM || canUse();
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null || action() != ACTION_NONE) return;
            getLookControl().setLookAt(t, 30, 30);
            double d = distanceToSqr(t);
            if (--repath <= 0) {
                repath = 10;
                if (d > 144 || !hasLineOfSight(t)) {
                    getNavigation().moveTo(t, 1.0);
                } else if (d < 36) {
                    Vec3 away = DefaultRandomPos.getPosAway(QuillStalker.this, 6, 3, t.position());
                    if (away != null) getNavigation().moveTo(away.x, away.y, away.z, 1.2);
                } else {
                    getNavigation().stop();
                }
            }
            if (shootCooldown <= 0 && d < 196 && hasLineOfSight(t)) startAim();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    /** Scribe: every few seconds, scribble a rune under the target. */
    private final class RuneGoal extends Goal {
        RuneGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (action() == ACTION_WRITE) return true;
            LivingEntity t = getTarget();
            return variant() == SCRIBE && t != null && runeCooldown <= 0 && t.onGround() && distanceToSqr(t) < 144 && hasLineOfSight(t);
        }

        @Override
        public boolean canContinueToUse() {
            return action() == ACTION_WRITE;
        }

        @Override
        public void start() {
            LivingEntity t = getTarget();
            if (action() == ACTION_NONE && t != null) {
                Vec3 lead = t.getDeltaMovement().multiply(4, 0, 4);
                startRune(t.position().add(lead));
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    public float rearAmount(float partial) {
        return Mth.lerp(partial, rearO, rear);
    }
}
