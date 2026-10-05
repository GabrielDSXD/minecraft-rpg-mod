package com.alvorecer.entity.mob;

import com.alvorecer.registry.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Dragonete: pequeno dragão voador das montanhas. Sopro de fogo em cone (7 blocos) por 1,5 s quando o alvo está a até
 * 7 blocos; o cone de chamas é visível antes de causar dano. Imune a fogo.
 */
public class WyrmlingEntity extends RpgMonster {
    private static final EntityDataAccessor<Boolean> BREATHING = SynchedEntityData.defineId(WyrmlingEntity.class, EntityDataSerializers.BOOLEAN);
    private int breathCooldown = 60;

    public WyrmlingEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, "wyrmling");
        moveControl = new FlyingMoveControl(this, 15, true);
        setNoGravity(true);
        xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.5).add(Attributes.ATTACK_DAMAGE, 5).add(Attributes.ARMOR, 4)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BREATHING, false);
    }

    public boolean isBreathing() {
        return entityData.get(BREATHING);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new BreathGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (breathCooldown > 0) breathCooldown--;
    }

    private final class BreathGoal extends Goal {
        private int ticks;

        BreathGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return t != null && breathCooldown == 0 && distanceToSqr(t) < 49 && hasLineOfSight(t);
        }

        @Override
        public boolean canContinueToUse() {
            return ticks < 30 && getTarget() != null && getTarget().isAlive();
        }

        @Override
        public void start() {
            ticks = 0;
            getNavigation().stop();
            entityData.set(BREATHING, true);
            play("wyrmling.breath", 1.2f, 1f);
        }

        @Override
        public void tick() {
            ticks++;
            LivingEntity t = getTarget();
            if (t == null) {
                ticks = 30;
                return;
            }
            getLookControl().setLookAt(t, 30, 30);
            Vec3 head = position().add(0, getBbHeight() * 0.6, 0);
            Vec3 dir = t.getEyePosition().subtract(head).normalize();
            for (int i = 0; i < 6; i++) {
                Vec3 spread = dir.add((random.nextDouble() - 0.5) * 0.3, (random.nextDouble() - 0.5) * 0.3, (random.nextDouble() - 0.5) * 0.3).normalize();
                server().sendParticles(ParticleTypes.FLAME, head.x, head.y, head.z, 0, spread.x, spread.y, spread.z, 0.5);
            }
            if (ticks < 8 || ticks % 4 != 0) return; // 0,4 s de aviso antes do dano
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(7), e -> e != WyrmlingEntity.this)) {
                Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(head);
                if (to.length() > 7 || to.normalize().dot(dir) < 0.85 || !hasLineOfSight(e)) continue;
                if (e.hurt(damageSources().source(ModDamageTypes.FIRE, WyrmlingEntity.this), 2.5f)) e.setRemainingFireTicks(60);
            }
        }

        @Override
        public void stop() {
            entityData.set(BREATHING, false);
            breathCooldown = 100;
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }
}
