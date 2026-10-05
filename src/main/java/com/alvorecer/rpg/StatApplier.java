package com.alvorecer.rpg;

import com.alvorecer.Alvorecer;
import com.alvorecer.ability.Abilities;
import com.alvorecer.ability.AbilityService;
import com.alvorecer.registry.ModAttachments;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.alvorecer.rpg.talent.BonusType;
import com.alvorecer.rpg.talent.Talents;

/**
 * Converte a ficha em modificadores de atributo vanilla. Os modificadores são permanentes (salvos com o jogador) para
 * que a vida extra sobreviva ao relogin; são sempre recalculados por completo.
 */
public final class StatApplier {
    private StatApplier() {
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(Alvorecer.MODID, "rpg." + name);
    }

    private static void set(ServerPlayer p, Holder<Attribute> attribute, String name, double amount, Operation op) {
        AttributeInstance inst = p.getAttribute(attribute);
        if (inst == null) return;
        ResourceLocation id = id(name);
        inst.removeModifier(id);
        if (amount != 0) inst.addPermanentModifier(new AttributeModifier(id, amount, op));
    }

    public static void apply(ServerPlayer p) {
        com.alvorecer.item.gear.Equipment.refresh(p);
        PlayerRpgData d = ModAttachments.get(p);
        boolean has = d.hasCharacter();
        Race race = has ? d.race : null;

        set(p, Attributes.MAX_HEALTH, "health", has ? Derived.bonusHealth(d) : 0, Operation.ADD_VALUE);
        set(p, Attributes.SCALE, "scale", has ? race.scale - 1 : 0, Operation.ADD_VALUE);
        set(p, Attributes.ATTACK_SPEED, "dex_attack_speed", has ? 0.02 * d.mod(Stat.DEX) : 0, Operation.ADD_MULTIPLIED_BASE);
        set(p, Attributes.LUCK, "luck", has ? 0.25 * d.mod(Stat.CHA) + (race == Race.HALFLING ? 1 : 0) : 0, Operation.ADD_VALUE);

        int heavy = has ? AbilityService.nonProficientArmor(p, d) : 0;
        double speed = (race == Race.DWARF ? -0.08 : 0) - 0.05 * heavy + (has ? Derived.speedBonus(d) : 0);
        set(p, Attributes.MOVEMENT_SPEED, "speed", speed, Operation.ADD_MULTIPLIED_TOTAL);

        set(p, Attributes.ENTITY_INTERACTION_RANGE, "reach", race == Race.HALFLING ? -0.5 : 0, Operation.ADD_VALUE);
        set(p, Attributes.SNEAKING_SPEED, "sneak", race == Race.HALFLING ? 0.15 : 0, Operation.ADD_VALUE);
        set(p, Attributes.JUMP_STRENGTH, "jump", has ? Talents.bonus(d, BonusType.JUMP) : 0, Operation.ADD_MULTIPLIED_BASE);
        set(p, Attributes.SAFE_FALL_DISTANCE, "fall", race == Race.HALFLING || race == Race.ELF ? 1 : 0, Operation.ADD_VALUE);

        double mining = (race == Race.DWARF ? 0.2 : 0) + 0.02 * Abilities.professionLevel(d, Profession.MINER);
        set(p, Attributes.BLOCK_BREAK_SPEED, "mining", has ? mining : 0, Operation.ADD_MULTIPLIED_BASE);

        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
        if (has) {
            d.mana = Math.min(d.mana, Derived.maxMana(d));
            d.energy = Math.min(d.energy, Derived.maxEnergy(d));
        }
    }
}
