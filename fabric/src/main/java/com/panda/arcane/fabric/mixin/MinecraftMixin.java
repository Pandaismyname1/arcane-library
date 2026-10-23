package com.panda.arcane.fabric.mixin;

import com.panda.arcane.client.boss.ArchivistClient;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.Music;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The Archivist's fight music replaces the situational track, as NeoForge's SelectMusicEvent does there. */
@Mixin(Minecraft.class)
abstract class MinecraftMixin {
    @Inject(method = "getSituationalMusic", at = @At("HEAD"), cancellable = true)
    private void arcane$fightMusic(CallbackInfoReturnable<Music> cir) {
        Music fight = ArchivistClient.music();
        if (fight != null) cir.setReturnValue(fight);
    }
}
