package com.alvorecer;

import com.alvorecer.dungeon.Dungeons;
import com.alvorecer.world.DungeonStructure;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

class Stage6Test {

    private static JsonObject json(String path) throws Exception {
        try (var in = Stage6Test.class.getResourceAsStream(path)) {
            assertNotNull(in, path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static CompoundTag template(String name) throws Exception {
        try (var in = Stage6Test.class.getResourceAsStream("/data/alvorecer/structure/" + name + ".nbt")) {
            assertNotNull(in, name);
            return NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
        }
    }

    /** Posições (x, y, z) dos blocos com esse nome no molde. */
    private static List<int[]> blocks(CompoundTag t, String name) {
        ListTag palette = t.getList("palette", Tag.TAG_COMPOUND);
        List<int[]> out = new ArrayList<>();
        for (Tag b : t.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) b;
            if (palette.getCompound(c.getInt("state")).getString("Name").equals(name)) {
                ListTag pos = c.getList("pos", Tag.TAG_INT);
                out.add(new int[]{pos.getInt(0), pos.getInt(1), pos.getInt(2)});
            }
        }
        return out;
    }

    @Test
    void layoutIsAChainOfDistinctNeighbours() {
        for (long seed = 0; seed < 300; seed++) {
            List<int[]> cells = Dungeons.layout(RandomSource.create(seed), DungeonStructure.ORDER.size());
            assertNotNull(cells, "semente " + seed);
            assertEquals(7, cells.size());
            Set<Long> seen = new HashSet<>();
            for (int i = 0; i < cells.size(); i++) {
                int[] c = cells.get(i);
                assertTrue(Math.abs(c[0]) <= Dungeons.MAX_CELL && Math.abs(c[1]) <= Dungeons.MAX_CELL);
                assertTrue(seen.add(((long) c[0] << 32) ^ (c[1] & 0xffffffffL)), "sala repetida");
                if (i > 0) assertEquals(1, Math.abs(c[0] - cells.get(i - 1)[0]) + Math.abs(c[1] - cells.get(i - 1)[1]), "vizinhas");
            }
        }
    }

    @Test
    void partyScalingFormulas() {
        assertEquals(3, Dungeons.mobCount(3, 1, 2), "solo: só a base");
        assertEquals(9, Dungeons.mobCount(3, 4, 2));
        assertEquals(1.0, Dungeons.multiplier(1, 0.35), 1e-9);
        assertEquals(1.7, Dungeons.multiplier(3, 0.35), 1e-9);
        assertEquals(3, Dungeons.mobCount(3, 0, 2), "nunca menos que a base");
        for (int x = 0; x < 50; x++) {
            int t = DungeonStructure.RoomPiece.target(new BlockPos(17, 40, -3), x);
            assertTrue(t >= 1 && t <= 3, "alvo nunca é o símbolo inicial");
        }
    }

    @Test
    void roomTemplatesHaveTheirMechanisms() throws Exception {
        for (var room : DungeonStructure.Room.values()) {
            CompoundTag t = template(room.template().getPath());
            ListTag size = t.getList("size", Tag.TAG_INT);
            assertEquals(List.of(15, 9, 15), List.of(size.getInt(0), size.getInt(1), size.getInt(2)), room.name());
        }
        template("dungeon_entrance");
        for (String[] r : new String[][]{{"dungeon_combat", "combat"}, {"dungeon_puzzle", "puzzle"}, {"dungeon_champion", "champion"}}) {
            CompoundTag t = template(r[0]);
            List<int[]> seals = blocks(t, "alvorecer:dungeon_seal");
            assertEquals(1, seals.size(), r[0]);
            assertArrayEquals(new int[]{7, 0, 7}, seals.get(0), "selo no centro do piso");
        }
        CompoundTag puzzle = template("dungeon_puzzle");
        Set<Integer> runeX = new TreeSet<>(), glyphX = new TreeSet<>();
        blocks(puzzle, "alvorecer:dungeon_rune").forEach(p -> runeX.add(p[0]));
        blocks(puzzle, "alvorecer:rune_glyph").forEach(p -> glyphX.add(p[0]));
        assertEquals(4, runeX.size());
        assertEquals(runeX, glyphX, "cada runa embaixo do seu glifo");
        for (int x : runeX) assertTrue(x < 6 || x > 8, "glifos fora da passagem central");
        assertEquals(13 * 13 - 1, blocks(template("dungeon_trap"), "alvorecer:spike_trap").size(), "piso inteiro de espinhos, menos o do baú");
        assertEquals(1, blocks(template("dungeon_treasure"), "alvorecer:dungeon_vault").size());
        CompoundTag start = template("dungeon_start");
        assertTrue(blocks(start, "minecraft:air").stream().anyMatch(p -> p[0] == 7 && p[1] == 8 && p[2] == 7), "abertura do poço no teto");
    }

    @Test
    void dataTextsAndLoot() throws Exception {
        JsonObject lang = json("/assets/alvorecer/lang/pt_br.json");
        for (String b : new String[]{"dungeon_seal", "sealed_door", "dungeon_rune", "rune_glyph", "spike_trap", "dungeon_vault"}) {
            assertTrue(lang.has("block.alvorecer." + b), b);
            assertNotNull(json("/assets/alvorecer/blockstates/" + b + ".json"));
            assertNotNull(json("/assets/alvorecer/models/item/" + b + ".json"));
        }
        for (String s : new String[]{"dungeon", "aether_dungeon"}) {
            assertEquals("alvorecer:dungeon", json("/data/alvorecer/worldgen/structure/" + s + ".json").get("type").getAsString());
            assertNotNull(json("/data/alvorecer/worldgen/structure_set/" + s + ".json"));
            assertTrue(lang.has("structure.alvorecer." + s));
        }
        for (String t : new String[]{"dungeon/vault", "dungeon/vault_aether", "chests/dungeon_room", "chests/dungeon_treasure"}) {
            String loot = json("/data/alvorecer/loot_table/" + t + ".json").toString();
            if (t.startsWith("dungeon/")) assertTrue(loot.contains("alvorecer:dawn_sigil_fragment"), "todo relicário dá um fragmento");
        }
        for (String k : new String[]{"codex.alvorecer.dungeons", "quest.alvorecer.dungeon_delver", "advancement.alvorecer.dungeon",
                "quest.alvorecer.objective.clear_dungeon", "entity.alvorecer.dungeon_champion"}) {
            assertTrue(lang.has(k), k);
        }
        assertNotNull(json("/data/alvorecer/advancement/dungeon.json"));
    }
}
