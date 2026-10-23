package com.panda.arcane.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panda.arcane.ArcaneMod;
import com.panda.arcane.item.TomeItem;
import com.panda.arcane.registry.ModComponents;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Picks the enchantment a looted tome teaches from a rarity pool, so enchantments added by other mods drop too.
 * A pool is the tag {@code arcane:tome_pool/<category>} plus, for foreign enchantments no pool tag mentions, an automatic
 * guess from vanilla's own signals (treasure tag, rarity weight). Curses and {@code arcane:tome_pool/excluded} never drop.
 * The pick is weighted by each enchantment's own rarity weight.
 */
public final class RandomTomeFunction extends LootItemConditionalFunction {
    public static final MapCodec<RandomTomeFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> commonFields(i)
            .and(Category.CODEC.fieldOf("category").forGetter(f -> f.category))
            .apply(i, RandomTomeFunction::new));

    public static final TagKey<Enchantment> EXCLUDED = TagKey.create(Registries.ENCHANTMENT, ArcaneMod.id("tome_pool/excluded"));

    public enum Category implements StringRepresentable {
        COMMON, UNCOMMON, RARE, EXCLUSIVE;

        public static final com.mojang.serialization.Codec<Category> CODEC = StringRepresentable.fromEnum(Category::values);

        public final TagKey<Enchantment> tag = TagKey.create(Registries.ENCHANTMENT, ArcaneMod.id("tome_pool/" + getSerializedName()));

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private final Category category;

    private RandomTomeFunction(Optional<Holder<LootItemCondition>> condition, Category category) {
        super(condition);
        this.category = category;
    }

    @Override
    public MapCodec<RandomTomeFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        if (!(stack.getItem() instanceof TomeItem)) return stack;
        List<Holder.Reference<Enchantment>> pool = candidates(context.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT), category);
        Holder.Reference<Enchantment> pick = pickWeighted(pool, context.getRandom());
        if (pick == null) {
            ArcaneMod.LOGGER.warn("No enchantment available for a {} tome", category.getSerializedName());
            return stack;
        }
        stack.set(ModComponents.TOME_ENCHANTMENT.get(), pick.key().identifier());
        return stack;
    }

    /** Every enchantment a tome of this category may teach. Public for tests and the Codex. */
    public static List<Holder.Reference<Enchantment>> candidates(HolderLookup.RegistryLookup<Enchantment> lookup, Category category) {
        List<Holder.Reference<Enchantment>> out = new ArrayList<>();
        lookup.listElements().forEach(ref -> {
            if (ref.is(EXCLUDED) || ref.is(EnchantmentTags.CURSE)) return;
            if (ref.is(category.tag) || (!pooled(ref) && guess(ref) == category)) out.add(ref);
        });
        return out;
    }

    private static boolean pooled(Holder<Enchantment> holder) {
        for (Category c : Category.values()) if (holder.is(c.tag)) return true;
        return false;
    }

    /** Where an enchantment nobody sorted would go. Only foreign enchantments are guessed: ours and vanilla's are curated. */
    static Category guess(Holder.Reference<Enchantment> ref) {
        String ns = ref.key().identifier().getNamespace();
        if (ns.equals("minecraft") || ns.equals(ArcaneMod.MODID)) return null;
        if (ref.is(EnchantmentTags.TREASURE)) return Category.RARE;
        return ref.value().getWeight() >= 5 ? Category.COMMON : Category.UNCOMMON;
    }

    private static Holder.Reference<Enchantment> pickWeighted(List<Holder.Reference<Enchantment>> pool, RandomSource random) {
        int total = 0;
        for (var ref : pool) total += Math.max(1, ref.value().getWeight());
        if (total == 0) return null;
        int roll = random.nextInt(total);
        for (var ref : pool) {
            roll -= Math.max(1, ref.value().getWeight());
            if (roll < 0) return ref;
        }
        return null;
    }
}
