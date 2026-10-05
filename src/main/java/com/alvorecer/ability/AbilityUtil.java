package com.alvorecer.ability;

import com.alvorecer.config.RpgConfig;
import com.alvorecer.registry.ModAttachments;
import com.alvorecer.rpg.Derived;
import com.alvorecer.rpg.PlayerRpgData;
import com.alvorecer.rpg.Stat;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/** Alvos, linha de visão, dano, cura, testes de resistência e efeitos visuais das habilidades. */
public final class AbilityUtil {
    private AbilityUtil() {
    }

    // ---------------------------------------------------------------- aliados e inimigos

    /** Jogadores só são inimigos se o PvP do servidor, das equipes e da configuração permitir. */
    public static boolean canHarmPlayer(ServerPlayer caster, Player other) {
        if (other == caster || other.isSpectator() || other.isCreative()) return false;
        return RpgConfig.PVP_ABILITIES.get() && caster.server.isPvpAllowed() && caster.canHarmPlayer(other)
                && !com.alvorecer.party.Parties.sameParty(caster, other);
    }

    public static boolean isAlly(ServerPlayer caster, Entity e) {
        if (e instanceof Player other && com.alvorecer.party.Parties.sameParty(caster, other)) return true;
        if (e == caster) return true;
        if (e instanceof Player other) return !canHarmPlayer(caster, other);
        if (e instanceof OwnableEntity owned && caster.getUUID().equals(owned.getOwnerUUID())) return true;
        return caster.isAlliedTo(e);
    }

    /**
     * @param area em áreas só são atingidos monstros, jogadores hostis e criaturas que estão mirando o conjurador
     *             (aldeões e animais ficam de fora).
     */
    public static boolean isEnemy(ServerPlayer caster, Entity e, boolean area) {
        if (!(e instanceof LivingEntity le) || !le.isAlive() || e == caster || e instanceof ArmorStand) return false;
        if (le.isInvulnerable() || isAlly(caster, e)) return false;
        if (!area) return true;
        return e instanceof Enemy || e instanceof Player || (e instanceof Mob m && m.getTarget() == caster);
    }

    // ---------------------------------------------------------------- mira

    /** Primeira entidade válida na linha de mira, sem atravessar blocos. */
    @Nullable
    public static LivingEntity raycast(ServerPlayer p, double range, boolean ally) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getViewVector(1);
        Vec3 end = eye.add(look.scale(range));
        var block = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        AABB box = p.getBoundingBox().expandTowards(look.scale(range)).inflate(1);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p.level(), p, eye, end, box,
                e -> ally ? (e != p && e instanceof LivingEntity && isAlly(p, e)) : isEnemy(p, e, false), 0.6f);
        return hit == null ? null : (LivingEntity) hit.getEntity();
    }

    /** Ponto onde a mira encontra um bloco (ou o fim do alcance). */
    public static Vec3 aimPoint(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getViewVector(1).scale(range));
        var block = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        LivingEntity target = raycast(p, range, false);
        if (target != null && target.distanceToSqr(eye) < block.getLocation().distanceToSqr(eye)) return target.position();
        return block.getLocation();
    }

    public static boolean lineOfSight(Entity from, Vec3 origin, Entity to) {
        Vec3 dest = to.position().add(0, to.getBbHeight() * 0.5, 0);
        return from.level().clip(new ClipContext(origin, dest, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, from))
                .getType() == HitResult.Type.MISS;
    }

    public static List<LivingEntity> enemiesAround(ServerPlayer p, Vec3 center, double radius) {
        return p.level().getEntitiesOfClass(LivingEntity.class, AABB.ofSize(center, radius * 2, radius * 2, radius * 2),
                e -> isEnemy(p, e, true) && e.distanceToSqr(center) <= radius * radius && lineOfSight(p, center.add(0, 0.5, 0), e));
    }

    public static List<LivingEntity> enemiesInCone(ServerPlayer p, double range, double halfAngleDeg) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getViewVector(1);
        double cos = Math.cos(Math.toRadians(halfAngleDeg));
        return enemiesAround(p, eye, range).stream().filter(e -> {
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eye).normalize();
            return to.dot(look) >= cos;
        }).toList();
    }

    /** Aliados no raio, incluindo o próprio conjurador. */
    public static List<LivingEntity> alliesAround(ServerPlayer p, double radius) {
        return p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(radius),
                e -> e.isAlive() && isAlly(p, e) && !(e instanceof ArmorStand) && e.distanceToSqr(p) <= radius * radius);
    }

    public static List<LivingEntity> nearest(List<LivingEntity> list, Entity from, int max) {
        return list.stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(from))).limit(max).toList();
    }

    // ---------------------------------------------------------------- dano, cura e testes

    public static DamageSource spell(ServerPlayer p, ResourceKey<DamageType> type) {
        return p.damageSources().source(type, p);
    }

    public static DamageSource physical(ServerPlayer p) {
        return p.damageSources().playerAttack(p);
    }

    /** Aplica dano de habilidade ignorando a invulnerabilidade pós-golpe; contra jogadores usa o multiplicador PvP. */
    public static boolean damage(LivingEntity target, DamageSource source, float amount) {
        if (amount <= 0) return false;
        float a = (float) (amount * RpgConfig.ABILITY_DAMAGE_MULTIPLIER.get());
        if (target instanceof Player && source.getEntity() instanceof Player) a *= RpgConfig.PVP_DAMAGE_MULTIPLIER.get().floatValue();
        target.invulnerableTime = 0;
        return target.hurt(source, a);
    }

    /** Cura de habilidade: × configuração × bônus de cura do conjurador; Luz Persistente (Clérigo) adiciona Regeneração I por 4 s. */
    public static void heal(ServerPlayer caster, LivingEntity target, float amount) {
        PlayerRpgData d = ModAttachments.get(caster);
        target.heal((float) (amount * RpgConfig.ABILITY_DAMAGE_MULTIPLIER.get() * Derived.healingMultiplier(d)));
        if (d.hasTalent("cleric_lingering_light")) target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0), caster);
    }

    /** Aplica efeito negativo; contra jogadores respeita a configuração de efeitos de controle. */
    public static void control(ServerPlayer caster, LivingEntity target, MobEffectInstance effect) {
        if (target instanceof Player && !RpgConfig.PVP_CONTROL_EFFECTS.get()) return;
        target.addEffect(effect, caster);
    }

    /**
     * Teste de resistência: d20 + bônus ≥ CD. Jogadores usam modificador + proficiência da classe;
     * criaturas usam vida máxima / 10 (máx. +8). 20 natural sempre passa, 1 natural sempre falha.
     */
    public static boolean save(LivingEntity target, Stat stat, int dc) {
        int roll = 1 + target.getRandom().nextInt(20);
        if (roll == 20) return true;
        if (roll == 1) return false;
        int bonus;
        if (target instanceof Player pl) {
            PlayerRpgData d = ModAttachments.get(pl);
            bonus = d.hasCharacter() ? Derived.saveBonus(d, stat) : 0;
        } else {
            bonus = Math.min(8, (int) (target.getMaxHealth() / 10));
        }
        return roll + bonus >= dc;
    }

    // ---------------------------------------------------------------- movimento

    /** Destino seguro para teleporte na direção da mira (não atravessa paredes). Nulo se não houver espaço. */
    @Nullable
    public static Vec3 blinkTarget(ServerPlayer p, double distance) {
        Vec3 look = p.getViewVector(1);
        Vec3 eye = p.getEyePosition();
        var hit = p.level().clip(new ClipContext(eye, eye.add(look.scale(distance)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        double max = hit.getLocation().distanceTo(eye) - 0.5;
        for (double dist = max; dist >= 1; dist -= 0.5) {
            Vec3 feet = eye.add(look.scale(dist)).subtract(0, p.getEyeHeight(), 0);
            for (double up : new double[]{0, 1}) {
                Vec3 pos = feet.add(0, up, 0);
                if (p.level().noCollision(p, p.getBoundingBox().move(pos.subtract(p.position())))) return pos;
            }
        }
        return null;
    }

    public static void teleport(ServerPlayer p, Vec3 pos) {
        p.teleportTo(pos.x, pos.y, pos.z);
        p.resetFallDistance();
    }

    /** Impulso: a velocidade é enviada ao cliente via hurtMarked; queda ignorada por 3 s. */
    public static void launch(ServerPlayer p, PlayerRpgData d, Vec3 velocity) {
        p.setDeltaMovement(velocity);
        p.hurtMarked = true;
        d.noFallUntil = p.level().getGameTime() + 60;
    }

    public static Vec3 horizontalLook(ServerPlayer p) {
        Vec3 look = p.getViewVector(1);
        Vec3 flat = new Vec3(look.x, 0, look.z);
        return flat.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : flat.normalize();
    }

    // ---------------------------------------------------------------- visual e som

    public static void sound(ServerLevel level, Vec3 pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    public static void line(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, double step) {
        Vec3 delta = to.subtract(from);
        int n = Math.max(1, (int) (delta.length() / step));
        for (int i = 0; i <= n; i++) {
            Vec3 at = from.add(delta.scale(i / (double) n));
            level.sendParticles(particle, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
    }

    public static void ring(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double a = Mth.TWO_PI * i / points;
            level.sendParticles(particle, center.x + Math.cos(a) * radius, center.y + 0.2, center.z + Math.sin(a) * radius, 1, 0, 0.05, 0, 0.01);
        }
    }

    public static void burst(ServerLevel level, ParticleOptions particle, Entity at, int count) {
        level.sendParticles(particle, at.getX(), at.getY() + at.getBbHeight() * 0.5, at.getZ(), count,
                at.getBbWidth() * 0.5, at.getBbHeight() * 0.4, at.getBbWidth() * 0.5, 0.05);
    }

    public static Vec3 chest(Entity e) {
        return e.position().add(0, e.getBbHeight() * 0.6, 0);
    }
}
