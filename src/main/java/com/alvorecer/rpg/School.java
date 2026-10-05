package com.alvorecer.rpg;

/** Escola de uma habilidade: define o atributo de escala e os modificadores raciais. */
public enum School {
    PHYSICAL, ARCANE, ELEMENTAL, DIVINE, NATURE, SHADOW;

    public boolean magical() {
        return this != PHYSICAL;
    }

    public String langKey() {
        return "school.alvorecer." + name().toLowerCase();
    }
}
