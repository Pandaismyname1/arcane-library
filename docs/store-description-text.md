# Arcane Library

**Learn enchantments in an endless living library, then inscribe them at the Arcane Lectern.**

Vanilla enchanting is a slot machine. In **Arcane Library**, **knowledge picks the enchantment, a roll picks its power, and effort improves the roll.** You learn enchantments from tomes found in the Arcane Library, an endless dimension of streets, stacks and vaults kept by things that don't want you there. Then, at the **Arcane Lectern**, you choose what to inscribe and spend Lumen Dust to roll for its level.

## Requirements

Arcane Library runs on **NeoForge** and **Fabric** and plays the same on both. Download the jar for your loader.

- **Minecraft 26.3** and **Java 25**
- **NeoForge:** NeoForge 26.3.0.89 or newer and `arcane-<version>-neoforge.jar`. No other mods needed.
- **Fabric:** Fabric Loader 0.19.5 or newer, **Fabric API 0.161.0+26.3 or newer (required)** and `arcane-<version>-fabric.jar`.
- Install it on the client, and on the server too when you play on one.

Arcane Library is developed **with the help of AI**.

## How it plays

1. **Find a Ruined Librarian Tower** in the Overworld (plains, forests, taiga, snowy plains, savanna, desert, swamps, meadows and cherry groves). Its top-floor chest holds a **Book of Portals**, Lumen Dust, Inscribed Bricks, Ink Vials, a Codex and your first tome. A broken portal frame stands beside it as a hint.
2. **Build a portal frame** of Inscribed Bricks, upright like a Nether portal, from 2 × 3 up to 21 × 21 inside (corners optional), and use the Book of Portals on it.
3. **Explore the Library.** Loot tomes from its chests and brush them out of suspicious bookshelves, and survive its keepers.
4. **Study a tome** (hold use) to learn its enchantment as **Glimpsed**, **Studied** or **Mastered**.
5. **Inscribe** it at the **Arcane Lectern**: pick what you know, roll its power, refine the roll, accept it.
6. **Take spare enchanted gear apart** at the **Deconstruction Table**: learn what it carried and get Lumen Dust back.
7. **Go deeper.** Past the fourth ring of the Library lies the **Restricted Section**, its richest vaults and the **Archivist**.

## Knowledge: tomes and mastery

Every tome teaches one enchantment, vanilla or modded, at one of three tiers. Hold use for 2.5 seconds to study it. Mastery only ever rises, and a tome that would teach you nothing new is not used up.

| Mastery | Refinements per inscription | Roll bonus |
|---|---|---|
| Glimpsed | 1 | +0 |
| Studied | 2 | +8 |
| Mastered | 3 | +16 |

What you know is **yours**: knowledge is per player, kept through death, and the **Codex of the Learned** lists all of it, with search, a filter by tier, which mod each enchantment comes from, and your odds of rolling it at full power.

## The Arcane Lectern

Put in a weapon, tool, armour piece or book, plus Lumen Dust. The lectern lists every enchantment you know that the item can take, including **higher levels of ones it already has**.

- **Inscribe** rolls a power level: `score = d100 + your mastery bonus`, and the level is that share of the enchantment's maximum. Sharpness at 70 rolls Sharpness IV. It costs `2 + max level / 2` dust, 2 more for rare enchantments.
- **Refine** rerolls and **keeps the better result**, as many times as your mastery allows. Each refine costs one dust more than the last.
- **Accept** binds the result to the item. A plain book becomes an enchanted book.
- A **Quill of Insight** in the third slot adds +6 to every roll and one more refinement. It wears out after 48 rolls.

The vanilla enchanting table is turned off by default (it can be turned back on in the config). Anvils, grindstones, enchanted books and villager trades are untouched.

## The Deconstruction Table

Spare enchanted gear has a use. The Deconstruction Table destroys an enchanted item or book and **teaches you everything on it**: unknown enchantments become Glimpsed, and Glimpsed ones become Studied if the item carried them at level III or higher (or at their maximum). It returns **3 Lumen Dust per enchantment level**, 3 to 36. Mastery comes only from Mastered tomes.

## The Arcane Library

An endless dimension under a bedrock roof, built street by street as you explore: corridors of shelves 14 blocks high, lantern-lit plazas, and behind every wall a room.

- **Stacks**, **reading rooms** (each with an Arcane Lectern), **studies**, **scriptoriums**, **atriums**, two-storey **galleries** with balconies, the **card catalogue**, **binderies**, **orreries** under a starry vault, and **vaults** with no door, only a hidden gap.
- **Gate halls** hold a lit return portal and a Deconstruction Table. Every Library portal takes you back to **the portal you came in by**, rebuilt if it is gone, even underground or in the Nether.
- **Suspicious bookshelves**, a dusty shelf with one book out of place, give up a hidden tome, dust or book to a vanilla brush.
- Monsters spawn at any light level, beds don't work, and the clock never moves.

Where you enter matters: you arrive at the gate hall nearest your portal's position. A portal within about 96 blocks of the Overworld's centre opens into the heart of the Library; one built farther out opens straight into the Restricted Section.

### The Restricted Section

From the fourth ring of rooms outwards the Library turns darker: fewer lanterns, cobwebs and cracked floors, many more vaults with Mastered tomes, diamonds and ancient debris, **sealed archives** of chained books, harder variants of every keeper, and the arenas of the Archivist.

## Its keepers

Ten creatures, each with its own model and three or four variants that fight differently. The Restricted Section rolls the harder ones.

| Creature | Variants |
|---|---|
| **Lumen Moth** | harmless light; Common sheds Lumen Dust, Ember lights candles, Pale gives Night Vision, Void brings Darkness |
| **Silent Librarian** | Shusher silences with Slowness and Weakness, the Clerk steals a stack and runs, the Censor blinds and vanishes, the Head Librarian rallies the others |
| **Ink Golem** | Ink leaves slowing pools, Iron-bound winds up a slam that launches you, Blotted bursts into Inklings |
| **Vault Ward** | stationary: Sentinel shields allies, Bulwark reflects projectiles, Seeker fires homing bolts, the Warden of Keys seals a vault's chests until it falls |
| **Inkling** | Black splits in two, Blue slows, Red swells and bursts, Gilded is shy and drops dust |
| **Page Wraith** | circles and dives; Gilded shields its allies, Cursed weakens, Blank copies the damage of your weapon |
| **Tome Mimic** | sleeps as a book until you come close; Pocket bites and runs, Grimoire casts hexes, Ledger swallows your items |
| **Bookwyrm** | burrows through shelves and bursts out of another (it rattles first); Glossy shrugs off hits from the front, Ancient eats books to heal |
| **Quill Stalker** | climbs; Needle fires quills, Inkfang leaps and poisons, Scribe binds you with a rune |
| **Bookend Gargoyle** | a statue until it notices you; Granite moves only when unwatched, Marble pounces, Obsidian deflects arrows |

## The Archivist

The Library's guardian waits in the sanctums of the Restricted Section: 360 health, three phases, and every attack telegraphed and counterable.

- **Page Storm**: aim lines lock before the volley. Sidestep.
- **Erasure**: marked floor bands are wiped by a wave that deletes placed blocks and your longest-lasting buff. Step off.
- **Recall and Page Slam**: a tether drags you to the dais. Break line of sight behind a pillar to snap it and stagger the Archivist, or run out of the ring.
- **The Index**: summoned Vault Wards shield it until they're destroyed.

It drops Mastered tomes, exclusive tomes, a pile of Lumen Dust, the **Archivist's Seal** and the **Archivist's Bookmark**: sneak-use it to mark a spot, hold use to return there (60-second cooldown, same dimension).

## Six new enchantments

Found only in the Library's vaults and from the Archivist.

| Enchantment | On | Effect |
|---|---|---|
| **Lumen Step** I–III | boots | +7/14/21% speed and a higher step |
| **Page Turn** I–III | melee weapons | +8/16/24% attack speed |
| **Knowledge Leech** I–II | melee weapons | ×1.5 / ×2 experience from kills |
| **Ink Bound** I–II | anything with durability | slowly repairs itself anywhere in your inventory |
| **Arcane Ward** I–III | armour | protection against projectiles, magic and wither |
| **Lumen Edge** I–II | melee weapons | makes what you hit glow |

## Crafting

| Item | Recipe |
|---|---|
| Lumen Dust ×3 | 2 amethyst shards and 2 glowstone dust |
| Inscribed Bricks ×4 | 4 stone bricks and a Lumen Dust |
| Ink Vial | a glass bottle, an ink sac and lapis lazuli |
| Book of Portals | seven Inscribed Bricks, an Ink Vial, two books and a Lumen Dust |
| Arcane Lectern | a lectern on three Inscribed Bricks, between two Lumen Dust |
| Deconstruction Table | a grindstone on three Inscribed Bricks, between two Lumen Dust |
| Quill of Insight | Lumen Dust, a feather and an Ink Vial, in a column |
| Codex of the Learned | a book, an Ink Vial and Lumen Dust |
| Knowledge Shelf, Lumen Lantern, Chiseled Inscribed Bricks | for building |

## Other mods' enchantments

Everything reads the enchantment registry, so **enchantments from other mods** can be learned, inscribed, deconstructed and browsed in the Codex, and tomes for them drop from Library loot automatically. Modpacks can move an enchantment between loot pools with the tags `arcane:tome_pool/common`, `uncommon`, `rare` and `exclusive`, or keep it out with `arcane:tome_pool/excluded`. Curses never drop.

## Configuration and commands

Three settings: turn the vanilla enchanting table back on (`disableVanillaEnchantingTable`), how often Ink Bound repairs (`inkBoundIntervalTicks`), and how long you stand in a portal (`portalDelayTicks`). On NeoForge they are in `config/arcane-synced.toml`, synced from the server; on Fabric in `config/arcane.properties`.

Operators can manage knowledge with `/arcane knowledge learn | forget | learnall | reset | list`.
