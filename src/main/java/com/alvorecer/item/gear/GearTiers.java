package com.alvorecer.item.gear;

import com.alvorecer.registry.ModGear;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/** Materiais de ferramentas: Prata Lunar (entre ferro e diamante) e Aurorita (acima do diamante). */
public enum GearTiers implements Tier {
    LUNAR_SILVER(640, 7.0f, 2.5f, BlockTags.INCORRECT_FOR_IRON_TOOL, 18, () -> Ingredient.of(ModGear.LUNAR_SILVER_INGOT.get())),
    AURORITE(1900, 9.0f, 4.0f, BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 16, () -> Ingredient.of(ModGear.AURORITE_GEM.get()));

    private final int uses;
    private final float speed;
    private final float damage;
    private final TagKey<Block> incorrect;
    private final int enchantment;
    private final Supplier<Ingredient> repair;

    GearTiers(int uses, float speed, float damage, TagKey<Block> incorrect, int enchantment, Supplier<Ingredient> repair) {
        this.uses = uses;
        this.speed = speed;
        this.damage = damage;
        this.incorrect = incorrect;
        this.enchantment = enchantment;
        this.repair = repair;
    }

    @Override
    public int getUses() {
        return uses;
    }

    @Override
    public float getSpeed() {
        return speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return damage;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return incorrect;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantment;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repair.get();
    }
}
