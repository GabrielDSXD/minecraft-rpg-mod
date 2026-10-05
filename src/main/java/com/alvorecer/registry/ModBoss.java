package com.alvorecer.registry;

import com.alvorecer.entity.boss.DawnAltarBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/** Etapa 7: Guardião do Primeiro Alvorecer (altar, sigilo e materiais). O chefe está em ModMobs e o Coração em ModGear. */
public final class ModBoss {
    public static final DeferredBlock<DawnAltarBlock> ALTAR = ModBlocks.BLOCKS.registerBlock("dawn_altar", DawnAltarBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(-1.0f, 3_600_000f).sound(SoundType.AMETHYST)
                    .lightLevel(s -> 12).noLootTable().pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<?> ALTAR_ITEM = ModItems.ITEMS.registerSimpleBlockItem(ALTAR);
    public static final DeferredItem<Item> DAWN_SIGIL = ModItems.ITEMS.registerSimpleItem("dawn_sigil",
            new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    public static final DeferredItem<Item> DAWN_ESSENCE = ModItems.ITEMS.registerSimpleItem("dawn_essence",
            new Item.Properties().rarity(Rarity.EPIC).fireResistant());

    private ModBoss() {
    }

    public static void init() {
    }
}
