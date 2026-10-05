package com.alvorecer.entity.boss;

import com.alvorecer.ability.Scheduler;
import com.alvorecer.registry.ModBoss;
import com.alvorecer.registry.ModMobs;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.event.EventHooks;

/**
 * Altar do Alvorecer, no centro da Arena Celeste. Usar o Sigilo do Primeiro Alvorecer nele desperta o Guardião depois de
 * {@link #SUMMON_DELAY} ticks de aviso. Só um Guardião por arena: com outro vivo a 64 blocos, o sigilo não é gasto.
 */
public class DawnAltarBlock extends Block {
    public static final int SUMMON_DELAY = 60;

    public DawnAltarBlock(Properties properties) {
        super(properties);
    }

    public static boolean guardianNear(Level level, BlockPos pos) {
        return !level.getEntitiesOfClass(DawnGuardianEntity.class, new AABB(pos).inflate(64)).isEmpty();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModBoss.DAWN_SIGIL.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (guardianNear(level, pos)) {
            player.displayClientMessage(Component.translatable("msg.alvorecer.guardian_awake").withStyle(ChatFormatting.RED), true);
            return ItemInteractionResult.FAIL;
        }
        stack.consume(1, player);
        summon((ServerLevel) level, pos);
        return ItemInteractionResult.CONSUME;
    }

    /** Coluna de luz por 3 s e então o Guardião surge sobre o altar. */
    public static void summon(ServerLevel level, BlockPos altar) {
        level.playSound(null, altar, SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 3f, 0.5f);
        for (Player p : level.getEntitiesOfClass(Player.class, new AABB(altar).inflate(48))) {
            p.displayClientMessage(Component.translatable("msg.alvorecer.guardian_summon").withStyle(ChatFormatting.GOLD), true);
        }
        for (int i = 1; i <= SUMMON_DELAY / 5; i++) {
            int step = i;
            Scheduler.after(step * 5, () -> level.sendParticles(ParticleTypes.END_ROD, altar.getX() + 0.5, altar.getY() + 1 + step * 0.4,
                    altar.getZ() + 0.5, 20, 0.6, 0.3, 0.6, 0.05));
        }
        Scheduler.after(SUMMON_DELAY, () -> {
            if (guardianNear(level, altar)) return;
            DawnGuardianEntity boss = ModMobs.DAWN_GUARDIAN.get().create(level);
            if (boss == null) return;
            BlockPos at = altar.above();
            boss.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
            EventHooks.finalizeMobSpawn(boss, level, level.getCurrentDifficultyAt(at), MobSpawnType.TRIGGERED, null);
            boss.setHome(at, true);
            level.addFreshEntity(boss);
            level.sendParticles(ParticleTypes.FLASH, at.getX() + 0.5, at.getY() + 2, at.getZ() + 0.5, 3, 0.5, 1, 0.5, 0);
            level.playSound(null, at, com.alvorecer.registry.ModSounds.get("dawn_guardian.roar"), SoundSource.HOSTILE, 4f, 0.8f);
        });
    }
}
