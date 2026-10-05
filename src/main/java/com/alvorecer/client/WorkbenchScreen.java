package com.alvorecer.client;

import com.alvorecer.crafting.ProfessionRecipes;
import com.alvorecer.network.ModNetwork;
import com.alvorecer.registry.ModAttachments;
import com.alvorecer.rpg.PlayerRpgData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Bancada de Ofício: lista de receitas (as das suas profissões primeiro), ingredientes e botão Fabricar. */
public class WorkbenchScreen extends Screen {
    private static final int W = 420, H = 236, ROWS = 11;
    private String selected;
    private int scroll;

    public WorkbenchScreen() {
        super(Component.translatable("block.alvorecer.profession_workbench"));
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
        List<ProfessionRecipes.Recipe> list = ProfessionRecipes.visible(d);
        if (selected == null && !list.isEmpty()) selected = list.get(0).id();
        scroll = Math.clamp(scroll, 0, Math.max(0, list.size() - ROWS));
        for (int i = 0; i < ROWS && i + scroll < list.size(); i++) {
            ProfessionRecipes.Recipe r = list.get(i + scroll);
            boolean ok = ProfessionRecipes.check(minecraft.player, d, r) == null;
            boolean mine = d.levelOf(r.profession()) > 0;
            Component label = r.resultStack().getHoverName().copy()
                    .withStyle(ok ? ChatFormatting.GREEN : mine ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY);
            if (r.id().equals(selected)) label = Component.literal("> ").append(label);
            addRenderableWidget(Button.builder(label, b -> {
                selected = r.id();
                rebuildWidgets();
            }).bounds(left() + 8, top() + 20 + i * 18, 170, 16).build());
        }
        ProfessionRecipes.Recipe r = ProfessionRecipes.get(selected);
        if (r != null && ProfessionRecipes.check(minecraft.player, d, r) == null) {
            addRenderableWidget(Button.builder(Component.translatable("ui.alvorecer.craft"), b -> {
                PacketDistributor.sendToServer(new ModNetwork.CraftRecipe(r.id()));
            }).bounds(left() + 190, top() + H - 24, 110, 18).build());
        }
    }

    @Override
    public void tick() {
        if (minecraft.player.tickCount % 10 == 0) rebuildWidgets();
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

    private static Component inputName(ProfessionRecipes.Input in) {
        if (in.id().startsWith("#")) {
            String tag = in.id().substring(1);
            return Component.translatable("tag.item." + tag.replace(':', '.').replace('/', '.'));
        }
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(in.id())).getDescription();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        PlayerRpgData d = data();
        g.drawString(font, title, left() + 8, top() + 7, Ui.GOLD, false);
        Ui.scrollbar(g, left() + 181, top() + 20, ROWS * 18 - 2, scroll, ProfessionRecipes.visible(d).size(), ROWS);
        ProfessionRecipes.Recipe r = ProfessionRecipes.get(selected);
        if (r == null) return;
        int x = left() + 192, y = top() + 22;
        ItemStack out = r.resultStack();
        g.renderItem(out, x, y);
        g.drawString(font, out.getHoverName(), x + 20, y + 4, Ui.GOLD, false);
        y += 22;
        int lvl = d.levelOf(r.profession());
        Component prof = Component.translatable(r.profession().langKey());
        g.drawString(font, Component.translatable("ui.alvorecer.craft.requires", prof, r.level()), x, y,
                lvl >= r.level() ? 0xFF55FF55 : 0xFFFF5555, false);
        y += 12;
        g.drawString(font, Component.translatable("ui.alvorecer.craft.xp", r.xp()), x, y, Ui.MUTED, false);
        y += 16;
        g.drawString(font, Component.translatable("ui.alvorecer.craft.inputs"), x, y, Ui.TEXT, false);
        y += 11;
        for (ProfessionRecipes.Input in : r.inputs()) {
            int have = ProfessionRecipes.count(minecraft.player, in);
            ItemStack icon = in.id().startsWith("#")
                    ? BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, ResourceLocation.parse(in.id().substring(1))))
                    .flatMap(t -> t.stream().findFirst()).map(h -> new ItemStack(h.value())).orElse(ItemStack.EMPTY)
                    : new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(in.id())));
            g.renderItem(icon, x, y - 4);
            g.drawString(font, Component.literal(have + "/" + in.count() + " ").append(inputName(in)), x + 20, y,
                    have >= in.count() ? 0xFF55FF55 : 0xFFFF7777, false);
            y += 18;
        }
        if (mouseX >= x && mouseX < x + 16 && mouseY >= top() + 22 && mouseY < top() + 38) {
            g.renderTooltip(font, out, mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
