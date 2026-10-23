package com.panda.arcane.gametest;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.panda.arcane.ArcaneMod;
import com.panda.arcane.entity.ArcaneBolt;
import com.panda.arcane.knowledge.Knowledge;
import com.panda.arcane.logic.Mastery;
import com.panda.arcane.loot.RandomTomeFunction;
import com.panda.arcane.loot.RandomTomeFunction.Category;
import com.panda.arcane.portal.ArcanePortalShape;
import com.panda.arcane.registry.ModBlocks;
import com.panda.arcane.registry.ModComponents;
import com.panda.arcane.registry.ModEntities;
import com.panda.arcane.registry.ModItems;
import com.panda.arcane.item.TomeItem;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registration, data and behaviour checks that run on a GameTest server (data-driven tests). */
public final class ArcaneGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, ArcaneMod.MODID);
    private static final Identifier ARENA = ArcaneMod.id("test_arena");

    static {
        FUNCTIONS.register("lang_coverage", () -> ArcaneGameTests::langCoverage);
        FUNCTIONS.register("recipes_loaded", () -> ArcaneGameTests::recipesLoaded);
        FUNCTIONS.register("loot_tables_resolve", () -> ArcaneGameTests::lootTablesResolve);
        FUNCTIONS.register("vault_loot_has_tomes", () -> ArcaneGameTests::vaultLootHasTomes);
        FUNCTIONS.register("enchantments_registered", () -> ArcaneGameTests::enchantmentsRegistered);
        FUNCTIONS.register("tomes_for_every_enchantment", () -> ArcaneGameTests::tomesForEveryEnchantment);
        FUNCTIONS.register("knowledge_roundtrip", () -> ArcaneGameTests::knowledgeRoundtrip);
        FUNCTIONS.register("library_dimension_decodes", () -> ArcaneGameTests::libraryDimensionDecodes);
        FUNCTIONS.register("portal_shape", () -> ArcaneGameTests::portalShape);
        FUNCTIONS.register("entities_registered", () -> ArcaneGameTests::entitiesRegistered);
        FUNCTIONS.register("advancements_load", () -> ArcaneGameTests::advancementsLoad);
        // art and particles
        FUNCTIONS.register("art_particles_registered", () -> ArcaneGameTests::artParticlesRegistered);
        // compatibility with other mods' enchantments (the gametest run loads datapacks/compat_test)
        FUNCTIONS.register("modded_enchantments_drop_as_tomes", () -> ArcaneGameTests::moddedEnchantmentsDropAsTomes);
    }

    /**
     * Enchantments from another mod (compat_test: frostbite weight 6, hoarding treasure, withered curse) must be sorted
     * into tome pools automatically, actually drop from real loot tables, and be inscribable at the lectern.
     */
    private static void moddedEnchantmentsDropAsTomes(GameTestHelper helper) {
        var level = helper.getLevel();
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Identifier frostbite = Identifier.parse("compat_test:frostbite"), hoarding = Identifier.parse("compat_test:hoarding"),
                withered = Identifier.parse("compat_test:withered");
        if (enchantments.get(ResourceKey.create(Registries.ENCHANTMENT, frostbite)).isEmpty())
            throw helper.assertionException("compat_test pack not loaded: run with -Darcane.compatTest");
        java.util.function.Function<Category, List<Identifier>> pool = c ->
                RandomTomeFunction.candidates(enchantments, c).stream().map(r -> r.key().identifier()).toList();
        if (!pool.apply(Category.COMMON).contains(frostbite)) helper.fail("frostbite (weight 6) should be a common tome");
        if (!pool.apply(Category.RARE).contains(hoarding)) helper.fail("hoarding (treasure) should be a rare tome");
        for (Category c : Category.values()) {
            if (pool.apply(c).contains(withered)) helper.fail("curses must never drop as tomes, found in " + c);
            if (c != Category.COMMON && pool.apply(c).contains(frostbite)) helper.fail("frostbite also in " + c);
        }
        if (!pool.apply(Category.COMMON).contains(Identifier.parse("minecraft:sharpness"))) helper.fail("curated vanilla pool lost sharpness");
        // the real study table must hand them out
        LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, ArcaneMod.id("chests/restricted_vault")));
        java.util.Set<Identifier> seen = new java.util.HashSet<>();
        for (int i = 0; i < 2000 && !(seen.contains(frostbite) && seen.contains(hoarding)); i++) {
            LootParams params = new LootParams.Builder(level).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,
                    helper.absoluteVec(new net.minecraft.world.phys.Vec3(1, 1, 1))).create(LootContextParamSets.CHEST);
            for (ItemStack stack : table.getRandomItems(params)) {
                Identifier id = stack.get(ModComponents.TOME_ENCHANTMENT.get());
                if (id != null) seen.add(id);
            }
        }
        if (seen.contains(withered)) helper.fail("a curse dropped as a tome");
        if (!seen.contains(frostbite) || !seen.contains(hoarding)) helper.fail("modded tomes never dropped from the restricted vault: " + seen);
        // and the lectern offers them once known
        ItemStack sword = new ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
        var options = com.panda.arcane.lectern.LecternOptions.compute(level.registryAccess(), sword, Knowledge.EMPTY.with(frostbite, Mastery.STUDIED));
        if (options.stream().noneMatch(o -> o.id().equals(frostbite))) helper.fail("the lectern does not offer a known modded enchantment");
        helper.succeed();
    }

    /** Art: every particle id the code spawns resolves in the registry (an unknown id silently does nothing). */
    private static void artParticlesRegistered(GameTestHelper helper) {
        List<String> missing = new ArrayList<>();
        for (String id : List.of("lumen_mote", "ink_drop", "portal_glyph")) {
            if (BuiltInRegistries.PARTICLE_TYPE.getOptional(ArcaneMod.id(id)).isEmpty()) missing.add(id);
        }
        if (!missing.isEmpty()) helper.fail("particle types not registered: " + missing);
        if (com.panda.arcane.registry.ModBlocks.DECONSTRUCTION_TABLE.get().defaultBlockState().canOcclude())
            helper.fail("the deconstruction table has legs: it must not occlude its neighbours");
        helper.succeed();
    }

    private ArcaneGameTests() {}

    public static void register(IEventBus modBus) {
        FUNCTIONS.register(modBus);
        modBus.addListener(ArcaneGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(ArcaneMod.id("default"));
        for (var entry : FUNCTIONS.getEntries()) {
            ResourceKey<Consumer<GameTestHelper>> key = ResourceKey.create(Registries.TEST_FUNCTION, entry.getId());
            event.registerTest(key.identifier(), new FunctionGameTestInstance(key, new TestData<>(env, ARENA, 100, 0, true)));
        }
    }

    // ---- tests -------------------------------------------------------------------------------------------------

    private static void langCoverage(GameTestHelper helper) {
        var lang = net.minecraft.locale.Language.getInstance();
        List<String> missing = new ArrayList<>();
        ModItems.ITEMS.getEntries().forEach(h -> {
            if (h.get() instanceof TomeItem) return; // tomes name themselves from the enchantment
            String key = h.get().getDescriptionId();
            if (!lang.has(key)) missing.add(key);
        });
        ModBlocks.BLOCKS.getEntries().forEach(h -> {
            String key = h.get().getDescriptionId();
            if (!lang.has(key)) missing.add(key);
        });
        for (var type : List.of(ModEntities.LUMEN_MOTH, ModEntities.SILENT_LIBRARIAN, ModEntities.INK_GOLEM, ModEntities.VAULT_WARD,
                ModEntities.ARCHIVIST, ModEntities.ARCANE_BOLT)) {
            String key = type.get().getDescriptionId();
            if (!lang.has(key)) missing.add(key);
        }
        for (String key : List.of("itemGroup.arcane", "container.arcane.lectern", "container.arcane.deconstruction", "item.arcane.tome",
                "mastery.arcane.1", "mastery.arcane.2", "mastery.arcane.3", "message.arcane.tome.learned", "screen.arcane.lectern.inscribe",
                "screen.arcane.codex.title", "structure.arcane.ruined_librarian_tower")) {
            if (!lang.has(key)) missing.add(key);
        }
        var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        enchantments.listElements().forEach(ref -> {
            if (!ref.key().identifier().getNamespace().equals(ArcaneMod.MODID)) return;
            String key = ref.key().identifier().toLanguageKey("enchantment");
            if (!lang.has(key)) missing.add(key);
        });
        MinecraftServer server = helper.getLevel().getServer();
        server.getAdvancements().getAllAdvancements().forEach(holder -> {
            if (!holder.id().getNamespace().equals(ArcaneMod.MODID)) return;
            holder.value().display().ifPresent(d -> {
                for (Component c : List.of(d.title(), d.description())) {
                    if (c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t && !lang.has(t.getKey())) missing.add(t.getKey());
                }
            });
        });
        if (!missing.isEmpty()) helper.fail("missing lang keys: " + missing);
        helper.succeed();
    }

    private static void recipesLoaded(GameTestHelper helper) {
        var recipes = helper.getLevel().getServer().getRecipeManager();
        List<String> missing = new ArrayList<>();
        for (String id : List.of("arcane_lectern", "deconstruction_table", "inscribed_bricks", "chiseled_inscribed_bricks", "lumen_lantern",
                "knowledge_shelf", "ink_vial", "quill_of_insight", "codex", "lumen_dust_from_crystals", "book_of_portals")) {
            if (recipes.byKey(ResourceKey.create(Registries.RECIPE, ArcaneMod.id(id))).isEmpty()) missing.add(id);
        }
        if (!missing.isEmpty()) helper.fail("recipes not loaded: " + missing);
        helper.succeed();
    }

    private static final List<String> LOOT = List.of("chests/stacks", "chests/study", "chests/restricted_study", "chests/scriptorium", "chests/atrium",
            "chests/vault", "chests/restricted_vault", "brushing/suspicious_bookshelf", "chests/tower", "chests/tower_extra", "entities/archivist", "entities/lumen_moth",
            "entities/silent_librarian", "entities/ink_golem", "entities/vault_ward");

    private static void lootTablesResolve(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        List<String> missing = new ArrayList<>();
        for (String id : LOOT) {
            LootTable table = server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, ArcaneMod.id(id)));
            if (table == LootTable.EMPTY) missing.add(id);
        }
        for (var h : ModBlocks.BLOCKS.getEntries()) {
            if (h.get() == ModBlocks.LIBRARY_PORTAL.get() || h.get() == ModBlocks.SUSPICIOUS_BOOKSHELF.get()) continue; // both drop nothing by design
            Identifier key = h.getId();
            LootTable table = server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, key.withPrefix("blocks/")));
            if (table == LootTable.EMPTY) missing.add("blocks/" + key.getPath());
        }
        if (!missing.isEmpty()) helper.fail("loot tables missing: " + missing);
        helper.succeed();
    }

    /** Rolls the vault table many times: it must give tomes that carry a valid enchantment component. */
    private static void vaultLootHasTomes(GameTestHelper helper) {
        var level = helper.getLevel();
        LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, ArcaneMod.id("chests/vault")));
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        int tomes = 0;
        for (int i = 0; i < 300; i++) {
            LootParams params = new LootParams.Builder(level).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, helper.absoluteVec(new net.minecraft.world.phys.Vec3(1, 1, 1)))
                    .create(LootContextParamSets.CHEST);
            for (ItemStack stack : table.getRandomItems(params)) {
                if (stack.getItem() instanceof TomeItem) {
                    tomes++;
                    Identifier id = stack.get(ModComponents.TOME_ENCHANTMENT.get());
                    if (id == null) throw helper.assertionException("tome without an enchantment component");
                    if (enchantments.get(id).isEmpty()) throw helper.assertionException("tome names an unknown enchantment %s", id);
                }
            }
        }
        if (tomes < 100) helper.fail("vault loot should drop plenty of tomes, got " + tomes + " in 300 rolls");
        helper.succeed();
    }

    private static void enchantmentsRegistered(GameTestHelper helper) {
        var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        List<String> missing = new ArrayList<>();
        for (String id : List.of("lumen_step", "page_turn", "knowledge_leech", "ink_bound", "arcane_ward", "lumen_edge")) {
            Holder<Enchantment> holder = enchantments.get(ResourceKey.create(Registries.ENCHANTMENT, ArcaneMod.id(id))).orElse(null);
            if (holder == null) missing.add(id);
            else if (holder.value().getMaxLevel() < 1) missing.add(id + " has no levels");
        }
        if (!missing.isEmpty()) helper.fail("enchantments: " + missing);
        helper.succeed();
    }

    private static void tomesForEveryEnchantment(GameTestHelper helper) {
        var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        int count = 0;
        for (var ref : enchantments.listElements().toList()) {
            ItemStack stack = TomeItem.create(ModItems.TOME_STUDIED.get(), ref.key().identifier());
            if (stack.isEmpty() || stack.getHoverName().getString().isBlank()) throw helper.assertionException("tome for %s has no name", ref.key().identifier());
            count++;
        }
        if (count < 40) helper.fail("expected the whole enchantment registry, got " + count);
        helper.succeed();
    }

    private static void knowledgeRoundtrip(GameTestHelper helper) {
        Knowledge k = Knowledge.EMPTY.with(Identifier.parse("minecraft:sharpness"), Mastery.STUDIED).with(Identifier.parse("arcane:ink_bound"), Mastery.MASTERED);
        var encoded = Knowledge.CODEC.encodeStart(JsonOps.INSTANCE, k).getOrThrow();
        Knowledge back = Knowledge.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        if (!back.equals(k)) helper.fail("knowledge did not survive a codec round trip: " + back + " vs " + k);
        if (k.with(Identifier.parse("minecraft:sharpness"), Mastery.GLIMPSED) != k) helper.fail("knowledge must never go down");
        helper.succeed();
    }

    private static void libraryDimensionDecodes(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        try (BufferedReader reader = server.getResourceManager().getResourceOrThrow(Identifier.parse("arcane:dimension/library.json")).openAsReader()) {
            var json = JsonParser.parseReader(reader);
            var ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
            LevelStem stem = LevelStem.CODEC.parse(ops, json).getOrThrow();
            if (!(stem.generator() instanceof com.panda.arcane.worldgen.LibraryChunkGenerator)) helper.fail("library dimension has the wrong generator: " + stem.generator());
        } catch (Exception e) {
            throw helper.assertionException("library dimension json failed to decode: %s", e);
        }
        helper.succeed();
    }

    private static void portalShape(GameTestHelper helper) {
        var frame = ModBlocks.INSCRIBED_BRICKS.get().defaultBlockState();
        // a 2 x 3 frame on axis X at the origin of the arena: sill y=1, interior y 2..4, lintel y=5
        for (int x = 1; x <= 4; x++) {
            helper.setBlock(new BlockPos(x, 1, 3), frame);
            helper.setBlock(new BlockPos(x, 5, 3), frame);
        }
        for (int y = 2; y <= 4; y++) {
            helper.setBlock(new BlockPos(1, y, 3), frame);
            helper.setBlock(new BlockPos(4, y, 3), frame);
        }
        BlockPos inside = helper.absolutePos(new BlockPos(2, 2, 3));
        var shape = ArcanePortalShape.find(helper.getLevel(), inside, Direction.Axis.X);
        if (shape.isEmpty() || shape.get().width() != 2 || shape.get().height() != 3) throw helper.assertionException("complete frame not recognised: %s", shape);
        if (ArcanePortalShape.find(helper.getLevel(), inside, Direction.Axis.Z).isPresent()) helper.fail("a frame on the X axis must not light on Z");
        shape.get().light(helper.getLevel());
        helper.assertBlockState(new BlockPos(2, 3, 3), s -> s.is(ModBlocks.LIBRARY_PORTAL.get()), s -> Component.literal("interior not lit: " + s));
        // break a post: the shape is no longer complete
        helper.setBlock(new BlockPos(4, 3, 3), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        if (ArcanePortalShape.find(helper.getLevel(), inside, Direction.Axis.X).isPresent()) helper.fail("a broken frame must not count as a portal");
        helper.succeed();
    }

    private static void entitiesRegistered(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        for (EntityType<?> type : List.of(ModEntities.LUMEN_MOTH.get(), ModEntities.SILENT_LIBRARIAN.get(), ModEntities.INK_GOLEM.get(),
                ModEntities.VAULT_WARD.get(), ModEntities.ARCHIVIST.get())) {
            var entity = type.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            if (!(entity instanceof net.minecraft.world.entity.LivingEntity living) || living.getMaxHealth() < 1) bad.add(String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(type)));
        }
        if (ModEntities.ARCANE_BOLT.get().create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND) == null) bad.add("arcane_bolt");
        if (!bad.isEmpty()) helper.fail("entities failed to instantiate: " + bad);
        helper.succeed();
    }

    private static void advancementsLoad(GameTestHelper helper) {
        var advancements = helper.getLevel().getServer().getAdvancements();
        long count = advancements.getAllAdvancements().stream().filter(h -> h.id().getNamespace().equals(ArcaneMod.MODID)).count();
        if (count < 10) helper.fail("expected the Arcane advancement tab, found " + count + " advancements");
        helper.succeed();
    }
}
