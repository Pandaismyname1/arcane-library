package com.panda.arcane.mixin;

import com.mojang.serialization.Lifecycle;
import com.panda.arcane.ArcaneMod;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla calls every non-vanilla dimension "experimental", so each new world with the Library asked "these settings
 * are experimental, proceed?". Our own dimensions are stable; other mods' dimensions keep the vanilla check.
 */
@Mixin(WorldDimensions.class)
public abstract class WorldDimensionsMixin {
    @Inject(method = "checkStability", at = @At("HEAD"), cancellable = true)
    private static void arcane$ourDimensionsAreStable(ResourceKey<LevelStem> key, LevelStem dimension, CallbackInfoReturnable<Lifecycle> cir) {
        if (ArcaneMod.MODID.equals(key.identifier().getNamespace())) cir.setReturnValue(Lifecycle.stable());
    }
}
