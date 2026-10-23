package com.panda.arcane.registry;

import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.ServerLevelAccessor;

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
import com.panda.arcane.platform.registry.DeferredRegister;

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

    /** Every mob's attributes, for the loader's attribute registration. */
    public static void attributes(BiConsumer<EntityType<? extends LivingEntity>, AttributeSupplier> sink) {
        sink.accept(LUMEN_MOTH.get(), LumenMoth.createAttributes().build());
        sink.accept(SILENT_LIBRARIAN.get(), SilentLibrarian.createAttributes().build());
        sink.accept(INK_GOLEM.get(), InkGolem.createAttributes().build());
        sink.accept(VAULT_WARD.get(), VaultWard.createAttributes().build());
        sink.accept(ARCHIVIST.get(), Archivist.createAttributes().build());
        sink.accept(INKLING.get(), com.panda.arcane.entity.Inkling.createAttributes().build()); // Creatures A
        // ---- Creatures B
        sink.accept(PAGE_WRAITH.get(), com.panda.arcane.entity.PageWraith.createAttributes().build());
        sink.accept(TOME_MIMIC.get(), com.panda.arcane.entity.TomeMimic.createAttributes().build());
        sink.accept(BOOKWYRM.get(), com.panda.arcane.entity.Bookwyrm.createAttributes().build());
        sink.accept(QUILL_STALKER.get(), com.panda.arcane.entity.QuillStalker.createAttributes().build());
        sink.accept(BOOKEND_GARGOYLE.get(), com.panda.arcane.entity.BookendGargoyle.createAttributes().build());
    }

    /** A loader's way to set a mob's natural spawn rule (replacing any default). */
    public interface SpawnRules {
        <T extends Mob> void set(EntityType<T> type, SpawnPlacementType placement, Heightmap.Types heightmap, SpawnPlacements.SpawnPredicate<T> predicate);
    }

    /** Natural spawns happen only inside the Library, wherever there is floor to stand on. */
    public static void spawnRules(SpawnRules rules) {
        rules.set(SILENT_LIBRARIAN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::inLibrary);
        rules.set(INK_GOLEM.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::inLibrary);
        rules.set(LUMEN_MOTH.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::inLibrary);
        rules.set(INKLING.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::inLibrary); // Creatures A
    }

    private static <T extends Mob> boolean inLibrary(EntityType<T> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return level.getLevel().dimension().equals(PortalTravel.LIBRARY);
    }
}
