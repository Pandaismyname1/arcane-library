package com.panda.arcane.entity;

import com.panda.arcane.ArcaneMod;
import com.panda.arcane.portal.PortalTravel;
import com.panda.arcane.worldgen.LibraryLayout;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jspecify.annotations.Nullable;

/**
 * Creatures B (Page Wraith, Tome Mimic, Bookwyrm, Quill Stalker, Bookend Gargoyle): shared sound events, the spawn
 * zone lookup that picks variants by location, and small helpers. Sound events are layered from vanilla ones in
 * assets/arcane/sounds.json.
 */
public final class MobsB {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ArcaneMod.MODID);

    public static final Supplier<SoundEvent> WRAITH_FLUTTER = sound("page_wraith.flutter");
    public static final Supplier<SoundEvent> WRAITH_WHISPER = sound("page_wraith.whisper");
    public static final Supplier<SoundEvent> WRAITH_TELL = sound("page_wraith.tell");
    public static final Supplier<SoundEvent> WRAITH_DIVE = sound("page_wraith.dive");
    public static final Supplier<SoundEvent> WRAITH_HURT = sound("page_wraith.hurt");
    public static final Supplier<SoundEvent> WRAITH_DEATH = sound("page_wraith.death");
    public static final Supplier<SoundEvent> WRAITH_WARD = sound("page_wraith.ward");
    public static final Supplier<SoundEvent> WRAITH_COPY = sound("page_wraith.copy");

    public static final Supplier<SoundEvent> MIMIC_WAKE = sound("tome_mimic.wake");
    public static final Supplier<SoundEvent> MIMIC_SETTLE = sound("tome_mimic.settle");
    public static final Supplier<SoundEvent> MIMIC_BITE = sound("tome_mimic.bite");
    public static final Supplier<SoundEvent> MIMIC_CHATTER = sound("tome_mimic.chatter");
    public static final Supplier<SoundEvent> MIMIC_CAST = sound("tome_mimic.cast");
    public static final Supplier<SoundEvent> MIMIC_GULP = sound("tome_mimic.gulp");
    public static final Supplier<SoundEvent> MIMIC_HURT = sound("tome_mimic.hurt");
    public static final Supplier<SoundEvent> MIMIC_DEATH = sound("tome_mimic.death");

    public static final Supplier<SoundEvent> WYRM_AMBIENT = sound("bookwyrm.ambient");
    public static final Supplier<SoundEvent> WYRM_BURROW = sound("bookwyrm.burrow");
    public static final Supplier<SoundEvent> WYRM_EMERGE = sound("bookwyrm.emerge");
    public static final Supplier<SoundEvent> WYRM_CHEW = sound("bookwyrm.chew");
    public static final Supplier<SoundEvent> WYRM_CLANK = sound("bookwyrm.clank");
    public static final Supplier<SoundEvent> WYRM_HURT = sound("bookwyrm.hurt");
    public static final Supplier<SoundEvent> WYRM_DEATH = sound("bookwyrm.death");

    public static final Supplier<SoundEvent> STALKER_AMBIENT = sound("quill_stalker.ambient");
    public static final Supplier<SoundEvent> STALKER_SHOOT = sound("quill_stalker.shoot");
    public static final Supplier<SoundEvent> STALKER_SCRIBBLE = sound("quill_stalker.scribble");
    public static final Supplier<SoundEvent> STALKER_BITE = sound("quill_stalker.bite");
    public static final Supplier<SoundEvent> STALKER_HURT = sound("quill_stalker.hurt");
    public static final Supplier<SoundEvent> STALKER_DEATH = sound("quill_stalker.death");

    public static final Supplier<SoundEvent> GARGOYLE_FREEZE = sound("bookend_gargoyle.freeze");
    public static final Supplier<SoundEvent> GARGOYLE_UNFREEZE = sound("bookend_gargoyle.unfreeze");
    public static final Supplier<SoundEvent> GARGOYLE_GRIND = sound("bookend_gargoyle.grind");
    public static final Supplier<SoundEvent> GARGOYLE_POUNCE = sound("bookend_gargoyle.pounce");
    public static final Supplier<SoundEvent> GARGOYLE_LAND = sound("bookend_gargoyle.land");
    public static final Supplier<SoundEvent> GARGOYLE_DEFLECT = sound("bookend_gargoyle.deflect");
    public static final Supplier<SoundEvent> GARGOYLE_HURT = sound("bookend_gargoyle.hurt");
    public static final Supplier<SoundEvent> GARGOYLE_DEATH = sound("bookend_gargoyle.death");

    private MobsB() {}

    private static Supplier<SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ArcaneMod.id(name)));
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
        com.panda.arcane.gametest.MobsBGameTests.register(modBus);
    }

    /** Where in the Library a creature stands: the cell kind and whether it is in the Restricted Section. */
    public record Zone(boolean library, boolean restricted, LibraryLayout.@Nullable Kind kind) {
        public static final Zone OUTSIDE = new Zone(false, false, null);

        public boolean is(LibraryLayout.Kind k) {
            return kind == k;
        }
    }

    public static Zone zone(ServerLevelAccessor level, BlockPos pos) {
        ServerLevel server = level.getLevel();
        if (!server.dimension().equals(PortalTravel.LIBRARY)) return Zone.OUTSIDE;
        LibraryLayout layout = new LibraryLayout(server.getSeed());
        LibraryLayout.Cell cell = layout.cell(Math.floorDiv(pos.getX(), LibraryLayout.CELL), Math.floorDiv(pos.getZ(), LibraryLayout.CELL));
        boolean street = Math.floorMod(pos.getX(), LibraryLayout.CELL) < LibraryLayout.STREET
                || Math.floorMod(pos.getZ(), LibraryLayout.CELL) < LibraryLayout.STREET;
        return new Zone(true, cell.restricted(), street ? null : cell.kind());
    }

    /** Index into {@code weights}, chosen with probability proportional to each weight. */
    public static int weighted(RandomSource random, int... weights) {
        int total = 0;
        for (int w : weights) total += Math.max(0, w);
        if (total <= 0) return 0;
        int roll = random.nextInt(total);
        for (int i = 0; i < weights.length; i++) {
            roll -= Math.max(0, weights[i]);
            if (roll < 0) return i;
        }
        return weights.length - 1;
    }

    /** Two sounds at once from the entity, so one cue reads as a layered sound. */
    public static void layered(Entity e, SoundEvent a, float volA, float pitchA, SoundEvent b, float volB, float pitchB) {
        e.level().playSound(null, e.getX(), e.getY(), e.getZ(), a, e.getSoundSource(), volA, pitchA);
        e.level().playSound(null, e.getX(), e.getY(), e.getZ(), b, e.getSoundSource(), volB, pitchB);
    }

    public static void play(Entity e, SoundEvent s, float vol, float pitch) {
        e.level().playSound(null, e.getX(), e.getY(), e.getZ(), s, e.getSoundSource(), vol, pitch);
    }
}
