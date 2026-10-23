package com.panda.arcane.registry;

import com.panda.arcane.ArcaneMod;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import com.panda.arcane.platform.registry.DeferredRegister;

/** Sound events. assets/arcane/sounds.json layers vanilla events onto them. */
public final class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ArcaneMod.MODID);

    public static final Supplier<SoundEvent> INSCRIBE_ROLL = sound("lectern.roll");
    public static final Supplier<SoundEvent> INSCRIBE_MAX = sound("lectern.max");
    public static final Supplier<SoundEvent> STUDY_COMPLETE = sound("tome.study");
    public static final Supplier<SoundEvent> PORTAL_AMBIENT = sound("portal.ambient");
    public static final Supplier<SoundEvent> PORTAL_TRAVEL = sound("portal.travel");
    public static final Supplier<SoundEvent> MOTH_FLUTTER = sound("moth.flutter");
    public static final Supplier<SoundEvent> LIBRARIAN_HUSH = sound("librarian.hush");
    public static final Supplier<SoundEvent> GOLEM_STEP = sound("ink_golem.step");
    public static final Supplier<SoundEvent> WARD_ZAP = sound("vault_ward.zap");
    public static final Supplier<SoundEvent> ARCHIVIST_AMBIENT = sound("archivist.ambient");
    public static final Supplier<SoundEvent> ARCHIVIST_SLAM = sound("archivist.slam");
    public static final Supplier<SoundEvent> ARCHIVIST_DEATH = sound("archivist.death");

    // ---- Boss: the Archivist's telegraphs and fight music ----
    public static final com.panda.arcane.platform.registry.DeferredHolder<SoundEvent, SoundEvent> ARCHIVIST_STORM_CHARGE = boss("archivist.storm_charge");
    public static final com.panda.arcane.platform.registry.DeferredHolder<SoundEvent, SoundEvent> ARCHIVIST_STORM_LOCK = boss("archivist.storm_lock");
    public static final com.panda.arcane.platform.registry.DeferredHolder<SoundEvent, SoundEvent> ARCHIVIST_STORM_RELEASE = boss("archivist.storm_release");
    public static final com.panda.arcane.platform.registry.DeferredHolder<SoundEvent, SoundEvent> ARCHIVIST_ERASE_CHARGE = boss("archivist.erase_charge");
    public static final com.panda.arcane.platform.registry.DeferredHolder<SoundEvent, SoundEvent> ARCHIVIST_ERASE = boss("archivist.erase");
    public static final com.panda.arcane.platform.registry.DeferredHolder<SoundEvent, SoundEvent> ARCHIVIST_RECALL_CHARGE = boss("archivist.recall_charge");
    public static final com.panda.arcane.platform.registry.DeferredHolder<SoundEvent, SoundEvent> ARCHIVIST_RECALL_PULL = boss("archivist.recall_pull");
    public static final com.panda.arcane.platform.registry.DeferredHolder<SoundEvent, SoundEvent> ARCHIVIST_TETHER_SNAP = boss("archivist.tether_snap");
    public static final com.panda.arcane.platform.registry.DeferredHolder<SoundEvent, SoundEvent> ARCHIVIST_SLAM_CHARGE = boss("archivist.slam_charge");
    public static final com.panda.arcane.platform.registry.DeferredHolder<SoundEvent, SoundEvent> ARCHIVIST_INDEX = boss("archivist.index");
    public static final com.panda.arcane.platform.registry.DeferredHolder<SoundEvent, SoundEvent> ARCHIVIST_PHASE = boss("archivist.phase");
    public static final com.panda.arcane.platform.registry.DeferredHolder<SoundEvent, SoundEvent> ARCHIVIST_MUSIC = boss("archivist.music");
    public static final com.panda.arcane.platform.registry.DeferredHolder<SoundEvent, SoundEvent> BOOKMARK_USE = boss("bookmark.use");

    private static com.panda.arcane.platform.registry.DeferredHolder<SoundEvent, SoundEvent> boss(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ArcaneMod.id(name)));
    }
    // ---- end Boss ----

    private ModSounds() {}

    private static Supplier<SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ArcaneMod.id(name)));
    }

}
