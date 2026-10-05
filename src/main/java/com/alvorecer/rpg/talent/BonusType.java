package com.alvorecer.rpg.talent;

import com.alvorecer.rpg.School;
import com.alvorecer.rpg.Stat;

/**
 * Tipos de bônus concedidos por talentos e especializações.
 * {@code percent} indica se o valor é uma fração exibida como porcentagem.
 */
public enum BonusType {
    STR(false), DEX(false), CON(false), INT(false), WIS(false), CHA(false),
    POWER_PHYSICAL(true), POWER_ARCANE(true), POWER_ELEMENTAL(true), POWER_DIVINE(true), POWER_NATURE(true), POWER_SHADOW(true),
    MELEE(true), RANGED(true),
    CRIT_CHANCE(true), CRIT_DAMAGE(false),
    COOLDOWN(true), COST(true),
    MAX_MANA(true), MAX_ENERGY(true), REGEN(true),
    MAX_HEALTH(false), HEALING(true), DAMAGE_TAKEN(true), MAGIC_RESIST(true), SPEED(true),
    SAVE_DC(false), SNEAK_ATTACK(true), JUMP(true);

    public final boolean percent;

    BonusType(boolean percent) {
        this.percent = percent;
    }

    public String langKey() {
        return "bonus.alvorecer." + name().toLowerCase();
    }

    public static BonusType stat(Stat stat) {
        return values()[stat.ordinal()];
    }

    public static BonusType power(School school) {
        return switch (school) {
            case PHYSICAL -> POWER_PHYSICAL;
            case ARCANE -> POWER_ARCANE;
            case ELEMENTAL -> POWER_ELEMENTAL;
            case DIVINE -> POWER_DIVINE;
            case NATURE -> POWER_NATURE;
            case SHADOW -> POWER_SHADOW;
        };
    }
}
