package com.alvorecer.client;

import com.alvorecer.network.ModNetwork;
import com.alvorecer.quest.Quest;
import com.alvorecer.quest.Quests;
import com.alvorecer.registry.ModAttachments;
import com.alvorecer.rpg.PlayerRpgData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Mural de Missões: lista à esquerda (ativas primeiro), detalhes e ações à direita. */
public class QuestBoardScreen extends Screen {
    private static final int W = 420, H = 236, ROWS = 11;
    private String selected;
    private int scroll;
    private int lastSignature;

    public QuestBoardScreen() {
        super(Component.translatable("block.alvorecer.quest_board"));
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

    private long now() {
        return minecraft.player.level().getGameTime();
    }

    private List<Quest> sorted() {
        PlayerRpgData d = data();
        List<Quest> list = new ArrayList<>(Quests.all().values());
        list.removeIf(q -> Quests.status(minecraft.player, d, q, now()) == Quests.Status.DONE);
        list.sort(Comparator.comparingInt(q -> switch (Quests.status(minecraft.player, d, q, now())) {
            case READY -> 0;
            case ACTIVE -> 1;
            case AVAILABLE -> 2;
            case COOLDOWN -> 3;
            default -> 4;
        }));
        return list;
    }

    private int signature() {
        PlayerRpgData d = data();
        return d.activeQuests.hashCode() * 31 + d.completedQuests.hashCode() + d.level;
    }

    @Override
    protected void init() {
        lastSignature = signature();
        List<Quest> list = sorted();
        if (selected == null && !list.isEmpty()) selected = list.get(0).id();
        scroll = Math.clamp(scroll, 0, Math.max(0, list.size() - ROWS));
        PlayerRpgData d = data();
        for (int i = 0; i < ROWS && i + scroll < list.size(); i++) {
            Quest q = list.get(i + scroll);
            Quests.Status st = Quests.status(minecraft.player, d, q, now());
            MutableComponent label = Component.translatable(q.nameKey()).copy();
            ChatFormatting color = switch (st) {
                case READY -> ChatFormatting.GREEN;
                case ACTIVE -> ChatFormatting.YELLOW;
                case AVAILABLE -> ChatFormatting.WHITE;
                default -> ChatFormatting.DARK_GRAY;
            };
            label.withStyle(color);
            if (q.id().equals(selected)) label = Component.literal("▶ ").append(label);
            addRenderableWidget(Button.builder(label, b -> {
                selected = q.id();
                rebuildWidgets();
            }).bounds(left() + 8, top() + 20 + i * 18, 150, 16).build());
        }
        Quest q = selected == null ? null : Quests.get(selected);
        if (q != null) {
            Quests.Status st = Quests.status(minecraft.player, d, q, now());
            int bx = left() + 170, by = top() + H - 24;
            if (st == Quests.Status.AVAILABLE) action(Component.translatable("ui.alvorecer.quest.accept"), 0, q, bx, by);
            if (st == Quests.Status.READY) action(Component.translatable("ui.alvorecer.quest.complete"), 1, q, bx, by);
            if (st == Quests.Status.ACTIVE || st == Quests.Status.READY) action(Component.translatable("ui.alvorecer.quest.abandon"), 2, q, bx + 120, by);
        }
    }

    private void action(Component label, int action, Quest q, int x, int y) {
        addRenderableWidget(Button.builder(label, b -> PacketDistributor.sendToServer(new ModNetwork.QuestAction(action, q.id())))
                .bounds(x, y, 110, 18).build());
    }

    @Override
    public void tick() {
        if (signature() != lastSignature) rebuildWidgets();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, scroll - (int) Math.signum(scrollY));
        rebuildWidgets();
        return true;
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
        g.drawString(font, title, left() + 8, top() + 7, Ui.GOLD, false);
        g.drawString(font, Component.translatable("ui.alvorecer.quest.active", d.activeQuests.size()), left() + 170, top() + 7, Ui.MUTED, false);
        Ui.scrollbar(g, left() + 160, top() + 20, ROWS * 18 - 2, scroll, sorted().size(), ROWS);
        Quest q = selected == null ? null : Quests.get(selected);
        if (q == null) return;
        List<FormattedCharSequence> lines = new ArrayList<>();
        int width = W - 182;
        for (Component c : describe(minecraft.player, d, q, now())) {
            if (c.getString().isEmpty()) lines.add(FormattedCharSequence.EMPTY);
            else lines.addAll(font.split(c, width));
        }
        Ui.drawLines(g, font, lines, left() + 172, top() + 22, H - 52, 0);
    }

    /** Texto completo de uma missão (também usado pelo diário no Codex). */
    public static List<Component> describe(net.minecraft.world.entity.player.Player p, PlayerRpgData d, Quest q, long now) {
        List<Component> out = new ArrayList<>();
        Quests.Status st = Quests.status(p, d, q, now);
        out.add(Component.translatable(q.nameKey()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        out.add(Component.translatable("quest.alvorecer.category." + q.category().name().toLowerCase())
                .append(" • ").append(Component.translatable("quest.alvorecer.status." + st.name().toLowerCase()))
                .append(q.repeatable() ? Component.translatable("ui.alvorecer.quest.daily") : Component.empty()).withStyle(ChatFormatting.GRAY));
        out.add(Component.translatable(q.descKey()));
        out.add(Component.empty());
        out.add(objective(q, Quests.progress(p, d, q)).copy().withStyle(st == Quests.Status.READY ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        if (st == Quests.Status.LOCKED) out.add(Component.translatable("ui.alvorecer.quest.min_level", q.minLevel()).withStyle(ChatFormatting.RED));
        if (st == Quests.Status.COOLDOWN) {
            long left = d.completedQuests.get(q.id()) + Quest.REPEAT_TICKS - now;
            out.add(Component.translatable("ui.alvorecer.quest.cooldown", (left + 1199) / 1200).withStyle(ChatFormatting.GRAY));
        }
        out.add(Component.empty());
        MutableComponent reward = Component.translatable("ui.alvorecer.quest.reward", q.xp()).copy();
        if (q.professionXp() > 0) reward.append(Component.translatable("ui.alvorecer.quest.reward_prof", q.professionXp()));
        for (Quest.RewardItem r : q.items()) {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(r.id()));
            reward.append(", " + r.count() + "× ").append(item.getDescription());
        }
        out.add(reward.withStyle(ChatFormatting.AQUA));
        return out;
    }

    public static Component objective(Quest q, int progress) {
        Component target = targetName(q);
        return Component.translatable("quest.alvorecer.objective." + q.objective().name().toLowerCase(), target, progress, q.count());
    }

    private static Component targetName(Quest q) {
        if (q.target().isEmpty()) return Component.empty();
        if (q.target().startsWith("#")) return Component.translatable("quest.alvorecer.target." + q.target().substring(1).replace(':', '.').replace('/', '.'));
        ResourceLocation id = ResourceLocation.parse(q.target());
        if (q.objective() == Quest.Objective.KILL) return BuiltInRegistries.ENTITY_TYPE.get(id).getDescription();
        if (q.objective() == Quest.Objective.GATHER) return BuiltInRegistries.ITEM.get(id).getDescription();
        return Component.translatable("quest.alvorecer.target." + id.getNamespace() + "." + id.getPath());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
