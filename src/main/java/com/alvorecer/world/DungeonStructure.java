package com.alvorecer.world;

import com.alvorecer.Alvorecer;
import com.alvorecer.config.RpgConfig;
import com.alvorecer.dungeon.Dungeons;
import com.alvorecer.dungeon.RuneBlock;
import com.alvorecer.registry.ModDungeon;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.List;
import java.util.Optional;

/**
 * Masmorra: entrada na superfície, poço com escada e 7 salas de 15×9×15 encadeadas por um passeio aleatório em grade,
 * {@link #DEPTH} blocos abaixo da superfície. Ordem: início, combate, armadilha, enigma, combate, campeão, tesouro.
 * Os portões depois das salas de combate, enigma e campeão começam selados.
 */
public class DungeonStructure extends Structure {
    public static final MapCodec<DungeonStructure> CODEC = simpleCodec(DungeonStructure::new);
    public static final int CELL = 15, ROOM_HEIGHT = 9, DEPTH = 24;

    public enum Room {
        START(false), COMBAT(true), TRAP(false), PUZZLE(true), CHAMPION(true), TREASURE(false);
        public final boolean sealsExit;

        Room(boolean sealsExit) {
            this.sealsExit = sealsExit;
        }

        public ResourceLocation template() {
            return ResourceLocation.fromNamespaceAndPath(Alvorecer.MODID, "dungeon_" + name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    public static final List<Room> ORDER = List.of(Room.START, Room.COMBAT, Room.TRAP, Room.PUZZLE, Room.COMBAT, Room.CHAMPION, Room.TREASURE);
    public static final ResourceLocation ENTRANCE = ResourceLocation.fromNamespaceAndPath(Alvorecer.MODID, "dungeon_entrance");

    public DungeonStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        if (ctx.random().nextDouble() >= RpgConfig.structureFrequency("dungeon")) return Optional.empty();
        int x = ctx.chunkPos().getMiddleBlockX(), z = ctx.chunkPos().getMiddleBlockZ();
        int y = ctx.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
        int min = ctx.heightAccessor().getMinBuildHeight();
        if (y <= min + 2) return Optional.empty(); // vazio entre ilhas
        int roomY = Math.max(min + 4, y - DEPTH);
        if (roomY + ROOM_HEIGHT + 2 >= y) return Optional.empty();
        List<int[]> cells = Dungeons.layout(ctx.random(), ORDER.size());
        if (cells == null) return Optional.empty();
        return Optional.of(new GenerationStub(new BlockPos(x, y, z), b -> addPieces(b, ctx.structureTemplateManager(), x, y, z, roomY, cells)));
    }

    /** Monta as peças; a sala inicial fica centrada sob a entrada em (x, z). */
    public static void addPieces(StructurePiecesBuilder b, StructureTemplateManager manager, int x, int y, int z, int roomY, List<int[]> cells) {
        BlockPos origin = new BlockPos(x - 7, roomY, z - 7);
        for (int i = 0; i < ORDER.size(); i++) {
            b.addPiece(new RoomPiece(manager, ORDER.get(i).template(), origin.offset(cells.get(i)[0] * CELL, 0, cells.get(i)[1] * CELL)));
        }
        b.addPiece(new RoomPiece(manager, ENTRANCE, new BlockPos(x - 3, y - 1, z - 3)));
        b.addPiece(new ShaftPiece(new BoundingBox(x - 1, roomY + 1, z - 1, x + 1, y - 1, z + 1)));
        for (int i = 0; i + 1 < ORDER.size(); i++) {
            int[] a = cells.get(i), c = cells.get(i + 1);
            BlockPos ra = origin.offset(a[0] * CELL, 0, a[1] * CELL);
            int dx = c[0] - a[0], dz = c[1] - a[1];
            BoundingBox box;
            if (dx != 0) {
                int wx = ra.getX() + (dx > 0 ? CELL - 1 : -1);
                box = new BoundingBox(wx, roomY + 1, ra.getZ() + 6, wx + 1, roomY + 3, ra.getZ() + 8);
            } else {
                int wz = ra.getZ() + (dz > 0 ? CELL - 1 : -1);
                box = new BoundingBox(ra.getX() + 6, roomY + 1, wz, ra.getX() + 8, roomY + 3, wz + 1);
            }
            b.addPiece(new DoorwayPiece(box, ORDER.get(i).sealsExit));
        }
    }

    @Override
    public StructureType<?> type() {
        return ModDungeon.DUNGEON_STRUCTURE.get();
    }

    /** Sala a partir de um molde NBT. Ao gerar, sorteia os alvos do enigma (glifo e runa da mesma coluna X têm o mesmo alvo). */
    public static class RoomPiece extends TemplateStructurePiece {
        public RoomPiece(StructureTemplateManager manager, ResourceLocation template, BlockPos pos) {
            super(ModDungeon.ROOM_PIECE.get(), 0, manager, template, template.toString(), settings(), pos);
        }

        public RoomPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
            super(ModDungeon.ROOM_PIECE.get(), tag, ctx.structureTemplateManager(), id -> settings());
        }

        private static StructurePlaceSettings settings() {
            return new StructurePlaceSettings().addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
                    // Lanternas colocadas sobre água de aquífero não viram fontes de água dentro da sala
                    .setLiquidSettings(net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings.IGNORE_WATERLOGGING);
        }

        /** Alvo 1..3 (nunca 0, o valor inicial das runas), igual para glifo e runa de uma coluna. */
        public static int target(BlockPos roomOrigin, int worldX) {
            return 1 + (int) Math.floorMod(Mth.getSeed(roomOrigin.getX(), roomOrigin.getZ(), worldX), 3L);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                                BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
            super.postProcess(level, structures, generator, random, chunkBox, chunkPos, pivot);
            BoundingBox box = getBoundingBox();
            for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                if (!chunkBox.isInside(p)) continue;
                BlockState s = level.getBlockState(p);
                if (s.getBlock() instanceof RuneBlock) level.setBlock(p, s.setValue(RuneBlock.TARGET, target(templatePosition, p.getX())), 2);
            }
        }

        @Override
        protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
        }
    }

    /** Poço 3×3 com escada de mão, da sala inicial até o piso da entrada. */
    public static class ShaftPiece extends StructurePiece {
        public ShaftPiece(BoundingBox box) {
            super(ModDungeon.SHAFT_PIECE.get(), 0, box);
        }

        public ShaftPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
            super(ModDungeon.SHAFT_PIECE.get(), tag);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                                BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
            BoundingBox box = getBoundingBox();
            int cx = box.getCenter().getX(), cz = box.getCenter().getZ();
            int ceiling = box.minY() + ROOM_HEIGHT - 2; // teto da sala inicial (y local 8)
            BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
            for (int y = box.minY(); y <= box.maxY(); y++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos p = new BlockPos(cx + dx, y, cz + dz);
                        if (!chunkBox.isInside(p)) continue;
                        if (dx == 0 && dz == 0) level.setBlock(p, ladder, 2);
                        else if (y > ceiling && y < box.maxY()) level.setBlock(p, Blocks.STONE_BRICKS.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    /** Passagem 3×3 entre duas salas vizinhas (atravessa as duas paredes): aberta ou fechada por Portões Selados. */
    public static class DoorwayPiece extends StructurePiece {
        private final boolean sealed;

        public DoorwayPiece(BoundingBox box, boolean sealed) {
            super(ModDungeon.DOORWAY_PIECE.get(), 0, box);
            this.sealed = sealed;
        }

        public DoorwayPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
            super(ModDungeon.DOORWAY_PIECE.get(), tag);
            this.sealed = tag.getBoolean("Sealed");
        }

        public boolean sealed() {
            return sealed;
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
            tag.putBoolean("Sealed", sealed);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                                BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
            BlockState state = sealed ? ModDungeon.SEALED_DOOR.get().defaultBlockState() : Blocks.AIR.defaultBlockState();
            BoundingBox box = getBoundingBox();
            for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                if (chunkBox.isInside(p)) level.setBlock(p, state, 2);
            }
        }
    }
}
