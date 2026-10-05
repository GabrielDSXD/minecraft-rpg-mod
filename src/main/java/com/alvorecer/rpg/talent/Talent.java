package com.alvorecer.rpg.talent;

import com.alvorecer.rpg.RpgClass;

import java.util.List;

/**
 * Talento de uma árvore de classe. Bônus são por rank; talentos de tier 3 (rank único) costumam ter também um efeito
 * especial implementado no código (verificado com {@code PlayerRpgData.hasTalent}).
 */
public record Talent(String id, RpgClass rpgClass, int tier, int maxRank, List<Bonus> perRank, boolean special) {
    public record Bonus(BonusType type, double amount) {
    }

    public String nameKey() {
        return "talent.alvorecer." + id;
    }

    public String descKey() {
        return "talent.alvorecer." + id + ".desc";
    }
}
