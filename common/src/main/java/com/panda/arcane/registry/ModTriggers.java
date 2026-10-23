package com.panda.arcane.registry;

import com.panda.arcane.ArcaneMod;
import com.panda.arcane.advancement.ArcaneTrigger;
import java.util.function.Supplier;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import com.panda.arcane.platform.registry.DeferredRegister;

public final class ModTriggers {
    private static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, ArcaneMod.MODID);

    public static final Supplier<ArcaneTrigger> ARCANE = TRIGGERS.register("event", ArcaneTrigger::new);

    private ModTriggers() {}

}
