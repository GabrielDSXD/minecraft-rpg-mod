package com.alvorecer.entity.mob;

import com.alvorecer.registry.ModGear;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Goblin: pequeno, rápido e covarde em bando. Emboscada: quando o alvo não está olhando, salta sobre ele e o primeiro
 * golpe após o salto causa 50% a mais.
 */
public class GoblinEntity extends RpgMonster {
    private static final EntityDataAccessor<Boolean> LEAPING = SynchedEntityData.defineId(GoblinEntity.class, EntityDataSerializers.BOOLEAN);
    private int leapCooldown;

    public GoblinEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, "goblin");
        xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 14).add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 3).add(Attributes.ARMOR, 1).add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LEAPING, false);
    }

    public boolean isLeaping() {
        return entityData.get(LEAPING);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new AmbushLeapGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, @Nullable SpawnGroupData data) {
        if (random.nextFloat() < 0.4f) {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModGear.IRON_DAGGER.get()));
            setDropChance(EquipmentSlot.MAINHAND, 0.05f);
        }
        return super.finalizeSpawn(level, difficulty, type, data);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (leapCooldown > 0) leapCooldown--;
    }

    /** Salta sobre o alvo distraído (3 a 8 blocos, alvo olhando para outro lado). */
    private final class AmbushLeapGoal extends Goal {
        private int ticks;
        private boolean struck;

        AmbushLeapGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            if (t == null || leapCooldown > 0 || !onGround()) return false;
            double dist = distanceTo(t);
            if (dist < 3 || dist > 8 || !hasLineOfSight(t)) return false;
            Vec3 toGoblin = position().subtract(t.position()).normalize();
            return t.getViewVector(1).dot(toGoblin) < 0.3;
        }

        @Override
        public void start() {
            LivingEntity t = getTarget();
            Vec3 dir = new Vec3(t.getX() - getX(), 0, t.getZ() - getZ()).normalize();
            setDeltaMovement(dir.x * 1.1, 0.45, dir.z * 1.1);
            hurtMarked = true;
            leapCooldown = 80;
            ticks = 0;
            struck = false;
            entityData.set(LEAPING, true);
            play("goblin.leap", 1f, 1f + random.nextFloat() * 0.2f);
        }

        @Override
        public boolean canContinueToUse() {
            return ticks < 30 && !(ticks > 3 && onGround());
        }

        @Override
        public void tick() {
            ticks++;
            LivingEntity t = getTarget();
            if (!struck && t != null && distanceToSqr(t) < 2.5) {
                float dmg = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5f;
                t.hurt(damageSources().mobAttack(GoblinEntity.this), dmg);
                server().sendParticles(ParticleTypes.CRIT, t.getX(), t.getY() + 1, t.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
                struck = true;
            }
        }

        @Override
        public void stop() {
            entityData.set(LEAPING, false);
        }
    }
}
