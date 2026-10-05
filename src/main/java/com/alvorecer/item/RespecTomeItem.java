package com.alvorecer.item;

import com.alvorecer.command.RpgCommands;
import com.alvorecer.config.RpgConfig;
import com.alvorecer.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Tomo do Recomeço: permite refazer raça, classe e profissão quando o modo de troca é ITEM (ou FREE). */
public class RespecTomeItem extends Item {
    public RespecTomeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        if (RpgConfig.RESPEC_MODE.get() == RpgConfig.RespecMode.ADMIN_ONLY) {
            sp.displayClientMessage(Component.translatable("msg.alvorecer.respec_admin_only"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!ModAttachments.get(sp).hasCharacter() || !RpgCommands.respecReady(sp)) return InteractionResultHolder.fail(stack);
        RpgCommands.doReset(sp);
        stack.consume(1, sp);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.alvorecer.respec_tome.tooltip"));
    }
}
