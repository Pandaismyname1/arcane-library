package com.panda.arcane.lectern;

import com.panda.arcane.knowledge.Knowledge;
import com.panda.arcane.knowledge.KnowledgeManager;
import com.panda.arcane.logic.Mastery;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** What a given item can be inscribed with, given what the player knows. Shared by server and client. */
public final class LecternOptions {
    private LecternOptions() {}

    public record Option(Holder<Enchantment> holder, Identifier id, int maxLevel, int weight, Mastery mastery,
                         int currentLevel) {
        public boolean upgradeable() {
            return currentLevel < maxLevel;
        }
    }

    public static boolean acceptsItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK)) return true;
        return stack.has(DataComponents.ENCHANTABLE) && EnchantmentHelper.canStoreEnchantments(stack);
    }

    public static List<Option> compute(RegistryAccess access, ItemStack stack, Knowledge knowledge) {
        List<Option> out = new ArrayList<>();
        if (!acceptsItem(stack)) return out;
        ItemEnchantments existing = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        boolean book = stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK);
        access.lookupOrThrow(Registries.ENCHANTMENT).listElements().forEach(ref -> {
            Holder<Enchantment> holder = ref;
            Identifier id = ref.key().identifier();
            Mastery mastery = knowledge.mastery(id);
            if (!mastery.known()) return;
            if (!book && !holder.value().isSupportedItem(stack)) return;
            for (Holder<Enchantment> other : existing.keySet()) {
                if (!other.equals(holder) && !Enchantment.areCompatible(holder, other)) return;
            }
            int current = existing.getLevel(holder);
            Option option = new Option(holder, id, holder.value().getMaxLevel(), holder.value().getWeight(), mastery, current);
            if (option.upgradeable()) out.add(option);
        });
        out.sort(Comparator.comparing((Option o) -> o.id().toString()));
        return out;
    }

    public static List<Option> compute(net.minecraft.world.entity.player.Player player, ItemStack stack) {
        return compute(player.level().registryAccess(), stack, KnowledgeManager.get(player));
    }
}
