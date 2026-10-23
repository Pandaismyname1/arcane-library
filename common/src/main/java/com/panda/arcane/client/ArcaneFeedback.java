package com.panda.arcane.client;

import com.panda.arcane.ArcaneMod;
import com.panda.arcane.item.TomeItem;
import com.panda.arcane.knowledge.Knowledge;
import com.panda.arcane.knowledge.KnowledgeManager;
import com.panda.arcane.logic.Mastery;
import com.panda.arcane.registry.ModItems;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * In-world feedback that needs no extra packets: a toast whenever the synced knowledge attachment gains or deepens
 * an entry (tome study, deconstruction, anything else the server teaches), and a study progress bar under the
 * crosshair while a tome is being read.
 */
public final class ArcaneFeedback {
    /** Ticks after joining or respawning during which knowledge changes are the initial sync, not news. */
    private static final int GRACE_TICKS = 40;

    private static LocalPlayer trackedPlayer;
    private static Map<Identifier, Integer> known = Map.of();
    private static int grace;

    private ArcaneFeedback() {}

    /** The HUD layer's id; loaders draw {@link #studyBar} above the crosshair under it. */
    public static final Identifier STUDY_LAYER = ArcaneMod.id("study");

    /** After each client tick. */
    public static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            trackedPlayer = null;
            return;
        }
        Knowledge now = KnowledgeManager.get(player);
        if (player != trackedPlayer) {
            trackedPlayer = player;
            known = new HashMap<>(now.tiers());
            grace = GRACE_TICKS;
            return;
        }
        if (grace > 0) {
            grace--;
            known = new HashMap<>(now.tiers());
            return;
        }
        if (now.tiers().equals(known)) return;
        for (Map.Entry<Identifier, Integer> e : now.tiers().entrySet()) {
            int before = known.getOrDefault(e.getKey(), 0);
            if (e.getValue() > before) announce(mc, e.getKey(), before, e.getValue());
        }
        known = new HashMap<>(now.tiers());
    }

    private static void announce(Minecraft mc, Identifier id, int before, int tier) {
        Component name = TomeItem.enchantmentName(id);
        Component title = Component.translatable(before == 0 ? "toast.arcane.learned" : "toast.arcane.deepened");
        Component line = Component.translatable("toast.arcane.line", name, ArcaneUi.masteryName(tier));
        ItemStack icon = TomeItem.create(ModItems.tome(Mastery.ofTier(tier)).get(), id);
        mc.gui.toastManager().addToast(new LearnedToast(title, line, icon, tier, id.toString() + "#" + tier));
    }

    /** For tests: show a toast as if knowledge had just changed. */
    public static void announceForTest(Identifier id, int before, int tier) {
        announce(Minecraft.getInstance(), id, before, tier);
    }

    public static void studyBar(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.gui.hud.isHidden() || mc.gui.screen() != null || !p.isUsingItem()) return;
        ItemStack using = p.getUseItem();
        if (!(using.getItem() instanceof TomeItem tome) || TomeItem.enchantmentOf(using) == null) return;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        int total = using.getUseDuration(p);
        float progress = Math.max(0F, Math.min(1F, (total - p.getUseItemRemainingTicks() + partial) / Math.max(1, total)));
        int w = 64, x = (g.guiWidth() - w) / 2, y = g.guiHeight() / 2 + 12;
        int tier = tome.mastery().tier();
        ArcaneUi.sprite(g, ArcaneUi.STUDY_FRAME, x, y, w, 7);
        int fill = Math.round(62 * progress);
        if (fill > 0) ArcaneUi.sprite(g, ArcaneUi.STUDY_FILL, x + 1, y + 1, fill, 5, ArcaneUi.tierColor(tier));
        Component label = Component.translatable("hud.arcane.studying", TomeItem.enchantmentName(TomeItem.enchantmentOf(using)));
        g.text(mc.font, label, (g.guiWidth() - mc.font.width(label)) / 2, y + 10, ArcaneUi.tierColor(tier), true);
    }
}
