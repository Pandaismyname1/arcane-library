package com.panda.arcane.registry;

import com.panda.arcane.ArcaneMod;
import com.panda.arcane.item.BookOfPortalsItem;
import com.panda.arcane.item.CodexItem;
import com.panda.arcane.item.TomeItem;
import com.panda.arcane.logic.Mastery;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import com.panda.arcane.platform.registry.DeferredItem;
import com.panda.arcane.platform.registry.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ArcaneMod.MODID);

    public static final DeferredItem<Item> LUMEN_DUST = ITEMS.registerSimpleItem("lumen_dust");
    public static final DeferredItem<Item> INK_VIAL = ITEMS.registerSimpleItem("ink_vial", p -> p.stacksTo(16));
    public static final DeferredItem<Item> QUILL_OF_INSIGHT = ITEMS.registerSimpleItem("quill_of_insight",
            p -> p.durability(48).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> ARCHIVISTS_SEAL = ITEMS.registerSimpleItem("archivists_seal",
            p -> p.rarity(Rarity.EPIC).stacksTo(1));
    public static final DeferredItem<BookOfPortalsItem> BOOK_OF_PORTALS = ITEMS.registerItem("book_of_portals",
            BookOfPortalsItem::new, p -> p.stacksTo(1).durability(16).rarity(Rarity.RARE));
    public static final DeferredItem<CodexItem> CODEX = ITEMS.registerItem("codex", CodexItem::new,
            p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));

    public static final DeferredItem<TomeItem> TOME_GLIMPSED = ITEMS.registerItem("tome_glimpsed",
            p -> new TomeItem(p, Mastery.GLIMPSED), p -> p.stacksTo(16));
    public static final DeferredItem<TomeItem> TOME_STUDIED = ITEMS.registerItem("tome_studied",
            p -> new TomeItem(p, Mastery.STUDIED), p -> p.stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<TomeItem> TOME_MASTERED = ITEMS.registerItem("tome_mastered",
            p -> new TomeItem(p, Mastery.MASTERED), p -> p.stacksTo(16).rarity(Rarity.RARE));

    public static final DeferredItem<Item> LUMEN_MOTH_SPAWN_EGG = ITEMS.registerItem("lumen_moth_spawn_egg", Item::new,
            p -> p.spawnEgg(ModEntities.LUMEN_MOTH.get()));
    public static final DeferredItem<Item> SILENT_LIBRARIAN_SPAWN_EGG = ITEMS.registerItem("silent_librarian_spawn_egg", Item::new,
            p -> p.spawnEgg(ModEntities.SILENT_LIBRARIAN.get()));
    public static final DeferredItem<Item> INK_GOLEM_SPAWN_EGG = ITEMS.registerItem("ink_golem_spawn_egg", Item::new,
            p -> p.spawnEgg(ModEntities.INK_GOLEM.get()));
    public static final DeferredItem<Item> VAULT_WARD_SPAWN_EGG = ITEMS.registerItem("vault_ward_spawn_egg", Item::new,
            p -> p.spawnEgg(ModEntities.VAULT_WARD.get()));
    public static final DeferredItem<Item> ARCHIVIST_SPAWN_EGG = ITEMS.registerItem("archivist_spawn_egg", Item::new,
            p -> p.spawnEgg(ModEntities.ARCHIVIST.get()).rarity(Rarity.EPIC));
    // ---- Creatures A ----
    public static final DeferredItem<Item> INKLING_SPAWN_EGG = ITEMS.registerItem("inkling_spawn_egg", Item::new,
            p -> p.spawnEgg(ModEntities.INKLING.get()));
    // ---- end Creatures A ----

    // ---- Boss: the Archivist's reward ----
    public static final DeferredItem<com.panda.arcane.item.ArchivistsBookmarkItem> ARCHIVISTS_BOOKMARK = ITEMS.registerItem("archivists_bookmark",
            com.panda.arcane.item.ArchivistsBookmarkItem::new, p -> p.stacksTo(1).rarity(Rarity.EPIC));
    // ---- end Boss ----

    // ---- Creatures B spawn eggs ----------------------------------------------------------------------------------
    public static final DeferredItem<Item> PAGE_WRAITH_SPAWN_EGG = ITEMS.registerItem("page_wraith_spawn_egg", Item::new,
            p -> p.spawnEgg(ModEntities.PAGE_WRAITH.get()));
    public static final DeferredItem<Item> TOME_MIMIC_SPAWN_EGG = ITEMS.registerItem("tome_mimic_spawn_egg", Item::new,
            p -> p.spawnEgg(ModEntities.TOME_MIMIC.get()));
    public static final DeferredItem<Item> BOOKWYRM_SPAWN_EGG = ITEMS.registerItem("bookwyrm_spawn_egg", Item::new,
            p -> p.spawnEgg(ModEntities.BOOKWYRM.get()));
    public static final DeferredItem<Item> QUILL_STALKER_SPAWN_EGG = ITEMS.registerItem("quill_stalker_spawn_egg", Item::new,
            p -> p.spawnEgg(ModEntities.QUILL_STALKER.get()));
    public static final DeferredItem<Item> BOOKEND_GARGOYLE_SPAWN_EGG = ITEMS.registerItem("bookend_gargoyle_spawn_egg", Item::new,
            p -> p.spawnEgg(ModEntities.BOOKEND_GARGOYLE.get()));
    // ---- end Creatures B -----------------------------------------------------------------------------------------

    private ModItems() {}

    public static DeferredItem<TomeItem> tome(Mastery mastery) {
        return switch (mastery) {
            case STUDIED -> TOME_STUDIED;
            case MASTERED -> TOME_MASTERED;
            default -> TOME_GLIMPSED;
        };
    }

}
