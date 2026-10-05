package com.alvorecer.rpg;

/**
 * Valores derivados da ficha (vida extra, mana, energia, crítico, multiplicadores).
 * Puro: depende só de {@link PlayerRpgData}. As fórmulas aparecem no Codex.
 */
public final class Derived {
    private Derived() {
    }

    /** Vida máxima adicional: (nível-1) × dado de vida da classe + 2 × mod CON + ajuste racial. */
    public static double bonusHealth(PlayerRpgData d) {
        double hp = 2.0 * d.mod(Stat.CON);
        if (d.rpgClass != null) hp += (d.level - 1) * d.rpgClass.hitDie;
        if (d.race == Race.ELF) hp -= 4;
        return hp;
    }

    public static float maxMana(PlayerRpgData d) {
        boolean caster = d.rpgClass != null && d.rpgClass.resource == Resource.MANA;
        Stat casting = caster ? d.rpgClass.keyStat : Stat.INT;
        double mana = (caster ? 100 : 40) + (caster ? 4 : 2) * (d.level - 1) + 10 * d.mod(casting);
        mana *= raceManaFactor(d.race) * (1 + 0.01 * d.levelOf(Profession.ENCHANTER));
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
        return (float) Math.max(20, energy);
    }

    /** Mana por segundo: 1,5% da mana máxima × (1 + 10% por mod SAB), mínimo 0,5. */
    public static float manaRegen(PlayerRpgData d) {
        return (float) Math.max(0.5, maxMana(d) * 0.015 * (1 + 0.1 * d.mod(Stat.WIS)));
    }

    /** Energia por segundo: 5 (Ladino 7), Draconato ×0,75. */
    public static float energyRegen(PlayerRpgData d) {
        double e = d.rpgClass == RpgClass.ROGUE ? 7 : 5;
        if (d.race == Race.DRAGONBORN) e *= 0.75;
        return (float) e;
    }

    /** Chance de crítico: 5% + 1% por mod DES (+5% Ladino, +3% Halfling), entre 0% e 50%. */
    public static double critChance(PlayerRpgData d) {
        double c = 0.05 + 0.01 * d.mod(Stat.DEX);
        if (d.rpgClass == RpgClass.ROGUE) c += 0.05;
        if (d.race == Race.HALFLING) c += 0.03;
        return Math.clamp(c, 0, 0.5);
    }

    public static double critMultiplier(PlayerRpgData d) {
        return d.rpgClass == RpgClass.ROGUE ? 1.75 : 1.5;
    }

    /** Dano corpo a corpo físico: +4% por mod FOR. */
    public static double meleeMultiplier(PlayerRpgData d) {
        return Math.max(0.5, 1 + 0.04 * d.mod(Stat.STR));
    }

    /** Dano físico à distância: +4% por mod DES. */
    public static double rangedMultiplier(PlayerRpgData d) {
        return Math.max(0.5, 1 + 0.04 * d.mod(Stat.DEX));
    }

    /** Redução de dano mágico recebido: 1,5% por mod SAB (máx. 30%). */
    public static double magicResistance(PlayerRpgData d) {
        return Math.clamp(0.015 * d.mod(Stat.WIS), -0.15, 0.3);
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

    /** Multiplicador final de uma habilidade: progressão × escola racial. */
    public static double power(PlayerRpgData d, Stat stat, School school) {
        return Progression.powerMultiplier(d.level, d.score(stat)) * raceSchoolFactor(d.race, school);
    }

    public static int saveBonus(PlayerRpgData d, Stat stat) {
        int bonus = d.mod(stat);
        if (d.rpgClass != null && d.rpgClass.proficientSave(stat)) bonus += Progression.proficiency(d.level);
        return bonus;
    }

    /** Desconto com mercadores (usado pelos NPCs): 2% por mod CAR, entre -10% e 20%. */
    public static double tradeDiscount(PlayerRpgData d) {
        return Math.clamp(0.02 * d.mod(Stat.CHA), -0.1, 0.2);
    }
}
