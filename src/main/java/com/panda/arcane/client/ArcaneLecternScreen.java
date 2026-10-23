package com.panda.arcane.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.panda.arcane.lectern.ArcaneLecternMenu;
import com.panda.arcane.lectern.LecternOptions;
import com.panda.arcane.logic.Mastery;
import com.panda.arcane.logic.Roll;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * The Arcane Lectern. Everything the screen shows is derived from the menu's synced slots and DataSlots; every action
 * is a {@code clickMenuButton} id that the server validates. The screen only adds presentation: an animated reveal
 * of a roll the server already made, and explanations of why an action is unavailable.
 */
public class ArcaneLecternScreen extends AbstractContainerScreen<ArcaneLecternMenu> {
    // layout (matches the screen texture)
    static final int W = 270, H = 226;
    static final int LIST_X = 43, LIST_Y = 17, LIST_W = 104, ROW_H = 18, ROWS = 6;
    static final int SCROLL_X = 150;
    static final int DETAIL_X = 159, DETAIL_Y = 17, DETAIL_W = 103, DETAIL_H = 82;
    static final int BTN_X = 158, BTN_Y = 103, BTN_W = 105, BTN_H = 20, BTN_GAP = 3;
    static final int HALF_W = 58; // refine (label + cost) is wider than accept

    private int scroll;
    private int ticks;
    private int lastRollCounter;
    private int lastPending;
    private RollReveal reveal;
    private float revealStart;
    private int chimed;

    public ArcaneLecternScreen(ArcaneLecternMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 54;
        this.inventoryLabelY = 131;
    }

    // ---- state -------------------------------------------------------------------------------------------------

    @Override
    public void containerTick() {
        super.containerTick();
        ticks++;
        int rc = menu.rollCounter();
        int pending = menu.pendingPower();
        if (rc != lastRollCounter) {
            LecternOptions.Option o = menu.selectedOption();
            if (rc > lastRollCounter && o != null && pending > 0) {
                reveal = new RollReveal(lastPending, pending, o.maxLevel(), rc * 31L + ticks);
                revealStart = ticks;
                chimed = 0;
            }
            lastRollCounter = rc;
        }
        if (reveal != null && pending != reveal.target()) reveal = null; // selection changed or accepted
        if (reveal == null || reveal.done(ticks - revealStart)) lastPending = pending;
    }

    private float animTime(float partial) {
        return reveal == null ? Float.MAX_VALUE : ticks - revealStart + partial;
    }

    private boolean animating(float partial) {
        return reveal != null && !reveal.revealed(animTime(partial));
    }

    /** Why an action is unavailable, or null when it can be done. Mirrors the server's checks for display only. */
    private Component inscribeBlocked(LecternOptions.Option o) {
        if (o == null) return Component.translatable("screen.arcane.lectern.reason.choose");
        int cost = menu.inscribeCost(o);
        if (!menu.freeMode() && menu.dustCount() < cost) {
            return Component.translatable("screen.arcane.lectern.reason.dust", cost, menu.dustCount());
        }
        return null;
    }

    private Component refineBlocked(LecternOptions.Option o) {
        if (animating(0)) return Component.translatable("screen.arcane.lectern.reason.rolling");
        if (menu.pendingPower() >= o.maxLevel()) return Component.translatable("screen.arcane.lectern.reason.max");
        if (menu.refinesUsed() >= menu.maxRefines(o)) return Component.translatable("screen.arcane.lectern.reason.norefines");
        int cost = menu.refineCost();
        if (!menu.freeMode() && menu.dustCount() < cost) {
            return Component.translatable("screen.arcane.lectern.reason.dust", cost, menu.dustCount());
        }
        return null;
    }

    private Component acceptBlocked(LecternOptions.Option o) {
        if (animating(0)) return Component.translatable("screen.arcane.lectern.reason.rolling");
        if (menu.pendingPower() <= o.currentLevel()) {
            return Component.translatable("screen.arcane.lectern.reason.notbetter", ArcaneUi.roman(o.currentLevel()));
        }
        return null;
    }

    // ---- bounds (public so tests can click with real input) ---------------------------------

    public int[] rowBounds(int visibleRow) {
        return new int[] {leftPos + LIST_X, topPos + LIST_Y + visibleRow * ROW_H, LIST_W, ROW_H};
    }

    /** Visible row showing this enchantment, or -1 when it is scrolled out of view or not offered. */
    public int visibleRowOf(Identifier id) {
        List<LecternOptions.Option> options = menu.options();
        for (int r = 0; r < ROWS && scroll + r < options.size(); r++) {
            if (options.get(scroll + r).id().equals(id)) return r;
        }
        return -1;
    }

    public int[] inscribeBounds() {
        return new int[] {leftPos + BTN_X, topPos + BTN_Y, BTN_W, BTN_H};
    }

    public int[] refineBounds() {
        return new int[] {leftPos + BTN_X, topPos + BTN_Y, HALF_W, BTN_H};
    }

    public int[] acceptBounds() {
        return new int[] {leftPos + BTN_X + HALF_W + BTN_GAP, topPos + BTN_Y, BTN_W - HALF_W - BTN_GAP, BTN_H};
    }

    private int[] dustBounds() {
        String count = dustText();
        int w = 11 + font.width(count);
        return new int[] {leftPos + W - 8 - w, topPos + 4, w, 10};
    }

    private static boolean in(double mx, double my, int[] b) {
        return ArcaneUi.inside(mx, my, b[0], b[1], b[2], b[3]);
    }

    private String dustText() {
        return menu.freeMode() ? "-" : Integer.toString(menu.dustCount());
    }

    // ---- drawing -----------------------------------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partial) {
        super.extractBackground(g, mx, my, partial);
        int x = leftPos, y = topPos;
        ArcaneUi.background(g, ArcaneUi.LECTERN_BG, x, y, W, H, 512, 256);
        drawGhosts(g);
        drawDust(g);
        drawList(g, mx, my);
        drawDetails(g, partial);
        drawButtons(g, mx, my, partial);
    }

    private void drawGhosts(GuiGraphicsExtractor g) {
        Slot item = menu.getSlot(ArcaneLecternMenu.SLOT_ITEM);
        Slot dust = menu.getSlot(ArcaneLecternMenu.SLOT_DUST);
        Slot quill = menu.getSlot(ArcaneLecternMenu.SLOT_QUILL);
        if (!item.hasItem()) {
            // cycle through what the slot takes, like the smithing table
            Identifier[] ghosts = {Identifier.withDefaultNamespace("container/slot/sword"), Identifier.withDefaultNamespace("container/slot/pickaxe"),
                    Identifier.withDefaultNamespace("container/slot/chestplate"), ArcaneUi.GHOST_BOOK};
            ArcaneUi.sprite(g, ghosts[(ticks / 30) % ghosts.length], leftPos + item.x, topPos + item.y, 16, 16);
        }
        if (!dust.hasItem()) ArcaneUi.sprite(g, ArcaneUi.GHOST_DUST, leftPos + dust.x, topPos + dust.y, 16, 16);
        if (!quill.hasItem()) ArcaneUi.sprite(g, ArcaneUi.GHOST_QUILL, leftPos + quill.x, topPos + quill.y, 16, 16);
    }

    private void drawDust(GuiGraphicsExtractor g) {
        int[] b = dustBounds();
        LecternOptions.Option o = menu.selectedOption();
        int next = o == null ? 0 : menu.pendingPower() == 0 ? menu.inscribeCost(o) : menu.refineCost();
        boolean short_ = !menu.freeMode() && o != null && menu.dustCount() < next;
        ArcaneUi.sprite(g, menu.dustCount() > 0 || menu.freeMode() ? ArcaneUi.DUST : ArcaneUi.DUST_OFF, b[0], b[1], 9, 9);
        g.text(font, dustText(), b[0] + 11, b[1] + 1, short_ ? 0xFF931D2A : ArcaneUi.LABEL, false);
    }

    private void drawList(GuiGraphicsExtractor g, int mx, int my) {
        List<LecternOptions.Option> options = menu.options();
        int maxScroll = Math.max(0, options.size() - ROWS);
        scroll = Math.max(0, Math.min(maxScroll, scroll));
        int lx = leftPos + LIST_X, ly = topPos + LIST_Y;
        if (options.isEmpty()) {
            Component msg = menu.targetItem().isEmpty() ? Component.translatable("screen.arcane.lectern.empty.noitem")
                    : Component.translatable("screen.arcane.lectern.empty.none");
            g.textWithWordWrap(font, msg, lx + 6, ly + 8, LIST_W - 12, ArcaneUi.WELL_BROWN_TEXT, false);
        }
        int sel = menu.selectedIndex();
        for (int r = 0; r < ROWS; r++) {
            int idx = scroll + r;
            if (idx >= options.size()) break;
            LecternOptions.Option o = options.get(idx);
            int rx = lx, ry = ly + r * ROW_H;
            boolean selected = idx == sel;
            boolean hover = !selected && ArcaneUi.inside(mx, my, rx, ry, LIST_W, ROW_H);
            ArcaneUi.sprite(g, selected ? ArcaneUi.ROW_SELECTED : hover ? ArcaneUi.ROW_HOVER : ArcaneUi.ROW, rx, ry, LIST_W, ROW_H);
            ArcaneUi.sprite(g, ArcaneUi.tierV(o.mastery().tier()), rx + 6, ry + 2, 5, 15);
            String name = ArcaneUi.fit(font, o.holder().value().description().getString(), LIST_W - 19);
            int color = selected ? ArcaneUi.ROW_TEXT_SELECTED : hover ? ArcaneUi.ROW_TEXT_HOVER : ArcaneUi.ROW_TEXT;
            g.text(font, name, rx + 15, ry + 5, color, selected);
        }
        if (maxScroll > 0) {
            int trackH = ROWS * ROW_H;
            int thumbH = Math.max(15, trackH * ROWS / options.size());
            int thumbY = ly + (trackH - thumbH) * scroll / maxScroll;
            ArcaneUi.sprite(g, ArcaneUi.SCROLLER, leftPos + SCROLL_X, thumbY, 6, thumbH);
        }
    }

    private void drawDetails(GuiGraphicsExtractor g, float partial) {
        int dx = leftPos + DETAIL_X + 4, dy = topPos + DETAIL_Y, dw = DETAIL_W - 8;
        LecternOptions.Option o = menu.selectedOption();
        if (o == null) {
            // a short how-to: three numbered steps, the current one highlighted
            int step = menu.targetItem().isEmpty() || menu.options().isEmpty() ? 0 : 1;
            int ly = dy + 6;
            for (int i = 1; i <= 3; i++) {
                boolean current = i == step;
                ArcaneUi.sprite(g, ArcaneUi.pip(current ? "max" : "full", true), dx, ly + 1, 7, 7);
                for (FormattedCharSequence line : font.split(Component.translatable("screen.arcane.lectern.howto." + i), dw - 10)) {
                    g.text(font, line, dx + 10, ly, current ? ArcaneUi.GOLD : ArcaneUi.WELL_MUTED, false);
                    ly += 10;
                }
                ly += 4;
            }
            return;
        }
        int tier = o.mastery().tier();
        g.text(font, ArcaneUi.fit(font, o.holder().value().description().getString(), dw), dx, dy + 4, ArcaneUi.WELL_TEXT, true);
        ArcaneUi.sprite(g, ArcaneUi.tierH(tier), dx, dy + 17, 15, 5);
        g.text(font, ArcaneUi.masteryName(tier), dx + 18, dy + 15, ArcaneUi.tierColor(tier), false);
        ArcaneUi.rule(g, dx, dy + 27, dw, ArcaneUi.WELL_LINE);

        // power pips: the roll, or an empty row of sockets to show the range
        float t = animTime(partial);
        int pending = menu.pendingPower();
        int max = o.maxLevel();
        int count = Math.min(max, 10);
        boolean small = count > 6;
        int pip = small ? 7 : 11, gap = small ? 2 : 3;
        int pipsW = count * pip + (count - 1) * gap;
        int px = leftPos + DETAIL_X + (DETAIL_W - pipsW) / 2, py = dy + 33 + (small ? 2 : 0);
        boolean spinning = reveal != null && reveal.spinning(t);
        int lit = reveal == null ? pending : spinning ? reveal.spinValue(t) : reveal.litPips(t);
        boolean revealedNow = reveal == null || reveal.revealed(t);
        if (reveal != null && !spinning && lit > chimed) {
            chimed = lit;
            ArcaneUi.play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F + 0.9F * lit / Math.max(1, max), 0.6F);
        }
        boolean atMax = pending >= max && revealedNow;
        for (int i = 0; i < count; i++) {
            int x = px + i * (pip + gap);
            String kind = i < lit ? (spinning ? "flicker" : atMax ? "max" : "full") : "empty";
            ArcaneUi.sprite(g, ArcaneUi.pip(kind, small), x, py, pip, pip);
            if (i < o.currentLevel()) ArcaneUi.sprite(g, ArcaneUi.pip("current", small), x - 1, py - 1, pip + 2, pip + 2);
        }
        float glow = reveal == null ? 0F : reveal.glow(t);
        if (glow > 0.02F && lit > 0) {
            int a = (int) (glow * 140);
            int col = (a << 24) | (atMax ? 0xFFD36B : 0xB69CFF);
            int gx0 = px - 3, gx1 = px + lit * (pip + gap) - gap + 3;
            g.fill(gx0, py - 3, gx1, py - 2, col);
            g.fill(gx0, py + pip + 2, gx1, py + pip + 3, col);
            g.fill(gx0 - 1, py - 2, gx0, py + pip + 2, col);
            g.fill(gx1, py - 2, gx1 + 1, py + pip + 2, col);
        }

        // result line
        int cx = leftPos + DETAIL_X + DETAIL_W / 2;
        int resultY = dy + 50;
        if (spinning || (reveal != null && !revealedNow)) {
            g.centeredText(font, Component.translatable("screen.arcane.lectern.rolling"), cx, resultY, ArcaneUi.WELL_MUTED);
        } else if (pending > 0) {
            Component name = Enchantment.getFullname(o.holder(), pending);
            String s = ArcaneUi.fit(font, name.getString(), dw);
            g.centeredText(font, Component.literal(s), cx, resultY, atMax ? ArcaneUi.GOLD : ArcaneUi.WELL_TEXT);
        } else {
            g.centeredText(font, Component.translatable("screen.arcane.lectern.range", ArcaneUi.roman(max)), cx, resultY, ArcaneUi.WELL_MUTED);
        }

        // info line: chance before rolling, outcome after
        Component info;
        int infoColor = ArcaneUi.WELL_MUTED;
        if (pending == 0) {
            int pct = (int) Math.round(100 * Roll.chanceAtLeast(max, o.mastery(), menu.quillBonus(), max));
            info = Component.translatable("screen.arcane.lectern.chance.full", pct);
        } else if (!revealedNow) {
            info = null;
        } else if (atMax) {
            info = Component.translatable("screen.arcane.lectern.fullpower");
            infoColor = ArcaneUi.GOLD;
        } else if (reveal != null && reveal.refine() && !reveal.improved()) {
            info = Component.translatable("screen.arcane.lectern.kept", ArcaneUi.roman(pending));
        } else if (reveal != null && reveal.refine()) {
            info = Component.translatable("screen.arcane.lectern.improved", ArcaneUi.roman(reveal.previous()));
            infoColor = ArcaneUi.GREEN;
        } else {
            int pct = (int) Math.round(100 * Roll.chanceAtLeast(max, o.mastery(), menu.quillBonus(), max));
            info = Component.translatable("screen.arcane.lectern.chance.full", pct);
        }
        if (info != null) g.centeredText(font, Component.literal(ArcaneUi.fit(font, info.getString(), dw)), cx, dy + 61, infoColor);

        // status line: refinements left, or why the next action is blocked
        int sy = dy + 72;
        int need = pending == 0 ? menu.inscribeCost(o) : menu.refineCost();
        boolean dustShort = !menu.freeMode() && menu.dustCount() < need
                && (pending == 0 || (revealedNow && pending < max && menu.refinesUsed() < menu.maxRefines(o)));
        if (dustShort) {
            Component reason = Component.translatable("screen.arcane.lectern.reason.dust.short", need);
            ArcaneUi.sprite(g, ArcaneUi.DUST_OFF, dx, sy - 1, 9, 9);
            g.text(font, ArcaneUi.fit(font, reason.getString(), dw - 11), dx + 11, sy, ArcaneUi.RED, false);
        } else {
            // refinements left as small gems; the one the quill adds is cyan
            Component label = Component.translatable("screen.arcane.lectern.refines.left");
            g.text(font, label, dx, sy, ArcaneUi.WELL_MUTED, false);
            int total = menu.maxRefines(o), used = menu.refinesUsed();
            int rx = dx + font.width(label) + 4;
            for (int i = 0; i < total; i++) {
                boolean left = i >= used;
                boolean quill = menu.hasQuill() && i == total - 1;
                ArcaneUi.sprite(g, ArcaneUi.pip(!left ? "empty" : quill ? "quill" : "full", true), rx + i * 9, sy, 7, 7);
            }
        }
    }

    private void drawButtons(GuiGraphicsExtractor g, int mx, int my, float partial) {
        LecternOptions.Option o = menu.selectedOption();
        boolean free = menu.freeMode();
        if (o == null || menu.pendingPower() == 0) {
            int cost = o == null ? 0 : menu.inscribeCost(o);
            boolean can = inscribeBlocked(o) == null;
            int[] b = inscribeBounds();
            ArcaneUi.button(g, font, Component.translatable("screen.arcane.lectern.inscribe"), b[0], b[1], b[2], b[3], can,
                    can && in(mx, my, b), free ? 0 : cost, free || menu.dustCount() >= cost);
        } else {
            boolean canRefine = refineBlocked(o) == null;
            boolean canAccept = acceptBlocked(o) == null;
            boolean refinable = menu.refinesUsed() < menu.maxRefines(o) && menu.pendingPower() < o.maxLevel();
            int[] r = refineBounds(), a = acceptBounds();
            ArcaneUi.button(g, font, Component.translatable("screen.arcane.lectern.refine"), r[0], r[1], r[2], r[3], canRefine,
                    canRefine && in(mx, my, r), free || !refinable ? 0 : menu.refineCost(), free || menu.dustCount() >= menu.refineCost());
            ArcaneUi.button(g, font, Component.translatable("screen.arcane.lectern.accept"), a[0], a[1], a[2], a[3], canAccept,
                    canAccept && in(mx, my, a), 0, true);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        g.text(font, title, titleLabelX, titleLabelY, ArcaneUi.LABEL, false);
        g.text(font, Component.translatable("screen.arcane.lectern.inventory"), inventoryLabelX, inventoryLabelY, ArcaneUi.LABEL, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
        super.extractRenderState(g, mx, my, partial);
        tooltips(g, mx, my);
    }

    private void tooltips(GuiGraphicsExtractor g, int mx, int my) {
        if (!menu.getCarried().isEmpty()) return;
        List<LecternOptions.Option> options = menu.options();
        for (int r = 0; r < ROWS; r++) {
            int idx = scroll + r;
            if (idx >= options.size()) break;
            if (in(mx, my, rowBounds(r))) {
                g.setComponentTooltipForNextFrame(font, optionTooltip(options.get(idx)), mx, my);
                return;
            }
        }
        LecternOptions.Option o = menu.selectedOption();
        if (o == null || menu.pendingPower() == 0) {
            if (in(mx, my, inscribeBounds())) {
                List<Component> lines = new ArrayList<>();
                Component reason = inscribeBlocked(o);
                if (o != null) lines.add(Component.translatable("screen.arcane.lectern.tip.inscribe", menu.inscribeCost(o)));
                if (reason != null) lines.add(reason.copy().withStyle(ChatFormatting.RED));
                if (!lines.isEmpty()) g.setComponentTooltipForNextFrame(font, lines, mx, my);
                return;
            }
        } else {
            if (in(mx, my, refineBounds())) {
                List<Component> lines = new ArrayList<>();
                lines.add(Component.translatable("screen.arcane.lectern.tip.refine", menu.refineCost()));
                Component reason = refineBlocked(o);
                if (reason != null) lines.add(reason.copy().withStyle(ChatFormatting.RED));
                g.setComponentTooltipForNextFrame(font, lines, mx, my);
                return;
            }
            if (in(mx, my, acceptBounds())) {
                List<Component> lines = new ArrayList<>();
                lines.add(Component.translatable("screen.arcane.lectern.tip.accept", Enchantment.getFullname(o.holder(), menu.pendingPower())));
                Component reason = acceptBlocked(o);
                if (reason != null) lines.add(reason.copy().withStyle(ChatFormatting.RED));
                g.setComponentTooltipForNextFrame(font, lines, mx, my);
                return;
            }
        }
        if (in(mx, my, dustBounds())) {
            g.setTooltipForNextFrame(font, menu.freeMode() ? Component.translatable("screen.arcane.lectern.dust.free")
                    : Component.translatable("screen.arcane.lectern.dust", menu.dustCount()), mx, my);
            return;
        }
        if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index < 3) {
            String key = new String[] {"screen.arcane.lectern.slot.item", "screen.arcane.lectern.slot.dust", "screen.arcane.lectern.slot.quill"}[hoveredSlot.index];
            g.setTooltipForNextFrame(font, Component.translatable(key), mx, my);
        }
    }

    private List<Component> optionTooltip(LecternOptions.Option o) {
        List<Component> lines = new ArrayList<>();
        Mastery m = o.mastery();
        lines.add(o.holder().value().description().copy().withStyle(ChatFormatting.WHITE));
        lines.add(ArcaneUi.masteryEffect(m).copy().withColor(ArcaneUi.tierColor(m.tier())));
        lines.add(Component.translatable("screen.arcane.lectern.tip.maxpower", Enchantment.getFullname(o.holder(), o.maxLevel())).withStyle(ChatFormatting.GRAY));
        if (o.currentLevel() > 0) {
            lines.add(Component.translatable("screen.arcane.lectern.tip.onitem", ArcaneUi.roman(o.currentLevel())).withColor(ArcaneUi.CYAN));
        }
        int pct = (int) Math.round(100 * Roll.chanceAtLeast(o.maxLevel(), m, menu.quillBonus(), o.maxLevel()));
        lines.add(Component.translatable("screen.arcane.lectern.tip.chance", pct).withStyle(ChatFormatting.GRAY));
        if (menu.hasQuill()) lines.add(Component.translatable("screen.arcane.lectern.tip.quill").withColor(ArcaneUi.CYAN));
        return lines;
    }

    // ---- input -------------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && menu.getCarried().isEmpty()) {
            double mx = event.x(), my = event.y();
            List<LecternOptions.Option> options = menu.options();
            for (int r = 0; r < ROWS; r++) {
                int idx = scroll + r;
                if (idx >= options.size()) break;
                if (in(mx, my, rowBounds(r))) {
                    if (idx != menu.selectedIndex()) {
                        reveal = null;
                        send(idx);
                    }
                    return true;
                }
            }
            LecternOptions.Option o = menu.selectedOption();
            if (o != null) {
                if (menu.pendingPower() == 0) {
                    if (in(mx, my, inscribeBounds())) {
                        if (inscribeBlocked(o) == null) send(ArcaneLecternMenu.BTN_INSCRIBE);
                        return true;
                    }
                } else {
                    if (in(mx, my, refineBounds())) {
                        if (refineBlocked(o) == null) send(ArcaneLecternMenu.BTN_REFINE);
                        return true;
                    }
                    if (in(mx, my, acceptBounds())) {
                        if (acceptBlocked(o) == null) send(ArcaneLecternMenu.BTN_ACCEPT);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // arrow keys move the selection through the list
        if (event.isUp() || event.isDown()) {
            List<LecternOptions.Option> options = menu.options();
            if (!options.isEmpty()) {
                int sel = menu.selectedIndex();
                int next = sel < 0 ? 0 : Math.max(0, Math.min(options.size() - 1, sel + (event.isUp() ? -1 : 1)));
                if (next != sel) {
                    reveal = null;
                    send(next);
                    if (next < scroll) scroll = next;
                    if (next >= scroll + ROWS) scroll = next - ROWS + 1;
                }
                return true;
            }
        }
        return super.keyPressed(event);
    }

    private void send(int id) {
        ArcaneUi.click();
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (ArcaneUi.inside(mx, my, leftPos + LIST_X, topPos + LIST_Y, LIST_W + 14, ROWS * ROW_H)) {
            scroll -= (int) Math.signum(sy);
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }
}
