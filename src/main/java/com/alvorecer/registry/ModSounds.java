package com.alvorecer.registry;

import com.alvorecer.Alvorecer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

/** Sons originais das criaturas e NPCs (arquivos .ogg gerados por tools/gen_sounds.py). */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Alvorecer.MODID);
    private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> ALL = new LinkedHashMap<>();

    public static final String[] CREATURES = {"goblin", "skeleton_warrior", "arcane_wolf", "elemental", "stone_sentinel", "fairy", "wyrmling", "dawn_guardian"};
    public static final String[] SPECIALS = {"goblin.leap", "skeleton_warrior.charge", "skeleton_warrior.block", "arcane_wolf.blink",
            "elemental.shoot", "elemental.pulse", "stone_sentinel.slam", "wyrmling.breath", "npc.greet",
            "dawn_guardian.roar", "dawn_guardian.slam", "dawn_guardian.beam_charge", "dawn_guardian.beam", "dawn_guardian.ring",
            "dawn_guardian.summon", "dawn_guardian.discharge_charge", "dawn_guardian.discharge"};

    static {
        for (String c : CREATURES) {
            for (String kind : new String[]{"ambient", "hurt", "death"}) reg(c + "." + kind);
        }
        for (String s : SPECIALS) reg(s);
    }

    private ModSounds() {
    }

    private static void reg(String name) {
        ALL.put(name, SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Alvorecer.MODID, name))));
    }

    public static SoundEvent get(String name) {
        return ALL.get(name).get();
    }
}
