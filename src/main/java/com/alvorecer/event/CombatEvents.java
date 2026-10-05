package com.alvorecer.event;

import com.alvorecer.registry.ModAttachments;
import com.alvorecer.registry.ModDamageTypes;
import com.alvorecer.registry.ModEffects;
import com.alvorecer.rpg.Derived;
import com.alvorecer.rpg.Element;
import com.alvorecer.rpg.PlayerRpgData;
import com.alvorecer.rpg.Race;
import com.alvorecer.rpg.RpgClass;
import com.alvorecer.rpg.WeaponCategory;
import com.alvorecer.ability.Abilities;
import com.alvorecer.config.RpgConfig;
import com.alvorecer.rpg.Stat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import com.alvorecer.party.Parties;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Fórmulas de combate (ver Codex, capítulo Fórmulas):
 * atacante → FOR/DES, proficiência, armas pesadas, fúrias, ataque furtivo, marca do caçador, crítico;
 * defensor → evasão, elementos raciais, veneno, resistência mágica (SAB), Pele de Pedra, fúrias.
 */
public final class CombatEvents {
    private CombatEvents() {
    }

    private static boolean disabled(LivingEntity e) {
        return e.hasEffect(ModEffects.STUN) || e.hasEffect(ModEffects.CHARM);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return;
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        float amount = event.getAmount();

        if (attacker instanceof LivingEntity la && la != target && disabled(la) && direct == la) {
            event.setCanceled(true);
            return;
        }
        if (attacker instanceof ServerPlayer ap && target instanceof ServerPlayer tp0 && ap != tp0
                && !RpgConfig.PARTY_FRIENDLY_FIRE.get() && Parties.sameParty(ap, tp0)) {
            event.setCanceled(true);
            return;
        }

        if (attacker instanceof ServerPlayer p && p != target) {
            amount = attackerModifiers(p, target, source, direct, amount);
        }

        if (target instanceof ServerPlayer tp) {
            PlayerRpgData d = ModAttachments.get(tp);
            if (d.hasCharacter()) {
                if (tp.hasEffect(ModEffects.EVASION) && attacker != null && attacker != tp
                        && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                    tp.removeEffect(ModEffects.EVASION);
                    ((ServerLevel) tp.level()).sendParticles(ParticleTypes.CLOUD, tp.getX(), tp.getY() + 1, tp.getZ(), 15, 0.4, 0.5, 0.4, 0.02);
                    tp.level().playSound(null, tp.blockPosition(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 1f, 1.4f);
                    tp.displayClientMessage(Component.translatable("msg.alvorecer.evaded"), true);
                    event.setCanceled(true);
                    return;
                }
                amount = defenderModifiers(tp, d, source, amount);
            }
        }
        event.setAmount(Math.max(0, amount));
    }

    static float attackerModifiers(ServerPlayer p, LivingEntity target, DamageSource source, Entity direct, float amount) {
        PlayerRpgData d = ModAttachments.get(p);
        if (!d.hasCharacter()) return amount;
        boolean melee = direct == p && source.is(DamageTypes.PLAYER_ATTACK);
        boolean ranged = direct instanceof Projectile;
        double m = 1;
        ItemStack weapon = p.getMainHandItem();
        WeaponCategory cat = WeaponCategory.of(weapon);

        if (melee) {
            m *= Derived.meleeMultiplier(d);
            if (!d.rpgClass.proficient(cat)) m *= 0.75;
            if (cat.heavy()) {
                if (d.race == Race.ORC) m *= 1.15;
                if (d.race == Race.HALFLING) m *= 0.75;
                if (d.race == Race.GNOME) m *= 0.8;
            }
            if (p.hasEffect(ModEffects.ORC_FURY)) m *= 1.3;
            if (p.hasEffect(ModEffects.BARBARIAN_RAGE)) m *= 1.2;
            if (d.rpgClass == RpgClass.ROGUE && target instanceof Mob mob && mob.getTarget() != p) m *= Derived.sneakMultiplier(d);
            if (d.hasTalent("rogue_assassinate") && target.getHealth() >= target.getMaxHealth()) m *= 1.5;
            if (p.hasEffect(ModEffects.POISON_BLADES)) target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1), p);
            if (d.rpgClass == RpgClass.WARRIOR) d.energy = Math.min(Derived.maxEnergy(d), d.energy + 3);
        } else if (ranged) {
            m *= Derived.rangedMultiplier(d);
            WeaponCategory held = cat == WeaponCategory.BOW || cat == WeaponCategory.CROSSBOW ? cat
                    : WeaponCategory.of(p.getOffhandItem());
            if (held != WeaponCategory.NONE && !d.rpgClass.proficient(held)) m *= 0.75;
            if (d.race == Race.ELF && held == WeaponCategory.BOW) m *= 1.1;
        }

        if (d.markedTarget != null && d.markedTarget.equals(target.getUUID()) && p.level().getGameTime() < d.markedUntil) {
            m *= d.hasTalent("ranger_deadly_shot") ? 1.5 : 1.2;
        }
        if (d.hasTalent("warrior_executioner") && target.getHealth() < target.getMaxHealth() * 0.3f) m *= 1.3;
        if (d.hasTalent("cleric_judgment") && ModDamageTypes.isRadiant(source) && target.getType().is(EntityTypeTags.UNDEAD)) m *= 1.5;
        int hunter = Abilities.professionLevel(d, com.alvorecer.rpg.Profession.HUNTER);
        if (hunter > 0 && target instanceof Animal) m *= 1 + 0.01 * hunter;

        // Crítico: ataques diretos, projéteis e habilidades do jogador.
        if (p.getRandom().nextDouble() < Derived.critChance(d)) {
            m *= Derived.critMultiplier(d);
            if (d.hasTalent("wizard_overload") && ModDamageTypes.isMagic(source)) d.mana = Math.min(Derived.maxMana(d), d.mana + 10);
            ((ServerLevel) p.level()).sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.6,
                    target.getZ(), 12, 0.3, 0.3, 0.3, 0.2);
            p.level().playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8f, 1.2f);
        }
        return (float) (amount * m);
    }

    static float defenderModifiers(ServerPlayer tp, PlayerRpgData d, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return amount;
        double m = 1;
        Element el = ModDamageTypes.elementOf(source);
        if (d.race == Race.DRAGONBORN && el != null) {
            if (el == d.element) m *= 0.5;
            else if (el == d.element.opposite()) m *= 1.5;
        }
        if (d.race == Race.TIEFLING) {
            if (el == Element.FIRE) m *= 0.5;
            if (ModDamageTypes.isRadiant(source)) m *= 1.5;
        }
        if (ModDamageTypes.isPoison(source)) {
            if (d.race == Race.ELF) m *= 1.5;
            if (d.race == Race.DWARF) m *= 0.5;
        }
        if (ModDamageTypes.isMagic(source)) m *= 1 - Derived.magicResistance(d);
        if (tp.hasEffect(ModEffects.STONESKIN)) m *= 0.6;
        if (tp.hasEffect(ModEffects.ORC_FURY)) m *= 1.2;
        if (tp.hasEffect(ModEffects.BARBARIAN_RAGE) && !ModDamageTypes.isMagic(source)) m *= 0.75;
        m *= 1 - Derived.damageReduction(d);
        if (d.hasTalent("warrior_unbreakable") && tp.getHealth() < tp.getMaxHealth() * 0.3f) m *= 0.75;
        return (float) (amount * m);
    }

    /** Fúria Imortal: o dano não reduz a vida abaixo de 1. Bárbaro: recupera energia ao sofrer dano. */
    @SubscribeEvent
    public static void onDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (p.hasEffect(ModEffects.UNDYING) && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && p.getHealth() - event.getNewDamage() < 1) {
            event.setNewDamage(Math.max(0, p.getHealth() - 1));
        }
    }

    @SubscribeEvent
    public static void onDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        PlayerRpgData d = ModAttachments.get(p);
        if (d.rpgClass == RpgClass.BARBARIAN && event.getNewDamage() > 0) {
            d.energy = Math.min(Derived.maxEnergy(d), d.energy + event.getNewDamage() * 1.5f);
        }
        long now = p.level().getGameTime();
        if (d.hasTalent("rogue_elusive") && p.isAlive() && p.getHealth() < p.getMaxHealth() * 0.3f && !d.isOnCooldown("talent_rogue_elusive", now)) {
            p.addEffect(new MobEffectInstance(ModEffects.EVASION, 60, 0));
            d.cooldowns.put("talent_rogue_elusive", now + 1200);
            p.displayClientMessage(Component.translatable("talent.alvorecer.rogue_elusive"), true);
        }
    }

    /** Tiefling: curas recebidas -20%. */
    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof Player p && ModAttachments.get(p).race == Race.TIEFLING) {
            event.setAmount(event.getAmount() * 0.8f);
        }
    }

    /**
     * Testes de resistência de jogadores contra efeitos negativos e imunidade da Determinação.
     * Efeitos aplicados pelo próprio jogador (poções que ele bebe) não pedem teste.
     */
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        MobEffectInstance inst = event.getEffectInstance();
        if (inst == null || inst.getEffect().value().getCategory() != MobEffectCategory.HARMFUL) return;
        boolean unstoppable = p.hasEffect(ModEffects.BARBARIAN_RAGE) && ModAttachments.get(p).hasTalent("barbarian_unstoppable")
                && (inst.getEffect().is(MobEffects.MOVEMENT_SLOWDOWN) || inst.getEffect().value() == ModEffects.ROOT.value());
        if (unstoppable || (p.hasEffect(ModEffects.DETERMINATION) && Abilities.isControl(inst.getEffect()))) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            return;
        }
        PlayerRpgData d = ModAttachments.get(p);
        if (!RpgConfig.PLAYER_SAVING_THROWS.get() || !d.hasCharacter() || event.getEffectSource() == null || event.getEffectSource() == p) return;
        Stat stat = saveStatFor(inst);
        int roll = 1 + p.getRandom().nextInt(20);
        boolean success = roll == 20 || (roll != 1 && roll + Derived.saveBonus(d, stat) >= RpgConfig.EFFECT_SAVE_DC.get());
        if (success) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            p.displayClientMessage(Component.translatable("msg.alvorecer.save_success", Component.translatable(stat.langKey()), roll), true);
        }
    }

    static Stat saveStatFor(MobEffectInstance inst) {
        var e = inst.getEffect();
        if (e.is(MobEffects.POISON) || e.is(MobEffects.WITHER) || e.is(MobEffects.HUNGER)) return Stat.CON;
        if (e.is(MobEffects.MOVEMENT_SLOWDOWN) || e.is(MobEffects.LEVITATION) || e.value() == ModEffects.STUN.value()
                || e.value() == ModEffects.ROOT.value()) return Stat.DEX;
        return Stat.WIS;
    }

    /** Atordoados e encantados não atacam. */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (disabled(event.getEntity())) event.setCanceled(true);
    }

    /** Criaturas encantadas esquecem o alvo. */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide && mob.getTarget() != null && disabled(mob)) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
    }
}
