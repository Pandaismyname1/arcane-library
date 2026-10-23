package com.panda.arcane.item;

import com.panda.arcane.knowledge.KnowledgeManager;
import com.panda.arcane.logic.Mastery;
import com.panda.arcane.registry.ModComponents;
import com.panda.arcane.registry.ModSounds;
import com.panda.arcane.registry.ModTriggers;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/** A tome teaches one enchantment at the item's mastery tier when studied (hold use). */
public class TomeItem extends Item {
    public static final int STUDY_TICKS = 50;
    private final Mastery mastery;

    public TomeItem(Properties properties, Mastery mastery) {
        super(properties);
        this.mastery = mastery;
    }

    public Mastery mastery() {
        return mastery;
    }

    public static Identifier enchantmentOf(ItemStack stack) {
        return stack.get(ModComponents.TOME_ENCHANTMENT.get());
    }

    public static ItemStack create(TomeItem item, Identifier enchantment) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModComponents.TOME_ENCHANTMENT.get(), enchantment);
        return stack;
    }

    public static Component enchantmentName(Identifier id) {
        return Component.translatable(id.toLanguageKey("enchantment"));
    }

    @Override
    public Component getName(ItemStack stack) {
        Identifier id = enchantmentOf(stack);
        if (id == null) return Component.translatable("item.arcane.tome.blank");
        return Component.translatable("item.arcane.tome", enchantmentName(id));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (enchantmentOf(stack) == null) return InteractionResult.FAIL;
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return STUDY_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BLOCK;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int ticksRemaining) {
        if (level instanceof ServerLevel server && ticksRemaining % 5 == 0) {
            server.sendParticles(com.panda.arcane.registry.ModParticles.PORTAL_GLYPH.get(), entity.getX(), entity.getEyeY() - 0.2, entity.getZ(), 3, 0.3, 0.3, 0.3, 0.6);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return stack;
        Identifier id = enchantmentOf(stack);
        if (id == null) return stack;
        if (!KnowledgeManager.exists(player, id)) {
            player.sendOverlayMessage(Component.translatable("message.arcane.tome.unknown").withStyle(ChatFormatting.RED));
            return stack;
        }
        Mastery before = KnowledgeManager.mastery(player, id);
        if (!KnowledgeManager.learn(player, id, mastery)) {
            player.sendOverlayMessage(Component.translatable("message.arcane.tome.known", enchantmentName(id)).withStyle(ChatFormatting.GRAY));
            return stack;
        }
        level.playSound(null, player.blockPosition(), ModSounds.STUDY_COMPLETE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        if (level instanceof ServerLevel server) {
            server.sendParticles(com.panda.arcane.registry.ModParticles.PORTAL_GLYPH.get(), player.getX(), player.getEyeY(), player.getZ(), 40, 0.5, 0.6, 0.5, 1.0);
        }
        player.sendOverlayMessage(Component.translatable(before.known() ? "message.arcane.tome.deepened" : "message.arcane.tome.learned",
                enchantmentName(id), Component.translatable("mastery.arcane." + mastery.tier())).withStyle(ChatFormatting.AQUA));
        ModTriggers.ARCANE.get().trigger(player, "learned", mastery.tier());
        stack.consume(1, player);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        Identifier id = enchantmentOf(stack);
        if (id == null) return;
        lines.accept(Component.translatable("tooltip.arcane.tome.teaches", Component.translatable("mastery.arcane." + mastery.tier()))
                .withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("tooltip.arcane.tome.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
