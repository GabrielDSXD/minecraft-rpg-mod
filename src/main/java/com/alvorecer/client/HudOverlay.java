package com.alvorecer.client;

import com.alvorecer.ability.Abilities;
import com.alvorecer.ability.Ability;
import com.alvorecer.ability.AbilityService;
import com.alvorecer.registry.ModAttachments;
import com.alvorecer.rpg.Derived;
import com.alvorecer.rpg.PlayerRpgData;
import com.alvorecer.rpg.Progression;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * HUD: barras de mana/energia/XP no canto inferior esquerdo e a barra de habilidades na lateral esquerda,
 * com tecla, recarga restante e estado (bloqueada / sem recurso).
 */
public final class HudOverlay {
    private HudOverlay() {
    }

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.isSpectator()) return;
        PlayerRpgData d = ModAttachments.get(mc.player);
        if (!d.hasCharacter()) return;
        Font font = mc.font;
        int h = g.guiHeight();

        int x = 4, y = h - 34;
        float maxMana = Derived.maxMana(d), maxEnergy = Derived.maxEnergy(d);
        Ui.bar(g, x, y, 84, 7, d.mana / maxMana, Ui.MANA);
        Ui.bar(g, x, y + 9, 84, 7, d.energy / maxEnergy, Ui.ENERGY);
        Ui.bar(g, x, y + 18, 84, 4, d.level >= Progression.MAX_LEVEL ? 1 : d.xp / (double) Progression.xpToNext(d.level), Ui.XP);
        g.pose().pushPose();
        g.pose().scale(0.75f, 0.75f, 1);
        drawSmall(g, font, (int) d.mana + "/" + (int) maxMana, x + 87, y);
        drawSmall(g, font, (int) d.energy + "/" + (int) maxEnergy, x + 87, y + 9);
        drawSmall(g, font, Component.translatable("ui.alvorecer.level_short", d.level).getString(), x + 87, y + 17);
        g.pose().popPose();

        long now = mc.player.level().getGameTime();
        int size = 18, gap = 2;
        int top = h / 2 - (AbilityService.SLOT_COUNT * (size + gap)) / 2;
        for (int slot = 0; slot < AbilityService.SLOT_COUNT; slot++) {
            String id = AbilityService.abilityId(d, slot);
            if (id == null) continue;
            Ability a = Abilities.get(id);
            int sx = 4, sy = top + slot * (size + gap);
            boolean locked = d.level < AbilityService.requiredLevel(slot);
            g.fill(sx, sy, sx + size, sy + size, locked ? 0xC0202020 : (Ui.schoolColor(a.school()) & 0x00FFFFFF) | 0xD0000000);
            g.renderOutline(sx, sy, size, size, slot == AbilityService.SLOT_ULTIMATE ? Ui.GOLD : 0xFF000000);
            String abbr = Component.translatable(a.nameKey()).getString();
            abbr = abbr.substring(0, Math.min(2, abbr.length()));
            g.drawCenteredString(font, abbr, sx + size / 2, sy + 5, locked ? Ui.MUTED : 0xFFFFFFFF);
            long remaining = d.cooldownRemaining(id, now);
            if (remaining > 0 && !locked) {
                int total = Math.max(1, a.cooldownTicks());
                int cover = (int) (size * Math.min(1, remaining / (double) total));
                g.fill(sx, sy + size - cover, sx + size, sy + size, 0xA0000000);
                g.drawCenteredString(font, String.valueOf((remaining + 19) / 20), sx + size / 2, sy + 5, 0xFFFF5555);
            }
            String key = AlvorecerClient.ABILITY_KEYS[slot].getTranslatedKeyMessage().getString();
            g.pose().pushPose();
            g.pose().scale(0.6f, 0.6f, 1);
            g.drawString(font, locked ? "Nv" + AbilityService.requiredLevel(slot) : key, (int) ((sx + size + 2) / 0.6f), (int) ((sy + 6) / 0.6f), Ui.TEXT);
            g.pose().popPose();
        }
    }

    private static void drawSmall(GuiGraphics g, Font font, String text, int x, int y) {
        g.drawString(font, text, (int) (x / 0.75f), (int) (y / 0.75f), Ui.TEXT);
    }
}
