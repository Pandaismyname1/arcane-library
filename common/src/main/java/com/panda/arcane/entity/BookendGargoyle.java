package com.panda.arcane.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A crouching stone gargoyle carved onto a bookend. It perches as a statue until it notices prey, and goes back to
 * being a statue when it loses it.
 *
 * <ul>
 *   <li><b>Granite</b>: moves only while nobody looks at it. Watched, it freezes mid-step and its stone skin halves the
 *       damage it takes. Keep your eyes on it and back away.</li>
 *   <li><b>Marble</b>: crouches, grinds (the tell), and pounces across the room. It is stunned for a moment after it
 *       lands.</li>
 *   <li><b>Obsidian</b> (Restricted Section, vaults): slow and heavy. Arrows and other projectiles glance off it; its
 *       blows send you flying. Wakes only when you come very close or strike it.</li>
 * </ul>
 */
public class BookendGargoyle extends VariantMonster {
    public static final int GRANITE = 0, MARBLE = 1, OBSIDIAN = 2;
    private static final String[] NAMES = {"granite", "marble", "obsidian"};
    public static final int POUNCE_NONE = 0, POUNCE_TELL = 1, POUNCE_AIR = 2, POUNCE_RECOVER = 3;
    public static final int TELL_TICKS = 16, RECOVER_TICKS = 24;

    private static final EntityDataAccessor<Boolean> DATA_STATUE = SynchedEntityData.defineId(BookendGargoyle.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_FROZEN = SynchedEntityData.defineId(BookendGargoyle.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_POUNCE = SynchedEntityData.defineId(BookendGargoyle.class, EntityDataSerializers.INT);

    private int lostTicks;
    private int pounceTicks;
    private int pounceCooldown = 40;
    private float frozenYaw, frozenHeadYaw, frozenBodyYaw;
    private boolean captured;

    // client-side: animation that stops dead while the statue is frozen
    public float animAge, animAgeO;
    public float limbPos, limbSpeed;
    public float statueO, statue = 1.0F;

    public BookendGargoyle(EntityType<? extends BookendGargoyle> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.ATTACK_KNOCKBACK, 0.3)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    public String[] variantNames() {
        return NAMES;
    }

    @Override
    protected int chooseVariant(MobsB.Zone zone, RandomSource random) {
        if (zone.restricted()) return MobsB.weighted(random, 30, 30, 40);
        if (zone.is(com.panda.arcane.worldgen.LibraryLayout.Kind.VAULT)) return MobsB.weighted(random, 40, 40, 20);
        return MobsB.weighted(random, 60, 40, 0);
    }

    @Override
    protected void applyVariantStats(int v) {
        base(Attributes.MAX_HEALTH, switch (v) { case MARBLE -> 28.0; case OBSIDIAN -> 48.0; default -> 30.0; });
        base(Attributes.MOVEMENT_SPEED, switch (v) { case GRANITE -> 0.36; case MARBLE -> 0.3; default -> 0.25; });
        base(Attributes.ATTACK_DAMAGE, switch (v) { case OBSIDIAN -> 9.0; case MARBLE -> 6.0; default -> 6.0; });
        base(Attributes.ARMOR, v == OBSIDIAN ? 12.0 : 6.0);
        base(Attributes.KNOCKBACK_RESISTANCE, v == OBSIDIAN ? 1.0 : 0.6);
        base(Attributes.ATTACK_KNOCKBACK, v == OBSIDIAN ? 1.6 : 0.3);
    }

    // ---- state ---------------------------------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATUE, true);
        builder.define(DATA_FROZEN, false);
        builder.define(DATA_POUNCE, POUNCE_NONE);
    }

    public boolean isStatue() {
        return entityData.get(DATA_STATUE);
    }

    public boolean isFrozen() {
        return entityData.get(DATA_FROZEN);
    }

    public int pounce() {
        return entityData.get(DATA_POUNCE);
    }

    /** Statue or frozen: no movement, no attacks, no turning. */
    public boolean stone() {
        return isStatue() || isFrozen();
    }

    private void setPounce(int p) {
        entityData.set(DATA_POUNCE, p);
        pounceTicks = 0;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Statue", isStatue());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(DATA_STATUE, input.getBooleanOr("Statue", true));
        entityData.set(DATA_FROZEN, false);
        setPounce(POUNCE_NONE);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PounceGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true) {
            @Override
            public boolean canUse() {
                return !stone() && pounce() == POUNCE_NONE && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !stone() && pounce() == POUNCE_NONE && super.canContinueToUse();
            }
        });
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                (t, l) -> !isStatue() || distanceToSqr(t) < wakeRange() * wakeRange()));
    }

    private double wakeRange() {
        return switch (variant()) { case OBSIDIAN -> 4.5; case MARBLE -> 10.0; default -> 16.0; };
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || stone() || pounce() == POUNCE_TELL || pounce() == POUNCE_RECOVER;
    }

    // ---- ticking -------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            animAgeO = animAge;
            statueO = statue;
            statue += ((isStatue() ? 1.0F : 0.0F) - statue) * 0.2F;
            if (!isFrozen()) {
                animAge += 1.0F;
                limbPos = walkAnimation.position();
                limbSpeed = walkAnimation.speed();
            }
            return;
        }
        if (level() instanceof ServerLevel server && isAlive() && !isNoAi()) serverStateTick(server);
        if (!stone()) {
            captured = false;
        } else if (!captured) {
            captureRotation();
        } else {
            setYRot(frozenYaw);
            yHeadRot = frozenHeadYaw;
            yBodyRot = frozenBodyYaw;
        }
    }

    /**
     * State upkeep runs from tick(), not customServerAiStep: a statue, frozen or crouching gargoyle is immobile, and
     * vanilla skips the AI step of immobile mobs, which would leave it stuck in that state.
     */
    private void serverStateTick(ServerLevel level) {
        if (pounceCooldown > 0) pounceCooldown--;
        // A statue is immobile, and vanilla skips the AI step (target goals included) of immobile mobs: the perched
        // statue would never notice anyone. So it watches for prey here, twice a second.
        if (isStatue() && tickCount % 10 == 0) {
            Player prey = level.getNearestPlayer(net.minecraft.world.entity.ai.targeting.TargetingConditions.forCombat().range(wakeRange()), this);
            if (prey != null) wake(prey);
        }
        LivingEntity target = getTarget();
        boolean wasStone = stone();
        // perched statue: wakes when it has prey, turns back to stone when it loses it
        if (target != null && target.isAlive()) {
            lostTicks = 0;
            if (isStatue()) setStatue(false);
        } else if (!isStatue() && ++lostTicks > 100) {
            setStatue(true);
        }
        if (variant() == GRANITE) {
            boolean watched = !isStatue() && isWatched(level);
            if (watched != isFrozen()) {
                entityData.set(DATA_FROZEN, watched);
                if (watched) MobsB.layered(this, MobsB.GARGOYLE_FREEZE.get(), 1.0F, 1.0F, MobsB.GARGOYLE_GRIND.get(), 0.5F, 1.4F);
                else MobsB.layered(this, MobsB.GARGOYLE_UNFREEZE.get(), 1.0F, 1.0F, MobsB.GARGOYLE_GRIND.get(), 0.5F, 0.8F);
            }
        }
        if (stone()) {
            if (!wasStone) captureRotation();
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
        }
        tickPounce(level, target);
    }

    private void captureRotation() {
        captured = true;
        frozenYaw = getYRot();
        frozenHeadYaw = yHeadRot;
        frozenBodyYaw = yBodyRot;
    }

    public void setStatue(boolean statue) {
        if (statue) captureRotation();
        entityData.set(DATA_STATUE, statue);
        if (!statue) {
            MobsB.layered(this, MobsB.GARGOYLE_UNFREEZE.get(), 1.0F, 0.8F, MobsB.GARGOYLE_GRIND.get(), 0.8F, 0.7F);
            if (level() instanceof ServerLevel server) {
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                        getX(), getY() + 0.8, getZ(), 14, 0.3, 0.5, 0.3, 0.05);
            }
        } else {
            MobsB.play(this, MobsB.GARGOYLE_FREEZE.get(), 0.8F, 0.7F);
        }
    }

    /** Granite: is any player within 32 blocks looking at it (60 degree cone, through glass)? */
    public boolean isWatched(ServerLevel level) {
        for (Player p : level.players()) {
            if (p.isSpectator() || !p.isAlive() || p.distanceToSqr(this) > 32 * 32) continue;
            if (isLookingAtMe(p, 0.5, false, true, getEyeY(), getY() + 0.5 * getScale(), (getEyeY() + getY()) / 2.0)) return true;
        }
        return false;
    }

    /** Test hook: wake the statue now. */
    public void wake(LivingEntity target) {
        setTarget(target);
        setStatue(false);
    }

    // ---- marble pounce -------------------------------------------------------------------------------------------

    /** Test hook and goal entry: crouch and grind before a pounce. */
    public void startPounce() {
        setPounce(POUNCE_TELL);
        pounceCooldown = 80;
        MobsB.layered(this, MobsB.GARGOYLE_GRIND.get(), 1.0F, 0.6F, MobsB.GARGOYLE_UNFREEZE.get(), 0.6F, 1.2F);
    }

    private void tickPounce(ServerLevel level, LivingEntity target) {
        int p = pounce();
        if (p == POUNCE_NONE) return;
        pounceTicks++;
        if (p == POUNCE_TELL) {
            getNavigation().stop();
            if (target != null) getLookControl().setLookAt(target, 60, 60);
            if (pounceTicks % 4 == 0) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CALCITE.defaultBlockState()),
                    getX(), getY() + 0.1, getZ(), 6, 0.4, 0.05, 0.4, 0.05);
            if (pounceTicks >= TELL_TICKS) {
                if (target == null) {
                    setPounce(POUNCE_NONE);
                    return;
                }
                Vec3 d = target.position().subtract(position());
                double horiz = Math.sqrt(d.x * d.x + d.z * d.z);
                double speed = Math.min(1.4, horiz / 11.0 + 0.2);
                Vec3 v = new Vec3(d.x, 0, d.z).normalize().scale(speed);
                setDeltaMovement(v.x, 0.62 + Math.max(0, d.y) * 0.08, v.z);
                float yaw = -((float) Mth.atan2(d.x, d.z)) * Mth.RAD_TO_DEG;
                setYRot(yaw);
                yBodyRot = yaw;
                yHeadRot = yaw;
                needsSync = true;
                setPounce(POUNCE_AIR);
                MobsB.layered(this, MobsB.GARGOYLE_POUNCE.get(), 1.0F, 1.0F, MobsB.GARGOYLE_GRIND.get(), 0.6F, 1.5F);
            }
        } else if (p == POUNCE_AIR) {
            if (target != null && getBoundingBox().inflate(0.4).intersects(target.getBoundingBox())) doHurtTarget(level, target);
            if (pounceTicks > 3 && onGround() || pounceTicks > 40) {
                land(level, target);
            }
        } else if (p == POUNCE_RECOVER) {
            if (pounceTicks % 6 == 0) level.sendParticles(ParticleTypes.CRIT, getX(), getEyeY() + 0.3, getZ(), 3, 0.3, 0.1, 0.3, 0.0);
            if (pounceTicks >= RECOVER_TICKS) setPounce(POUNCE_NONE);
        }
    }

    private void land(ServerLevel level, LivingEntity target) {
        setPounce(POUNCE_RECOVER);
        BlockState below = level.getBlockState(blockPosition().below());
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below.isAir() ? Blocks.STONE.defaultBlockState() : below),
                getX(), getY() + 0.1, getZ(), 30, 0.8, 0.1, 0.8, 0.15);
        MobsB.layered(this, MobsB.GARGOYLE_LAND.get(), 1.2F, 1.0F, MobsB.GARGOYLE_HURT.get(), 0.6F, 0.6F);
        if (target != null && distanceToSqr(target) < 4.0) doHurtTarget(level, target);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (variant() == OBSIDIAN && source.is(DamageTypeTags.IS_PROJECTILE)) {
            MobsB.layered(this, MobsB.GARGOYLE_DEFLECT.get(), 1.0F, 1.0F, MobsB.GARGOYLE_HURT.get(), 0.4F, 1.6F);
            level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 1.0, getZ(), 8, 0.3, 0.4, 0.3, 0.3);
            if (source.getEntity() instanceof LivingEntity attacker && isStatue()) wake(attacker);
            return false;
        }
        if (isFrozen() || isStatue()) damage *= 0.5F;
        if (pounce() == POUNCE_RECOVER) damage *= 1.5F;
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && isStatue() && source.getEntity() instanceof LivingEntity attacker) wake(attacker);
        return hurt;
    }

    @Override
    public void knockback(double power, double xd, double zd, DamageSource source, float damage, boolean comesFromEffect) {
        if (!stone()) super.knockback(power, xd, zd, source, damage, comesFromEffect);
    }

    @Override
    public boolean isPushable() {
        return !stone() && super.isPushable();
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            MobsB.play(this, MobsB.GARGOYLE_LAND.get(), 0.6F, 1.4F);
            if (target instanceof net.minecraft.server.level.ServerPlayer sp) {
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
            }
        }
        return hit;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return stone() ? null : MobsB.GARGOYLE_GRIND.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return MobsB.GARGOYLE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return MobsB.GARGOYLE_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(net.minecraft.sounds.SoundEvents.DEEPSLATE_STEP, 0.6F, variant() == OBSIDIAN ? 0.6F : 0.9F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (variant() == OBSIDIAN ? 0.7F : variant() == MARBLE ? 1.15F : 1.0F);
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    // ---- goals ---------------------------------------------------------------------------------------------------

    private final class PounceGoal extends Goal {
        PounceGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (pounce() != POUNCE_NONE) return true;
            LivingEntity t = getTarget();
            if (variant() != MARBLE || stone() || t == null || pounceCooldown > 0 || !onGround()) return false;
            double d = distanceToSqr(t);
            return d > 9 && d < 110 && hasLineOfSight(t) && random.nextInt(5) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return pounce() != POUNCE_NONE;
        }

        @Override
        public void start() {
            if (pounce() == POUNCE_NONE) startPounce();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    public float statueAmount(float partial) {
        return Mth.lerp(partial, statueO, statue);
    }
}
