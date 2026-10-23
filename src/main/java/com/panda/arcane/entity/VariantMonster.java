package com.panda.arcane.entity;

import com.panda.arcane.ArcaneMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

/**
 * Base of the Creatures B mobs: one entity type, several variants. The variant is a synced id saved to NBT
 * ("Variant": its name), picked at spawn from where the creature stands in the Library, and it sets stats, the
 * texture, behaviour, and an extra loot table {@code entities/<type>/<variant>} rolled on top of the type's own.
 */
public abstract class VariantMonster extends Monster implements LibraryDweller {
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(VariantMonster.class, EntityDataSerializers.INT);

    protected VariantMonster(EntityType<? extends VariantMonster> type, Level level) {
        super(type, level);
    }

    /** Variant names in id order. The first is the default. */
    public abstract String[] variantNames();

    /** Pick the variant for a natural or structure spawn at this place. */
    protected abstract int chooseVariant(MobsB.Zone zone, RandomSource random);

    /** The spawn-time variant roll, exposed for tests. */
    public final int pickVariantFor(MobsB.Zone zone, RandomSource random) {
        return chooseVariant(zone, random);
    }

    /** Stats for a variant: called whenever the variant is set or loaded. */
    protected abstract void applyVariantStats(int variant);

    public final int variant() {
        return Mth.clamp(entityData.get(DATA_VARIANT), 0, variantNames().length - 1);
    }

    public final String variantName() {
        return variantNames()[variant()];
    }

    public void setVariant(int variant) {
        entityData.set(DATA_VARIANT, Mth.clamp(variant, 0, variantNames().length - 1));
        applyVariantStats(variant());
        refreshDimensions();
    }

    /** Sets the variant and fills health, as a fresh spawn would. */
    public void setVariantFresh(int variant) {
        setVariant(variant);
        setHealth(getMaxHealth());
    }

    protected final void base(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
        var instance = getAttribute(attribute);
        if (instance != null) instance.setBaseValue(value);
    }

    protected final void scale(double s) {
        base(Attributes.SCALE, s);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("Variant", variantName());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        String name = input.getStringOr("Variant", variantNames()[0]);
        int index = 0;
        for (int i = 0; i < variantNames().length; i++) if (variantNames()[i].equals(name)) index = i;
        setVariant(index);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
                                                  @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        setVariantFresh(chooseVariant(MobsB.zone(level, blockPosition()), getRandom()));
        return result;
    }

    @Override
    protected void dropFromLootTable(ServerLevel level, DamageSource source, boolean playerKilled) {
        super.dropFromLootTable(level, source, playerKilled);
        var key = ResourceKey.create(Registries.LOOT_TABLE, ArcaneMod.id("entities/" + typeName() + "/" + variantName()));
        if (level.getServer().reloadableRegistries().getLootTable(key) != LootTable.EMPTY) {
            dropFromLootTable(level, source, playerKilled, key);
        }
    }

    public String typeName() {
        return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath();
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof LibraryDweller) && super.canAttack(target);
    }

    @Override
    public boolean canUsePortal(boolean ignorePassenger) {
        return false;
    }
}
