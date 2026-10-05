package com.alvorecer.rpg;

/** Os seis atributos clássicos. O índice é usado nos arrays de PlayerRpgData. */
public enum Stat {
    STR("str"), DEX("dex"), CON("con"), INT("int"), WIS("wis"), CHA("cha");

    public final String key;

    Stat(String key) {
        this.key = key;
    }

    public String langKey() {
        return "stat.alvorecer." + key;
    }
}
