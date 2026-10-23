package com.panda.arcane.gametest;

import com.panda.arcane.ArcaneMod;
import com.panda.arcane.entity.BookendGargoyle;
import com.panda.arcane.entity.Bookwyrm;
import com.panda.arcane.entity.MobsB;
import com.panda.arcane.entity.PageWraith;
import com.panda.arcane.entity.QuillStalker;
import com.panda.arcane.entity.VariantMonster;
import com.panda.arcane.registry.ModEntities;
import com.panda.arcane.worldgen.LibraryLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Creatures B checks: variants, spawn rolls by zone, loot tables, lang, and two variant mechanics. */
public final class MobsBGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, ArcaneMod.MODID);
    private static final Identifier ARENA = ArcaneMod.id("test_arena");

    static {
        FUNCTIONS.register("mobs_b_variants", () -> MobsBGameTests::variants);
        FUNCTIONS.register("mobs_b_zone_rolls", () -> MobsBGameTests::zoneRolls);
        FUNCTIONS.register("mobs_b_loot_and_lang", () -> MobsBGameTests::lootAndLang);
        FUNCTIONS.register("mobs_b_obsidian_ignores_arrows", () -> MobsBGameTests::obsidianIgnoresArrows);
        FUNCTIONS.register("mobs_b_statue_halves_damage", () -> MobsBGameTests::statueHalvesDamage);
        FUNCTIONS.register("mobs_b_granite_hunts_unprovoked", () -> h -> gargoyleHunts(h, BookendGargoyle.GRANITE));
        FUNCTIONS.register("mobs_b_marble_hunts_unprovoked", () -> h -> gargoyleHunts(h, BookendGargoyle.MARBLE));
        FUNCTIONS.register("mobs_b_obsidian_hunts_unprovoked", () -> h -> gargoyleHunts(h, BookendGargoyle.OBSIDIAN));
        FUNCTIONS.register("library_dimension_is_stable", () -> MobsBGameTests::libraryIsStable);
        FUNCTIONS.register("portal_home_is_the_entry_portal", () -> MobsBGameTests::homeIsEntryPortal);
        FUNCTIONS.register("portal_home_without_entry_is_spawn", () -> MobsBGameTests::homeWithoutEntryIsSpawn);
    }

    private MobsBGameTests() {}

    public static void register(IEventBus modBus) {
        FUNCTIONS.register(modBus);
        modBus.addListener(MobsBGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(ArcaneMod.id("mobs_b"));
        for (var entry : FUNCTIONS.getEntries()) {
            ResourceKey<Consumer<GameTestHelper>> key = ResourceKey.create(Registries.TEST_FUNCTION, entry.getId());
            event.registerTest(key.identifier(), new FunctionGameTestInstance(key, new TestData<>(env, ARENA, 100, 0, true)));
        }
    }

    private static List<Supplier<? extends EntityType<? extends VariantMonster>>> types() {
        return List.of(ModEntities.PAGE_WRAITH, ModEntities.TOME_MIMIC, ModEntities.BOOKWYRM, ModEntities.QUILL_STALKER, ModEntities.BOOKEND_GARGOYLE);
    }

    private static VariantMonster make(GameTestHelper helper, EntityType<? extends VariantMonster> type) {
        VariantMonster m = type.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        if (m == null) throw helper.assertionException("could not create " + type);
        return m;
    }

    /** Every type has 3+ variants, and setting a variant changes its stats. */
    private static void variants(GameTestHelper helper) {
        List<String> bad = new ArrayList<>();
        for (var t : types()) {
            VariantMonster m = make(helper, t.get());
            String[] names = m.variantNames();
            if (names.length < 3) bad.add(m.typeName() + " has " + names.length + " variants");
            java.util.Set<Float> healths = new java.util.HashSet<>();
            for (int i = 0; i < names.length; i++) {
                m.setVariantFresh(i);
                if (!m.variantName().equals(names[i])) bad.add(m.typeName() + " variant " + i + " did not stick");
                healths.add(m.getMaxHealth());
            }
            if (healths.size() < 2) bad.add(m.typeName() + " variants share one max health");
        }
        if (!bad.isEmpty()) helper.fail(String.join("; ", bad));
        helper.succeed();
    }

    /** Ordinary stacks never roll the Restricted-only variants; the Restricted Section does. */
    private static void zoneRolls(GameTestHelper helper) {
        MobsB.Zone stacks = new MobsB.Zone(true, false, LibraryLayout.Kind.STACKS);
        MobsB.Zone restricted = new MobsB.Zone(true, true, LibraryLayout.Kind.STACKS);
        RandomSource random = RandomSource.create(42);
        record Hard(EntityType<? extends VariantMonster> type, int minHard) {}
        List<Hard> hard = List.of(new Hard(ModEntities.PAGE_WRAITH.get(), PageWraith.CURSED), new Hard(ModEntities.BOOKWYRM.get(), Bookwyrm.ANCIENT),
                new Hard(ModEntities.QUILL_STALKER.get(), QuillStalker.SCRIBE), new Hard(ModEntities.BOOKEND_GARGOYLE.get(), BookendGargoyle.OBSIDIAN));
        List<String> bad = new ArrayList<>();
        for (Hard h : hard) {
            VariantMonster m = make(helper, h.type());
            int hardOutside = 0, hardInside = 0;
            for (int i = 0; i < 400; i++) {
                if (m.pickVariantFor(stacks, random) >= h.minHard()) hardOutside++;
                if (m.pickVariantFor(restricted, random) >= h.minHard()) hardInside++;
                if (m.pickVariantFor(MobsB.Zone.OUTSIDE, random) >= h.minHard()) hardOutside++;
            }
            if (hardOutside > 0) bad.add(m.typeName() + " rolled a restricted variant outside " + hardOutside + "x");
            if (hardInside == 0) bad.add(m.typeName() + " never rolled its hard variant in the Restricted Section");
        }
        if (!bad.isEmpty()) helper.fail(String.join("; ", bad));
        helper.succeed();
    }

    private static void lootAndLang(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var lang = net.minecraft.locale.Language.getInstance();
        List<String> missing = new ArrayList<>();
        for (var t : types()) {
            VariantMonster m = make(helper, t.get());
            List<String> ids = new ArrayList<>(List.of("entities/" + m.typeName()));
            for (String v : m.variantNames()) {
                ids.add("entities/" + m.typeName() + "/" + v);
                String key = t.get().getDescriptionId() + "." + v;
                if (!lang.has(key)) missing.add(key);
            }
            for (String id : ids) {
                if (server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, ArcaneMod.id(id))) == LootTable.EMPTY) missing.add(id);
            }
            if (!lang.has(t.get().getDescriptionId())) missing.add(t.get().getDescriptionId());
        }
        if (!missing.isEmpty()) helper.fail("missing: " + missing);
        helper.succeed();
    }

    private static void obsidianIgnoresArrows(GameTestHelper helper) {
        BookendGargoyle g = helper.spawn(ModEntities.BOOKEND_GARGOYLE.get(), new BlockPos(2, 1, 2));
        g.setVariantFresh(BookendGargoyle.OBSIDIAN);
        var arrow = EntityTypes.ARROW.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        if (arrow == null) throw helper.assertionException("no arrow");
        float before = g.getHealth();
        g.hurtServer(helper.getLevel(), helper.getLevel().damageSources().arrow(arrow, null), 6.0F);
        if (g.getHealth() != before) helper.fail("obsidian gargoyle took arrow damage");
        g.setInvulnerableTime(0);
        g.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 6.0F);
        if (g.getHealth() >= before) helper.fail("obsidian gargoyle took no melee damage");
        helper.succeed();
    }

    private static void statueHalvesDamage(GameTestHelper helper) {
        BookendGargoyle statue = helper.spawn(ModEntities.BOOKEND_GARGOYLE.get(), new BlockPos(1, 1, 1));
        BookendGargoyle awake = helper.spawn(ModEntities.BOOKEND_GARGOYLE.get(), new BlockPos(3, 1, 3));
        statue.setVariantFresh(BookendGargoyle.GRANITE);
        awake.setVariantFresh(BookendGargoyle.GRANITE);
        statue.setStatue(true);
        awake.setStatue(false);
        var src = helper.getLevel().damageSources().generic();
        float a0 = statue.getHealth(), b0 = awake.getHealth();
        statue.hurtServer(helper.getLevel(), src, 8.0F);
        awake.hurtServer(helper.getLevel(), src, 8.0F);
        float a = a0 - statue.getHealth(), b = b0 - awake.getHealth();
        if (Math.abs(a - b * 0.5F) > 0.01F) helper.fail("statue took " + a + ", awake took " + b);
        helper.succeed();
    }

    /**
     * A real in-level player in survival. GameTestHelper's mock server player always reports creative, which mobs
     * never target. Discard it when the test ends.
     */
    public static ServerPlayer survivalPlayer(GameTestHelper helper, double x, double y, double z) {
        var level = helper.getLevel();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-survivor"), false);
        ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        // the test server places players in its default (creative) mode, which also makes them invulnerable
        player.setGameMode(GameType.SURVIVAL);
        player.snapTo(helper.absoluteVec(new Vec3(x, y, z)));
        return player;
    }

    /** A perched statue notices a survival player in range, wakes and closes in without being hit first. */
    private static void gargoyleHunts(GameTestHelper helper, int variant) {
        BookendGargoyle g = helper.spawn(ModEntities.BOOKEND_GARGOYLE.get(), new BlockPos(5, 1, 2));
        g.setVariantFresh(variant);
        g.setStatue(true);
        // the player stands 3 blocks off and faces away, so a granite gargoyle is free to move
        ServerPlayer player = survivalPlayer(helper, 5.5, 1, 5.5);
        player.setYRot(0);
        player.setYHeadRot(0);
        double start = g.distanceTo(player);
        helper.runAtTickTime(60, () -> {
            try {
                helper.assertTrue(!g.isStatue(), "the statue woke without being hit");
                helper.assertTrue(g.getTarget() == player, "it targets the player, got " + g.getTarget());
                helper.assertTrue(g.distanceTo(player) < start - 0.5 || player.getHealth() < player.getMaxHealth(),
                        "it closed in or hit (distance " + start + " -> " + g.distanceTo(player) + ")");
            } finally {
                player.discard();
            }
            helper.succeed();
        });
    }

    /** The Library must not mark worlds "experimental" (that asked for confirmation on every new world). */
    private static void libraryIsStable(GameTestHelper helper) {
        // the test server has no Library level; vanilla treats a non-vanilla dimension by its key alone, so any stem will do
        var level = helper.getLevel();
        var key = ResourceKey.create(Registries.LEVEL_STEM, ArcaneMod.id("library"));
        var overworld = new net.minecraft.world.level.dimension.LevelStem(level.dimensionTypeRegistration(), level.getChunkSource().getGenerator());
        var otherType = level.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(net.minecraft.world.level.dimension.BuiltinDimensionTypes.NETHER);
        var stem = new net.minecraft.world.level.dimension.LevelStem(otherType, level.getChunkSource().getGenerator());
        var none = new net.minecraft.core.MappedRegistry<>(Registries.LEVEL_STEM, com.mojang.serialization.Lifecycle.stable()).freeze();
        var baked = new net.minecraft.world.level.levelgen.WorldDimensions(java.util.Map.of(net.minecraft.world.level.dimension.LevelStem.OVERWORLD, overworld, key, stem)).bake(none);
        var lifecycle = baked.dimensions().registrationInfo(key).map(net.minecraft.core.RegistrationInfo::lifecycle).orElse(null);
        helper.assertTrue(lifecycle == com.mojang.serialization.Lifecycle.stable(), "the Library dimension bakes as " + lifecycle);
        helper.succeed();
    }

    /** A Library portal takes you back to the very spot of the portal you entered by, rebuilt there if it is gone. */
    private static void homeIsEntryPortal(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper, 1.5, 1, 1.5);
        BlockPos entry = helper.absolutePos(new BlockPos(4, 2, 4));
        player.setData(com.panda.arcane.registry.ModAttachments.PORTAL_RETURN.get(),
                java.util.Optional.of(net.minecraft.core.GlobalPos.of(helper.getLevel().dimension(), entry)));
        try {
            var t = com.panda.arcane.portal.PortalTravel.leave(helper.getLevel().getServer(), helper.getLevel(), player);
            helper.assertTrue(t != null, "a way home");
            helper.assertTrue(t.newLevel() == helper.getLevel(), "home is the level I came from");
            helper.assertTrue(t.position().distanceTo(Vec3.atBottomCenterOf(entry)) < 3.5, "home is the entry portal, got " + t.position() + " for " + entry);
            helper.assertTrue(com.panda.arcane.portal.PortalTravel.findNear(helper.getLevel(), entry) != null, "a portal stands there again");
            var again = com.panda.arcane.portal.PortalTravel.leave(helper.getLevel().getServer(), helper.getLevel(), player);
            helper.assertTrue(again != null && again.position().distanceTo(t.position()) < 1.0, "every Library portal leads to the same place");
        } finally {
            player.discard();
        }
        helper.succeed();
    }

    /** With no entry portal on record, home is the world spawn, not a position mapped from Library coordinates. */
    private static void homeWithoutEntryIsSpawn(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper, 1.5, 1, 1.5);
        try {
            var server = helper.getLevel().getServer();
            var t = com.panda.arcane.portal.PortalTravel.leave(server, helper.getLevel(), player);
            BlockPos spawn = server.getRespawnData().pos();
            helper.assertTrue(t != null, "a way home");
            double dx = t.position().x - spawn.getX(), dz = t.position().z - spawn.getZ();
            helper.assertTrue(dx * dx + dz * dz < 16 * 16, "home is near world spawn " + spawn + ", got " + t.position());
        } finally {
            player.discard();
        }
        helper.succeed();
    }
}
