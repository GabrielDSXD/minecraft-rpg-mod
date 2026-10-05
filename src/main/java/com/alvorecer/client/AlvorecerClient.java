package com.alvorecer.client;

import com.alvorecer.Alvorecer;
import com.alvorecer.network.ModNetwork;
import com.alvorecer.registry.ModAttachments;
import com.alvorecer.registry.ModEntities;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Ponto de entrada só do cliente: teclas, HUD e renderizadores. */
@Mod(value = Alvorecer.MODID, dist = Dist.CLIENT)
public class AlvorecerClient {
    private static final String CATEGORY = "key.categories.alvorecer";

    /** Teclas dos slots de habilidade, na ordem de AbilityService (raça, classe 1-4, suprema, profissão, secundária). */
    public static final KeyMapping[] ABILITY_KEYS = {
            key("race", GLFW.GLFW_KEY_R),
            key("class1", GLFW.GLFW_KEY_Z),
            key("class2", GLFW.GLFW_KEY_V),
            key("class3", GLFW.GLFW_KEY_B),
            key("class4", GLFW.GLFW_KEY_N),
            key("ultimate", GLFW.GLFW_KEY_G),
            key("profession", GLFW.GLFW_KEY_H),
            key("secondary", GLFW.GLFW_KEY_U),
    };
    public static final KeyMapping SHEET = key("sheet", GLFW.GLFW_KEY_K);
    public static final KeyMapping CODEX = key("codex", GLFW.GLFW_KEY_J);
    public static final KeyMapping TALENTS = key("talents", GLFW.GLFW_KEY_Y);

    private static KeyMapping key(String name, int code) {
        return new KeyMapping("key.alvorecer." + name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, code, CATEGORY);
    }

    public AlvorecerClient(IEventBus modBus) {
        modBus.addListener(AlvorecerClient::registerKeys);
        modBus.addListener(AlvorecerClient::registerLayers);
        modBus.addListener(AlvorecerClient::registerRenderers);
        ClientGear.init(modBus);
        com.alvorecer.client.mob.ClientMobs.init(modBus);
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent e) ->
                e.register(ResourceLocation.fromNamespaceAndPath(Alvorecer.MODID, "aether"), new AetherSky()));
        NeoForge.EVENT_BUS.addListener(AlvorecerClient::onClientTick);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> ClientHooks.party(java.util.List.of()));
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        for (KeyMapping k : ABILITY_KEYS) event.register(k);
        event.register(SHEET);
        event.register(CODEX);
        event.register(TALENTS);
    }

    private static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(Alvorecer.MODID, "hud"), HudOverlay::render);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ILLUSION.get(), IllusionRenderer::new);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        for (int slot = 0; slot < ABILITY_KEYS.length; slot++) {
            while (ABILITY_KEYS[slot].consumeClick()) {
                if (ModAttachments.get(mc.player).hasCharacter()) PacketDistributor.sendToServer(new ModNetwork.UseAbility(slot));
            }
        }
        while (SHEET.consumeClick()) ClientHooks.openScreen(ModNetwork.Screen.SHEET.ordinal());
        while (CODEX.consumeClick()) ClientHooks.openScreen(ModNetwork.Screen.CODEX.ordinal());
        while (TALENTS.consumeClick()) ClientHooks.openScreen(ModNetwork.Screen.TALENTS.ordinal());
    }
}
