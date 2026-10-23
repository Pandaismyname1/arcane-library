package com.panda.arcane.neoforge;

import com.panda.arcane.ArcaneConfig;
import com.panda.arcane.ArcaneMod;
import com.panda.arcane.command.ArcaneCommand;
import com.panda.arcane.event.ArcaneEvents;
import com.panda.arcane.event.ShelfFeedback;
import com.panda.arcane.platform.registry.DeferredRegister;
import com.panda.arcane.registry.ModBlocks;
import com.panda.arcane.registry.ModEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** The NeoForge entry point: registers the common content and wires the common hooks to NeoForge's events. */
@Mod(ArcaneMod.MODID)
public final class ArcaneNeoForge {
    public ArcaneNeoForge(IEventBus modBus, ModContainer container) {
        ArcaneMod.init();
        modBus.addListener((RegisterEvent e) -> DeferredRegister.all().forEach(register -> registerInto(e, register)));
        NeoForgeAttachments.register(modBus);
        container.registerConfig(ModConfig.Type.SYNCED, NeoForgeConfig.SPEC);
        ArcaneConfig.use(new NeoForgeConfig());

        modBus.addListener((EntityAttributeCreationEvent e) -> ModEntities.attributes(e::put));
        modBus.addListener((RegisterSpawnPlacementsEvent e) -> ModEntities.spawnRules(new ModEntities.SpawnRules() {
            @Override
            public <T extends Mob> void set(EntityType<T> type, SpawnPlacementType placement, Heightmap.Types heightmap,
                                            SpawnPlacements.SpawnPredicate<T> predicate) {
                e.register(type, placement, heightmap, predicate, RegisterSpawnPlacementsEvent.Operation.REPLACE);
            }
        }));
        modBus.addListener((BlockEntityTypeAddBlocksEvent e) -> e.modify(BlockEntityTypes.BRUSHABLE_BLOCK, ModBlocks.SUSPICIOUS_BOOKSHELF.get()));
        if (System.getProperty("arcane.compatTest") != null) {
            // test runs only: a built-in pack with enchantments shaped like another mod's (datapacks/compat_test)
            modBus.addListener((AddPackFindersEvent e) -> e.addPackFinders(ArcaneMod.id("datapacks/compat_test"), PackType.SERVER_DATA,
                    Component.literal("Arcane compat test"), PackSource.BUILT_IN, true, Pack.Position.TOP));
        }

        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener((PlayerInteractEvent.RightClickBlock e) -> {
            InteractionResult result = ArcaneEvents.onUseBlock(e.getEntity(), e.getLevel(), e.getHand(), e.getPos());
            if (result == InteractionResult.PASS) return;
            e.setCanceled(true);
            e.setCancellationResult(result);
        });
        game.addListener((PlayerTickEvent.Post e) -> ArcaneEvents.onPlayerTick(e.getEntity()));
        game.addListener((PlayerContainerEvent.Open e) -> ArcaneEvents.onContainerOpen(e.getEntity(), e.getContainer()));
        game.addListener((MobSpawnEvent.SpawnPlacementCheck e) -> {
            if (!ArcaneEvents.spawnAllowed(e.getLevel(), e.getPos(), e.getSpawnType())) e.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
        });
        game.addListener((RegisterCommandsEvent e) -> ArcaneCommand.register(e.getDispatcher(), e.getBuildContext()));
        game.addListener((EntityJoinLevelEvent e) -> {
            if (!e.loadedFromDisk()) ShelfFeedback.onSpawn(e.getEntity(), e.getLevel());
        });

        com.panda.arcane.gametest.ArcaneGameTests.register(modBus);
        com.panda.arcane.gametest.CreaturesAGameTests.register(modBus);
        com.panda.arcane.gametest.MobsBGameTests.register(modBus);
    }

    private static <R> void registerInto(RegisterEvent event, DeferredRegister<R> register) {
        event.register(register.getRegistryKey(), helper -> register.registerAll(helper::register));
    }
}
