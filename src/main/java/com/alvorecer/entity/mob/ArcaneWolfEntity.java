package com.alvorecer.entity.mob;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Lobo Arcano: caça em matilha e, a cada 6–10 s, teleporta-se para as costas do alvo. */
public class ArcaneWolfEntity extends RpgMonster {
    private int blinkCooldown = 100;

    public ArcaneWolfEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, "arcane_wolf");
        xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ATTACK_DAMAGE, 4).add(Attributes.ARMOR, 2).add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.4f));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (random.nextInt(5) == 0) {
                level().addParticle(ParticleTypes.ENCHANT, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0.1, 0);
            }
            return;
        }
        if (blinkCooldown > 0) {
            blinkCooldown--;
            return;
        }
        LivingEntity t = getTarget();
        if (t == null || distanceTo(t) > 12 || !hasLineOfSight(t)) return;
        Vec3 behind = t.position().add(Vec3.directionFromRotation(0, t.getYRot()).scale(-2));
        if (level().noCollision(this, getBoundingBox().move(behind.subtract(position())))) {
            server().sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 0.5, getZ(), 20, 0.3, 0.3, 0.3, 0.05);
            teleportTo(behind.x, behind.y, behind.z);
            server().sendParticles(ParticleTypes.REVERSE_PORTAL, behind.x, behind.y + 0.5, behind.z, 20, 0.3, 0.3, 0.3, 0.05);
            play("arcane_wolf.blink", 1f, 1f);
        }
        blinkCooldown = 120 + random.nextInt(80);
    }
}
