package com.alvorecer.client;

import com.alvorecer.ability.Ability;
import com.alvorecer.config.RpgConfig;
import com.alvorecer.rpg.Resource;
import com.alvorecer.rpg.School;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Desenho compartilhado pelas telas e pelo HUD. */
public final class Ui {
    public static final int GOLD = 0xFFE0B85A;
    public static final int TEXT = 0xFFE8E2D0;
    public static final int MUTED = 0xFF9A9384;
    public static final int MANA = 0xFF3D7BE0;
    public static final int ENERGY = 0xFFE0B23D;
    public static final int XP = 0xFF6BD06B;
    public static final int HEALTH = 0xFFD04545;

    private Ui() {
    }

    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xE0141018);
        g.renderOutline(x, y, w, h, 0xFF5A4A2E);
    }

    public static void bar(GuiGraphics g, int x, int y, int w, int h, double fraction, int color) {
        g.fill(x, y, x + w, y + h, 0xFF000000);
        g.fill(x + 1, y + 1, x + 1 + (int) ((w - 2) * Math.clamp(fraction, 0, 1)), y + h - 1, color);
    }

    /** Barra de rolagem fina; não desenha nada se todo o texto cabe. */
    public static void scrollbar(GuiGraphics g, int x, int y, int height, int scroll, int total, int visible) {
        if (total <= visible) return;
        g.fill(x, y, x + 3, y + height, 0x60FFFFFF);
        int thumb = Math.max(8, height * visible / total);
        int pos = y + (height - thumb) * scroll / Math.max(1, total - visible);
        g.fill(x, pos, x + 3, pos + thumb, GOLD);
    }

    public static int schoolColor(School school) {
        return switch (school) {
            case PHYSICAL -> 0xFF8C6E4A;
            case ARCANE -> 0xFF7E57C2;
            case ELEMENTAL -> 0xFFE0662F;
            case DIVINE -> 0xFFE8C547;
            case NATURE -> 0xFF4CAF50;
            case SHADOW -> 0xFF5E3A6E;
        };
    }

    /** Linhas quebradas na largura; "\n" no texto força quebra. */
    public static List<FormattedCharSequence> wrap(Font font, Component text, int width) {
        return font.split(text, width);
    }

    /** Desenha linhas a partir de {@code scroll} dentro de uma altura; retorna o total de linhas. */
    public static int drawLines(GuiGraphics g, Font font, List<FormattedCharSequence> lines, int x, int y, int height, int scroll) {
        int maxLines = height / 10;
        for (int i = 0; i < maxLines && i + scroll < lines.size(); i++) {
            g.drawString(font, lines.get(i + scroll), x, y + i * 10, TEXT, false);
        }
        return lines.size();
    }

    public static Component costLine(Ability a) {
        MutableComponent line = Component.empty();
        if (a.resource() != Resource.NONE) {
            int cost = (int) Math.round(a.cost() * RpgConfig.COST_MULTIPLIER.get());
            line.append(Component.translatable("ui.alvorecer.cost", cost, Component.translatable(a.resource().langKey())));
            line.append(" • ");
        }
        line.append(Component.translatable("ui.alvorecer.cooldown", String.format("%.1f", a.cooldownTicks() * RpgConfig.COOLDOWN_MULTIPLIER.get() / 20.0)));
        line.append(" • ").append(Component.translatable(a.school().langKey()));
        if (a.mobility()) line.append(" • ").append(Component.translatable("ui.alvorecer.mobility"));
        return line.withStyle(ChatFormatting.GRAY);
    }

    /** "+4% de dano corpo a corpo", "+2 de vida máxima"... a partir do tipo de bônus. */
    public static Component bonusLine(com.alvorecer.rpg.talent.Talent.Bonus b, int ranks) {
        double v = b.amount() * ranks;
        String value = b.type().percent ? (Math.round(v * 1000) / 10.0 + "%").replace(".0%", "%") : (v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v));
        return Component.translatable(b.type().langKey(), value).withStyle(ChatFormatting.GREEN);
    }

    /** Bloco de texto (nome, custo, descrição) de uma habilidade. */
    public static List<Component> abilityBlock(Ability a, Component prefix) {
        List<Component> out = new ArrayList<>();
        out.add(Component.empty().append(prefix).append(Component.translatable(a.nameKey()).withStyle(ChatFormatting.GOLD)));
        out.add(costLine(a));
        out.add(Component.translatable(a.descKey()));
        return out;
    }
}
