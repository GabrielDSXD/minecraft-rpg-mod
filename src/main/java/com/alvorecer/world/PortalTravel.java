package com.alvorecer.world;

import com.alvorecer.registry.ModWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Optional;

/**
 * Viagem entre o mundo principal e os Reinos do Éter (escala 1:1). Procura um portal existente num raio de 32 blocos
 * (POI) e, se não houver, constrói um portal de retorno com plataforma num ponto seguro.
 */
public final class PortalTravel {
    public static final int SEARCH_RADIUS = 32;

    private PortalTravel() {
    }

    @Nullable
    public static DimensionTransition destination(ServerLevel from, Entity entity, BlockPos portalPos) {
        boolean inAether = from.dimension() == ModWorld.AETHER;
        ServerLevel to = from.getServer().getLevel(inAether ? Level.OVERWORLD : ModWorld.AETHER);
        if (to == null) return null;
        BlockPos center = to.getWorldBorder().clampToBounds(entity.getX(), entity.getY(), entity.getZ());
        BlockPos portal = findPortal(to, center).orElseGet(() -> buildPortal(to, center));
        return new DimensionTransition(to, Vec3.atBottomCenterOf(exit(to, portal)), Vec3.ZERO, entity.getYRot(), entity.getXRot(),
                DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET));
    }

    /** Sai ao lado do portal (para não voltar sem querer), no nível do chão da moldura; sem espaço, sai no próprio portal. */
    public static BlockPos exit(ServerLevel level, BlockPos portal) {
        BlockState state = level.getBlockState(portal);
        Direction.Axis axis = state.hasProperty(BlockStateProperties.HORIZONTAL_AXIS) ? state.getValue(BlockStateProperties.HORIZONTAL_AXIS) : Direction.Axis.X;
        BlockPos side = portal.relative(axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST);
        if (!level.getBlockState(side).isAir()) return portal;
        return level.getBlockState(side.below()).isAir() && !level.getBlockState(side.below(2)).isAir() ? side.below() : side;
    }

    /** Bloco de portal mais baixo do portal mais próximo. */
    public static Optional<BlockPos> findPortal(ServerLevel level, BlockPos center) {
        PoiManager poi = level.getPoiManager();
        poi.ensureLoadedAndValid(level, center, SEARCH_RADIUS);
        return poi.getInSquare(type -> type.is(ModWorld.DAWN_PORTAL_POI.getKey()), center, SEARCH_RADIUS, PoiManager.Occupancy.ANY)
                .map(PoiRecord::getPos)
                .filter(p -> level.getBlockState(p).is(ModWorld.DAWN_PORTAL.get()))
                .min(Comparator.<BlockPos>comparingDouble(p -> p.distSqr(center)).thenComparingInt(BlockPos::getY))
                .map(p -> {
                    BlockPos q = p;
                    while (level.getBlockState(q.below()).is(ModWorld.DAWN_PORTAL.get())) q = q.below();
                    return q;
                });
    }

    /** Constrói moldura 4×5 (interior 2×3) sobre uma plataforma, procurando terreno sólido perto do centro. */
    public static BlockPos buildPortal(ServerLevel level, BlockPos center) {
        BlockPos base = null;
        outer:
        for (int r = 0; r <= 48; r += 4) {
            for (int dx = -r; dx <= r; dx += 4) {
                for (int dz = -r; dz <= r; dz += 4) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int x = center.getX() + dx, z = center.getZ() + dz;
                    int y, floor;
                    if (level.hasChunk(x >> 4, z >> 4)) {
                        y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                        floor = level.getBlockState(new BlockPos(x, y - 1, z)).liquid() ? Integer.MIN_VALUE : y;
                    } else { // chunk ainda não gerado (getHeight daria a altura mínima): usa o ruído do gerador, sem gerar o chunk
                        var gen = level.getChunkSource().getGenerator();
                        var noise = level.getChunkSource().randomState();
                        y = gen.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, noise);
                        floor = gen.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, noise); // diferente = água por cima
                    }
                    if (y == floor && y > level.getMinBuildHeight() && y < level.getMaxBuildHeight() - 10) {
                        base = new BlockPos(x, y, z);
                        break outer;
                    }
                }
            }
        }
        if (base == null) base = new BlockPos(center.getX(), Math.min(level.getMaxBuildHeight() - 20, 100), center.getZ());
        BlockState stone = ModWorld.DAWNSTONE.get().defaultBlockState();
        for (int x = -2; x <= 3; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlockAndUpdate(base.offset(x, -1, z), stone);
                for (int y = 0; y <= 4; y++) level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
            }
        }
        for (int x = -1; x <= 2; x++) {
            level.setBlockAndUpdate(base.offset(x, 0, 0), stone);
            level.setBlockAndUpdate(base.offset(x, 4, 0), stone);
        }
        for (int y = 1; y <= 3; y++) {
            level.setBlockAndUpdate(base.offset(-1, y, 0), stone);
            level.setBlockAndUpdate(base.offset(2, y, 0), stone);
        }
        new PortalShape(base.offset(0, 1, 0), Direction.Axis.X, 2, 3).fill(level);
        return base.offset(0, 1, 0);
    }
}
