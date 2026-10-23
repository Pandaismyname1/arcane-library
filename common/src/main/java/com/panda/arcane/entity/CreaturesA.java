package com.panda.arcane.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Hooks for the Creatures A mobs (see ArcaneEvents). */
public final class CreaturesA {
    private CreaturesA() {}

    /** A Warden of Keys keeps every container near it shut: FAIL when it does, else PASS. */
    public static InteractionResult onUseBlock(Player user, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof Container)) return InteractionResult.PASS;
        if (!VaultWard.isLocked(level, pos)) return InteractionResult.PASS;
        if (user instanceof ServerPlayer player && level instanceof ServerLevel server) {
            player.sendOverlayMessage(Component.translatable("message.arcane.vault_ward.locked"));
            server.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 0.8F);
            server.sendParticles(ParticleTypes.WAX_ON, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 8, 0.3, 0.2, 0.3, 0.0);
        }
        return InteractionResult.FAIL;
    }
}
