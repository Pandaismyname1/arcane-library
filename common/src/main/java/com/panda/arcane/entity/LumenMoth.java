package com.panda.arcane.entity;

import com.panda.arcane.registry.ModItems;
import com.panda.arcane.registry.ModSounds;
import java.util.EnumSet;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A glowing moth that drifts toward light (Creatures A). Four variants, each with its own effect on whoever comes near:
 * <ul>
 *   <li>Common: leaves a trail of light and now and then sheds a mote of Lumen Dust.</li>
 *   <li>Ember: lights candles and campfires it passes, and sets anyone who swats it alight.</li>
 *   <li>Pale: a gentle light; standing near it grants Night Vision.</li>
 *   <li>Void (Restricted Section): drinks the light. Snuffs candles and wraps nearby players in Darkness.</li>
 * </ul>
 */
public class LumenMoth extends PathfinderMob implements LibraryDweller, CreatureVisuals {
    public enum Variant {
        COMMON(60, 20), EMBER(15, 30), PALE(25, 12), VOID(0, 38);

        final int normalWeight, restrictedWeight;

        Variant(int normalWeight, int restrictedWeight) {
            this.normalWeight = normalWeight;
            this.restrictedWeight = restrictedWeight;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** How far the aura of a Pale or Void moth reaches, in blocks. */
    public static final double AURA_RADIUS = 5.0;
    /** Ticks between aura pulses. */
    public static final int PULSE_TICKS = 40;

    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(LumenMoth.class, EntityDataSerializers.INT);
    private int dustTimer = 2400;

    public LumenMoth(EntityType<? extends LumenMoth> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl<>(this, 20, true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.FLYING_SPEED, 0.4F)
                .add(Attributes.MOVEMENT_SPEED, 0.2F)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    // ---- variant ------------------------------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, 0);
    }

    public Variant variant() {
        return Variant.values()[Math.floorMod(entityData.get(DATA_VARIANT), Variant.values().length)];
    }

    public void setVariant(Variant variant) {
        entityData.set(DATA_VARIANT, variant.ordinal());
    }

    @Override
    public int variantIndex() {
        return variant().ordinal();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("Variant", variant().id());
        output.putInt("DustTimer", dustTimer);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setVariant(CreatureZones.byName(Variant.values(), input.getStringOr("Variant", "common")));
        dustTimer = input.getIntOr("DustTimer", 2400);
    }

    public static Variant pickVariant(CreatureZones zone, net.minecraft.util.RandomSource random) {
        return CreatureZones.pick(Variant.values(), v -> zone.restricted() ? v.restrictedWeight : v.normalWeight, random);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
                                                  @Nullable SpawnGroupData data) {
        setVariant(pickVariant(CreatureZones.at(level, blockPosition()), random));
        dustTimer = 1200 + random.nextInt(2400);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    protected Component getTypeName() {
        return Component.translatable(getType().getDescriptionId() + "." + variant().id());
    }

    // ---- behaviour ----------------------------------------------------------------------------------------------

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        goalSelector.addGoal(2, new SeekLightGoal(this));
        goalSelector.addGoal(3, new WaterAvoidingRandomFlyingGoal(this, 1.0));
    }

    @Override
    public void travel(Vec3 input) {
        travelFlying(input, getSpeed());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() && random.nextInt(4) == 0) {
            ParticleOptions trail = switch (variant()) {
                case EMBER -> random.nextBoolean() ? ParticleTypes.SMALL_FLAME : ParticleTypes.SMOKE;
                case PALE -> ParticleTypes.WHITE_ASH;
                case VOID -> ParticleTypes.REVERSE_PORTAL;
                default -> ParticleTypes.END_ROD;
            };
            Vec3 tail = position().add(getViewVector(1.0F).scale(-0.35));
            level().addParticle(trail, tail.x, getY() + 0.25, tail.z, 0.0, variant() == Variant.EMBER ? 0.01 : -0.01, 0.0);
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if ((tickCount + getId()) % PULSE_TICKS == 0) pulse(level);
        if (variant() == Variant.COMMON && --dustTimer <= 0) {
            dustTimer = 2400 + random.nextInt(2400);
            spawnAtLocation(level, new ItemStack(ModItems.LUMEN_DUST.get(), 3));
            level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.2, getZ(), 6, 0.2, 0.2, 0.2, 0.02);
        }
    }

    /** One aura pulse of this moth's variant. Public so tests can trigger it directly. */
    public void pulse(ServerLevel level) {
        switch (variant()) {
            case EMBER -> touchLights(level, true);
            case VOID -> {
                touchLights(level, false);
                for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(AURA_RADIUS))) {
                    if (p.isSpectator()) continue;
                    p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0, true, true), this);
                }
                level.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 0.25, getZ(), 8, 0.4, 0.3, 0.4, 0.01);
            }
            case PALE -> {
                for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(AURA_RADIUS))) {
                    if (p.isSpectator()) continue;
                    p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 320, 0, true, false), this);
                }
            }
            default -> { }
        }
    }

    /** Ember moths light candles and campfires within three blocks; Void moths put them out. Returns how many changed. */
    public int touchLights(ServerLevel level, boolean light) {
        int changed = 0;
        BlockPos origin = blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-3, -3, -3), origin.offset(3, 3, 3))) {
            BlockState state = level.getBlockState(pos);
            if (!(state.is(BlockTags.CANDLES) || state.is(BlockTags.CANDLE_CAKES) || state.is(BlockTags.CAMPFIRES))) continue;
            if (!state.hasProperty(BlockStateProperties.LIT) || state.getValue(BlockStateProperties.LIT) == light) continue;
            if (light && state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)) continue;
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, light), 11);
            level.sendParticles(light ? ParticleTypes.FLAME : ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 4, 0.1, 0.1, 0.1, 0.01);
            level.playSound(null, pos, light ? SoundEvents.FLINTANDSTEEL_USE : SoundEvents.CANDLE_EXTINGUISH, getSoundSource(), 0.6F, 1.4F);
            changed++;
        }
        return changed;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (variant() == Variant.EMBER && source.getDirectEntity() instanceof LivingEntity attacker && attacker.distanceToSqr(this) < 16.0) {
            attacker.igniteForSeconds(3.0F);
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    public boolean fireImmune() {
        return variant() == Variant.EMBER || super.fireImmune();
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean ignorePassenger) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return random.nextInt(4) == 0 ? ModSounds.MOTH_FLUTTER.get() : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BAT_DEATH;
    }

    /** Wanders toward the nearest bright block it can find (Void moths seek the dark instead). */
    private static final class SeekLightGoal extends Goal {
        private final LumenMoth moth;
        private BlockPos target;

        SeekLightGoal(LumenMoth moth) {
            this.moth = moth;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (moth.getRandom().nextInt(30) != 0) return false;
            boolean dark = moth.variant() == Variant.VOID;
            BlockPos best = null;
            int bestScore = dark ? -6 : 11;
            BlockPos origin = moth.blockPosition();
            for (int i = 0; i < 24; i++) {
                BlockPos p = origin.offset(moth.getRandom().nextInt(17) - 8, moth.getRandom().nextInt(9) - 4, moth.getRandom().nextInt(17) - 8);
                int light = moth.level().getBrightness(LightLayer.BLOCK, p);
                int score = dark ? -light : light;
                if (score > bestScore && moth.level().getBlockState(p).isAir()) {
                    best = p;
                    bestScore = score;
                }
            }
            target = best;
            return best != null;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && !moth.getNavigation().isDone();
        }

        @Override
        public void start() {
            moth.getNavigation().moveTo(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, 1.0);
        }
    }
}
