package com.alvorecer.registry;

import com.alvorecer.Alvorecer;
import com.alvorecer.item.DawnKeyItem;
import com.alvorecer.world.DawnPortalBlock;
import com.alvorecer.world.RpgStructure;
import com.google.common.collect.ImmutableSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Etapa 5: Reinos do Éter, Portal do Alvorecer e estruturas. */
public final class ModWorld {
    public static final DeferredRegister<PoiType> POI = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, Alvorecer.MODID);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Alvorecer.MODID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Alvorecer.MODID);

    public static final ResourceKey<Level> AETHER = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(Alvorecer.MODID, "aether_realms"));

    public static final DeferredBlock<Block> DAWNSTONE = ModBlocks.BLOCKS.registerSimpleBlock("dawnstone", BlockBehaviour.Properties.of()
            .mapColor(MapColor.GOLD).instrument(NoteBlockInstrument.BASEDRUM).strength(3.0f, 9.0f).sound(SoundType.STONE)
            .lightLevel(s -> 4).requiresCorrectToolForDrops());
    public static final DeferredBlock<DawnPortalBlock> DAWN_PORTAL = ModBlocks.BLOCKS.registerBlock("dawn_portal", DawnPortalBlock::new,
            BlockBehaviour.Properties.of().noCollission().strength(-1.0f).sound(SoundType.GLASS).lightLevel(s -> 11)
                    .pushReaction(PushReaction.BLOCK).noLootTable());
    public static final DeferredItem<?> DAWNSTONE_ITEM = ModItems.ITEMS.registerSimpleBlockItem(DAWNSTONE);
    public static final DeferredItem<DawnKeyItem> DAWN_KEY = ModItems.ITEMS.register("dawn_key",
            () -> new DawnKeyItem(new Item.Properties().durability(8).rarity(Rarity.RARE)));

    public static final DeferredHolder<PoiType, PoiType> DAWN_PORTAL_POI = POI.register("dawn_portal",
            () -> new PoiType(ImmutableSet.copyOf(DAWN_PORTAL.get().getStateDefinition().getPossibleStates()), 0, 1));

    public static final DeferredHolder<StructureType<?>, StructureType<RpgStructure>> RPG_STRUCTURE = STRUCTURE_TYPES.register("template",
            () -> () -> RpgStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> RPG_PIECE = PIECES.register("template",
            () -> (StructurePieceType) RpgStructure.Piece::new);

    private ModWorld() {
    }

    public static void init() {
    }
}
