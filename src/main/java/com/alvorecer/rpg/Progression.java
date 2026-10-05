package com.alvorecer.rpg;

/**
 * Fórmulas de progressão, puras (sem estado do jogo) para poderem ser testadas.
 * Documentadas para o jogador no Codex (capítulo "Fórmulas").
 */
public final class Progression {
    public static final int MAX_LEVEL = 20;
    public static final int BASE_SCORE = 10;
    public static final int MAX_SCORE = 24;
    /** Pontos livres concedidos na criação do personagem. */
    public static final int CREATION_POINTS = 3;
    /** Bônus fixo no atributo-chave da classe. */
    public static final int CLASS_KEY_BONUS = 2;
    public static final int MAX_PROFESSION_LEVEL = 20;

    private Progression() {
    }

    /** Modificador estilo D&D: (valor - 10) / 2, arredondado para baixo. */
    public static int modifier(int score) {
        return Math.floorDiv(score - 10, 2);
    }

    /** XP necessário para sair de {@code level} para {@code level + 1}: 50 × nível^1,6. */
    public static long xpToNext(int level) {
        if (level >= MAX_LEVEL) return Long.MAX_VALUE;
        return Math.round(50 * Math.pow(level, 1.6));
    }

    /** Pontos de atributo ganhos ao atingir {@code newLevel}: 1, ou 3 nos níveis 4, 8, 12, 16 e 19. */
    public static int statPointsAt(int newLevel) {
        return switch (newLevel) {
            case 4, 8, 12, 16, 19 -> 3;
            default -> 1;
        };
    }

    /** Bônus de proficiência: +2 no nível 1, +1 a cada 4 níveis (máx. +6). */
    public static int proficiency(int level) {
        return 2 + (Math.max(1, level) - 1) / 4;
    }

    /** CD de resistência das habilidades: 8 + proficiência + modificador do atributo. */
    public static int saveDc(int level, int statScore) {
        return 8 + proficiency(level) + modifier(statScore);
    }

    /** Multiplicador de poder das habilidades: (1 + 4% por nível acima do 1º) × (1 + 6% por ponto de modificador), mínimo ×0,5 no fator do atributo. */
    public static double powerMultiplier(int level, int statScore) {
        return (1 + 0.04 * (level - 1)) * Math.max(0.5, 1 + 0.06 * modifier(statScore));
    }

    /** XP de profissão para o próximo nível: 20 × nível^1,5. */
    public static long professionXpToNext(int level) {
        if (level >= MAX_PROFESSION_LEVEL) return Long.MAX_VALUE;
        return Math.round(20 * Math.pow(level, 1.5));
    }

    /** XP de classe por abater um monstro: 1,5 × vida máxima + 2 × armadura, mínimo 1. */
    public static long killXp(float maxHealth, int armor) {
        return Math.max(1, Math.round(1.5 * maxHealth + 2 * armor));
    }
}
