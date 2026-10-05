package com.alvorecer.world;

import com.alvorecer.registry.ModWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

/**
 * Moldura do Portal do Alvorecer: retângulo de Pedra do Alvorecer com interior vazio de 2×3 até 21×21 blocos
 * (como o portal do Nether, os cantos não são exigidos). Puro em relação ao mundo: só lê blocos.
 */
public record PortalShape(BlockPos bottomLeft, Direction.Axis axis, int width, int height) {
    public static final int MIN_W = 2, MIN_H = 3, MAX = 21;

    private static boolean frame(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(ModWorld.DAWNSTONE.get());
    }

    private static boolean empty(Level level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return s.isAir() || s.is(ModWorld.DAWN_PORTAL.get()) || s.canBeReplaced();
    }

    /** Procura uma moldura válida cujo interior contém {@code inner}, testando os dois eixos. */
    @Nullable
    public static PortalShape find(Level level, BlockPos inner) {
        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            PortalShape s = tryAxis(level, inner, axis);
            if (s != null) return s;
        }
        return null;
    }

    @Nullable
    private static PortalShape tryAxis(Level level, BlockPos inner, Direction.Axis axis) {
        if (!empty(level, inner)) return null;
        Direction left = axis == Direction.Axis.X ? Direction.WEST : Direction.NORTH;
        Direction right = left.getOpposite();
        BlockPos p = inner;
        int guard = 0;
        while (empty(level, p.below()) && guard++ < MAX) p = p.below();
        if (!frame(level, p.below())) return null;
        guard = 0;
        while (empty(level, p.relative(left)) && guard++ < MAX) p = p.relative(left);
        if (!frame(level, p.relative(left))) return null;
        int width = 0;
        while (width <= MAX && empty(level, p.relative(right, width))) {
            if (!frame(level, p.relative(right, width).below())) return null;
            width++;
        }
        if (width < MIN_W || width > MAX || !frame(level, p.relative(right, width))) return null;
        int height = 0;
        outer:
        while (height <= MAX) {
            for (int i = 0; i < width; i++) {
                if (!empty(level, p.relative(right, i).above(height))) break outer;
            }
            if (!frame(level, p.relative(left).above(height)) || !frame(level, p.relative(right, width).above(height))) return null;
            height++;
        }
        if (height < MIN_H || height > MAX) return null;
        for (int i = 0; i < width; i++) if (!frame(level, p.relative(right, i).above(height))) return null;
        return new PortalShape(p, axis, width, height);
    }

    public void fill(Level level) {
        Direction right = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        BlockState portal = ModWorld.DAWN_PORTAL.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_AXIS, axis);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) level.setBlock(bottomLeft.relative(right, i).above(j), portal, 18);
        }
    }
}
