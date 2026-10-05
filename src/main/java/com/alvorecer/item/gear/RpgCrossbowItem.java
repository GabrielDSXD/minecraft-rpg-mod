package com.alvorecer.item.gear;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;

/** Besta com multiplicador de dano dos virotes. */
public class RpgCrossbowItem extends CrossbowItem {
    private final double damageMultiplier;

    public RpgCrossbowItem(Properties properties, double damageMultiplier) {
        super(properties);
        this.damageMultiplier = damageMultiplier;
    }

    @Override
    public AbstractArrow customArrow(AbstractArrow arrow, ItemStack projectileStack, ItemStack weaponStack) {
        arrow.setBaseDamage(arrow.getBaseDamage() * damageMultiplier);
        return super.customArrow(arrow, projectileStack, weaponStack);
    }
}
