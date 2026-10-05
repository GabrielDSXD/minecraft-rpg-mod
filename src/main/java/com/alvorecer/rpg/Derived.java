package com.alvorecer.rpg;

import com.alvorecer.rpg.talent.BonusType;
import com.alvorecer.rpg.talent.Talents;

/**
 * Valores derivados da ficha (vida extra, mana, energia, crítico, multiplicadores), já somando talentos e
 * especialização. Puro: depende só de {@link PlayerRpgData}. As fórmulas aparecem no Codex.
 */
public final class Derived {
    private Derived() {
    }

    private static double t(PlayerRpgData d, BonusType type) {
        return Talents.bonus(d, type);
    }

    /** Vida máxima adicional: (nível-1) × dado de vida da classe + 2 × mod CON + ajuste racial + talentos. */
    public static double bonusHealth(PlayerRpgData d) {
        double hp = 2.0 * d.mod(Stat.CON) + t(d, BonusType.MAX_HEALTH);
        if (d.rpgClass != null) hp += (d.level - 1) * d.rpgClass.hitDie;
        if (d.race == Race.ELF) hp -= 4;
        return hp;
    }

    public static float maxMana(PlayerRpgData d) {
        boolean caster = d.rpgClass != null && d.rpgClass.resource == Resource.MANA;
        Stat casting = caster ? d.rpgClass.keyStat : Stat.INT;
        double mana = (caster ? 100 : 40) + (caster ? 4 : 2) * (d.level - 1) + 10 * d.mod(casting);
        mana *= raceManaFactor(d.race) * (1 + 0.01 * d.levelOf(Profession.ENCHANTER)) * (1 + t(d, BonusType.MAX_MANA));
        return (float) Math.max(10, mana);
    }

    public static double raceManaFactor(Race race) {
        if (race == null) return 1;
        return switch (race) {
            case HUMAN -> 0.9;
            case ORC -> 0.85;
            case ELF, GNOME -> 1.1;
            default -> 1;
        };
    }

    public static float maxEnergy(PlayerRpgData d) {
        boolean martial = d.rpgClass != null && d.rpgClass.resource == Resource.ENERGY;
        double energy = (martial ? 100 : 60) + 2 * (d.level - 1) + 5 * d.mod(Stat.CON);
        energy *= 1 + t(d, BonusType.MAX_ENERGY);
        return (float) Math.max(20, energy);
    }

    /** Mana por segundo: 1,5% da mana máxima × (1 + 10% por mod SAB) × talentos, mínimo 0,5. */
    public static float manaRegen(PlayerRpgData d) {
        return (float) Math.max(0.5, maxMana(d) * 0.015 * (1 + 0.1 * d.mod(Stat.WIS)) * (1 + t(d, BonusType.REGEN)));
    }

    /** Energia por segundo: 5 (Ladino 7), Draconato ×0,75, × talentos. */
    public static float energyRegen(PlayerRpgData d) {
        double e = d.rpgClass == RpgClass.ROGUE ? 7 : 5;
        if (d.race == Race.DRAGONBORN) e *= 0.75;
        return (float) (e * (1 + t(d, BonusType.REGEN)));
    }

    /** Chance de crítico: 5% + 1% por mod DES (+5% Ladino, +3% Halfling) + talentos, entre 0% e 60%. */
    public static double critChance(PlayerRpgData d) {
        double c = 0.05 + 0.01 * d.mod(Stat.DEX) + t(d, BonusType.CRIT_CHANCE);
        if (d.rpgClass == RpgClass.ROGUE) c += 0.05;
        if (d.race == Race.HALFLING) c += 0.03;
        return Math.clamp(c, 0, 0.6);
    }

    public static double critMultiplier(PlayerRpgData d) {
        return (d.rpgClass == RpgClass.ROGUE ? 1.75 : 1.5) + t(d, BonusType.CRIT_DAMAGE);
    }

    /** Dano corpo a corpo físico: +4% por mod FOR + talentos. */
    public static double meleeMultiplier(PlayerRpgData d) {
        return Math.max(0.5, 1 + 0.04 * d.mod(Stat.STR) + t(d, BonusType.MELEE));
    }

    /** Dano físico à distância: +4% por mod DES + talentos. */
    public static double rangedMultiplier(PlayerRpgData d) {
        return Math.max(0.5, 1 + 0.04 * d.mod(Stat.DEX) + t(d, BonusType.RANGED));
    }

    /** Redução de dano mágico recebido: 1,5% por mod SAB + talentos (máx. 45%). */
    public static double magicResistance(PlayerRpgData d) {
        return Math.clamp(0.015 * d.mod(Stat.WIS) + t(d, BonusType.MAGIC_RESIST), -0.15, 0.45);
    }

    /** Redução geral de dano recebida por talentos/especialização (máx. 40%). */
    public static double damageReduction(PlayerRpgData d) {
        return Math.clamp(t(d, BonusType.DAMAGE_TAKEN), 0, 0.4);
    }

    /** Bônus de cura feita (talentos). */
    public static double healingMultiplier(PlayerRpgData d) {
        return 1 + t(d, BonusType.HEALING);
    }

    /** Multiplicador de ataque furtivo do Ladino: ×1,35 + especialização. */
    public static double sneakMultiplier(PlayerRpgData d) {
        return 1.35 + t(d, BonusType.SNEAK_ATTACK);
    }

    /** Fração de redução de recarga (máx. 50%) e de custo (máx. 60%). */
    public static double cooldownReduction(PlayerRpgData d) {
        return Math.clamp(t(d, BonusType.COOLDOWN), 0, 0.5);
    }

    public static double costReduction(PlayerRpgData d) {
        return Math.clamp(t(d, BonusType.COST), 0, 0.6);
    }

    public static double speedBonus(PlayerRpgData d) {
        return t(d, BonusType.SPEED);
    }

    /** Modificador racial por escola de magia. */
    public static double raceSchoolFactor(Race race, School school) {
        if (race == null || !school.magical()) return 1;
        return switch (race) {
            case ORC -> 0.85;
            case TIEFLING -> school == School.SHADOW ? 1.15 : 1;
            case ELF, GNOME -> school == School.ARCANE || school == School.ELEMENTAL ? 1.05 : 1;
            default -> 1;
        };
    }

    /** Multiplicador final de uma habilidade: progressão × escola racial × talentos da escola. */
    public static double power(PlayerRpgData d, Stat stat, School school) {
        return Progression.powerMultiplier(d.level, d.score(stat)) * raceSchoolFactor(d.race, school)
                * (1 + t(d, BonusType.power(school)));
    }

    /** CD das habilidades: 8 + proficiência + mod do atributo + talentos. */
    public static int saveDc(PlayerRpgData d, Stat stat) {
        return Progression.saveDc(d.level, d.score(stat)) + (int) t(d, BonusType.SAVE_DC);
    }

    public static int saveBonus(PlayerRpgData d, Stat stat) {
        int bonus = d.mod(stat);
        if (d.rpgClass != null && d.rpgClass.proficientSave(stat)) bonus += Progression.proficiency(d.level);
        return bonus;
    }

    /** Desconto com mercadores: 2% por mod CAR, entre -10% e 20%. */
    public static double tradeDiscount(PlayerRpgData d) {
        return Math.clamp(0.02 * d.mod(Stat.CHA), -0.1, 0.2);
    }
}
