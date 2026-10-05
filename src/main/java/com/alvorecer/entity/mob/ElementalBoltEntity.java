package com.alvorecer.entity.mob;

import com.alvorecer.registry.ModDamageTypes;
import com.alvorecer.registry.ModMobs;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Projétil do Elemental: fogo incendeia, gelo causa Lentidão II. Não quebra blocos. */
public class ElementalBoltEntity extends AbstractHurtingProjectile {
    private static final EntityDataAccessor<Boolean> FROST = SynchedEntityData.defineId(ElementalBoltEntity.class, EntityDataSerializers.BOOLEAN);

    public ElementalBoltEntity(EntityType<? extends AbstractHurtingProjectile> type, Level level) {
        super(type, level);
    }

    public ElementalBoltEntity(LivingEntity owner, Vec3 movement, boolean frost) {
        super(ModMobs.ELEMENTAL_BOLT.get(), owner, movement, owner.level());
        entityData.set(FROST, frost);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FROST, false);
    }

    public boolean isFrost() {
        return entityData.get(FROST);
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return isFrost() ? ParticleTypes.SNOWFLAKE : ParticleTypes.FLAME;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide || !(result.getEntity() instanceof LivingEntity target)) return;
        var type = isFrost() ? ModDamageTypes.COLD : ModDamageTypes.FIRE;
        if (target.hurt(damageSources().source(type, this, getOwner()), 5)) {
            if (isFrost()) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), getOwner());
            else target.setRemainingFireTicks(60);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) discard();
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
