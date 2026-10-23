package com.panda.arcane.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Game-bus hooks for the Creatures A mobs (one line in ArcaneMod installs them). */
public final class CreaturesA {
    private CreaturesA() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(CreaturesA::onRightClickBlock);
    }

    /** A Warden of Keys keeps every container near it shut. */
    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        var level = event.getLevel();
        if (!(level.getBlockEntity(event.getPos()) instanceof Container)) return;
        if (!VaultWard.isLocked(level, event.getPos())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        if (event.getEntity() instanceof ServerPlayer player && level instanceof ServerLevel server) {
            player.sendOverlayMessage(Component.translatable("message.arcane.vault_ward.locked"));
            server.playSound(null, event.getPos(), SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 0.8F);
            server.sendParticles(ParticleTypes.WAX_ON, event.getPos().getX() + 0.5, event.getPos().getY() + 1.0, event.getPos().getZ() + 0.5,
                    8, 0.3, 0.2, 0.3, 0.0);
        }
    }
}
