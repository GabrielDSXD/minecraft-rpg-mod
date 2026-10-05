package com.alvorecer.rpg;

/**
 * Raças jogáveis. Bônus de atributo seguem a ordem FOR, DES, CON, INT, SAB, CAR.
 * Os efeitos passivos estão em {@link RaceTraits}; a habilidade ativa em ability/Abilities.
 */
public enum Race {
    HUMAN(new int[]{1, 1, 1, 1, 1, 1}, 1.0, "human_determination"),
    ELF(new int[]{0, 2, 0, 1, 0, 0}, 1.0, "elf_fey_step"),
    DWARF(new int[]{1, 0, 2, 0, 0, 0}, 0.9, "dwarf_stoneskin"),
    HALFLING(new int[]{0, 2, 0, 0, 0, 1}, 0.8, "halfling_luck"),
    ORC(new int[]{2, 0, 1, -1, 0, 0}, 1.05, "orc_fury"),
    DRAGONBORN(new int[]{2, 0, 0, 0, 0, 1}, 1.05, "dragonborn_breath"),
    TIEFLING(new int[]{0, 0, 0, 1, 0, 2}, 1.0, "tiefling_hellblast"),
    GNOME(new int[]{-1, 1, 0, 2, 0, 0}, 0.85, "gnome_illusion");

    private final int[] statBonus;
    public final double scale;
    public final String abilityId;

    Race(int[] statBonus, double scale, String abilityId) {
        this.statBonus = statBonus;
        this.scale = scale;
        this.abilityId = abilityId;
    }

    public int statBonus(Stat stat) {
        return statBonus[stat.ordinal()];
    }

    public String id() {
        return name().toLowerCase();
    }

    public String langKey() {
        return "race.alvorecer." + id();
    }
}
