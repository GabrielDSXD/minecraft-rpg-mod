package com.alvorecer.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Configuração de servidor (por mundo, sincronizada com os clientes): balanceamento e regras administrativas. */
public final class RpgConfig {
    public enum RespecMode {
        /** Só administradores, via /rpg reset. */
        ADMIN_ONLY,
        /** Jogadores usam o Tomo do Recomeço (ou o comando de admin). */
        ITEM,
        /** Jogadores podem usar /rpg respec livremente (respeitando a recarga). */
        FREE
    }

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue XP_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue SPAWNER_XP_MULTIPLIER;
    public static final ModConfigSpec.IntValue EXPLORATION_XP;
    public static final ModConfigSpec.DoubleValue PROFESSION_XP_MULTIPLIER;
    public static final ModConfigSpec.IntValue PROFESSION_DAILY_CAP;
    public static final ModConfigSpec.IntValue SECONDARY_PROFESSION_LEVEL;
    public static final ModConfigSpec.DoubleValue SECONDARY_PROFESSION_XP_FACTOR;

    public static final ModConfigSpec.DoubleValue COST_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue COOLDOWN_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue ABILITY_DAMAGE_MULTIPLIER;

    public static final ModConfigSpec.BooleanValue PVP_ABILITIES;
    public static final ModConfigSpec.DoubleValue PVP_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue PVP_CONTROL_EFFECTS;

    public static final ModConfigSpec.IntValue EFFECT_SAVE_DC;
    public static final ModConfigSpec.BooleanValue PLAYER_SAVING_THROWS;

    public static final ModConfigSpec.IntValue PARTY_MAX_SIZE;
    public static final ModConfigSpec.BooleanValue PARTY_SHARE_XP;
    public static final ModConfigSpec.DoubleValue PARTY_XP_BONUS;
    public static final ModConfigSpec.IntValue PARTY_RANGE;
    public static final ModConfigSpec.BooleanValue PARTY_FRIENDLY_FIRE;

    public static final ModConfigSpec.IntValue MAX_ACTIVE_QUESTS;
    public static final ModConfigSpec.DoubleValue CREATURE_SPAWN_RATE;
    private static final java.util.Map<String, ModConfigSpec.DoubleValue> STRUCTURES = new java.util.LinkedHashMap<>();

    public static final ModConfigSpec.IntValue DUNGEON_MOBS_PER_PLAYER;
    public static final ModConfigSpec.DoubleValue DUNGEON_HEALTH_PER_PLAYER;
    public static final ModConfigSpec.DoubleValue DUNGEON_DAMAGE_PER_PLAYER;
    public static final ModConfigSpec.IntValue DUNGEON_CLEAR_XP;
    public static final ModConfigSpec.IntValue DUNGEON_CHAMPION_XP;

    public static final ModConfigSpec.EnumValue<RespecMode> RESPEC_MODE;
    public static final ModConfigSpec.IntValue RESPEC_COOLDOWN_MINUTES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("Progressão de classe e profissão").push("progression");
        XP_MULTIPLIER = b.comment("Multiplicador global de XP de classe.").defineInRange("xpMultiplier", 1.0, 0.0, 100.0);
        SPAWNER_XP_MULTIPLIER = b.comment("Multiplicador de XP para criaturas nascidas de geradores (anti-farm).").defineInRange("spawnerXpMultiplier", 0.25, 0.0, 1.0);
        EXPLORATION_XP = b.comment("XP concedido na primeira visita a cada bioma.").defineInRange("explorationXp", 30, 0, 10000);
        PROFESSION_XP_MULTIPLIER = b.comment("Multiplicador global de XP de profissão.").defineInRange("professionXpMultiplier", 1.0, 0.0, 100.0);
        PROFESSION_DAILY_CAP = b.comment("XP máximo de profissão por dia de jogo (24000 ticks). 0 = sem limite.").defineInRange("professionDailyCap", 1500, 0, 1_000_000);
        SECONDARY_PROFESSION_LEVEL = b.comment("Nível de personagem que libera a profissão secundária.").defineInRange("secondaryProfessionLevel", 10, 1, 20);
        SECONDARY_PROFESSION_XP_FACTOR = b.comment("Fração de XP que a profissão secundária recebe.").defineInRange("secondaryProfessionXpFactor", 0.5, 0.0, 1.0);
        b.pop();

        b.comment("Habilidades").push("abilities");
        COST_MULTIPLIER = b.comment("Multiplicador global de custo de mana/energia.").defineInRange("costMultiplier", 1.0, 0.0, 10.0);
        COOLDOWN_MULTIPLIER = b.comment("Multiplicador global de recarga.").defineInRange("cooldownMultiplier", 1.0, 0.0, 10.0);
        ABILITY_DAMAGE_MULTIPLIER = b.comment("Multiplicador global de dano/cura de habilidades.").defineInRange("abilityDamageMultiplier", 1.0, 0.0, 10.0);
        b.pop();

        b.comment("Regras de PvP para habilidades (o PvP do servidor e as equipes vanilla também são respeitados)").push("pvp");
        PVP_ABILITIES = b.comment("Habilidades podem causar dano e efeitos negativos em outros jogadores.").define("abilitiesAffectPlayers", true);
        PVP_DAMAGE_MULTIPLIER = b.comment("Multiplicador de dano de habilidades contra jogadores.").defineInRange("pvpDamageMultiplier", 0.6, 0.0, 10.0);
        PVP_CONTROL_EFFECTS = b.comment("Efeitos de controle (atordoar, enraizar, encantar) funcionam contra jogadores.").define("controlEffectsOnPlayers", true);
        b.pop();

        b.comment("Testes de resistência").push("saves");
        PLAYER_SAVING_THROWS = b.comment("Jogadores fazem teste de resistência contra efeitos negativos de outras fontes.").define("playerSavingThrows", true);
        EFFECT_SAVE_DC = b.comment("CD dos testes de resistência contra efeitos negativos ambientais/de criaturas.").defineInRange("effectSaveDc", 15, 1, 40);
        b.pop();

        b.comment("Grupos de jogadores").push("party");
        PARTY_MAX_SIZE = b.comment("Tamanho máximo de um grupo.").defineInRange("maxSize", 5, 2, 20);
        PARTY_SHARE_XP = b.comment("XP de abates é dividido entre os membros próximos.").define("shareXp", true);
        PARTY_XP_BONUS = b.comment("Bônus de XP por membro extra que participa da divisão (0,1 = +10%).").defineInRange("xpBonusPerMember", 0.1, 0.0, 1.0);
        PARTY_RANGE = b.comment("Distância (blocos) para receber XP e crédito de missão do grupo.").defineInRange("range", 48, 8, 256);
        PARTY_FRIENDLY_FIRE = b.comment("Membros do mesmo grupo podem ferir uns aos outros.").define("friendlyFire", false);
        b.pop();

        b.comment("Criaturas do mod").push("creatures");
        CREATURE_SPAWN_RATE = b.comment("Chance (0 a 1) de cada tentativa de nascimento natural das criaturas do mod ser aceita. 0 desativa.")
                .defineInRange("spawnRate", 1.0, 0.0, 1.0);
        b.pop();

        b.comment("Frequência das estruturas: chance (0 a 1) de cada ponto candidato gerar a estrutura. 0 desativa.").push("worldgen");
        for (String[] s : new String[][]{{"camp", "Acampamentos goblins (mundo principal)"}, {"tower", "Torres de vigia (mundo principal)"},
                {"shrine", "Santuários do Alvorecer (mundo principal)"}, {"dungeon", "Entradas de masmorras (mundo principal e Éter)"},
                {"ruins", "Ruínas Etéreas (Reinos do Éter)"}, {"arena", "Arenas Celestes (Reinos do Éter)"}}) {
            STRUCTURES.put(s[0], b.comment(s[1]).defineInRange(s[0], 1.0, 0.0, 1.0));
        }
        b.pop();

        b.comment("Masmorras: escala pelo número de jogadores a até 24 blocos do selo quando o encontro começa").push("dungeons");
        DUNGEON_MOBS_PER_PLAYER = b.comment("Criaturas a mais por jogador além do primeiro, em cada onda.").defineInRange("mobsPerExtraPlayer", 2, 0, 20);
        DUNGEON_HEALTH_PER_PLAYER = b.comment("Vida extra das criaturas por jogador além do primeiro (0,35 = +35%).").defineInRange("healthPerExtraPlayer", 0.35, 0.0, 5.0);
        DUNGEON_DAMAGE_PER_PLAYER = b.comment("Dano extra das criaturas por jogador além do primeiro.").defineInRange("damagePerExtraPlayer", 0.15, 0.0, 5.0);
        DUNGEON_CLEAR_XP = b.comment("XP de classe para cada jogador presente ao vencer uma sala de combate (enigma: metade).").defineInRange("roomClearXp", 80, 0, 100000);
        DUNGEON_CHAMPION_XP = b.comment("XP de classe para cada jogador presente ao derrotar o campeão.").defineInRange("championXp", 400, 0, 100000);
        b.pop();

        b.comment("Missões").push("quests");
        MAX_ACTIVE_QUESTS = b.comment("Máximo de missões ativas por jogador.").defineInRange("maxActive", 5, 1, 50);
        b.pop();

        b.comment("Troca de raça/classe/profissão").push("respec");
        RESPEC_MODE = b.comment("ADMIN_ONLY, ITEM ou FREE.").defineEnum("mode", RespecMode.ITEM);
        RESPEC_COOLDOWN_MINUTES = b.comment("Tempo mínimo (minutos de jogo) entre trocas feitas pelo jogador.").defineInRange("cooldownMinutes", 30, 0, 100000);
        b.pop();

        SPEC = b.build();
    }

    private RpgConfig() {
    }

    /** Frequência configurada de um tipo de estrutura (1 se desconhecido ou se a configuração ainda não carregou). */
    public static double structureFrequency(String kind) {
        ModConfigSpec.DoubleValue v = STRUCTURES.get(kind);
        if (v == null) return 1;
        try {
            return v.get();
        } catch (IllegalStateException e) {
            return 1;
        }
    }
}
