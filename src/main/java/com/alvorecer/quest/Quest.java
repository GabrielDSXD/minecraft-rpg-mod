package com.alvorecer.quest;

import java.util.List;

/**
 * Definição de missão.
 *
 * @param target id de entidade/item/dimensão, ou tag com prefixo "#" (ex.: "#minecraft:undead")
 * @param count  quantidade exigida (ou nível/número de biomas, conforme o tipo)
 */
public record Quest(String id, Category category, int minLevel, Objective objective, String target, int count,
                    long xp, long professionXp, List<RewardItem> items, boolean repeatable) {

    public enum Category { COMBAT, EXPLORATION, GATHERING, PROFESSION, PROGRESSION }

    public enum Objective {
        /** Abater criaturas (crédito compartilhado com o grupo próximo). */
        KILL,
        /** Entregar itens no mural (são consumidos). */
        GATHER,
        /** Alcançar nível de personagem. */
        REACH_LEVEL,
        /** Alcançar nível na profissão principal. */
        PROFESSION_LEVEL,
        /** Descobrir N biomas. */
        BIOMES,
        /** Entrar numa dimensão. */
        VISIT_DIMENSION,
        /** Derrotar campeões de masmorra (crédito para quem está presente). */
        CLEAR_DUNGEON
    }

    public record RewardItem(String id, int count) {
    }

    /** Missões repetíveis ficam disponíveis de novo um dia de jogo após a conclusão. */
    public static final long REPEAT_TICKS = 24000L;

    public String nameKey() {
        return "quest.alvorecer." + id;
    }

    public String descKey() {
        return "quest.alvorecer." + id + ".desc";
    }

    /** Objetivos cujo progresso é guardado (os outros são calculados na hora). */
    public boolean tracked() {
        return objective == Objective.KILL || objective == Objective.VISIT_DIMENSION || objective == Objective.CLEAR_DUNGEON;
    }
}
