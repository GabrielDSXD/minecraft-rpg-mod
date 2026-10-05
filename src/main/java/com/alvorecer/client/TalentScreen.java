package com.alvorecer.client;

import com.alvorecer.network.ModNetwork;
import com.alvorecer.registry.ModAttachments;
import com.alvorecer.rpg.PlayerRpgData;
import com.alvorecer.rpg.talent.Talent;
import com.alvorecer.rpg.talent.Talents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Árvore de talentos da classe (3 tiers × 2) e escolha de especialização. */
public class TalentScreen extends Screen {
    private static final int W = 410, H = 236, CARD_W = 194, CARD_H = 46;
    private int lastSignature = -1;

    public TalentScreen() {
        super(Component.translatable("ui.alvorecer.talents.title"));
    }

    private PlayerRpgData data() {
        return ModAttachments.get(minecraft.player);
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return Math.max(2, (height - H) / 2);
    }

    private int cardX(int col) {
        return left() + 8 + col * (CARD_W + 6);
    }

    private int cardY(int tier) {
        return top() + 66 + (tier - 1) * (CARD_H + 6);
    }

    /** Muda quando pontos/talentos/especialização mudam: aí os botões são refeitos. */
    private int signature() {
        PlayerRpgData d = data();
        return d.talents.hashCode() * 31 + (d.specialization == null ? 0 : d.specialization.hashCode()) + d.level * 7;
    }

    @Override
    protected void init() {
        PlayerRpgData d = data();
        lastSignature = signature();
        if (d.specialization == null && d.level >= Talents.SPEC_LEVEL) {
            int i = 0;
            for (Talents.Specialization s : Talents.specsFor(d.rpgClass)) {
                Button b = Button.builder(Component.translatable(s.nameKey()), btn ->
                        PacketDistributor.sendToServer(new ModNetwork.ChooseSpec(s.id()))).bounds(left() + 8 + i * (CARD_W + 6), top() + 39, CARD_W, 15).build();
                b.setTooltip(Tooltip.create(specText(s)));
                addRenderableWidget(b);
                i++;
            }
        }
        List<Talent> talents = Talents.forClass(d.rpgClass);
        int[] col = new int[4];
        for (Talent t : talents) {
            int c = col[t.tier()]++;
            if (Talents.canSpend(d, t.id()) == null) {
                addRenderableWidget(Button.builder(Component.literal("+"), b -> PacketDistributor.sendToServer(new ModNetwork.SpendTalent(t.id())))
                        .bounds(cardX(c) + CARD_W - 16, cardY(t.tier()) + 3, 13, 13).build());
            }
        }
        addRenderableWidget(Button.builder(Component.translatable("ui.alvorecer.back"), b -> minecraft.setScreen(new CharacterSheetScreen()))
                .bounds(left() + W - 70, top() + 4, 62, 14).build());
    }

    @Override
    public void tick() {
        if (signature() != lastSignature) rebuildWidgets();
    }

    private static Component specText(Talents.Specialization s) {
        MutableComponent c = Component.translatable(s.descKey()).copy();
        for (Talent.Bonus b : s.bonuses()) c.append("\n").append(Ui.bonusLine(b, 1));
        return c;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        Ui.panel(g, left(), top(), W, H);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        PlayerRpgData d = data();
        int x = left() + 8, y = top() + 6;
        g.drawString(font, Component.translatable("ui.alvorecer.talents.title").append(" — ").append(Component.translatable(d.rpgClass.langKey())), x, y, Ui.GOLD, false);
        int points = Talents.available(d);
        g.drawString(font, Component.translatable("ui.alvorecer.talents.points", points), x, y + 12, points > 0 ? 0xFF55FF55 : Ui.MUTED, false);

        Talents.Specialization spec = Talents.spec(d.specialization);
        if (spec != null) {
            g.drawString(font, Component.translatable("ui.alvorecer.spec").append(": ").append(Component.translatable(spec.nameKey()))
                    .withStyle(ChatFormatting.AQUA), x, y + 26, Ui.TEXT, false);
            MutableComponent bonuses = Component.empty();
            for (Talent.Bonus b : spec.bonuses()) bonuses.append(Ui.bonusLine(b, 1)).append("  ");
            g.drawString(font, bonuses, x, y + 37, Ui.MUTED, false);
            if (mouseX >= x && mouseX < x + W - 16 && mouseY >= y + 24 && mouseY < y + 47) {
                g.renderTooltip(font, font.split(specText(spec), 240), mouseX, mouseY);
            }
        } else if (d.level < Talents.SPEC_LEVEL) {
            g.drawString(font, Component.translatable("ui.alvorecer.spec_locked", Talents.SPEC_LEVEL), x, y + 26, Ui.MUTED, false);
        } else {
            g.drawString(font, Component.translatable("ui.alvorecer.spec_choose"), x, y + 23, Ui.GOLD, false);
        }

        for (int tier = 1; tier <= 3; tier++) {
            boolean open = d.level >= Talents.TIER_LEVEL[tier];
            g.drawString(font, Component.translatable("ui.alvorecer.tier", tier, Talents.TIER_LEVEL[tier], Talents.TIER_POINTS[tier]),
                    x, cardY(tier) - 9, open ? Ui.MUTED : 0xFF884444, false);
        }
        int[] col = new int[4];
        for (Talent t : Talents.forClass(d.rpgClass)) {
            int c = col[t.tier()]++;
            int cx = cardX(c), cy = cardY(t.tier());
            int rank = d.talents.getOrDefault(t.id(), 0);
            g.fill(cx, cy, cx + CARD_W, cy + CARD_H, rank > 0 ? 0xA0302818 : 0x80101010);
            g.renderOutline(cx, cy, CARD_W, CARD_H, rank >= t.maxRank() ? Ui.GOLD : 0xFF4A4030);
            g.drawString(font, Component.translatable(t.nameKey()), cx + 4, cy + 4, rank > 0 ? Ui.GOLD : Ui.TEXT, false);
            g.drawString(font, rank + "/" + t.maxRank(), cx + CARD_W - 40, cy + 6, Ui.MUTED, false);
            List<FormattedCharSequence> lines = font.split(talentText(t, false), CARD_W - 8);
            for (int i = 0; i < Math.min(3, lines.size()); i++) {
                g.pose().pushPose();
                g.pose().scale(0.8f, 0.8f, 1);
                g.drawString(font, lines.get(i), (int) ((cx + 4) / 0.8f), (int) ((cy + 16 + i * 9) / 0.8f), Ui.MUTED, false);
                g.pose().popPose();
            }
            if (mouseX >= cx && mouseX < cx + CARD_W && mouseY >= cy && mouseY < cy + CARD_H) {
                g.renderTooltip(font, font.split(talentText(t, true), 240), mouseX, mouseY);
            }
        }
    }

    private Component talentText(Talent t, boolean full) {
        MutableComponent c = Component.empty();
        if (t.special()) c.append(Component.translatable(t.descKey()));
        for (Talent.Bonus b : t.perRank()) {
            if (!c.getString().isEmpty()) c.append(full ? "\n" : " ");
            c.append(Ui.bonusLine(b, 1)).append(t.maxRank() > 1 ? Component.translatable("ui.alvorecer.per_rank") : Component.empty());
        }
        return c;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
