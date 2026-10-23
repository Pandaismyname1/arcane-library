package com.panda.arcane.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.panda.arcane.knowledge.Knowledge;
import com.panda.arcane.knowledge.KnowledgeManager;
import com.panda.arcane.logic.Mastery;
import com.panda.arcane.logic.Roll;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * The Codex: an open book of everything the player has learned. Left page: search, tier filter and a paged list.
 * Right page: the chosen enchantment and what its mastery means at the Lectern. Read-only; the data is the synced
 * knowledge attachment.
 */
public class CodexScreen extends Screen {
    // layout (matches the screen texture)
    static final int W = 300, H = 184;
    static final int L_X = 16, L_W = 124, R_X = 160, R_W = 124;
    static final int SEARCH_Y = 34, CHIP_Y = 52, CHIP_W = 28, CHIP_GAP = 4;
    static final int LIST_Y = 69, ROW_H = 12, ROWS = 7;
    static final int ARROW_Y = 158, BACK_X = 16, FWD_X = 117;
    static final int FLIP_TICKS = 6;

    private final CodexModel model = new CodexModel(ROWS);
    private EditBox search;
    private int x0, y0;
    private int ticks;
    private int flipStart = -100;
    private int flipDir;
    private int discoveredOf;

    public CodexScreen() {
        super(Component.translatable("screen.arcane.codex.title"));
    }

    @Override
    protected void init() {
        x0 = (width - W) / 2;
        y0 = (height - H) / 2;
        String old = search == null ? "" : search.getValue();
        search = new EditBox(font, x0 + L_X + 14, y0 + SEARCH_Y + 3, L_W - 18, 10, Component.translatable("screen.arcane.codex.search"));
        search.setBordered(false);
        search.setTextColor(ArcaneUi.INK);
        search.setTextShadow(false);
        search.setMaxLength(40);
        search.setValue(old);
        search.setResponder(model::setQuery);
        addRenderableWidget(search);
        refresh();
    }

    private void refresh() {
        Knowledge k = KnowledgeManager.get(minecraft.player);
        List<CodexModel.Entry> entries = new ArrayList<>();
        for (Map.Entry<Identifier, Integer> e : k.tiers().entrySet()) {
            entries.add(new CodexModel.Entry(e.getKey().toString(), name(e.getKey()).getString(), e.getValue()));
        }
        model.setEntries(entries);
        discoveredOf = minecraft.level == null ? 0 : (int) minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().count();
    }

    private static Component name(Identifier id) {
        return Component.translatable(id.toLanguageKey("enchantment"));
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        if (ticks % 20 == 0) refresh(); // knowledge can change while the book is open
    }

    // ---- bounds (also used by tests) -------------------------------------------------------------------------

    public int[] chipBounds(int tier) {
        return new int[] {x0 + L_X + tier * (CHIP_W + CHIP_GAP), y0 + CHIP_Y, CHIP_W, 12};
    }

    public int[] rowBounds(int i) {
        return new int[] {x0 + L_X - 2, y0 + LIST_Y + i * ROW_H, L_W + 4, ROW_H};
    }

    public int[] forwardBounds() {
        return new int[] {x0 + FWD_X, y0 + ARROW_Y, 23, 13};
    }

    public int[] backBounds() {
        return new int[] {x0 + BACK_X, y0 + ARROW_Y, 23, 13};
    }

    public EditBox searchBox() {
        return search;
    }

    private static boolean in(double mx, double my, int[] b) {
        return ArcaneUi.inside(mx, my, b[0], b[1], b[2], b[3]);
    }

    // ---- drawing -----------------------------------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partial) {
        super.extractBackground(g, mx, my, partial);
        ArcaneUi.background(g, ArcaneUi.CODEX_BG, x0, y0, W, H, 512, 256);
        ArcaneUi.sprite(g, ArcaneUi.FIELD, x0 + L_X, y0 + SEARCH_Y, L_W, 14);
        ArcaneUi.sprite(g, ArcaneUi.SEARCH, x0 + L_X + 3, y0 + SEARCH_Y + 3, 8, 8);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
        super.extractRenderState(g, mx, my, partial);
        drawLeft(g, mx, my);
        drawRight(g);
        drawFlip(g, partial);
        tooltips(g, mx, my);
    }

    private void drawLeft(GuiGraphicsExtractor g, int mx, int my) {
        int cx = x0 + L_X + L_W / 2;
        g.text(font, title, cx - font.width(title) / 2, y0 + 12, ArcaneUi.INK, false);
        if (search.getValue().isEmpty() && !search.isFocused()) {
            g.text(font, Component.translatable("screen.arcane.codex.search"), search.getX(), search.getY(), ArcaneUi.INK_MUTED, false);
        }
        Component count = Component.translatable("screen.arcane.codex.discovered", model.all().size(), discoveredOf);
        g.text(font, count, cx - font.width(count) / 2, y0 + 23, ArcaneUi.INK_MUTED, false);

        for (int t = 0; t <= 3; t++) {
            int[] b = chipBounds(t);
            boolean selected = model.tierFilter() == t;
            boolean hover = !selected && in(mx, my, b);
            ArcaneUi.sprite(g, selected ? ArcaneUi.CHIP_SELECTED : hover ? ArcaneUi.CHIP_HOVER : ArcaneUi.CHIP, b[0], b[1], b[2], b[3]);
            if (t == 0) {
                Component all = Component.translatable("screen.arcane.codex.filter.all");
                g.text(font, all, b[0] + (b[2] - font.width(all)) / 2, b[1] + 2, selected ? 0xFFEEE1B9 : ArcaneUi.INK, false);
            } else {
                ArcaneUi.sprite(g, ArcaneUi.tierH(t), b[0] + (b[2] - 15) / 2, b[1] + 4, 15, 5);
            }
            if (selected) g.fill(b[0] + 3, b[1] + b[3] + 1, b[0] + b[2] - 3, b[1] + b[3] + 2, ArcaneUi.INK); // underline: not colour alone
        }

        List<CodexModel.Entry> page = model.pageEntries();
        if (model.all().isEmpty()) {
            g.textWithWordWrap(font, Component.translatable("screen.arcane.codex.empty"), x0 + L_X + 2, y0 + LIST_Y + 4, L_W - 4, ArcaneUi.INK_MUTED, false);
        } else if (page.isEmpty()) {
            g.text(font, Component.translatable("screen.arcane.codex.nomatch"), x0 + L_X + 2, y0 + LIST_Y + 4, ArcaneUi.INK_MUTED, false);
        }
        CodexModel.Entry sel = model.selected();
        for (int i = 0; i < page.size(); i++) {
            CodexModel.Entry e = page.get(i);
            int[] b = rowBounds(i);
            boolean selected = sel != null && sel.key().equals(e.key());
            if (selected) ArcaneUi.sprite(g, ArcaneUi.ENTRY_SELECTED, b[0], b[1], b[2], b[3]);
            else if (in(mx, my, b)) ArcaneUi.sprite(g, ArcaneUi.ENTRY_HOVER, b[0], b[1], b[2], b[3]);
            String name = ArcaneUi.fit(font, e.name(), L_W - 22);
            g.text(font, name, b[0] + 4, b[1] + 2, selected ? ArcaneUi.INK : 0xFF382411, false);
            ArcaneUi.sprite(g, ArcaneUi.tierH(e.tier()), b[0] + b[2] - 18, b[1] + 3, 15, 5);
        }

        int pages = model.pages();
        if (pages > 1) {
            int[] back = backBounds(), fwd = forwardBounds();
            if (model.page() > 0) ArcaneUi.sprite(g, in(mx, my, back) ? ArcaneUi.PAGE_BACKWARD_HOVER : ArcaneUi.PAGE_BACKWARD, back[0], back[1], 23, 13);
            if (model.page() < pages - 1) ArcaneUi.sprite(g, in(mx, my, fwd) ? ArcaneUi.PAGE_FORWARD_HOVER : ArcaneUi.PAGE_FORWARD, fwd[0], fwd[1], 23, 13);
            Component label = Component.translatable("screen.arcane.codex.page", model.page() + 1, pages);
            g.text(font, label, cx - font.width(label) / 2, y0 + ARROW_Y + 3, ArcaneUi.INK_MUTED, false);
        }
    }

    private void drawRight(GuiGraphicsExtractor g) {
        int x = x0 + R_X, y = y0 + 12;
        if (model.all().isEmpty()) {
            g.text(font, Component.translatable("screen.arcane.codex.howto.title"), x, y, ArcaneUi.INK, false);
            ArcaneUi.rule(g, x, y + 12, R_W, ArcaneUi.INK_RULE);
            g.textWithWordWrap(font, Component.translatable("screen.arcane.codex.howto"), x, y + 20, R_W, ArcaneUi.INK_MUTED, false);
            return;
        }
        CodexModel.Entry e = model.selected();
        if (e == null) {
            g.textWithWordWrap(font, Component.translatable("screen.arcane.codex.select"), x, y + 4, R_W, ArcaneUi.INK_MUTED, false);
            return;
        }
        Identifier id = Identifier.parse(e.key());
        Mastery m = Mastery.ofTier(e.tier());
        ArcaneUi.sprite(g, ArcaneUi.tierGem(e.tier()), x, y - 1, 9, 9);
        g.text(font, ArcaneUi.fit(font, e.name(), R_W - 12), x + 12, y, ArcaneUi.INK, false);
        ArcaneUi.sprite(g, ArcaneUi.tierH(e.tier()), x, y + 14, 15, 5);
        g.text(font, ArcaneUi.masteryName(e.tier()), x + 18, y + 12, ArcaneUi.tierInk(e.tier()), false);
        ArcaneUi.rule(g, x, y + 24, R_W, ArcaneUi.INK_RULE);
        int ly = y + 30;

        String descKey = id.toLanguageKey("enchantment") + ".desc";
        if (Language.getInstance().has(descKey)) {
            List<FormattedCharSequence> lines = font.split(Component.translatable(descKey), R_W);
            for (int i = 0; i < Math.min(3, lines.size()); i++) {
                g.text(font, lines.get(i), x, ly, ArcaneUi.INK_MUTED, false);
                ly += 9;
            }
            ly += 5;
        } else if (!id.getNamespace().equals("minecraft")) {
            // another mod's enchantment without a description: at least say where it comes from
            String mod = net.neoforged.fml.ModList.get().getModContainerById(id.getNamespace())
                    .map(c -> c.getModInfo().getDisplayName()).orElse(id.getNamespace());
            g.text(font, ArcaneUi.fit(font, Component.translatable("screen.arcane.codex.from_mod", mod).getString(), R_W), x, ly, ArcaneUi.INK_MUTED, false);
            ly += 14;
        }

        Optional<Holder.Reference<Enchantment>> holder = minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .get(ResourceKey.create(Registries.ENCHANTMENT, id));
        int max = holder.map(h -> h.value().getMaxLevel()).orElse(1);
        g.text(font, Component.translatable("screen.arcane.codex.atlectern"), x, ly, ArcaneUi.INK, false);
        ly += 12;
        int pct = (int) Math.round(100 * Roll.chanceAtLeast(max, m, 0, max));
        ly = stat(g, x, ly, Component.translatable("screen.arcane.codex.maxlevel"), ArcaneUi.roman(max));
        ly = stat(g, x, ly, Component.translatable("screen.arcane.codex.refines"), Component.literal(Integer.toString(m.refinements())));
        ly = stat(g, x, ly, Component.translatable("screen.arcane.codex.bonus"), Component.literal("+" + m.rollBonus()));
        ly = stat(g, x, ly, Component.translatable("screen.arcane.codex.full"), Component.translatable("screen.arcane.codex.full.value", pct));
        ly += 6;

        // mastery track: three gems joined by a line; the ones not reached yet are faded
        int tx = x + 2;
        for (int t = 1; t <= 3; t++) {
            int gx = tx + (t - 1) * 30;
            if (t < 3) g.fill(gx + 9, ly + 4, gx + 30, ly + 5, t < e.tier() ? ArcaneUi.tierInk(t + 1) : ArcaneUi.INK_RULE);
            ArcaneUi.sprite(g, ArcaneUi.tierGem(t), gx, ly, 9, 9, t <= e.tier() ? 0xFFFFFFFF : 0x50FFFFFF);
        }
        ly += 14;
        String next = e.tier() >= 3 ? "screen.arcane.codex.next.done" : "screen.arcane.codex.next." + (e.tier() + 1);
        g.textWithWordWrap(font, Component.translatable(next), x, ly, R_W, ArcaneUi.INK_MUTED, false);
    }

    private int stat(GuiGraphicsExtractor g, int x, int y, Component label, Component value) {
        g.text(font, label, x + 4, y, ArcaneUi.INK_MUTED, false);
        g.text(font, value, x + R_W - font.width(value), y, ArcaneUi.INK, false);
        // dotted leader between label and value
        int from = x + 4 + font.width(label) + 3, to = x + R_W - font.width(value) - 3;
        for (int dx = from; dx < to; dx += 3) g.fill(dx, y + 7, dx + 1, y + 8, ArcaneUi.INK_RULE);
        return y + 10;
    }

    /** A sheet lifting off one page and settling on the other: drawn above the content on its own stratum. */
    private void drawFlip(GuiGraphicsExtractor g, float partial) {
        float t = (ticks - flipStart + partial) / FLIP_TICKS;
        if (t < 0F || t >= 1F) return;
        g.nextStratum();
        int pageW = 140, gutter = x0 + 150, top = y0 + 6, bottom = y0 + 176;
        float k = t < 0.5F ? 1F - 2F * t : 2F * t - 1F;
        int w = Math.max(1, Math.round(pageW * k));
        boolean onRight = (flipDir > 0) == (t < 0.5F);
        int sx0 = onRight ? gutter : gutter - w, sx1 = onRight ? gutter + w : gutter;
        int lift = Math.round(6 * (1F - k)); // the sheet rises toward the middle of the turn
        g.fill(sx0, top - lift, sx1, bottom - lift, 0xFFEEE1B9); // PARCHMENT[4]
        g.fill(onRight ? sx1 - 1 : sx0, top - lift, onRight ? sx1 : sx0 + 1, bottom - lift, 0xFFB49E72); // PARCHMENT[2]
        g.fillGradient(sx0, bottom - lift, sx1, bottom - lift + 3, 0x50000000, 0x00000000);
    }

    private void tooltips(GuiGraphicsExtractor g, int mx, int my) {
        for (int t = 0; t <= 3; t++) {
            if (in(mx, my, chipBounds(t))) {
                g.setTooltipForNextFrame(font, t == 0 ? Component.translatable("screen.arcane.codex.filter.tip.all")
                        : Component.translatable("screen.arcane.codex.filter.tip", ArcaneUi.masteryName(t)), mx, my);
                return;
            }
        }
        List<CodexModel.Entry> page = model.pageEntries();
        for (int i = 0; i < page.size(); i++) {
            CodexModel.Entry e = page.get(i);
            if (in(mx, my, rowBounds(i)) && !ArcaneUi.fits(font, e.name(), L_W - 22)) {
                g.setTooltipForNextFrame(font, Component.literal(e.name()), mx, my);
                return;
            }
        }
    }

    // ---- input -------------------------------------------------------------------------------------------------

    private void turn(int delta) {
        if (model.turn(delta)) {
            flipStart = ticks;
            flipDir = delta;
            ArcaneUi.play(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            double mx = event.x(), my = event.y();
            for (int t = 0; t <= 3; t++) {
                if (in(mx, my, chipBounds(t))) {
                    if (model.tierFilter() != t) {
                        model.setTierFilter(t);
                        ArcaneUi.click();
                    }
                    return true;
                }
            }
            List<CodexModel.Entry> page = model.pageEntries();
            for (int i = 0; i < page.size(); i++) {
                if (in(mx, my, rowBounds(i))) {
                    model.select(page.get(i).key());
                    ArcaneUi.play(SoundEvents.BOOK_PAGE_TURN, 1.6F, 0.4F);
                    return true;
                }
            }
            if (model.page() > 0 && in(mx, my, backBounds())) { turn(-1); return true; }
            if (model.page() < model.pages() - 1 && in(mx, my, forwardBounds())) { turn(1); return true; }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        turn(sy > 0 ? -1 : 1);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!search.isFocused()) {
            if (event.isLeft()) { turn(-1); return true; }
            if (event.isRight()) { turn(1); return true; }
        }
        if (event.isUp() || event.isDown()) {
            int before = model.page();
            model.step(event.isUp() ? -1 : 1);
            if (model.page() != before) {
                flipStart = ticks;
                flipDir = model.page() - before;
            }
            return true;
        }
        if (event.key() == InputConstants.KEY_PAGEUP) { turn(-1); return true; }
        if (event.key() == InputConstants.KEY_PAGEDOWN) { turn(1); return true; }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** For tests: set the filter without a click. */
    public void filter(int tier) {
        model.setTierFilter(tier);
    }
}
