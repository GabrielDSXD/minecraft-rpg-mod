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
import net.minecraft.world.item.ItemStack;
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
        return (float) (c * (1 - Derived.costReduction(d)));
    }

    /** Recarga final: base × servidor × (1 − redução de talentos). */
    public static int cooldown(PlayerRpgData d, Ability a, double configMultiplier) {
        return (int) Math.round(a.cooldownTicks() * configMultiplier * (1 - Derived.cooldownReduction(d)));
    }

    /** Validação pura (testada em AbilityValidationTest). */
    @Nullable
    public static Failure validate(PlayerRpgData d, int slot, long now, float cost, boolean stunned, boolean bearForm) {
        if (!d.hasCharacter()) return Failure.NO_CHARACTER;
        String id = abilityId(d, slot);
        if (id == null || Abilities.get(id) == null) return Failure.EMPTY_SLOT;
        if (d.level < requiredLevel(slot)) return Failure.LOCKED;
        if (bearForm && d.rpgClass == RpgClass.DRUID && slot >= 1 && slot <= SLOT_ULTIMATE && !id.equals("druid_bear_form")) {
            return Failure.BEAR_FORM;
        }
        return validateAbility(d, id, now, cost, stunned);
    }

    /** Checagens comuns a slots e itens: atordoamento, recarga e recurso. */
    @Nullable
    public static Failure validateAbility(PlayerRpgData d, String id, long now, float cost, boolean stunned) {
        if (!d.hasCharacter()) return Failure.NO_CHARACTER;
        Ability a = Abilities.get(id);
        if (a == null) return Failure.EMPTY_SLOT;
        if (stunned) return Failure.STUNNED;
        if (d.isOnCooldown(id, now)) return Failure.COOLDOWN;
        if (a.resource() == Resource.MANA && d.mana < cost) return Failure.NO_MANA;
        if (a.resource() == Resource.ENERGY && d.energy < cost) return Failure.NO_ENERGY;
        return null;
    }

    private static void report(ServerPlayer p, PlayerRpgData d, Failure f, @Nullable Ability a, String id, long now, int requiredLevel) {
        Component msg = switch (f) {
            case LOCKED -> Component.translatable("msg.alvorecer.locked", requiredLevel);
            case COOLDOWN -> Component.translatable("msg.alvorecer.cooldown",
                    Component.translatable(a.nameKey()), String.format("%.1f", d.cooldownRemaining(id, now) / 20.0));
            default -> Component.translatable("msg.alvorecer.fail." + f.name().toLowerCase());
        };
        p.displayClientMessage(msg, true);
    }

    /** Executa a habilidade já validada: cobra o recurso, inicia a recarga e sincroniza. Retorna false se não havia alvo. */
    private static boolean execute(ServerPlayer p, PlayerRpgData d, Ability a, String id, float cost, long now) {
        double power = Derived.power(d, a.stat(), a.school());
        if (!a.executor().run(p, d, p.serverLevel(), power)) return false;
        if (a.resource() == Resource.MANA) d.mana -= cost;
        else if (a.resource() == Resource.ENERGY) d.energy -= cost;
        d.cooldowns.put(id, now + cooldown(d, a, RpgConfig.COOLDOWN_MULTIPLIER.get()));
        d.cooldowns.values().removeIf(end -> end <= now);
        p.syncData(ModAttachments.RPG);
        return true;
    }

    /** Habilidade de item (cajado, varinha, arma especial). O item precisa estar na mão e o nível exigido ser atingido. */
    public static boolean useItemAbility(ServerPlayer p, ItemStack stack, String id) {
        PlayerRpgData d = ModAttachments.get(p);
        Ability a = Abilities.get(id);
        if (a == null || !p.isAlive() || p.isSpectator()) return false;
        com.alvorecer.item.gear.GearInfo gear = com.alvorecer.item.gear.GearInfo.get(stack.getItem());
        if (gear != null && d.hasCharacter() && d.level < gear.level()) {
            p.displayClientMessage(Component.translatable("msg.alvorecer.gear_level", gear.level()), true);
            return false;
        }
        long now = p.level().getGameTime();
        float cost = cost(d, a, nonProficientArmor(p, d), RpgConfig.COST_MULTIPLIER.get());
        Failure f = validateAbility(d, id, now, cost, p.hasEffect(ModEffects.STUN));
        if (f != null) {
            report(p, d, f, a, id, now, 0);
            return false;
        }
        if (!execute(p, d, a, id, cost, now)) return false;
        p.getCooldowns().addCooldown(stack.getItem(), cooldown(d, a, RpgConfig.COOLDOWN_MULTIPLIER.get()));
        return true;
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
            report(p, d, f, a, id, now, requiredLevel(slot));
            return;
        }
        if (execute(p, d, a, id, cost, now) && slot == SLOT_ULTIMATE) com.alvorecer.achievement.Achievements.grant(p, "first_ultimate");
    }
}
