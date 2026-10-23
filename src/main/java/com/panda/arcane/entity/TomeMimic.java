package com.panda.arcane.entity;

import com.panda.arcane.worldgen.LibraryLayout;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A book that is not a book. It lies closed on the floor, perfectly still, a ribbon hanging out of its pages, until
 * someone steps close or strikes it: then it springs up on stubby legs and bites. Left alone for ten seconds it
 * settles back into a book.
 *
 * <ul>
 *   <li><b>Pocket</b>: small and quick. Bites, then scurries away to bite again later.</li>
 *   <li><b>Grimoire</b> (studies and the Restricted Section): wakes from further away and casts from range: a lifting
 *       hex, a slowing hex, or a swap that trades places with you. Each cast is telegraphed by its pages flipping.</li>
 *   <li><b>Ledger</b> (vaults and scriptoria): swallows an item from your pack with each bite and gobbles items on the
 *       floor. Everything it ate spills out when it dies.</li>
 * </ul>
 */
public class TomeMimic extends VariantMonster {
    public static final int POCKET = 0, GRIMOIRE = 1, LEDGER = 2;
    private static final String[] NAMES = {"pocket", "grimoire", "ledger"};
    public static final int SPELL_NONE = 0, SPELL_LIFT = 1, SPELL_HEX = 2, SPELL_SWAP = 3;
    public static final int CAST_TICKS = 24;
    private static final int MAX_EATEN = 12;

    private static final EntityDataAccessor<Boolean> DATA_AWAKE = SynchedEntityData.defineId(TomeMimic.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SPELL = SynchedEntityData.defineId(TomeMimic.class, EntityDataSerializers.INT);

    private final List<ItemStack> eaten = new ArrayList<>();
    private int idleTicks;
    private int fleeTicks;
    private int castCooldown = 60;
    private int castTicks;

    // client-side animation
    public float openO, open;
    public int biteTicks;

    public TomeMimic(EntityType<? extends TomeMimic> type, Level level) {
        super(type, level);
        this.xpReward = 7;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.SCALE, 1.25) // polish: read at a distance (renders and collides 25% larger)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    public String[] variantNames() {
        return NAMES;
    }

    @Override
    protected int chooseVariant(MobsB.Zone zone, RandomSource random) {
        if (zone.is(LibraryLayout.Kind.VAULT) || zone.is(LibraryLayout.Kind.SCRIPTORIUM)) return zone.restricted() ? MobsB.weighted(random, 10, 40, 50) : MobsB.weighted(random, 30, 10, 60);
        if (zone.restricted()) return MobsB.weighted(random, 25, 50, 25);
        if (zone.is(LibraryLayout.Kind.STUDY)) return MobsB.weighted(random, 55, 30, 15);
        return MobsB.weighted(random, 75, 10, 15);
    }

    @Override
    protected void applyVariantStats(int v) {
        base(Attributes.MAX_HEALTH, switch (v) { case GRIMOIRE -> 22.0; case LEDGER -> 28.0; default -> 16.0; });
        base(Attributes.ATTACK_DAMAGE, switch (v) { case GRIMOIRE -> 3.0; case LEDGER -> 5.0; default -> 3.0; });
        base(Attributes.MOVEMENT_SPEED, switch (v) { case POCKET -> 0.34; case LEDGER -> 0.24; default -> 0.27; });
        base(Attributes.ARMOR, v == LEDGER ? 6.0 : 2.0);
        scale(switch (v) { case POCKET -> 0.75; case LEDGER -> 1.2; default -> 1.0; });
    }

    // ---- state ---------------------------------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_AWAKE, false);
        builder.define(DATA_SPELL, SPELL_NONE);
    }

    public boolean awake() {
        return entityData.get(DATA_AWAKE);
    }

    public int spell() {
        return entityData.get(DATA_SPELL);
    }

    public List<ItemStack> eaten() {
        return eaten;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Awake", awake());
        var list = output.list("Eaten", ItemStack.CODEC);
        for (ItemStack s : eaten) if (!s.isEmpty()) list.add(s);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(DATA_AWAKE, input.getBooleanOr("Awake", false));
        eaten.clear();
        input.list("Eaten", ItemStack.CODEC).ifPresent(l -> l.forEach(eaten::add));
        entityData.set(DATA_SPELL, SPELL_NONE);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new CastGoal());
        goalSelector.addGoal(2, new FleeGoal());
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.25, true) {
            @Override
            public boolean canUse() {
                return awake() && fleeTicks <= 0 && spell() == SPELL_NONE && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return awake() && fleeTicks <= 0 && spell() == SPELL_NONE && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(4, new GobbleGoal());
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return awake() && super.canUse();
            }
        });
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F) {
            @Override
            public boolean canUse() {
                return awake() && super.canUse();
            }
        });
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, (t, l) -> awake()));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            openO = open;
            float goal = !awake() ? 0.0F : spell() != SPELL_NONE ? 1.0F : 0.55F;
            open += (goal - open) * (awake() ? 0.25F : 0.12F);
            if (biteTicks > 0) biteTicks--;
            if (spell() != SPELL_NONE && random.nextInt(2) == 0) {
                level().addParticle(ParticleTypes.ENCHANT, getRandomX(0.8), getY() + 0.6, getRandomZ(0.8), 0, 0.3, 0);
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) biteTicks = 10;
        else super.handleEntityEvent(id);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (fleeTicks > 0) fleeTicks--;
        if (castCooldown > 0) castCooldown--;
        if (!awake()) {
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
            double range = variant() == GRIMOIRE ? 4.5 : 2.6;
            Player near = level.getNearestPlayer(getX(), getY(), getZ(), range, p -> !p.isSpectator() && !((Player) p).isCreative());
            if (near != null) wake(near);
            return;
        }
        LivingEntity target = getTarget();
        idleTicks = target == null || !target.isAlive() ? idleTicks + 1 : 0;
        if (idleTicks > 200 && spell() == SPELL_NONE) settle();
        if (spell() != SPELL_NONE) tickCast(level, target);
    }

    /** Spring up from the disguise and go for this player. */
    public void wake(LivingEntity cause) {
        if (awake()) return;
        entityData.set(DATA_AWAKE, true);
        idleTicks = 0;
        setTarget(cause);
        setDeltaMovement(getDeltaMovement().add(0, 0.42, 0));
        MobsB.layered(this, MobsB.MIMIC_WAKE.get(), 1.0F, variant() == POCKET ? 1.3F : 0.9F, MobsB.MIMIC_CHATTER.get(), 1.0F, 1.0F);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.3, getZ(), 8, 0.3, 0.1, 0.3, 0.02);
        }
    }

    /** Close up and lie still again. */
    public void settle() {
        entityData.set(DATA_AWAKE, false);
        setTarget(null);
        idleTicks = 0;
        MobsB.play(this, MobsB.MIMIC_SETTLE.get(), 0.8F, 1.0F);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && !awake() && source.getEntity() instanceof LivingEntity attacker) wake(attacker);
        if (hurt && spell() != SPELL_NONE && damage >= 4.0F) { // a solid hit breaks the cast
            entityData.set(DATA_SPELL, SPELL_NONE);
            castCooldown = 80;
        }
        return hurt;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (!hit) return false;
        level.broadcastEntityEvent(this, (byte) 4);
        MobsB.layered(this, MobsB.MIMIC_BITE.get(), 1.0F, variant() == POCKET ? 1.4F : 1.0F, MobsB.MIMIC_CHATTER.get(), 0.6F, 1.2F);
        if (variant() == POCKET) fleeTicks = 50;
        if (variant() == LEDGER && target instanceof Player player) swallowFrom(player);
        return true;
    }

    /** Ledger: swallow one stack from the player's pack (never armour or the item in hand). Returns what it took. */
    public ItemStack swallowFrom(Player player) {
        if (eaten.size() >= MAX_EATEN) return ItemStack.EMPTY;
        var inv = player.getInventory();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            if (i == inv.getSelectedSlot()) continue;
            if (!inv.getItem(i).isEmpty()) slots.add(i);
        }
        if (slots.isEmpty()) return ItemStack.EMPTY;
        int slot = slots.get(random.nextInt(slots.size()));
        ItemStack taken = inv.removeItemNoUpdate(slot);
        eaten.add(taken.copy());
        MobsB.play(this, MobsB.MIMIC_GULP.get(), 1.0F, 1.0F);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, taken.getItem()), getX(), getY() + 0.5, getZ(), 10, 0.2, 0.2, 0.2, 0.05);
        }
        if (player instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(Component.translatable("message.arcane.tome_mimic.swallowed", taken.getHoverName()));
        }
        return taken;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        for (ItemStack s : eaten) spawnAtLocation(level, s);
        eaten.clear();
    }

    // ---- grimoire spells -------------------------------------------------------------------------------------------

    /** Test hook and goal entry: begin a telegraphed cast. */
    public void startCast(int spell) {
        entityData.set(DATA_SPELL, spell);
        castTicks = 0;
        getNavigation().stop();
        MobsB.layered(this, MobsB.MIMIC_CAST.get(), 1.0F, 0.8F, MobsB.WRAITH_FLUTTER.get(), 0.8F, 0.9F);
    }

    private void tickCast(ServerLevel level, LivingEntity target) {
        castTicks++;
        getNavigation().stop();
        if (target != null) getLookControl().setLookAt(target, 40, 40);
        int spell = spell();
        var particle = spell == SPELL_LIFT ? ParticleTypes.END_ROD : spell == SPELL_HEX ? ParticleTypes.WITCH : ParticleTypes.PORTAL;
        if (castTicks % 3 == 0) level.sendParticles(particle, getX(), getY() + 0.7, getZ(), 4, 0.3, 0.2, 0.3, 0.05);
        if (castTicks < CAST_TICKS) return;
        entityData.set(DATA_SPELL, SPELL_NONE);
        castCooldown = 70 + random.nextInt(30);
        if (target == null || !target.isAlive()) return;
        Vec3 start = new Vec3(getX(), getY() + 0.6 * getScale(), getZ());
        switch (spell) {
            case SPELL_LIFT, SPELL_HEX -> {
                Vec3 aim = target.getEyePosition().subtract(start).normalize();
                ArcaneBolt bolt = new ArcaneBolt(level, this, aim).damage(2.0F)
                        .effect(spell == SPELL_LIFT ? MobEffects.LEVITATION : MobEffects.SLOWNESS, spell == SPELL_LIFT ? 30 : 100);
                bolt.setPos(start.x, start.y, start.z);
                level.addFreshEntity(bolt);
            }
            case SPELL_SWAP -> {
                Vec3 mine = position(), theirs = target.position();
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, mine.x, mine.y + 0.5, mine.z, 20, 0.3, 0.4, 0.3, 0.05);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, theirs.x, theirs.y + 1, theirs.z, 20, 0.3, 0.6, 0.3, 0.05);
                target.teleportTo(mine.x, mine.y, mine.z);
                teleportTo(theirs.x, theirs.y, theirs.z);
                MobsB.play(this, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, 0.8F, 1.3F);
            }
            default -> { }
        }
        MobsB.play(this, MobsB.MIMIC_CAST.get(), 1.0F, 1.3F);
    }

    // ---- sounds --------------------------------------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return awake() ? MobsB.MIMIC_CHATTER.get() : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return MobsB.MIMIC_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return MobsB.MIMIC_DEATH.get();
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (variant() == POCKET ? 1.3F : variant() == LEDGER ? 0.8F : 1.0F);
    }

    @Override
    public boolean isPushable() {
        return awake() && super.isPushable();
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    // ---- goals ---------------------------------------------------------------------------------------------------

    private final class CastGoal extends Goal {
        CastGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (spell() != SPELL_NONE) return true;
            LivingEntity t = getTarget();
            if (variant() != GRIMOIRE || !awake() || t == null || castCooldown > 0) return false;
            double d = distanceToSqr(t);
            return d > 9 && d < 196 && hasLineOfSight(t);
        }

        @Override
        public boolean canContinueToUse() {
            return spell() != SPELL_NONE;
        }

        @Override
        public void start() {
            if (spell() != SPELL_NONE) return;
            LivingEntity t = getTarget();
            int roll = random.nextInt(3);
            if (roll == 2 && t != null && distanceToSqr(t) < 36) roll = random.nextInt(2);
            startCast(roll == 0 ? SPELL_LIFT : roll == 1 ? SPELL_HEX : SPELL_SWAP);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    /** Pocket: after a bite, scurry away for a couple of seconds. */
    private final class FleeGoal extends Goal {
        FleeGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return fleeTicks > 0 && getTarget() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return fleeTicks > 0 && !getNavigation().isDone();
        }

        @Override
        public void start() {
            LivingEntity t = getTarget();
            if (t == null) return;
            Vec3 away = DefaultRandomPos.getPosAway(TomeMimic.this, 8, 4, t.position());
            if (away != null) getNavigation().moveTo(away.x, away.y, away.z, 1.6);
        }
    }

    /** Ledger: waddle to items lying on the floor nearby and swallow them. */
    private final class GobbleGoal extends Goal {
        private ItemEntity item;

        GobbleGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (variant() != LEDGER || !awake() || eaten.size() >= MAX_EATEN || random.nextInt(10) != 0) return false;
            List<ItemEntity> items = level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(8, 2, 8), ItemEntity::isAlive);
            item = items.isEmpty() ? null : items.get(0);
            return item != null;
        }

        @Override
        public boolean canContinueToUse() {
            return item != null && item.isAlive() && !getNavigation().isDone();
        }

        @Override
        public void start() {
            getNavigation().moveTo(item, 1.2);
        }

        @Override
        public void tick() {
            if (item != null && item.isAlive() && distanceToSqr(item) < 2.0) {
                eaten.add(item.getItem().copy());
                item.discard();
                MobsB.play(TomeMimic.this, MobsB.MIMIC_GULP.get(), 1.0F, 1.1F);
                item = null;
            }
        }
    }

    /** For the renderer: how open the jaws are, 0 closed .. 1 wide. */
    public float openAmount(float partial) {
        return Mth.lerp(partial, openO, open);
    }
}
