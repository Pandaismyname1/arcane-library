package com.panda.arcane.registry;

import com.panda.arcane.ArcaneMod;
import com.panda.arcane.entity.ArcaneBolt;
import com.panda.arcane.entity.Archivist;
import com.panda.arcane.entity.InkGolem;
import com.panda.arcane.entity.LumenMoth;
import com.panda.arcane.entity.SilentLibrarian;
import com.panda.arcane.entity.VaultWard;
import com.panda.arcane.portal.PortalTravel;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    private static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(ArcaneMod.MODID);

    public static final Supplier<EntityType<LumenMoth>> LUMEN_MOTH = ENTITIES.registerEntityType("lumen_moth", LumenMoth::new,
            MobCategory.AMBIENT, b -> b.sized(0.6F, 0.5F).eyeHeight(0.25F).clientTrackingRange(8));
    public static final Supplier<EntityType<SilentLibrarian>> SILENT_LIBRARIAN = ENTITIES.registerEntityType("silent_librarian", SilentLibrarian::new,
            MobCategory.MONSTER, b -> b.sized(0.6F, 1.95F).eyeHeight(1.7F).clientTrackingRange(10));
    public static final Supplier<EntityType<InkGolem>> INK_GOLEM = ENTITIES.registerEntityType("ink_golem", InkGolem::new,
            MobCategory.MONSTER, b -> b.sized(1.4F, 2.7F).eyeHeight(2.2F).clientTrackingRange(10));
    public static final Supplier<EntityType<VaultWard>> VAULT_WARD = ENTITIES.registerEntityType("vault_ward", VaultWard::new,
            MobCategory.MONSTER, b -> b.sized(0.7F, 1.4F).eyeHeight(1.0F).clientTrackingRange(10));
    public static final Supplier<EntityType<Archivist>> ARCHIVIST = ENTITIES.registerEntityType("archivist", Archivist::new,
            MobCategory.MONSTER, b -> b.sized(0.6F, 1.95F).eyeHeight(1.7F).fireImmune().clientTrackingRange(12));
    public static final Supplier<EntityType<ArcaneBolt>> ARCANE_BOLT = ENTITIES.registerEntityType("arcane_bolt", ArcaneBolt::new,
            MobCategory.MISC, b -> b.sized(0.4F, 0.4F).clientTrackingRange(4).updateInterval(2));

    // ---- Creatures A: new mobs ----
    public static final Supplier<EntityType<com.panda.arcane.entity.Inkling>> INKLING = ENTITIES.registerEntityType("inkling", com.panda.arcane.entity.Inkling::new,
            MobCategory.MONSTER, b -> b.sized(0.55F, 0.6F).eyeHeight(0.4F).clientTrackingRange(8));
    // ---- end Creatures A ----
    // ---- Creatures B (Page Wraith, Tome Mimic, Bookwyrm, Quill Stalker, Bookend Gargoyle) ----------------------------
    public static final Supplier<EntityType<com.panda.arcane.entity.PageWraith>> PAGE_WRAITH = ENTITIES.registerEntityType("page_wraith",
            com.panda.arcane.entity.PageWraith::new, MobCategory.MONSTER, b -> b.sized(0.7F, 1.6F).eyeHeight(1.25F).clientTrackingRange(10));
    public static final Supplier<EntityType<com.panda.arcane.entity.TomeMimic>> TOME_MIMIC = ENTITIES.registerEntityType("tome_mimic",
            com.panda.arcane.entity.TomeMimic::new, MobCategory.MONSTER, b -> b.sized(0.8F, 0.6F).eyeHeight(0.4F).clientTrackingRange(10));
    public static final Supplier<EntityType<com.panda.arcane.entity.Bookwyrm>> BOOKWYRM = ENTITIES.registerEntityType("bookwyrm",
            com.panda.arcane.entity.Bookwyrm::new, MobCategory.MONSTER, b -> b.sized(0.9F, 0.8F).eyeHeight(0.55F).clientTrackingRange(10));
    public static final Supplier<EntityType<com.panda.arcane.entity.QuillStalker>> QUILL_STALKER = ENTITIES.registerEntityType("quill_stalker",
            com.panda.arcane.entity.QuillStalker::new, MobCategory.MONSTER, b -> b.sized(1.1F, 0.75F).eyeHeight(0.5F).clientTrackingRange(10));
    public static final Supplier<EntityType<com.panda.arcane.entity.BookendGargoyle>> BOOKEND_GARGOYLE = ENTITIES.registerEntityType("bookend_gargoyle",
            com.panda.arcane.entity.BookendGargoyle::new, MobCategory.MONSTER, b -> b.sized(0.85F, 1.6F).eyeHeight(1.35F).clientTrackingRange(10));
    // ---- end Creatures B -----------------------------------------------------------------------------------------

    private ModEntities() {}

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        // ---- Creatures B
        com.panda.arcane.entity.MobsB.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(PAGE_WRAITH.get(), com.panda.arcane.entity.PageWraith.createAttributes().build());
            e.put(TOME_MIMIC.get(), com.panda.arcane.entity.TomeMimic.createAttributes().build());
            e.put(BOOKWYRM.get(), com.panda.arcane.entity.Bookwyrm.createAttributes().build());
            e.put(QUILL_STALKER.get(), com.panda.arcane.entity.QuillStalker.createAttributes().build());
            e.put(BOOKEND_GARGOYLE.get(), com.panda.arcane.entity.BookendGargoyle.createAttributes().build());
        });
        // ---- end Creatures B
        modBus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(LUMEN_MOTH.get(), LumenMoth.createAttributes().build());
            e.put(SILENT_LIBRARIAN.get(), SilentLibrarian.createAttributes().build());
            e.put(INK_GOLEM.get(), InkGolem.createAttributes().build());
            e.put(VAULT_WARD.get(), VaultWard.createAttributes().build());
            e.put(ARCHIVIST.get(), Archivist.createAttributes().build());
            e.put(INKLING.get(), com.panda.arcane.entity.Inkling.createAttributes().build()); // Creatures A
        });
        modBus.addListener((RegisterSpawnPlacementsEvent e) -> {
            // Natural spawns happen only inside the Library, wherever there is floor to stand on.
            e.register(SILENT_LIBRARIAN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (t, level, reason, pos, rand) -> level.getLevel().dimension().equals(PortalTravel.LIBRARY), RegisterSpawnPlacementsEvent.Operation.REPLACE);
            e.register(INK_GOLEM.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (t, level, reason, pos, rand) -> level.getLevel().dimension().equals(PortalTravel.LIBRARY), RegisterSpawnPlacementsEvent.Operation.REPLACE);
            e.register(LUMEN_MOTH.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (t, level, reason, pos, rand) -> level.getLevel().dimension().equals(PortalTravel.LIBRARY), RegisterSpawnPlacementsEvent.Operation.REPLACE);
            // Creatures A
            e.register(INKLING.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (t, level, reason, pos, rand) -> level.getLevel().dimension().equals(PortalTravel.LIBRARY), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        });
    }
}
