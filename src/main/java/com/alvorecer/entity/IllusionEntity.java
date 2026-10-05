package com.alvorecer.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

/**
 * Ilusão do Gnomo: cópia visual do conjurador que atrai os inimigos.
 * Não morre, não dropa nada e não é salva: some após {@link #LIFETIME} ticks ou {@link #MAX_HITS} golpes.
 */
public class IllusionEntity extends PathfinderMob {
    public static final int LIFETIME = 160;
    public static final int MAX_HITS = 5;
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(IllusionEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private int hitsTaken;

    public IllusionEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER, Optional.empty());
    }

    public void setOwner(Player owner) {
        entityData.set(OWNER, Optional.of(owner.getUUID()));
        moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), 0);
        setYHeadRot(owner.getYHeadRot());
        setYBodyRot(owner.getYRot());
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ANIMAL_ARMOR) continue;
            setItemSlot(slot, owner.getItemBySlot(slot).copy());
            setDropChance(slot, 0);
        }
    }

    public Optional<UUID> getOwnerId() {
        return entityData.get(OWNER);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (random.nextInt(4) == 0) {
                level().addParticle(ParticleTypes.WITCH, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0.02, 0);
            }
        } else if (tickCount > LIFETIME) {
            vanish();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        if (level().isClientSide) return false;
        if (++hitsTaken >= MAX_HITS) vanish();
        else level().playSound(null, getX(), getY(), getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.NEUTRAL, 0.6f, 1.6f);
        return true;
    }

    public void vanish() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 20, 0.3, 0.6, 0.3, 0.02);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.NEUTRAL, 0.8f, 1.4f);
        }
        discard();
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
