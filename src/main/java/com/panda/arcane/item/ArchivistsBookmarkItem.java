package com.panda.arcane.item;

import com.panda.arcane.registry.ModSounds;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * The Archivist's Bookmark: the boss's own Recall, tamed. Sneak-use on a block to mark that page of the world; hold use
 * anywhere in the same dimension to be pulled back to it. One minute between recalls.
 *
 * <p>The mark is stored in the vanilla {@code lodestone_tracker} component (untracked), so no new component type.
 */
public class ArchivistsBookmarkItem extends Item {
    public static final int READ_TICKS = 32;
    public static final int COOLDOWN_TICKS = 1200;

    public ArchivistsBookmarkItem(Properties properties) {
        super(properties);
    }

    public static Optional<GlobalPos> mark(ItemStack stack) {
        LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
        return tracker == null ? Optional.empty() : tracker.target();
    }

    public static void setMark(ItemStack stack, GlobalPos pos) {
        stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(pos), false));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isSecondaryUseActive()) return InteractionResult.PASS;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        if (!level.isClientSide()) {
            setMark(context.getItemInHand(), GlobalPos.of(level.dimension(), pos));
            level.playSound(null, pos, ModSounds.BOOKMARK_USE.get(), SoundSource.PLAYERS, 0.7F, 1.4F);
            ((ServerLevel) level).sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 30, 0.3, 0.3, 0.3, 0.6);
            player.sendOverlayMessage(Component.translatable("message.arcane.bookmark.marked", pos.getX(), pos.getY(), pos.getZ())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Optional<GlobalPos> mark = mark(stack);
        if (mark.isEmpty()) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.arcane.bookmark.unmarked").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        if (!mark.get().dimension().equals(level.dimension())) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.arcane.bookmark.elsewhere").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return READ_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BLOCK;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int ticksRemaining) {
        if (level instanceof ServerLevel server && ticksRemaining % 3 == 0) {
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, entity.getX(), entity.getY() + 1.0, entity.getZ(), 4, 0.4, 0.6, 0.4, 0.02);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player) || !(level instanceof ServerLevel server)) return stack;
        Optional<GlobalPos> mark = mark(stack);
        if (mark.isEmpty() || !mark.get().dimension().equals(level.dimension())) return stack;
        BlockPos pos = mark.get().pos();
        var box = player.getBoundingBox().move(pos.getX() + 0.5 - player.getX(), pos.getY() - player.getY(), pos.getZ() + 0.5 - player.getZ());
        if (!server.noCollision(player, box)) {
            player.sendOverlayMessage(Component.translatable("message.arcane.bookmark.blocked").withStyle(ChatFormatting.RED));
            return stack;
        }
        server.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
        player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        player.resetFallDistance();
        server.playSound(null, pos, ModSounds.BOOKMARK_USE.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
        server.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 50, 0.4, 0.8, 0.4, 1.0);
        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        return stack;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return mark(stack).isPresent() || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        Optional<GlobalPos> mark = mark(stack);
        if (mark.isPresent()) {
            BlockPos p = mark.get().pos();
            lines.accept(Component.translatable("tooltip.arcane.bookmark.marked", p.getX(), p.getY(), p.getZ(),
                    mark.get().dimension().identifier().toString()).withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            lines.accept(Component.translatable("tooltip.arcane.bookmark.unmarked").withStyle(ChatFormatting.DARK_GRAY));
        }
        lines.accept(Component.translatable("tooltip.arcane.bookmark.how").withStyle(ChatFormatting.GRAY));
    }
}
