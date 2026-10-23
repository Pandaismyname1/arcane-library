package com.panda.arcane.entity;

import com.panda.arcane.worldgen.LibraryLayout;
import java.util.EnumSet;
import java.util.List;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A hooded drift of loose pages that hunts in small flocks. It circles its prey, rears back with a rustle (the tell),
 * then dives in a straight line. A dive that misses leaves it dazed for a moment: that is the opening.
 *
 * <ul>
 *   <li><b>Torn</b>: the quickest dive, the shortest tell, the longest daze.</li>
 *   <li><b>Gilded</b>: hangs back and wraps nearby Library creatures in gold leaf (Absorption). Kill it first.</li>
 *   <li><b>Cursed</b> (Restricted Section): its touch saps strength (Weakness) and it bursts into a cursed cloud on death.</li>
 *   <li><b>Blank</b> (Restricted Section): copies the attack damage of whatever its target holds when it starts a dive.
 *       Switch to an empty hand and it copies nothing.</li>
 * </ul>
 */
public class PageWraith extends VariantMonster {
    public static final int TORN = 0, GILDED = 1, CURSED = 2, BLANK = 3;
    private static final String[] NAMES = {"torn", "gilded", "cursed", "blank"};

    public enum Phase { NONE, TELL, DIVE, DAZED }

    private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(PageWraith.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_COPIED = SynchedEntityData.defineId(PageWraith.class, EntityDataSerializers.BOOLEAN);

    private int phaseTicks;
    private int cooldown = 40;
    private int wardCooldown = 60;
    private Vec3 diveDir = Vec3.ZERO;

    // client-side animation
    public float diveAmountO, diveAmount;
    public float wardFlash;

    public PageWraith(EntityType<? extends PageWraith> type, Level level) {
        super(type, level);
        this.moveControl = new WraithMoveControl(this);
        setNoGravity(true);
        this.xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    public String[] variantNames() {
        return NAMES;
    }

    @Override
    protected int chooseVariant(MobsB.Zone zone, RandomSource random) {
        if (zone.restricted()) return MobsB.weighted(random, 25, 15, 32, 28);
        if (zone.is(LibraryLayout.Kind.READING_ROOM) || zone.is(LibraryLayout.Kind.STUDY)) return MobsB.weighted(random, 55, 45, 0, 0);
        return MobsB.weighted(random, 75, 25, 0, 0);
    }

    @Override
    protected void applyVariantStats(int v) {
        base(Attributes.MAX_HEALTH, switch (v) { case GILDED -> 14.0; case CURSED -> 16.0; case BLANK -> 18.0; default -> 10.0; });
        base(Attributes.ATTACK_DAMAGE, switch (v) { case CURSED -> 4.0; case BLANK -> 2.0; default -> 3.0; });
        base(Attributes.FLYING_SPEED, v == TORN ? 0.75 : 0.6);
        base(Attributes.ARMOR, v == GILDED ? 4.0 : 0.0);
    }

    // ---- tuning per variant --------------------------------------------------------------------------------------

    public int tellTicks() {
        return variant() == TORN ? 14 : 20;
    }

    public double diveSpeed() {
        return variant() == TORN ? 1.05 : 0.75;
    }

    public int dazeTicks() {
        return variant() == TORN ? 36 : 24;
    }

    // ---- state ---------------------------------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASE, 0);
        builder.define(DATA_COPIED, false);
    }

    public Phase phase() {
        return Phase.values()[Mth.clamp(entityData.get(DATA_PHASE), 0, Phase.values().length - 1)];
    }

    private void setPhase(Phase phase) {
        entityData.set(DATA_PHASE, phase.ordinal());
        phaseTicks = 0;
    }

    public boolean copied() {
        return entityData.get(DATA_COPIED);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Copied", copied());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(DATA_COPIED, input.getBooleanOr("Copied", false));
        setPhase(Phase.NONE); // a reload mid-dive ends the dive
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new DiveGoal());
        goalSelector.addGoal(2, new WardAlliesGoal());
        goalSelector.addGoal(3, new CircleTargetGoal());
        goalSelector.addGoal(8, new DriftGoal());
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 10.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(PageWraith.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        noPhysics = false;
        super.tick();
        setNoGravity(true);
        if (level().isClientSide()) {
            diveAmountO = diveAmount;
            float goal = phase() == Phase.DIVE ? 1.0F : phase() == Phase.TELL ? -0.6F : 0.0F;
            diveAmount += (goal - diveAmount) * 0.3F;
            if (wardFlash > 0) wardFlash -= 0.05F;
            if (random.nextInt(phase() == Phase.DIVE ? 1 : 8) == 0) {
                level().addParticle(variant() == CURSED ? ParticleTypes.SMOKE : ParticleTypes.WHITE_ASH,
                        getRandomX(0.6), getY() + random.nextDouble() * 1.2, getRandomZ(0.6), 0, -0.02, 0);
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 61) wardFlash = 1.0F;
        else super.handleEntityEvent(id);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        phaseTicks++;
        if (cooldown > 0) cooldown--;
        if (wardCooldown > 0) wardCooldown--;
        LivingEntity target = getTarget();
        switch (phase()) {
            case TELL -> {
                setDeltaMovement(getDeltaMovement().scale(0.6).add(0, 0.02, 0));
                if (target != null) getLookControl().setLookAt(target, 60, 60);
                if (phaseTicks % 4 == 0) level.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + 0.9, getZ(), 6, 0.4, 0.4, 0.4, 0.4);
                if (phaseTicks >= tellTicks()) {
                    if (target == null || !target.isAlive()) {
                        setPhase(Phase.NONE);
                    } else {
                        startDive(target);
                    }
                }
            }
            case DIVE -> {
                if (target != null && phaseTicks < 5) { // a little homing at the start, then committed
                    Vec3 want = target.getBoundingBox().getCenter().subtract(position().add(0, 0.7, 0)).normalize();
                    diveDir = diveDir.add(want.subtract(diveDir).scale(0.25)).normalize();
                }
                setDeltaMovement(diveDir.scale(diveSpeed()));
                setYRot(-((float) Mth.atan2(diveDir.x, diveDir.z)) * Mth.RAD_TO_DEG);
                yBodyRot = getYRot();
                AABB reach = getBoundingBox().inflate(0.35);
                if (target != null && reach.intersects(target.getBoundingBox())) {
                    doHurtTarget(level, target);
                    setPhase(Phase.NONE);
                    cooldown = variant() == TORN ? 45 : 70;
                    setDeltaMovement(diveDir.scale(-0.4).add(0, 0.3, 0));
                } else if ((phaseTicks > 2 && (horizontalCollision || verticalCollision)) || phaseTicks > 26) {
                    setPhase(Phase.DAZED);
                    level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 0.8, getZ(), 8, 0.3, 0.3, 0.3, 0.1);
                    MobsB.play(this, MobsB.WRAITH_HURT.get(), 0.8F, 1.6F);
                }
            }
            case DAZED -> {
                setDeltaMovement(getDeltaMovement().scale(0.5).add(0, -0.015, 0));
                if (phaseTicks % 5 == 0) level.sendParticles(ParticleTypes.WHITE_ASH, getX(), getY() + 1.2, getZ(), 4, 0.3, 0.1, 0.3, 0.0);
                if (phaseTicks >= dazeTicks()) {
                    setPhase(Phase.NONE);
                    cooldown = variant() == TORN ? 30 : 50;
                }
            }
            default -> { }
        }
    }

    /** Test hook and goal entry: rear back before a dive. */
    public void startTell(LivingEntity target) {
        setPhase(Phase.TELL);
        setTarget(target);
        MobsB.layered(this, MobsB.WRAITH_TELL.get(), 1.0F, 1.0F, MobsB.WRAITH_FLUTTER.get(), 0.8F, 0.7F);
    }

    private void startDive(LivingEntity target) {
        if (variant() == BLANK) copyWeapon(target);
        diveDir = target.getBoundingBox().getCenter().subtract(position().add(0, 0.7, 0)).normalize();
        setPhase(Phase.DIVE);
        MobsB.layered(this, MobsB.WRAITH_DIVE.get(), 1.2F, 1.0F, MobsB.WRAITH_FLUTTER.get(), 0.7F, 1.4F);
    }

    /** Blank: take the target's current attack damage as its own (at least 2). Public for the screenshot test. */
    public double copyWeapon(LivingEntity target) {
        double dmg = Math.max(2.0, target.getAttributeValue(Attributes.ATTACK_DAMAGE));
        base(Attributes.ATTACK_DAMAGE, dmg);
        boolean wasCopied = copied();
        entityData.set(DATA_COPIED, dmg > 2.0);
        if (dmg > 2.0 && !wasCopied && level() instanceof ServerLevel server) {
            MobsB.play(this, MobsB.WRAITH_COPY.get(), 1.0F, 1.2F);
            server.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getEyeY(), target.getZ(), 20, 0.3, 0.3, 0.3, 1.0);
        }
        return dmg;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && variant() == CURSED && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 140, 0), this);
            level.sendParticles(ParticleTypes.WITCH, target.getX(), target.getEyeY(), target.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
        }
        return hit;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (phase() == Phase.DAZED) damage *= 1.5F; // the punish window
        return super.hurtServer(level, source, damage);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (variant() == CURSED && level() instanceof ServerLevel) {
            AreaEffectCloud cloud = new AreaEffectCloud(level(), getX(), getY() + 0.2, getZ());
            cloud.setRadius(2.2F);
            cloud.setDuration(100);
            cloud.setRadiusPerTick(-0.01F);
            cloud.setCustomParticle(ParticleTypes.WITCH);
            cloud.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 0));
            cloud.setOwner(this);
            level().addFreshEntity(cloud);
        }
    }

    @Override
    public void travel(Vec3 input) {
        if (isInWater()) {
            moveRelative(0.02F, input);
            move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.8));
        } else {
            moveRelative(getSpeed() * 0.1F, input);
            move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.91));
        }
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    public void playAmbientSound() {
        MobsB.layered(this, MobsB.WRAITH_FLUTTER.get(), 0.6F, getVoicePitch(), MobsB.WRAITH_WHISPER.get(), 0.4F, getVoicePitch() * 0.8F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return MobsB.WRAITH_WHISPER.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return MobsB.WRAITH_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return MobsB.WRAITH_DEATH.get();
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (variant() == CURSED ? 0.75F : variant() == BLANK ? 1.2F : 1.0F);
    }

    private boolean busy() {
        return phase() != Phase.NONE;
    }

    // ---- goals ---------------------------------------------------------------------------------------------------

    /** Picks the moment to dive: off cooldown, target in sight within 12 blocks. Gilded wraiths rarely dive. */
    private final class DiveGoal extends Goal {
        DiveGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (busy()) return true;
            LivingEntity t = getTarget();
            if (t == null || !t.isAlive() || cooldown > 0) return false;
            if (variant() == GILDED && random.nextInt(4) != 0) return false;
            double d = distanceToSqr(t);
            return d < 144 && d > 4 && hasLineOfSight(t) && random.nextInt(6) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return busy();
        }

        @Override
        public void start() {
            LivingEntity t = getTarget();
            if (!busy() && t != null) startTell(t);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    /** Gilded: every few seconds, gild the nearby Library creatures (and itself, weakly) with Absorption. */
    private final class WardAlliesGoal extends Goal {
        WardAlliesGoal() {
            setFlags(EnumSet.noneOf(Flag.class));
        }

        @Override
        public boolean canUse() {
            return variant() == GILDED && wardCooldown <= 0 && getTarget() != null && !busy();
        }

        @Override
        public void start() {
            wardAllies();
        }
    }

    /** Public for the screenshot test: gild every Library creature within 10 blocks. Returns how many. */
    public int wardAllies() {
        wardCooldown = 120;
        if (!(level() instanceof ServerLevel server)) return 0;
        List<Mob> allies = server.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(10.0),
                m -> m != this && m instanceof LibraryDweller && m.isAlive());
        for (Mob ally : allies) {
            ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 1), this);
            Vec3 from = position().add(0, 1.0, 0), to = ally.position().add(0, ally.getBbHeight() * 0.6, 0);
            for (int i = 0; i <= 8; i++) {
                Vec3 p = from.lerp(to, i / 8.0);
                server.sendParticles(ParticleTypes.WAX_ON, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }
        addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 0), this);
        server.broadcastEntityEvent(this, (byte) 61);
        MobsB.play(this, MobsB.WRAITH_WARD.get(), 1.0F, 1.0F);
        return allies.size();
    }

    /** Circles the target a few blocks up and out, so a dive comes from above. Gilded wraiths keep further back. */
    private final class CircleTargetGoal extends Goal {
        private int retarget;
        private float angle;

        CircleTargetGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return getTarget() != null && !busy();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            angle = random.nextFloat() * Mth.TWO_PI;
            retarget = 0;
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null) return;
            if (--retarget <= 0) {
                retarget = 10 + random.nextInt(10);
                angle += 0.6F + random.nextFloat() * 0.4F;
                double r = variant() == GILDED ? 7.0 : 4.5;
                double y = t.getY() + (variant() == GILDED ? 3.0 : 2.2);
                double x = t.getX() + Math.cos(angle) * r, z = t.getZ() + Math.sin(angle) * r;
                if (!level().getBlockState(BlockPos.containing(x, y, z)).isAir()) y = t.getY() + 1.0;
                getMoveControl().setWantedPosition(x, y, z, 1.0);
            }
            getLookControl().setLookAt(t, 30, 30);
        }
    }

    /** Idle drifting from one open spot to another nearby. */
    private final class DriftGoal extends Goal {
        DriftGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !getMoveControl().hasWanted() && getTarget() == null && random.nextInt(reducedTickDelay(20)) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void tick() {
        }

        @Override
        public void start() {
            BlockPos origin = blockPosition();
            for (int i = 0; i < 3; i++) {
                BlockPos p = origin.offset(random.nextInt(11) - 5, random.nextInt(5) - 2, random.nextInt(11) - 5);
                if (level().isEmptyBlock(p) && level().isEmptyBlock(p.above())) {
                    getMoveControl().setWantedPosition(p.getX() + 0.5, p.getY() + 0.3, p.getZ() + 0.5, 0.5);
                    return;
                }
            }
        }
    }

    /** Vex-style steering that keeps collisions: push the velocity toward the wanted point. */
    private static final class WraithMoveControl extends MoveControl<PageWraith> {
        WraithMoveControl(PageWraith mob) {
            super(mob);
        }

        @Override
        public void tick() {
            if (operation != Operation.MOVE_TO || mob.busy()) return;
            Vec3 delta = new Vec3(wantedX - mob.getX(), wantedY - mob.getY(), wantedZ - mob.getZ());
            double len = delta.length();
            if (len < 0.5) {
                operation = Operation.WAIT;
                mob.setDeltaMovement(mob.getDeltaMovement().scale(0.5));
                return;
            }
            double speed = mob.getAttributeValue(Attributes.FLYING_SPEED) * speedModifier;
            mob.setDeltaMovement(mob.getDeltaMovement().add(delta.scale(speed * 0.06 / len)));
            if (mob.getTarget() == null) {
                Vec3 m = mob.getDeltaMovement();
                mob.setYRot(-((float) Mth.atan2(m.x, m.z)) * Mth.RAD_TO_DEG);
            } else {
                double tx = mob.getTarget().getX() - mob.getX(), tz = mob.getTarget().getZ() - mob.getZ();
                mob.setYRot(-((float) Mth.atan2(tx, tz)) * Mth.RAD_TO_DEG);
            }
            mob.yBodyRot = mob.getYRot();
        }
    }
}
