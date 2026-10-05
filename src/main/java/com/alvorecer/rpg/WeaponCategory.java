package com.alvorecer.rpg;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

/** Categorias de arma usadas pelas proficiências de classe. */
public enum WeaponCategory {
    NONE, SWORD, DAGGER, AXE, MACE, HAMMER, SPEAR, BOW, CROSSBOW, STAFF, WAND, SHIELD;

    public boolean heavy() {
        return this == AXE || this == MACE || this == HAMMER;
    }

    public String langKey() {
        return "weapon.alvorecer." + name().toLowerCase();
    }

    /** Itens do mod implementam {@link Categorized}; itens vanilla são classificados pelo tipo. */
    public static WeaponCategory of(ItemStack stack) {
        var item = stack.getItem();
        if (item instanceof Categorized c) return c.weaponCategory();
        if (item instanceof SwordItem) return SWORD;
        if (item instanceof AxeItem) return AXE;
        if (item instanceof MaceItem) return MACE;
        if (item instanceof TridentItem) return SPEAR;
        if (item instanceof BowItem) return BOW;
        if (item instanceof CrossbowItem) return CROSSBOW;
        if (item instanceof ShieldItem) return SHIELD;
        return NONE;
    }

    public interface Categorized {
        WeaponCategory weaponCategory();
    }
}
