package com.alvorecer.entity.mob;

import com.alvorecer.registry.ModGear;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Esqueleto Guerreiro: morto-vivo com espada e escudo. Bloqueia 70% dos projéteis vindos da frente e faz uma
 * investida telegrafada (0,5 s parado brilhando) quando o alvo está entre 5 e 12 blocos. Queima ao sol.
 */
public class SkeletonWarriorEntity extends RpgMonster {
    private static final EntityDataAccessor<Integer> CHARGE = SynchedEntityData.defineId(SkeletonWarriorEntity.class, EntityDataSerializers.INT);
    private int chargeCooldown = 60;

    public SkeletonWarriorEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, "skeleton_warrior");
        xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 26).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 5).add(Attributes.ARMOR, 6).add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(Attributes.FOLLOW_RANGE, 28);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARGE, 0);
    }

    /** 0 = normal, 1 = preparando, 2 = investindo (para a animação). */
    public int chargeState() {
        return entityData.get(CHARGE);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new ChargeGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, @Nullable SpawnGroupData data) {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(random.nextFloat() < 0.1f ? ModGear.LUNAR_SWORD.get() : Items.IRON_SWORD));
        setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        setDropChance(EquipmentSlot.MAINHAND, 0.06f);
        setDropChance(EquipmentSlot.OFFHAND, 0.04f);
        return super.finalizeSpawn(level, difficulty, type, data);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && source.getDirectEntity() instanceof Projectile && source.getSourcePosition() != null
                && inFront(this, source.getSourcePosition(), 0.3) && random.nextFloat() < 0.7f) {
            play("skeleton_warrior.block", 1f, 1f);
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void aiStep() {
        if (isAlive() && isSunBurnTick() && getItemBySlot(EquipmentSlot.HEAD).isEmpty()) setRemainingFireTicks(160);
        super.aiStep();
        if (chargeCooldown > 0) chargeCooldown--;
    }

    private final class ChargeGoal extends Goal {
        private int ticks;
        private Vec3 dir;
        private boolean hit;

        ChargeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            if (t == null || chargeCooldown > 0 || !onGround()) return false;
            double d = distanceTo(t);
            return d >= 5 && d <= 12 && hasLineOfSight(t);
        }

        @Override
        public void start() {
            ticks = 0;
            hit = false;
            entityData.set(CHARGE, 1);
            getNavigation().stop();
        }

        @Override
        public boolean canContinueToUse() {
            return ticks < 30 && getTarget() != null && getTarget().isAlive();
        }

        @Override
        public void tick() {
            ticks++;
            LivingEntity t = getTarget();
            if (ticks < 10) {
                getLookControl().setLookAt(t, 30, 30);
                server().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 1, getZ(), 2, 0.3, 0.5, 0.3, 0.01);
                return;
            }
            if (ticks == 10) {
                dir = new Vec3(t.getX() - getX(), 0, t.getZ() - getZ()).normalize();
                entityData.set(CHARGE, 2);
                play("skeleton_warrior.charge", 1f, 1f);
            }
            setDeltaMovement(dir.x * 0.9, getDeltaMovement().y, dir.z * 0.9);
            hurtMarked = true;
            server().sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.2, getZ(), 1, 0.2, 0, 0.2, 0);
            if (!hit && distanceToSqr(t) < 3.0) {
                t.hurt(damageSources().mobAttack(SkeletonWarriorEntity.this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5f);
                t.knockback(1.0, -dir.x, -dir.z);
                hit = true;
                ticks = 30;
            }
            if (horizontalCollision) ticks = 30;
        }

        @Override
        public void stop() {
            entityData.set(CHARGE, 0);
            chargeCooldown = 120;
        }
    }
}
