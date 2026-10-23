package com.panda.arcane.fabric.mixin;

import com.panda.arcane.event.ShelfFeedback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Entities spawned into a level (not loaded with their chunk): what NeoForge's EntityJoinLevelEvent gives us there. */
@Mixin(ServerLevel.class)
abstract class ServerLevelMixin {
    @Inject(method = "addFreshEntity", at = @At("RETURN"))
    private void arcane$onSpawn(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) ShelfFeedback.onSpawn(entity, (ServerLevel) (Object) this);
    }
}
