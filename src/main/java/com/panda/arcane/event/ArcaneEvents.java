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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Game-bus behaviour that does not belong to a block or item. */
public final class ArcaneEvents {
    public static final ResourceKey<Enchantment> INK_BOUND = ResourceKey.create(Registries.ENCHANTMENT, ArcaneMod.id("ink_bound"));

    private ArcaneEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ArcaneEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(ArcaneEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(ArcaneEvents::onContainerOpen);
        NeoForge.EVENT_BUS.addListener(ArcaneEvents::noSpawnsOnTheRoof);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent e) ->
                com.panda.arcane.command.ArcaneCommand.register(e.getDispatcher(), e.getBuildContext()));
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!ArcaneConfig.disableVanillaTable()) return;
        if (!event.getLevel().getBlockState(event.getPos()).is(Blocks.ENCHANTING_TABLE)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS_SERVER);
        if (event.getEntity() instanceof ServerPlayer player) {
            player.sendOverlayMessage(Component.translatable("message.arcane.vanilla_table"));
        }
    }

    /** Opening a chest in the stacks disturbs the silent keepers nearby. */
    private static void onContainerOpen(net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getContainer() instanceof net.minecraft.world.inventory.ChestMenu)) return;
        if (!player.level().dimension().equals(com.panda.arcane.portal.PortalTravel.LIBRARY)) return;
        var box = player.getBoundingBox().inflate(12.0, 4.0, 12.0);
        for (var keeper : player.level().getEntitiesOfClass(com.panda.arcane.entity.SilentLibrarian.class, box)) {
            keeper.provoke(player, 600);
        }
    }

    /** Ink Bound: every pulse, carried items with the enchantment regain durability equal to its level. */
    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
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
    private static void noSpawnsOnTheRoof(net.neoforged.neoforge.event.entity.living.MobSpawnEvent.SpawnPlacementCheck e) {
        if (e.getPos().getY() <= com.panda.arcane.worldgen.LibraryLayout.ROOF) return;
        if (!e.getLevel().getLevel().dimension().equals(com.panda.arcane.portal.PortalTravel.LIBRARY)) return;
        if (net.minecraft.world.entity.EntitySpawnReason.isSpawner(e.getSpawnType()) || e.getSpawnType() == net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE) return;
        e.setResult(net.neoforged.neoforge.event.entity.living.MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
    }
}
