package com.alvorecer.item;

import com.alvorecer.registry.ModWorld;
import com.alvorecer.world.PortalShape;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Chave do Alvorecer: usada numa moldura completa de Pedra do Alvorecer (interior de 2×3 a 21×21) no mundo principal
 * ou nos Reinos do Éter, acende o Portal do Alvorecer. Gasta 1 de durabilidade por ativação.
 */
public class DawnKeyItem extends Item {
    public DawnKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (!level.getBlockState(ctx.getClickedPos()).is(ModWorld.DAWNSTONE.get())) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        var player = ctx.getPlayer();
        boolean allowed = level.dimension() == Level.OVERWORLD || level.dimension() == ModWorld.AETHER;
        PortalShape shape = allowed ? PortalShape.find(level, ctx.getClickedPos().relative(ctx.getClickedFace())) : null;
        if (shape == null) {
            if (player != null) player.displayClientMessage(Component.translatable(allowed ? "msg.alvorecer.portal_invalid" : "msg.alvorecer.portal_dimension"), true);
            return InteractionResult.FAIL;
        }
        shape.fill(level);
        level.playSound(null, ctx.getClickedPos(), SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.8f, 1.4f);
        ItemStack stack = ctx.getItemInHand();
        if (player != null) {
            stack.hurtAndBreak(1, player, ctx.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) com.alvorecer.achievement.Achievements.grant(sp, "portal");
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.alvorecer.dawn_key.tooltip"));
    }
}
