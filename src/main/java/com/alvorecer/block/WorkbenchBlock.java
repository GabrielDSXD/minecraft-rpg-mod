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

/** Bancada de Ofício: receitas exclusivas das profissões. */
public class WorkbenchBlock extends Block {
    public WorkbenchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            ModAttachments.get(sp).lastWorkbenchPos = pos.immutable();
            ModNetwork.openScreen(sp, ModNetwork.Screen.WORKBENCH);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
