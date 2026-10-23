package com.panda.arcane.fabric.mixin;

import com.panda.arcane.event.ArcaneEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vetoes spawns the mod forbids (the Library's roof), where NeoForge fires MobSpawnEvent.SpawnPlacementCheck. */
@Mixin(SpawnPlacements.class)
abstract class SpawnPlacementsMixin {
    @Inject(method = "checkSpawnRules", at = @At("HEAD"), cancellable = true)
    private static <T extends Entity> void arcane$spawnAllowed(EntityType<T> type, ServerLevelAccessor level, EntitySpawnReason reason,
                                                               BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> cir) {
        if (!ArcaneEvents.spawnAllowed(level, pos, reason)) cir.setReturnValue(false);
    }
}
