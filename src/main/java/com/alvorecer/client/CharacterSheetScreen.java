package com.alvorecer.client;

import com.alvorecer.ability.Abilities;
import com.alvorecer.ability.Ability;
import com.alvorecer.ability.AbilityService;
import com.alvorecer.config.RpgConfig;
import com.alvorecer.network.ModNetwork;
import com.alvorecer.registry.ModAttachments;
import com.alvorecer.rpg.Derived;
import com.alvorecer.rpg.PlayerRpgData;
import com.alvorecer.rpg.Profession;
import com.alvorecer.rpg.Progression;
import com.alvorecer.rpg.Race;
import com.alvorecer.rpg.School;
import com.alvorecer.rpg.Stat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Ficha do personagem: atributos (com distribuição de pontos), valores derivados, habilidades e profissões. */
public class CharacterSheetScreen extends Screen {
    private static final int W = 410, H = 236;
    private boolean choosingSecondary;

    public CharacterSheetScreen() {
        super(Component.translatable("ui.alvorecer.sheet.title"));
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

    @Override
    protected void init() {
        PlayerRpgData d = data();
        int x = left(), y = top();
        if (choosingSecondary) {
            int i = 0;
            for (Profession p : Profession.values()) {
                if (p == d.profession) continue;
                addRenderableWidget(Button.builder(Component.translatable(p.langKey()), b -> {
                    PacketDistributor.sendToServer(new ModNetwork.ChooseSecondary(p.ordinal()));
                    choosingSecondary = false;
                    rebuildWidgets();
                }).bounds(x + 214 + (i % 2) * 94, y + 150 + (i / 2) * 18, 90, 16).build());
                i++;
            }
        } else if (d.secondaryProfession == null && d.level >= RpgConfig.SECONDARY_PROFESSION_LEVEL.get()) {
            addRenderableWidget(Button.builder(Component.translatable("ui.alvorecer.choose_secondary"), b -> {
                choosingSecondary = true;
                rebuildWidgets();
            }).bounds(x + 210, y + 172, 180, 16).build());
        }
        if (d.unspentPoints > 0) {
            for (Stat s : Stat.values()) {
                addRenderableWidget(Button.builder(Component.literal("+"), b -> PacketDistributor.sendToServer(new ModNetwork.AllocateStat(s.ordinal())))
                        .bounds(x + 150, y + 72 + s.ordinal() * 12, 12, 11).build());
            }
        }
        addRenderableWidget(Button.builder(Component.translatable("ui.alvorecer.open_codex"), b -> minecraft.setScreen(new CodexScreen()))
                .bounds(x + W - 80, y + H - 22, 72, 16).build());
        Component talentLabel = Component.translatable("ui.alvorecer.open_talents", com.alvorecer.rpg.talent.Talents.available(d));
        addRenderableWidget(Button.builder(talentLabel, b -> minecraft.setScreen(new TalentScreen()))
                .bounds(x + W - 186, y + H - 22, 102, 16).build());
    }

    private int lastUnspent = -1;

    @Override
    public void tick() {
        int unspent = data().unspentPoints;
        if (lastUnspent != -1 && (unspent == 0) != (lastUnspent == 0)) rebuildWidgets();
        lastUnspent = unspent;
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
        if (!d.hasCharacter()) return;
        int x = left() + 8, y = top() + 6;

        Component header = Component.translatable(d.race.langKey()).append(" ").append(Component.translatable(d.rpgClass.langKey()));
        if (d.race == Race.DRAGONBORN) header = header.copy().append(" (").append(Component.translatable(d.element.langKey())).append(")");
        g.drawString(font, minecraft.player.getName(), x, y, Ui.GOLD, false);
        g.drawString(font, header, x, y + 10, Ui.TEXT, false);
        g.drawString(font, Component.translatable("ui.alvorecer.level", d.level), x + 130, y, Ui.GOLD, false);

        long next = Progression.xpToNext(d.level);
        Ui.bar(g, x, y + 22, 190, 6, d.level >= Progression.MAX_LEVEL ? 1 : d.xp / (double) next, Ui.XP);
        String xpText = d.level >= Progression.MAX_LEVEL ? "MAX" : d.xp + " / " + next + " XP";
        g.drawString(font, xpText, x, y + 30, Ui.MUTED, false);

        float maxMana = Derived.maxMana(d), maxEnergy = Derived.maxEnergy(d);
        Ui.bar(g, x, y + 42, 60, 6, minecraft.player.getHealth() / minecraft.player.getMaxHealth(), Ui.HEALTH);
        Ui.bar(g, x + 64, y + 42, 60, 6, d.mana / maxMana, Ui.MANA);
        Ui.bar(g, x + 128, y + 42, 62, 6, d.energy / maxEnergy, Ui.ENERGY);
        small(g, Math.round(minecraft.player.getHealth()) + "/" + Math.round(minecraft.player.getMaxHealth()), x, y + 50);
        small(g, Math.round(d.mana) + "/" + Math.round(maxMana), x + 64, y + 50);
        small(g, Math.round(d.energy) + "/" + Math.round(maxEnergy), x + 128, y + 50);

        int sy = y + 66;
        for (Stat s : Stat.values()) {
            int row = sy + s.ordinal() * 12;
            int mod = d.mod(s);
            g.drawString(font, Component.translatable(s.langKey()), x, row + 2, Ui.TEXT, false);
            g.drawString(font, String.valueOf(d.score(s)), x + 100, row + 2, Ui.GOLD, false);
            g.drawString(font, "(" + (mod >= 0 ? "+" : "") + mod + ")", x + 118, row + 2, Ui.MUTED, false);
            if (mouseX >= x && mouseX < x + 140 && mouseY >= row && mouseY < row + 12) {
                g.renderTooltip(font, font.split(Component.translatable(s.langKey() + ".desc"), 220), mouseX, mouseY);
            }
        }
        g.drawString(font, Component.translatable("ui.alvorecer.unspent", d.unspentPoints), x, sy + 74,
                d.unspentPoints > 0 ? 0xFF55FF55 : Ui.MUTED, false);

        int dy = sy + 88;
        School main = d.rpgClass.resource == com.alvorecer.rpg.Resource.MANA ? School.ARCANE : School.PHYSICAL;
        String[] derived = {
                tr("ui.alvorecer.d.crit", pct(Derived.critChance(d)), String.format("%.2f", Derived.critMultiplier(d))),
                tr("ui.alvorecer.d.melee", pct(Derived.meleeMultiplier(d) - 1), pct(Derived.rangedMultiplier(d) - 1)),
                tr("ui.alvorecer.d.power", String.format("%.2f", Derived.power(d, d.rpgClass.keyStat, main)), Progression.saveDc(d.level, d.score(d.rpgClass.keyStat))),
                tr("ui.alvorecer.d.resist", pct(Derived.magicResistance(d)), Progression.proficiency(d.level)),
                tr("ui.alvorecer.d.regen", String.format("%.1f", Derived.manaRegen(d)), String.format("%.1f", Derived.energyRegen(d))),
        };
        for (int i = 0; i < derived.length; i++) small(g, derived[i], x, dy + i * 9);

        renderAbilities(g, d, mouseX, mouseY);
        renderProfessions(g, d);
    }

    private void renderAbilities(GuiGraphics g, PlayerRpgData d, int mouseX, int mouseY) {
        int x = left() + 210, y = top() + 6;
        g.drawString(font, Component.translatable("ui.alvorecer.abilities"), x, y, Ui.GOLD, false);
        long now = minecraft.player.level().getGameTime();
        for (int slot = 0; slot < AbilityService.SLOT_COUNT; slot++) {
            String id = AbilityService.abilityId(d, slot);
            int row = y + 12 + slot * 15;
            String key = AlvorecerClient.ABILITY_KEYS[slot].getTranslatedKeyMessage().getString();
            g.drawString(font, "[" + key + "]", x, row, Ui.MUTED, false);
            if (id == null) {
                g.drawString(font, Component.translatable("ui.alvorecer.empty_slot"), x + 26, row, Ui.MUTED, false);
                continue;
            }
            Ability a = Abilities.get(id);
            boolean locked = d.level < AbilityService.requiredLevel(slot);
            Component name = Component.translatable(a.nameKey());
            g.drawString(font, name, x + 26, row, locked ? Ui.MUTED : (slot == AbilityService.SLOT_ULTIMATE ? Ui.GOLD : Ui.TEXT), false);
            String status = locked ? tr("ui.alvorecer.level_short", AbilityService.requiredLevel(slot))
                    : d.isOnCooldown(id, now) ? String.format("%.0fs", d.cooldownRemaining(id, now) / 20.0) : "";
            g.drawString(font, status, x + 170, row, locked ? 0xFFAA5555 : 0xFFFF7777, false);
            if (mouseX >= x && mouseX < x + 190 && mouseY >= row - 2 && mouseY < row + 12) {
                List<FormattedCharSequence> tip = new ArrayList<>();
                for (Component c : Ui.abilityBlock(a, Component.empty())) tip.addAll(font.split(c, 240));
                g.renderTooltip(font, tip, mouseX, mouseY);
            }
        }
    }

    private void renderProfessions(GuiGraphics g, PlayerRpgData d) {
        int x = left() + 210, y = top() + 136;
        g.drawString(font, Component.translatable("ui.alvorecer.professions"), x, y, Ui.GOLD, false);
        professionLine(g, d.profession, d.professionLevel, d.professionXp, x, y + 12);
        if (d.secondaryProfession != null) {
            professionLine(g, d.secondaryProfession, d.secondaryLevel, d.secondaryXp, x, y + 34);
        } else if (!choosingSecondary && d.level < RpgConfig.SECONDARY_PROFESSION_LEVEL.get()) {
            small(g, tr("ui.alvorecer.secondary_locked", RpgConfig.SECONDARY_PROFESSION_LEVEL.get()), x, y + 36);
        }
    }

    private void professionLine(GuiGraphics g, Profession p, int level, long xp, int x, int y) {
        g.drawString(font, Component.translatable(p.langKey()).append(" — ").append(Component.translatable("ui.alvorecer.level", level)), x, y, Ui.TEXT, false);
        long next = Progression.professionXpToNext(level);
        Ui.bar(g, x, y + 10, 180, 5, level >= Progression.MAX_PROFESSION_LEVEL ? 1 : xp / (double) next, Ui.XP);
    }

    private void small(GuiGraphics g, String text, int x, int y) {
        g.pose().pushPose();
        g.pose().scale(0.8f, 0.8f, 1);
        g.drawString(font, text, (int) (x / 0.8f), (int) (y / 0.8f), Ui.TEXT, false);
        g.pose().popPose();
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    private static String pct(double v) {
        return Math.round(v * 100) + "%";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
