package com.panda.arcane.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Every Creatures A variant has a texture and a glow texture, and a lang name. */
class CreatureTexturesTest {
    private static final Map<String, Class<? extends Enum<?>>> MOBS = Map.of(
            "lumen_moth", LumenMoth.Variant.class,
            "silent_librarian", SilentLibrarian.Variant.class,
            "ink_golem", InkGolem.Variant.class,
            "vault_ward", VaultWard.Variant.class,
            "inkling", Inkling.Variant.class);
    private static final Path RES = Path.of("src", "main", "resources", "assets", "arcane");

    @Test
    void everyVariantHasTexturesAndAName() throws Exception {
        String lang = Files.readString(RES.resolve("lang/en_us.json"));
        List<String> missing = new ArrayList<>();
        int variants = 0;
        for (var e : MOBS.entrySet()) {
            for (Enum<?> v : e.getValue().getEnumConstants()) {
                variants++;
                String id = v.name().toLowerCase(Locale.ROOT);
                for (String f : new String[] {id + ".png", id + "_glow.png"}) {
                    if (!Files.isRegularFile(RES.resolve("textures/entity/" + e.getKey() + "/" + f))) missing.add(e.getKey() + "/" + f);
                }
                if (!lang.contains("\"entity.arcane." + e.getKey() + "." + id + "\"")) missing.add("lang entity.arcane." + e.getKey() + "." + id);
            }
            int count = e.getValue().getEnumConstants().length;
            assertTrue(count >= 3 && count <= 4, e.getKey() + " should have 3-4 variants, has " + count);
        }
        assertEquals(List.of(), missing);
        assertEquals(19, variants);
    }

    @Test
    void zonesFollowTheLayout() {
        int vaults = 0, restricted = 0;
        for (int cx = -6; cx <= 6; cx++) for (int cz = -6; cz <= 6; cz++) {
            var cell = new com.panda.arcane.worldgen.LibraryLayout(5L).cell(cx, cz);
            CreatureZones z = CreatureZones.of(5L, cell.originX() + 8, cell.originZ() + 8);
            assertEquals(cell.kind(), z.kind());
            assertEquals(cell.restricted(), z.restricted());
            assertTrue(z.central());
            if (z.kind() == com.panda.arcane.worldgen.LibraryLayout.Kind.VAULT) vaults++;
            if (z.restricted()) restricted++;
            CreatureZones street = CreatureZones.of(5L, cell.originX() - 3, cell.originZ() + 4);
            assertEquals(null, street.kind(), "streets are not rooms");
        }
        assertTrue(vaults > 0 && restricted > 0);
    }
}
