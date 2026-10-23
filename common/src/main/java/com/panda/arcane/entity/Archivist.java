package com.panda.arcane.entity;

import com.panda.arcane.ArcaneMod;
import com.panda.arcane.entity.ArchivistPattern.Move;
import com.panda.arcane.portal.PortalTravel;
import com.panda.arcane.registry.ModEntities;
import com.panda.arcane.registry.ModSounds;
import com.panda.arcane.registry.ModTriggers;
import com.panda.arcane.worldgen.LibraryLayout;
import com.panda.arcane.worldgen.Mat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Archivist: keeper of the Restricted Section, fought in its Sanctum (the ARENA cell).
 *
 * <p>Four telegraphed, counterable moves. Each has a pose (client clip), a floor/air particle tell and a sound:
 * <ul>
 * <li><b>Page Storm</b>: arms raised, pages swirl, gold aim lines that turn red when the aim locks. Phase 1 a fan
 * (sidestep after the lock), phase 2 a fan then a slow ring of bolts (stand straight behind a pillar), phase 3 a
 * rotating spiral (keep moving, use pillars).</li>
 * <li><b>Erasure</b>: the giant quill is raised and red lines are scored across the floor; then a wave runs along them,
 * deleting bolts, projectiles and any block that is not part of the Library, and erasing anyone standing on them
 * (heavy damage, erases your longest-lasting beneficial effect). Step off the line. One line, then a V, then a cross.</li>
 * <li><b>Recall</b> (phase 2+): the tome is held out and a violet tether binds the target. Put a pillar between you
 * and the Archivist before it closes and the tether snaps, staggering the Archivist (takes 50% more damage). If it
 * holds you are yanked to the dais and the <b>Page Slam</b> follows: run out of the orange ring before it lands.</li>
 * <li><b>Index</b> (phase 2+): rune circles light at the arena's ward sockets and Vault Wards appear. While any lives,
 * the Archivist is warded (75% less damage, a beam joins it to each ward, boss bar turns blue). Kill the wards.</li>
 * </ul>
 * Phases (by health, forward only, each opened by an invulnerable transition): 1 The Reading (walks, melee, fan,
 * single Erasure, slam if hugged); 2 The Index (wards, Recall + Slam, ring storm, V Erasure); 3 The Final Chapter
 * (hovers over the dais and never walks again: spiral storm, Erasure cross, faster Recall).
 */
public class Archivist extends Monster implements LibraryDweller {
    private static final EntityDataAccessor<Byte> DATA_MOVE = SynchedEntityData.defineId(Archivist.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_PHASE = SynchedEntityData.defineId(Archivist.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_FLAGS = SynchedEntityData.defineId(Archivist.class, EntityDataSerializers.BYTE);
    private static final int FLAG_FIGHTING = 1, FLAG_WARDED = 2, FLAG_HOVER = 4;
    /** How high phase 3 hovers over the dais. */
    public static final double HOVER_HEIGHT = 1.4;
    /** The Archivist forgets anyone this far from its dais. */
    private static final double LEASH = 22.0;

    // tells
    // Full-bright vanilla particles where the tell must read in a dark room; small dust for coloured lines.
    private static final ParticleOptions AIM_GOLD = ParticleTypes.END_ROD;
    private static final ParticleOptions AIM_RED = ParticleTypes.FLAME;
    private static final DustParticleOptions SPOKE = new DustParticleOptions(0xB07CFF, 1.0F);
    private static final DustParticleOptions ERASE_LINE = new DustParticleOptions(0xFF3050, 1.6F);
    private static final DustParticleOptions ERASE_EDGE = new DustParticleOptions(0xE02040, 1.2F);
    private static final DustParticleOptions TETHER = new DustParticleOptions(0xC08CFF, 1.0F);
    private static final ParticleOptions TETHER_WEAK = ParticleTypes.ELECTRIC_SPARK; // full-bright, so it reads in the dark
    private static final ParticleOptions SLAM_RING = ParticleTypes.FLAME;
    private static final ParticleOptions SLAM_FILL = ParticleTypes.SMALL_FLAME;
    private static final ParticleOptions SLAM_HOT = ParticleTypes.END_ROD;
    private static final ParticleOptions RUNE = ParticleTypes.SOUL_FIRE_FLAME;
    private static final DustParticleOptions WARD_BEAM = new DustParticleOptions(0x7FD8FF, 0.9F);

    private final ServerBossEvent bossEvent = new ServerBossEvent(getUUID(), getDisplayName(), BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.NOTCHED_6);

    // client animation clips (started from the synced move id)
    public final AnimationState idleAnimation = new AnimationState();
    public final AnimationState stormAnimation = new AnimationState();
    public final AnimationState erasureAnimation = new AnimationState();
    public final AnimationState recallAnimation = new AnimationState();
    public final AnimationState slamAnimation = new AnimationState();
    public final AnimationState indexAnimation = new AnimationState();
    public final AnimationState transitionAnimation = new AnimationState();
    public final AnimationState staggerAnimation = new AnimationState();

    // server state
    private @Nullable BlockPos home;
    private int phaseReached = 1;
    private Move move = Move.NONE;
    private int moveTick;
    private int moveWindup;
    private int moveEnd;
    private @Nullable LivingEntity moveTarget;
    private Vec3 aimPoint = Vec3.ZERO;
    private Vec3 lineOrigin = Vec3.ZERO;
    private float[] lineYaws = new float[0];
    private Vec3 pullDest = Vec3.ZERO;
    private final List<BlockPos> sockets = new ArrayList<>();
    private final Set<UUID> hitThisMove = new HashSet<>();
    private final List<UUID> wards = new ArrayList<>();
    private final Map<Move, Integer> cooldowns = new EnumMap<>(Move.class);
    private int globalCooldown = 40;
    private int hugTicks;
    private int calmTicks;
    private boolean autonomous = true;
    private @Nullable LibraryLayout layout;

    /** Counters for tests. */
    public final Stats stats = new Stats();

    public static final class Stats {
        public int stormBolts, erasedBlocks, erasedProjectiles, erasureHits, pulls, snaps, slamHits, wardsSummoned, transitions;
        public final List<Move> moves = new ArrayList<>();

        @Override
        public String toString() {
            return "bolts=" + stormBolts + " erasedBlocks=" + erasedBlocks + " erasedProjectiles=" + erasedProjectiles + " erasureHits=" + erasureHits
                    + " pulls=" + pulls + " snaps=" + snaps + " slamHits=" + slamHits + " wards=" + wardsSummoned + " transitions=" + transitions
                    + " moves=" + moves;
        }
    }

    public Archivist(EntityType<? extends Archivist> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        bossEvent.setDarkenScreen(false);
    }

    @Override
    protected int getBaseExperienceReward(ServerLevel level) {
        return 500;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 360.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.SCALE, 1.6);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_MOVE, (byte) 0);
        builder.define(DATA_PHASE, (byte) 1);
        builder.define(DATA_FLAGS, (byte) 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new HoldStillGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** While casting or hovering the Archivist holds the move and look controls, so melee and wandering stand down. */
    private final class HoldStillGoal extends Goal {
        HoldStillGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return holdsStill();
        }

        @Override
        public boolean canContinueToUse() {
            return holdsStill();
        }

        @Override
        public void start() {
            getNavigation().stop();
        }
    }

    // ---- public view ---------------------------------------------------------------------------------------------

    public boolean holdsStill() {
        return move() != Move.NONE || isHovering() || !autonomous;
    }

    public Move move() {
        return Move.byId(entityData.get(DATA_MOVE));
    }

    /** Ticks into the current move (server only). */
    public int moveTick() {
        return moveTick;
    }

    public int moveWindup() {
        return moveWindup;
    }

    /** The phase on the boss bar: 1, 2 or 3. Synced. */
    public int phase() {
        return entityData.get(DATA_PHASE);
    }

    public boolean isFighting() {
        return (entityData.get(DATA_FLAGS) & FLAG_FIGHTING) != 0;
    }

    public boolean isWarded() {
        return (entityData.get(DATA_FLAGS) & FLAG_WARDED) != 0;
    }

    public boolean isHovering() {
        return (entityData.get(DATA_FLAGS) & FLAG_HOVER) != 0;
    }

    public boolean isStaggered() {
        return move() == Move.STAGGER;
    }

    public @Nullable BlockPos home() {
        return home;
    }

    /** Test hook: stop choosing moves by itself (forced moves still run). */
    public void setAutonomous(boolean autonomous) {
        this.autonomous = autonomous;
    }

    /** Test hook: start a move now against a target, cancelling whatever was running. */
    public void forceMove(Move m, LivingEntity target) {
        setTarget(target);
        startMove(m, target);
    }

    public List<Entity> liveWards() {
        List<Entity> out = new ArrayList<>();
        if (level() instanceof ServerLevel server) {
            for (UUID id : wards) {
                Entity e = server.getEntity(id);
                if (e != null && e.isAlive()) out.add(e);
            }
        }
        return out;
    }

    private void setFlag(int flag, boolean on) {
        byte b = entityData.get(DATA_FLAGS);
        byte n = (byte) (on ? b | flag : b & ~flag);
        if (n != b) entityData.set(DATA_FLAGS, n);
    }

    // ---- client --------------------------------------------------------------------------------------------------

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_MOVE.equals(accessor) && level().isClientSide()) {
            for (AnimationState s : List.of(stormAnimation, erasureAnimation, recallAnimation, slamAnimation, indexAnimation,
                    transitionAnimation, staggerAnimation)) s.stop();
            switch (move()) {
                case STORM -> stormAnimation.start(tickCount);
                case ERASURE -> erasureAnimation.start(tickCount);
                case RECALL -> recallAnimation.start(tickCount);
                case SLAM -> slamAnimation.start(tickCount);
                case INDEX -> indexAnimation.start(tickCount);
                case TRANSITION -> transitionAnimation.start(tickCount);
                case STAGGER -> staggerAnimation.start(tickCount);
                case NONE -> { }
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) idleAnimation.startIfStopped(tickCount);
    }

    // ---- server AI -----------------------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (home == null) {
            home = blockPosition();
            setHomeTo(home, 9);
        }
        updateBossBar(level);
        tickWards(level);

        LivingEntity target = getTarget();
        if (target != null && !validTarget(target)) {
            setTarget(null);
            target = null;
        }
        boolean fighting = target != null || move != Move.NONE;
        setFlag(FLAG_FIGHTING, fighting);
        if (isHovering()) hover();

        int want = ArchivistPattern.phaseFor(getHealth() / getMaxHealth(), phaseReached);
        if (want > phaseReached && move != Move.TRANSITION) {
            startMove(Move.TRANSITION, target);
        }

        if (move != Move.NONE) {
            tickMove(level);
            return;
        }
        if (!fighting) {
            if (++calmTicks > 60 && !isHovering() && home != null && distanceToSqr(Vec3.atBottomCenterOf(home)) > 4.0) {
                getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0.8);
            }
            if (calmTicks > 100 && tickCount % 20 == 0 && getHealth() < getMaxHealth()) heal(2.0F);
            return;
        }
        calmTicks = 0;
        if (!autonomous) return;
        double dist = horizontalDistance(target);
        hugTicks = dist < 3.5 ? hugTicks + 1 : Math.max(0, hugTicks - 2);
        if (home != null && !isHovering() && distanceToSqr(Vec3.atBottomCenterOf(home)) > 100) {
            getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.0);
        }
        cooldowns.replaceAll((m, cd) -> Math.max(0, cd - 1));
        if (globalCooldown > 0) {
            globalCooldown--;
            return;
        }
        Move next = chooseMove(target, dist);
        if (next != Move.NONE) startMove(next, target);
    }

    private boolean validTarget(LivingEntity target) {
        if (!target.isAlive() || target.level() != level()) return false;
        if (target instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        Vec3 h = home == null ? position() : Vec3.atBottomCenterOf(home);
        return target.distanceToSqr(h) < LEASH * LEASH;
    }

    private double horizontalDistance(Entity e) {
        double dx = e.getX() - getX(), dz = e.getZ() - getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private Move chooseMove(LivingEntity target, double dist) {
        int phase = phaseReached;
        List<Move> pool = new ArrayList<>();
        List<Integer> weight = new ArrayList<>();
        java.util.function.BiConsumer<Move, Integer> offer = (m, w) -> {
            if (w > 0 && cooldowns.getOrDefault(m, 0) <= 0) {
                pool.add(m);
                weight.add(w);
            }
        };
        boolean sees = hasLineOfSight(target);
        offer.accept(Move.STORM, dist > 3.0 && sees ? 3 : 0);
        offer.accept(Move.ERASURE, 3);
        offer.accept(Move.SLAM, hugTicks > 40 || (isHovering() && dist < 3.5) ? 6 : 0);
        if (phase >= 2) {
            offer.accept(Move.RECALL, dist > 6.0 ? 3 : 0);
            offer.accept(Move.INDEX, liveWards().isEmpty() ? (phase >= 3 ? 2 : 4) : 0);
        }
        int total = weight.stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) return Move.NONE;
        int roll = random.nextInt(total);
        for (int i = 0; i < pool.size(); i++) {
            roll -= weight.get(i);
            if (roll < 0) return pool.get(i);
        }
        return Move.NONE;
    }

    private void startMove(Move m, @Nullable LivingEntity target) {
        int phase = m == Move.TRANSITION ? phaseReached + 1 : phaseReached;
        move = m;
        moveTick = 0;
        moveTarget = target;
        moveWindup = ArchivistPattern.windup(m, phase);
        moveEnd = moveWindup + ArchivistPattern.active(m, phase) + ArchivistPattern.recover(m, phase);
        hitThisMove.clear();
        entityData.set(DATA_MOVE, (byte) m.ordinal());
        getNavigation().stop();
        cooldowns.put(m, ArchivistPattern.cooldown(m, phase));
        stats.moves.add(m);
        ArcaneMod.LOGGER.debug("Archivist phase {} starts {}", phase, m);
        switch (m) {
            case STORM -> {
                sound(ModSounds.ARCHIVIST_STORM_CHARGE.get(), 1.6F, 1.0F);
                if (target != null) aimPoint = target.position().add(0, 1.0, 0);
            }
            case ERASURE -> {
                lineOrigin = new Vec3(getX(), groundY(), getZ());
                float aim = target == null ? getYRot() : ArchivistPattern.yawTo(getX(), getZ(), target.getX(), target.getZ());
                lineYaws = ArchivistPattern.erasureYaws(phase, aim);
                faceYaw(aim);
                sound(ModSounds.ARCHIVIST_ERASE_CHARGE.get(), 1.8F, 0.8F);
            }
            case RECALL -> sound(ModSounds.ARCHIVIST_RECALL_CHARGE.get(), 2.0F, 0.7F);
            case SLAM -> sound(ModSounds.ARCHIVIST_SLAM_CHARGE.get(), 1.8F, 0.8F);
            case INDEX -> {
                pickSockets(phase, target);
                sound(ModSounds.ARCHIVIST_INDEX.get(), 2.0F, 0.8F);
            }
            case TRANSITION -> beginTransition(phase);
            case STAGGER -> { }
            case NONE -> { }
        }
    }

    private void endMove() {
        Move ended = move;
        move = Move.NONE;
        moveTarget = null;
        entityData.set(DATA_MOVE, (byte) 0);
        globalCooldown = ArchivistPattern.globalCooldown(phaseReached);
        if (ended == Move.TRANSITION) finishTransition();
    }

    private void tickMove(ServerLevel level) {
        moveTick++;
        LivingEntity target = moveTarget;
        if (target != null && !target.isAlive()) target = moveTarget = null;
        getNavigation().stop();
        if (target != null && move != Move.ERASURE && move != Move.STAGGER && move != Move.TRANSITION) {
            faceYaw(ArchivistPattern.yawTo(getX(), getZ(), target.getX(), target.getZ()));
            getLookControl().setLookAt(target, 60F, 60F);
        }
        Move current = move;
        if (moveTick < moveWindup) {
            telegraph(level, target, moveTick);
        } else if (moveTick < moveWindup + ArchivistPattern.active(move, phaseReached)) {
            act(level, target, moveTick - moveWindup);
        }
        if (move != current) return; // the move handed over to another (Recall -> Slam, snapped tether -> Stagger)
        if (moveTick >= moveEnd) endMove();
    }

    private void faceYaw(float yaw) {
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    private double groundY() {
        return isHovering() && home != null ? home.getY() : getY();
    }

    private Vec3 chest() {
        return new Vec3(getX(), groundY() + 2.0, getZ());
    }

    // ---- tells ---------------------------------------------------------------------------------------------------

    private void telegraph(ServerLevel level, @Nullable LivingEntity target, int t) {
        switch (move) {
            case STORM -> stormTell(level, target, t);
            case ERASURE -> erasureTell(level, t);
            case RECALL -> recallTell(level, target, t);
            case SLAM -> slamTell(level, t);
            case INDEX -> indexTell(level, t);
            case TRANSITION -> transitionTell(level, t);
            case STAGGER -> {
                if (t % 4 == 0) {
                    for (int i = 0; i < 5; i++) {
                        double a = t * 0.3 + i * Math.PI * 2 / 5;
                        particle(level, ParticleTypes.CRIT, getX() + Math.cos(a) * 0.8, getY() + 3.4, getZ() + Math.sin(a) * 0.8);
                    }
                }
            }
            case NONE -> { }
        }
    }

    private void stormTell(ServerLevel level, @Nullable LivingEntity target, int t) {
        int lockAt = moveWindup - ArchivistPattern.STORM_LOCK_BEFORE;
        if (target != null && t <= lockAt) aimPoint = target.position().add(0, 1.0, 0);
        if (t == lockAt) sound(ModSounds.ARCHIVIST_STORM_LOCK.get(), 1.4F, 1.6F);
        Vec3 c = chest();
        // pages swirl inward around the Archivist
        for (int i = 0; i < 4; i++) {
            double a = t * 0.35 + i * Math.PI / 2;
            double r = 2.2 - t * 0.04;
            level.sendParticles(ParticleTypes.ENCHANT, c.x + Math.cos(a) * r, c.y + 0.6 + Math.sin(t * 0.2 + i) * 0.5, c.z + Math.sin(a) * r,
                    0, -Math.cos(a), 0, -Math.sin(a), 0.6);
        }
        boolean locked = t > lockAt;
        if (t % (locked ? 2 : 4) != 0) return;
        if (phaseReached < 3) {
            float baseYaw = ArchivistPattern.yawTo(getX(), getZ(), aimPoint.x, aimPoint.z);
            for (float off : ArchivistPattern.fanOffsets(phaseReached)) {
                Vec3 dir = Vec3.directionFromRotation(0, baseYaw + off).add(0, (aimPoint.y - c.y) / Math.max(1, c.distanceTo(aimPoint)), 0).normalize();
                for (double s = 1.6; s < 8.5; s += 0.7) {
                    Vec3 p = c.add(dir.scale(s));
                    particle(level, locked ? AIM_RED : AIM_GOLD, p.x, p.y, p.z);
                }
            }
        }
        if (phaseReached == 2 && t > moveWindup / 2) spokes(level, ArchivistPattern.RING_BOLTS, 0F, 1.4, 3.4);
        if (phaseReached >= 3) spokes(level, 3, t * 9F, 1.4, 4.0);
    }

    private void spokes(ServerLevel level, int n, float startYaw, double from, double to) {
        double y = groundY() + 1.2;
        for (int i = 0; i < n; i++) {
            Vec3 dir = Vec3.directionFromRotation(0, startYaw + i * 360F / n);
            for (double s = from; s <= to; s += 0.6) particle(level, SPOKE, getX() + dir.x * s, y, getZ() + dir.z * s);
        }
    }

    private void erasureTell(ServerLevel level, int t) {
        if (t == moveWindup - 12) sound(ModSounds.ARCHIVIST_ERASE_CHARGE.get(), 1.8F, 1.3F);
        if (t % 2 != 0) return;
        boolean late = t > moveWindup - 12;
        double y = lineOrigin.y + 0.12;
        for (float yaw : lineYaws) {
            Vec3 dir = Vec3.directionFromRotation(0, yaw);
            Vec3 side = new Vec3(-dir.z, 0, dir.x).scale(ArchivistPattern.ERASE_HALF_WIDTH);
            for (double s = 0.8; s <= ArchivistPattern.ERASE_LENGTH; s += 0.5) {
                Vec3 p = lineOrigin.add(dir.scale(s));
                particle(level, ERASE_LINE, p.x, y, p.z);
                particle(level, ERASE_EDGE, p.x + side.x, y, p.z + side.z);
                particle(level, ERASE_EDGE, p.x - side.x, y, p.z - side.z);
                if (late && ((int) (s / 0.5)) % 3 == 0) particle(level, ParticleTypes.END_ROD, p.x, y + 0.1, p.z);
            }
        }
    }

    private void recallTell(ServerLevel level, @Nullable LivingEntity target, int t) {
        if (target == null) return;
        if (t == moveWindup - 10) sound(SoundEvents.CHAIN_PLACE, 1.5F, 0.6F);
        boolean holds = hasLineOfSight(target);
        Vec3 from = tomePoint();
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.55, 0);
        double len = from.distanceTo(to);
        Vec3 step = to.subtract(from).normalize();
        double sway = Math.sin(t * 0.5) * 0.15;
        for (double s = 0.3; s < len; s += 0.45) {
            Vec3 p = from.add(step.scale(s)).add(0, Math.sin(s * 0.9 + t * 0.6) * sway, 0);
            if (holds) particle(level, TETHER, p.x, p.y, p.z);
            else if (((int) (s / 0.45)) % 3 != 2) particle(level, TETHER_WEAK, p.x, p.y, p.z); // a broken, crackling dashed line
        }
        if (t % 4 == 0) level.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getY() + 0.2, target.getZ(), 8, 0.4, 0.1, 0.4, 0.02);
    }

    private Vec3 tomePoint() {
        Vec3 fwd = Vec3.directionFromRotation(0, getYRot());
        return new Vec3(getX() + fwd.x * 1.2, groundY() + 2.3, getZ() + fwd.z * 1.2);
    }

    private void slamTell(ServerLevel level, int t) {
        double y = groundY() + 0.15;
        boolean hot = t > moveWindup - 6;
        if (t % 2 == 0) {
            int n = 40;
            for (int i = 0; i < n; i++) {
                double a = i * Math.PI * 2 / n;
                particle(level, hot ? SLAM_HOT : SLAM_RING, getX() + Math.cos(a) * ArchivistPattern.SLAM_RADIUS, y, getZ() + Math.sin(a) * ArchivistPattern.SLAM_RADIUS);
            }
            double r = ArchivistPattern.SLAM_RADIUS * (t + 1) / moveWindup;
            int m = Math.max(8, (int) (r * 6));
            for (int i = 0; i < m; i++) {
                double a = i * Math.PI * 2 / m + t * 0.1;
                particle(level, SLAM_FILL, getX() + Math.cos(a) * r, y, getZ() + Math.sin(a) * r);
            }
        }
        if (hot) level.sendParticles(ParticleTypes.CRIT, tomePoint().x, tomePoint().y + 1.5, tomePoint().z, 3, 0.3, 0.3, 0.3, 0.1);
    }

    private void indexTell(ServerLevel level, int t) {
        if (t % 2 != 0) return;
        for (BlockPos s : sockets) {
            double cx = s.getX() + 0.5, cz = s.getZ() + 0.5, y = s.getY() + 0.12;
            for (int i = 0; i < 14; i++) {
                double a = i * Math.PI * 2 / 14 + t * 0.08;
                particle(level, RUNE, cx + Math.cos(a) * 1.1, y, cz + Math.sin(a) * 1.1);
            }
            double rise = 2.6 * t / moveWindup;
            level.sendParticles(ParticleTypes.END_ROD, cx, y + rise * 0.5, cz, 2, 0.15, rise * 0.25, 0.15, 0.0);
            if (t > moveWindup - 14) {
                Vec3 from = chest();
                Vec3 to = new Vec3(cx, y + 1.4, cz);
                Vec3 step = to.subtract(from);
                for (double k = 0.1; k < 1.0; k += 0.08) particle(level, SPOKE, from.x + step.x * k, from.y + step.y * k, from.z + step.z * k);
            }
        }
    }

    private void transitionTell(ServerLevel level, int t) {
        Vec3 c = chest();
        for (int i = 0; i < 3; i++) {
            double a = t * 0.5 + i * Math.PI * 2 / 3;
            level.sendParticles(ParticleTypes.END_ROD, c.x + Math.cos(a) * 1.4, c.y - 1.5 + t * 0.06, c.z + Math.sin(a) * 1.4, 1, 0, 0, 0, 0.0);
        }
        if (t % 5 == 0) level.sendParticles(ParticleTypes.ENCHANT, c.x, c.y + 0.5, c.z, 30, 1.0, 1.4, 1.0, 1.2);
        if (t == 30) {
            for (int i = 0; i < 48; i++) {
                double a = i * Math.PI * 2 / 48;
                level.sendParticles(ParticleTypes.SONIC_BOOM, getX() + Math.cos(a) * 4, groundY() + 0.6, getZ() + Math.sin(a) * 4, 1, 0, 0, 0, 0);
            }
        }
    }

    // ---- moves landing -------------------------------------------------------------------------------------------

    private void act(ServerLevel level, @Nullable LivingEntity target, int t) {
        switch (move) {
            case STORM -> stormAct(level, t);
            case ERASURE -> erasureAct(level, t);
            case RECALL -> recallAct(level, target, t);
            case SLAM -> slamAct(level);
            case INDEX -> indexAct(level, target);
            default -> { }
        }
    }

    private void stormAct(ServerLevel level, int t) {
        Vec3 c = chest();
        if (phaseReached >= 3) {
            if (t % 2 == 0) {
                for (int k = 0; k < 3; k++) {
                    Vec3 dir = Vec3.directionFromRotation(0, t * 9F + k * 120F);
                    bolt(level, new Vec3(getX(), groundY() + 1.2, getZ()).add(dir.scale(1.0)), dir, 0.5, 4.0F);
                }
                if (t % 6 == 0) sound(ModSounds.ARCHIVIST_STORM_RELEASE.get(), 0.9F, 1.4F);
            }
            return;
        }
        if (t == 0) {
            float baseYaw = ArchivistPattern.yawTo(getX(), getZ(), aimPoint.x, aimPoint.z);
            for (float off : ArchivistPattern.fanOffsets(phaseReached)) {
                Vec3 dir = Vec3.directionFromRotation(0, baseYaw + off).add(0, (aimPoint.y - c.y) / Math.max(1, c.distanceTo(aimPoint)), 0).normalize();
                bolt(level, c.add(dir.scale(1.2)), dir, 0.9, 6.0F).effect(MobEffects.SLOWNESS, 40);
            }
            sound(ModSounds.ARCHIVIST_STORM_RELEASE.get(), 1.5F, 0.9F);
        }
        if (phaseReached == 2 && t == 10) {
            for (int i = 0; i < ArchivistPattern.RING_BOLTS; i++) {
                Vec3 dir = Vec3.directionFromRotation(0, i * 360F / ArchivistPattern.RING_BOLTS);
                bolt(level, new Vec3(getX(), groundY() + 1.2, getZ()).add(dir.scale(1.0)), dir, 0.45, 5.0F);
            }
            sound(ModSounds.ARCHIVIST_STORM_RELEASE.get(), 1.5F, 0.6F);
        }
    }

    private ArcaneBolt bolt(ServerLevel level, Vec3 at, Vec3 dir, double speed, float damage) {
        ArcaneBolt bolt = new ArcaneBolt(level, this, dir).damage(damage).speed(speed, dir).lifetime(60);
        bolt.setPos(at.x, at.y, at.z);
        level.addFreshEntity(bolt);
        stats.stormBolts++;
        return bolt;
    }

    private void erasureAct(ServerLevel level, int t) {
        if (t == 0) sound(ModSounds.ARCHIVIST_ERASE.get(), 2.0F, 0.9F);
        double prev = t * ArchivistPattern.ERASE_SPEED;
        double front = Math.min(ArchivistPattern.ERASE_LENGTH, (t + 1) * ArchivistPattern.ERASE_SPEED);
        if (prev > ArchivistPattern.ERASE_LENGTH) return;
        double y0 = lineOrigin.y;
        boolean grief = com.panda.arcane.platform.Services.PLATFORM.canGrief(level, this);
        for (float yaw : lineYaws) {
            Vec3 dir = Vec3.directionFromRotation(0, yaw);
            Vec3 side = new Vec3(-dir.z, 0, dir.x);
            for (double s = Math.max(0.6, prev); s <= front; s += 0.5) {
                Vec3 p = lineOrigin.add(dir.scale(s));
                level.sendParticles(ParticleTypes.SQUID_INK, p.x, y0 + 0.3, p.z, 2, 0.4, 0.1, 0.4, 0.01);
                level.sendParticles(ParticleTypes.END_ROD, p.x, y0 + 0.2, p.z, 1, 0.3, 0.6, 0.3, 0.02);
                if (((int) (s * 2)) % 2 == 0) level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, y0 + 0.9, p.z, 1, 0, 0, 0, 0);
                if (grief) {
                    for (double w = -ArchivistPattern.ERASE_HALF_WIDTH + 0.1; w <= ArchivistPattern.ERASE_HALF_WIDTH; w += 0.5) {
                        for (int h = 0; h < 4; h++) eraseBlock(level, BlockPos.containing(p.x + side.x * w, y0 + h, p.z + side.z * w));
                    }
                }
            }
            AABB band = new AABB(lineOrigin.add(dir.scale(prev)), lineOrigin.add(dir.scale(front))).inflate(ArchivistPattern.ERASE_HALF_WIDTH + 0.6, 0, ArchivistPattern.ERASE_HALF_WIDTH + 0.6)
                    .expandTowards(0, 3.5, 0).move(0, -0.5, 0);
            for (Projectile proj : level.getEntitiesOfClass(Projectile.class, band)) {
                if (!ArchivistPattern.inBand(lineOrigin.x, lineOrigin.z, yaw, proj.getX(), proj.getZ())) continue;
                stats.erasedProjectiles++;
                if (proj instanceof ArcaneBolt b) b.erase();
                else {
                    level.sendParticles(ParticleTypes.SQUID_INK, proj.getX(), proj.getY(), proj.getZ(), 6, 0.1, 0.1, 0.1, 0.02);
                    proj.discard();
                }
            }
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, band, this::isEnemy)) {
                if (!ArchivistPattern.inBand(lineOrigin.x, lineOrigin.z, yaw, victim.getX(), victim.getZ())) continue;
                if (Math.abs(victim.getY() - y0) > 3.0 || !hitThisMove.add(victim.getUUID())) continue;
                if (victim.hurtServer(level, damageSources().mobAttack(this), 12.0F)) stats.erasureHits++;
                // Erasure strikes out one line of you: the longest-lasting beneficial effect, not all of them
                MobEffectInstance longest = null;
                for (MobEffectInstance fx : victim.getActiveEffects()) {
                    if (!fx.getEffect().value().isBeneficial()) continue;
                    if (longest == null || fx.isInfiniteDuration() || (!longest.isInfiniteDuration() && fx.getDuration() > longest.getDuration())) longest = fx;
                }
                if (longest != null) victim.removeEffect(longest.getEffect());
                double sideSign = Math.signum((victim.getX() - lineOrigin.x) * side.x + (victim.getZ() - lineOrigin.z) * side.z);
                shove(victim, side.scale(sideSign == 0 ? 1 : sideSign).scale(0.7).add(0, 0.35, 0));
                level.sendParticles(ParticleTypes.SQUID_INK, victim.getX(), victim.getY() + 1, victim.getZ(), 20, 0.3, 0.6, 0.3, 0.05);
            }
        }
    }

    /** Clears a block the Library did not put there: player towers, cover and clutter. Library blocks stay. */
    private void eraseBlock(ServerLevel level, BlockPos pos) {
        if (!level.dimension().equals(PortalTravel.LIBRARY) || home == null) return;
        if (Math.abs(pos.getX() - home.getX()) > 8 || Math.abs(pos.getZ() - home.getZ()) > 8) return;
        var state = level.getBlockState(pos);
        if (state.isAir() || level.getBlockEntity(pos) != null) return;
        if (layout == null) layout = new LibraryLayout(level.getSeed());
        if (layout.blockAt(pos.getX(), pos.getY(), pos.getZ()) != Mat.AIR) return;
        if (level.destroyBlock(pos, false, this)) stats.erasedBlocks++;
    }

    private void recallAct(ServerLevel level, @Nullable LivingEntity target, int t) {
        if (t == 0) {
            if (target == null || target.distanceTo(this) > ArchivistPattern.RECALL_RANGE || !hasLineOfSight(target)) {
                Vec3 from = tomePoint();
                Vec3 mid = target == null ? from : from.add(target.position().add(0, 1, 0)).scale(0.5);
                level.sendParticles(ParticleTypes.CRIT, mid.x, mid.y, mid.z, 30, 0.5, 0.5, 0.5, 0.4);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, from.x, from.y, from.z, 12, 0.3, 0.3, 0.3, 0.02);
                sound(ModSounds.ARCHIVIST_TETHER_SNAP.get(), 2.0F, 0.9F);
                stats.snaps++;
                startMove(Move.STAGGER, target);
                return;
            }
            stats.pulls++;
            sound(ModSounds.ARCHIVIST_RECALL_PULL.get(), 2.0F, 0.8F);
            pullDest = pullDestination(level, target);
        }
        if (target == null) return;
        int left = ArchivistPattern.PULL_TICKS - t;
        Vec3 now = target.position();
        Vec3 next = now.add(pullDest.subtract(now).scale(1.0 / Math.max(1, left)));
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, now.x, now.y + 1, now.z, 10, 0.3, 0.5, 0.3, 0.05);
        target.teleportTo(next.x, next.y, next.z);
        target.setDeltaMovement(Vec3.ZERO);
        target.resetFallDistance();
        if (t == ArchivistPattern.PULL_TICKS - 1) startMove(Move.SLAM, target);
    }

    private Vec3 pullDestination(ServerLevel level, LivingEntity target) {
        Vec3 dir = new Vec3(target.getX() - getX(), 0, target.getZ() - getZ());
        dir = dir.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0, getYRot()) : dir.normalize();
        double y = groundY();
        for (double d = 2.4; d < 8; d += 0.5) {
            Vec3 p = new Vec3(getX() + dir.x * d, y, getZ() + dir.z * d);
            AABB box = target.getBoundingBox().move(p.x - target.getX(), p.y - target.getY(), p.z - target.getZ());
            if (level.noCollision(target, box)) return p;
        }
        return target.position();
    }

    private void slamAct(ServerLevel level) {
        sound(ModSounds.ARCHIVIST_SLAM.get(), 2.0F, 0.8F);
        double y = groundY();
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI * 2 / 16;
            double r = ArchivistPattern.SLAM_RADIUS * 0.7;
            level.sendParticles(ParticleTypes.EXPLOSION, getX() + Math.cos(a) * r, y + 0.4, getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
        level.sendParticles(ParticleTypes.SQUID_INK, getX(), y + 0.3, getZ(), 60, 2.5, 0.1, 2.5, 0.05);
        AABB area = new AABB(getX(), y, getZ(), getX(), y, getZ()).inflate(ArchivistPattern.SLAM_RADIUS, 0, ArchivistPattern.SLAM_RADIUS).expandTowards(0, 3, 0).move(0, -0.5, 0);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area, this::isEnemy)) {
            if (horizontalDistance(victim) > ArchivistPattern.SLAM_RADIUS) continue;
            if (victim.hurtServer(level, damageSources().mobAttack(this), 12.0F)) stats.slamHits++;
            Vec3 out = new Vec3(victim.getX() - getX(), 0, victim.getZ() - getZ());
            out = out.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : out.normalize();
            shove(victim, out.scale(1.1).add(0, 0.45, 0));
            victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), this);
        }
    }

    private void pickSockets(int phase, @Nullable LivingEntity target) {
        sockets.clear();
        if (home == null) return;
        List<BlockPos> all = new ArrayList<>();
        for (int[] o : new int[][] {{5, 5}, {-5, 5}, {5, -5}, {-5, -5}}) all.add(home.offset(o[0], 0, o[1]));
        if (target != null) all.sort((a, b) -> Double.compare(b.distToCenterSqr(target.position()), a.distToCenterSqr(target.position())));
        for (int i = 0; i < ArchivistPattern.wardsFor(phase) && i < all.size(); i++) sockets.add(all.get(i));
    }

    private void indexAct(ServerLevel level, @Nullable LivingEntity target) {
        sound(SoundEvents.EVOKER_CAST_SPELL, 2.0F, 0.6F);
        for (BlockPos s : sockets) {
            VaultWard ward = ModEntities.VAULT_WARD.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            if (ward == null) continue;
            ward.snapTo(s.getX() + 0.5, s.getY() + 1.2, s.getZ() + 0.5, random.nextFloat() * 360F, 0);
            ward.finalizeSpawn(level, level.getCurrentDifficultyAt(s), EntitySpawnReason.MOB_SUMMONED, null);
            ward.fadeAfter(1200);
            if (target != null) ward.setTarget(target);
            level.addFreshEntity(ward);
            wards.add(ward.getUUID());
            stats.wardsSummoned++;
            level.sendParticles(ParticleTypes.ENCHANT, ward.getX(), ward.getY() + 0.6, ward.getZ(), 40, 0.4, 0.6, 0.4, 1.0);
            level.sendParticles(ParticleTypes.POOF, ward.getX(), ward.getY() + 0.6, ward.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
        }
    }

    private void tickWards(ServerLevel level) {
        List<Entity> live = liveWards();
        wards.removeIf(id -> {
            Entity e = level.getEntity(id);
            return e == null || !e.isAlive();
        });
        boolean warded = !live.isEmpty();
        setFlag(FLAG_WARDED, warded);
        if (warded && tickCount % 4 == 0) {
            Vec3 c = chest();
            for (Entity w : live) {
                Vec3 from = w.position().add(0, 0.7, 0);
                Vec3 d = c.subtract(from);
                double len = d.length();
                for (double k = 0; k < len; k += 0.7) {
                    Vec3 p = from.add(d.scale(k / len));
                    particle(level, WARD_BEAM, p.x, p.y, p.z);
                }
            }
            level.sendParticles(ParticleTypes.ENCHANT, c.x, c.y, c.z, 6, 0.8, 1.0, 0.8, 0.4);
        }
    }

    // ---- phases --------------------------------------------------------------------------------------------------

    private void beginTransition(int toPhase) {
        sound(ModSounds.ARCHIVIST_PHASE.get(), 3.0F, toPhase >= 3 ? 0.7F : 1.0F);
        if (!(level() instanceof ServerLevel level)) return;
        Component line = Component.translatable(toPhase >= 3 ? "message.arcane.archivist.phase3" : "message.arcane.archivist.phase2").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC);
        for (ServerPlayer p : bossEvent.getPlayers()) p.sendOverlayMessage(line);
        // a new chapter: every bolt in flight is turned to ink, and anyone close is pushed back
        for (ArcaneBolt b : level.getEntitiesOfClass(ArcaneBolt.class, getBoundingBox().inflate(24))) b.erase();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(6, 2, 6), this::isEnemy)) {
            Vec3 out = new Vec3(e.getX() - getX(), 0, e.getZ() - getZ());
            out = out.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : out.normalize();
            shove(e, out.scale(1.0).add(0, 0.4, 0));
        }
    }

    private void finishTransition() {
        phaseReached = Math.min(3, phaseReached + 1);
        stats.transitions++;
        entityData.set(DATA_PHASE, (byte) phaseReached);
        cooldowns.clear();
        LivingEntity target = getTarget();
        if (phaseReached == 2) {
            startMove(Move.INDEX, target);
        } else if (phaseReached >= 3) {
            setFlag(FLAG_HOVER, true);
            setNoGravity(true);
            startMove(Move.STORM, target);
        }
    }

    private void hover() {
        setNoGravity(true);
        getNavigation().stop();
        if (home == null) return;
        Vec3 want = new Vec3(home.getX() + 0.5, home.getY() + HOVER_HEIGHT + Math.sin(tickCount * 0.08) * 0.12, home.getZ() + 0.5);
        setDeltaMovement(want.subtract(position()).scale(0.25));
    }

    // ---- helpers -------------------------------------------------------------------------------------------------

    private boolean isEnemy(LivingEntity e) {
        if (e == this || e instanceof LibraryDweller || !e.isAlive()) return false;
        return !(e instanceof Player p) || !(p.isCreative() || p.isSpectator());
    }

    private static void shove(LivingEntity e, Vec3 impulse) {
        e.setDeltaMovement(e.getDeltaMovement().add(impulse));
        e.needsSync = true;
        if (e instanceof ServerPlayer sp) sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
    }

    private void particle(ServerLevel level, ParticleOptions p, double x, double y, double z) {
        level.sendParticles(p, true, true, x, y, z, 1, 0, 0, 0, 0);
    }

    private void sound(SoundEvent sound, float volume, float pitch) {
        playSound(sound, volume, pitch);
    }

    private void updateBossBar(ServerLevel level) {
        bossEvent.setProgress(getHealth() / getMaxHealth());
        bossEvent.setColor(isWarded() ? BossEvent.BossBarColor.BLUE : phaseReached >= 3 ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.PURPLE);
        if (tickCount % 10 == 0) {
            bossEvent.setName(Component.translatable(phaseReached >= 3 ? "boss.arcane.archivist.phase3"
                    : phaseReached == 2 ? "boss.arcane.archivist.phase2" : "boss.arcane.archivist.phase1", getDisplayName()));
            for (ServerPlayer stale : new ArrayList<>(bossEvent.getPlayers())) {
                if (stale.isRemoved() || !stale.isAlive() || stale.level() != level) bossEvent.removePlayer(stale);
            }
            for (ServerPlayer player : level.players()) {
                boolean near = player.isAlive() && !player.isSpectator() && distanceToSqr(player) < 40.0 * 40.0
                        && (isFighting() || distanceToSqr(player) < 14.0 * 14.0);
                if (near) bossEvent.addPlayer(player); else bossEvent.removePlayer(player);
            }
        }
    }

    // ---- damage, death, persistence ------------------------------------------------------------------------------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (move == Move.TRANSITION && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        if (isWarded()) amount *= 0.25F;
        if (move == Move.STAGGER) amount *= 1.5F;
        // the fight cannot be skipped: until the Final Chapter, no blow takes the last point of health
        if (phaseReached < 3 && amount >= getHealth() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) amount = Math.max(0, getHealth() - 1.0F);
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC) || source.is(DamageTypes.WITHER)) return true;
        return super.isInvulnerableTo(level, source);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel server) {
            for (Entity w : liveWards()) w.discard();
            for (ServerPlayer player : server.getEntitiesOfClass(ServerPlayer.class, new AABB(blockPosition()).inflate(48.0))) {
                ModTriggers.ARCANE.get().trigger(player, "slain_archivist");
            }
            server.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + 1.5, getZ(), 200, 1.0, 1.5, 1.0, 2.0);
            bossEvent.removeAllPlayers();
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        bossEvent.removeAllPlayers();
        super.remove(reason);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Phase", phaseReached);
        if (home != null) {
            output.putInt("HomeX", home.getX());
            output.putInt("HomeY", home.getY());
            output.putInt("HomeZ", home.getZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        phaseReached = Mth.clamp(input.getIntOr("Phase", 1), 1, 3);
        entityData.set(DATA_PHASE, (byte) phaseReached);
        if (input.getIntOr("HomeY", Integer.MIN_VALUE) != Integer.MIN_VALUE) {
            home = new BlockPos(input.getIntOr("HomeX", 0), input.getIntOr("HomeY", 0), input.getIntOr("HomeZ", 0));
            setHomeTo(home, 9);
        }
        // a reload ends any move in progress; phase 3 resumes hovering
        move = Move.NONE;
        entityData.set(DATA_MOVE, (byte) 0);
        setFlag(FLAG_HOVER, phaseReached >= 3);
        setNoGravity(phaseReached >= 3);
    }

    @Override
    public boolean removeWhenFarAway(double distanceSqr) {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean ignorePassenger) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        bossEvent.setName(getDisplayName());
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return move == Move.NONE ? ModSounds.ARCHIVIST_AMBIENT.get() : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITHER_HURT;
    }

    @Override
    public float getVoicePitch() {
        return 1.3F;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ARCHIVIST_DEATH.get();
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof LibraryDweller) && super.canAttack(target);
    }
}
