package com.alvorecer.client;

import com.alvorecer.ability.Abilities;
import com.alvorecer.ability.AbilityService;
import com.alvorecer.registry.ModAttachments;
import com.alvorecer.rpg.PlayerRpgData;
import com.alvorecer.rpg.Profession;
import com.alvorecer.rpg.Race;
import com.alvorecer.rpg.RpgClass;
import com.alvorecer.rpg.Stat;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Codex do Aventureiro. Capítulos de texto (arquivo de idioma) e capítulos gerados a partir das definições do jogo
 * (raças, classes, profissões), para que números e descrições nunca divirjam do que está implementado.
 * O capítulo de fórmulas só abre a partir do nível 3 (informação avançada).
 */
public class CodexScreen extends Screen {
    private static final int W = 420, H = 236;
    private static final String[] CHAPTERS = {"history", "attributes", "races", "classes", "talents", "professions", "formulas",
            "quests", "achievements", "party", "journal", "controls", "admin"};
    private static final int FORMULAS_LEVEL = 3;
    private static int chapter;
    private int scroll;
    private List<FormattedCharSequence> lines = List.of();

    public CodexScreen() {
        super(Component.translatable("ui.alvorecer.codex.title"));
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return Math.max(2, (height - H) / 2);
    }

    @Override
    protected void init() {
        for (int i = 0; i < CHAPTERS.length; i++) {
            int c = i;
            Component label = Component.translatable("codex.alvorecer.chapter." + CHAPTERS[i]);
            if (c == chapter) label = label.copy().withStyle(ChatFormatting.GOLD);
            addRenderableWidget(Button.builder(label, b -> {
                chapter = c;
                scroll = 0;
                rebuildWidgets();
            }).bounds(left() + 8, top() + 20 + i * 16, 100, 15).build());
        }
        build();
    }

    private PlayerRpgData data() {
        return ModAttachments.get(minecraft.player);
    }

    private void build() {
        List<Component> out = new ArrayList<>();
        String id = CHAPTERS[chapter];
        out.add(Component.translatable("codex.alvorecer.chapter." + id).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        switch (id) {
            case "attributes" -> {
                out.add(Component.translatable("codex.alvorecer.attributes"));
                for (Stat s : Stat.values()) {
                    out.add(Component.empty());
                    out.add(Component.translatable(s.langKey()).withStyle(ChatFormatting.YELLOW));
                    out.add(Component.translatable(s.langKey() + ".desc"));
                }
            }
            case "races" -> {
                out.add(Component.translatable("codex.alvorecer.races"));
                for (Race r : Race.values()) {
                    out.add(Component.empty());
                    out.add(Component.translatable(r.langKey()).withStyle(ChatFormatting.YELLOW));
                    out.add(Component.translatable(r.langKey() + ".desc"));
                    out.addAll(Ui.abilityBlock(Abilities.get(r.abilityId), Component.translatable("ui.alvorecer.active").append(" ")));
                }
            }
            case "classes" -> {
                out.add(Component.translatable("codex.alvorecer.classes"));
                for (RpgClass c : RpgClass.values()) {
                    out.add(Component.empty());
                    out.add(Component.translatable(c.langKey()).withStyle(ChatFormatting.YELLOW));
                    out.add(Component.translatable(c.langKey() + ".desc"));
                    for (int i = 0; i < c.abilities.size(); i++) {
                        out.add(Component.translatable(i == 4 ? "ui.alvorecer.ultimate_at" : "ui.alvorecer.level_at", RpgClass.UNLOCK_LEVELS[i])
                                .withStyle(ChatFormatting.AQUA).append(" ").append(Component.translatable("ability.alvorecer." + c.abilities.get(i))));
                    }
                }
            }
            case "professions" -> {
                out.add(Component.translatable("codex.alvorecer.professions"));
                for (Profession p : Profession.values()) {
                    out.add(Component.empty());
                    out.add(Component.translatable(p.langKey()).withStyle(ChatFormatting.YELLOW));
                    out.add(Component.translatable(p.langKey() + ".desc"));
                    out.addAll(Ui.abilityBlock(Abilities.get(p.abilityId), Component.translatable("ui.alvorecer.active").append(" ")));
                }
            }
            case "talents" -> {
                out.add(Component.translatable("codex.alvorecer.talents"));
                for (RpgClass c : RpgClass.values()) {
                    out.add(Component.empty());
                    out.add(Component.translatable(c.langKey()).withStyle(ChatFormatting.YELLOW));
                    for (var s : com.alvorecer.rpg.talent.Talents.specsFor(c)) {
                        var line = Component.translatable("ui.alvorecer.spec").append(": ").append(Component.translatable(s.nameKey()).withStyle(ChatFormatting.AQUA)).append(" - ");
                        for (var b : s.bonuses()) line.append(Ui.bonusLine(b, 1)).append(" ");
                        out.add(line);
                    }
                    for (var t : com.alvorecer.rpg.talent.Talents.forClass(c)) {
                        var line = Component.literal("T" + t.tier() + " ").append(Component.translatable(t.nameKey()).withStyle(ChatFormatting.GOLD)).append(" - ");
                        if (t.special()) line.append(Component.translatable(t.descKey())).append(" ");
                        for (var b : t.perRank()) line.append(Ui.bonusLine(b, 1)).append(t.maxRank() > 1 ? Component.translatable("ui.alvorecer.per_rank") : Component.empty()).append(" ");
                        out.add(line);
                    }
                }
            }
            case "quests" -> {
                PlayerRpgData d = data();
                long now = minecraft.player.level().getGameTime();
                out.add(Component.translatable("codex.alvorecer.quests", d.questsCompleted));
                if (d.activeQuests.isEmpty()) out.add(Component.translatable("codex.alvorecer.quests_none").withStyle(ChatFormatting.GRAY));
                for (String qid : d.activeQuests.keySet()) {
                    var q = com.alvorecer.quest.Quests.get(qid);
                    if (q == null) continue;
                    out.add(Component.empty());
                    out.addAll(QuestBoardScreen.describe(minecraft.player, d, q, now));
                }
                if (!d.completedQuests.isEmpty()) {
                    out.add(Component.empty());
                    out.add(Component.translatable("codex.alvorecer.quests_done").withStyle(ChatFormatting.YELLOW));
                    for (String qid : d.completedQuests.keySet()) out.add(Component.literal("\u2714 ").append(Component.translatable("quest.alvorecer." + qid)));
                }
            }
            case "achievements" -> {
                PlayerRpgData d = data();
                out.add(Component.translatable("codex.alvorecer.achievements", d.achievements.size(), com.alvorecer.achievement.Achievements.ALL.size()));
                for (String aid : com.alvorecer.achievement.Achievements.ALL) {
                    boolean got = d.achievements.contains(aid);
                    out.add(Component.empty());
                    out.add(Component.literal(got ? "\u2714 " : "\u2716 ").append(Component.translatable("advancement.alvorecer." + aid))
                            .withStyle(got ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
                    out.add(Component.translatable("advancement.alvorecer." + aid + ".desc").withStyle(ChatFormatting.GRAY));
                }
            }
            case "formulas" -> {
                if (data().level < FORMULAS_LEVEL) out.add(Component.translatable("codex.alvorecer.locked_level", FORMULAS_LEVEL).withStyle(ChatFormatting.RED));
                else out.add(Component.translatable("codex.alvorecer.formulas"));
            }
            case "journal" -> {
                PlayerRpgData d = data();
                out.add(Component.translatable("codex.alvorecer.journal", d.visitedBiomes.size()));
                d.visitedBiomes.stream().sorted().forEach(b -> {
                    ResourceLocation rl = ResourceLocation.tryParse(b);
                    out.add(rl == null ? Component.literal("• " + b)
                            : Component.literal("• ").append(Component.translatable(Util.makeDescriptionId("biome", rl))));
                });
            }
            case "controls" -> {
                out.add(Component.translatable("codex.alvorecer.controls"));
                for (int slot = 0; slot < AbilityService.SLOT_COUNT; slot++) {
                    out.add(Component.literal("[" + AlvorecerClient.ABILITY_KEYS[slot].getTranslatedKeyMessage().getString() + "] ")
                            .append(Component.translatable(AlvorecerClient.ABILITY_KEYS[slot].getName())));
                }
                out.add(Component.literal("[" + AlvorecerClient.SHEET.getTranslatedKeyMessage().getString() + "] ").append(Component.translatable(AlvorecerClient.SHEET.getName())));
                out.add(Component.literal("[" + AlvorecerClient.CODEX.getTranslatedKeyMessage().getString() + "] ").append(Component.translatable(AlvorecerClient.CODEX.getName())));
                out.add(Component.literal("[" + AlvorecerClient.TALENTS.getTranslatedKeyMessage().getString() + "] ").append(Component.translatable(AlvorecerClient.TALENTS.getName())));
            }
            default -> out.add(Component.translatable("codex.alvorecer." + id));
        }
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (Component c : out) {
            if (c.getString().isEmpty()) wrapped.add(FormattedCharSequence.EMPTY);
            else wrapped.addAll(font.split(c, W - 132));
        }
        lines = wrapped;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        Ui.panel(g, left(), top(), W, H);
        g.drawCenteredString(font, title, width / 2, top() + 6, Ui.GOLD);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        Ui.panel(g, left() + 114, top() + 20, W - 122, H - 28);
        Ui.drawLines(g, font, lines, left() + 120, top() + 25, H - 36, scroll);
        Ui.scrollbar(g, left() + W - 11, top() + 22, H - 32, scroll, lines.size(), (H - 36) / 10);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int visible = (H - 36) / 10;
        scroll = Math.clamp(scroll - (int) Math.signum(scrollY) * 3, 0, Math.max(0, lines.size() - visible));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
