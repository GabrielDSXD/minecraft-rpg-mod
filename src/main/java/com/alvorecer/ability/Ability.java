package com.alvorecer.ability;

import com.alvorecer.rpg.PlayerRpgData;
import com.alvorecer.rpg.Resource;
import com.alvorecer.rpg.School;
import com.alvorecer.rpg.Stat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Definição imutável de uma habilidade. A execução só acontece no servidor, depois de {@link AbilityService} validar
 * personagem, nível, recarga e recurso.
 *
 * @param mobility habilidades de deslocamento (Anões pagam +50%)
 */
public record Ability(String id, Resource resource, int cost, int cooldownTicks, School school, Stat stat, boolean mobility,
                      Executor executor) {

    @FunctionalInterface
    public interface Executor {
        /**
         * @param power multiplicador de poder já calculado (nível, atributo, raça, configuração)
         * @return {@code false} se não havia alvo/condição válida (nada é cobrado)
         */
        boolean run(ServerPlayer player, PlayerRpgData data, ServerLevel level, double power);
    }

    public String nameKey() {
        return "ability.alvorecer." + id;
    }

    public String descKey() {
        return "ability.alvorecer." + id + ".desc";
    }
}
