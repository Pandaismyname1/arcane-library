package com.panda.arcane.registry;

import com.panda.arcane.ArcaneMod;
import com.panda.arcane.lectern.ArcaneLecternMenu;
import com.panda.arcane.lectern.DeconstructionMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import com.panda.arcane.platform.registry.DeferredHolder;
import com.panda.arcane.platform.registry.DeferredRegister;

public final class ModMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ArcaneMod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ArcaneLecternMenu>> LECTERN = MENUS.register("lectern",
            () -> new MenuType<>(ArcaneLecternMenu::new, FeatureFlags.VANILLA_SET));
    public static final DeferredHolder<MenuType<?>, MenuType<DeconstructionMenu>> DECONSTRUCTION = MENUS.register("deconstruction",
            () -> new MenuType<>(DeconstructionMenu::new, FeatureFlags.VANILLA_SET));

    private ModMenus() {}

}
