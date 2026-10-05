package com.alvorecer.world;

import com.alvorecer.config.RpgConfig;
import com.alvorecer.registry.ModWorld;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Optional;

/**
 * Estrutura de um único modelo NBT colocada na superfície. A chance de cada ponto candidato gerar a estrutura vem da
 * configuração (worldgen.&lt;kind&gt;), permitindo ajustar ou desligar acampamentos, torres, santuários, ruínas e arenas.
 */
public class RpgStructure extends Structure {
    public static final MapCodec<RpgStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            ResourceLocation.CODEC.fieldOf("template").forGetter(s -> s.template),
            Codec.STRING.fieldOf("kind").forGetter(s -> s.kind),
            Codec.INT.optionalFieldOf("y_offset", 0).forGetter(s -> s.yOffset)
    ).apply(i, RpgStructure::new));

    private final ResourceLocation template;
    private final String kind;
    private final int yOffset;

    public RpgStructure(StructureSettings settings, ResourceLocation template, String kind, int yOffset) {
        super(settings);
        this.template = template;
        this.kind = kind;
        this.yOffset = yOffset;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        if (ctx.random().nextDouble() >= RpgConfig.structureFrequency(kind)) return Optional.empty();
        int x = ctx.chunkPos().getMiddleBlockX(), z = ctx.chunkPos().getMiddleBlockZ();
        int y = ctx.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
        if (y <= ctx.heightAccessor().getMinBuildHeight() + 2) return Optional.empty(); // vazio entre ilhas
        Rotation rotation = Rotation.getRandom(ctx.random());
        BlockPos pos = new BlockPos(x, y + yOffset, z);
        return Optional.of(new GenerationStub(pos, b -> b.addPiece(new Piece(ctx.structureTemplateManager(), template, pos, rotation))));
    }

    @Override
    public StructureType<?> type() {
        return ModWorld.RPG_STRUCTURE.get();
    }

    public static class Piece extends TemplateStructurePiece {
        public Piece(StructureTemplateManager manager, ResourceLocation template, BlockPos pos, Rotation rotation) {
            super(ModWorld.RPG_PIECE.get(), 0, manager, template, template.toString(), settings(rotation), pos);
        }

        public Piece(StructurePieceSerializationContext ctx, CompoundTag tag) {
            super(ModWorld.RPG_PIECE.get(), tag, ctx.structureTemplateManager(), id -> settings(Rotation.valueOf(tag.getString("Rot"))));
        }

        private static StructurePlaceSettings settings(Rotation rotation) {
            return new StructurePlaceSettings().setRotation(rotation).addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
                    // Lanternas colocadas sobre água de aquífero não viram fontes de água dentro da sala
                    .setLiquidSettings(net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings.IGNORE_WATERLOGGING);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
            super.addAdditionalSaveData(ctx, tag);
            tag.putString("Rot", placeSettings.getRotation().name());
        }

        @Override
        protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
        }
    }
}
