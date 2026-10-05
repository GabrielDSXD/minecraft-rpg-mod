package com.alvorecer;

import com.alvorecer.rpg.Leveling;
import com.alvorecer.rpg.PlayerRpgData;
import com.alvorecer.rpg.Progression;
import com.alvorecer.rpg.Race;
import com.alvorecer.rpg.RpgClass;
import com.alvorecer.rpg.Profession;
import com.alvorecer.rpg.Stat;
import com.alvorecer.network.ModNetwork;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProgressionTest {

    @Test
    void modifierFollowsDndTable() {
        assertEquals(-1, Progression.modifier(8));
        assertEquals(-1, Progression.modifier(9));
        assertEquals(0, Progression.modifier(10));
        assertEquals(0, Progression.modifier(11));
        assertEquals(1, Progression.modifier(12));
        assertEquals(7, Progression.modifier(24));
    }

    @Test
    void xpCurveIsIncreasingAndCapped() {
        assertEquals(50, Progression.xpToNext(1));
        for (int l = 1; l < Progression.MAX_LEVEL - 1; l++) {
            assertTrue(Progression.xpToNext(l + 1) > Progression.xpToNext(l));
        }
        assertEquals(Long.MAX_VALUE, Progression.xpToNext(Progression.MAX_LEVEL));
    }

    @Test
    void proficiencyAndDc() {
        assertEquals(2, Progression.proficiency(1));
        assertEquals(3, Progression.proficiency(5));
        assertEquals(6, Progression.proficiency(17));
        assertEquals(8 + 2 + 3, Progression.saveDc(1, 16));
    }

    @Test
    void totalPointsAtMaxLevel() {
        PlayerRpgData d = new PlayerRpgData();
        d.level = 20;
        // 3 na criação + 19 níveis + 2 extras em 5 marcos
        assertEquals(3 + 19 + 10, d.totalStatPoints());
    }

    @Test
    void addXpLevelsUpMultipleTimesAndStopsAtCap() {
        PlayerRpgData d = new PlayerRpgData();
        long needed = Progression.xpToNext(1) + Progression.xpToNext(2) + Progression.xpToNext(3) + 10;
        int gained = Leveling.addXp(d, needed);
        assertEquals(3, gained);
        assertEquals(4, d.level);
        assertEquals(10, d.xp);
        assertEquals(1 + 1 + 3, d.unspentPoints);

        assertEquals(16, Leveling.addXp(d, Long.MAX_VALUE / 4));
        assertEquals(Progression.MAX_LEVEL, d.level);
        assertEquals(0, d.xp);
        assertEquals(0, Leveling.addXp(d, 1000));
    }

    @Test
    void professionDailyCapResetsEachDay() {
        PlayerRpgData d = new PlayerRpgData();
        assertEquals(1500, Leveling.professionAllowance(d, 100, 1500));
        d.professionXpToday = 1400;
        assertEquals(100, Leveling.professionAllowance(d, 200, 1500));
        assertEquals(1500, Leveling.professionAllowance(d, 24000 + 5, 1500), "novo dia zera o contador");
        assertEquals(Long.MAX_VALUE, Leveling.professionAllowance(d, 24000 + 5, 0), "0 = sem limite");
    }

    @Test
    void creationValidatesInputAndOnlyOnce() {
        PlayerRpgData d = new PlayerRpgData();
        assertFalse(ModNetwork.createCharacter(d, new ModNetwork.CreateCharacter(99, 0, 0, 0)));
        assertFalse(ModNetwork.createCharacter(d, new ModNetwork.CreateCharacter(0, -1, 0, 0)));
        assertFalse(d.hasCharacter());

        assertTrue(ModNetwork.createCharacter(d, new ModNetwork.CreateCharacter(Race.ELF.ordinal(), RpgClass.RANGER.ordinal(), Profession.HUNTER.ordinal(), 0)));
        assertEquals(Race.ELF, d.race);
        assertEquals(Progression.CREATION_POINTS, d.unspentPoints);
        assertEquals(14, d.score(Stat.DEX), "10 + 2 do elfo + 2 do atributo-chave do Patrulheiro");
        assertFalse(ModNetwork.createCharacter(d, new ModNetwork.CreateCharacter(0, 0, 0, 0)), "não pode recriar sem reset");
    }

    @Test
    void allocationRespectsPointsAndCap() {
        PlayerRpgData d = new PlayerRpgData();
        ModNetwork.createCharacter(d, new ModNetwork.CreateCharacter(Race.HUMAN.ordinal(), RpgClass.WIZARD.ordinal(), 0, 0));
        assertEquals(13, d.score(Stat.INT), "10 + 1 humano + 2 atributo-chave");
        assertTrue(ModNetwork.allocate(d, Stat.INT.ordinal()));
        assertEquals(14, d.score(Stat.INT));
        assertFalse(ModNetwork.allocate(d, 42), "índice inválido");
        d.unspentPoints = 100;
        while (ModNetwork.allocate(d, Stat.INT.ordinal())) {
            // até o teto
        }
        assertEquals(Progression.MAX_SCORE, d.score(Stat.INT));
        d.unspentPoints = 0;
        assertFalse(ModNetwork.allocate(d, Stat.STR.ordinal()), "sem pontos livres");
    }
}
