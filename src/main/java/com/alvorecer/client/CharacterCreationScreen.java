package com.alvorecer.client;

import com.alvorecer.ability.Abilities;
import com.alvorecer.ability.Ability;
import com.alvorecer.network.ModNetwork;
import com.alvorecer.rpg.Element;
import com.alvorecer.rpg.PlayerRpgData;
import com.alvorecer.rpg.Profession;
import com.alvorecer.rpg.Progression;
import com.alvorecer.rpg.Race;
import com.alvorecer.rpg.RpgClass;
import com.alvorecer.rpg.Stat;
import com.alvorecer.rpg.WeaponCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Criação de personagem em quatro passos: raça (e elemento), classe, profissão e confirmação. */
public class CharacterCreationScreen extends Screen {
    private static final int W = 400, H = 240;
    private int tab;
    private Race race = Race.HUMAN;
    private RpgClass rpgClass = RpgClass.WARRIOR;
    private Profession profession = Profession.BLACKSMITH;
    private Element element = Element.FIRE;
    private int scroll;
    private List<FormattedCharSequence> lines = List.of();

    public CharacterCreationScreen() {
        super(Component.translatable("ui.alvorecer.creation.title"));
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return Math.max(0, (height - H) / 2);
    }

    @Override
    protected void init() {
        int x = left(), y = top();
        String[] tabs = {"race", "class", "profession", "confirm"};
        for (int i = 0; i < tabs.length; i++) {
            int t = i;
            Component label = Component.translatable("ui.alvorecer.tab." + tabs[i]);
            if (t == tab) label = label.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE);
            addRenderableWidget(Button.builder(label, b -> switchTab(t)).bounds(x + 8 + i * 97, y + 6, 94, 16).build());
        }
        int ly = y + 28;
        switch (tab) {
            case 0 -> {
                for (Race r : Race.values()) {
                    addOption(Component.translatable(r.langKey()), r == race, ly, () -> race = r);
                    ly += 18;
                }
                if (race == Race.DRAGONBORN) {
                    for (int i = 0; i < Element.values().length; i++) {
                        Element e = Element.values()[i];
                        Component label = Component.translatable(e.langKey());
                        if (e == element) label = label.copy().withStyle(ChatFormatting.GOLD);
                        addRenderableWidget(Button.builder(label, b -> {
                            element = e;
                            refresh();
                        }).bounds(x + 8 + (i % 2) * 51, ly + 2 + (i / 2) * 16, 50, 14).build());
                    }
                }
            }
            case 1 -> {
                for (RpgClass c : RpgClass.values()) {
                    addOption(Component.translatable(c.langKey()), c == rpgClass, ly, () -> rpgClass = c);
                    ly += 18;
                }
            }
            case 2 -> {
                for (Profession p : Profession.values()) {
                    addOption(Component.translatable(p.langKey()), p == profession, ly, () -> profession = p);
                    ly += 18;
                }
            }
            default -> addRenderableWidget(Button.builder(Component.translatable("ui.alvorecer.confirm").withStyle(ChatFormatting.GREEN), b -> confirm())
                    .bounds(x + 8, y + H - 24, 100, 18).build());
        }
        if (tab > 0) addRenderableWidget(Button.builder(Component.translatable("ui.alvorecer.back"), b -> switchTab(tab - 1))
                .bounds(x + W - 168, y + H - 24, 78, 18).build());
        if (tab < 3) addRenderableWidget(Button.builder(Component.translatable("ui.alvorecer.next"), b -> switchTab(tab + 1))
                .bounds(x + W - 86, y + H - 24, 78, 18).build());
        buildText();
    }

    private void addOption(Component label, boolean selected, int y, Runnable select) {
        Component text = selected ? Component.literal("▶ ").append(label).withStyle(ChatFormatting.GOLD) : label;
        addRenderableWidget(Button.builder(text, b -> {
            select.run();
            refresh();
        }).bounds(left() + 8, y, 100, 16).build());
    }

    private void switchTab(int t) {
        tab = t;
        refresh();
    }

    private void refresh() {
        scroll = 0;
        rebuildWidgets();
    }

    private void confirm() {
        PacketDistributor.sendToServer(new ModNetwork.CreateCharacter(race.ordinal(), rpgClass.ordinal(), profession.ordinal(), element.ordinal()));
        onClose();
    }

    private void buildText() {
        List<Component> out = new ArrayList<>();
        switch (tab) {
            case 0 -> raceText(out);
            case 1 -> classText(out);
            case 2 -> professionText(out);
            default -> summaryText(out);
        }
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        int width = W - 128;
        for (Component c : out) {
            wrapped.addAll(Ui.wrap(font, c, width));
            if (c.getString().isEmpty()) wrapped.add(FormattedCharSequence.EMPTY);
        }
        lines = wrapped;
    }

    private static Component heading(Component c) {
        return c.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
    }

    private static MutableComponent label(String key) {
        return Component.translatable(key).withStyle(ChatFormatting.YELLOW);
    }

    private void raceText(List<Component> out) {
        out.add(heading(Component.translatable(race.langKey())));
        out.add(Component.translatable(race.langKey() + ".desc"));
        out.add(Component.empty());
        MutableComponent bonus = label("ui.alvorecer.stat_bonus").append(" ");
        for (Stat s : Stat.values()) {
            int b = race.statBonus(s);
            if (b != 0) bonus.append(Component.translatable(s.langKey() + ".short")).append((b > 0 ? " +" : " ") + b + "  ");
        }
        out.add(bonus);
        out.add(label("ui.alvorecer.scale").append(" " + Math.round(race.scale * 100) + "%"));
        out.add(Component.empty());
        Ability a = Abilities.get(race.abilityId);
        out.addAll(Ui.abilityBlock(a, label("ui.alvorecer.active").append(" ")));
        if (race == Race.DRAGONBORN) {
            out.add(Component.empty());
            out.add(label("ui.alvorecer.element_choice").append(" ").append(Component.translatable(element.langKey()))
                    .append(Component.translatable("ui.alvorecer.element_opposite", Component.translatable(element.opposite().langKey()))));
        }
    }

    private void classText(List<Component> out) {
        out.add(heading(Component.translatable(rpgClass.langKey())));
        out.add(Component.translatable(rpgClass.langKey() + ".desc"));
        out.add(Component.empty());
        out.add(label("ui.alvorecer.resource").append(" ").append(Component.translatable(rpgClass.resource.langKey())));
        out.add(label("ui.alvorecer.key_stat").append(" ").append(Component.translatable(rpgClass.keyStat.langKey()))
                .append(" (+" + Progression.CLASS_KEY_BONUS + ")"));
        out.add(label("ui.alvorecer.saves").append(" ").append(Component.translatable(rpgClass.save1.langKey()))
                .append(", ").append(Component.translatable(rpgClass.save2.langKey())));
        MutableComponent weapons = label("ui.alvorecer.weapons").append(" ");
        boolean first = true;
        for (WeaponCategory w : rpgClass.weapons) {
            if (w == WeaponCategory.NONE) continue;
            if (!first) weapons.append(", ");
            weapons.append(Component.translatable(w.langKey()));
            first = false;
        }
        out.add(weapons);
        out.add(label("ui.alvorecer.max_armor").append(" ").append(Component.translatable(rpgClass.maxArmor.langKey())));
        out.add(label("ui.alvorecer.hit_die").append(" ×" + rpgClass.hitDie));
        out.add(Component.empty());
        for (int i = 0; i < rpgClass.abilities.size(); i++) {
            Ability a = Abilities.get(rpgClass.abilities.get(i));
            Component prefix = Component.translatable(i == 4 ? "ui.alvorecer.ultimate_at" : "ui.alvorecer.level_at", RpgClass.UNLOCK_LEVELS[i])
                    .withStyle(ChatFormatting.AQUA).append(" ");
            out.addAll(Ui.abilityBlock(a, prefix));
            out.add(Component.empty());
        }
    }

    private void professionText(List<Component> out) {
        out.add(heading(Component.translatable(profession.langKey())));
        out.add(Component.translatable(profession.langKey() + ".desc"));
        out.add(Component.empty());
        out.addAll(Ui.abilityBlock(Abilities.get(profession.abilityId), label("ui.alvorecer.active").append(" ")));
        out.add(Component.empty());
        out.add(Component.translatable("ui.alvorecer.secondary_hint").withStyle(ChatFormatting.GRAY));
    }

    private void summaryText(List<Component> out) {
        PlayerRpgData preview = new PlayerRpgData();
        preview.race = race;
        preview.rpgClass = rpgClass;
        preview.profession = profession;
        preview.element = element;
        out.add(heading(Component.translatable("ui.alvorecer.summary")));
        MutableComponent raceLine = label("ui.alvorecer.tab.race").append(": ").append(Component.translatable(race.langKey()));
        if (race == Race.DRAGONBORN) raceLine.append(" (").append(Component.translatable(element.langKey())).append(")");
        out.add(raceLine);
        out.add(label("ui.alvorecer.tab.class").append(": ").append(Component.translatable(rpgClass.langKey())));
        out.add(label("ui.alvorecer.tab.profession").append(": ").append(Component.translatable(profession.langKey())));
        out.add(Component.empty());
        MutableComponent stats = Component.empty();
        for (Stat s : Stat.values()) {
            int mod = preview.mod(s);
            stats.append(Component.translatable(s.langKey() + ".short")).append(" " + preview.score(s) + " (" + (mod >= 0 ? "+" : "") + mod + ")   ");
        }
        out.add(stats);
        out.add(Component.empty());
        out.add(Component.translatable("ui.alvorecer.summary_hint", Progression.CREATION_POINTS));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x = left(), y = top();
        Ui.panel(g, x + 114, y + 28, W - 122, H - 58);
        Ui.drawLines(g, font, lines, x + 120, y + 33, H - 66, scroll);
        Ui.scrollbar(g, x + W - 11, y + 30, H - 62, scroll, lines.size(), (H - 66) / 10);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        Ui.panel(g, left(), top(), W, H);
        if (top() >= 14) g.drawCenteredString(font, title, width / 2, top() - 12, Ui.GOLD);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int visible = (H - 66) / 10;
        scroll = Math.clamp(scroll - (int) Math.signum(scrollY) * 2, 0, Math.max(0, lines.size() - visible));
        return true;
    }
}
