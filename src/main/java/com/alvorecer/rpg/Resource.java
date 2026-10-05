package com.alvorecer.rpg;

public enum Resource {
    NONE, MANA, ENERGY;

    public String langKey() {
        return "resource.alvorecer." + name().toLowerCase();
    }
}
