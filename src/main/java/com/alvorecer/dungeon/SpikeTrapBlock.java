package com.alvorecer.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;

/**
 * Placa de Espinhos: pisada, estala e brilha por {@link #WARN_TICKS} (aviso) e então ergue espinhos que causam
 * {@link #DAMAGE} a quem estiver em cima, recolhendo depois de {@link #UP_TICKS}. Andar agachado não dispara.
 * STAGE: 0 armada, 1 disparada (aviso), 2 espinhos erguidos.
 */
public class SpikeTrapBlock extends Block {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 2);
    public static final int WARN_TICKS = 12, UP_TICKS = 20;
    public static final float DAMAGE = 6;

    public SpikeTrapBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level.isClientSide || !(entity instanceof LivingEntity living)) return;
        int stage = state.getValue(STAGE);
        if (stage == 0 && !entity.isSteppingCarefully()) {
            level.setBlock(pos, state.setValue(STAGE, 1), 3);
            level.scheduleTick(pos, this, WARN_TICKS);
            level.playSound(null, pos, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, 1f, 0.6f);
        } else if (stage == 2) {
            pierce(living);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(STAGE) == 1) {
            level.setBlock(pos, state.setValue(STAGE, 2), 3);
            level.playSound(null, pos, SoundEvents.POINTED_DRIPSTONE_LAND, SoundSource.BLOCKS, 1.2f, 0.8f);
            level.sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 8, 0.3, 0.1, 0.3, 0.1);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos.above()).expandTowards(0, 0.5, 0))) pierce(e);
            level.scheduleTick(pos, this, UP_TICKS);
        } else if (state.getValue(STAGE) == 2) {
            level.setBlock(pos, state.setValue(STAGE, 0), 3);
        }
    }

    private static void pierce(LivingEntity e) {
        e.hurt(e.damageSources().stalagmite(), DAMAGE);
    }
}
