package com.alvorecer;

import com.alvorecer.ability.Abilities;
import com.alvorecer.ability.Ability;
import com.alvorecer.ability.AbilityService;
import com.alvorecer.ability.AbilityService.Failure;
import com.alvorecer.network.ModNetwork;
import com.alvorecer.rpg.PlayerRpgData;
import com.alvorecer.rpg.Profession;
import com.alvorecer.rpg.Race;
import com.alvorecer.rpg.RpgClass;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AbilityValidationTest {

    private static PlayerRpgData character(Race race, RpgClass cls) {
        PlayerRpgData d = new PlayerRpgData();
        assertTrue(ModNetwork.createCharacter(d, new ModNetwork.CreateCharacter(race.ordinal(), cls.ordinal(), Profession.MINER.ordinal(), 0)));
        return d;
    }

    @Test
    void everyReferencedAbilityExists() {
        List<String> ids = new ArrayList<>();
        for (Race r : Race.values()) ids.add(r.abilityId);
        for (RpgClass c : RpgClass.values()) {
            assertEquals(5, c.abilities.size(), c + " precisa de 4 habilidades + suprema");
            ids.addAll(c.abilities);
        }
        for (Profession p : Profession.values()) ids.add(p.abilityId);
        ids.addAll(Abilities.ITEM_ABILITIES);
        for (String id : ids) assertNotNull(Abilities.get(id), "habilidade sem implementação: " + id);
        assertEquals(ids.size(), Abilities.all().size(), "habilidade registrada que nada usa");
    }

    @Test
    void ultimatesAreExpensive() {
        for (RpgClass c : RpgClass.values()) {
            Ability ult = Abilities.get(c.abilities.get(4));
            for (int i = 0; i < 4; i++) {
                Ability a = Abilities.get(c.abilities.get(i));
                assertTrue(ult.cost() > a.cost(), c + ": suprema deve custar mais que " + a.id());
                assertTrue(ult.cooldownTicks() > a.cooldownTicks(), c + ": suprema deve ter recarga maior que " + a.id());
            }
            assertTrue(ult.cooldownTicks() >= 150 * 20, c + ": suprema com recarga de pelo menos 150 s");
        }
    }

    @Test
    void everyAbilityAndChoiceIsTranslated() throws Exception {
        JsonObject lang;
        try (var in = AbilityValidationTest.class.getResourceAsStream("/assets/alvorecer/lang/pt_br.json")) {
            assertNotNull(in, "pt_br.json ausente");
            lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        for (Ability a : Abilities.all().values()) {
            assertTrue(lang.has(a.nameKey()), a.nameKey());
            assertTrue(lang.has(a.descKey()), a.descKey());
        }
        for (Race r : Race.values()) assertTrue(lang.has(r.langKey() + ".desc"), r.langKey());
        for (RpgClass c : RpgClass.values()) assertTrue(lang.has(c.langKey() + ".desc"), c.langKey());
        for (Profession p : Profession.values()) assertTrue(lang.has(p.langKey() + ".desc"), p.langKey());
        for (Failure f : Failure.values()) {
            if (f != Failure.LOCKED && f != Failure.COOLDOWN) assertTrue(lang.has("msg.alvorecer.fail." + f.name().toLowerCase()), f.name());
        }
    }

    @Test
    void validationOrder() {
        PlayerRpgData none = new PlayerRpgData();
        assertEquals(Failure.NO_CHARACTER, AbilityService.validate(none, 1, 0, 0, false, false));

        PlayerRpgData d = character(Race.HUMAN, RpgClass.WARRIOR);
        assertNull(AbilityService.validate(d, 1, 0, 15, false, false));
        assertEquals(Failure.EMPTY_SLOT, AbilityService.validate(d, AbilityService.SLOT_SECONDARY, 0, 0, false, false));
        assertEquals(Failure.EMPTY_SLOT, AbilityService.validate(d, 42, 0, 0, false, false));
        assertEquals(Failure.LOCKED, AbilityService.validate(d, 2, 0, 0, false, false), "nível 3 necessário");
        assertEquals(Failure.LOCKED, AbilityService.validate(d, AbilityService.SLOT_ULTIMATE, 0, 0, false, false));
        assertEquals(Failure.STUNNED, AbilityService.validate(d, 1, 0, 15, true, false));

        d.cooldowns.put("warrior_power_strike", 100L);
        assertEquals(Failure.COOLDOWN, AbilityService.validate(d, 1, 99, 15, false, false));
        assertNull(AbilityService.validate(d, 1, 100, 15, false, false), "recarga termina no tick exato");

        d.energy = 10;
        assertEquals(Failure.NO_ENERGY, AbilityService.validate(d, 1, 100, 15, false, false));
    }

    @Test
    void bearFormBlocksOtherDruidAbilities() {
        PlayerRpgData d = character(Race.ELF, RpgClass.DRUID);
        d.level = 20;
        assertEquals(Failure.BEAR_FORM, AbilityService.validate(d, 1, 0, 0, false, true));
        assertNull(AbilityService.validate(d, 3, 0, 0, false, true), "a própria Forma de Urso pode ser renovada");
        assertNull(AbilityService.validate(d, AbilityService.SLOT_RACE, 0, 0, false, true), "habilidade racial não é bloqueada");
    }

    @Test
    void costModifiers() {
        Ability blink = Abilities.get("wizard_blink");
        Ability missile = Abilities.get("wizard_arcane_missile");
        PlayerRpgData dwarf = character(Race.DWARF, RpgClass.WIZARD);
        assertEquals(blink.cost() * 1.5f, AbilityService.cost(dwarf, blink, 0, 1.0), 1e-4, "anão paga +50% em mobilidade");
        assertEquals(missile.cost(), AbilityService.cost(dwarf, missile, 0, 1.0), 1e-4);
        assertEquals(missile.cost() * 1.2f, AbilityService.cost(dwarf, missile, 2, 1.0), 1e-4, "+10% por peça de armadura pesada demais");

        PlayerRpgData orc = character(Race.ORC, RpgClass.WIZARD);
        assertEquals(missile.cost() * 1.1f, AbilityService.cost(orc, missile, 0, 1.0), 1e-4, "orc paga +10% de mana");
        Ability fury = Abilities.get("orc_fury");
        assertEquals(fury.cost(), AbilityService.cost(orc, fury, 4, 1.0), 1e-4, "armadura não afeta energia");
        assertEquals(fury.cost() * 2f, AbilityService.cost(orc, fury, 0, 2.0), 1e-4, "multiplicador do servidor");
    }

    @Test
    void secondaryProfessionSlot() {
        PlayerRpgData d = character(Race.GNOME, RpgClass.BARD);
        assertEquals("miner_ore_sense", AbilityService.abilityId(d, AbilityService.SLOT_PROFESSION));
        assertNull(AbilityService.abilityId(d, AbilityService.SLOT_SECONDARY));
        d.secondaryProfession = Profession.COOK;
        assertEquals("cook_feast", AbilityService.abilityId(d, AbilityService.SLOT_SECONDARY));
    }
}
