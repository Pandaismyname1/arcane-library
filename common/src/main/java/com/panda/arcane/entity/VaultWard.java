package com.panda.arcane.entity;

import com.panda.arcane.registry.ModSounds;
import com.panda.arcane.worldgen.LibraryLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A floating stone reliquary with a living gem for a heart (Creatures A). It never moves; it watches and fires bolts.
 * Variants:
 * <ul>
 *   <li>Sentinel: links a beam to the nearest fellow guardian and shields it (Resistance II) while both live.</li>
 *   <li>Bulwark: heavy shield plates. Arrows and other projectiles bounce straight back at whoever fired them.</li>
 *   <li>Seeker (more common deep in): fewer hits, but its bolts turn to follow you.</li>
 *   <li>Warden of Keys: appears only at the heart of a vault. Every chest and barrel within eight blocks stays sealed
 *       until it is destroyed.</li>
 * </ul>
 */
public class VaultWard extends Monster implements RangedAttackMob, LibraryDweller, CreatureVisuals {
    public enum Variant {
        SENTINEL(55, 25), BULWARK(30, 30), SEEKER(15, 45), KEYS(0, 0);

        final int normalWeight, restrictedWeight;

        Variant(int normalWeight, int restrictedWeight) {
            this.normalWeight = normalWeight;
            this.restrictedWeight = restrictedWeight;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final double LOCK_RADIUS = 8.0;
    public static final double LINK_RADIUS = 10.0;
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(VaultWard.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LINK = SynchedEntityData.defineId(VaultWard.class, EntityDataSerializers.INT);

    /** Ticks until a summoned ward fades. Zero means permanent. */
    private int lifetime;
    private int attackAnim;
    private final List<ArcaneBolt> seeking = new ArrayList<>();

    public VaultWard(EntityType<? extends VaultWard> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 18.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 18.0);
    }

    public VaultWard fadeAfter(int ticks) {
        this.lifetime = ticks;
        return this;
    }

    // ---- variant ------------------------------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, 0);
        builder.define(DATA_LINK, -1);
    }

    public Variant variant() {
        return Variant.values()[Math.floorMod(entityData.get(DATA_VARIANT), Variant.values().length)];
    }

    public VaultWard setVariant(Variant variant) {
        entityData.set(DATA_VARIANT, variant.ordinal());
        double health = switch (variant) {
            case BULWARK -> 30.0;
            case KEYS -> 40.0;
            case SEEKER -> 16.0;
            default -> 18.0;
        };
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        getAttribute(Attributes.ARMOR).setBaseValue(variant == Variant.BULWARK ? 8.0 : variant == Variant.KEYS ? 6.0 : 2.0);
        if (getHealth() > health || tickCount == 0) setHealth((float) health);
        return this;
    }

    @Override
    public int variantIndex() {
        return variant().ordinal();
    }

    @Override
    public float attackProgress(float partialTick) {
        return attackAnim <= 0 ? 0.0F : Math.max(0.0F, attackAnim - partialTick) / 10.0F;
    }

    @Override
    public boolean visualFlag() {
        return entityData.get(DATA_LINK) >= 0;
    }

    public static Variant pickVariant(CreatureZones zone, RandomSource random) {
        if (zone.in(LibraryLayout.Kind.VAULT) && zone.central()) return Variant.KEYS;
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
        output.putInt("Lifetime", lifetime);
        output.putString("Variant", variant().id());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        lifetime = input.getIntOr("Lifetime", 0);
        entityData.set(DATA_VARIANT, CreatureZones.byName(Variant.values(), input.getStringOr("Variant", "sentinel")).ordinal());
    }

    // ---- behaviour ----------------------------------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new RangedAttackGoal(this, 0.0, 30, 46, 18.0F) {
            @Override
            public boolean canUse() {
                return super.canUse();
            }
        });
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (attackAnim > 0) attackAnim--;
        if (level().isClientSide() && variant() == Variant.KEYS && random.nextInt(6) == 0) {
            level().addParticle(ParticleTypes.WAX_ON, getRandomX(0.6), getY() + random.nextDouble() * 1.2, getRandomZ(0.6), 0, 0.02, 0);
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (lifetime > 0 && --lifetime == 0) {
            level.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.6, getZ(), 12, 0.3, 0.3, 0.3, 0.02);
            discard();
            return;
        }
        switch (variant()) {
            case SENTINEL -> tickLink(level);
            case SEEKER -> steerBolts();
            case KEYS -> {
                if (tickCount % 20 == 0) {
                    for (BlockPos pos : lockedContainers(level)) {
                        level.sendParticles(ParticleTypes.WAX_ON, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 1, 0.2, 0.0, 0.2, 0.0);
                    }
                }
            }
            default -> { }
        }
    }

    /** Sentinel: picks the nearest fellow guardian within reach and keeps it shielded. Returns the link, if any. */
    public @Nullable LivingEntity tickLink(ServerLevel level) {
        LivingEntity link = null;
        double best = LINK_RADIUS * LINK_RADIUS;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(LINK_RADIUS))) {
            if (mob == this || !(mob instanceof LibraryDweller) || mob instanceof VaultWard w && w.variant() == Variant.SENTINEL) continue;
            // the Archivist has its own ward shield (the Index); a tether on top would outlive the wards and double it
            if (!mob.isAlive() || mob instanceof LumenMoth || mob instanceof Archivist) continue;
            double d = mob.distanceToSqr(this);
            if (d < best) {
                best = d;
                link = mob;
            }
        }
        entityData.set(DATA_LINK, link == null ? -1 : link.getId());
        if (link != null) {
            link.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 30, 1, true, false), this);
            if (tickCount % 3 == 0) beam(level, link);
        }
        return link;
    }

    private void beam(ServerLevel level, LivingEntity to) {
        Vec3 a = new Vec3(getX(), getY() + 0.75, getZ());
        Vec3 b = to.position().add(0, to.getBbHeight() * 0.6, 0);
        int steps = (int) Math.ceil(a.distanceTo(b) * 2);
        DustParticleOptions dust = new DustParticleOptions(0xC48CFF, 0.8F);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = a.lerp(b, i / (double) Math.max(1, steps));
            level.sendParticles(dust, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** Seeker: each tracked bolt turns a little toward the target every tick for two seconds. */
    private void steerBolts() {
        LivingEntity target = getTarget();
        seeking.removeIf(b -> b.isRemoved() || b.tickCount > 50);
        if (target == null) return;
        for (ArcaneBolt bolt : seeking) {
            Vec3 v = bolt.getDeltaMovement();
            double speed = Math.max(0.35, v.length());
            Vec3 want = target.getEyePosition().subtract(bolt.position()).normalize().scale(speed);
            bolt.setDeltaMovement(v.scale(0.82).add(want.scale(0.18)).normalize().scale(speed));
            bolt.needsSync = true;
        }
    }

    public int seekingBolts() {
        return seeking.size();
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        level().broadcastEntityEvent(this, (byte) 4);
        attackAnim = 10;
        Vec3 start = new Vec3(getX(), getY() + 0.75, getZ());
        Vec3 aim = target.getEyePosition().subtract(start);
        ArcaneBolt bolt = new ArcaneBolt(level(), this, aim.normalize());
        switch (variant()) {
            case SEEKER -> {
                bolt.damage(3.0F).effect(MobEffects.GLOWING, 100);
                seeking.add(bolt);
            }
            case BULWARK -> bolt.damage(5.0F).effect(MobEffects.SLOWNESS, 60);
            case KEYS -> bolt.damage(4.0F).effect(MobEffects.MINING_FATIGUE, 200);
            default -> bolt.damage(4.0F).effect(MobEffects.SLOWNESS, 60);
        }
        bolt.setPos(start.x, start.y, start.z);
        level().addFreshEntity(bolt);
        playSound(ModSounds.WARD_ZAP.get(), 1.0F, variant() == Variant.SEEKER ? 1.4F : 1.0F);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) attackAnim = 10;
        else super.handleEntityEvent(id);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (variant() == Variant.BULWARK && source.getDirectEntity() instanceof Projectile projectile && projectile.getOwner() != this) {
            reflect(level, projectile);
            return false;
        }
        return super.hurtServer(level, source, damage);
    }

    /** Bulwark: sends a projectile back the way it came, now owned by the ward. */
    public void reflect(ServerLevel level, Projectile projectile) {
        Entity shooter = projectile.getOwner();
        Vec3 back = shooter != null ? shooter.getEyePosition().subtract(projectile.position()).normalize()
                : projectile.getDeltaMovement().scale(-1).normalize();
        double speed = Math.max(0.6, projectile.getDeltaMovement().length());
        projectile.setOwner(this);
        projectile.setDeltaMovement(back.scale(speed));
        projectile.needsSync = true;
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, projectile.getX(), projectile.getY(), projectile.getZ(), 12, 0.2, 0.2, 0.2, 0.3);
        playSound(SoundEvents.SHIELD_BLOCK.value(), 1.0F, 1.3F);
    }

    // ---- Warden of Keys -----------------------------------------------------------------------------------------

    /** The containers this warden is keeping shut. */
    public List<BlockPos> lockedContainers(ServerLevel level) {
        List<BlockPos> out = new ArrayList<>();
        BlockPos origin = blockPosition();
        int r = (int) LOCK_RADIUS;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -4, -r), origin.offset(r, 4, r))) {
            if (level.getBlockEntity(pos) instanceof net.minecraft.world.Container && pos.distSqr(origin) <= LOCK_RADIUS * LOCK_RADIUS) {
                out.add(pos.immutable());
            }
        }
        return out;
    }

    /** True while a living Warden of Keys is within reach of this block. */
    public static boolean isLocked(Level level, BlockPos pos) {
        AABB box = new AABB(pos).inflate(LOCK_RADIUS);
        for (VaultWard ward : level.getEntitiesOfClass(VaultWard.class, box)) {
            if (ward.isAlive() && ward.variant() == Variant.KEYS && ward.blockPosition().distSqr(pos) <= LOCK_RADIUS * LOCK_RADIUS) return true;
        }
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean ignorePassenger) {
        return false;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AMETHYST_CLUSTER_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return variant() == Variant.KEYS ? SoundEvents.CHEST_LOCKED : SoundEvents.AMETHYST_CLUSTER_BREAK;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof LibraryDweller) && super.canAttack(target);
    }
}
