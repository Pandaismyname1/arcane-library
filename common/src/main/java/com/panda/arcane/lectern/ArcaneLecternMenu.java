package com.panda.arcane.lectern;

import com.panda.arcane.logic.Costs;
import com.panda.arcane.logic.Mastery;
import com.panda.arcane.logic.Roll;
import com.panda.arcane.registry.ModBlocks;
import com.panda.arcane.registry.ModItems;
import com.panda.arcane.registry.ModMenus;
import com.panda.arcane.registry.ModSounds;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

/**
 * The Arcane Lectern. The server owns every number: the client only sends button ids and re-derives the option
 * list from synced knowledge for display.
 */
public class ArcaneLecternMenu extends AbstractContainerMenu {
    public static final int SLOT_ITEM = 0;
    public static final int SLOT_DUST = 1;
    public static final int SLOT_QUILL = 2;
    public static final int BTN_SELECT_MAX = 500;
    public static final int BTN_INSCRIBE = 1000;
    public static final int BTN_REFINE = 1001;
    public static final int BTN_ACCEPT = 1002;
    public static final int QUILL_ROLL_BONUS = 6;
    public static final int QUILL_REFINES = 1;

    private final Container inputs = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
            ArcaneLecternMenu.this.slotsChanged(this);
        }
    };
    private final ContainerLevelAccess access;
    private final Inventory inventory;
    private final DataSlot selected = DataSlot.standalone();
    private final DataSlot pending = DataSlot.standalone();
    private final DataSlot refines = DataSlot.standalone();
    private final DataSlot rolls = DataSlot.standalone();
    private ItemStack lastItem = ItemStack.EMPTY;
    /** Server-side identity of the selection, so a changed knowledge list can never retarget a pending roll. */
    private net.minecraft.resources.Identifier selectedId;

    public ArcaneLecternMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public ArcaneLecternMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.LECTERN.get(), containerId);
        this.access = access;
        this.inventory = inventory;
        addSlot(new Slot(inputs, SLOT_ITEM, 17, 24) {
            @Override public int getMaxStackSize() { return 1; }
            @Override public boolean mayPlace(ItemStack stack) { return LecternOptions.acceptsItem(stack); }
        });
        addSlot(new Slot(inputs, SLOT_DUST, 17, 54) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(ModItems.LUMEN_DUST.get()); }
        });
        addSlot(new Slot(inputs, SLOT_QUILL, 17, 84) {
            @Override public int getMaxStackSize() { return 1; }
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(ModItems.QUILL_OF_INSIGHT.get()); }
        });
        addStandardInventorySlots(inventory, 54, 142); // UI: centred in the 270-wide screen
        addDataSlot(selected).set(-1);
        addDataSlot(pending);
        addDataSlot(refines);
        addDataSlot(rolls);
    }

    // ---- shared derived state (server and client) -------------------------------------------------------------

    public ItemStack targetItem() { return inputs.getItem(SLOT_ITEM); }
    public ItemStack dust() { return inputs.getItem(SLOT_DUST); }
    public ItemStack quill() { return inputs.getItem(SLOT_QUILL); }
    public boolean hasQuill() { return quill().is(ModItems.QUILL_OF_INSIGHT.get()); }
    public int selectedIndex() { return selected.get(); }
    public int pendingPower() { return pending.get(); }
    public int refinesUsed() { return refines.get(); }
    public int rollCounter() { return rolls.get(); }
    public int quillBonus() { return hasQuill() ? QUILL_ROLL_BONUS : 0; }

    public List<LecternOptions.Option> options() {
        return LecternOptions.compute(inventory.player, targetItem());
    }

    public LecternOptions.Option selectedOption() {
        List<LecternOptions.Option> list = options();
        if (!inventory.player.level().isClientSide()) {
            if (selectedId == null) return null;
            for (int k = 0; k < list.size(); k++) {
                if (list.get(k).id().equals(selectedId)) {
                    selected.set(k);
                    return list.get(k);
                }
            }
            return null;
        }
        int i = selected.get();
        return i >= 0 && i < list.size() ? list.get(i) : null;
    }

    public int maxRefines(LecternOptions.Option option) {
        return option.mastery().refinements() + (hasQuill() ? QUILL_REFINES : 0);
    }

    public int inscribeCost(LecternOptions.Option option) { return Costs.inscribe(option.maxLevel(), option.weight()); }
    public int refineCost() { return Costs.refine(refines.get()); }
    public int dustCount() { return dust().is(ModItems.LUMEN_DUST.get()) ? dust().getCount() : 0; }

    public boolean freeMode() { return inventory.player.hasInfiniteMaterials(); }

    // ---- server logic ------------------------------------------------------------------------------------------

    @Override
    public void slotsChanged(Container container) {
        if (container != inputs) return;
        ItemStack now = inputs.getItem(SLOT_ITEM);
        if (!ItemStack.matches(now, lastItem)) {
            lastItem = now.copy();
            selected.set(-1);
            selectedId = null;
            pending.set(0);
            refines.set(0);
        }
        if (selected.get() >= 0 && selectedOption() == null) {
            selected.set(-1);
            selectedId = null;
            pending.set(0);
            refines.set(0);
        }
        broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id < BTN_SELECT_MAX) {
            if (id >= options().size()) return false;
            selected.set(id);
            selectedId = options().get(id).id();
            pending.set(0);
            refines.set(0);
            return true;
        }
        LecternOptions.Option option = selectedOption();
        if (option == null) return false;
        switch (id) {
            case BTN_INSCRIBE -> {
                if (pending.get() != 0) return false;
                if (!pay(player, inscribeCost(option))) return false;
                wearQuill(player);
                int power = Roll.inscribe(player.getRandom()::nextInt, option.mastery(), quillBonus(), option.maxLevel());
                pending.set(power);
                refines.set(0);
                rolls.set(rolls.get() + 1);
                feedback(player, power, option.maxLevel());
                return true;
            }
            case BTN_REFINE -> {
                if (pending.get() == 0 || refines.get() >= maxRefines(option)) return false;
                if (pending.get() >= option.maxLevel()) return false;
                if (!pay(player, refineCost())) return false;
                wearQuill(player);
                int power = Roll.refine(player.getRandom()::nextInt, pending.get(), option.mastery(), quillBonus(), option.maxLevel());
                refines.set(refines.get() + 1);
                pending.set(power);
                rolls.set(rolls.get() + 1);
                feedback(player, power, option.maxLevel());
                return true;
            }
            case BTN_ACCEPT -> {
                int power = pending.get();
                if (power <= option.currentLevel() || power > option.maxLevel()) return false;
                ItemStack stack = targetItem();
                ItemStack result = stack.is(Items.BOOK) ? stack.transmuteCopy(Items.ENCHANTED_BOOK) : stack.copy();
                final int level = power;
                EnchantmentHelper.updateEnchantments(result, m -> m.set(option.holder(), level));
                inputs.setItem(SLOT_ITEM, result);
                lastItem = result.copy();
                pending.set(0);
                refines.set(0);
                selected.set(-1);
            selectedId = null;
                access.execute((level2, pos) -> {
                    level2.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                    if (level2 instanceof ServerLevel sl) {
                        sl.sendParticles(com.panda.arcane.registry.ModParticles.PORTAL_GLYPH.get(), pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.5);
                        sl.sendParticles(com.panda.arcane.registry.ModParticles.LUMEN_MOTE.get(), pos.getX() + 0.5, pos.getY() + 1.0,
                                pos.getZ() + 0.5, 12, 0.3, 0.1, 0.3, 0.02); // Art: the spent dust rises off the book
                    }
                });
                if (player instanceof ServerPlayer sp) {
                    com.panda.arcane.registry.ModTriggers.ARCANE.get().trigger(sp, power >= option.maxLevel() ? "inscribed_max" : "inscribed", power);
                }
                inputs.setChanged();
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    /** The quill is spent by every roll it influences, not only by the final accept. */
    private void wearQuill(Player player) {
        if (!hasQuill() || player.hasInfiniteMaterials() || !(player instanceof ServerPlayer sp)) return;
        ItemStack q = quill();
        q.hurtAndBreak(1, sp.level(), sp, item -> {});
        if (q.isEmpty()) inputs.setItem(SLOT_QUILL, ItemStack.EMPTY);
        inputs.setChanged();
    }

    private boolean pay(Player player, int cost) {
        if (player.hasInfiniteMaterials()) return true;
        ItemStack stack = dust();
        if (!stack.is(ModItems.LUMEN_DUST.get()) || stack.getCount() < cost) return false;
        stack.shrink(cost);
        if (stack.isEmpty()) inputs.setItem(SLOT_DUST, ItemStack.EMPTY);
        inputs.setChanged();
        return true;
    }

    private void feedback(Player player, int power, int max) {
        access.execute((level, pos) -> {
            float pitch = 0.8F + 0.4F * power / Math.max(1, max);
            level.playSound(null, pos, power >= max ? ModSounds.INSCRIBE_MAX.get() : ModSounds.INSCRIBE_ROLL.get(),
                    SoundSource.BLOCKS, 0.8F, pitch);
        });
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, inputs));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.ARCANE_LECTERN.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            if (index < 3) {
                if (!moveItemStackTo(stack, 3, 39, true)) return ItemStack.EMPTY;
            } else if (stack.is(ModItems.LUMEN_DUST.get())) {
                if (!moveItemStackTo(stack, SLOT_DUST, SLOT_DUST + 1, false)) return ItemStack.EMPTY;
            } else if (stack.is(ModItems.QUILL_OF_INSIGHT.get())) {
                if (!moveItemStackTo(stack, SLOT_QUILL, SLOT_QUILL + 1, false)) return ItemStack.EMPTY;
            } else if (LecternOptions.acceptsItem(stack)) {
                if (this.slots.get(SLOT_ITEM).hasItem()) return ItemStack.EMPTY;
                this.slots.get(SLOT_ITEM).setByPlayer(stack.copyWithCount(1));
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
