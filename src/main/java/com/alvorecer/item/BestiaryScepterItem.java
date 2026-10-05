package com.alvorecer.item;

import com.alvorecer.ability.AbilityUtil;
import com.alvorecer.registry.ModMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Cetro do Bestiário (ferramenta administrativa): agachado + botão direito escolhe a criatura; botão direito a invoca
 * no ponto mirado. Só funciona no modo criativo ou para operadores (nível 2).
 */
public class BestiaryScepterItem extends Item {
    public BestiaryScepterItem(Properties properties) {
        super(properties);
    }

    private static String selected(ItemStack stack) {
        String s = stack.get(ModMobs.SELECTED_CREATURE.get());
        return s != null && ModMobs.CREATURES.containsKey(s) ? s : ModMobs.CREATURES.keySet().iterator().next();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        if (!sp.isCreative() && !sp.hasPermissions(2)) {
            sp.displayClientMessage(Component.translatable("msg.alvorecer.scepter_denied"), true);
            return InteractionResultHolder.fail(stack);
        }
        String sel = selected(stack);
        if (sp.isShiftKeyDown()) {
            List<String> keys = new ArrayList<>(ModMobs.CREATURES.keySet());
            String next = keys.get((keys.indexOf(sel) + 1) % keys.size());
            stack.set(ModMobs.SELECTED_CREATURE.get(), next);
            sp.displayClientMessage(Component.translatable("msg.alvorecer.scepter_selected",
                    ModMobs.CREATURES.get(next).get().getDescription()), true);
        } else {
            Vec3 at = AbilityUtil.aimPoint(sp, 12);
            ModMobs.CREATURES.get(sel).get().spawn(sp.serverLevel(), BlockPos.containing(at), MobSpawnType.SPAWN_EGG);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.alvorecer.bestiary_scepter.tooltip", ModMobs.CREATURES.get(selected(stack)).get().getDescription()));
    }
}
