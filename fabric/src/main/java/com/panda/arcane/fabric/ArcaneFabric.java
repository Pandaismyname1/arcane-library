package com.panda.arcane.fabric;

import com.panda.arcane.ArcaneConfig;
import com.panda.arcane.ArcaneMod;
import com.panda.arcane.command.ArcaneCommand;
import com.panda.arcane.event.ArcaneEvents;
import com.panda.arcane.platform.registry.DeferredRegister;
import com.panda.arcane.registry.ModBlocks;
import com.panda.arcane.registry.ModEntities;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.levelgen.Heightmap;

/** The Fabric entry point: registers the common content and wires the common hooks to Fabric API's events. */
public final class ArcaneFabric implements ModInitializer {
    /**
     * Registries whose entries others are made from, registered first in this order: items take blocks, components and
     * entity types (spawn eggs). The rest follow in the order the mod created them.
     */
    private static final List<ResourceKey<?>> FIRST = List.of(Registries.BLOCK, Registries.DATA_COMPONENT_TYPE, Registries.SOUND_EVENT,
            Registries.PARTICLE_TYPE, Registries.ENTITY_TYPE, Registries.BLOCK_ENTITY_TYPE, Registries.ITEM);

    @Override
    public void onInitialize() {
        ArcaneConfig.use(FabricConfig.load());
        ArcaneMod.init();
        DeferredRegister.all().stream()
                .sorted(Comparator.comparingInt(r -> {
                    int i = FIRST.indexOf(r.getRegistryKey());
                    return i < 0 ? FIRST.size() : i;
                }))
                .forEach(ArcaneFabric::registerAll);

        ModEntities.attributes(FabricDefaultAttributeRegistry::register);
        ModEntities.spawnRules(new ModEntities.SpawnRules() {
            @Override
            public <T extends Mob> void set(EntityType<T> type, SpawnPlacementType placement, Heightmap.Types heightmap,
                                            SpawnPlacements.SpawnPredicate<T> predicate) {
                SpawnPlacements.register(type, placement, heightmap, predicate);
            }
        });
        ((FabricBlockEntityType) BlockEntityTypes.BRUSHABLE_BLOCK).addValidBlock(ModBlocks.SUSPICIOUS_BOOKSHELF.get());

        UseBlockCallback.EVENT.register((player, level, hand, hit) -> ArcaneEvents.onUseBlock(player, level, hand, hit.getBlockPos()));
        ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(ArcaneEvents::onPlayerTick));
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> ArcaneCommand.register(dispatcher, context));
    }

    @SuppressWarnings("unchecked")
    private static <R> void registerAll(DeferredRegister<R> register) {
        Registry<R> registry = (Registry<R>) BuiltInRegistries.REGISTRY.getValue(register.getRegistryKey().identifier());
        if (registry == null) throw new IllegalStateException("No registry " + register.getRegistryKey().identifier());
        register.registerAll((key, value) -> Registry.register(registry, key, value));
    }
}
