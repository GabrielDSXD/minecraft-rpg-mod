package com.alvorecer.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Enigma rúnico. A Runa Giratória mostra o símbolo RUNE e gira ao ser usada; o Glifo Rúnico (fixo, na parede) mostra o
 * símbolo TARGET da runa da mesma coluna. Os alvos são sorteados por masmorra quando a sala é gerada.
 */
public class RuneBlock extends Block {
    public static final IntegerProperty RUNE = IntegerProperty.create("rune", 0, 3);
    public static final IntegerProperty TARGET = IntegerProperty.create("target", 0, 3);
    private final boolean glyph;

    public RuneBlock(boolean glyph, Properties properties) {
        super(properties);
        this.glyph = glyph;
        registerDefaultState(stateDefinition.any().setValue(RUNE, 0).setValue(TARGET, 0));
    }

    public boolean isGlyph() {
        return glyph;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RUNE, TARGET);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (glyph) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockState next = state.cycle(RUNE);
        level.setBlock(pos, next, 3);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.2f, 0.7f + 0.25f * next.getValue(RUNE));
        if (SealBlockEntity.checkPuzzle((ServerLevel) level, pos)) {
            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1f, 1.4f);
        }
        return InteractionResult.CONSUME;
    }
}
