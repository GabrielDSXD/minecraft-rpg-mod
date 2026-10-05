package com.alvorecer.world;

import com.alvorecer.registry.ModWorld;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/**
 * Bloco do Portal do Alvorecer: leva do mundo principal para os Reinos do Éter e de volta. Jogadores atravessam após
 * 3 s dentro do portal (instantâneo no criativo). Some se a moldura de Pedra do Alvorecer for quebrada.
 */
public class DawnPortalBlock extends Block implements Portal {
    public static final MapCodec<DawnPortalBlock> CODEC = simpleCodec(DawnPortalBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final VoxelShape X_SHAPE = Block.box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape Z_SHAPE = Block.box(6, 0, 0, 10, 16, 16);

    public DawnPortalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(AXIS) == Direction.Axis.Z ? Z_SHAPE : X_SHAPE;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction.Axis axis = state.getValue(AXIS);
        boolean relevant = dir.getAxis() == Direction.Axis.Y || dir.getAxis() == axis;
        boolean ok = neighbor.is(this) || neighbor.is(ModWorld.DAWNSTONE.get());
        return relevant && !ok ? Blocks.AIR.defaultBlockState() : super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity.canUsePortal(false)) entity.setAsInsidePortal(this, pos);
    }

    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        if (entity instanceof Player p) return p.getAbilities().invulnerable ? 1 : 60;
        return 0;
    }

    @Override
    public DimensionTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
        return PortalTravel.destination(level, entity, pos);
    }

    @Override
    public Transition getLocalTransition() {
        return Transition.CONFUSION;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 2; i++) {
            float hue = random.nextFloat();
            var dust = new DustParticleOptions(new Vector3f(1f, 0.75f + hue * 0.2f, 0.35f + hue * 0.3f), 1.0f);
            level.addParticle(dust, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0, 0.05, 0);
        }
    }

    @Override
    public net.minecraft.world.item.ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {
        return net.minecraft.world.item.ItemStack.EMPTY;
    }
}
