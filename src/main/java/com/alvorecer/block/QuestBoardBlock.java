package com.alvorecer.block;

import com.alvorecer.network.ModNetwork;
import com.alvorecer.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Mural de Missões: abre a lista de missões; aceitar e entregar só funcionam perto dele. */
public class QuestBoardBlock extends Block {
    public QuestBoardBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            ModAttachments.get(sp).lastBoardPos = pos.immutable();
            ModNetwork.openScreen(sp, ModNetwork.Screen.QUEST_BOARD);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
