package com.alvorecer.item.gear;

import net.minecraft.ChatFormatting;

/** Raridade dos equipamentos do mod (cor do nome e linha no tooltip). */
public enum GearRarity {
    COMMON(ChatFormatting.WHITE), UNCOMMON(ChatFormatting.GREEN), RARE(ChatFormatting.AQUA),
    EPIC(ChatFormatting.LIGHT_PURPLE), LEGENDARY(ChatFormatting.GOLD);

    public final ChatFormatting color;

    GearRarity(ChatFormatting color) {
        this.color = color;
    }

    public String langKey() {
        return "rarity.alvorecer." + name().toLowerCase();
    }
}
