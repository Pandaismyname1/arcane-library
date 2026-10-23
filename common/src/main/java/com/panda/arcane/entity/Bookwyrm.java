package com.panda.arcane.entity;

import com.panda.arcane.worldgen.LibraryLayout;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A long worm of bound books that lives in the shelves. Its head is a heavy tome with page-teeth.
 *
 * <ul>
 *   <li><b>Common</b>: burrows into a bookshelf and bursts out of another one near its prey. The shelf it will come out
 *       of rattles and sheds pages for a second first: step away from it.</li>
 *   <li><b>Glossy</b>: a lacquered, gilt-plated head shrugs off blows from the front (a quarter of the damage, with a
 *       clank). Its flanks and tail are soft: hit it from the side or behind for extra damage.</li>
 *   <li><b>Ancient</b> (Restricted Section): when hurt it crawls to a shelf and eats the books to heal. A solid hit
 *       while it chews makes it spit them out.</li>
 * </ul>
 */
public class Bookwyrm extends VariantMonster {
    public static final int COMMON = 0, GLOSSY = 1, ANCIENT = 2;
    private static final String[] NAMES = {"common", "glossy", "ancient"};
    public static final int SEGMENTS = 7;

    public enum Phase { NONE, DIVE, UNDER, EMERGE, CHEW }

    private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(Bookwyrm.class, EntityDataSerializers.INT);

    private int phaseTicks;
    private int burrowCooldown = 60;
    private int chewCooldown = 0;
    private @Nullable BlockPos emergeShelf;
    private @Nullable Vec3 emergeAt;
    private @Nullable BlockPos chewShelf;

    // client-side: yaw history sampled by distance travelled, for the trailing segments
    private final float[] yawHistory = new float[64];
    private int historyHead;
    private double lastX, lastZ;
    private boolean historyInit;
    public float jawO, jaw;

    public Bookwyrm(EntityType<? extends Bookwyrm> type, Level level) {
        super(type, level);
        this.xpReward = 9;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    public String[] variantNames() {
        return NAMES;
    }

    @Override
    protected int chooseVariant(MobsB.Zone zone, RandomSource random) {
        if (zone.restricted()) return MobsB.weighted(random, 20, 35, 45);
        if (zone.is(LibraryLayout.Kind.VAULT)) return MobsB.weighted(random, 30, 70, 0);
        return MobsB.weighted(random, 70, 30, 0);
    }

    @Override
    protected void applyVariantStats(int v) {
        base(Attributes.MAX_HEALTH, switch (v) { case GLOSSY -> 32.0; case ANCIENT -> 46.0; default -> 28.0; });
        base(Attributes.ATTACK_DAMAGE, switch (v) { case ANCIENT -> 7.0; default -> 5.0; });
        base(Attributes.ARMOR, v == GLOSSY ? 4.0 : 2.0);
        base(Attributes.MOVEMENT_SPEED, v == ANCIENT ? 0.23 : 0.27);
        scale(v == ANCIENT ? 1.25 : 1.0);
    }

    // ---- state ---------------------------------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASE, 0);
    }

    public Phase phase() {
        return Phase.values()[Mth.clamp(entityData.get(DATA_PHASE), 0, Phase.values().length - 1)];
    }

    private void setPhase(Phase p) {
        entityData.set(DATA_PHASE, p.ordinal());
        phaseTicks = 0;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setPhase(Phase.NONE);
        setInvisible(false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new BurrowGoal());
        goalSelector.addGoal(2, new ChewGoal());
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, true) {
            @Override
            public boolean canUse() {
                return phase() == Phase.NONE && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return phase() == Phase.NONE && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) clientTick();
    }

    private void clientTick() {
        if (!historyInit) {
            java.util.Arrays.fill(yawHistory, yBodyRot);
            lastX = getX();
            lastZ = getZ();
            historyInit = true;
        }
        double moved = Math.hypot(getX() - lastX, getZ() - lastZ);
        if (moved > 0.12) {
            historyHead = (historyHead + 1) % yawHistory.length;
            yawHistory[historyHead] = yBodyRot;
            lastX = getX();
            lastZ = getZ();
        } else {
            yawHistory[historyHead] = yBodyRot; // turning on the spot bends the neck first...
            for (int i = 0; i < yawHistory.length; i++) { // ...then the resting body slowly straightens behind it
                yawHistory[i] += Mth.clamp(Mth.wrapDegrees(yBodyRot - yawHistory[i]), -3.0F, 3.0F);
            }
        }
        jawO = jaw;
        float goal = phase() == Phase.CHEW ? 0.5F + 0.4F * Mth.sin(tickCount * 0.9F) : phase() == Phase.EMERGE ? 1.0F
                : isSwinging() || getTarget() != null && distanceToSqr(getTarget()) < 9 ? 0.6F : 0.08F;
        jaw += (goal - jaw) * 0.35F;
        if (phase() == Phase.CHEW && random.nextInt(2) == 0) {
            level().addParticle(ParticleTypes.WHITE_ASH, getX(), getY() + 0.5, getZ(), 0, 0.05, 0);
        }
    }

    /** Yaw (degrees) of segment {@code i} relative to the one in front of it, for the renderer. */
    public float segmentYaw(int i) {
        int spacing = 3;
        float front = yawHistory[Math.floorMod(historyHead - i * spacing, yawHistory.length)];
        float back = yawHistory[Math.floorMod(historyHead - (i + 1) * spacing, yawHistory.length)];
        return Mth.clamp(Mth.wrapDegrees(back - front), -40.0F, 40.0F);
    }

    // ---- mechanics -----------------------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        phaseTicks++;
        if (burrowCooldown > 0) burrowCooldown--;
        if (chewCooldown > 0) chewCooldown--;
        switch (phase()) {
            case DIVE -> {
                getNavigation().stop();
                setDeltaMovement(0, getDeltaMovement().y, 0);
                if (phaseTicks % 2 == 0) shelfBurst(level, blockPosition(), 6);
                if (phaseTicks >= 10) {
                    setInvisible(true);
                    setPhase(Phase.UNDER);
                    emergeShelf = null;
                }
            }
            case UNDER -> {
                getNavigation().stop();
                setDeltaMovement(0, 0, 0);
                LivingEntity target = getTarget() != null ? getTarget() : burrowTarget;
                if (emergeShelf == null && phaseTicks == 4) pickEmergeSpot(level, target);
                if (emergeShelf != null && phaseTicks >= 12) {
                    if (phaseTicks % 3 == 0) shelfBurst(level, emergeShelf, 5); // the tell: that shelf rattles
                    if (phaseTicks % 6 == 0) MobsB.play(this, MobsB.WYRM_CHEW.get(), 0.6F, 1.4F);
                }
                if (phaseTicks >= 32) emerge(level, target);
            }
            case EMERGE -> {
                LivingEntity target = getTarget();
                if (target != null && phaseTicks <= 6 && getBoundingBox().inflate(0.6).intersects(target.getBoundingBox())) {
                    doHurtTarget(level, target);
                    setPhase(Phase.NONE);
                }
                if (phaseTicks > 10) setPhase(Phase.NONE);
            }
            case CHEW -> tickChew(level);
            default -> { }
        }
    }

    /** Test hook and goal entry: dive into the shelves. */
    public void startBurrow() {
        burrowTarget = getTarget();
        setPhase(Phase.DIVE);
        burrowCooldown = 160;
        MobsB.layered(this, MobsB.WYRM_BURROW.get(), 1.0F, 1.0F, MobsB.WYRM_AMBIENT.get(), 0.7F, 0.6F);
    }

    /** Whom it burrowed after: an invisible mob can lose its target while under the shelves. */
    private @Nullable LivingEntity burrowTarget;

    private void pickEmergeSpot(ServerLevel level, @Nullable LivingEntity target) {
        BlockPos around = target != null ? target.blockPosition() : blockPosition();
        BlockPos best = null;
        Vec3 bestStand = null;
        double bestScore = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(around.offset(-6, -1, -6), around.offset(6, 2, 6))) {
            if (!isShelf(level.getBlockState(p))) continue;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos stand = p.relative(d);
                if (!level.getBlockState(stand).isAir() || !level.getBlockState(stand.above()).isAir()) continue;
                BlockPos floor = stand.below();
                if (level.getBlockState(floor).isAir()) {
                    if (!level.getBlockState(floor.below()).isAir() && level.getBlockState(floor.above()).isAir()) stand = floor;
                    else continue;
                }
                double dist = target == null ? 0 : Math.abs(Math.sqrt(stand.distSqr(target.blockPosition())) - 2.5);
                if (dist < bestScore) {
                    bestScore = dist;
                    best = p.immutable();
                    bestStand = Vec3.atBottomCenterOf(stand);
                }
            }
        }
        emergeShelf = best;
        emergeAt = bestStand;
    }

    private void emerge(ServerLevel level, @Nullable LivingEntity target) {
        if (emergeAt != null) {
            teleportTo(emergeAt.x, emergeAt.y, emergeAt.z);
            if (target != null) {
                setTarget(target);
                double dx = target.getX() - getX(), dz = target.getZ() - getZ();
                float yaw = -((float) Mth.atan2(dx, dz)) * Mth.RAD_TO_DEG;
                setYRot(yaw);
                yBodyRot = yaw;
                yHeadRot = yaw;
                setDeltaMovement(new Vec3(dx, 0, dz).normalize().scale(0.55).add(0, 0.25, 0));
            }
            if (emergeShelf != null) shelfBurst(level, emergeShelf, 18);
        }
        setInvisible(false);
        setPhase(Phase.EMERGE);
        MobsB.layered(this, MobsB.WYRM_EMERGE.get(), 1.2F, 1.0F, MobsB.WYRM_BURROW.get(), 0.8F, 1.3F);
    }

    private static void shelfBurst(ServerLevel level, BlockPos pos, int count) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BOOKSHELF.defaultBlockState()),
                pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, count, 0.4, 0.4, 0.4, 0.05);
        level.sendParticles(ParticleTypes.WHITE_ASH, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, count / 2, 0.5, 0.5, 0.5, 0.02);
    }

    public static boolean isShelf(BlockState state) {
        return state.is(Blocks.BOOKSHELF) || state.is(Blocks.CHISELED_BOOKSHELF) || state.is(com.panda.arcane.registry.ModBlocks.LIBRARY_BOOKSHELF.get())
                || net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().endsWith("knowledge_shelf");
    }

    private @Nullable BlockPos adjacentShelf(int radius) {
        BlockPos me = blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(me.offset(-radius, 0, -radius), me.offset(radius, 1, radius))) {
            if (isShelf(level().getBlockState(p))) return p.immutable();
        }
        return null;
    }

    /** Ancient: test hook and goal entry. Start chewing the books of this shelf. */
    public void startChew(BlockPos shelf) {
        chewShelf = shelf.immutable();
        setPhase(Phase.CHEW);
    }

    private void tickChew(ServerLevel level) {
        getNavigation().stop();
        if (chewShelf == null || !isShelf(level.getBlockState(chewShelf))) {
            setPhase(Phase.NONE);
            return;
        }
        getLookControl().setLookAt(Vec3.atCenterOf(chewShelf));
        if (phaseTicks % 5 == 0) {
            shelfBurst(level, chewShelf, 4);
            MobsB.play(this, MobsB.WYRM_CHEW.get(), 0.9F, 0.8F + random.nextFloat() * 0.2F);
        }
        if (phaseTicks >= 40) {
            if (com.panda.arcane.platform.Services.PLATFORM.canGrief(level, this)) {
                level.destroyBlock(chewShelf, false, this);
            }
            heal(12.0F);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 0.8, getZ(), 8, 0.4, 0.3, 0.4, 0.0);
            chewCooldown = 140;
            chewShelf = null;
            setPhase(Phase.NONE);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        Phase p = phase();
        if (p == Phase.UNDER || p == Phase.DIVE && phaseTicks > 6) return false;
        if (variant() == GLOSSY && !source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            Vec3 from = source.getSourcePosition();
            if (from != null) {
                Vec3 look = Vec3.directionFromRotation(0, yBodyRot);
                Vec3 to = from.subtract(position()).multiply(1, 0, 1).normalize();
                double dot = look.dot(to);
                if (dot > 0.45) { // struck on the armoured head
                    damage *= 0.25F;
                    MobsB.play(this, MobsB.WYRM_CLANK.get(), 1.0F, 1.0F);
                    level.sendParticles(ParticleTypes.CRIT, getX() + look.x * 0.8, getY() + 0.6, getZ() + look.z * 0.8, 10, 0.2, 0.2, 0.2, 0.2);
                } else if (dot < -0.2) {
                    damage *= 1.25F;
                }
            }
        }
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && p == Phase.CHEW && damage >= 3.0F) { // spit the books out
            setPhase(Phase.NONE);
            chewCooldown = 100;
            MobsB.play(this, MobsB.WYRM_HURT.get(), 1.0F, 0.6F);
        }
        return hurt;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) MobsB.play(this, MobsB.WYRM_EMERGE.get(), 0.6F, 1.5F);
        return hit;
    }

    @Override
    public boolean isPickable() {
        return phase() != Phase.UNDER && super.isPickable();
    }

    @Override
    public boolean isPushable() {
        return phase() == Phase.NONE && super.isPushable();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return phase() == Phase.UNDER ? null : MobsB.WYRM_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return MobsB.WYRM_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return MobsB.WYRM_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (tickCount % 4 == 0) playSound(net.minecraft.sounds.SoundEvents.SILVERFISH_STEP, 0.3F, 0.6F);
    }

    // ---- goals ---------------------------------------------------------------------------------------------------

    private final class BurrowGoal extends Goal {
        BurrowGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            Phase p = phase();
            if (p == Phase.DIVE || p == Phase.UNDER || p == Phase.EMERGE) return true;
            LivingEntity t = getTarget();
            if (variant() == GLOSSY || p != Phase.NONE || t == null || burrowCooldown > 0 || !onGround()) return false;
            double d = distanceToSqr(t);
            return d > 16 && d < 400 && adjacentShelf(2) != null && random.nextInt(10) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            Phase p = phase();
            return p == Phase.DIVE || p == Phase.UNDER || p == Phase.EMERGE;
        }

        @Override
        public void start() {
            if (phase() == Phase.NONE) startBurrow();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    private final class ChewGoal extends Goal {
        private @Nullable BlockPos shelf;

        ChewGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (phase() == Phase.CHEW) return true;
            if (variant() != ANCIENT || phase() != Phase.NONE || chewCooldown > 0 || getHealth() > getMaxHealth() * 0.6F) return false;
            shelf = adjacentShelf(6);
            return shelf != null;
        }

        @Override
        public boolean canContinueToUse() {
            return phase() == Phase.CHEW || shelf != null && phase() == Phase.NONE && !getNavigation().isDone();
        }

        @Override
        public void start() {
            if (phase() != Phase.CHEW && shelf != null) getNavigation().moveTo(shelf.getX() + 0.5, shelf.getY(), shelf.getZ() + 0.5, 1.2);
        }

        @Override
        public void tick() {
            if (phase() == Phase.NONE && shelf != null && distanceToSqr(Vec3.atCenterOf(shelf)) < 3.2) {
                startChew(shelf);
                shelf = null;
            }
        }

        @Override
        public void stop() {
            shelf = null;
        }
    }
}
