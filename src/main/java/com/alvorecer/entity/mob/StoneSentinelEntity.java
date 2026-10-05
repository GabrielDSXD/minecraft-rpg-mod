package com.alvorecer.entity.mob;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.EnumSet;

/**
 * Sentinela de Pedra: guardiã lenta e muito resistente. Golpe no chão: ergue os braços por 1 s (anel de poeira marca a
 * área de 4,5 blocos) e então esmaga, causando 12 de dano e arremessando quem estiver dentro.
 */
public class StoneSentinelEntity extends RpgMonster {
    public static final int SLAM_WINDUP = 20;
    public static final double SLAM_RADIUS = 4.5;
    private static final EntityDataAccessor<Integer> SLAM = SynchedEntityData.defineId(StoneSentinelEntity.class, EntityDataSerializers.INT);
    private int slamCooldown = 60;

    public StoneSentinelEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, "stone_sentinel");
        xpReward = 30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 80).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 10).add(Attributes.ARMOR, 12).add(Attributes.ARMOR_TOUGHNESS, 4)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 24).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SLAM, 0);
    }

    /** Ticks do golpe no chão em andamento (0 = nenhum). */
    public int slamTicks() {
        return entityData.get(SLAM);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SlamGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (slamCooldown > 0) slamCooldown--;
    }

    private final class SlamGoal extends Goal {
        private int ticks;

        SlamGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return t != null && slamCooldown == 0 && onGround() && distanceToSqr(t) < 16;
        }

        @Override
        public boolean canContinueToUse() {
            return ticks <= SLAM_WINDUP;
        }

        @Override
        public void start() {
            ticks = 0;
            getNavigation().stop();
            play("stone_sentinel.ambient", 1.5f, 0.6f);
        }

        @Override
        public void tick() {
            ticks++;
            entityData.set(SLAM, ticks);
            if (ticks < SLAM_WINDUP) {
                if (ticks % 4 == 0) {
                    for (int i = 0; i < 24; i++) {
                        double a = Mth.TWO_PI * i / 24;
                        server().sendParticles(ParticleTypes.CRIT, getX() + Math.cos(a) * SLAM_RADIUS, getY() + 0.1,
                                getZ() + Math.sin(a) * SLAM_RADIUS, 1, 0, 0, 0, 0);
                    }
                }
                return;
            }
            play("stone_sentinel.slam", 1.6f, 0.8f);
            server().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                    getX(), getY() + 0.1, getZ(), 80, SLAM_RADIUS / 2, 0.1, SLAM_RADIUS / 2, 0.2);
            server().sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.2, getZ(), 3, 1.5, 0.1, 1.5, 0);
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(SLAM_RADIUS),
                    e -> e != StoneSentinelEntity.this && !(e instanceof StoneSentinelEntity))) {
                if (e.distanceToSqr(StoneSentinelEntity.this) > SLAM_RADIUS * SLAM_RADIUS || !e.onGround()) continue;
                if (e.hurt(damageSources().mobAttack(StoneSentinelEntity.this), 12)) {
                    e.push(0, 0.7, 0);
                    e.hurtMarked = true;
                }
            }
        }

        @Override
        public void stop() {
            entityData.set(SLAM, 0);
            slamCooldown = 100;
        }
    }
}
