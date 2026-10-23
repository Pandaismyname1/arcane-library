package com.panda.arcane.event;

import com.panda.arcane.ArcaneMod;
import com.panda.arcane.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * UI/UX feedback when a suspicious bookshelf gives up its book: an action-bar line naming the find, a chime and a
 * puff of enchant glyphs. Vanilla brushing drops the item next to the shelf while the shelf is still fully dusted
 * and already empty, and only then turns it into a bookshelf; that moment is what this recognises. It works for
 * shelves loaded from disk too (their block entity is a plain {@link BrushableBlockEntity}). Loot is untouched.
 */
@EventBusSubscriber(modid = ArcaneMod.MODID)
public final class ShelfFeedback {
    private ShelfFeedback() {}

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getEntity() instanceof ItemEntity item) || !(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos at = item.blockPosition();
        for (Direction d : Direction.values()) {
            BlockPos shelf = at.relative(d);
            BlockState state = level.getBlockState(shelf);
            if (!state.is(ModBlocks.SUSPICIOUS_BOOKSHELF.get()) || state.getValue(BlockStateProperties.DUSTED) < 3) continue;
            if (!(level.getBlockEntity(shelf) instanceof BrushableBlockEntity be) || !be.getItem().isEmpty()) continue;
            reveal(level, shelf, d.getOpposite(), item);
            return;
        }
    }

    private static void reveal(ServerLevel level, BlockPos shelf, Direction face, ItemEntity item) {
        ServerPlayer finder = null;
        double best = 8 * 8;
        for (ServerPlayer p : level.players()) {
            double dist = p.distanceToSqr(shelf.getX() + 0.5, shelf.getY() + 0.5, shelf.getZ() + 0.5);
            boolean brushing = p.isUsingItem() && p.getUseItem().is(Items.BRUSH);
            if (dist < best || (brushing && dist < 8 * 8)) {
                best = brushing ? -1 : dist;
                finder = p;
            }
        }
        if (finder != null) {
            finder.sendOverlayMessage(Component.translatable("message.arcane.shelf.found", item.getItem().getHoverName())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        level.playSound(null, shelf, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.2F);
        level.playSound(null, shelf, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.sendParticles(com.panda.arcane.registry.ModParticles.PORTAL_GLYPH.get(), shelf.getX() + 0.5 + face.getStepX() * 0.7, shelf.getY() + 0.6,
                shelf.getZ() + 0.5 + face.getStepZ() * 0.7, 24, 0.3, 0.3, 0.3, 0.4);
    }
}
