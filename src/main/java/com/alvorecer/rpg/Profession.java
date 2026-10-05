package com.alvorecer.rpg;

public enum Profession {
    BLACKSMITH("blacksmith_field_repair"),
    ALCHEMIST("alchemist_improvised_elixir"),
    ENCHANTER("enchanter_arcane_channel"),
    HERBALIST("herbalist_bloom"),
    MINER("miner_ore_sense"),
    HUNTER("hunter_track"),
    COOK("cook_feast"),
    ARTISAN("artisan_portable_bench");

    public final String abilityId;

    Profession(String abilityId) {
        this.abilityId = abilityId;
    }

    public String id() {
        return name().toLowerCase();
    }

    public String langKey() {
        return "profession.alvorecer." + id();
    }
}
