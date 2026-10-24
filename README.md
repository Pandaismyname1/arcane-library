# Arcane Library

[![Build](https://github.com/Pandaismyname1/arcane-library/actions/workflows/build.yml/badge.svg)](https://github.com/Pandaismyname1/arcane-library/actions/workflows/build.yml)

**Website: https://pandaismyname1.github.io/arcane-library/** (description, showcase, bestiary, recipes, guides)

A Minecraft 26.3 mod for NeoForge (26.3.0.89) and Fabric (Fabric API 0.161.0), Java 25.

Vanilla enchanting is a slot machine. Here **knowledge picks the enchantment, a roll picks its power, and effort
improves the roll.** You earn the *what* by exploring the Arcane Library, an endless dimension of living stacks, and
you earn the *how strong* at the Arcane Lectern by spending Lumen Dust.

## How it plays

1. **Find a Ruined Librarian Tower** in the Overworld. Its top-floor chest holds a Book of Portals, Lumen Dust,
   inscribed bricks, ink vials, a Codex and a first (Glimpsed) tome. A broken portal frame stands beside it as a hint.
2. **Build a frame** of Inscribed Bricks or Chiseled Inscribed Bricks (2 wide, 3 tall inside, any size up to 21x21
   inside, corners optional) and use the Book of Portals on it (16 uses). You arrive at the Library gate hall nearest
   your x/z: within about 96 blocks of Overworld 0,0 that is the central one, farther out it is in the Restricted Section.
3. **Explore the Library.** Streets, stacks, reading rooms, studies, scriptoriums, atriums, vaults, two-storey galleries,
   the card catalogue, binderies, orreries under a starry vault, and (Restricted Section only) sealed archives of
   chained books. Loot **tomes**, fight the keepers: gargoyles and librarians attack on sight.
3b. **Brush suspicious bookshelves** (they look like dusty bookshelves with one book out of place) with a vanilla brush to uncover a hidden item: often a tome, dust, or a book. They occur in the Library walls and stacks and in the tower.
4. **Study** a tome (hold use). It teaches one enchantment at a mastery tier: Glimpsed, Studied or Mastered. The tome
   is used up, unless it would teach you nothing new.
5. At an **Arcane Lectern** place gear and Lumen Dust, pick a known, compatible enchantment and **Inscribe**.
   The roll shows a power level. **Refine** to reroll and keep the better result (limited by mastery; a Quill of
   Insight adds one more refinement and +6 to every roll, and wears out after 48 rolls). **Accept** to bind it to the item.
6. Spare enchanted gear goes to the **Deconstruction Table**: it is destroyed, teaches what it carries (unknown
   becomes Glimpsed; Glimpsed becomes Studied if the item had it at level III or its maximum), returns dust.
   Mastered comes only from Mastered tomes.
7. Past the 4th ring of cells lies the **Restricted Section**: darker, richer vaults, and the **Archivist** boss.

The vanilla enchanting table is disabled by default (config `disableVanillaEnchantingTable`).

### Other mods' enchantments

Everything reads the enchantment registry, so enchantments added by other mods can be learned, inscribed, deconstructed
and browsed in the Codex. Tomes for them drop from loot automatically: an enchantment in `#minecraft:treasure` goes to
the rare pool, one with weight 5 or more to the common pool, the rest to the uncommon pool; curses never drop. Modpacks
can place an enchantment explicitly with the tags `arcane:tome_pool/common`, `uncommon`, `rare`, `exclusive`, or keep it
out with `arcane:tome_pool/excluded`. Codex descriptions use the `enchantment.<mod>.<name>.desc` key many mods already ship.

### Numbers

| Mastery | Refinements | Roll bonus |
|---|---|---|
| Glimpsed | 1 | +0 |
| Studied | 2 | +8 |
| Mastered | 3 | +16 |

`score = clamp(d100 + bonus, 1, 100)`, `power = ceil(score * maxLevel / 100)`. Costs in Lumen Dust: inscribe
`2 + maxLevel/2` (+2 for rare enchantments), refine `2 + refinesUsed`. Deconstruction returns 3 dust per
enchantment level (3 to 36).

## Content

* Blocks: Arcane Lectern, Deconstruction Table, Inscribed Bricks (+ Chiseled), Lumen Lantern, Knowledge Shelf (decorative), Library Bookshelf, Suspicious Bookshelf (brushable), Library Portal.
* Items: Book of Portals, Ink Vial, Lumen Dust, Quill of Insight, Codex of the Learned, Archivist's Seal, Archivist's Bookmark (mark a spot, recall to it), Tomes (3 tiers, one per enchantment), spawn eggs.
* Enchantments (data-driven): Lumen Step, Page Turn, Knowledge Leech, Ink Bound, Arcane Ward, Lumen Edge.
* Mobs (ten, each with custom animated models and 3-4 variants that each have their own mechanic; the Restricted
  Section rolls the harder variants): Lumen Moth, Silent Librarian, Ink Golem, Vault Ward, Inkling, Page Wraith,
  Tome Mimic, Bookwyrm, Quill Stalker, Bookend Gargoyle.
* Boss: **The Archivist**, three phases with telegraphed, counterable moves: Page Storm (aim lines lock before the
  volley, sidestep), Erasure (marked floor bands wiped by a wave, step off), Recall and Page Slam (a tether pulls you
  to the dais, break line of sight behind a pillar to snap it and stagger the boss, or run out of the flame ring), and
  the Index (summoned wards shield the boss until killed). Drops the Archivist's Seal and the Archivist's Bookmark.
* Dimension `arcane:library` with a custom chunk generator, chests with tiered loot, gate halls with return portals
  (a fixed grid every 8 cells plus about 1 in 20 other cells). Every Library portal takes you back to the portal you
  entered by, rebuilt in place if it is gone (underground and in the Nether too); with none on record, to your
  respawn point, else world spawn.
* Structure: Ruined Librarian Tower. Advancements: an 11-step tab.

## Building and running

JDK 25 is required. This project was built with Temurin 25 at `C:/dev/jdk25-dl/jdk-25.0.4.1+1` (see `gradle.properties`,
`org.gradle.java.home`). Change that path for your machine.

```bash
./gradlew build
```

```bash
./gradlew test
```

```bash
./gradlew :neoforge:runGameTestServer
```

```bash
./gradlew :neoforge:runClient
```

```bash
./gradlew :fabric:runClient
```

The project is an Architectury multi-loader build: `common` holds the mod (content, logic, client code) with no loader
code, `neoforge` and `fabric` hold each loader's entry points, platform service (`ArcanePlatform`) and event wiring.
`./gradlew build` makes `neoforge/build/libs/arcane-<version>-neoforge.jar` and
`fabric/build/libs/arcane-<version>-fabric.jar` (needs Fabric API). Access wideners live in
`common/src/main/resources/arcane.accesswidener`; NeoForge's access transformer is generated from it.

## Releasing

Push a tag such as `v0.2.0` (`-alpha`/`-beta`/`-rc` in the name marks a pre-release). The Release workflow builds,
runs the tests and GameTests, then publishes the NeoForge and Fabric jars to CurseForge and GitHub Releases. It needs the repository secret
`PUBLISH_CURSEFORGE_TOKEN` and the repository variable `CURSEFORGE_PROJECT_ID`. Bugs go to the issue forms; questions,
ideas and finds to Discussions.

## Website

`site/` is the GitHub Pages site, deployed by `.github/workflows/pages.yml` (set Settings > Pages > Source to GitHub Actions once).

## Store pages

The CurseForge text is `docs/store-description.md` (`docs/store-description-text.md` is the same without images,
ready to paste; on CurseForge the cards go stacked at the top). The icon, cards and gallery images are in
`docs/showcase/`.
