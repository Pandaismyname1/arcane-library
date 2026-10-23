package com.panda.arcane.entity;

import com.panda.arcane.registry.ModSounds;
import com.panda.arcane.worldgen.LibraryLayout;
import java.util.EnumSet;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
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
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Wanders the stacks in silence; calm until a chest is opened nearby or it is struck (Creatures A). Variants:
 * <ul>
 *   <li>Shusher: raises a finger (a one-second tell), then hushes everyone close: no sprinting, slowed, weakened.</li>
 *   <li>Archivist's Clerk: no hexes. It closes in, lifts one stack from your pack and runs; kill it to get it back,
 *       or it files it away in the nearest chest.</li>
 *   <li>Censor: hexes that blind; when struck it vanishes into a cloud of blinding ink.</li>
 *   <li>Head Librarian (Restricted Section): calls every librarian in earshot to its target and shields them.</li>
 * </ul>
 */
public class SilentLibrarian extends Monster implements RangedAttackMob, LibraryDweller, CreatureVisuals {
    public enum Variant {
        SHUSHER(60, 25), CLERK(30, 15), CENSOR(10, 35), HEAD(0, 25);

        final int normalWeight, restrictedWeight;

        Variant(int normalWeight, int restrictedWeight) {
            this.normalWeight = normalWeight;
            this.restrictedWeight = restrictedWeight;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** Length of the Shusher's raised-finger tell, in ticks (the model raises its arm over exactly this time). */
    public static final int HUSH_TELL = 20;
    public static final double HUSH_RADIUS = 10.0;
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(SilentLibrarian.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TELL = SynchedEntityData.defineId(SilentLibrarian.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_CARRYING = SynchedEntityData.defineId(SilentLibrarian.class, EntityDataSerializers.BOOLEAN);

    private int angerTicks;
    private int hushCooldown = 60;
    private int inkCooldown;
    private int rallyCooldown;
    private int stealCooldown;
    private int fleeTicks;
    private ItemStack stolen = ItemStack.EMPTY;
    private int attackAnim;

    public SilentLibrarian(EntityType<? extends SilentLibrarian> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 26.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    // ---- variant ------------------------------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, 0);
        builder.define(DATA_TELL, 0);
        builder.define(DATA_CARRYING, false);
    }

    public Variant variant() {
        return Variant.values()[Math.floorMod(entityData.get(DATA_VARIANT), Variant.values().length)];
    }

    public void setVariant(Variant variant) {
        entityData.set(DATA_VARIANT, variant.ordinal());
        applyStats(variant);
    }

    private void applyStats(Variant variant) {
        double health = variant == Variant.HEAD ? 40.0 : variant == Variant.CLERK ? 20.0 : 26.0;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(variant == Variant.CLERK ? 0.3 : 0.25);
        getAttribute(Attributes.SCALE).setBaseValue(variant == Variant.HEAD ? 1.15 : 1.0);
        if (getHealth() > health || tickCount == 0) setHealth((float) health);
    }

    @Override
    public int variantIndex() {
        return variant().ordinal();
    }

    @Override
    public float actionProgress(float partialTick) {
        int tell = entityData.get(DATA_TELL);
        return tell <= 0 ? 0.0F : 1.0F - Math.max(0.0F, tell - partialTick) / HUSH_TELL;
    }

    @Override
    public float attackProgress(float partialTick) {
        return attackAnim <= 0 ? 0.0F : Math.max(0.0F, attackAnim - partialTick) / 10.0F;
    }

    @Override
    public boolean visualFlag() {
        return entityData.get(DATA_CARRYING);
    }

    public static Variant pickVariant(CreatureZones zone, RandomSource random) {
        if (zone.restricted() && zone.in(LibraryLayout.Kind.STUDY) && random.nextInt(3) == 0) return Variant.HEAD;
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
        output.putInt("Anger", angerTicks);
        if (!stolen.isEmpty()) output.store("Stolen", ItemStack.CODEC, stolen);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(DATA_VARIANT, CreatureZones.byName(Variant.values(), input.getStringOr("Variant", "shusher")).ordinal());
        angerTicks = input.getIntOr("Anger", 0);
        stolen = input.read("Stolen", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        entityData.set(DATA_CARRYING, !stolen.isEmpty());
        entityData.set(DATA_TELL, 0);
    }

    // ---- goals --------------------------------------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new FleeWithLootGoal());
        goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 45, 14.0F) {
            @Override
            public boolean canUse() {
                return variant() != Variant.CLERK && entityData.get(DATA_TELL) <= 0 && super.canUse();
            }
        });
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true) {
            @Override
            public boolean canUse() {
                return variant() == Variant.CLERK && stolen.isEmpty() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return variant() == Variant.CLERK && stolen.isEmpty() && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, (target, level) -> angerTicks > 0));
    }

    /** How far a librarian notices an intruder it can see. */
    public static final double NOTICE_RANGE = 12.0;

    public boolean isAngry() {
        return angerTicks > 0;
    }

    /** Called when something disturbs the library near this keeper. A Head Librarian passes the alarm on. */
    public void provoke(LivingEntity cause, int ticks) {
        boolean wasCalm = angerTicks <= 0;
        if (wasCalm && level() instanceof ServerLevel server) {
            playSound(ModSounds.LIBRARIAN_HUSH.get(), 1.0F, 0.8F);
            server.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getEyeY() + 0.3, getZ(), 3, 0.2, 0.1, 0.2, 0.0);
        }
        angerTicks = Math.max(angerTicks, ticks);
        setTarget(cause);
        if (wasCalm && variant() == Variant.HEAD) rally(cause);
    }

    /** Head Librarian: every librarian within 24 blocks takes up the hunt. Returns how many answered. */
    public int rally(LivingEntity cause) {
        int answered = 0;
        if (!(level() instanceof ServerLevel server)) return 0;
        for (SilentLibrarian other : server.getEntitiesOfClass(SilentLibrarian.class, getBoundingBox().inflate(24.0))) {
            if (other == this) continue;
            if (other.angerTicks <= 0) {
                other.angerTicks = 600;
                other.setTarget(cause);
                answered++;
                server.sendParticles(ParticleTypes.NOTE, other.getX(), other.getEyeY() + 0.6, other.getZ(), 1, 0, 0, 0, 0.5);
            }
        }
        playSound(SoundEvents.BELL_BLOCK, 1.2F, 0.6F);
        server.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getEyeY(), getZ(), 1, 0, 0, 0, 0);
        return answered;
    }

    // ---- per-tick mechanics -------------------------------------------------------------------------------------

    @Override
    public void aiStep() {
        super.aiStep();
        if (attackAnim > 0) attackAnim--;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (angerTicks > 0 && --angerTicks == 0 && stolen.isEmpty()) setTarget(null);
        // keepers of the stacks: a player they can see within 12 blocks is an intruder (hush, then the attack)
        if (angerTicks <= 0 && tickCount % 10 == 0) {
            Player intruder = level.getNearestPlayer(net.minecraft.world.entity.ai.targeting.TargetingConditions.forCombat().range(NOTICE_RANGE), this);
            if (intruder != null) provoke(intruder, 600);
        }
        if (inkCooldown > 0) inkCooldown--;
        if (stealCooldown > 0) stealCooldown--;
        LivingEntity target = getTarget();
        switch (variant()) {
            case SHUSHER -> tickHush(level, target);
            case HEAD -> {
                if (target != null && --rallyCooldown <= 0) {
                    rallyCooldown = 160;
                    for (SilentLibrarian ally : level.getEntitiesOfClass(SilentLibrarian.class, getBoundingBox().inflate(12.0))) {
                        ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 0), this);
                        ally.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 0), this);
                        level.sendParticles(ParticleTypes.ENCHANT, ally.getX(), ally.getY() + 1.2, ally.getZ(), 12, 0.3, 0.6, 0.3, 0.4);
                    }
                }
            }
            case CLERK -> tickClerk(level);
            default -> { }
        }
    }

    private void tickHush(ServerLevel level, @Nullable LivingEntity target) {
        int tell = entityData.get(DATA_TELL);
        if (tell > 0) {
            getNavigation().stop();
            if (target != null) getLookControl().setLookAt(target, 30.0F, 30.0F);
            entityData.set(DATA_TELL, --tell);
            if (tell % 5 == 0) level.sendParticles(ParticleTypes.WHITE_SMOKE, getX(), getEyeY(), getZ(), 2, 0.15, 0.05, 0.15, 0.0);
            if (tell == 0) hush(level);
            return;
        }
        if (--hushCooldown <= 0 && target != null && distanceToSqr(target) < HUSH_RADIUS * HUSH_RADIUS) startHush();
    }

    /** Starts the Shusher's tell. Public as a test hook. */
    public void startHush() {
        entityData.set(DATA_TELL, HUSH_TELL);
        hushCooldown = 160;
        playSound(ModSounds.LIBRARIAN_HUSH.get(), 1.2F, 1.1F);
    }

    /** Holds the tell at a given number of ticks left (screenshots and tests; the AI counts it down normally). */
    public void poseTell(int ticksLeft) {
        entityData.set(DATA_TELL, Math.max(0, Math.min(HUSH_TELL, ticksLeft)));
    }

    /** The hush itself: everyone near stops sprinting and is slowed and weakened. Returns how many were hushed. */
    public int hush(ServerLevel level) {
        int n = 0;
        for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(HUSH_RADIUS))) {
            if (p.isSpectator() || p.isCreative()) continue;
            p.setSprinting(false);
            p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1), this);
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), this);
            n++;
        }
        level.playSound(null, blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, getSoundSource(), 0.5F, 2.0F);
        level.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getEyeY(), getZ(), 1, 0, 0, 0, 0);
        return n;
    }

    private void tickClerk(ServerLevel level) {
        if (stolen.isEmpty()) return;
        if (fleeTicks > 0 && --fleeTicks == 0) {
            if (fileAway(level)) {
                setTarget(null);
                angerTicks = 0;
            } else {
                fleeTicks = 200;
            }
        }
    }

    /** Clerk: puts the stolen stack into the nearest container within 12 blocks. True when it found room. */
    public boolean fileAway(ServerLevel level) {
        BlockPos origin = blockPosition();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-12, -3, -12), origin.offset(12, 3, 12))) {
            if (level.getBlockEntity(pos) instanceof Container) {
                double d = pos.distSqr(origin);
                if (d < bestD) {
                    bestD = d;
                    best = pos.immutable();
                }
            }
        }
        if (best == null || !(level.getBlockEntity(best) instanceof Container container)) return false;
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).isEmpty()) {
                container.setItem(i, stolen);
                container.setChanged();
                stolen = ItemStack.EMPTY;
                entityData.set(DATA_CARRYING, false);
                level.playSound(null, best, SoundEvents.BOOK_PUT, getSoundSource(), 1.0F, 0.8F);
                level.sendParticles(ParticleTypes.ENCHANT, best.getX() + 0.5, best.getY() + 1.0, best.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.5);
                return true;
            }
        }
        return false;
    }

    public ItemStack stolen() {
        return stolen;
    }

    /** Clerk: lifts one stack out of the player's pack (never armour, never the held item). True when it took one. */
    public boolean steal(Player player) {
        if (!stolen.isEmpty() || stealCooldown > 0 || player.isCreative()) return false;
        var items = player.getInventory().getNonEquipmentItems();
        int selected = player.getInventory().getSelectedSlot();
        int count = 0;
        for (int i = 0; i < items.size(); i++) if (i != selected && !items.get(i).isEmpty()) count++;
        if (count == 0) return false;
        int pick = random.nextInt(count);
        for (int i = 0; i < items.size(); i++) {
            if (i == selected || items.get(i).isEmpty()) continue;
            if (pick-- == 0) {
                stolen = items.get(i).copy();
                items.set(i, ItemStack.EMPTY);
                break;
            }
        }
        player.getInventory().setChanged();
        entityData.set(DATA_CARRYING, true);
        fleeTicks = 600;
        stealCooldown = 400;
        playSound(SoundEvents.ITEM_PICKUP, 1.0F, 0.6F);
        playSound(ModSounds.LIBRARIAN_HUSH.get(), 0.8F, 1.6F);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.WITCH, getX(), getY() + 1.2, getZ(), 10, 0.3, 0.3, 0.3, 0.02);
        }
        return true;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        level.broadcastEntityEvent(this, (byte) 4);
        attackAnim = 10;
        boolean hit = super.doHurtTarget(level, target);
        if (variant() == Variant.CLERK && target instanceof Player player) steal(player);
        return hit;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) attackAnim = 10;
        else super.handleEntityEvent(id);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && variant() == Variant.CENSOR && inkCooldown <= 0 && isAlive()) inkCloud(level);
        return hurt;
    }

    /** Censor: a blinding ink cloud where it stood, and it fades from sight for two seconds. */
    public void inkCloud(ServerLevel level) {
        inkCooldown = 200;
        AreaEffectCloud cloud = new AreaEffectCloud(level, getX(), getY(), getZ());
        cloud.setOwner(this);
        cloud.setRadius(3.0F);
        cloud.setRadiusPerTick(-0.02F);
        cloud.setDuration(80);
        cloud.setWaitTime(0);
        cloud.setCustomParticle(ParticleTypes.SQUID_INK);
        cloud.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
        level.addFreshEntity(cloud);
        level.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1.0, getZ(), 40, 0.6, 0.6, 0.6, 0.05);
        addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false));
        playSound(SoundEvents.SQUID_SQUIRT, 1.0F, 0.7F);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        level().broadcastEntityEvent(this, (byte) 4);
        attackAnim = 10;
        Vec3 start = new Vec3(getX(), getEyeY() - 0.3, getZ());
        Vec3 aim = target.getEyePosition().subtract(start);
        ArcaneBolt bolt = new ArcaneBolt(level(), this, aim.normalize());
        switch (variant()) {
            case CENSOR -> bolt.damage(3.0F).effect(MobEffects.BLINDNESS, 60);
            case HEAD -> bolt.damage(4.5F).effect(MobEffects.WEAKNESS, 140);
            default -> bolt.damage(3.0F).effect(MobEffects.WEAKNESS, 100);
        }
        bolt.setPos(start.x, start.y, start.z);
        level().addFreshEntity(bolt);
        playSound(ModSounds.LIBRARIAN_HUSH.get(), 0.6F, 1.3F);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        if (!stolen.isEmpty()) {
            spawnAtLocation(level, stolen);
            stolen = ItemStack.EMPTY;
        }
    }

    @Override
    public boolean canUsePortal(boolean ignorePassenger) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return random.nextInt(6) == 0 ? ModSounds.LIBRARIAN_HUSH.get() : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof LibraryDweller) && super.canAttack(target);
    }

    /** Clerk with a stolen stack: runs from whoever it robbed. */
    private final class FleeWithLootGoal extends Goal {
        FleeWithLootGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !stolen.isEmpty() && fleeTicks > 0 && getTarget() != null && distanceToSqr(getTarget()) < 20 * 20;
        }

        @Override
        public void tick() {
            LivingEntity from = getTarget();
            if (from == null || !getNavigation().isDone()) return;
            Vec3 away = DefaultRandomPos.getPosAway(SilentLibrarian.this, 12, 4, from.position());
            if (away != null) getNavigation().moveTo(away.x, away.y, away.z, 1.35);
        }
    }
}
