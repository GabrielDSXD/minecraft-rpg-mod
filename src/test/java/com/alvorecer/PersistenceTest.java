package com.alvorecer;

import com.alvorecer.rpg.Element;
import com.alvorecer.rpg.PlayerRpgData;
import com.alvorecer.rpg.Profession;
import com.alvorecer.rpg.Race;
import com.alvorecer.rpg.RpgClass;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PersistenceTest {
    private static final RegistryAccess NONE = RegistryAccess.EMPTY;

    @Test
    void roundTripKeepsEverything() {
        PlayerRpgData d = new PlayerRpgData();
        d.race = Race.TIEFLING;
        d.rpgClass = RpgClass.BARD;
        d.profession = Profession.ALCHEMIST;
        d.secondaryProfession = Profession.COOK;
        d.element = Element.LIGHTNING;
        d.level = 12;
        d.xp = 345;
        d.allocated[5] = 6;
        d.unspentPoints = 2;
        d.mana = 77.5f;
        d.energy = 12f;
        d.cooldowns.put("bard_charm", 999L);
        d.professionLevel = 8;
        d.professionXp = 40;
        d.secondaryLevel = 3;
        d.secondaryXp = 7;
        d.professionDay = 4;
        d.professionXpToday = 900;
        d.visitedBiomes.add("minecraft:desert");
        d.lastRespecTime = 5000;
        d.codexGiven = true;
        d.enchantsSeen = 9;

        CompoundTag tag = d.serializeNBT(NONE);
        PlayerRpgData r = new PlayerRpgData();
        r.deserializeNBT(NONE, tag);

        assertEquals(tag, r.serializeNBT(NONE), "serializar de novo deve dar o mesmo NBT");
        assertEquals(Race.TIEFLING, r.race);
        assertEquals(Profession.COOK, r.secondaryProfession);
        assertEquals(Element.LIGHTNING, r.element);
        assertEquals(12, r.level);
        assertEquals(6, r.allocated[5]);
        assertEquals(999L, r.cooldowns.get("bard_charm"));
        assertTrue(r.visitedBiomes.contains("minecraft:desert"));
        assertTrue(r.codexGiven);
        assertEquals(9, r.enchantsSeen);
    }

    @Test
    void emptyTagGivesSafeDefaults() {
        PlayerRpgData r = new PlayerRpgData();
        r.deserializeNBT(NONE, new CompoundTag());
        assertFalse(r.hasCharacter());
        assertEquals(1, r.level);
        assertEquals(1, r.professionLevel);
        assertEquals(Element.FIRE, r.element);
        assertEquals(-1, r.enchantsSeen);
        assertEquals(Long.MIN_VALUE, r.lastRespecTime);
    }

    @Test
    void corruptValuesAreSanitized() {
        CompoundTag tag = new CompoundTag();
        tag.putString("race", "DRAGAO_INEXISTENTE");
        tag.putInt("level", 999);
        tag.putLong("xp", -50);
        tag.putIntArray("allocated", new int[]{1, 2, 3, 4, 5, 6, 7, 8});
        PlayerRpgData r = new PlayerRpgData();
        r.deserializeNBT(NONE, tag);
        assertNull(r.race);
        assertEquals(20, r.level);
        assertEquals(0, r.xp);
        assertEquals(6, r.allocated[5]);
    }

    @Test
    void resetKeepsLevelButClearsChoices() {
        PlayerRpgData d = new PlayerRpgData();
        d.race = Race.ORC;
        d.rpgClass = RpgClass.WARRIOR;
        d.profession = Profession.MINER;
        d.level = 9;
        d.allocated[0] = 5;
        d.cooldowns.put("orc_fury", 10L);
        d.resetCharacter();
        assertFalse(d.hasCharacter());
        assertEquals(9, d.level);
        assertEquals(0, d.allocated[0]);
        assertTrue(d.cooldowns.isEmpty());
    }
}
