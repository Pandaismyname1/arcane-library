package com.panda.arcane.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** "Enchantment learned!" toast, on vanilla's advancement toast frame so it sits with the others. */
public class LearnedToast implements Toast {
    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("toast/advancement");
    private static final long DISPLAY_MS = 5000L;

    private final Component title;
    private final Component line;
    private final ItemStack icon;
    private final int tier;
    private final Object token;
    private Toast.Visibility visibility = Toast.Visibility.SHOW;

    public LearnedToast(Component title, Component line, ItemStack icon, int tier, Object token) {
        this.title = title;
        this.line = line;
        this.icon = icon;
        this.tier = tier;
        this.token = token;
    }

    @Override
    public Toast.Visibility getWantedVisibility() {
        return visibility;
    }

    @Override
    public void update(ToastManager manager, long fullyVisibleForMs) {
        visibility = fullyVisibleForMs >= DISPLAY_MS * manager.getNotificationDisplayTimeMultiplier() ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, Font font, long fullyVisibleForMs) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, 0, 0, width(), height());
        g.text(font, title, 30, 7, ArcaneUi.tierColor(tier), false);
        g.text(font, Component.literal(ArcaneUi.fit(font, line.getString(), width() - 34)), 30, 18, 0xFFFFFFFF, false);
        g.fakeItem(icon, 8, 8);

    }

    @Override
    public Object getToken() {
        return token;
    }
}
