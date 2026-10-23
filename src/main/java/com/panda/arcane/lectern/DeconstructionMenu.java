package com.panda.arcane.lectern;

import com.panda.arcane.knowledge.KnowledgeManager;
import com.panda.arcane.logic.Costs;
import com.panda.arcane.logic.Mastery;
import com.panda.arcane.registry.ModBlocks;
import com.panda.arcane.registry.ModItems;
import com.panda.arcane.registry.ModMenus;
import com.panda.arcane.registry.ModTriggers;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Breaks enchanted gear down: teaches what is on it and returns Lumen Dust. */
public class DeconstructionMenu extends AbstractContainerMenu {
    public static final int BTN_DECONSTRUCT = 0;

    private final Container input = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            DeconstructionMenu.this.slotsChanged(this);
        }
    };
    private final ContainerLevelAccess access;
    private final Inventory inventory;

    public DeconstructionMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public DeconstructionMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.DECONSTRUCTION.get(), containerId);
        this.access = access;
        this.inventory = inventory;
        addSlot(new Slot(input, 0, 16, 38) { // UI: big slot left of the preview
            @Override public int getMaxStackSize() { return 1; }
            @Override public boolean mayPlace(ItemStack stack) { return hasEnchantments(stack); }
        });
        addStandardInventorySlots(inventory, 8, 132);
    }

    public static boolean hasEnchantments(ItemStack stack) {
        return !EnchantmentHelper.getEnchantmentsForCrafting(stack).isEmpty();
    }

    public ItemStack item() { return input.getItem(0); }

    /** What deconstructing the current item would teach: id, resulting mastery, whether it is an improvement. */
    public record Lesson(Identifier id, Mastery result, boolean gain) {}

    public List<Lesson> lessons() {
        return lessonsFor(inventory.player, item());
    }

    /** What deconstructing {@code stack} would teach {@code player}. Shared with the client tooltips (display only). */
    public static List<Lesson> lessonsFor(Player player, ItemStack stack) {
        List<Lesson> out = new ArrayList<>();
        ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        for (Holder<Enchantment> holder : enchants.keySet()) {
            Identifier id = holder.unwrapKey().orElseThrow().identifier();
            Mastery now = KnowledgeManager.mastery(player, id);
            int level = enchants.getLevel(holder);
            Mastery result = now;
            if (!now.known()) result = Mastery.GLIMPSED;
            else if (now == Mastery.GLIMPSED && level >= Math.min(3, holder.value().getMaxLevel())) result = Mastery.STUDIED;
            out.add(new Lesson(id, result, result.tier() > now.tier()));
        }
        return out;
    }

    public int dustYield() {
        ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(item());
        int total = 0;
        for (Holder<Enchantment> holder : enchants.keySet()) total += enchants.getLevel(holder);
        return hasEnchantments(item()) ? Costs.salvage(total) : 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BTN_DECONSTRUCT || !hasEnchantments(item())) return false;
        List<Lesson> lessons = lessons();
        int dust = dustYield();
        int learned = 0;
        for (Lesson lesson : lessons) {
            if (lesson.gain() && KnowledgeManager.learn(player, lesson.id(), lesson.result())) learned++;
        }
        input.setItem(0, ItemStack.EMPTY);
        ItemStack reward = new ItemStack(ModItems.LUMEN_DUST.get(), dust);
        if (!inventory.add(reward)) inventory.placeItemBackInInventory(reward, net.minecraft.util.Prediction.SERVER_ONLY);
        access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.0F, 0.8F));
        // Art: the gear grinds down into lumen motes off the wheel, and spits a little ink
        access.execute((level, pos) -> {
            if (level instanceof net.minecraft.server.level.ServerLevel server) {
                server.sendParticles(com.panda.arcane.registry.ModParticles.LUMEN_MOTE.get(), pos.getX() + 0.5, pos.getY() + 1.0,
                        pos.getZ() + 0.5, 14, 0.2, 0.1, 0.25, 0.03);
                server.sendParticles(com.panda.arcane.registry.ModParticles.INK_DROP.get(), pos.getX() + 0.5, pos.getY() + 1.0,
                        pos.getZ() + 0.5, 5, 0.25, 0.05, 0.25, 0.08);
            }
        });
        if (player instanceof ServerPlayer sp) {
            ModTriggers.ARCANE.get().trigger(sp, "deconstructed", learned);
        }
        broadcastChanges();
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, input));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.DECONSTRUCTION_TABLE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            if (index == 0) {
                if (!moveItemStackTo(stack, 1, 37, true)) return ItemStack.EMPTY;
            } else if (hasEnchantments(stack) && !this.slots.get(0).hasItem()) {
                this.slots.get(0).setByPlayer(stack.copyWithCount(1));
                stack.shrink(1);
            } else {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
            if (stack.getCount() == clicked.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }
        return clicked;
    }
}
