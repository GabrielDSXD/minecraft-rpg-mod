package com.alvorecer.item.gear;

import com.alvorecer.ability.Abilities;
import com.alvorecer.ability.Ability;
import com.alvorecer.ability.AbilityService;
import com.alvorecer.registry.ModAttachments;
import com.alvorecer.registry.ModGear;
import com.alvorecer.rpg.PlayerRpgData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Cajados e varinhas. Botão direito lança o feitiço do item; o Cajado Arcano ({@code selectable}) lança o feitiço
 * selecionado entre os desbloqueados pelo personagem, e agachado + botão direito troca a seleção.
 */
public class CasterItem extends Item {
    private final String baseSpell;
    private final boolean selectable;

    public CasterItem(Properties properties, String baseSpell, boolean selectable) {
        super(properties);
        this.baseSpell = baseSpell;
        this.selectable = selectable;
    }

    public boolean selectable() {
        return selectable;
    }

    /** Feitiço básico do item + habilidades mágicas já desbloqueadas (raça e classe), se o item permitir seleção. */
    public List<String> spells(PlayerRpgData d) {
        List<String> out = new ArrayList<>();
        out.add(baseSpell);
        if (!selectable || !d.hasCharacter()) return out;
        for (int slot = 0; slot <= AbilityService.SLOT_ULTIMATE; slot++) {
            String id = AbilityService.abilityId(d, slot);
            Ability a = id == null ? null : Abilities.get(id);
            if (a != null && a.school().magical() && d.level >= AbilityService.requiredLevel(slot)) out.add(id);
        }
        return out;
    }

    public String selected(ItemStack stack, PlayerRpgData d) {
        String sel = stack.get(ModGear.SELECTED_SPELL.get());
        List<String> list = spells(d);
        return sel != null && list.contains(sel) ? sel : list.get(0);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            PlayerRpgData d = ModAttachments.get(sp);
            if (selectable && sp.isShiftKeyDown()) {
                List<String> list = spells(d);
                int next = (list.indexOf(selected(stack, d)) + 1) % list.size();
                stack.set(ModGear.SELECTED_SPELL.get(), list.get(next));
                sp.displayClientMessage(Component.translatable("msg.alvorecer.spell_selected",
                        Component.translatable("ability.alvorecer." + list.get(next))), true);
            } else {
                AbilityService.useItemAbility(sp, stack, selected(stack, d));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
