package com.alvorecer.client;

import com.alvorecer.entity.npc.NpcEntity;
import com.alvorecer.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Diálogo com NPC: saudação, conversa (falas em sequência), comércio e missões. */
public class NpcScreen extends Screen {
    private static final int W = 320, H = 150, TALK_LINES = 4;
    private final int entityId;
    private final NpcEntity.Role role;
    private int line = -1;

    public NpcScreen(int entityId, NpcEntity.Role role) {
        super(Component.translatable("entity.alvorecer." + role.id()));
        this.entityId = entityId;
        this.role = role;
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return (height - H) / 2 + 30;
    }

    @Override
    protected void init() {
        int y = top() + H - 26, x = left() + 8;
        addRenderableWidget(Button.builder(Component.translatable("ui.alvorecer.npc.talk"), b -> line = (line + 1) % TALK_LINES)
                .bounds(x, y, 72, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("ui.alvorecer.npc.trade"), b -> {
            PacketDistributor.sendToServer(new ModNetwork.NpcAction(entityId, 0));
        }).bounds(x + 78, y, 72, 18).build());
        if (role.givesQuests()) {
            addRenderableWidget(Button.builder(Component.translatable("ui.alvorecer.npc.quests"), b ->
                    PacketDistributor.sendToServer(new ModNetwork.NpcAction(entityId, 1))).bounds(x + 156, y, 72, 18).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("ui.alvorecer.npc.bye"), b -> onClose()).bounds(x + 234, y, 70, 18).build());
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Ui.panel(g, left(), top(), W, H);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawString(font, title.copy().withStyle(ChatFormatting.BOLD), left() + 10, top() + 8, Ui.GOLD, false);
        String key = "npc.alvorecer." + role.id() + (line < 0 ? ".greeting" : ".talk." + line);
        List<FormattedCharSequence> lines = font.split(Component.translatable(key), W - 20);
        for (int i = 0; i < Math.min(8, lines.size()); i++) g.drawString(font, lines.get(i), left() + 10, top() + 24 + i * 10, Ui.TEXT, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
