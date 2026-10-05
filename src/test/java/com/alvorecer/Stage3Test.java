package com.alvorecer;

import com.alvorecer.ability.Abilities;
import com.alvorecer.crafting.ProfessionRecipes;
import com.alvorecer.item.gear.CasterItem;
import com.alvorecer.item.gear.Equipment;
import com.alvorecer.item.gear.GearInfo;
import com.alvorecer.network.ModNetwork;
import com.alvorecer.registry.ModGear;
import com.alvorecer.rpg.PlayerRpgData;
import com.alvorecer.rpg.Profession;
import com.alvorecer.rpg.Race;
import com.alvorecer.rpg.RpgClass;
import com.alvorecer.rpg.WeaponCategory;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class Stage3Test {

    private static JsonObject lang() throws Exception {
        try (var in = Stage3Test.class.getResourceAsStream("/assets/alvorecer/lang/pt_br.json")) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    void everyGearItemHasTextsModelAndTexture() throws Exception {
        JsonObject lang = lang();
        assertTrue(GearInfo.all().size() >= 45, "quantidade de equipamentos: " + GearInfo.all().size());
        for (var e : GearInfo.all()) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(e.getKey().get());
            String key = "item.alvorecer." + id.getPath();
            assertTrue(lang.has(key), key);
            assertTrue(lang.has(key + ".lore"), key + ".lore");
            assertNotNull(Stage3Test.class.getResource("/assets/alvorecer/models/item/" + id.getPath() + ".json"), "modelo " + id);
            GearInfo g = e.getValue();
            assertTrue(g.level() >= 1 && g.level() <= 20, id.toString());
            if (g.ability() != null) assertNotNull(Abilities.get(g.ability()), g.ability());
            if (g.set() != null) assertNotNull(Equipment.set(g.set()), g.set());
        }
    }

    @Test
    void weaponCategoriesComeFromGear() {
        assertEquals(WeaponCategory.DAGGER, WeaponCategory.of(new ItemStack(ModGear.IRON_DAGGER.get())));
        assertEquals(WeaponCategory.HAMMER, WeaponCategory.of(new ItemStack(ModGear.SOLAR_BREAKER.get())));
        assertEquals(WeaponCategory.SPEAR, WeaponCategory.of(new ItemStack(ModGear.AURORITE_SPEAR.get())));
        assertEquals(WeaponCategory.STAFF, WeaponCategory.of(new ItemStack(ModGear.ARCANE_STAFF.get())));
        assertTrue(WeaponCategory.HAMMER.heavy(), "martelos são pesados (Orc +15%, Halfling −25%)");
        assertEquals(com.alvorecer.rpg.ArmorWeight.HEAVY, com.alvorecer.rpg.ArmorWeight.of(new ItemStack(ModGear.AURORITE_ARMOR[1].get())));
        assertEquals(com.alvorecer.rpg.ArmorWeight.LIGHT, com.alvorecer.rpg.ArmorWeight.of(new ItemStack(ModGear.ARCANE[0].get())));
    }

    @Test
    void professionRecipesAreValid() {
        for (var r : ProfessionRecipes.all().values()) {
            assertTrue(r.level() >= 1 && r.level() <= 20, r.id());
            assertFalse(r.resultStack().isEmpty(), r.id());
            for (var in : r.inputs()) {
                if (!in.id().startsWith("#")) assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(in.id())), r.id() + ": " + in.id());
                assertTrue(in.count() > 0, r.id());
            }
        }
        for (Profession p : Profession.values()) {
            assertTrue(ProfessionRecipes.all().values().stream().anyMatch(r -> r.profession() == p), "profissão sem receita: " + p);
        }
    }

    @Test
    void arcaneStaffListsOnlyUnlockedMagic() {
        PlayerRpgData d = new PlayerRpgData();
        d.level = 6;
        ModNetwork.createCharacter(d, new ModNetwork.CreateCharacter(Race.ELF.ordinal(), RpgClass.WIZARD.ordinal(), 0, 0));
        CasterItem staff = ModGear.ARCANE_STAFF.get();
        var spells = staff.spells(d);
        assertEquals("staff_arcane_bolt", spells.get(0));
        assertTrue(spells.contains("elf_fey_step") && spells.contains("wizard_arcane_missile") && spells.contains("wizard_fireball"));
        assertFalse(spells.contains("wizard_blink"), "Piscar só no nível 10");
        assertEquals(1, ModGear.OAK_STAFF.get().spells(d).size(), "cajado simples não seleciona feitiços");

        PlayerRpgData warrior = new PlayerRpgData();
        ModNetwork.createCharacter(warrior, new ModNetwork.CreateCharacter(Race.HUMAN.ordinal(), RpgClass.WARRIOR.ordinal(), 0, 0));
        assertEquals(1, staff.spells(warrior).size(), "habilidades físicas não entram no cajado");
    }
}
