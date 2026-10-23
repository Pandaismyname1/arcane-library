package com.panda.arcane.client;

import com.panda.arcane.ArcaneMod;
import com.panda.arcane.item.TomeItem;
import com.panda.arcane.knowledge.KnowledgeManager;
import com.panda.arcane.lectern.DeconstructionMenu;
import com.panda.arcane.logic.Mastery;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Tooltip lines about knowledge: on tomes, what you know and what studying would change; on enchanted gear and
 * books, how much of it you know and (with Shift) what deconstructing it would teach. Display only.
 */
public final class ArcaneTooltips {
    /** For tests: SDL key state can't be faked, so this stands in for holding Shift. */
    public static boolean forceDetail;

    private ArcaneTooltips() {}

    /** Adds the mod's lines to an item's tooltip. The player is null when the loader doesn't know one. */
    public static void onTooltip(ItemStack stack, Player viewer, TooltipFlag flags, List<Component> tip) {
        List<Component> extra = new ArrayList<>();
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId.getNamespace().equals(ArcaneMod.MODID)) {
            String key = "tooltip.arcane.item." + itemId.getPath();
            if (Language.getInstance().has(key)) extra.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        }
        Player player = viewer != null ? viewer : Minecraft.getInstance().player;
        if (player != null) {
            if (stack.getItem() instanceof TomeItem tome) tome(extra, player, stack, tome);
            else if (DeconstructionMenu.hasEnchantments(stack)) gear(extra, player, stack);
        }
        if (extra.isEmpty()) return;
        // in advanced mode keep our lines above the item id and component count lines
        int at = tip.size();
        if (flags.isAdvanced()) {
            for (int i = tip.size() - 1; i > 0; i--) {
                String s = tip.get(i).getString();
                if (s.equals(itemId.toString()) || s.startsWith(itemId.toString())) { at = i; break; }
            }
        }
        tip.addAll(at, extra);
    }

    private static void tome(List<Component> out, Player player, ItemStack stack, TomeItem tome) {
        Identifier id = TomeItem.enchantmentOf(stack);
        if (id == null) return;
        Mastery now = KnowledgeManager.mastery(player, id);
        Mastery teaches = tome.mastery();
        if (!now.known()) {
            out.add(Component.translatable("tooltip.arcane.know.unknown").withColor(ArcaneUi.WELL_MUTED));
            out.add(Component.translatable("tooltip.arcane.know.learn", ArcaneUi.masteryName(teaches.tier())).withStyle(ChatFormatting.GREEN));
        } else {
            out.add(Component.translatable("tooltip.arcane.know.tier", ArcaneUi.masteryName(now.tier())).withColor(ArcaneUi.tierColor(now.tier())));
            if (teaches.tier() > now.tier()) {
                out.add(Component.translatable("tooltip.arcane.know.raise", ArcaneUi.masteryName(teaches.tier())).withStyle(ChatFormatting.GREEN));
            } else {
                out.add(Component.translatable("tooltip.arcane.know.nothing").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        Mastery shown = teaches.tier() > now.tier() ? teaches : now;
        out.add(Component.translatable("tooltip.arcane.know.effect", ArcaneUi.masteryName(shown.tier()), shown.refinements(), shown.rollBonus())
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void gear(List<Component> out, Player player, ItemStack stack) {
        List<DeconstructionMenu.Lesson> lessons = DeconstructionMenu.lessonsFor(player, stack);
        out.add(Component.empty());
        int knownCount = 0;
        for (DeconstructionMenu.Lesson l : lessons) if (KnowledgeManager.mastery(player, l.id()).known()) knownCount++;
        out.add(Component.translatable("tooltip.arcane.know.summary", knownCount, lessons.size())
                .withColor(knownCount == lessons.size() ? ArcaneUi.WELL_MUTED : ArcaneUi.CYAN));
        if (!forceDetail && !Minecraft.getInstance().hasShiftDown()) {
            out.add(Component.translatable("tooltip.arcane.know.shift").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        for (DeconstructionMenu.Lesson l : lessons) {
            Component name = TomeItem.enchantmentName(l.id());
            Mastery now = KnowledgeManager.mastery(player, l.id());
            if (now.known()) {
                out.add(Component.literal(" ").append(Component.translatable("tooltip.arcane.know.line.known", name, ArcaneUi.masteryName(now.tier())))
                        .withColor(ArcaneUi.tierColor(now.tier())));
            } else {
                out.add(Component.literal(" ").append(Component.translatable("tooltip.arcane.know.line.unknown", name)).withStyle(ChatFormatting.GRAY));
            }
            if (l.gain()) {
                out.add(Component.translatable("tooltip.arcane.know.line.teach", ArcaneUi.masteryName(l.result().tier())).withStyle(ChatFormatting.GREEN));
            }
        }
    }
}
