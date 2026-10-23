package com.panda.arcane.registry;

import com.panda.arcane.ArcaneMod;
import com.panda.arcane.block.ArcaneLecternBlock;
import com.panda.arcane.block.DeconstructionTableBlock;
import com.panda.arcane.block.LibraryPortalBlock;
import com.panda.arcane.block.SuspiciousBookshelfBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import com.panda.arcane.platform.registry.DeferredBlock;
import com.panda.arcane.platform.registry.DeferredItem;
import com.panda.arcane.platform.registry.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ArcaneMod.MODID);

    public static final DeferredBlock<ArcaneLecternBlock> ARCANE_LECTERN = BLOCKS.registerBlock("arcane_lectern",
            ArcaneLecternBlock::new, p -> p.mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion().lightLevel(s -> 7));
    public static final DeferredBlock<DeconstructionTableBlock> DECONSTRUCTION_TABLE = BLOCKS.registerBlock("deconstruction_table",
            DeconstructionTableBlock::new, p -> p.mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<Block> INSCRIBED_BRICKS = BLOCKS.registerSimpleBlock("inscribed_bricks",
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(2.0F, 6.0F).sound(SoundType.DEEPSLATE_BRICKS).requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> CHISELED_INSCRIBED_BRICKS = BLOCKS.registerSimpleBlock("chiseled_inscribed_bricks",
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(2.0F, 6.0F).sound(SoundType.DEEPSLATE_BRICKS).requiresCorrectToolForDrops());
    public static final DeferredBlock<com.panda.arcane.block.LumenLanternBlock> LUMEN_LANTERN = BLOCKS.registerBlock("lumen_lantern",
            com.panda.arcane.block.LumenLanternBlock::new,
            p -> p.mapColor(MapColor.GOLD).strength(0.5F).sound(SoundType.GLASS).lightLevel(s -> 15));
    public static final DeferredBlock<Block> KNOWLEDGE_SHELF = BLOCKS.registerSimpleBlock("knowledge_shelf",
            p -> p.mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.WOOD));
    /** The Library's ordinary shelves: a vanilla bookshelf whose model picks one of several book arrangements at random. */
    public static final DeferredBlock<Block> LIBRARY_BOOKSHELF = BLOCKS.registerSimpleBlock("library_bookshelf",
            p -> p.mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.WOOD).ignitedByLava());

    public static final DeferredBlock<SuspiciousBookshelfBlock> SUSPICIOUS_BOOKSHELF = BLOCKS.registerBlock("suspicious_bookshelf",
            SuspiciousBookshelfBlock::new, p -> p.mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.CHISELED_BOOKSHELF).noLootTable());
    public static final DeferredBlock<LibraryPortalBlock> LIBRARY_PORTAL = BLOCKS.registerBlock("library_portal",
            LibraryPortalBlock::new, p -> p.mapColor(MapColor.COLOR_PURPLE).noCollision().strength(-1.0F).sound(SoundType.GLASS)
                    .lightLevel(s -> 11).noLootTable().pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE));

    public static final DeferredItem<BlockItem> ARCANE_LECTERN_ITEM = ModBlocks.item(ARCANE_LECTERN);
    public static final DeferredItem<BlockItem> DECONSTRUCTION_TABLE_ITEM = ModBlocks.item(DECONSTRUCTION_TABLE);
    public static final DeferredItem<BlockItem> INSCRIBED_BRICKS_ITEM = ModBlocks.item(INSCRIBED_BRICKS);
    public static final DeferredItem<BlockItem> CHISELED_INSCRIBED_BRICKS_ITEM = ModBlocks.item(CHISELED_INSCRIBED_BRICKS);
    public static final DeferredItem<BlockItem> LUMEN_LANTERN_ITEM = ModBlocks.item(LUMEN_LANTERN);
    public static final DeferredItem<BlockItem> KNOWLEDGE_SHELF_ITEM = ModBlocks.item(KNOWLEDGE_SHELF);
    public static final DeferredItem<BlockItem> LIBRARY_BOOKSHELF_ITEM = ModBlocks.item(LIBRARY_BOOKSHELF);
    public static final DeferredItem<BlockItem> SUSPICIOUS_BOOKSHELF_ITEM = ModBlocks.item(SUSPICIOUS_BOOKSHELF);

    private ModBlocks() {}

    private static DeferredItem<BlockItem> item(DeferredBlock<? extends Block> block) {
        return ModItems.ITEMS.registerSimpleBlockItem(block);
    }

}
