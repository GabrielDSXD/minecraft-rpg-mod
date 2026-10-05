package com.alvorecer.registry;

import com.alvorecer.Alvorecer;
import com.alvorecer.block.QuestBoardBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Alvorecer.MODID);

    public static final DeferredBlock<QuestBoardBlock> QUEST_BOARD = BLOCKS.registerBlock("quest_board", QuestBoardBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredItem<BlockItem> QUEST_BOARD_ITEM = ModItems.ITEMS.registerSimpleBlockItem(QUEST_BOARD);

    private ModBlocks() {
    }
}
