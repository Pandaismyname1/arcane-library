package com.panda.arcane.worldgen;

/** Material tokens for the Library layout. Pure data: the generator maps each to a block state spec. */
public enum Mat {
    AIR("minecraft:air"),
    BEDROCK("minecraft:bedrock"),
    BASE("minecraft:deepslate"),
    FLOOR_A("minecraft:dark_oak_planks"),
    FLOOR_B("minecraft:spruce_planks"),
    FLOOR_RUIN_A("minecraft:deepslate_tiles"),
    FLOOR_RUIN_B("minecraft:cracked_deepslate_tiles"),
    CARPET("minecraft:purple_carpet"),
    CARPET_DARK("minecraft:black_carpet"),
    SHELF("arcane:library_bookshelf"),
    SHELF_KNOW("arcane:knowledge_shelf"),
    SHELF_SUS("arcane:suspicious_bookshelf"),
    SHELF_CHISEL("minecraft:chiseled_bookshelf[facing=north]"),
    PILLAR("minecraft:stripped_dark_oak_log[axis=y]"),
    PILLAR_RUIN("minecraft:polished_deepslate"),
    BEAM_X("minecraft:dark_oak_log[axis=x]"),
    BEAM_Z("minecraft:dark_oak_log[axis=z]"),
    CEILING("minecraft:dark_oak_planks"),
    CEILING_RUIN("minecraft:cracked_deepslate_bricks"),
    TRIM("arcane:inscribed_bricks"),
    CHISEL("arcane:chiseled_inscribed_bricks"),
    CHAIN("minecraft:iron_chain[axis=y]"),
    LANTERN("arcane:lumen_lantern"),
    TABLE_LEG("minecraft:dark_oak_fence"),
    TABLE_TOP("minecraft:dark_oak_slab[type=top]"),
    LECTERN_N("minecraft:lectern[facing=north]"),
    LECTERN_E("minecraft:lectern[facing=east]"),
    LECTERN_S("minecraft:lectern[facing=south]"),
    LECTERN_W("minecraft:lectern[facing=west]"),
    ARCANE_LECTERN("arcane:arcane_lectern[facing=south]"),
    BARS("minecraft:iron_bars"),
    COBWEB("minecraft:cobweb"),
    MOSS("minecraft:moss_block"),
    MOSS_CARPET("minecraft:moss_carpet"),
    LEAVES("minecraft:flowering_azalea_leaves[persistent=true]"),
    LEAVES_PLAIN("minecraft:azalea_leaves[persistent=true]"),
    LOG("minecraft:spruce_log[axis=y]"),
    BARREL("minecraft:barrel[facing=up]"),
    CANDLE("minecraft:purple_candle[candles=3,lit=true]"),
    LOOM("minecraft:loom[facing=south]"),
    DECON_TABLE("arcane:deconstruction_table"),
    PORTAL_X("arcane:library_portal[axis=x]"),
    PORTAL_Z("arcane:library_portal[axis=z]"),
    // gallery
    LADDER_N("minecraft:ladder[facing=north]"),
    RAIL_X("minecraft:dark_oak_fence[east=true,west=true]"),
    RAIL_Z("minecraft:dark_oak_fence[north=true,south=true]"),
    LADDER_E("minecraft:ladder[facing=east]"),
    LADDER_S("minecraft:ladder[facing=south]"),
    LADDER_W("minecraft:ladder[facing=west]"),
    // card catalogue: barrels on their side read as drawer fronts
    DRAWER_N("minecraft:barrel[facing=north]"),
    DRAWER_E("minecraft:barrel[facing=east]"),
    DRAWER_S("minecraft:barrel[facing=south]"),
    DRAWER_W("minecraft:barrel[facing=west]"),
    CANDLE_WHITE("minecraft:white_candle[candles=2,lit=true]"),
    // bindery
    PRESS("minecraft:anvil[facing=north]"),
    WORKBENCH("minecraft:crafting_table"),
    CARTOGRAPHY("minecraft:cartography_table"),
    VAT("minecraft:water_cauldron[level=3]"),
    PAPER("minecraft:white_carpet"),
    CHAIN_X("minecraft:iron_chain[axis=x]"),
    CHAIN_Z("minecraft:iron_chain[axis=z]"),
    // orrery
    NIGHT("minecraft:black_concrete"),
    STAR("minecraft:sea_lantern"),
    SUN("minecraft:glowstone"),
    RING("minecraft:waxed_cut_copper"),
    RING_GOLD("minecraft:gold_block"),
    PLANET_A("minecraft:amethyst_block"),
    PLANET_B("minecraft:lapis_block"),
    CRYSTAL("minecraft:amethyst_cluster[facing=up]"),
    // sealed archive
    SOUL_LANTERN("minecraft:soul_lantern"),
    CRYING("minecraft:crying_obsidian"),
    CANDLE_BLACK("minecraft:black_candle[candles=3,lit=true]");

    private final String spec;

    Mat(String spec) {
        this.spec = spec;
    }

    public String spec() {
        return spec;
    }

    /** True when entities can walk through the material. */
    public boolean passable() {
        return switch (this) {
            case AIR, CARPET, CARPET_DARK, COBWEB, MOSS_CARPET, CANDLE, CANDLE_WHITE, CANDLE_BLACK, PAPER, LADDER_N, LADDER_E, LADDER_S, LADDER_W -> true;
            default -> false;
        };
    }
}
