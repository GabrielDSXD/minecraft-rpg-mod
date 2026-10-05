package com.alvorecer;

import com.alvorecer.command.PartyCommands;
import com.alvorecer.command.RpgCommands;
import com.alvorecer.registry.ModBlocks;
import com.alvorecer.registry.ModGear;
import com.alvorecer.config.RpgConfig;
import com.alvorecer.event.CombatEvents;
import com.alvorecer.event.PlayerEvents;
import com.alvorecer.event.ProfessionEvents;
import com.alvorecer.network.ModNetwork;
import com.alvorecer.registry.ModAttachments;
import com.alvorecer.registry.ModEffects;
import com.alvorecer.registry.ModEntities;
import com.alvorecer.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Alvorecer.MODID)
public class Alvorecer {
    public static final String MODID = "alvorecer";

    public Alvorecer(IEventBus modBus, ModContainer container) {
        ModAttachments.ATTACHMENTS.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModGear.init();
        com.alvorecer.registry.ModMobs.init();
        com.alvorecer.registry.ModSounds.SOUNDS.register(modBus);
        modBus.addListener(com.alvorecer.registry.ModMobs::attributes);
        modBus.addListener(com.alvorecer.registry.ModMobs::spawnPlacements);
        ModGear.ARMOR_MATERIALS.register(modBus);
        ModGear.COMPONENTS.register(modBus);
        ModItems.TABS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        modBus.addListener(ModEntities::attributes);
        modBus.addListener(ModNetwork::register);
        container.registerConfig(ModConfig.Type.SERVER, RpgConfig.SPEC);

        NeoForge.EVENT_BUS.register(CombatEvents.class);
        NeoForge.EVENT_BUS.register(PlayerEvents.class);
        NeoForge.EVENT_BUS.register(ProfessionEvents.class);
        NeoForge.EVENT_BUS.register(com.alvorecer.event.GearEvents.class);
        NeoForge.EVENT_BUS.addListener(RpgCommands::register);
        NeoForge.EVENT_BUS.addListener(PartyCommands::register);
    }
}
