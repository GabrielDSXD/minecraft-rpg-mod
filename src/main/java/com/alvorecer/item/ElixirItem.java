package com.alvorecer.item;

import com.alvorecer.registry.ModAttachments;
import com.alvorecer.rpg.Derived;
import com.alvorecer.rpg.PlayerRpgData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/** Elixires de profissão (bebidos) e o Unguento Curativo (aplicado). */
public class ElixirItem extends Item {
    public enum Kind { MANA, VIGOR, SALVE }

    private final Kind kind;

    public ElixirItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return kind == Kind.SALVE ? UseAnim.BOW : UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return kind == Kind.SALVE ? 20 : 32;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer p) {
            PlayerRpgData d = ModAttachments.get(p);
            switch (kind) {
                case MANA -> d.mana = Math.min(Derived.maxMana(d), d.mana + Derived.maxMana(d) * 0.4f);
                case VIGOR -> {
                    d.energy = Math.min(Derived.maxEnergy(d), d.energy + Derived.maxEnergy(d) * 0.5f);
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0));
                }
                case SALVE -> {
                    p.heal(8);
                    p.removeEffect(MobEffects.POISON);
                }
            }
            p.syncData(ModAttachments.RPG);
        }
        if (entity instanceof Player p && p.getAbilities().instabuild) return stack;
        stack.shrink(1);
        if (kind == Kind.SALVE) return stack;
        if (stack.isEmpty()) return new ItemStack(Items.GLASS_BOTTLE);
        if (entity instanceof Player p && !p.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) p.drop(new ItemStack(Items.GLASS_BOTTLE), false);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.alvorecer." + switch (kind) {
            case MANA -> "mana_elixir";
            case VIGOR -> "vigor_elixir";
            case SALVE -> "healing_salve";
        } + ".tooltip"));
    }
}
