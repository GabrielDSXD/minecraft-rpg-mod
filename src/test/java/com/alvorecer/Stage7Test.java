package com.alvorecer;

import com.alvorecer.ability.Abilities;
import com.alvorecer.entity.boss.DawnGuardianEntity;
import com.alvorecer.item.gear.GearInfo;
import com.alvorecer.item.gear.GearRarity;
import com.alvorecer.quest.Quests;
import com.alvorecer.registry.ModGear;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class Stage7Test {
    private static final String[] SOUNDS = {"ambient", "hurt", "death", "roar", "slam", "beam_charge", "beam", "ring", "summon",
            "discharge_charge", "discharge"};

    private static JsonObject json(String path) throws Exception {
        try (var in = Stage7Test.class.getResourceAsStream(path)) {
            assertNotNull(in, path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    void guardianAssetsSoundsAndTexts() throws Exception {
        JsonObject lang = json("/assets/alvorecer/lang/pt_br.json");
        JsonObject sounds = json("/assets/alvorecer/sounds.json");
        for (String s : SOUNDS) {
            String event = "dawn_guardian." + s;
            assertTrue(sounds.has(event), event);
            assertTrue(lang.has("subtitles.alvorecer." + event), "legenda " + event);
            assertNotNull(Stage7Test.class.getResource("/assets/alvorecer/sounds/dawn_guardian/" + s + ".ogg"), event);
        }
        for (String t : new String[]{"entity/dawn_guardian", "entity/dawn_guardian_glow", "entity/dawn_guardian_charge", "item/heart_of_dawn",
                "item/dawn_sigil", "item/dawn_essence", "block/dawn_altar_top", "block/dawn_altar_side"}) {
            assertNotNull(Stage7Test.class.getResource("/assets/alvorecer/textures/" + t + ".png"), t);
        }
        for (String k : new String[]{"entity.alvorecer.dawn_guardian", "bestiary.alvorecer.dawn_guardian.advanced", "item.alvorecer.heart_of_dawn",
                "item.alvorecer.dawn_sigil", "item.alvorecer.dawn_essence", "block.alvorecer.dawn_altar", "ability.alvorecer.item_dawn_heart",
                "msg.alvorecer.guardian_hide", "msg.alvorecer.guardian_discharge", "codex.alvorecer.bosses", "quest.alvorecer.first_dawn",
                "advancement.alvorecer.guardian", "item.alvorecer.heart_of_dawn.passive"}) {
            assertTrue(lang.has(k), k);
        }
        assertEquals(lang.keySet(), json("/assets/alvorecer/lang/en_us.json").keySet(), "pt_br e en_us com as mesmas chaves");
    }

    @Test
    void rewardsRecipesAndHeartLimits() throws Exception {
        String loot = json("/data/alvorecer/loot_table/entities/dawn_guardian.json").toString();
        for (String item : new String[]{"alvorecer:dawn_essence", "alvorecer:aurorite_gem", "alvorecer:sentinel_heart"}) assertTrue(loot.contains(item), item);
        assertTrue(json("/data/alvorecer/recipe/dawn_sigil.json").toString().contains("alvorecer:dawn_sigil_fragment"));
        assertTrue(json("/data/alvorecer/recipe/dawn_sigil_from_essence.json").toString().contains("alvorecer:dawn_essence"));
        assertNotNull(json("/data/alvorecer/advancement/guardian.json"));
        assertNotNull(Quests.get("first_dawn"));
        GearInfo heart = GearInfo.get(ModGear.HEART_OF_DAWN.get());
        assertEquals(GearRarity.LEGENDARY, heart.rarity());
        assertEquals(15, heart.level(), "Coração exige nível 15");
        assertEquals("item_dawn_heart", heart.ability());
        assertEquals(300 * 20, Abilities.get("item_dawn_heart").cooldownTicks(), "Aurora Renascente: 5 min");
        assertEquals(1, ModGear.HEART_OF_DAWN.get().getDefaultMaxStackSize(), "não empilha");
    }

    @Test
    void telegraphsAreLongEnoughToReact() {
        assertTrue(DawnGuardianEntity.SLAM_WINDUP >= 20, "golpe no chão: >= 1 s de aviso");
        assertTrue(DawnGuardianEntity.BEAM_WINDUP - DawnGuardianEntity.BEAM_LOCK >= 10, "raio: >= 0,5 s com a mira travada");
        assertTrue(DawnGuardianEntity.DISCHARGE_CHARGE >= 100, "descarga: >= 5 s para se esconder");
        assertTrue(DawnGuardianEntity.PHASE2 > DawnGuardianEntity.PHASE3 && DawnGuardianEntity.PHASE3 > 0, "3 fases");
    }

    @Test
    void skyArenaHasAltarAndPillars() throws Exception {
        CompoundTag t;
        try (var in = Stage7Test.class.getResourceAsStream("/data/alvorecer/structure/sky_arena.nbt")) {
            t = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
        }
        ListTag palette = t.getList("palette", Tag.TAG_COMPOUND);
        Set<String> pillarColumns = new HashSet<>();
        boolean altarAtCenter = false;
        for (Tag b : t.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) b;
            String name = palette.getCompound(c.getInt("state")).getString("Name");
            ListTag pos = c.getList("pos", Tag.TAG_INT);
            if (name.equals("alvorecer:dawn_altar")) altarAtCenter = pos.getInt(0) == 13 && pos.getInt(2) == 13;
            if (name.equals("minecraft:quartz_pillar")) pillarColumns.add((pos.getInt(0) / 2) + "," + (pos.getInt(2) / 2));
        }
        assertTrue(altarAtCenter, "altar no centro da arena");
        assertTrue(pillarColumns.size() >= 8, "8 pilares de cobertura: " + pillarColumns.size());
    }
}
