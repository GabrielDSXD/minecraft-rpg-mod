package com.alvorecer.rpg;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;

/** Peso de armadura: leve < média < pesada. */
public enum ArmorWeight {
    NONE, LIGHT, MEDIUM, HEAVY;

    public String langKey() {
        return "armor_weight.alvorecer." + name().toLowerCase();
    }

    public static ArmorWeight of(ItemStack stack) {
        if (!(stack.getItem() instanceof ArmorItem armor)) return NONE;
        if (armor instanceof Weighted w) return w.armorWeight();
        var mat = armor.getMaterial();
        if (mat.is(ArmorMaterials.LEATHER) || mat.is(ArmorMaterials.TURTLE)) return LIGHT;
        if (mat.is(ArmorMaterials.CHAIN) || mat.is(ArmorMaterials.GOLD)) return MEDIUM;
        return HEAVY;
    }

    public interface Weighted {
        ArmorWeight armorWeight();
    }
}
