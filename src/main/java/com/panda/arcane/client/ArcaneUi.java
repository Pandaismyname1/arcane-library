package com.panda.arcane.client;

import com.panda.arcane.ArcaneMod;
import com.panda.arcane.logic.Mastery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * The small design system every Arcane screen shares: colour tokens, sprite ids and the
 * few drawing helpers that are not plain sprites. Chrome is vanilla; magic happens on the dark "scrying well".
 */
public final class ArcaneUi {
    // ---- colour tokens (ramp steps from docs/ART_STYLE.md) --------------------------------------------------------
    /** Vanilla container label grey. */
    public static final int LABEL = 0xFF404040;
    /** Text on the dark scrying well. */
    public static final int WELL_TEXT = 0xFFFBF5E0;
    public static final int WELL_MUTED = 0xFFB8A2F5;
    public static final int WELL_LINE = 0xFF40366F;
    public static final int GOLD = 0xFFFFDD7A;
    public static final int RED = 0xFFEA5C55;
    public static final int GREEN = 0xFF8CF5A0;
    public static final int CYAN = 0xFF93ECF5;
    /** Text on the tan enchanting rows (vanilla's colours). */
    public static final int ROW_TEXT = 0xFF685E4A;
    public static final int ROW_TEXT_HOVER = 0xFFFFFF80;
    public static final int ROW_TEXT_SELECTED = 0xFFFFF1C4;
    /** Text on the brown list well. */
    public static final int WELL_BROWN_TEXT = 0xFFD8CBB0;
    /** Ink on Codex parchment. */
    public static final int INK = 0xFF2B1B0D;
    public static final int INK_MUTED = 0xFF5E4B30;
    public static final int INK_RULE = 0xFFB49E72;
    /** Vanilla button text. */
    public static final int BUTTON_TEXT = 0xFFFFFFFF;
    public static final int BUTTON_TEXT_DISABLED = 0xFFA0A0A0;

    /** Mastery tier colours: bright for dark backgrounds, deep for parchment. Never the only signal (gem count). */
    public static int tierColor(int tier) {
        return switch (tier) {
            case 1 -> 0xFFB8A2F5; // VIOLET[5]
            case 2 -> 0xFF6AA3D1; // STUDIED[5]
            case 3 -> 0xFFE4BD5E; // BRASS[4]: the gilt of a mastered tome
            default -> WELL_MUTED;
        };
    }

    public static int tierInk(int tier) {
        return switch (tier) {
            case 1 -> 0xFF6849AD; // VIOLET[3]
            case 2 -> 0xFF2F6194; // STUDIED[3]
            case 3 -> 0xFF931D2A; // CRIMSON[2]: oxblood
            default -> INK_MUTED;
        };
    }

    // ---- textures and sprites --------------------------------------------------------------------------------------
    public static final Identifier LECTERN_BG = ArcaneMod.id("textures/gui/container/arcane_lectern.png");
    public static final Identifier DECONSTRUCTION_BG = ArcaneMod.id("textures/gui/container/deconstruction_table.png");
    public static final Identifier CODEX_BG = ArcaneMod.id("textures/gui/codex.png");

    public static final Identifier ROW = ArcaneMod.id("lectern/row");
    public static final Identifier ROW_HOVER = ArcaneMod.id("lectern/row_hover");
    public static final Identifier ROW_SELECTED = ArcaneMod.id("lectern/row_selected");
    public static final Identifier DUST = ArcaneMod.id("icon/dust");
    public static final Identifier DUST_OFF = ArcaneMod.id("icon/dust_off");
    public static final Identifier LOCK = ArcaneMod.id("icon/lock");
    public static final Identifier SEARCH = ArcaneMod.id("icon/search");
    public static final Identifier NEW = ArcaneMod.id("icon/new");
    public static final Identifier UP = ArcaneMod.id("icon/up");
    public static final Identifier GHOST_DUST = ArcaneMod.id("slot/lumen_dust");
    public static final Identifier GHOST_QUILL = ArcaneMod.id("slot/quill");
    public static final Identifier GHOST_BOOK = ArcaneMod.id("slot/book");
    public static final Identifier STUDY_FRAME = ArcaneMod.id("hud/study_frame");
    public static final Identifier STUDY_FILL = ArcaneMod.id("hud/study_fill");
    public static final Identifier CHIP = ArcaneMod.id("codex/chip");
    public static final Identifier CHIP_HOVER = ArcaneMod.id("codex/chip_hover");
    public static final Identifier CHIP_SELECTED = ArcaneMod.id("codex/chip_selected");
    public static final Identifier FIELD = ArcaneMod.id("codex/field");
    public static final Identifier ENTRY_SELECTED = ArcaneMod.id("codex/entry_selected");
    public static final Identifier ENTRY_HOVER = ArcaneMod.id("codex/entry_hover");

    public static final Identifier BUTTON = Identifier.withDefaultNamespace("widget/button");
    public static final Identifier BUTTON_HOVER = Identifier.withDefaultNamespace("widget/button_highlighted");
    public static final Identifier BUTTON_DISABLED = Identifier.withDefaultNamespace("widget/button_disabled");
    public static final Identifier SCROLLER = Identifier.withDefaultNamespace("widget/scroller");
    public static final Identifier PAGE_FORWARD = ArcaneMod.id("codex/page_forward");
    public static final Identifier PAGE_FORWARD_HOVER = ArcaneMod.id("codex/page_forward_hover");
    public static final Identifier PAGE_BACKWARD = ArcaneMod.id("codex/page_backward");
    public static final Identifier PAGE_BACKWARD_HOVER = ArcaneMod.id("codex/page_backward_hover");

    /** Vertical tier gems (5x13) for list rows. */
    public static Identifier tierV(int tier) {
        return ArcaneMod.id("tier/v" + Math.max(1, Math.min(3, tier)));
    }

    /** Horizontal tier gems (13x5) for lines of text. */
    public static Identifier tierH(int tier) {
        return ArcaneMod.id("tier/h" + Math.max(1, Math.min(3, tier)));
    }

    /** A single 9x9 tier gem. */
    public static Identifier tierGem(int tier) {
        return ArcaneMod.id("tier/gem" + Math.max(1, Math.min(3, tier)));
    }

    public static Identifier pip(String kind, boolean small) {
        return ArcaneMod.id("pip/" + (small ? "small_" : "") + kind);
    }

    private ArcaneUi() {}

    // ---- drawing ---------------------------------------------------------------------------------------------------
    public static void sprite(GuiGraphicsExtractor g, Identifier id, int x, int y, int w, int h) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, id, x, y, w, h);
    }

    public static void sprite(GuiGraphicsExtractor g, Identifier id, int x, int y, int w, int h, int argb) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, id, x, y, w, h, argb);
    }

    public static void background(GuiGraphicsExtractor g, Identifier texture, int x, int y, int w, int h, int texW, int texH) {
        g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, w, h, texW, texH);
    }

    /**
     * A vanilla button with an optional Lumen Dust cost tag after the label. Disabled buttons use vanilla's disabled
     * face, a padlock and grey text; the cost turns red when it can't be paid. Pressed state is not drawn: menu
     * buttons fire on press.
     */
    public static void button(GuiGraphicsExtractor g, Font font, Component label, int x, int y, int w, int h,
                              boolean enabled, boolean hovered, int cost, boolean affordable) {
        sprite(g, !enabled ? BUTTON_DISABLED : hovered ? BUTTON_HOVER : BUTTON, x, y, w, h);
        int labelW = font.width(label);
        String costText = cost > 0 ? Integer.toString(cost) : "";
        int costW = cost > 0 ? 3 + 9 + 1 + font.width(costText) : 0;
        int lockW = enabled ? 0 : 9;
        int total = lockW + labelW + costW;
        int tx = x + (w - total) / 2;
        int ty = y + (h - 8) / 2;
        if (!enabled) {
            sprite(g, LOCK, tx, ty - 1, 7, 9);
            tx += lockW;
        }
        g.text(font, label, tx, ty, enabled ? BUTTON_TEXT : BUTTON_TEXT_DISABLED, enabled);
        if (cost > 0) {
            int cx = tx + labelW + 3;
            sprite(g, affordable ? DUST : DUST_OFF, cx, ty - 1, 9, 9);
            g.text(font, costText, cx + 10, ty, affordable ? (enabled ? GOLD : BUTTON_TEXT_DISABLED) : RED, enabled || !affordable);
        }
    }

    /** One-line text cut to {@code maxWidth} with an ellipsis. */
    public static String fit(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        return font.plainSubstrByWidth(text, maxWidth - font.width("...")) + "...";
    }

    public static boolean fits(Font font, String text, int maxWidth) {
        return font.width(text) <= maxWidth;
    }

    /** A thin rule that fades out at both ends, with a small diamond in the middle. */
    public static void rule(GuiGraphicsExtractor g, int x, int y, int w, int color) {
        int fade = Math.min(10, w / 4);
        int rgb = color & 0xFFFFFF;
        int alpha = color >>> 24;
        for (int i = 0; i < w; i++) {
            int edge = Math.min(i, w - 1 - i);
            int a = edge >= fade ? alpha : alpha * (edge + 1) / (fade + 1);
            g.fill(x + i, y, x + i + 1, y + 1, (a << 24) | rgb);
        }
        int cx = x + w / 2;
        g.fill(cx - 1, y - 1, cx + 2, y + 2, color);
        g.fill(cx, y - 2, cx + 1, y + 3, color);
    }

    public static Component masteryName(int tier) {
        return Component.translatable("mastery.arcane." + tier);
    }

    public static Component masteryEffect(Mastery m) {
        return Component.translatable("screen.arcane.lectern.tip.mastery", masteryName(m.tier()), m.refinements(), m.rollBonus());
    }

    public static Component roman(int level) {
        return Component.translatable("enchantment.level." + level);
    }

    public static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public static void play(SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
}
