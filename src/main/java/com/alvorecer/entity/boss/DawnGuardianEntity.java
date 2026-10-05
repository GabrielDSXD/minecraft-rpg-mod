package com.alvorecer.entity.boss;

import com.alvorecer.Alvorecer;
import com.alvorecer.achievement.Achievements;
import com.alvorecer.config.RpgConfig;
import com.alvorecer.entity.mob.ElementalEntity;
import com.alvorecer.entity.mob.RpgMonster;
import com.alvorecer.registry.ModBoss;
import com.alvorecer.registry.ModDamageTypes;
import com.alvorecer.registry.ModGear;
import com.alvorecer.registry.ModMobs;
import com.alvorecer.rpg.Leveling;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Guardião do Primeiro Alvorecer. Fase 1 (100–66%): golpe no chão e raio de energia. Fase 2 (66–33%): + anéis que se
 * expandem (pule para evitar) e invocação de Centelhas. Fase 3 (abaixo de 33%): tudo mais rápido e, a cada 25 s, a
 * Descarga do Primeiro Alvorecer — 5 s de carga no centro da arena e então um clarão que atinge quem estiver à vista;
 * quem se esconde atrás de um pilar não sofre nada. Entre as fases fica invulnerável por 3 s. A vida escala com os
 * jogadores na arena quando ele surge. Ao morrer, cada participante recebe XP e o Coração do Alvorecer.
 */
public class DawnGuardianEntity extends RpgMonster {
    public enum Attack { NONE, SLAM, BEAM, RING, SUMMON, TRANSITION, DISCHARGE }

    public static final int SLAM_WINDUP = 25, BEAM_LOCK = 20, BEAM_WINDUP = 32, RING_WINDUP = 20, SUMMON_CAST = 20,
            TRANSITION_TICKS = 60, DISCHARGE_CHARGE = 100, DISCHARGE_INTERVAL = 500;
    public static final double SLAM_RADIUS = 6, BEAM_RANGE = 32, BEAM_WIDTH = 0.6, RING_SPEED = 0.6, RING_MAX = 20,
            RING_BAND = 0.9, LEASH = 24, ARENA_RADIUS = 40;
    public static final float SLAM_DAMAGE = 16, BEAM_DAMAGE = 14, RING_DAMAGE = 12;
    public static final double PHASE2 = 2 / 3.0, PHASE3 = 1 / 3.0;
    private static final ResourceLocation SCALE_ID = ResourceLocation.fromNamespaceAndPath(Alvorecer.MODID, "guardian_party");

    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(DawnGuardianEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ATTACK = SynchedEntityData.defineId(DawnGuardianEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ATTACK_TICKS = SynchedEntityData.defineId(DawnGuardianEntity.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.translatable("entity.alvorecer.dawn_guardian"),
            BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);
    private final Set<UUID> participants = new HashSet<>();
    private final List<UUID> adds = new ArrayList<>();
    private final Set<UUID> ringHits = new HashSet<>();
    @Nullable
    private BlockPos home;
    private boolean scaled, fromAltar;
    private int attackCooldown = 40, dischargeTimer = -1, absentTicks, summonCooldown;
    private double ringRadius;
    private Vec3 ringCenter = Vec3.ZERO, beamTarget = Vec3.ZERO;

    public DawnGuardianEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, "dawn_guardian");
        xpReward = 500;
        setPersistenceRequired();
        bossEvent.setDarkenScreen(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 800).add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 14).add(Attributes.ARMOR, 10).add(Attributes.ARMOR_TOUGHNESS, 6)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 48).add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, 1);
        builder.define(ATTACK, 0);
        builder.define(ATTACK_TICKS, 0);
    }

    public int phase() {
        return entityData.get(PHASE);
    }

    public Attack attack() {
        return Attack.values()[entityData.get(ATTACK)];
    }

    public int attackTicks() {
        return entityData.get(ATTACK_TICKS);
    }

    public boolean busy() {
        return attack() != Attack.NONE;
    }

    public boolean invulnerablePhase() {
        Attack a = attack();
        return a == Attack.TRANSITION || a == Attack.DISCHARGE;
    }

    @Nullable
    public BlockPos home() {
        return home;
    }

    /** Centro da arena (normalmente o altar): de onde sai a Descarga e para onde volta se for afastado. */
    public void setHome(BlockPos pos, boolean fromAltar) {
        this.home = pos.immutable();
        this.fromAltar = fromAltar;
    }

    public List<UUID> adds() {
        return adds;
    }

    public double ringRadius() {
        return ringRadius;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true) {
            @Override
            public boolean canUse() {
                return !busy() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !busy() && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean canChangeDimensions(Level from, Level to) {
        return false;
    }

    // ------------------------------------------------------------ vida, fases e invulnerabilidade

    /** Vida mínima enquanto a fase atual não termina: um golpe grande não pula a transição. */
    @Override
    public void setHealth(float health) {
        if (level() != null && !level().isClientSide && entityData != null) {
            double floor = switch (phase()) {
                case 1 -> PHASE2;
                case 2 -> PHASE3;
                default -> 0;
            };
            if (floor > 0) health = Math.max(health, (float) (getMaxHealth() * floor));
        }
        super.setHealth(health);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && invulnerablePhase() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (source.getEntity() instanceof Player) play("dawn_guardian.hurt", 0.6f, 1.8f);
            return false;
        }
        if (source.getEntity() instanceof ServerPlayer p) participants.add(p.getUUID());
        return super.hurt(source, amount);
    }

    private void scaleToPlayers() {
        scaled = true;
        int players = Math.max(1, arenaPlayers().size());
        AttributeInstance hp = getAttribute(Attributes.MAX_HEALTH);
        hp.setBaseValue(RpgConfig.BOSS_HEALTH.get());
        hp.removeModifier(SCALE_ID);
        double extra = RpgConfig.BOSS_HEALTH_PER_PLAYER.get() * (players - 1);
        if (extra > 0) hp.addPermanentModifier(new AttributeModifier(SCALE_ID, extra, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        super.setHealth(getMaxHealth());
    }

    // ------------------------------------------------------------ ciclo

    private ServerLevel sl() {
        return (ServerLevel) level();
    }

    private Vec3 homeCenter() {
        BlockPos h = home != null ? home : blockPosition();
        return Vec3.atBottomCenterOf(h);
    }

    public List<ServerPlayer> arenaPlayers() {
        Vec3 c = homeCenter();
        return sl().players().stream().filter(p -> !p.isSpectator() && p.isAlive() && p.distanceToSqr(c) <= ARENA_RADIUS * ARENA_RADIUS).toList();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (home == null) home = blockPosition();
        if (!scaled) scaleToPlayers();
        bossEvent.setProgress(getHealth() / getMaxHealth());
        List<ServerPlayer> players = arenaPlayers();
        if (players.isEmpty()) {
            if (++absentTicks > 600) resetFight();
            return;
        }
        absentTicks = 0;
        if (getTarget() == null || !getTarget().isAlive()) setTarget(nearest(players));
        if (attack() != Attack.DISCHARGE && distanceToSqr(homeCenter()) > LEASH * LEASH) {
            teleportTo(homeCenter().x, homeCenter().y, homeCenter().z);
            play("dawn_guardian.roar", 1.5f, 1.2f);
        }
        if (ringRadius > 0) advanceRing();
        adds.removeIf(id -> {
            Entity e = sl().getEntity(id);
            return e == null || !e.isAlive();
        });
        if (summonCooldown > 0) summonCooldown--;
        if (dischargeTimer > 0) dischargeTimer--;

        Attack a = attack();
        if (a == Attack.NONE) {
            if (phase() == 1 && getHealth() <= getMaxHealth() * PHASE2 + 0.01f) {
                begin(Attack.TRANSITION);
            } else if (phase() == 2 && getHealth() <= getMaxHealth() * PHASE3 + 0.01f) {
                begin(Attack.TRANSITION);
            } else if (phase() == 3 && dischargeTimer == 0) {
                begin(Attack.DISCHARGE);
            } else if (attackCooldown > 0) {
                attackCooldown--;
            } else {
                chooseAttack(players);
            }
            return;
        }
        int t = attackTicks() + 1;
        entityData.set(ATTACK_TICKS, t);
        getNavigation().stop();
        LivingEntity target = getTarget();
        if (target != null && a != Attack.DISCHARGE) getLookControl().setLookAt(target, 30, 30);
        switch (a) {
            case SLAM -> tickSlam(t);
            case BEAM -> tickBeam(t, target);
            case RING -> tickRing(t);
            case SUMMON -> tickSummon(t, players.size());
            case TRANSITION -> tickTransition(t, players.size());
            case DISCHARGE -> tickDischarge(t, players);
            default -> {
            }
        }
    }

    @Nullable
    private LivingEntity nearest(List<ServerPlayer> players) {
        ServerPlayer best = null;
        for (ServerPlayer p : players) if (!p.isCreative() && (best == null || p.distanceToSqr(this) < best.distanceToSqr(this))) best = p;
        return best;
    }

    private void chooseAttack(List<ServerPlayer> players) {
        LivingEntity target = getTarget();
        double dist = target == null ? 99 : distanceTo(target);
        int phase = phase();
        List<Attack> options = new ArrayList<>();
        if (dist < SLAM_RADIUS - 1) options.add(Attack.SLAM);
        if (target != null && dist > 4) options.add(Attack.BEAM);
        if (phase >= 2) {
            options.add(Attack.RING);
            if (summonCooldown == 0 && adds.size() < 2 + players.size()) options.add(Attack.SUMMON);
        }
        if (options.isEmpty()) {
            attackCooldown = 10;
            return;
        }
        begin(options.get(random.nextInt(options.size())));
    }

    /** Começa um ataque (ou transição): para de andar e zera o contador sincronizado com o cliente (animações). */
    public void begin(Attack attack) {
        entityData.set(ATTACK, attack.ordinal());
        entityData.set(ATTACK_TICKS, 0);
        getNavigation().stop();
        switch (attack) {
            case SLAM -> play("dawn_guardian.roar", 1.4f, 1.3f);
            case BEAM -> {
                play("dawn_guardian.beam_charge", 1.6f, 1f);
                if (getTarget() != null) beamTarget = getTarget().getEyePosition();
            }
            case RING -> play("dawn_guardian.ring", 1.2f, 0.7f);
            case SUMMON -> play("dawn_guardian.summon", 1.5f, 1f);
            case TRANSITION -> {
                play("dawn_guardian.roar", 2.5f, 0.7f);
                announce("msg.alvorecer.guardian_phase", ChatFormatting.GOLD);
            }
            case DISCHARGE -> {
                Vec3 c = homeCenter();
                teleportTo(c.x, c.y + 2, c.z);
                setNoGravity(true);
                setDeltaMovement(Vec3.ZERO);
                play("dawn_guardian.discharge_charge", 3f, 0.6f);
                bossEvent.setName(Component.translatable("msg.alvorecer.guardian_hide").withStyle(ChatFormatting.RED));
                bossEvent.setColor(BossEvent.BossBarColor.RED);
            }
            default -> {
            }
        }
    }

    private void finish(int cooldown) {
        entityData.set(ATTACK, Attack.NONE.ordinal());
        entityData.set(ATTACK_TICKS, 0);
        int phase = phase();
        attackCooldown = phase == 1 ? cooldown : phase == 2 ? cooldown * 3 / 4 : cooldown / 2;
    }

    private void announce(String key, ChatFormatting color, Object... args) {
        for (ServerPlayer p : arenaPlayers()) p.displayClientMessage(Component.translatable(key, args).withStyle(color), true);
    }

    private void particle(ParticleOptions type, double x, double y, double z, int count, double spread, double speed) {
        sl().sendParticles(type, x, y, z, count, spread, spread, spread, speed);
    }

    private void circle(ParticleOptions type, Vec3 center, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double a = Mth.TWO_PI * i / points;
            sl().sendParticles(type, center.x + Math.cos(a) * radius, center.y + 0.15, center.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
    }

    private boolean ally(Entity e) {
        return e == this || e instanceof DawnGuardianEntity || adds.contains(e.getUUID());
    }

    // ------------------------------------------------------------ ataques

    /** Golpe no chão: anel de aviso por {@link #SLAM_WINDUP} ticks, então dano e arremesso em {@link #SLAM_RADIUS} blocos. */
    private void tickSlam(int t) {
        if (t < SLAM_WINDUP) {
            if (t % 3 == 0) {
                circle(ParticleTypes.FLAME, position(), SLAM_RADIUS, 40);
                circle(ParticleTypes.SMALL_FLAME, position(), SLAM_RADIUS * t / SLAM_WINDUP, 24);
            }
            return;
        }
        play("dawn_guardian.slam", 2f, 0.8f);
        particle(ParticleTypes.EXPLOSION, getX(), getY() + 0.3, getZ(), 6, SLAM_RADIUS / 3, 0);
        circle(ParticleTypes.LAVA, position(), SLAM_RADIUS, 30);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(SLAM_RADIUS, 2, SLAM_RADIUS), e -> !ally(e))) {
            if (e.distanceToSqr(this) > SLAM_RADIUS * SLAM_RADIUS || !e.onGround()) continue;
            if (e.hurt(damageSources().mobAttack(this), SLAM_DAMAGE)) {
                Vec3 out = e.position().subtract(position()).multiply(1, 0, 1).normalize().scale(0.8);
                e.push(out.x, 0.7, out.z);
                e.hurtMarked = true;
            }
        }
        finish(60);
    }

    /** Raio: mira no alvo por {@link #BEAM_LOCK} ticks, trava a mira e dispara em {@link #BEAM_WINDUP}; blocos param o raio. */
    private void tickBeam(int t, @Nullable LivingEntity target) {
        Vec3 eye = getEyePosition();
        if (t <= BEAM_LOCK && target != null) beamTarget = target.getEyePosition();
        Vec3 dir = beamTarget.subtract(eye).normalize();
        Vec3 end = eye.add(dir.scale(BEAM_RANGE));
        var hit = level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() != HitResult.Type.MISS) end = hit.getLocation();
        if (t < BEAM_WINDUP) {
            if (t % 2 == 0) {
                double len = eye.distanceTo(end);
                for (double d = 1; d < len; d += t <= BEAM_LOCK ? 1.5 : 0.6) {
                    Vec3 p = eye.add(dir.scale(d));
                    sl().sendParticles(t <= BEAM_LOCK ? ParticleTypes.END_ROD : ParticleTypes.FLAME, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                }
            }
            return;
        }
        play("dawn_guardian.beam", 2f, 1f);
        double len = eye.distanceTo(end);
        for (double d = 1; d < len; d += 0.4) {
            Vec3 p = eye.add(dir.scale(d));
            sl().sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.05);
        }
        AABB area = new AABB(eye, end).inflate(BEAM_WIDTH + 1);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, area, e -> !ally(e))) {
            if (e.getBoundingBox().inflate(BEAM_WIDTH).clip(eye, end).isPresent() || e.getBoundingBox().inflate(BEAM_WIDTH).contains(eye)) {
                e.hurt(damageSources().source(ModDamageTypes.RADIANT, this), BEAM_DAMAGE);
            }
        }
        finish(50);
    }

    /** Anel: carga de {@link #RING_WINDUP} ticks e então uma onda que se expande; quem está no chão quando ela passa é atingido. */
    private void tickRing(int t) {
        if (t < RING_WINDUP) {
            if (t % 2 == 0) circle(ParticleTypes.SOUL_FIRE_FLAME, position(), 1.5 + t * 0.05, 16);
            return;
        }
        ringCenter = position();
        ringRadius = 1;
        ringHits.clear();
        play("dawn_guardian.slam", 1.5f, 1.4f);
        finish(55);
    }

    private void advanceRing() {
        ringRadius += RING_SPEED;
        if (ringRadius > RING_MAX) {
            ringRadius = 0;
            return;
        }
        circle(ParticleTypes.FLAME, ringCenter, ringRadius, (int) (ringRadius * 5) + 8);
        AABB box = new AABB(ringCenter, ringCenter).inflate(ringRadius + RING_BAND, 2, ringRadius + RING_BAND);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box, e -> !ally(e))) {
            double d = Math.sqrt(Mth.square(e.getX() - ringCenter.x) + Mth.square(e.getZ() - ringCenter.z));
            if (Math.abs(d - ringRadius) > RING_BAND || !e.onGround() || Math.abs(e.getY() - ringCenter.y) > 1.5 || !ringHits.add(e.getUUID())) continue;
            if (e.hurt(damageSources().source(ModDamageTypes.RADIANT, this), RING_DAMAGE)) {
                Vec3 out = e.position().subtract(ringCenter).multiply(1, 0, 1).normalize().scale(0.6);
                e.push(out.x, 0.4, out.z);
                e.hurtMarked = true;
            }
        }
    }

    private void tickSummon(int t, int players) {
        if (t < SUMMON_CAST) {
            particle(ParticleTypes.FLAME, getX(), getY() + 2.5, getZ(), 4, 0.8, 0.02);
            return;
        }
        summonAdds(1 + players);
        summonCooldown = 600;
        finish(40);
    }

    /** Centelhas: elementais de fogo ao redor do centro da arena. */
    public void summonAdds(int count) {
        Vec3 c = homeCenter();
        for (int i = 0; i < count; i++) {
            ElementalEntity add = ModMobs.ELEMENTAL.get().create(level());
            if (add == null) continue;
            double a = random.nextDouble() * Mth.TWO_PI, r = 4 + random.nextDouble() * 5;
            add.moveTo(c.x + Math.cos(a) * r, c.y + 0.5, c.z + Math.sin(a) * r, random.nextFloat() * 360, 0);
            EventHooks.finalizeMobSpawn(add, sl(), level().getCurrentDifficultyAt(add.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            add.setFrost(false);
            add.setPersistenceRequired();
            add.setTarget(getTarget());
            level().addFreshEntity(add);
            particle(ParticleTypes.FLASH, add.getX(), add.getY() + 1, add.getZ(), 1, 0, 0);
            adds.add(add.getUUID());
        }
    }

    private void tickTransition(int t, int players) {
        if (t % 5 == 0) particle(ParticleTypes.END_ROD, getX(), getY() + 2, getZ(), 12, 1.5, 0.15);
        if (t < TRANSITION_TICKS) return;
        entityData.set(PHASE, phase() + 1);
        bossEvent.setName(Component.translatable("entity.alvorecer.dawn_guardian").append(" — ")
                .append(Component.translatable("msg.alvorecer.guardian_phase_name." + phase())));
        summonAdds(1 + players);
        if (phase() == 3) dischargeTimer = 40;
        finish(30);
    }

    /** Descarga: 5 s de carga com aviso; atinge quem tem linha de visão até o Guardião (pilares protegem). */
    private void tickDischarge(int t, List<ServerPlayer> players) {
        Vec3 core = position().add(0, 2.2, 0);
        if (t < DISCHARGE_CHARGE) {
            if (t % 20 == 0) {
                announce("msg.alvorecer.guardian_discharge", ChatFormatting.RED, (DISCHARGE_CHARGE - t) / 20);
                play("dawn_guardian.discharge_charge", 2.5f, 0.6f + t / (float) DISCHARGE_CHARGE);
            }
            for (int i = 0; i < 6; i++) {
                Vec3 from = core.add(new Vec3(random.nextGaussian(), random.nextGaussian() * 0.5, random.nextGaussian()).normalize().scale(8));
                Vec3 v = core.subtract(from);
                sl().sendParticles(ParticleTypes.END_ROD, from.x, from.y, from.z, 0, v.x, v.y, v.z, 0.12);
            }
            return;
        }
        play("dawn_guardian.discharge", 4f, 1f);
        particle(ParticleTypes.FLASH, core.x, core.y, core.z, 3, 1, 0);
        float damage = RpgConfig.BOSS_DISCHARGE_DAMAGE.get().floatValue();
        for (ServerPlayer p : players) {
            if (!exposed(core, p)) {
                p.displayClientMessage(Component.translatable("msg.alvorecer.guardian_sheltered").withStyle(ChatFormatting.GREEN), true);
                continue;
            }
            Vec3 to = p.position().add(0, 1, 0);
            Vec3 dir = to.subtract(core);
            for (double d = 0; d < dir.length(); d += 0.5) {
                Vec3 q = core.add(dir.normalize().scale(d));
                sl().sendParticles(ParticleTypes.END_ROD, q.x, q.y, q.z, 1, 0, 0, 0, 0);
            }
            p.hurt(damageSources().source(ModDamageTypes.RADIANT, this), damage);
        }
        setNoGravity(false);
        bossEvent.setName(Component.translatable("entity.alvorecer.dawn_guardian").append(" — ")
                .append(Component.translatable("msg.alvorecer.guardian_phase_name.3")));
        bossEvent.setColor(BossEvent.BossBarColor.YELLOW);
        dischargeTimer = DISCHARGE_INTERVAL;
        finish(40);
    }

    /** À vista: algum ponto do corpo (olhos ou peito) sem bloco entre ele e o núcleo do Guardião. */
    public boolean exposed(Vec3 core, LivingEntity target) {
        for (Vec3 point : new Vec3[]{target.getEyePosition(), target.position().add(0, target.getBbHeight() * 0.5, 0)}) {
            if (level().clip(new ClipContext(core, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------ fim da luta

    private void resetFight() {
        for (UUID id : adds) {
            Entity e = sl().getEntity(id);
            if (e != null) e.discard();
        }
        if (fromAltar && home != null) {
            spawnAtLocation(new ItemStack(ModBoss.DAWN_SIGIL.get()));
        }
        bossEvent.removeAllPlayers();
        discard();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level().isClientSide) return;
        for (UUID id : adds) {
            Entity e = sl().getEntity(id);
            if (e != null) e.discard();
        }
        bossEvent.removeAllPlayers();
        Vec3 c = homeCenter();
        long xp = RpgConfig.BOSS_XP.get();
        for (ServerPlayer p : sl().players()) {
            if (p.isSpectator() || p.distanceToSqr(c) > 64 * 64) continue;
            if (!participants.contains(p.getUUID()) && p.distanceToSqr(c) > ARENA_RADIUS * ARENA_RADIUS) continue;
            Leveling.grantXp(p, xp);
            Achievements.grant(p, "guardian");
            boolean hasHeart = p.getInventory().contains(s -> s.is(ModGear.HEART_OF_DAWN.get()));
            ItemStack reward = hasHeart ? new ItemStack(ModBoss.DAWN_ESSENCE.get(), 3) : new ItemStack(ModGear.HEART_OF_DAWN.get());
            if (!p.getInventory().add(reward)) p.drop(reward, false);
            p.sendSystemMessage(Component.translatable(hasHeart ? "msg.alvorecer.guardian_reward_essence" : "msg.alvorecer.guardian_reward_heart")
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    // ------------------------------------------------------------ persistência

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Phase", phase());
        tag.putBoolean("Scaled", scaled);
        tag.putBoolean("FromAltar", fromAltar);
        tag.putInt("DischargeTimer", dischargeTimer);
        if (home != null) tag.put("Home", NbtUtils.writeBlockPos(home));
        ListTag list = new ListTag();
        for (UUID id : participants) list.add(NbtUtils.createUUID(id));
        tag.put("Participants", list);
        ListTag addList = new ListTag();
        for (UUID id : adds) addList.add(NbtUtils.createUUID(id));
        tag.put("Adds", addList);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(PHASE, Math.max(1, tag.getInt("Phase")));
        scaled = tag.getBoolean("Scaled");
        fromAltar = tag.getBoolean("FromAltar");
        dischargeTimer = tag.contains("DischargeTimer") ? tag.getInt("DischargeTimer") : -1;
        home = NbtUtils.readBlockPos(tag, "Home").orElse(null);
        participants.clear();
        for (Tag t : tag.getList("Participants", Tag.TAG_INT_ARRAY)) participants.add(NbtUtils.loadUUID(t));
        adds.clear();
        for (Tag t : tag.getList("Adds", Tag.TAG_INT_ARRAY)) adds.add(NbtUtils.loadUUID(t));
        if (hasCustomName()) bossEvent.setName(getDisplayName());
    }
}
