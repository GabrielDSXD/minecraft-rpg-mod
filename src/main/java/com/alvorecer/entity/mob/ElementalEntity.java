package com.alvorecer.entity.mob;

import com.alvorecer.registry.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Elemental de fogo ou de gelo (variante escolhida pela temperatura do bioma). Flutua, dispara projéteis do seu
 * elemento e, quando o alvo chega a 3 blocos, carrega um pulso em área (1 s de aviso com partículas).
 * Imune ao próprio elemento e recebe dano dobrado do oposto.
 */
public class ElementalEntity extends RpgMonster implements RangedAttackMob {
    private static final EntityDataAccessor<Boolean> FROST = SynchedEntityData.defineId(ElementalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> PULSE = SynchedEntityData.defineId(ElementalEntity.class, EntityDataSerializers.INT);
    private int pulseCooldown = 60;

    public ElementalEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, "elemental");
        moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
        xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 24).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FLYING_SPEED, 0.45).add(Attributes.ATTACK_DAMAGE, 4).add(Attributes.ARMOR, 4)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FROST, false);
        builder.define(PULSE, 0);
    }

    public boolean isFrost() {
        return entityData.get(FROST);
    }

    public void setFrost(boolean frost) {
        entityData.set(FROST, frost);
    }

    /** Ticks de carga do pulso (0 = parado). */
    public int pulseCharge() {
        return entityData.get(PULSE);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 40, 60, 16));
        goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, @Nullable SpawnGroupData data) {
        setFrost(level.getBiome(blockPosition()).value().getBaseTemperature() < 0.3f);
        return super.finalizeSpawn(level, difficulty, type, data);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Frost", isFrost());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setFrost(tag.getBoolean("Frost"));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        Vec3 dir = new Vec3(target.getX() - getX(), target.getY(0.5) - getY(0.5), target.getZ() - getZ()).normalize();
        ElementalBoltEntity bolt = new ElementalBoltEntity(this, dir.scale(0.6), isFrost());
        bolt.setPos(getX(), getY(0.6), getZ());
        level().addFreshEntity(bolt);
        play("elemental.shoot", 1f, isFrost() ? 1.4f : 0.9f);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean fire = source.is(DamageTypeTags.IS_FIRE) || source.is(ModDamageTypes.FIRE);
        boolean cold = source.is(DamageTypeTags.IS_FREEZING) || source.is(ModDamageTypes.COLD);
        if ((fire && !isFrost()) || (cold && isFrost())) return false;
        if ((fire && isFrost()) || (cold && !isFrost())) amount *= 2;
        return super.hurt(source, amount);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            for (int i = 0; i < 2; i++) {
                level().addParticle(isFrost() ? ParticleTypes.SNOWFLAKE : ParticleTypes.FLAME, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0.02, 0);
            }
            return;
        }
        // Flutua levemente
        setDeltaMovement(getDeltaMovement().add(0, Mth.sin(tickCount * 0.1f) * 0.004, 0));
        if (pulseCooldown > 0) pulseCooldown--;
        int charge = pulseCharge();
        LivingEntity t = getTarget();
        if (charge > 0) {
            charge++;
            entityData.set(PULSE, charge);
            for (int i = 0; i < 12; i++) {
                double a = Mth.TWO_PI * i / 12 + tickCount * 0.2;
                server().sendParticles(isFrost() ? ParticleTypes.SNOWFLAKE : ParticleTypes.FLAME,
                        getX() + Math.cos(a) * 3, getY() + 0.2, getZ() + Math.sin(a) * 3, 1, 0, 0, 0, 0);
            }
            if (charge >= 20) {
                entityData.set(PULSE, 0);
                pulseCooldown = 140;
                play("elemental.pulse", 1.2f, isFrost() ? 1.3f : 0.8f);
                server().sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 2, 1, 0.2, 1, 0);
                var type = isFrost() ? ModDamageTypes.COLD : ModDamageTypes.FIRE;
                for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3.2), e -> e != this && !(e instanceof ElementalEntity))) {
                    if (e.distanceToSqr(this) > 3.2 * 3.2 || !hasLineOfSight(e)) continue;
                    if (e.hurt(damageSources().source(type, this), 6)) {
                        if (isFrost()) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2), this);
                        else e.setRemainingFireTicks(80);
                    }
                }
            }
        } else if (t != null && pulseCooldown == 0 && distanceToSqr(t) < 9) {
            entityData.set(PULSE, 1);
        }
    }

    /** O elemental de fogo não queima; o de gelo sofre fogo normalmente (e em dobro, ver hurt). */
    @Override
    public boolean fireImmune() {
        return !isFrost();
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
    }
}
