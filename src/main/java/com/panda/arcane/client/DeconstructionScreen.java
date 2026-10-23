package com.panda.arcane.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.panda.arcane.lectern.DeconstructionMenu;
import com.panda.arcane.logic.Mastery;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Deconstruction Table: shows exactly what breaking the item down will teach and return, before you commit. */
public class DeconstructionScreen extends AbstractContainerScreen<DeconstructionMenu> {
    // layout (matches the screen texture)
    static final int W = 176, H = 214;
    static final int PREVIEW_X = 43, PREVIEW_Y = 17, PREVIEW_W = 125, PREVIEW_H = 77;
    static final int BTN_X = 42, BTN_Y = 98, BTN_W = 127, BTN_H = 20;
    static final int ROW_Y = 16, ROW_H = 11, MAX_ROWS = 4;

    private int ticks;

    public DeconstructionScreen(DeconstructionMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 121;
    }

    /** New enchantments first, then deepened ones, then what you already know; a stable order inside each group. */
    private List<DeconstructionMenu.Lesson> sortedLessons() {
        List<DeconstructionMenu.Lesson> out = new ArrayList<>(menu.lessons());
        out.sort(java.util.Comparator.comparingInt((DeconstructionMenu.Lesson l) -> !l.gain() ? 2 : l.result() == Mastery.GLIMPSED ? 0 : 1)
                .thenComparing(l -> l.id().toString()));
        return out;
    }

    private boolean ready() {
        return DeconstructionMenu.hasEnchantments(menu.item());
    }

    public int[] buttonBounds() {
        return new int[] {leftPos + BTN_X, topPos + BTN_Y, BTN_W, BTN_H};
    }

    private int[] rowBounds(int i) {
        return new int[] {leftPos + PREVIEW_X, topPos + PREVIEW_Y + ROW_Y + i * ROW_H - 1, PREVIEW_W, ROW_H};
    }

    private static boolean in(double mx, double my, int[] b) {
        return ArcaneUi.inside(mx, my, b[0], b[1], b[2], b[3]);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        ticks++;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partial) {
        super.extractBackground(g, mx, my, partial);
        int x = leftPos, y = topPos;
        ArcaneUi.background(g, ArcaneUi.DECONSTRUCTION_BG, x, y, W, H, 256, 256);
        if (!menu.getSlot(0).hasItem()) {
            Identifier[] ghosts = {Identifier.withDefaultNamespace("container/slot/sword"), Identifier.withDefaultNamespace("container/slot/chestplate"),
                    Identifier.withDefaultNamespace("container/slot/pickaxe"), ArcaneUi.GHOST_BOOK};
            ArcaneUi.sprite(g, ghosts[(ticks / 30) % ghosts.length], x + menu.getSlot(0).x, y + menu.getSlot(0).y, 16, 16);
        }
        int px = x + PREVIEW_X + 4, py = y + PREVIEW_Y, pw = PREVIEW_W - 8;
        if (!ready()) {
            g.textWithWordWrap(font, Component.translatable("screen.arcane.deconstruction.empty"), px, py + 6, pw, ArcaneUi.WELL_MUTED, false);
        } else {
            List<DeconstructionMenu.Lesson> lessons = sortedLessons();
            boolean anyGain = lessons.stream().anyMatch(DeconstructionMenu.Lesson::gain);
            g.text(font, Component.translatable(anyGain ? "screen.arcane.deconstruction.teaches" : "screen.arcane.deconstruction.summary.none"),
                    px, py + 4, ArcaneUi.WELL_MUTED, false);
            int shown = lessons.size() > MAX_ROWS ? MAX_ROWS - 1 : lessons.size();
            for (int i = 0; i < shown; i++) drawLesson(g, lessons.get(i), px, py + ROW_Y + i * ROW_H, pw);
            if (shown < lessons.size()) {
                g.text(font, Component.translatable("screen.arcane.deconstruction.more", lessons.size() - shown), px + 18,
                        py + ROW_Y + shown * ROW_H, ArcaneUi.WELL_MUTED, false);
            }
            ArcaneUi.rule(g, px, py + 62, pw, ArcaneUi.WELL_LINE);
            Component returns = Component.translatable("screen.arcane.deconstruction.returns");
            g.text(font, returns, px, py + 67, ArcaneUi.WELL_MUTED, false);
            int dx = px + font.width(returns) + 4;
            ArcaneUi.sprite(g, ArcaneUi.DUST, dx, py + 66, 9, 9);
            g.text(font, "x " + menu.dustYield(), dx + 11, py + 67, ArcaneUi.GOLD, true);
        }
        boolean can = ready();
        int[] b = buttonBounds();
        ArcaneUi.button(g, font, Component.translatable("screen.arcane.deconstruction.go"), b[0], b[1], b[2], b[3], can,
                can && in(mx, my, b), 0, true);
    }

    private void drawLesson(GuiGraphicsExtractor g, DeconstructionMenu.Lesson l, int x, int y, int w) {
        // [resulting tier gems] name ........ [tag icon]: green sparkle = new, chevron = deeper, nothing = known
        int tier = l.result().tier();
        ArcaneUi.sprite(g, ArcaneUi.tierH(tier), x, y + 2, 15, 5, l.gain() ? 0xFFFFFFFF : 0x80FFFFFF);
        String name = ArcaneUi.fit(font, Component.translatable(l.id().toLanguageKey("enchantment")).getString(), w - 18 - 10);
        g.text(font, name, x + 18, y, l.gain() ? ArcaneUi.WELL_TEXT : ArcaneUi.WELL_MUTED, false);
        if (l.gain() && l.result() == Mastery.GLIMPSED) ArcaneUi.sprite(g, ArcaneUi.NEW, x + w - 7, y, 7, 7);
        else if (l.gain()) ArcaneUi.sprite(g, ArcaneUi.UP, x + w - 7, y, 7, 7, ArcaneUi.tierColor(tier));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        g.text(font, title, titleLabelX, titleLabelY, ArcaneUi.LABEL, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, ArcaneUi.LABEL, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
        super.extractRenderState(g, mx, my, partial);
        if (!menu.getCarried().isEmpty()) return;
        if (in(mx, my, buttonBounds())) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("screen.arcane.deconstruction.tip.go").withStyle(ChatFormatting.GRAY));
            if (!ready()) lines.add(Component.translatable("screen.arcane.deconstruction.reason.empty").withStyle(ChatFormatting.RED));
            g.setComponentTooltipForNextFrame(font, lines, mx, my);
            return;
        }
        if (!ready()) return;
        List<DeconstructionMenu.Lesson> lessons = sortedLessons();
        int shown = lessons.size() > MAX_ROWS ? MAX_ROWS - 1 : lessons.size();
        for (int i = 0; i < shown; i++) {
            if (in(mx, my, rowBounds(i))) {
                g.setTooltipForNextFrame(font, lessonText(lessons.get(i)), mx, my);
                return;
            }
        }
        if (shown < lessons.size() && in(mx, my, rowBounds(shown))) {
            List<Component> lines = new ArrayList<>();
            for (int i = shown; i < lessons.size(); i++) lines.add(lessonText(lessons.get(i)));
            g.setComponentTooltipForNextFrame(font, lines, mx, my);
        }
    }

    private Component lessonText(DeconstructionMenu.Lesson l) {
        Component name = Component.translatable(l.id().toLanguageKey("enchantment"));
        Mastery now = com.panda.arcane.knowledge.KnowledgeManager.mastery(minecraft.player, l.id());
        if (!l.gain()) return Component.translatable("screen.arcane.deconstruction.tip.known", name, ArcaneUi.masteryName(now.tier()));
        if (!now.known()) return Component.translatable("screen.arcane.deconstruction.tip.new", name, ArcaneUi.masteryName(l.result().tier()));
        return Component.translatable("screen.arcane.deconstruction.tip.raise", name, ArcaneUi.masteryName(now.tier()), ArcaneUi.masteryName(l.result().tier()));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && menu.getCarried().isEmpty() && in(event.x(), event.y(), buttonBounds())) {
            if (ready()) {
                ArcaneUi.click();
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, DeconstructionMenu.BTN_DECONSTRUCT);
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
