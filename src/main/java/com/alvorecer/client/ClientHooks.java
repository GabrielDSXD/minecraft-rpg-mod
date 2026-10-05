package com.alvorecer.client;

import com.alvorecer.network.ModNetwork;
import com.alvorecer.registry.ModAttachments;
import net.minecraft.client.Minecraft;

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
        });
    }
}
