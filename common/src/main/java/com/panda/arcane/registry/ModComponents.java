package com.panda.arcane.registry;

import com.panda.arcane.ArcaneMod;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import com.panda.arcane.platform.registry.DeferredRegister;

public final class ModComponents {
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ArcaneMod.MODID);

    /** Which enchantment a tome teaches. */
    public static final Supplier<DataComponentType<Identifier>> TOME_ENCHANTMENT = COMPONENTS.registerComponentType("tome_enchantment",
            b -> b.persistent(Identifier.CODEC).networkSynchronized(Identifier.STREAM_CODEC));

    private ModComponents() {}

}
