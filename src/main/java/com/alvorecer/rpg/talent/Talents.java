package com.alvorecer.rpg.talent;

import com.alvorecer.rpg.PlayerRpgData;
import com.alvorecer.rpg.RpgClass;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.alvorecer.rpg.talent.BonusType.*;

/**
 * Árvores de talentos (3 tiers × 2 talentos por classe) e especializações (2 por classe, escolhidas no nível 5).
 * Pontos: 1 a cada nível par (10 no nível 20). Ranks possíveis: 3+3 (tier 1) + 3+3 (tier 2) + 1+1 (tier 3) = 14,
 * então não é possível pegar tudo.
 */
public final class Talents {
    public static final int SPEC_LEVEL = 5;
    /** Nível mínimo e pontos já gastos nos tiers anteriores para abrir cada tier. */
    public static final int[] TIER_LEVEL = {0, 1, 8, 14};
    public static final int[] TIER_POINTS = {0, 0, 3, 6};

    public record Specialization(String id, RpgClass rpgClass, List<Talent.Bonus> bonuses) {
        public String nameKey() {
            return "spec.alvorecer." + id;
        }

        public String descKey() {
            return "spec.alvorecer." + id + ".desc";
        }
    }

    public enum Failure { NO_CHARACTER, WRONG_CLASS, MAX_RANK, NO_POINTS, TIER_LEVEL, TIER_POINTS, UNKNOWN }

    private static final Map<String, Talent> TALENTS = new LinkedHashMap<>();
    private static final Map<String, Specialization> SPECS = new LinkedHashMap<>();

    private Talents() {
    }

    private static Talent.Bonus b(BonusType type, double amount) {
        return new Talent.Bonus(type, amount);
    }

    private static void t(RpgClass c, String id, int tier, Talent.Bonus... perRank) {
        TALENTS.put(id, new Talent(id, c, tier, tier == 3 ? 1 : 3, List.of(perRank), tier == 3));
    }

    private static void spec(RpgClass c, String id, Talent.Bonus... bonuses) {
        SPECS.put(id, new Specialization(id, c, List.of(bonuses)));
    }

    static {
        RpgClass c = RpgClass.WARRIOR;
        t(c, "warrior_weapon_training", 1, b(MELEE, 0.04));
        t(c, "warrior_tough_hide", 1, b(MAX_HEALTH, 2));
        t(c, "warrior_endless_breath", 2, b(COST, 0.06));
        t(c, "warrior_tactics", 2, b(COOLDOWN, 0.05));
        t(c, "warrior_unbreakable", 3);
        t(c, "warrior_executioner", 3);
        spec(c, "warrior_champion", b(CRIT_CHANCE, 0.05), b(CRIT_DAMAGE, 0.25));
        spec(c, "warrior_defender", b(DAMAGE_TAKEN, 0.10), b(MAX_HEALTH, 4));

        c = RpgClass.BARBARIAN;
        t(c, "barbarian_bloodthirst", 1, b(MELEE, 0.04));
        t(c, "barbarian_thick_skin", 1, b(DAMAGE_TAKEN, 0.03));
        t(c, "barbarian_rising_fury", 2, b(REGEN, 0.10));
        t(c, "barbarian_brutality", 2, b(CRIT_CHANCE, 0.03));
        t(c, "barbarian_unstoppable", 3);
        t(c, "barbarian_carnage", 3);
        spec(c, "barbarian_berserker", b(MELEE, 0.10), b(CRIT_CHANCE, 0.05));
        spec(c, "barbarian_totem", b(MAGIC_RESIST, 0.15), b(DAMAGE_TAKEN, 0.05));

        c = RpgClass.WIZARD;
        t(c, "wizard_arcane_study", 1, b(POWER_ARCANE, 0.05));
        t(c, "wizard_elemental_affinity", 1, b(POWER_ELEMENTAL, 0.05));
        t(c, "wizard_mana_reserve", 2, b(MAX_MANA, 0.08));
        t(c, "wizard_focus", 2, b(COOLDOWN, 0.05));
        t(c, "wizard_overload", 3);
        t(c, "wizard_sharp_mind", 3, b(SAVE_DC, 1), b(CRIT_CHANCE, 0.05));
        spec(c, "wizard_evoker", b(POWER_ELEMENTAL, 0.15));
        spec(c, "wizard_arcanist", b(POWER_ARCANE, 0.15), b(COST, 0.10));

        c = RpgClass.ROGUE;
        t(c, "rogue_sharp_blades", 1, b(MELEE, 0.04));
        t(c, "rogue_reflexes", 1, b(CRIT_CHANCE, 0.02));
        t(c, "rogue_precise_strikes", 2, b(CRIT_DAMAGE, 0.10));
        t(c, "rogue_thief_breath", 2, b(REGEN, 0.10));
        t(c, "rogue_assassinate", 3);
        t(c, "rogue_elusive", 3);
        spec(c, "rogue_assassin", b(SNEAK_ATTACK, 0.40), b(CRIT_DAMAGE, 0.15));
        spec(c, "rogue_trickster", b(COOLDOWN, 0.15), b(SPEED, 0.10));

        c = RpgClass.RANGER;
        t(c, "ranger_eagle_eye", 1, b(RANGED, 0.04));
        t(c, "ranger_light_steps", 1, b(SPEED, 0.03));
        t(c, "ranger_hunt", 2, b(POWER_NATURE, 0.05), b(POWER_PHYSICAL, 0.03));
        t(c, "ranger_deep_quiver", 2, b(COST, 0.06));
        t(c, "ranger_deadly_shot", 3);
        t(c, "ranger_instinct", 3, b(CRIT_CHANCE, 0.06));
        spec(c, "ranger_marksman", b(RANGED, 0.15));
        spec(c, "ranger_scout", b(SPEED, 0.10), b(COOLDOWN, 0.10));

        c = RpgClass.CLERIC;
        t(c, "cleric_faith", 1, b(POWER_DIVINE, 0.05));
        t(c, "cleric_healing_hands", 1, b(HEALING, 0.06));
        t(c, "cleric_devotion", 2, b(MAX_MANA, 0.08));
        t(c, "cleric_aegis", 2, b(DAMAGE_TAKEN, 0.03));
        t(c, "cleric_lingering_light", 3);
        t(c, "cleric_judgment", 3);
        spec(c, "cleric_life", b(HEALING, 0.20));
        spec(c, "cleric_war", b(POWER_DIVINE, 0.15), b(MELEE, 0.04));

        c = RpgClass.PALADIN;
        t(c, "paladin_zeal", 1, b(MELEE, 0.04));
        t(c, "paladin_oath", 1, b(POWER_DIVINE, 0.05));
        t(c, "paladin_holy_plate", 2, b(DAMAGE_TAKEN, 0.03));
        t(c, "paladin_blessing", 2, b(HEALING, 0.06));
        t(c, "paladin_shield_of_faith", 3);
        t(c, "paladin_burning_smite", 3);
        spec(c, "paladin_devotion", b(HEALING, 0.15), b(MAGIC_RESIST, 0.10));
        spec(c, "paladin_vengeance", b(MELEE, 0.10), b(POWER_DIVINE, 0.10));

        c = RpgClass.DRUID;
        t(c, "druid_deep_roots", 1, b(POWER_NATURE, 0.05));
        t(c, "druid_wild_vigor", 1, b(MAX_HEALTH, 2));
        t(c, "druid_cycle_of_life", 2, b(HEALING, 0.06));
        t(c, "druid_harmony", 2, b(REGEN, 0.10));
        t(c, "druid_ancestral_bear", 3);
        t(c, "druid_venom_thorns", 3);
        spec(c, "druid_moon", b(DAMAGE_TAKEN, 0.10), b(MAX_HEALTH, 4));
        spec(c, "druid_land", b(POWER_NATURE, 0.15), b(COST, 0.10));

        c = RpgClass.BARD;
        t(c, "bard_sharp_verse", 1, b(POWER_ARCANE, 0.05));
        t(c, "bard_natural_charm", 1, b(CHA, 1));
        t(c, "bard_repertoire", 2, b(COOLDOWN, 0.05));
        t(c, "bard_restoring_melody", 2, b(HEALING, 0.06));
        t(c, "bard_crescendo", 3);
        t(c, "bard_dissonance", 3, b(SAVE_DC, 1), b(CRIT_CHANCE, 0.05));
        spec(c, "bard_lore", b(POWER_ARCANE, 0.10), b(SAVE_DC, 1));
        spec(c, "bard_valor", b(MELEE, 0.08), b(DAMAGE_TAKEN, 0.05));
    }

    @Nullable
    public static Talent get(String id) {
        return TALENTS.get(id);
    }

    @Nullable
    public static Specialization spec(String id) {
        return id == null ? null : SPECS.get(id);
    }

    public static Map<String, Talent> all() {
        return TALENTS;
    }

    public static List<Talent> forClass(RpgClass c) {
        return TALENTS.values().stream().filter(t -> t.rpgClass() == c).toList();
    }

    public static List<Specialization> specsFor(RpgClass c) {
        return SPECS.values().stream().filter(s -> s.rpgClass() == c).toList();
    }

    public static Map<String, Specialization> allSpecs() {
        return SPECS;
    }

    /** Pontos de talento ganhos até o nível: um por nível par. */
    public static int pointsAt(int level) {
        return level / 2;
    }

    public static int spent(PlayerRpgData d) {
        return d.talents.values().stream().mapToInt(Integer::intValue).sum();
    }

    public static int available(PlayerRpgData d) {
        return pointsAt(d.level) - spent(d);
    }

    private static int spentBelow(PlayerRpgData d, int tier) {
        int n = 0;
        for (var e : d.talents.entrySet()) {
            Talent t = TALENTS.get(e.getKey());
            if (t != null && t.tier() < tier) n += e.getValue();
        }
        return n;
    }

    @Nullable
    public static Failure canSpend(PlayerRpgData d, String id) {
        if (!d.hasCharacter()) return Failure.NO_CHARACTER;
        Talent t = TALENTS.get(id);
        if (t == null) return Failure.UNKNOWN;
        if (t.rpgClass() != d.rpgClass) return Failure.WRONG_CLASS;
        if (d.talents.getOrDefault(id, 0) >= t.maxRank()) return Failure.MAX_RANK;
        if (available(d) <= 0) return Failure.NO_POINTS;
        if (d.level < TIER_LEVEL[t.tier()]) return Failure.TIER_LEVEL;
        if (spentBelow(d, t.tier()) < TIER_POINTS[t.tier()]) return Failure.TIER_POINTS;
        return null;
    }

    public static boolean spend(PlayerRpgData d, String id) {
        if (canSpend(d, id) != null) return false;
        d.talents.merge(id, 1, Integer::sum);
        return true;
    }

    public static boolean chooseSpec(PlayerRpgData d, String id) {
        Specialization s = SPECS.get(id);
        if (!d.hasCharacter() || s == null || s.rpgClass() != d.rpgClass || d.specialization != null || d.level < SPEC_LEVEL) return false;
        d.specialization = id;
        return true;
    }

    /** Soma de um tipo de bônus de talentos, especialização e equipamento. */
    public static double bonus(PlayerRpgData d, BonusType type) {
        double sum = d.gearBonus[type.ordinal()];
        for (var e : d.talents.entrySet()) {
            Talent t = TALENTS.get(e.getKey());
            if (t == null || t.rpgClass() != d.rpgClass) continue;
            for (Talent.Bonus b : t.perRank()) if (b.type() == type) sum += b.amount() * e.getValue();
        }
        Specialization s = spec(d.specialization);
        if (s != null && s.rpgClass() == d.rpgClass) {
            for (Talent.Bonus b : s.bonuses()) if (b.type() == type) sum += b.amount();
        }
        return sum;
    }
}
