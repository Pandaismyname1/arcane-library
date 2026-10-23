package com.panda.arcane.registry;

import com.panda.arcane.ArcaneMod;
import com.panda.arcane.item.TomeItem;
import com.panda.arcane.logic.Mastery;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModTabs {
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ArcaneMod.MODID);

    public static final Supplier<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.arcane"))
            .icon(() -> new ItemStack(ModItems.TOME_STUDIED.get()))
            .displayItems((params, output) -> {
                ModItems.ITEMS.getEntries().forEach(h -> {
                    if (!(h.get() instanceof TomeItem)) output.accept(h.get());
                });
                for (Mastery mastery : new Mastery[] {Mastery.GLIMPSED, Mastery.STUDIED, Mastery.MASTERED}) {
                    params.holders().lookup(Registries.ENCHANTMENT).ifPresent(lookup -> lookup.listElements().forEach(ref -> {
                        Holder<Enchantment> ignored = ref;
                        output.accept(TomeItem.create(ModItems.tome(mastery).get(), ref.key().identifier()));
                    }));
                }
            })
            .build());

    private ModTabs() {}

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }
}
