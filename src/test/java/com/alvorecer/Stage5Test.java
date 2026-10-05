package com.alvorecer;

import com.alvorecer.achievement.Achievements;
import com.alvorecer.quest.Quests;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

class Stage5Test {
    private static final String[] STRUCTURES = {"goblin_camp", "watch_tower", "dawn_shrine", "aether_ruins", "sky_arena"};
    private static final String[] BIOMES = {"aether_meadows", "luminous_forest", "crystal_highlands", "ruined_expanse"};

    private static JsonObject json(String path) throws Exception {
        try (var in = Stage5Test.class.getResourceAsStream(path)) {
            assertNotNull(in, path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    void structuresReferenceExistingData() throws Exception {
        JsonObject lang = json("/assets/alvorecer/lang/pt_br.json");
        Pattern loot = Pattern.compile("alvorecer:(chests/[a-z_]+)");
        for (String id : STRUCTURES) {
            JsonObject s = json("/data/alvorecer/worldgen/structure/" + id + ".json");
            assertEquals("alvorecer:template", s.get("type").getAsString());
            String template = s.get("template").getAsString().replace("alvorecer:", "");
            String biomes = s.get("biomes").getAsString().replace("#alvorecer:", "");
            assertNotNull(json("/data/alvorecer/tags/worldgen/biome/" + biomes + ".json"), "tag de biomas " + id);
            assertEquals("alvorecer:" + id, json("/data/alvorecer/worldgen/structure_set/" + id + ".json")
                    .getAsJsonArray("structures").get(0).getAsJsonObject().get("structure").getAsString());
            assertTrue(lang.has("structure.alvorecer." + id), "nome " + id);
            try (var in = Stage5Test.class.getResourceAsStream("/data/alvorecer/structure/" + template + ".nbt")) {
                assertNotNull(in, "molde " + template);
                var m = loot.matcher(new String(new GZIPInputStream(in).readAllBytes(), StandardCharsets.ISO_8859_1));
                while (m.find()) assertNotNull(json("/data/alvorecer/loot_table/" + m.group(1) + ".json"), "loot " + m.group(1));
            }
        }
    }

    @Test
    void aetherDimensionAndProgression() throws Exception {
        JsonObject lang = json("/assets/alvorecer/lang/pt_br.json");
        JsonObject dim = json("/data/alvorecer/dimension/aether_realms.json");
        assertEquals("alvorecer:aether_realms", dim.get("type").getAsString());
        String sources = dim.getAsJsonObject("generator").getAsJsonObject("biome_source").toString();
        for (String b : BIOMES) {
            assertTrue(sources.contains("alvorecer:" + b), "bioma na dimensão " + b);
            assertNotNull(json("/data/alvorecer/worldgen/biome/" + b + ".json"));
            assertTrue(lang.has("biome.alvorecer." + b), "nome " + b);
            // Ilhas chegam a y~250 e esfriam 0.05 a cada 40 blocos acima de y=80; abaixo de 0.15 a superfície neva
            assertTrue(json("/data/alvorecer/worldgen/biome/" + b + ".json").get("temperature").getAsFloat() >= 0.6f, "sem neve " + b);
        }
        // Mesma regra do jogo ("Feature order cycle"): cada feature numa só etapa e na mesma ordem relativa em todos os biomas
        java.util.Set<String> order = new java.util.HashSet<>();
        java.util.Map<String, Integer> stepOf = new java.util.HashMap<>();
        for (String b : BIOMES) {
            var steps = json("/data/alvorecer/worldgen/biome/" + b + ".json").getAsJsonArray("features");
            for (int st = 0; st < steps.size(); st++) {
                var list = steps.get(st).getAsJsonArray();
                for (int i = 0; i < list.size(); i++) {
                    String f = list.get(i).getAsString();
                    Integer prev = stepOf.putIfAbsent(f, st);
                    assertTrue(prev == null || prev == st, "etapa única " + f);
                    for (int j = i + 1; j < list.size(); j++) {
                        String g = list.get(j).getAsString();
                        assertNotEquals(f, g, "feature repetida em " + b);
                        assertFalse(order.contains(g + ">" + f), "ordem inconsistente " + f + "/" + g + " em " + b);
                        order.add(f + ">" + g);
                    }
                }
            }
        }
        assertEquals("alvorecer:aether", json("/data/alvorecer/dimension_type/aether_realms.json").get("effects").getAsString());
        assertTrue(Quests.get("into_the_aether") != null, "missão do Éter");
        for (String a : new String[]{"portal", "aether"}) {
            assertTrue(Achievements.ALL.contains(a));
            assertNotNull(json("/data/alvorecer/advancement/" + a + ".json"));
            assertTrue(lang.has("advancement.alvorecer." + a));
        }
        for (String k : new String[]{"block.alvorecer.dawnstone", "item.alvorecer.dawn_key", "codex.alvorecer.world", "msg.alvorecer.portal_invalid"}) {
            assertTrue(lang.has(k), k);
        }
        assertNotNull(json("/data/alvorecer/recipe/dawn_key.json"));
    }
}
