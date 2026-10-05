package com.alvorecer.item.gear;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

/** Arco com multiplicador de dano das flechas. */
public class RpgBowItem extends BowItem {
    private final double damageMultiplier;

    public RpgBowItem(Properties properties, double damageMultiplier) {
        super(properties);
        this.damageMultiplier = damageMultiplier;
    }

    @Override
    public AbstractArrow customArrow(AbstractArrow arrow, ItemStack projectileStack, ItemStack weaponStack) {
        arrow.setBaseDamage(arrow.getBaseDamage() * damageMultiplier);
        return super.customArrow(arrow, projectileStack, weaponStack);
    }
}
