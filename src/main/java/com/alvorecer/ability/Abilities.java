package com.alvorecer.ability;

import com.alvorecer.entity.IllusionEntity;
import com.alvorecer.registry.ModDamageTypes;
import com.alvorecer.registry.ModEffects;
import com.alvorecer.registry.ModEntities;
import com.alvorecer.rpg.Element;
import com.alvorecer.rpg.PlayerRpgData;
import com.alvorecer.rpg.Profession;
import com.alvorecer.rpg.Progression;
import com.alvorecer.rpg.Resource;
import com.alvorecer.rpg.RpgClass;
import com.alvorecer.rpg.School;
import com.alvorecer.rpg.Stat;
import com.alvorecer.rpg.WeaponCategory;
import com.alvorecer.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static com.alvorecer.ability.AbilityUtil.*;

/**
 * Todas as habilidades do mod. Números (dano base, duração, raio) estão aqui e são repetidos nas descrições do
 * arquivo de idioma; o custo e a recarga exibidos na interface vêm diretamente destes registros.
 * "power" = Derived.power(...) × configuração (ver Codex, capítulo Fórmulas).
 */
public final class Abilities {
    private static final Map<String, Ability> ALL = new LinkedHashMap<>();

    /** Efeitos de controle que a Determinação humana remove e impede. */
    public static final List<Holder<MobEffect>> CONTROL_EFFECTS = List.of(MobEffects.MOVEMENT_SLOWDOWN, MobEffects.WEAKNESS,
            MobEffects.BLINDNESS, MobEffects.CONFUSION, MobEffects.DARKNESS, MobEffects.LEVITATION, MobEffects.DIG_SLOWDOWN,
            ModEffects.STUN, ModEffects.ROOT, ModEffects.CHARM);

    private Abilities() {
    }

    public static Ability get(String id) {
        return ALL.get(id);
    }

    public static Map<String, Ability> all() {
        return ALL;
    }

    public static boolean isControl(Holder<MobEffect> effect) {
        return CONTROL_EFFECTS.stream().anyMatch(h -> h.value() == effect.value());
    }

    private static void reg(String id, Resource resource, int cost, double cooldownSeconds, School school, Stat stat,
                            boolean mobility, Ability.Executor executor) {
        ALL.put(id, new Ability(id, resource, cost, (int) Math.round(cooldownSeconds * 20), school, stat, mobility, executor));
    }

    /** Envia o motivo da falha na barra de ação e retorna false (nada é cobrado). */
    public static boolean fail(ServerPlayer p, String key) {
        p.displayClientMessage(Component.translatable("msg.alvorecer." + key), true);
        return false;
    }

    private static float weapon(ServerPlayer p) {
        return (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    private static int dc(PlayerRpgData d, Stat stat) {
        return com.alvorecer.rpg.Derived.saveDc(d, stat);
    }

    private static MobEffectInstance fx(Holder<MobEffect> effect, int ticks, int amp) {
        return new MobEffectInstance(effect, ticks, amp);
    }

    private static boolean holdsRanged(ServerPlayer p) {
        for (InteractionHand hand : InteractionHand.values()) {
            WeaponCategory c = WeaponCategory.of(p.getItemInHand(hand));
            if (c == WeaponCategory.BOW || c == WeaponCategory.CROSSBOW) return true;
        }
        return false;
    }

    private static void shootArrow(ServerPlayer p, Vec3 from, Vec3 direction, float speed, double damage) {
        Arrow arrow = new Arrow(p.level(), p, new ItemStack(Items.ARROW), null);
        arrow.setPos(from.x, from.y, from.z);
        arrow.shoot(direction.x, direction.y, direction.z, speed, 0);
        arrow.setBaseDamage(damage / speed);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        p.level().addFreshEntity(arrow);
    }

    private static boolean isUndead(LivingEntity e) {
        return e.getType().is(EntityTypeTags.UNDEAD);
    }

    /** Investida: impulso horizontal e golpe em quem estiver no caminho durante {@code ticks}. */
    private static void charge(ServerPlayer p, PlayerRpgData d, double speed, int ticks, ParticleOptions trail, Consumer<LivingEntity> onHit) {
        Vec3 dir = horizontalLook(p);
        launch(p, d, dir.scale(speed).add(0, 0.2, 0));
        Set<UUID> hit = new HashSet<>();
        for (int i = 1; i <= ticks; i++) {
            Scheduler.after(i, () -> {
                if (!p.isAlive() || p.isRemoved()) return;
                ServerLevel level = p.serverLevel();
                level.sendParticles(trail, p.getX(), p.getY() + 1, p.getZ(), 4, 0.3, 0.4, 0.3, 0.01);
                for (LivingEntity e : enemiesAround(p, p.position(), 1.8)) {
                    if (hit.add(e.getUUID())) onHit.accept(e);
                }
            });
        }
    }

    private static void effectOnAllies(ServerPlayer p, double radius, MobEffectInstance... effects) {
        for (LivingEntity ally : alliesAround(p, radius)) {
            for (MobEffectInstance e : effects) ally.addEffect(new MobEffectInstance(e), p);
        }
    }

    private static void restore(LivingEntity e, float mana, float energy) {
        if (e instanceof ServerPlayer sp) {
            PlayerRpgData d = ModAttachments.get(sp);
            d.mana = Math.min(com.alvorecer.rpg.Derived.maxMana(d), d.mana + mana);
            d.energy = Math.min(com.alvorecer.rpg.Derived.maxEnergy(d), d.energy + energy);
            sp.syncData(ModAttachments.RPG);
        }
    }

    /** Nível da profissão se for a principal ou a secundária; 0 se o jogador não a tiver. */
    public static int professionLevel(PlayerRpgData d, Profession prof) {
        return d.levelOf(prof);
    }

    static {
        registerRaces();
        registerWarrior();
        registerBarbarian();
        registerWizard();
        registerRogue();
        registerRanger();
        registerCleric();
        registerPaladin();
        registerDruid();
        registerBard();
        registerProfessions();
        registerItems();
    }

    /** Habilidades de itens (cajados, varinhas e armas especiais): usadas com o botão direito. */
    public static final List<String> ITEM_ABILITIES = List.of("staff_arcane_bolt", "wand_bolt", "item_solar_wave", "item_dawn_dash", "item_shadow_evasion", "item_dawn_heart");

    private static void registerItems() {
        reg("staff_arcane_bolt", Resource.MANA, 4, 0.8, School.ARCANE, Stat.INT, false, (p, d, level, power) -> {
            LivingEntity t = raycast(p, 24, false);
            if (t == null) return fail(p, "no_target");
            line(level, ParticleTypes.END_ROD, p.getEyePosition().subtract(0, 0.3, 0), chest(t), 0.5);
            damage(t, spell(p, ModDamageTypes.ARCANE), (float) (4 * power));
            sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.8f);
            return true;
        });

        reg("wand_bolt", Resource.MANA, 3, 0.6, School.ARCANE, Stat.INT, false, (p, d, level, power) -> {
            LivingEntity t = raycast(p, 18, false);
            if (t == null) return fail(p, "no_target");
            line(level, ParticleTypes.WITCH, p.getEyePosition().subtract(0, 0.3, 0), chest(t), 0.4);
            damage(t, spell(p, ModDamageTypes.ARCANE), (float) (3 * power));
            sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6f, 2.0f);
            return true;
        });

        reg("item_dawn_heart", Resource.NONE, 0, 300, School.DIVINE, Stat.WIS, false, (p, d, level, power) -> {
            sound(level, p.position(), SoundEvents.BEACON_ACTIVATE, 1.2f, 1.3f);
            level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 80, 3, 1, 3, 0.05);
            for (ServerPlayer ally : com.alvorecer.party.Parties.nearbyMembers(p)) {
                if (ally.distanceToSqr(p) > 12 * 12) continue;
                heal(p, ally, ally.getMaxHealth() * 0.4f);
                ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 1));
                ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            }
            for (LivingEntity t : enemiesAround(p, p.position(), 6)) {
                damage(t, spell(p, ModDamageTypes.RADIANT), (float) (12 * power * (isUndead(t) ? 2 : 1)));
                Vec3 away = t.position().subtract(p.position()).normalize();
                t.knockback(1.0, -away.x, -away.z);
            }
            return true;
        });

        reg("item_solar_wave", Resource.ENERGY, 30, 10, School.DIVINE, Stat.STR, false, (p, d, level, power) -> {
            Vec3 dir = horizontalLook(p);
            Vec3 side = new Vec3(-dir.z, 0, dir.x);
            Vec3 start = p.position().add(0, 0.6, 0);
            Set<UUID> hit = new HashSet<>();
            sound(level, p.position(), SoundEvents.BEACON_ACTIVATE, 1f, 1.6f);
            sound(level, p.position(), SoundEvents.GENERIC_EXPLODE.value(), 0.6f, 1.8f);
            for (int step = 1; step <= 7; step++) {
                Vec3 center = start.add(dir.scale(step * 2.0));
                Scheduler.after(step, () -> {
                    if (p.isRemoved()) return;
                    line(level, ParticleTypes.END_ROD, center.subtract(side.scale(1.8)), center.add(side.scale(1.8)), 0.3);
                    level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0, 0, 0, 0);
                    for (LivingEntity t : enemiesAround(p, center, 2.0)) {
                        if (hit.add(t.getUUID())) {
                            damage(t, spell(p, ModDamageTypes.RADIANT), (float) (10 * power * (isUndead(t) ? 2 : 1)));
                            t.knockback(0.8, -dir.x, -dir.z);
                        }
                    }
                });
            }
            return true;
        });

        reg("item_dawn_dash", Resource.ENERGY, 20, 6, School.PHYSICAL, Stat.STR, true, (p, d, level, power) -> {
            sound(level, p.position(), SoundEvents.TRIDENT_RIPTIDE_1.value(), 1f, 1.4f);
            float dmg = (float) (weapon(p) * 0.8 * power);
            charge(p, d, 2.0, 8, ParticleTypes.END_ROD, t -> {
                damage(t, physical(p), dmg);
                level.sendParticles(ParticleTypes.FLASH, t.getX(), t.getY() + 1, t.getZ(), 1, 0, 0, 0, 0);
            });
            return true;
        });

        reg("item_shadow_evasion", Resource.ENERGY, 15, 12, School.PHYSICAL, Stat.DEX, true, (p, d, level, power) -> {
            p.addEffect(fx(ModEffects.EVASION, 40, 0));
            p.addEffect(fx(MobEffects.INVISIBILITY, 40, 0));
            level.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 1, p.getZ(), 25, 0.4, 0.6, 0.4, 0.02);
            launch(p, d, horizontalLook(p).scale(-1.0).add(0, 0.3, 0));
            sound(level, p.position(), SoundEvents.ILLUSIONER_MIRROR_MOVE, 1f, 0.8f);
            return true;
        });
    }

    // ================================================================= RAÇAS

    private static void registerRaces() {
        reg("human_determination", Resource.ENERGY, 20, 90, School.PHYSICAL, Stat.CON, false, (p, d, level, power) -> {
            CONTROL_EFFECTS.forEach(p::removeEffect);
            p.addEffect(fx(ModEffects.DETERMINATION, 160, 0));
            burst(level, ParticleTypes.TOTEM_OF_UNDYING, p, 30);
            sound(level, p.position(), SoundEvents.BEACON_POWER_SELECT, 0.8f, 1.4f);
            return true;
        });

        reg("elf_fey_step", Resource.MANA, 15, 12, School.ARCANE, Stat.INT, true, (p, d, level, power) -> {
            Vec3 to = blinkTarget(p, 8);
            if (to == null) return fail(p, "no_space");
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 30, 0.3, 0.6, 0.3, 0.05);
            teleport(p, to);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, to.x, to.y + 1, to.z, 30, 0.3, 0.6, 0.3, 0.05);
            sound(level, to, SoundEvents.ENDERMAN_TELEPORT, 0.8f, 1.3f);
            return true;
        });

        reg("dwarf_stoneskin", Resource.ENERGY, 25, 60, School.PHYSICAL, Stat.CON, false, (p, d, level, power) -> {
            p.addEffect(fx(ModEffects.STONESKIN, 200, 0));
            burst(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), p, 40);
            sound(level, p.position(), SoundEvents.ANVIL_PLACE, 0.5f, 1.2f);
            return true;
        });

        reg("halfling_luck", Resource.ENERGY, 15, 20, School.PHYSICAL, Stat.DEX, false, (p, d, level, power) -> {
            p.addEffect(fx(ModEffects.EVASION, 60, 0));
            burst(level, ParticleTypes.CLOUD, p, 15);
            sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.6f);
            return true;
        });

        reg("orc_fury", Resource.ENERGY, 20, 45, School.PHYSICAL, Stat.STR, false, (p, d, level, power) -> {
            p.addEffect(fx(ModEffects.ORC_FURY, 240, 0));
            burst(level, ParticleTypes.ANGRY_VILLAGER, p, 10);
            sound(level, p.position(), SoundEvents.RAVAGER_ROAR, 0.8f, 1.2f);
            return true;
        });

        reg("dragonborn_breath", Resource.ENERGY, 20, 30, School.ELEMENTAL, Stat.CON, false, (p, d, level, power) -> {
            Element el = d.element;
            ParticleOptions particle = switch (el) {
                case FIRE -> ParticleTypes.FLAME;
                case COLD -> ParticleTypes.SNOWFLAKE;
                case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
                case ACID -> ParticleTypes.ITEM_SLIME;
            };
            Vec3 eye = p.getEyePosition();
            Vec3 look = p.getViewVector(1);
            for (int i = 0; i < 60; i++) {
                Vec3 dir = look.add((p.getRandom().nextDouble() - 0.5) * 0.7, (p.getRandom().nextDouble() - 0.5) * 0.5,
                        (p.getRandom().nextDouble() - 0.5) * 0.7).normalize();
                level.sendParticles(particle, eye.x, eye.y - 0.2, eye.z, 0, dir.x, dir.y, dir.z, 0.6);
            }
            sound(level, eye, SoundEvents.ENDER_DRAGON_SHOOT, 0.8f, 1.4f);
            int dc = dc(d, Stat.CON);
            for (LivingEntity t : enemiesInCone(p, 6, 35)) {
                boolean saved = save(t, Stat.DEX, dc);
                damage(t, spell(p, ModDamageTypes.forElement(el)), (float) (7 * power * (saved ? 0.5 : 1)));
                if (saved) continue;
                switch (el) {
                    case FIRE -> t.setRemainingFireTicks(80);
                    case COLD -> control(p, t, fx(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                    case LIGHTNING -> control(p, t, fx(ModEffects.STUN, 20, 0));
                    case ACID -> control(p, t, fx(MobEffects.WEAKNESS, 80, 0));
                }
            }
            return true;
        });

        reg("tiefling_hellblast", Resource.MANA, 25, 40, School.SHADOW, Stat.CHA, false, (p, d, level, power) -> {
            for (double r = 1.5; r <= 4.5; r += 1.5) ring(level, ParticleTypes.SOUL_FIRE_FLAME, p.position(), r, (int) (r * 12));
            level.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 0.5, p.getZ(), 30, 1.5, 0.3, 1.5, 0.02);
            sound(level, p.position(), SoundEvents.BLAZE_SHOOT, 1f, 0.7f);
            sound(level, p.position(), SoundEvents.GENERIC_EXPLODE.value(), 0.5f, 1.3f);
            int dc = dc(d, Stat.CHA);
            for (LivingEntity t : enemiesAround(p, p.position(), 4.5)) {
                boolean saved = save(t, Stat.CON, dc);
                float f = saved ? 0.5f : 1f;
                damage(t, spell(p, ModDamageTypes.FIRE), (float) (4 * power * f));
                damage(t, spell(p, ModDamageTypes.NECROTIC), (float) (4 * power * f));
                if (!saved) t.setRemainingFireTicks(60);
            }
            return true;
        });

        reg("gnome_illusion", Resource.MANA, 20, 35, School.ARCANE, Stat.INT, false, (p, d, level, power) -> {
            IllusionEntity illusion = ModEntities.ILLUSION.get().create(level);
            if (illusion == null) return fail(p, "no_space");
            illusion.setOwner(p);
            level.addFreshEntity(illusion);
            p.addEffect(fx(MobEffects.INVISIBILITY, 60, 0));
            sound(level, p.position(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, 1f, 1.2f);
            for (int i = 0; i < IllusionEntity.LIFETIME; i += 20) {
                Scheduler.after(i + 1, () -> {
                    if (!illusion.isAlive()) return;
                    for (Mob mob : level.getEntitiesOfClass(Mob.class, illusion.getBoundingBox().inflate(16))) {
                        if (mob.getTarget() == p) mob.setTarget(illusion);
                    }
                });
            }
            return true;
        });
    }

    // ================================================================= GUERREIRO

    private static void registerWarrior() {
        reg("warrior_power_strike", Resource.ENERGY, 15, 6, School.PHYSICAL, Stat.STR, false, (p, d, level, power) -> {
            LivingEntity t = raycast(p, 4.5, false);
            if (t == null) return fail(p, "no_target");
            p.swing(InteractionHand.MAIN_HAND, true);
            damage(t, physical(p), (float) (weapon(p) * 1.5 * power));
            t.knockback(1.2, p.getX() - t.getX(), p.getZ() - t.getZ());
            burst(level, ParticleTypes.CRIT, t, 20);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, t.getX(), t.getY() + 1, t.getZ(), 1, 0, 0, 0, 0);
            sound(level, t.position(), SoundEvents.PLAYER_ATTACK_STRONG, 1f, 0.8f);
            return true;
        });

        reg("warrior_second_wind", Resource.ENERGY, 20, 45, School.PHYSICAL, Stat.CON, false, (p, d, level, power) -> {
            heal(p, p, (float) (6 * power));
            burst(level, ParticleTypes.HEART, p, 6);
            sound(level, p.position(), SoundEvents.PLAYER_LEVELUP, 0.4f, 1.6f);
            return true;
        });

        reg("warrior_charge", Resource.ENERGY, 20, 12, School.PHYSICAL, Stat.STR, true, (p, d, level, power) -> {
            sound(level, p.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1f, 0.7f);
            float dmg = (float) (weapon(p) * power);
            charge(p, d, 1.6, 8, ParticleTypes.CLOUD, t -> {
                damage(t, physical(p), dmg);
                control(p, t, fx(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                t.knockback(0.8, p.getX() - t.getX(), p.getZ() - t.getZ());
            });
            return true;
        });

        reg("warrior_war_cry", Resource.ENERGY, 25, 40, School.PHYSICAL, Stat.STR, false, (p, d, level, power) -> {
            effectOnAllies(p, 10, fx(MobEffects.DAMAGE_BOOST, 200, 0));
            int dc = dc(d, Stat.STR);
            for (LivingEntity t : enemiesAround(p, p.position(), 6)) {
                if (!save(t, Stat.WIS, dc)) control(p, t, fx(MobEffects.WEAKNESS, 120, 0));
            }
            ring(level, ParticleTypes.ANGRY_VILLAGER, p.position(), 3, 16);
            sound(level, p.position(), SoundEvents.RAVAGER_ROAR, 1f, 1.4f);
            return true;
        });

        reg("warrior_action_surge", Resource.ENERGY, 50, 180, School.PHYSICAL, Stat.STR, false, (p, d, level, power) -> {
            for (String other : RpgClass.WARRIOR.abilities) {
                if (!other.equals("warrior_action_surge")) d.cooldowns.remove(other);
            }
            p.addEffect(fx(MobEffects.DAMAGE_BOOST, 200, 1));
            p.addEffect(fx(MobEffects.DIG_SPEED, 200, 1));
            p.addEffect(fx(MobEffects.DAMAGE_RESISTANCE, 200, 0));
            burst(level, ParticleTypes.TOTEM_OF_UNDYING, p, 40);
            sound(level, p.position(), SoundEvents.TOTEM_USE, 0.6f, 1.2f);
            return true;
        });
    }

    // ================================================================= BÁRBARO

    private static void registerBarbarian() {
        reg("barbarian_savage_cleave", Resource.ENERGY, 15, 5, School.PHYSICAL, Stat.STR, false, (p, d, level, power) -> {
            List<LivingEntity> targets = enemiesInCone(p, 4, 60);
            if (targets.isEmpty()) return fail(p, "no_target");
            p.swing(InteractionHand.MAIN_HAND, true);
            for (LivingEntity t : targets) {
                damage(t, physical(p), (float) (weapon(p) * 1.2 * power));
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, t.getX(), t.getY() + 1, t.getZ(), 1, 0, 0, 0, 0);
            }
            sound(level, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.8f);
            return true;
        });

        reg("barbarian_rage", Resource.ENERGY, 25, 50, School.PHYSICAL, Stat.STR, false, (p, d, level, power) -> {
            p.addEffect(fx(ModEffects.BARBARIAN_RAGE, 300, 0));
            burst(level, ParticleTypes.ANGRY_VILLAGER, p, 12);
            sound(level, p.position(), SoundEvents.RAVAGER_ROAR, 0.9f, 0.9f);
            return true;
        });

        reg("barbarian_brutal_leap", Resource.ENERGY, 20, 14, School.PHYSICAL, Stat.STR, true, (p, d, level, power) -> {
            launch(p, d, horizontalLook(p).scale(1.2).add(0, 0.9, 0));
            d.leapPending = true;
            d.leapStart = level.getGameTime();
            d.leapPower = power;
            sound(level, p.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1f, 0.6f);
            return true;
        });

        reg("barbarian_roar", Resource.ENERGY, 20, 30, School.PHYSICAL, Stat.STR, false, (p, d, level, power) -> {
            int dc = dc(d, Stat.STR);
            for (LivingEntity t : enemiesAround(p, p.position(), 8)) {
                if (save(t, Stat.WIS, dc)) continue;
                control(p, t, fx(MobEffects.MOVEMENT_SLOWDOWN, 120, 1));
                control(p, t, fx(MobEffects.WEAKNESS, 120, 0));
            }
            ring(level, ParticleTypes.SONIC_BOOM, p.position(), 2.5, 8);
            sound(level, p.position(), SoundEvents.POLAR_BEAR_WARNING, 1.2f, 0.6f);
            return true;
        });

        reg("barbarian_undying_rage", Resource.ENERGY, 60, 240, School.PHYSICAL, Stat.CON, false, (p, d, level, power) -> {
            p.addEffect(fx(ModEffects.UNDYING, 200, 0));
            p.addEffect(fx(ModEffects.BARBARIAN_RAGE, 200, 0));
            burst(level, ParticleTypes.DAMAGE_INDICATOR, p, 20);
            sound(level, p.position(), SoundEvents.WITHER_AMBIENT, 0.7f, 0.6f);
            Scheduler.after(200, () -> {
                if (p.isAlive() && !p.isRemoved()) heal(p, p, p.getMaxHealth() * 0.3f);
            });
            return true;
        });
    }

    // ================================================================= MAGO

    private static void registerWizard() {
        reg("wizard_arcane_missile", Resource.MANA, 10, 1.5, School.ARCANE, Stat.INT, false, (p, d, level, power) -> {
            LivingEntity t = raycast(p, 24, false);
            if (t == null) return fail(p, "no_target");
            line(level, ParticleTypes.END_ROD, p.getEyePosition().subtract(0, 0.2, 0), chest(t), 0.4);
            damage(t, spell(p, ModDamageTypes.ARCANE), (float) (5 * power));
            burst(level, ParticleTypes.ENCHANTED_HIT, t, 12);
            sound(level, p.position(), SoundEvents.ILLUSIONER_CAST_SPELL, 0.5f, 1.8f);
            return true;
        });

        reg("wizard_arcane_shield", Resource.MANA, 20, 25, School.ARCANE, Stat.INT, false, (p, d, level, power) -> {
            int amp = Mth.clamp((int) Math.round(power * 1.5), 1, 4) - 1;
            p.addEffect(fx(MobEffects.ABSORPTION, 200, amp));
            ring(level, ParticleTypes.ENCHANT, p.position().add(0, 1, 0), 1.2, 24);
            sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1f, 1.2f);
            return true;
        });

        reg("wizard_fireball", Resource.MANA, 30, 8, School.ELEMENTAL, Stat.INT, false, (p, d, level, power) -> {
            Vec3 point = aimPoint(p, 30);
            line(level, ParticleTypes.FLAME, p.getEyePosition().subtract(0, 0.2, 0), point, 0.5);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, point.x, point.y, point.z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.FLAME, point.x, point.y + 0.5, point.z, 50, 1.5, 0.8, 1.5, 0.05);
            sound(level, p.position(), SoundEvents.FIRECHARGE_USE, 1f, 1f);
            sound(level, point, SoundEvents.GENERIC_EXPLODE.value(), 1f, 1.1f);
            int dc = dc(d, Stat.INT);
            for (LivingEntity t : enemiesAround(p, point, 3)) {
                boolean saved = save(t, Stat.DEX, dc);
                damage(t, spell(p, ModDamageTypes.FIRE), (float) (8 * power * (saved ? 0.5 : 1)));
                if (!saved) t.setRemainingFireTicks(80);
            }
            return true;
        });

        reg("wizard_blink", Resource.MANA, 20, 10, School.ARCANE, Stat.INT, true, (p, d, level, power) -> {
            Vec3 to = blinkTarget(p, 10);
            if (to == null) return fail(p, "no_space");
            level.sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 40, 0.3, 0.6, 0.3, 0.2);
            teleport(p, to);
            level.sendParticles(ParticleTypes.PORTAL, to.x, to.y + 1, to.z, 40, 0.3, 0.6, 0.3, 0.2);
            sound(level, to, SoundEvents.ENDERMAN_TELEPORT, 0.8f, 1.6f);
            return true;
        });

        reg("wizard_meteor_storm", Resource.MANA, 80, 180, School.ELEMENTAL, Stat.INT, false, (p, d, level, power) -> {
            Vec3 center = aimPoint(p, 30);
            ring(level, ParticleTypes.FLAME, center, 5, 48);
            sound(level, center, SoundEvents.BLAZE_AMBIENT, 1.5f, 0.5f);
            for (int wave = 0; wave < 5; wave++) {
                Scheduler.after(12 + wave * 12, () -> {
                    if (p.isRemoved()) return;
                    double a = p.getRandom().nextDouble() * Mth.TWO_PI;
                    double r = p.getRandom().nextDouble() * 5;
                    Vec3 impact = center.add(Math.cos(a) * r, 0, Math.sin(a) * r);
                    line(level, ParticleTypes.FLAME, impact.add(0, 12, 0), impact, 0.6);
                    level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, impact.x, impact.y, impact.z, 1, 0, 0, 0, 0);
                    sound(level, impact, SoundEvents.GENERIC_EXPLODE.value(), 1.2f, 0.9f);
                    for (LivingEntity t : enemiesAround(p, impact, 2.5)) {
                        damage(t, spell(p, ModDamageTypes.FIRE), (float) (5 * power));
                        t.setRemainingFireTicks(60);
                    }
                });
            }
            return true;
        });
    }

    // ================================================================= LADINO

    private static boolean unaware(ServerPlayer p, LivingEntity t) {
        if (p.isInvisible()) return true;
        if (t instanceof Mob m) return m.getTarget() != p;
        Vec3 toRogue = p.position().subtract(t.position()).normalize();
        return t.getViewVector(1).dot(toRogue) < 0; // jogador alvo olhando para o outro lado
    }

    private static void registerRogue() {
        reg("rogue_treacherous_strike", Resource.ENERGY, 15, 5, School.PHYSICAL, Stat.DEX, false, (p, d, level, power) -> {
            LivingEntity t = raycast(p, 4.5, false);
            if (t == null) return fail(p, "no_target");
            boolean sneak = unaware(p, t);
            p.swing(InteractionHand.MAIN_HAND, true);
            damage(t, physical(p), (float) (weapon(p) * (sneak ? 2.6 : 1.3) * power));
            burst(level, sneak ? ParticleTypes.DAMAGE_INDICATOR : ParticleTypes.CRIT, t, 15);
            sound(level, t.position(), SoundEvents.PLAYER_ATTACK_CRIT, 1f, sneak ? 0.7f : 1.1f);
            if (sneak) p.displayClientMessage(Component.translatable("msg.alvorecer.sneak_attack"), true);
            return true;
        });

        reg("rogue_smoke_bomb", Resource.ENERGY, 25, 30, School.PHYSICAL, Stat.DEX, false, (p, d, level, power) -> {
            p.addEffect(fx(MobEffects.INVISIBILITY, 120, 0));
            for (LivingEntity t : enemiesAround(p, p.position(), 5)) control(p, t, fx(MobEffects.BLINDNESS, 60, 0));
            for (Mob mob : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(16))) {
                if (mob.getTarget() == p) mob.setTarget(null);
            }
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.getX(), p.getY() + 0.5, p.getZ(), 40, 1.5, 0.5, 1.5, 0.01);
            sound(level, p.position(), SoundEvents.FIRE_EXTINGUISH, 1f, 0.8f);
            return true;
        });

        reg("rogue_shadow_step", Resource.ENERGY, 20, 15, School.PHYSICAL, Stat.DEX, true, (p, d, level, power) -> {
            LivingEntity t = raycast(p, 12, false);
            if (t == null) return fail(p, "no_target");
            Vec3 back = Vec3.directionFromRotation(0, t.getYRot()).scale(-1.3);
            Vec3 dest = t.position().add(back);
            if (!level.noCollision(p, p.getBoundingBox().move(dest.subtract(p.position())))) return fail(p, "no_space");
            level.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 1, p.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
            Vec3 face = t.position().subtract(dest);
            float yaw = (float) (Mth.atan2(face.z, face.x) * Mth.RAD_TO_DEG) - 90f;
            p.connection.teleport(dest.x, dest.y, dest.z, yaw, p.getXRot());
            p.resetFallDistance();
            level.sendParticles(ParticleTypes.LARGE_SMOKE, dest.x, dest.y + 1, dest.z, 20, 0.3, 0.6, 0.3, 0.02);
            sound(level, dest, SoundEvents.ENDERMAN_TELEPORT, 0.7f, 0.6f);
            return true;
        });

        reg("rogue_poison_blades", Resource.ENERGY, 20, 35, School.PHYSICAL, Stat.DEX, false, (p, d, level, power) -> {
            p.addEffect(fx(ModEffects.POISON_BLADES, 400, 0));
            burst(level, ParticleTypes.ITEM_SLIME, p, 15);
            sound(level, p.position(), SoundEvents.BREWING_STAND_BREW, 1f, 1.3f);
            return true;
        });

        reg("rogue_blade_dance", Resource.ENERGY, 50, 150, School.PHYSICAL, Stat.DEX, false, (p, d, level, power) -> {
            List<LivingEntity> targets = nearest(enemiesAround(p, p.position(), 8), p, 6);
            if (targets.isEmpty()) return fail(p, "no_target");
            p.addEffect(fx(ModEffects.EVASION, 60, 0));
            float dmg = (float) (weapon(p) * 0.9 * power);
            for (int strike = 0; strike < 3; strike++) {
                Scheduler.after(1 + strike * 8, () -> {
                    if (!p.isAlive() || p.isRemoved()) return;
                    p.swing(InteractionHand.MAIN_HAND, true);
                    for (LivingEntity t : targets) {
                        if (!t.isAlive()) continue;
                        damage(t, physical(p), dmg);
                        level.sendParticles(ParticleTypes.SWEEP_ATTACK, t.getX(), t.getY() + 1, t.getZ(), 1, 0, 0, 0, 0);
                    }
                    sound(level, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.3f);
                });
            }
            return true;
        });
    }

    // ================================================================= PATRULHEIRO

    private static void registerRanger() {
        reg("ranger_aimed_shot", Resource.ENERGY, 10, 3, School.PHYSICAL, Stat.DEX, false, (p, d, level, power) -> {
            if (!holdsRanged(p)) return fail(p, "need_bow");
            shootArrow(p, p.getEyePosition().subtract(0, 0.1, 0), p.getViewVector(1), 3.2f, 7 * power);
            sound(level, p.position(), SoundEvents.ARROW_SHOOT, 1f, 0.8f);
            return true;
        });

        reg("ranger_hunters_mark", Resource.ENERGY, 10, 15, School.NATURE, Stat.WIS, false, (p, d, level, power) -> {
            LivingEntity t = raycast(p, 32, false);
            if (t == null) return fail(p, "no_target");
            d.markedTarget = t.getUUID();
            d.markedUntil = level.getGameTime() + 400;
            t.addEffect(fx(MobEffects.GLOWING, 400, 0), p);
            burst(level, ParticleTypes.ENCHANTED_HIT, t, 15);
            sound(level, t.position(), SoundEvents.CROSSBOW_LOADING_END.value(), 1f, 1.4f);
            return true;
        });

        reg("ranger_volley", Resource.ENERGY, 25, 12, School.PHYSICAL, Stat.DEX, false, (p, d, level, power) -> {
            if (!holdsRanged(p)) return fail(p, "need_bow");
            for (int i = -2; i <= 2; i++) {
                Vec3 dir = Vec3.directionFromRotation(p.getXRot(), p.getYRot() + i * 6);
                shootArrow(p, p.getEyePosition().subtract(0, 0.1, 0), dir, 2.8f, 4 * power);
            }
            sound(level, p.position(), SoundEvents.ARROW_SHOOT, 1f, 1.2f);
            return true;
        });

        reg("ranger_disengage", Resource.ENERGY, 15, 10, School.PHYSICAL, Stat.DEX, true, (p, d, level, power) -> {
            for (LivingEntity t : enemiesAround(p, p.position(), 3)) control(p, t, fx(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            launch(p, d, horizontalLook(p).scale(-1.4).add(0, 0.55, 0));
            level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY(), p.getZ(), 15, 0.4, 0.1, 0.4, 0.02);
            sound(level, p.position(), SoundEvents.PHANTOM_FLAP, 1f, 1.4f);
            return true;
        });

        reg("ranger_arrow_rain", Resource.ENERGY, 60, 150, School.PHYSICAL, Stat.DEX, false, (p, d, level, power) -> {
            if (!holdsRanged(p)) return fail(p, "need_bow");
            Vec3 center = aimPoint(p, 40);
            ring(level, ParticleTypes.CRIT, center, 5, 40);
            for (int wave = 0; wave < 10; wave++) {
                Scheduler.after(10 + wave * 6, () -> {
                    if (p.isRemoved()) return;
                    for (int i = 0; i < 4; i++) {
                        double a = p.getRandom().nextDouble() * Mth.TWO_PI;
                        double r = Math.sqrt(p.getRandom().nextDouble()) * 5;
                        shootArrow(p, center.add(Math.cos(a) * r, 12, Math.sin(a) * r), new Vec3(0, -1, 0), 2.2f, 3 * power);
                    }
                    sound(level, center.add(0, 8, 0), SoundEvents.ARROW_SHOOT, 1f, 0.9f);
                });
            }
            return true;
        });
    }

    // ================================================================= CLÉRIGO

    private static LivingEntity allyOrSelf(ServerPlayer p, double range) {
        LivingEntity t = raycast(p, range, true);
        return t == null ? p : t;
    }

    private static void registerCleric() {
        reg("cleric_sacred_flame", Resource.MANA, 10, 2, School.DIVINE, Stat.WIS, false, (p, d, level, power) -> {
            LivingEntity t = raycast(p, 20, false);
            if (t == null) return fail(p, "no_target");
            line(level, ParticleTypes.END_ROD, p.getEyePosition().subtract(0, 0.2, 0), chest(t), 0.5);
            damage(t, spell(p, ModDamageTypes.RADIANT), (float) (5 * power * (isUndead(t) ? 2 : 1)));
            burst(level, ParticleTypes.FLASH, t, 1);
            sound(level, t.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.5f);
            return true;
        });

        reg("cleric_heal", Resource.MANA, 20, 6, School.DIVINE, Stat.WIS, false, (p, d, level, power) -> {
            LivingEntity t = allyOrSelf(p, 20);
            heal(p, t, (float) (6 * power));
            burst(level, ParticleTypes.HEART, t, 6);
            sound(level, t.position(), SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6f, 1.4f);
            return true;
        });

        reg("cleric_word_of_protection", Resource.MANA, 30, 30, School.DIVINE, Stat.WIS, false, (p, d, level, power) -> {
            effectOnAllies(p, 8, fx(MobEffects.DAMAGE_RESISTANCE, 200, 0), fx(MobEffects.ABSORPTION, 200, 0));
            ring(level, ParticleTypes.END_ROD, p.position(), 4, 32);
            sound(level, p.position(), SoundEvents.BEACON_POWER_SELECT, 1f, 1.2f);
            return true;
        });

        reg("cleric_turn_undead", Resource.MANA, 30, 30, School.DIVINE, Stat.WIS, false, (p, d, level, power) -> {
            List<LivingEntity> undead = enemiesAround(p, p.position(), 10).stream().filter(Abilities::isUndead).toList();
            if (undead.isEmpty()) return fail(p, "no_undead");
            int dc = dc(d, Stat.WIS);
            for (LivingEntity t : undead) {
                boolean saved = save(t, Stat.WIS, dc);
                damage(t, spell(p, ModDamageTypes.RADIANT), (float) (10 * power * (saved ? 0.5 : 1)));
                if (saved) continue;
                control(p, t, fx(MobEffects.MOVEMENT_SLOWDOWN, 120, 2));
                control(p, t, fx(MobEffects.WEAKNESS, 120, 0));
                if (t instanceof Mob m) m.setTarget(null);
            }
            ring(level, ParticleTypes.END_ROD, p.position(), 5, 40);
            ring(level, ParticleTypes.END_ROD, p.position(), 10, 70);
            sound(level, p.position(), SoundEvents.BELL_BLOCK, 1f, 1.2f);
            return true;
        });

        reg("cleric_divine_intervention", Resource.MANA, 100, 300, School.DIVINE, Stat.WIS, false, (p, d, level, power) -> {
            for (LivingEntity ally : alliesAround(p, 15)) {
                heal(p, ally, ally.getMaxHealth());
                new ArrayList<>(ally.getActiveEffects()).stream()
                        .filter(e -> e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
                        .forEach(e -> ally.removeEffect(e.getEffect()));
                ally.addEffect(fx(MobEffects.REGENERATION, 200, 1), p);
                burst(level, ParticleTypes.TOTEM_OF_UNDYING, ally, 20);
            }
            sound(level, p.position(), SoundEvents.TOTEM_USE, 0.5f, 1.5f);
            return true;
        });
    }

    // ================================================================= PALADINO

    private static void registerPaladin() {
        reg("paladin_divine_smite", Resource.MANA, 15, 5, School.DIVINE, Stat.CHA, false, (p, d, level, power) -> {
            LivingEntity t = raycast(p, 4.5, false);
            if (t == null) return fail(p, "no_target");
            p.swing(InteractionHand.MAIN_HAND, true);
            damage(t, physical(p), weapon(p) * 1.2f);
            damage(t, spell(p, ModDamageTypes.RADIANT), (float) (4 * power * (isUndead(t) ? 2 : 1)));
            if (d.hasTalent("paladin_burning_smite")) t.setRemainingFireTicks(60);
            burst(level, ParticleTypes.END_ROD, t, 20);
            sound(level, t.position(), SoundEvents.BELL_BLOCK, 0.7f, 1.5f);
            return true;
        });

        reg("paladin_lay_on_hands", Resource.MANA, 25, 30, School.DIVINE, Stat.CHA, false, (p, d, level, power) -> {
            LivingEntity t = allyOrSelf(p, 20);
            heal(p, t, (float) (8 * power));
            t.removeEffect(MobEffects.POISON);
            t.removeEffect(MobEffects.WITHER);
            burst(level, ParticleTypes.HEART, t, 8);
            sound(level, t.position(), SoundEvents.EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
            return true;
        });

        reg("paladin_aura_of_protection", Resource.MANA, 30, 45, School.DIVINE, Stat.CHA, false, (p, d, level, power) -> {
            p.addEffect(fx(ModEffects.HOLY_AURA, 300, 0));
            ring(level, ParticleTypes.END_ROD, p.position(), 8, 64);
            sound(level, p.position(), SoundEvents.BEACON_ACTIVATE, 0.8f, 1.3f);
            return true;
        });

        reg("paladin_holy_charge", Resource.MANA, 25, 15, School.DIVINE, Stat.CHA, true, (p, d, level, power) -> {
            sound(level, p.position(), SoundEvents.BELL_BLOCK, 0.8f, 0.8f);
            charge(p, d, 1.8, 9, ParticleTypes.END_ROD, t -> {
                damage(t, spell(p, ModDamageTypes.RADIANT), (float) (5 * power * (isUndead(t) ? 2 : 1)));
                t.knockback(0.6, p.getX() - t.getX(), p.getZ() - t.getZ());
            });
            return true;
        });

        reg("paladin_dawn_avatar", Resource.MANA, 80, 240, School.DIVINE, Stat.CHA, false, (p, d, level, power) -> {
            p.addEffect(fx(ModEffects.DAWN_AVATAR, 300, 0));
            p.addEffect(fx(MobEffects.DAMAGE_BOOST, 300, 1));
            p.addEffect(fx(MobEffects.REGENERATION, 300, 0));
            p.addEffect(fx(MobEffects.DAMAGE_RESISTANCE, 300, 1));
            burst(level, ParticleTypes.FLASH, p, 1);
            burst(level, ParticleTypes.END_ROD, p, 50);
            sound(level, p.position(), SoundEvents.TOTEM_USE, 0.6f, 0.8f);
            return true;
        });
    }

    /** Pulso do Avatar da Aurora (chamado a cada segundo enquanto o efeito durar). */
    public static void avatarPulse(ServerPlayer p, PlayerRpgData d) {
        double power = com.alvorecer.rpg.Derived.power(d, Stat.CHA, School.DIVINE);
        ring(p.serverLevel(), ParticleTypes.END_ROD, p.position(), 4, 24);
        for (LivingEntity t : enemiesAround(p, p.position(), 4)) {
            damage(t, spell(p, ModDamageTypes.RADIANT), (float) (2 * power * (isUndead(t) ? 2 : 1)));
        }
    }

    /** Pulso da Aura de Proteção: Resistência I por 2 s aos aliados em 8 blocos. */
    public static void auraPulse(ServerPlayer p) {
        int amp = ModAttachments.get(p).hasTalent("paladin_shield_of_faith") ? 1 : 0;
        effectOnAllies(p, 8, fx(MobEffects.DAMAGE_RESISTANCE, 40, amp));
        ring(p.serverLevel(), ParticleTypes.END_ROD, p.position(), 8, 32);
    }

    /** Aterrissagem do Salto Brutal. */
    public static void leapLanding(ServerPlayer p, PlayerRpgData d) {
        ServerLevel level = p.serverLevel();
        level.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY(), p.getZ(), 3, 1, 0.1, 1, 0);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(p.blockPosition().below())),
                p.getX(), p.getY(), p.getZ(), 40, 1.5, 0.1, 1.5, 0.1);
        sound(level, p.position(), SoundEvents.MACE_SMASH_GROUND, 1f, 0.8f);
        for (LivingEntity t : enemiesAround(p, p.position(), 3.5)) {
            damage(t, physical(p), (float) (weapon(p) * d.leapPower));
            control(p, t, fx(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
        }
    }

    // ================================================================= DRUIDA

    private static void registerDruid() {
        reg("druid_thorns", Resource.MANA, 10, 3, School.NATURE, Stat.WIS, false, (p, d, level, power) -> {
            LivingEntity t = raycast(p, 20, false);
            if (t == null) return fail(p, "no_target");
            line(level, ParticleTypes.COMPOSTER, p.getEyePosition().subtract(0, 0.2, 0), chest(t), 0.5);
            damage(t, spell(p, ModDamageTypes.NATURE), (float) (4 * power));
            if (!save(t, Stat.STR, dc(d, Stat.WIS))) control(p, t, fx(ModEffects.ROOT, 40, 0));
            if (d.hasTalent("druid_venom_thorns")) t.addEffect(fx(MobEffects.POISON, 80, 0), p);
            burst(level, ParticleTypes.HAPPY_VILLAGER, t, 10);
            sound(level, t.position(), SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, 1f, 0.8f);
            return true;
        });

        reg("druid_rejuvenate", Resource.MANA, 20, 15, School.NATURE, Stat.WIS, false, (p, d, level, power) -> {
            LivingEntity t = allyOrSelf(p, 20);
            t.addEffect(fx(MobEffects.REGENERATION, 200, 1), p);
            burst(level, ParticleTypes.HAPPY_VILLAGER, t, 15);
            sound(level, t.position(), SoundEvents.GRASS_PLACE, 1f, 1.2f);
            return true;
        });

        reg("druid_bear_form", Resource.MANA, 30, 60, School.NATURE, Stat.WIS, false, (p, d, level, power) -> {
            p.addEffect(fx(ModEffects.BEAR_FORM, 400, 0));
            if (d.hasTalent("druid_ancestral_bear")) p.addEffect(fx(MobEffects.DAMAGE_RESISTANCE, 400, 0));
            burst(level, ParticleTypes.HAPPY_VILLAGER, p, 20);
            sound(level, p.position(), SoundEvents.POLAR_BEAR_WARNING, 1f, 0.8f);
            return true;
        });

        reg("druid_entangle", Resource.MANA, 30, 20, School.NATURE, Stat.WIS, false, (p, d, level, power) -> {
            Vec3 center = aimPoint(p, 25);
            int dc = dc(d, Stat.WIS);
            List<LivingEntity> targets = enemiesAround(p, center, 4);
            for (LivingEntity t : targets) {
                if (!save(t, Stat.STR, dc)) control(p, t, fx(ModEffects.ROOT, 80, 0));
            }
            ring(level, ParticleTypes.COMPOSTER, center, 4, 40);
            sound(level, center, SoundEvents.GRASS_BREAK, 1.2f, 0.6f);
            for (int s = 1; s <= 4; s++) {
                Scheduler.after(s * 20, () -> {
                    if (p.isRemoved()) return;
                    ring(level, ParticleTypes.COMPOSTER, center, 4, 24);
                    for (LivingEntity t : targets) {
                        if (t.isAlive() && t.distanceToSqr(center) <= 25) damage(t, spell(p, ModDamageTypes.NATURE), (float) (1.5 * power));
                    }
                });
            }
            return true;
        });

        reg("druid_natures_wrath", Resource.MANA, 80, 200, School.NATURE, Stat.WIS, false, (p, d, level, power) -> {
            List<LivingEntity> targets = nearest(enemiesAround(p, p.position(), 15), p, 5);
            if (targets.isEmpty()) return fail(p, "no_target");
            for (int i = 0; i < targets.size(); i++) {
                LivingEntity t = targets.get(i);
                Scheduler.after(1 + i * 5, () -> {
                    if (!t.isAlive() || p.isRemoved()) return;
                    LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                    if (bolt != null) {
                        bolt.moveTo(t.getX(), t.getY(), t.getZ());
                        bolt.setVisualOnly(true);
                        level.addFreshEntity(bolt);
                    }
                    damage(t, spell(p, ModDamageTypes.LIGHTNING), (float) (9 * power));
                });
            }
            return true;
        });
    }

    // ================================================================= BARDO

    private static void registerBard() {
        reg("bard_vicious_mockery", Resource.MANA, 10, 3, School.ARCANE, Stat.CHA, false, (p, d, level, power) -> {
            LivingEntity t = raycast(p, 20, false);
            if (t == null) return fail(p, "no_target");
            damage(t, spell(p, ModDamageTypes.PSYCHIC), (float) (4 * power));
            if (!save(t, Stat.WIS, dc(d, Stat.CHA))) control(p, t, fx(MobEffects.WEAKNESS, 80, 0));
            burst(level, ParticleTypes.NOTE, t, 6);
            sound(level, t.position(), SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), 1f, 0.7f);
            return true;
        });

        reg("bard_inspiration", Resource.MANA, 25, 30, School.ARCANE, Stat.CHA, false, (p, d, level, power) -> {
            int dur = d.hasTalent("bard_crescendo") ? 450 : 300;
            effectOnAllies(p, 10, fx(MobEffects.MOVEMENT_SPEED, dur, 0), fx(MobEffects.DAMAGE_BOOST, dur, 0));
            ring(level, ParticleTypes.NOTE, p.position().add(0, 1, 0), 2, 12);
            sound(level, p.position(), SoundEvents.NOTE_BLOCK_CHIME.value(), 1f, 1.2f);
            return true;
        });

        reg("bard_song_of_rest", Resource.MANA, 25, 30, School.ARCANE, Stat.CHA, false, (p, d, level, power) -> {
            for (LivingEntity ally : alliesAround(p, 10)) {
                ally.addEffect(fx(MobEffects.REGENERATION, 200, 0), p);
                restore(ally, 0, 15);
            }
            ring(level, ParticleTypes.NOTE, p.position().add(0, 1, 0), 3, 16);
            sound(level, p.position(), SoundEvents.NOTE_BLOCK_FLUTE.value(), 1f, 1f);
            return true;
        });

        reg("bard_charm", Resource.MANA, 25, 25, School.ARCANE, Stat.CHA, false, (p, d, level, power) -> {
            LivingEntity t = raycast(p, 20, false);
            if (t == null) return fail(p, "no_target");
            burst(level, ParticleTypes.HEART, t, 8);
            sound(level, t.position(), SoundEvents.NOTE_BLOCK_BELL.value(), 1f, 1.5f);
            if (save(t, Stat.WIS, dc(d, Stat.CHA))) {
                p.displayClientMessage(Component.translatable("msg.alvorecer.resisted", t.getDisplayName()), true);
                return true;
            }
            control(p, t, fx(ModEffects.CHARM, 120, 0));
            if (t instanceof Mob m) m.setTarget(null);
            return true;
        });

        reg("bard_heroic_symphony", Resource.MANA, 80, 240, School.ARCANE, Stat.CHA, false, (p, d, level, power) -> {
            int dur = d.hasTalent("bard_crescendo") ? 600 : 400;
            for (LivingEntity ally : alliesAround(p, 15)) {
                ally.addEffect(fx(MobEffects.DAMAGE_BOOST, dur, 0), p);
                ally.addEffect(fx(MobEffects.MOVEMENT_SPEED, dur, 0), p);
                ally.addEffect(fx(MobEffects.DAMAGE_RESISTANCE, dur, 0), p);
                ally.addEffect(fx(MobEffects.REGENERATION, dur, 0), p);
                if (ally != p) restore(ally, 30, 30);
                burst(level, ParticleTypes.NOTE, ally, 10);
            }
            restore(p, 0, 30);
            sound(level, p.position(), SoundEvents.NOTE_BLOCK_CHIME.value(), 1.5f, 0.8f);
            return true;
        });
    }

    // ================================================================= PROFISSÕES

    private static void registerProfessions() {
        reg("blacksmith_field_repair", Resource.NONE, 0, 120, School.PHYSICAL, Stat.STR, false, (p, d, level, power) -> {
            ItemStack stack = p.getMainHandItem();
            if (!stack.isDamageableItem() || !stack.isDamaged()) return fail(p, "nothing_to_repair");
            int lvl = professionLevel(d, Profession.BLACKSMITH);
            int amount = (int) (stack.getMaxDamage() * (0.2 + 0.015 * lvl));
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - amount));
            sound(level, p.position(), SoundEvents.ANVIL_USE, 0.8f, 1.2f);
            burst(level, ParticleTypes.LAVA, p, 4);
            return true;
        });

        reg("alchemist_improvised_elixir", Resource.NONE, 0, 180, School.NATURE, Stat.INT, false, (p, d, level, power) -> {
            List<Holder<MobEffect>> pool = List.of(MobEffects.REGENERATION, MobEffects.MOVEMENT_SPEED, MobEffects.DAMAGE_BOOST,
                    MobEffects.DAMAGE_RESISTANCE, MobEffects.FIRE_RESISTANCE, MobEffects.NIGHT_VISION, MobEffects.WATER_BREATHING);
            int lvl = professionLevel(d, Profession.ALCHEMIST);
            Holder<MobEffect> effect = pool.get(p.getRandom().nextInt(pool.size()));
            p.addEffect(fx(effect, (30 + 3 * lvl) * 20, lvl >= 10 ? 1 : 0));
            sound(level, p.position(), SoundEvents.GENERIC_DRINK, 1f, 1f);
            burst(level, ParticleTypes.EFFECT, p, 15);
            return true;
        });

        reg("enchanter_arcane_channel", Resource.NONE, 0, 120, School.ARCANE, Stat.INT, false, (p, d, level, power) -> {
            int lvl = professionLevel(d, Profession.ENCHANTER);
            restore(p, com.alvorecer.rpg.Derived.maxMana(d) * (0.2f + 0.01f * lvl), 0);
            level.sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1.5, p.getZ(), 40, 0.6, 0.6, 0.6, 0.5);
            sound(level, p.position(), SoundEvents.ENCHANTMENT_TABLE_USE, 1f, 1f);
            return true;
        });

        reg("herbalist_bloom", Resource.NONE, 0, 90, School.NATURE, Stat.WIS, false, (p, d, level, power) -> {
            int radius = 2 + professionLevel(d, Profession.HERBALIST) / 5;
            int grown = 0;
            for (BlockPos pos : BlockPos.betweenClosed(p.blockPosition().offset(-radius, -1, -radius), p.blockPosition().offset(radius, 1, radius))) {
                if (BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, pos)) {
                    grown++;
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0);
                }
            }
            if (grown == 0) return fail(p, "nothing_to_grow");
            sound(level, p.position(), SoundEvents.BONE_MEAL_USE, 1f, 1f);
            return true;
        });

        reg("miner_ore_sense", Resource.NONE, 0, 60, School.PHYSICAL, Stat.WIS, false, (p, d, level, power) -> {
            int radius = 8 + professionLevel(d, Profession.MINER) / 2;
            int found = 0;
            BlockPos nearest = null;
            for (BlockPos pos : BlockPos.betweenClosed(p.blockPosition().offset(-radius, -radius, -radius), p.blockPosition().offset(radius, radius, radius))) {
                if (!level.getBlockState(pos).is(Tags.Blocks.ORES)) continue;
                found++;
                level.sendParticles(p, ParticleTypes.WAX_ON, true, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.4, 0.4, 0.4, 0);
                if (nearest == null || pos.distSqr(p.blockPosition()) < nearest.distSqr(p.blockPosition())) nearest = pos.immutable();
            }
            if (nearest == null) return fail(p, "no_ores");
            p.displayClientMessage(Component.translatable("msg.alvorecer.ores_found", found,
                    level.getBlockState(nearest).getBlock().getName(), (int) Math.sqrt(nearest.distSqr(p.blockPosition()))), true);
            sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1f, 0.6f);
            return true;
        });

        reg("hunter_track", Resource.NONE, 0, 60, School.NATURE, Stat.WIS, false, (p, d, level, power) -> {
            int radius = 32 + professionLevel(d, Profession.HUNTER);
            List<Mob> mobs = level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(radius), m -> m.isAlive() && !isAlly(p, m));
            if (mobs.isEmpty()) return fail(p, "no_target");
            mobs.forEach(m -> m.addEffect(fx(MobEffects.GLOWING, 300, 0), p));
            sound(level, p.position(), SoundEvents.FOX_SNIFF, 1f, 1f);
            return true;
        });

        reg("cook_feast", Resource.NONE, 0, 300, School.NATURE, Stat.CON, false, (p, d, level, power) -> {
            int lvl = professionLevel(d, Profession.COOK);
            for (LivingEntity ally : alliesAround(p, 8)) {
                if (ally instanceof Player pl) {
                    pl.getFoodData().eat(6, 0.6f + 0.02f * lvl);
                    burst(level, ParticleTypes.HAPPY_VILLAGER, pl, 8);
                }
            }
            sound(level, p.position(), SoundEvents.PLAYER_BURP, 1f, 1f);
            return true;
        });

        reg("artisan_portable_bench", Resource.NONE, 0, 5, School.PHYSICAL, Stat.INT, false, (p, d, level, power) -> {
            ContainerLevelAccess access = ContainerLevelAccess.create(level, p.blockPosition());
            p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new CraftingMenu(id, inv, access) {
                @Override
                public boolean stillValid(Player player) {
                    return player.isAlive();
                }
            }, Component.translatable("container.crafting")));
            return true;
        });
    }
}
