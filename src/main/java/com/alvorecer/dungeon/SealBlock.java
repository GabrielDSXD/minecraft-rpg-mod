package com.alvorecer.dungeon;

import com.alvorecer.registry.ModDungeon;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Selo da Masmorra, no centro do piso de cada sala com desafio. COMBAT e CHAMPION começam o encontro quando um
 * jogador chega perto; PUZZLE é vencido pelas runas da sala. Vencido, abre os Portões Selados da sala.
 */
public class SealBlock extends BaseEntityBlock {
    public static final MapCodec<SealBlock> CODEC = simpleCodec(SealBlock::new);

    public enum Kind implements StringRepresentable {
        COMBAT, CHAMPION, PUZZLE;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public enum Phase implements StringRepresentable {
        IDLE, ACTIVE, CLEARED;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final EnumProperty<Kind> KIND = EnumProperty.create("kind", Kind.class);
    public static final EnumProperty<Phase> PHASE = EnumProperty.create("phase", Phase.class);

    public SealBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(KIND, Kind.COMBAT).setValue(PHASE, Phase.IDLE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(KIND, PHASE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SealBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModDungeon.SEAL_BE.get(),
                (l, pos, s, be) -> be.tick((ServerLevel) l, s));
    }
}
