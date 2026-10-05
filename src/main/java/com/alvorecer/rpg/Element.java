package com.alvorecer.rpg;

/** Elementos do sopro draconato. Cada um tem um oposto (vulnerabilidade). */
public enum Element {
    FIRE, COLD, LIGHTNING, ACID;

    public Element opposite() {
        return switch (this) {
            case FIRE -> COLD;
            case COLD -> FIRE;
            case LIGHTNING -> ACID;
            case ACID -> LIGHTNING;
        };
    }

    public String langKey() {
        return "element.alvorecer." + name().toLowerCase();
    }
}
