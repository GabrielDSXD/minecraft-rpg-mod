package com.alvorecer.dungeon;

import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Regras puras das masmorras: escala por tamanho do grupo e planta das salas.
 * Quantidade = base + extra × (jogadores − 1); vida/dano × (1 + fator × (jogadores − 1)). Com 1 jogador vale a base (solo viável).
 */
public final class Dungeons {
    /** Distância máxima (em salas) da sala inicial, para a estrutura caber no alcance de referência dos chunks. */
    public static final int MAX_CELL = 3;

    private Dungeons() {
    }

    public static int mobCount(int base, int players, int perExtraPlayer) {
        return base + perExtraPlayer * Math.max(0, players - 1);
    }

    public static double multiplier(int players, double perExtraPlayer) {
        return 1 + perExtraPlayer * Math.max(0, players - 1);
    }

    /**
     * Passeio aleatório em grade: {@code rooms} células distintas, cada uma vizinha da anterior. Null se não couber
     * (raro; tenta de novo algumas vezes).
     */
    @Nullable
    public static List<int[]> layout(RandomSource random, int rooms) {
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int attempt = 0; attempt < 20; attempt++) {
            List<int[]> path = new ArrayList<>();
            path.add(new int[]{0, 0});
            while (path.size() < rooms) {
                int[] last = path.get(path.size() - 1);
                List<int[]> options = new ArrayList<>();
                for (int[] d : dirs) {
                    int[] c = {last[0] + d[0], last[1] + d[1]};
                    if (Math.abs(c[0]) <= MAX_CELL && Math.abs(c[1]) <= MAX_CELL && path.stream().noneMatch(p -> p[0] == c[0] && p[1] == c[1])) {
                        options.add(c);
                    }
                }
                if (options.isEmpty()) break;
                path.add(options.get(random.nextInt(options.size())));
            }
            if (path.size() == rooms) return path;
        }
        return null;
    }
}
