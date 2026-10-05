package com.alvorecer.event;

import com.alvorecer.ability.AbilityService;
import com.alvorecer.item.gear.CasterItem;
import com.alvorecer.item.gear.GearInfo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Habilidades das armas especiais: botão direito com a arma na mão principal. */
public final class GearEvents {
    private GearEvents() {
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        GearInfo gear = GearInfo.get(stack.getItem());
        if (gear == null || gear.ability() == null || stack.getItem() instanceof CasterItem) return;
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        if (event.getEntity() instanceof ServerPlayer p) AbilityService.useItemAbility(p, stack, gear.ability());
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
    }
}
