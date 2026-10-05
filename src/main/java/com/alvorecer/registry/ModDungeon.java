package com.alvorecer.registry;

import com.alvorecer.Alvorecer;
import com.alvorecer.dungeon.RuneBlock;
import com.alvorecer.dungeon.SealBlock;
import com.alvorecer.dungeon.SealBlockEntity;
import com.alvorecer.dungeon.SpikeTrapBlock;
import com.alvorecer.dungeon.VaultBlock;
import com.alvorecer.dungeon.VaultBlockEntity;
import com.alvorecer.world.DungeonStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Etapa 6: masmorras (blocos de mecanismo, estrutura procedural e recompensas). */
public final class ModDungeon {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Alvorecer.MODID);

    /** Blocos de mecanismo são indestrutíveis fora do criativo, como os de uma estrutura de desafio. */
    private static BlockBehaviour.Properties fixed(MapColor color, SoundType sound, int light) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(-1.0f, 3_600_000f).sound(sound).lightLevel(s -> light)
                .noLootTable().pushReaction(PushReaction.BLOCK);
    }

    public static final DeferredBlock<SealBlock> SEAL = ModBlocks.BLOCKS.registerBlock("dungeon_seal", SealBlock::new,
            fixed(MapColor.COLOR_RED, SoundType.STONE, 7));
    public static final DeferredBlock<Block> SEALED_DOOR = ModBlocks.BLOCKS.registerBlock("sealed_door", Block::new,
            fixed(MapColor.COLOR_ORANGE, SoundType.CHAIN, 6).noOcclusion().isViewBlocking((s, l, p) -> false));
    public static final DeferredBlock<RuneBlock> RUNE = ModBlocks.BLOCKS.registerBlock("dungeon_rune", p -> new RuneBlock(false, p),
            fixed(MapColor.STONE, SoundType.STONE, 4));
    public static final DeferredBlock<RuneBlock> GLYPH = ModBlocks.BLOCKS.registerBlock("rune_glyph", p -> new RuneBlock(true, p),
            fixed(MapColor.STONE, SoundType.STONE, 9));
    public static final DeferredBlock<SpikeTrapBlock> SPIKE_TRAP = ModBlocks.BLOCKS.registerBlock("spike_trap", SpikeTrapBlock::new,
            fixed(MapColor.METAL, SoundType.METAL, 0));
    public static final DeferredBlock<VaultBlock> VAULT = ModBlocks.BLOCKS.registerBlock("dungeon_vault", VaultBlock::new,
            fixed(MapColor.GOLD, SoundType.METAL, 10).noOcclusion());

    static {
        for (DeferredBlock<?> b : java.util.List.of(SEAL, SEALED_DOOR, RUNE, GLYPH, SPIKE_TRAP, VAULT)) ModItems.ITEMS.registerSimpleBlockItem(b);
    }

    public static final DeferredItem<Item> SIGIL_FRAGMENT = ModItems.ITEMS.registerSimpleItem("dawn_sigil_fragment",
            new Item.Properties().rarity(Rarity.RARE));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SealBlockEntity>> SEAL_BE = BLOCK_ENTITIES.register("dungeon_seal",
            () -> BlockEntityType.Builder.of(SealBlockEntity::new, SEAL.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VaultBlockEntity>> VAULT_BE = BLOCK_ENTITIES.register("dungeon_vault",
            () -> BlockEntityType.Builder.of(VaultBlockEntity::new, VAULT.get()).build(null));

    public static final DeferredHolder<StructureType<?>, StructureType<DungeonStructure>> DUNGEON_STRUCTURE = ModWorld.STRUCTURE_TYPES.register("dungeon",
            () -> () -> DungeonStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> ROOM_PIECE = ModWorld.PIECES.register("dungeon_room",
            () -> (StructurePieceType) DungeonStructure.RoomPiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> SHAFT_PIECE = ModWorld.PIECES.register("dungeon_shaft",
            () -> (StructurePieceType) DungeonStructure.ShaftPiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> DOORWAY_PIECE = ModWorld.PIECES.register("dungeon_doorway",
            () -> (StructurePieceType) DungeonStructure.DoorwayPiece::new);

    private ModDungeon() {
    }

    public static void init() {
    }
}
