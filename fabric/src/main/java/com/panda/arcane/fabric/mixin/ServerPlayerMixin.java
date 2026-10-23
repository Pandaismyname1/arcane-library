package com.panda.arcane.fabric.mixin;

import com.panda.arcane.event.ArcaneEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A player opened a container: Fabric has no event for it, so this is where NeoForge's PlayerContainerEvent.Open fires. */
@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin {
    @Inject(method = "initMenu", at = @At("TAIL"))
    private void arcane$onContainerOpen(AbstractContainerMenu menu, CallbackInfo ci) {
        ArcaneEvents.onContainerOpen((ServerPlayer) (Object) this, menu);
    }
}
