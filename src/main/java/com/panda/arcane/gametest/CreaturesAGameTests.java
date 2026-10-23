package com.panda.arcane.gametest;

import com.panda.arcane.ArcaneMod;
import com.panda.arcane.entity.CreatureZones;
import com.panda.arcane.entity.InkGolem;
import com.panda.arcane.entity.Inkling;
import com.panda.arcane.entity.LumenMoth;
import com.panda.arcane.entity.SilentLibrarian;
import com.panda.arcane.entity.VaultWard;
import com.panda.arcane.registry.ModEntities;
import com.panda.arcane.worldgen.LibraryLayout;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** One GameTest per Creatures A mechanic: each variant does what its description says. */
public final class CreaturesAGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, ArcaneMod.MODID);
    private static final Identifier ARENA = ArcaneMod.id("test_arena");

    static {
        FUNCTIONS.register("creatures_a_variant_zones", () -> CreaturesAGameTests::variantZones);
        FUNCTIONS.register("moth_ember_lights_candles", () -> CreaturesAGameTests::emberLightsCandles);
        FUNCTIONS.register("moth_void_snuffs_candles", () -> CreaturesAGameTests::voidSnuffsCandles);
        FUNCTIONS.register("moth_pale_night_vision", () -> CreaturesAGameTests::paleNightVision);
        FUNCTIONS.register("librarian_clerk_steals", () -> CreaturesAGameTests::clerkSteals);
        FUNCTIONS.register("librarian_head_rallies", () -> CreaturesAGameTests::headRallies);
        FUNCTIONS.register("librarian_censor_ink", () -> CreaturesAGameTests::censorInk);
        FUNCTIONS.register("librarian_notices_intruder", () -> CreaturesAGameTests::noticesIntruder);
        FUNCTIONS.register("golem_ironbound_slam", () -> CreaturesAGameTests::ironboundSlam);
        FUNCTIONS.register("golem_blotted_brood", () -> CreaturesAGameTests::blottedBrood);
        FUNCTIONS.register("golem_ink_puddle", () -> CreaturesAGameTests::inkPuddle);
        FUNCTIONS.register("ward_keys_lock", () -> CreaturesAGameTests::keysLock);
        FUNCTIONS.register("ward_bulwark_reflects", () -> CreaturesAGameTests::bulwarkReflects);
        FUNCTIONS.register("ward_sentinel_links", () -> CreaturesAGameTests::sentinelLinks);
        FUNCTIONS.register("inkling_red_bursts", () -> CreaturesAGameTests::redBursts);
        FUNCTIONS.register("inkling_black_splits", () -> CreaturesAGameTests::blackSplits);
        FUNCTIONS.register("inkling_gold_drops_dust", () -> CreaturesAGameTests::goldDropsDust);
    }

    private CreaturesAGameTests() {}

    public static void register(IEventBus modBus) {
        FUNCTIONS.register(modBus);
        modBus.addListener(CreaturesAGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(ArcaneMod.id("creatures_a"));
        for (var entry : FUNCTIONS.getEntries()) {
            ResourceKey<Consumer<GameTestHelper>> key = ResourceKey.create(Registries.TEST_FUNCTION, entry.getId());
            event.registerTest(key.identifier(), new FunctionGameTestInstance(key, new TestData<>(env, ARENA, 120, 0, true)));
        }
    }

    // ---- helpers -----------------------------------------------------------------------------------------------

    private static <T extends net.minecraft.world.entity.Mob> T mob(GameTestHelper helper, EntityType<T> type, double x, double y, double z) {
        T m = helper.spawn(type, new Vec3(x, y, z));
        m.setNoAi(true);
        return m;
    }

    private static AABB arena(GameTestHelper helper) {
        return new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(12, 8, 12);
    }

    // ---- tests -------------------------------------------------------------------------------------------------

    /** Variants follow the Library layout: a vault's heart gets the Warden of Keys, streets and rooms near the gate never get it. */
    private static void variantZones(GameTestHelper helper) {
        RandomSource rng = RandomSource.create(7);
        LibraryLayout layout = new LibraryLayout(99L);
        int vaults = 0;
        for (int cx = -8; cx <= 8; cx++) for (int cz = -8; cz <= 8; cz++) {
            var cell = layout.cell(cx, cz);
            CreatureZones centre = CreatureZones.of(99L, cell.originX() + 8, cell.originZ() + 8);
            if (cell.kind() == LibraryLayout.Kind.VAULT) {
                vaults++;
                helper.assertTrue(VaultWard.pickVariant(centre, rng) == VaultWard.Variant.KEYS, "vault centre must get the Warden of Keys");
            } else {
                for (int i = 0; i < 20; i++) helper.assertTrue(VaultWard.pickVariant(centre, rng) != VaultWard.Variant.KEYS, "no Warden of Keys outside vaults");
            }
            if (!cell.restricted()) {
                for (int i = 0; i < 20; i++) {
                    helper.assertTrue(LumenMoth.pickVariant(centre, rng) != LumenMoth.Variant.VOID, "Void moths only in the Restricted Section");
                    helper.assertTrue(SilentLibrarian.pickVariant(centre, rng) != SilentLibrarian.Variant.HEAD, "Head Librarians only in the Restricted Section");
                }
            }
        }
        helper.assertTrue(vaults > 0, "layout has vaults");
        int deep = 0;
        CreatureZones restricted = new CreatureZones(true, true, LibraryLayout.Kind.STACKS, 3, 3);
        for (int i = 0; i < 400; i++) if (Inkling.pickVariant(restricted, rng) == Inkling.Variant.RED) deep++;
        int shallow = 0;
        CreatureZones near = new CreatureZones(true, false, LibraryLayout.Kind.STACKS, 3, 3);
        for (int i = 0; i < 400; i++) if (Inkling.pickVariant(near, rng) == Inkling.Variant.RED) shallow++;
        helper.assertTrue(deep > shallow * 2, "Red Inklings should be far more common deep in (" + deep + " vs " + shallow + ")");
        helper.succeed();
    }

    private static void emberLightsCandles(GameTestHelper helper) {
        helper.setBlock(new BlockPos(4, 1, 4), Blocks.CANDLE.defaultBlockState().setValue(BlockStateProperties.LIT, false));
        LumenMoth moth = mob(helper, ModEntities.LUMEN_MOTH.get(), 4.5, 2.5, 5.5);
        moth.setVariant(LumenMoth.Variant.EMBER);
        int lit = moth.touchLights(helper.getLevel(), true);
        helper.assertTrue(lit == 1, "ember moth should light one candle, lit " + lit);
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(4, 1, 4))).getValue(BlockStateProperties.LIT), "candle is lit");
        helper.succeed();
    }

    private static void voidSnuffsCandles(GameTestHelper helper) {
        helper.setBlock(new BlockPos(4, 1, 4), Blocks.CANDLE.defaultBlockState().setValue(BlockStateProperties.LIT, true));
        LumenMoth moth = mob(helper, ModEntities.LUMEN_MOTH.get(), 4.5, 2.5, 5.5);
        moth.setVariant(LumenMoth.Variant.VOID);
        moth.pulse(helper.getLevel());
        helper.assertTrue(!helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(4, 1, 4))).getValue(BlockStateProperties.LIT), "void moth snuffs the candle");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    private static void paleNightVision(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        player.snapTo(helper.absoluteVec(new Vec3(6.5, 1, 6.5)));
        LumenMoth moth = mob(helper, ModEntities.LUMEN_MOTH.get(), 6.5, 2.5, 8.5);
        moth.setVariant(LumenMoth.Variant.PALE);
        helper.runAtTickTime(2, () -> {
            moth.pulse(helper.getLevel());
            helper.assertTrue(player.hasEffect(MobEffects.NIGHT_VISION), "pale moth grants night vision");
            player.discard();
            helper.succeed();
        });
    }

    private static void clerkSteals(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(12, new ItemStack(Items.DIAMOND, 5));
        SilentLibrarian clerk = mob(helper, ModEntities.SILENT_LIBRARIAN.get(), 5.5, 1, 5.5);
        clerk.setVariant(SilentLibrarian.Variant.CLERK);
        helper.assertTrue(clerk.steal(player), "clerk takes a stack");
        helper.assertTrue(player.getInventory().getItem(12).isEmpty(), "the stack left the player's pack");
        helper.assertTrue(clerk.stolen().is(Items.DIAMOND) && clerk.stolen().getCount() == 5, "the clerk holds it");
        helper.assertTrue(clerk.visualFlag(), "the clerk shows it is carrying loot");
        clerk.kill(helper.getLevel());
        helper.succeedWhen(() -> helper.assertTrue(!helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, arena(helper),
                e -> e.getItem().is(Items.DIAMOND)).isEmpty(), "killing the clerk drops the stolen stack"));
    }

    private static void headRallies(GameTestHelper helper) {
        SilentLibrarian head = mob(helper, ModEntities.SILENT_LIBRARIAN.get(), 5.5, 1, 5.5);
        head.setVariant(SilentLibrarian.Variant.HEAD);
        SilentLibrarian a = mob(helper, ModEntities.SILENT_LIBRARIAN.get(), 2.5, 1, 2.5);
        SilentLibrarian b = mob(helper, ModEntities.SILENT_LIBRARIAN.get(), 9.5, 1, 9.5);
        Pig cause = mob(helper, EntityTypes.PIG, 7.5, 1, 3.5);
        helper.runAtTickTime(2, () -> {
            int answered = head.rally(cause);
            // >= 2: tests run side by side, so a librarian from a neighbouring test may also hear the call
            helper.assertTrue(answered >= 2, "both librarians answer the Head Librarian, got " + answered);
            helper.assertTrue(a.isAngry() && b.isAngry(), "the called librarians are angry");
            helper.assertTrue(head.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SCALE) > 1.1, "the Head Librarian stands taller");
            helper.succeed();
        });
    }

    private static void censorInk(GameTestHelper helper) {
        SilentLibrarian censor = mob(helper, ModEntities.SILENT_LIBRARIAN.get(), 5.5, 1, 5.5);
        censor.setVariant(SilentLibrarian.Variant.CENSOR);
        Pig pig = mob(helper, EntityTypes.PIG, 6.5, 1, 5.5);
        censor.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(pig), 2.0F);
        helper.assertTrue(censor.hasEffect(MobEffects.INVISIBILITY), "the censor vanishes into its ink");
        helper.succeedWhen(() -> helper.assertTrue(!helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.AreaEffectCloud.class, arena(helper)).isEmpty(),
                "a blinding ink cloud is left behind"));
    }

    private static void ironboundSlam(GameTestHelper helper) {
        InkGolem golem = mob(helper, ModEntities.INK_GOLEM.get(), 5.5, 1, 5.5);
        golem.setVariant(InkGolem.Variant.IRONBOUND);
        Pig pig = mob(helper, EntityTypes.PIG, 7.5, 1, 5.5);
        float before = pig.getHealth();
        helper.assertTrue(golem.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE) >= 1.0, "iron-bound cannot be knocked back");
        helper.runAtTickTime(2, () -> {
            int hit = golem.slam(helper.getLevel());
            helper.assertTrue(hit == 1, "the slam hits the pig, hit " + hit);
            helper.assertTrue(pig.getHealth() < before, "the pig is hurt");
            helper.assertTrue(pig.getDeltaMovement().y > 0.5, "the pig is thrown up, dy=" + pig.getDeltaMovement().y);
            helper.succeed();
        });
    }

    private static void blottedBrood(GameTestHelper helper) {
        InkGolem golem = mob(helper, ModEntities.INK_GOLEM.get(), 5.5, 1, 5.5);
        golem.setVariant(InkGolem.Variant.BLOTTED);
        helper.runAtTickTime(2, () -> golem.kill(helper.getLevel()));
        helper.succeedWhen(() -> {
            int n = helper.getLevel().getEntitiesOfClass(Inkling.class, arena(helper)).size();
            helper.assertTrue(n == InkGolem.BROOD, "a blotted golem bursts into " + InkGolem.BROOD + " inklings, found " + n);
        });
    }

    private static void inkPuddle(GameTestHelper helper) {
        InkGolem golem = mob(helper, ModEntities.INK_GOLEM.get(), 5.5, 1, 5.5);
        var cloud = golem.dropPuddle(helper.getLevel());
        helper.assertTrue(cloud.getRadius() > 1.0F, "the puddle has a real radius");
        helper.succeedWhen(() -> helper.assertTrue(!helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.AreaEffectCloud.class, arena(helper)).isEmpty(),
                "an ink puddle is on the floor"));
    }

    private static void keysLock(GameTestHelper helper) {
        BlockPos chest = new BlockPos(2, 1, 2);
        helper.setBlock(chest, Blocks.CHEST);
        VaultWard ward = mob(helper, ModEntities.VAULT_WARD.get(), 6.5, 2, 6.5);
        ward.setVariant(VaultWard.Variant.KEYS);
        helper.runAtTickTime(2, () -> {
            ServerLevel level = helper.getLevel();
            helper.assertTrue(VaultWard.isLocked(level, helper.absolutePos(chest)), "the chest is sealed while the warden lives");
            helper.assertTrue(ward.lockedContainers(level).contains(helper.absolutePos(chest)), "the warden lists the chest");
            ward.kill(level);
        });
        helper.runAtTickTime(6, () -> {
            helper.assertTrue(!VaultWard.isLocked(helper.getLevel(), helper.absolutePos(chest)), "the chest opens once the warden falls");
            helper.succeed();
        });
    }

    private static void bulwarkReflects(GameTestHelper helper) {
        VaultWard ward = mob(helper, ModEntities.VAULT_WARD.get(), 5.5, 2, 5.5);
        ward.setVariant(VaultWard.Variant.BULWARK);
        Pig thrower = mob(helper, EntityTypes.PIG, 9.5, 1, 5.5);
        Snowball ball = helper.spawn(EntityTypes.SNOWBALL, new Vec3(7.5, 2.5, 5.5));
        ball.setOwner(thrower);
        ball.setDeltaMovement(-0.8, 0, 0);
        float health = ward.getHealth();
        boolean hurt = ward.hurtServer(helper.getLevel(), helper.getLevel().damageSources().thrown(ball, thrower), 4.0F);
        helper.assertTrue(!hurt && ward.getHealth() == health, "the bulwark takes no damage from projectiles");
        helper.assertTrue(ball.getOwner() == ward, "the projectile now belongs to the ward");
        helper.assertTrue(ball.getDeltaMovement().x > 0.3, "the projectile flies back toward its thrower, vx=" + ball.getDeltaMovement().x);
        helper.succeed();
    }

    private static void sentinelLinks(GameTestHelper helper) {
        VaultWard ward = mob(helper, ModEntities.VAULT_WARD.get(), 3.5, 2, 3.5);
        ward.setVariant(VaultWard.Variant.SENTINEL);
        SilentLibrarian ally = mob(helper, ModEntities.SILENT_LIBRARIAN.get(), 8.5, 1, 8.5);
        helper.runAtTickTime(2, () -> {
            var link = ward.tickLink(helper.getLevel());
            helper.assertTrue(link == ally, "the sentinel links to the librarian, got " + link);
            helper.assertTrue(ally.hasEffect(MobEffects.RESISTANCE), "the linked guardian is shielded");
            helper.succeed();
        });
    }

    private static void redBursts(GameTestHelper helper) {
        Inkling red = mob(helper, ModEntities.INKLING.get(), 5.5, 1, 5.5);
        red.setVariant(Inkling.Variant.RED);
        Pig pig = mob(helper, EntityTypes.PIG, 6.5, 1, 5.5);
        float before = pig.getHealth();
        helper.runAtTickTime(2, () -> {
            for (int i = 0; i < Inkling.SWELL_TICKS - 1; i++) red.swellBy(1);
            helper.assertTrue(red.isAlive() && red.actionProgress(1.0F) > 0.9F, "it swells before it bursts");
            red.swellBy(-5);
            helper.assertTrue(red.swell() == Inkling.SWELL_TICKS - 6, "backing off calms it");
            for (int i = 0; i < 6; i++) red.swellBy(1);
            helper.assertTrue(red.isRemoved(), "a full swell bursts it");
            helper.assertTrue(pig.getHealth() < before && pig.hasEffect(MobEffects.BLINDNESS), "the burst hurts and blinds what is close");
            helper.succeed();
        });
    }

    private static void blackSplits(GameTestHelper helper) {
        Inkling black = mob(helper, ModEntities.INKLING.get(), 5.5, 1, 5.5);
        black.setVariant(Inkling.Variant.BLACK);
        helper.runAtTickTime(2, () -> black.kill(helper.getLevel()));
        helper.succeedWhen(() -> {
            var drops = helper.getLevel().getEntitiesOfClass(Inkling.class, arena(helper), Inkling::isAlive);
            helper.assertTrue(drops.size() == 2 && drops.stream().allMatch(Inkling::isSmall), "a black inkling splits into two small ones, got " + drops.size());
        });
    }

    private static void goldDropsDust(GameTestHelper helper) {
        Inkling gold = mob(helper, ModEntities.INKLING.get(), 5.5, 1, 5.5);
        gold.setVariant(Inkling.Variant.GOLD);
        helper.runAtTickTime(2, () -> gold.kill(helper.getLevel()));
        helper.succeedWhen(() -> helper.assertTrue(!helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, arena(helper),
                e -> e.getItem().is(com.panda.arcane.registry.ModItems.LUMEN_DUST.get())).isEmpty(), "a gilded inkling leaves lumen dust"));
    }

    /** A librarian that sees a survival player within range turns on them without being provoked. */
    private static void noticesIntruder(GameTestHelper helper) {
        SilentLibrarian keeper = helper.spawn(ModEntities.SILENT_LIBRARIAN.get(), new Vec3(2.5, 1, 2.5));
        keeper.setVariant(SilentLibrarian.Variant.SHUSHER);
        var player = MobsBGameTests.survivalPlayer(helper, 2.5, 1, 8.5);
        helper.runAtTickTime(30, () -> {
            try {
                helper.assertTrue(keeper.isAngry(), "the librarian is angry at the intruder");
                helper.assertTrue(keeper.getTarget() == player, "it targets the intruder, got " + keeper.getTarget());
            } finally {
                player.discard();
            }
            helper.succeed();
        });
    }
}
