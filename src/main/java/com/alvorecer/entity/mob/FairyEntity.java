package com.alvorecer.entity.mob;

import com.alvorecer.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Fada: criatura pacífica de prados e florestas floridas. Se ferida, cega o agressor por 3 s e se teleporta para longe.
 * Regenera a vida de fadas próximas lentamente.
 */
public class FairyEntity extends PathfinderMob {
    public FairyEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
        xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 6).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FLYING_SPEED, 0.6);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new WaterAvoidingRandomFlyingGoal(this, 0.9));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 6));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && !level().isClientSide && isAlive() && source.getEntity() instanceof LivingEntity attacker) {
            attacker.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), this);
            ServerLevel sl = (ServerLevel) level();
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.3, getZ(), 20, 0.3, 0.3, 0.3, 0.05);
            for (int i = 0; i < 10; i++) {
                Vec3 to = position().add(random.nextGaussian() * 8, random.nextInt(4), random.nextGaussian() * 8);
                if (level().noCollision(this, getBoundingBox().move(to.subtract(position())))) {
                    teleportTo(to.x, to.y, to.z);
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.END_ROD, getRandomX(0.4), getRandomY(), getRandomZ(0.4), 0, -0.02, 0);
            return;
        }
        if (tickCount % 100 == 0) {
            for (FairyEntity f : level().getEntitiesOfClass(FairyEntity.class, getBoundingBox().inflate(6))) f.heal(1);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.get("fairy.ambient");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.get("fairy.hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.get("fairy.death");
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return true;
    }
}
