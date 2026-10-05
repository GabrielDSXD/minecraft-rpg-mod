package com.alvorecer.ability;

import com.alvorecer.config.RpgConfig;
import com.alvorecer.registry.ModAttachments;
import com.alvorecer.registry.ModEffects;
import com.alvorecer.rpg.ArmorWeight;
import com.alvorecer.rpg.Derived;
import com.alvorecer.rpg.PlayerRpgData;
import com.alvorecer.rpg.Race;
import com.alvorecer.rpg.Resource;
import com.alvorecer.rpg.RpgClass;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Validação e execução de habilidades no servidor. O cliente só envia o número do slot; qual habilidade ele representa,
 * se está desbloqueada, a recarga e o custo são decididos aqui.
 */
public final class AbilityService {
    public static final int SLOT_RACE = 0;
    public static final int SLOT_ULTIMATE = 5;
    public static final int SLOT_PROFESSION = 6;
    public static final int SLOT_SECONDARY = 7;
    public static final int SLOT_COUNT = 8;

    public enum Failure {
        NO_CHARACTER, EMPTY_SLOT, LOCKED, COOLDOWN, NO_MANA, NO_ENERGY, STUNNED, BEAR_FORM
    }

    private AbilityService() {
    }

    /** Habilidade associada ao slot (ou nulo). Slots 1-5 são as habilidades de classe em ordem. */
    @Nullable
    public static String abilityId(PlayerRpgData d, int slot) {
        if (!d.hasCharacter()) return null;
        return switch (slot) {
            case SLOT_RACE -> d.race.abilityId;
            case 1, 2, 3, 4, SLOT_ULTIMATE -> d.rpgClass.abilities.get(slot - 1);
            case SLOT_PROFESSION -> d.profession.abilityId;
            case SLOT_SECONDARY -> d.secondaryProfession == null ? null : d.secondaryProfession.abilityId;
            default -> null;
        };
    }

    public static int requiredLevel(int slot) {
        return slot >= 1 && slot <= SLOT_ULTIMATE ? RpgClass.UNLOCK_LEVELS[slot - 1] : 1;
    }

    /** Custo final: base × configuração × Anão em mobilidade (×1,5) × Orc em mana (×1,1) × armadura não proficiente (+10% por peça, só mana). */
    public static float cost(PlayerRpgData d, Ability a, int heavyArmorPieces, double configMultiplier) {
        double c = a.cost() * configMultiplier;
        if (a.mobility() && d.race == Race.DWARF) c *= 1.5;
        if (a.resource() == Resource.MANA) {
            if (d.race == Race.ORC) c *= 1.1;
            c *= 1 + 0.1 * heavyArmorPieces;
        }
        return (float) c;
    }

    public static int cooldown(Ability a, double configMultiplier) {
        return (int) Math.round(a.cooldownTicks() * configMultiplier);
    }

    /** Validação pura (testada em AbilityValidationTest). */
    @Nullable
    public static Failure validate(PlayerRpgData d, int slot, long now, float cost, boolean stunned, boolean bearForm) {
        if (!d.hasCharacter()) return Failure.NO_CHARACTER;
        String id = abilityId(d, slot);
        if (id == null || Abilities.get(id) == null) return Failure.EMPTY_SLOT;
        if (d.level < requiredLevel(slot)) return Failure.LOCKED;
        if (stunned) return Failure.STUNNED;
        if (bearForm && d.rpgClass == RpgClass.DRUID && slot >= 1 && slot <= SLOT_ULTIMATE && !id.equals("druid_bear_form")) {
            return Failure.BEAR_FORM;
        }
        if (d.isOnCooldown(id, now)) return Failure.COOLDOWN;
        Ability a = Abilities.get(id);
        if (a.resource() == Resource.MANA && d.mana < cost) return Failure.NO_MANA;
        if (a.resource() == Resource.ENERGY && d.energy < cost) return Failure.NO_ENERGY;
        return null;
    }

    /** Peças de armadura mais pesadas do que a classe permite. */
    public static int nonProficientArmor(Player p, PlayerRpgData d) {
        if (d.rpgClass == null) return 0;
        int n = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;
            if (ArmorWeight.of(p.getItemBySlot(slot)).ordinal() > d.rpgClass.maxArmor.ordinal()) n++;
        }
        return n;
    }

    public static void use(ServerPlayer p, int slot) {
        PlayerRpgData d = ModAttachments.get(p);
        if (!p.isAlive() || p.isSpectator()) return;
        String id = abilityId(d, slot);
        Ability a = id == null ? null : Abilities.get(id);
        long now = p.level().getGameTime();
        float cost = a == null ? 0 : cost(d, a, nonProficientArmor(p, d), RpgConfig.COST_MULTIPLIER.get());
        Failure f = validate(d, slot, now, cost, p.hasEffect(ModEffects.STUN), p.hasEffect(ModEffects.BEAR_FORM));
        if (f != null) {
            Component msg = switch (f) {
                case LOCKED -> Component.translatable("msg.alvorecer.locked", requiredLevel(slot));
                case COOLDOWN -> Component.translatable("msg.alvorecer.cooldown",
                        Component.translatable(a.nameKey()), String.format("%.1f", d.cooldownRemaining(id, now) / 20.0));
                default -> Component.translatable("msg.alvorecer.fail." + f.name().toLowerCase());
            };
            p.displayClientMessage(msg, true);
            return;
        }
        double power = Derived.power(d, a.stat(), a.school());
        if (!a.executor().run(p, d, p.serverLevel(), power)) return;
        if (a.resource() == Resource.MANA) d.mana -= cost;
        else if (a.resource() == Resource.ENERGY) d.energy -= cost;
        d.cooldowns.put(id, now + cooldown(a, RpgConfig.COOLDOWN_MULTIPLIER.get()));
        d.cooldowns.values().removeIf(end -> end <= now);
        p.syncData(ModAttachments.RPG);
    }
}
