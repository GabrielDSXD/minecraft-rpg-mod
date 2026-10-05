package com.alvorecer.achievement;

import com.alvorecer.Alvorecer;
import com.alvorecer.registry.ModAttachments;
import com.alvorecer.rpg.PlayerRpgData;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Conquistas do mod. Ficam na ficha (para o Codex) e também concedem a conquista vanilla correspondente
 * (data/alvorecer/advancement/&lt;id&gt;.json, critério "done"), que mostra o aviso na tela e aparece na aba de progressos.
 */
public final class Achievements {
    /** Ordem de exibição no Codex. */
    public static final List<String> ALL = List.of(
            "root", "level_5", "level_10", "level_15", "level_20", "specialization", "capstone", "first_ultimate",
            "profession_10", "profession_20", "biomes_10", "biomes_30", "first_quest", "quests_10", "party", "portal", "aether");

    private Achievements() {
    }

    public static void grant(ServerPlayer p, String id) {
        PlayerRpgData d = ModAttachments.get(p);
        if (!d.achievements.add(id)) return;
        AdvancementHolder adv = p.server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath(Alvorecer.MODID, id));
        if (adv != null) p.getAdvancements().award(adv, "done");
        p.syncData(ModAttachments.RPG);
    }

    /** Conquistas que dependem só da ficha (nível, profissão, biomas); chamado quando esses valores mudam. */
    public static void checkProgress(ServerPlayer p) {
        PlayerRpgData d = ModAttachments.get(p);
        if (!d.hasCharacter()) return;
        grant(p, "root");
        for (int lvl : new int[]{5, 10, 15, 20}) if (d.level >= lvl) grant(p, "level_" + lvl);
        if (d.professionLevel >= 10 || d.secondaryLevel >= 10) grant(p, "profession_10");
        if (d.professionLevel >= 20 || d.secondaryLevel >= 20) grant(p, "profession_20");
        if (d.visitedBiomes.size() >= 10) grant(p, "biomes_10");
        if (d.visitedBiomes.size() >= 30) grant(p, "biomes_30");
        if (d.specialization != null) grant(p, "specialization");
    }
}
