package com.alvorecer;

import com.alvorecer.achievement.Achievements;
import com.alvorecer.network.ModNetwork;
import com.alvorecer.party.Parties;
import com.alvorecer.quest.Quest;
import com.alvorecer.quest.Quests;
import com.alvorecer.rpg.Derived;
import com.alvorecer.rpg.PlayerRpgData;
import com.alvorecer.rpg.Profession;
import com.alvorecer.rpg.Race;
import com.alvorecer.rpg.RpgClass;
import com.alvorecer.rpg.School;
import com.alvorecer.rpg.Stat;
import com.alvorecer.rpg.talent.Talent;
import com.alvorecer.rpg.talent.Talents;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Stage2Test {

    private static PlayerRpgData character(RpgClass cls, int level) {
        PlayerRpgData d = new PlayerRpgData();
        d.level = level;
        assertTrue(ModNetwork.createCharacter(d, new ModNetwork.CreateCharacter(Race.HUMAN.ordinal(), cls.ordinal(), Profession.MINER.ordinal(), 0)));
        return d;
    }

    private static JsonObject lang() throws Exception {
        try (var in = Stage2Test.class.getResourceAsStream("/assets/alvorecer/lang/pt_br.json")) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    void everyClassHasAFullTreeWhereChoicesMatter() throws Exception {
        JsonObject lang = lang();
        for (RpgClass c : RpgClass.values()) {
            List<Talent> tree = Talents.forClass(c);
            assertEquals(6, tree.size(), c.name());
            for (int tier = 1; tier <= 3; tier++) {
                int t = tier;
                assertEquals(2, tree.stream().filter(x -> x.tier() == t).count(), c + " tier " + tier);
            }
            int ranks = tree.stream().mapToInt(Talent::maxRank).sum();
            assertTrue(ranks > Talents.pointsAt(20), c + ": deve haver mais ranks do que pontos");
            assertEquals(2, Talents.specsFor(c).size(), c + " especializações");
            for (Talent t : tree) {
                assertTrue(lang.has(t.nameKey()), t.nameKey());
                if (t.special()) assertTrue(lang.has(t.descKey()), t.descKey());
                for (Talent.Bonus b : t.perRank()) assertTrue(lang.has(b.type().langKey()), b.type().langKey());
            }
            for (var s : Talents.specsFor(c)) {
                assertTrue(lang.has(s.nameKey()) && lang.has(s.descKey()), s.id());
                for (Talent.Bonus b : s.bonuses()) assertTrue(lang.has(b.type().langKey()), b.type().langKey());
            }
        }
    }

    @Test
    void talentSpendingRules() {
        PlayerRpgData d = character(RpgClass.WARRIOR, 1);
        assertEquals(Talents.Failure.NO_POINTS, Talents.canSpend(d, "warrior_weapon_training"));
        assertEquals(Talents.Failure.WRONG_CLASS, Talents.canSpend(d, "wizard_focus"));
        assertEquals(Talents.Failure.UNKNOWN, Talents.canSpend(d, "inexistente"));

        d.level = 10; // 5 pontos
        assertEquals(Talents.Failure.TIER_POINTS, Talents.canSpend(d, "warrior_tactics"), "tier 2 exige 3 pontos no tier 1");
        for (int i = 0; i < 3; i++) assertTrue(Talents.spend(d, "warrior_weapon_training"));
        assertEquals(Talents.Failure.MAX_RANK, Talents.canSpend(d, "warrior_weapon_training"));
        assertTrue(Talents.spend(d, "warrior_tactics"));
        assertEquals(Talents.Failure.TIER_LEVEL, Talents.canSpend(d, "warrior_unbreakable"), "tier 3 só no nível 14");
        assertTrue(Talents.spend(d, "warrior_tactics"));
        assertEquals(0, Talents.available(d));
        assertEquals(Talents.Failure.NO_POINTS, Talents.canSpend(d, "warrior_tactics"));
    }

    @Test
    void talentsAndSpecChangeDerivedValues() {
        PlayerRpgData d = character(RpgClass.WARRIOR, 20);
        double melee = Derived.meleeMultiplier(d);
        double crit = Derived.critChance(d);
        for (int i = 0; i < 3; i++) Talents.spend(d, "warrior_weapon_training");
        assertEquals(melee + 0.12, Derived.meleeMultiplier(d), 1e-9);

        assertFalse(Talents.chooseSpec(d, "wizard_evoker"), "especialização de outra classe");
        assertTrue(Talents.chooseSpec(d, "warrior_champion"));
        assertFalse(Talents.chooseSpec(d, "warrior_defender"), "só uma especialização");
        assertEquals(crit + 0.05, Derived.critChance(d), 1e-9);

        PlayerRpgData low = character(RpgClass.WIZARD, 4);
        assertFalse(Talents.chooseSpec(low, "wizard_evoker"), "especialização só no nível 5");
        low.level = 5;
        double before = Derived.power(low, Stat.INT, School.ELEMENTAL);
        assertTrue(Talents.chooseSpec(low, "wizard_evoker"));
        assertEquals(before * 1.15, Derived.power(low, Stat.INT, School.ELEMENTAL), 1e-9);

        PlayerRpgData bard = character(RpgClass.BARD, 2);
        int cha = bard.score(Stat.CHA);
        assertTrue(Talents.spend(bard, "bard_natural_charm"));
        assertEquals(cha + 1, bard.score(Stat.CHA), "talento de atributo soma no valor");
    }

    @Test
    void talentsPersistAndResetWithCharacter() {
        PlayerRpgData d = character(RpgClass.ROGUE, 12);
        Talents.spend(d, "rogue_reflexes");
        Talents.chooseSpec(d, "rogue_assassin");
        d.activeQuests.put("zombie_hunt", 4);
        d.completedQuests.put("lumber", 1234L);
        d.achievements.add("root");
        d.questsCompleted = 3;
        PlayerRpgData r = new PlayerRpgData();
        r.deserializeNBT(RegistryAccess.EMPTY, d.serializeNBT(RegistryAccess.EMPTY));
        assertEquals(1, r.talents.get("rogue_reflexes"));
        assertEquals("rogue_assassin", r.specialization);
        assertEquals(4, r.activeQuests.get("zombie_hunt"));
        assertEquals(1234L, r.completedQuests.get("lumber"));
        assertTrue(r.achievements.contains("root"));
        assertEquals(3, r.questsCompleted);

        r.resetCharacter();
        assertTrue(r.talents.isEmpty());
        assertNull(r.specialization);
        assertEquals(4, r.activeQuests.get("zombie_hunt"), "missões não são apagadas pelo recomeço");
    }

    @Test
    void partyXpSplit() {
        assertEquals(100, Parties.shareXp(100, 1, 0.1));
        assertEquals(55, Parties.shareXp(100, 2, 0.1), "100 × 1,1 ÷ 2");
        assertEquals(33, Parties.shareXp(100, 4, 0.1), "100 × 1,3 ÷ 4 = 32,5, arredondado");
    }

    @Test
    void questCatalogIsConsistent() throws Exception {
        JsonObject lang = lang();
        for (Quest q : Quests.all().values()) {
            assertTrue(lang.has(q.nameKey()) && lang.has(q.descKey()), q.id());
            assertTrue(q.count() > 0 && q.xp() > 0, q.id());
            for (Quest.RewardItem r : q.items()) {
                ResourceLocation id = ResourceLocation.parse(r.id());
                assertTrue(BuiltInRegistries.ITEM.containsKey(id), "item de recompensa inexistente: " + r.id());
            }
            if (q.objective() == Quest.Objective.KILL && !q.target().startsWith("#")) {
                assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.parse(q.target())), q.target());
            }
        }
    }

    @Test
    void questStatusFlow() {
        PlayerRpgData d = character(RpgClass.WARRIOR, 1);
        Quest hunt = Quests.get("zombie_hunt");
        Quest veteran = Quests.get("veteran");
        assertEquals(Quests.Status.AVAILABLE, Quests.status(null, d, hunt, 0));
        assertEquals(Quests.Status.LOCKED, Quests.status(null, d, veteran, 0), "exige nível 5");
        d.activeQuests.put("zombie_hunt", 9);
        assertEquals(Quests.Status.ACTIVE, Quests.status(null, d, hunt, 0));
        d.activeQuests.put("zombie_hunt", 10);
        assertEquals(Quests.Status.READY, Quests.status(null, d, hunt, 0));
        d.activeQuests.remove("zombie_hunt");
        d.completedQuests.put("zombie_hunt", 100L);
        assertEquals(Quests.Status.COOLDOWN, Quests.status(null, d, hunt, 100 + Quest.REPEAT_TICKS - 1), "diária ainda em espera");
        assertEquals(Quests.Status.AVAILABLE, Quests.status(null, d, hunt, 100 + Quest.REPEAT_TICKS), "diária volta após um dia");
        d.completedQuests.put("veteran", 1L);
        assertEquals(Quests.Status.DONE, Quests.status(null, d, veteran, 999999), "não repetível");
    }

    @Test
    void achievementsHaveAdvancementsAndTexts() throws Exception {
        JsonObject lang = lang();
        for (String id : Achievements.ALL) {
            assertNotNull(Stage2Test.class.getResource("/data/alvorecer/advancement/" + id + ".json"), id);
            assertTrue(lang.has("advancement.alvorecer." + id) && lang.has("advancement.alvorecer." + id + ".desc"), id);
        }
    }
}
