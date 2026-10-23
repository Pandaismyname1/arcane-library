package com.panda.arcane.item;

import com.panda.arcane.portal.ArcanePortalShape;
import com.panda.arcane.registry.ModSounds;
import com.panda.arcane.registry.ModTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Lights a frame of inscribed bricks, opening a way into the Arcane Library. */
public class BookOfPortalsItem extends Item {
    public BookOfPortalsItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockState state = level.getBlockState(clicked);
        if (!ArcanePortalShape.isFrame(state)) return InteractionResult.PASS;
        // the interior is next to the clicked face, or next to the clicked block for the sill and posts
        BlockPos[] candidates = {clicked.relative(context.getClickedFace()), clicked.above(), clicked.east(), clicked.west(), clicked.north(), clicked.south()};
        for (BlockPos start : candidates) {
            for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
                var shape = ArcanePortalShape.find(level, start, axis);
                if (shape.isEmpty()) continue;
                if (!level.isClientSide()) {
                    shape.get().light(level);
                    level.playSound(null, start, ModSounds.PORTAL_TRAVEL.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                    Player player = context.getPlayer();
                    if (player instanceof ServerPlayer sp) {
                        context.getItemInHand().hurtAndBreak(1, sp.level(), sp, item -> {});
                        ModTriggers.ARCANE.get().trigger(sp, "portal_lit");
                    }
                }
                return InteractionResult.SUCCESS;
            }
        }
        if (!level.isClientSide() && context.getPlayer() instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(Component.translatable("message.arcane.portal.invalid"));
        }
        return InteractionResult.FAIL;
    }
}
