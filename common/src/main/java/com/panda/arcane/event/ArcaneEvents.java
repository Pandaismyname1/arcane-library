package com.panda.arcane.event;

import com.panda.arcane.ArcaneConfig;
import com.panda.arcane.ArcaneMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Behaviour that does not belong to a block or item, as plain hooks: each loader module calls them from its own events
 * (NeoForge's game bus, Fabric's callbacks).
 */
public final class ArcaneEvents {
    public static final ResourceKey<Enchantment> INK_BOUND = ResourceKey.create(Registries.ENCHANTMENT, ArcaneMod.id("ink_bound"));

    private ArcaneEvents() {}

    /**
     * A player uses (right-clicks) a block. Returns PASS to let it happen, anything else to stop it with that result.
     * A Warden of Keys keeps containers near it shut; the vanilla enchanting table can be switched off.
     */
    public static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockPos pos) {
        InteractionResult locked = com.panda.arcane.entity.CreaturesA.onUseBlock(player, level, pos);
        if (locked != InteractionResult.PASS) return locked;
        if (!ArcaneConfig.disableVanillaTable()) return InteractionResult.PASS;
        if (!level.getBlockState(pos).is(Blocks.ENCHANTING_TABLE)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer server) {
            server.sendOverlayMessage(Component.translatable("message.arcane.vanilla_table"));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Opening a chest in the stacks disturbs the silent keepers nearby. */
    public static void onContainerOpen(Player opener, AbstractContainerMenu menu) {
        if (!(opener instanceof ServerPlayer player)) return;
        if (!(menu instanceof net.minecraft.world.inventory.ChestMenu)) return;
        if (!player.level().dimension().equals(com.panda.arcane.portal.PortalTravel.LIBRARY)) return;
        var box = player.getBoundingBox().inflate(12.0, 4.0, 12.0);
        for (var keeper : player.level().getEntitiesOfClass(com.panda.arcane.entity.SilentLibrarian.class, box)) {
            keeper.provoke(player, 600);
        }
    }

    /** Ink Bound: every pulse, carried items with the enchantment regain durability equal to its level. */
    /** After each player tick. */
    public static void onPlayerTick(Player ticked) {
        if (!(ticked instanceof ServerPlayer player)) return;
        if ((player.level().getGameTime() + player.getId()) % ArcaneConfig.inkBoundInterval() != 0) return;
        Holder<Enchantment> inkBound = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(INK_BOUND).orElse(null);
        if (inkBound == null) return;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !stack.isDamaged()) continue;
            int level = EnchantmentHelper.getItemEnchantmentLevel(inkBound, stack);
            if (level > 0) stack.setDamageValue(Math.max(0, stack.getDamageValue() - level));
        }
    }

    /** Nothing spawns on top of the Library's bedrock roof: only the rooms below it are the Library. */
    /** False when a mob may not spawn here, whatever its own rules say. */
    public static boolean spawnAllowed(ServerLevelAccessor level, BlockPos pos, EntitySpawnReason reason) {
        if (pos.getY() <= com.panda.arcane.worldgen.LibraryLayout.ROOF) return true;
        if (!level.getLevel().dimension().equals(com.panda.arcane.portal.PortalTravel.LIBRARY)) return true;
        return EntitySpawnReason.isSpawner(reason) || reason == EntitySpawnReason.SPAWN_ITEM_USE;
    }
}
