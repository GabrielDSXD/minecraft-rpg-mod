package com.alvorecer.client;

import com.alvorecer.network.ModNetwork;
import com.alvorecer.registry.ModAttachments;
import net.minecraft.client.Minecraft;

import java.util.List;

/** Chamado pelos pacotes S2C e pelas teclas; só é carregado no cliente. */
public final class ClientHooks {
    private ClientHooks() {
    }

    public static void openScreen(int screen) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        boolean hasCharacter = ModAttachments.get(mc.player).hasCharacter();
        ModNetwork.Screen s = ModNetwork.Screen.values()[Math.clamp(screen, 0, ModNetwork.Screen.values().length - 1)];
        mc.setScreen(switch (s) {
            case CREATION -> new CharacterCreationScreen();
            case SHEET -> hasCharacter ? new CharacterSheetScreen() : new CharacterCreationScreen();
            case CODEX -> new CodexScreen();
            case QUEST_BOARD -> new QuestBoardScreen();
            case TALENTS -> hasCharacter ? new TalentScreen() : new CharacterCreationScreen();
            case WORKBENCH -> new WorkbenchScreen();
        });
    }

    public static void openNpc(int entityId, int role) {
        var roles = com.alvorecer.entity.npc.NpcEntity.Role.values();
        Minecraft.getInstance().setScreen(new NpcScreen(entityId, roles[Math.clamp(role, 0, roles.length - 1)]));
    }

    /** Membros do grupo recebidos do servidor (vazio = sem grupo). */
    public static List<ModNetwork.PartyMember> partyMembers = List.of();

    public static void party(List<ModNetwork.PartyMember> members) {
        partyMembers = members;
    }
}
