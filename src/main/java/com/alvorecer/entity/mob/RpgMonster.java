package com.alvorecer.entity.mob;

import com.alvorecer.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Base das criaturas hostis do mod: sons próprios e utilitários de telegrafia de ataques. */
public abstract class RpgMonster extends Monster {
    private final String soundKey;

    protected RpgMonster(EntityType<? extends Monster> type, Level level, String soundKey) {
        super(type, level);
        this.soundKey = soundKey;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.get(soundKey + ".ambient");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.get(soundKey + ".hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.get(soundKey + ".death");
    }

    protected void play(String sound, float volume, float pitch) {
        level().playSound(null, getX(), getY(), getZ(), ModSounds.get(sound), getSoundSource(), volume, pitch);
    }

    protected ServerLevel server() {
        return (ServerLevel) level();
    }

    /** Alvo à frente (produto escalar entre o olhar e a direção ao alvo). */
    public static boolean inFront(LivingEntity self, Vec3 point, double minDot) {
        Vec3 look = Vec3.directionFromRotation(0, self.getYRot());
        Vec3 to = point.subtract(self.position());
        Vec3 flat = new Vec3(to.x, 0, to.z);
        return flat.lengthSqr() > 1e-4 && look.dot(flat.normalize()) >= minDot;
    }
}
