package com.alvorecer;

import com.alvorecer.registry.ModMobs;
import com.alvorecer.registry.ModSounds;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class Stage4Test {

    private static JsonObject json(String path) throws Exception {
        try (var in = Stage4Test.class.getResourceAsStream(path)) {
            assertNotNull(in, path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    void everyCreatureHasTextsEggLootAndAttributes() throws Exception {
        JsonObject lang = json("/assets/alvorecer/lang/pt_br.json");
        assertEquals(10, ModMobs.CREATURES.size(), "7 criaturas + 3 NPCs");
        for (var e : ModMobs.CREATURES.entrySet()) {
            String id = e.getKey();
            assertTrue(lang.has("entity.alvorecer." + id), id);
            assertTrue(lang.has("bestiary.alvorecer." + id), "bestiário " + id);
            assertTrue(lang.has("item.alvorecer." + id + "_spawn_egg"), "ovo " + id);
            assertNotNull(Stage4Test.class.getResource("/assets/alvorecer/models/item/" + id + "_spawn_egg.json"), "modelo do ovo " + id);
            assertNotNull(Stage4Test.class.getResource("/data/alvorecer/loot_table/entities/" + id + ".json"), "loot " + id);
            assertTrue(DefaultAttributes.hasSupplier(e.getValue().get()), "atributos " + id);
        }
        for (String id : new String[]{"goblin", "skeleton_warrior", "arcane_wolf", "elemental", "stone_sentinel", "fairy", "wyrmling"}) {
            assertTrue(lang.has("bestiary.alvorecer." + id + ".advanced"), "bestiário avançado " + id);
        }
    }

    @Test
    void everySoundEventHasFileAndSubtitle() throws Exception {
        JsonObject sounds = json("/assets/alvorecer/sounds.json");
        JsonObject lang = json("/assets/alvorecer/lang/pt_br.json");
        int count = 0;
        for (var holder : ModSounds.SOUNDS.getEntries()) {
            String name = holder.getId().getPath();
            assertTrue(sounds.has(name), "sounds.json sem " + name);
            String file = sounds.getAsJsonObject(name).getAsJsonArray("sounds").get(0).getAsString().replace("alvorecer:", "");
            assertNotNull(Stage4Test.class.getResource("/assets/alvorecer/sounds/" + file + ".ogg"), "arquivo " + file);
            assertTrue(lang.has("subtitles.alvorecer." + name), "legenda " + name);
            count++;
        }
        assertEquals(30, count);
    }

    @Test
    void entityTexturesExist() {
        for (String t : new String[]{"goblin", "skeleton_warrior", "arcane_wolf", "arcane_wolf_eyes", "elemental_fire", "elemental_frost",
                "stone_sentinel", "stone_sentinel_glow", "fairy", "wyrmling", "npc_guild_master", "npc_merchant", "npc_sage"}) {
            assertNotNull(Stage4Test.class.getResource("/assets/alvorecer/textures/entity/" + t + ".png"), t);
        }
        assertTrue(BuiltInRegistries.ITEM.containsKey(net.minecraft.resources.ResourceLocation.parse("alvorecer:bestiary_scepter")));
    }
}
