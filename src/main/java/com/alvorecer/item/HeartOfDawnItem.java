package com.alvorecer.item;

import com.alvorecer.config.RpgConfig;
import com.alvorecer.registry.ModAttachments;
import com.alvorecer.registry.ModGear;
import com.alvorecer.rpg.PlayerRpgData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Coração do Alvorecer (lendário, nível 15). Ativa (botão direito, recarga 5 min): Aurora Renascente — cura o portador e o
 * grupo próximo. Passiva: Último Alvorecer — carregado em qualquer lugar do inventário, impede uma morte e devolve metade
 * da vida; recarga configurável (20 min por padrão). Só um por jogador como recompensa do Guardião.
 */
public class HeartOfDawnItem extends Item {
    public static final String REVIVE_KEY = "heart_revive";
    public static final int LEVEL = 15;

    public HeartOfDawnItem(Properties properties) {
        super(properties);
    }

    public static long reviveCooldownTicks() {
        return RpgConfig.HEART_REVIVE_MINUTES.get() * 60L * 20L;
    }

    /** Tenta salvar o jogador da morte. @return true se salvou */
    public static boolean tryRevive(ServerPlayer p) {
        if (!p.getInventory().contains(s -> s.is(ModGear.HEART_OF_DAWN.get()))) return false;
        PlayerRpgData d = ModAttachments.get(p);
        long now = p.level().getGameTime();
        if (d.hasCharacter() && d.level < LEVEL) return false;
        if (d.cooldowns.getOrDefault(REVIVE_KEY, 0L) > now) return false;
        d.cooldowns.put(REVIVE_KEY, now + reviveCooldownTicks());
        p.setHealth(p.getMaxHealth() * 0.5f);
        p.removeAllEffects();
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
        p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));
        p.invulnerableTime = 40;
        ServerLevel level = p.serverLevel();
        level.playSound(null, p.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1f, 1.2f);
        level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 60, 0.6, 1, 0.6, 0.2);
        p.displayClientMessage(Component.translatable("msg.alvorecer.heart_revive").withStyle(ChatFormatting.GOLD), true);
        p.syncData(ModAttachments.RPG);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.alvorecer.heart_of_dawn.passive", RpgConfig.HEART_REVIVE_MINUTES.getDefault())
                .withStyle(ChatFormatting.GRAY));
        var player = net.neoforged.fml.loading.FMLEnvironment.dist.isClient() ? ClientAccess.player() : null;
        if (player == null) return;
        long left = ModAttachments.get(player).cooldowns.getOrDefault(REVIVE_KEY, 0L) - player.level().getGameTime();
        tooltip.add(left > 0 ? Component.translatable("item.alvorecer.heart_of_dawn.recharging", (left + 1199) / 1200).withStyle(ChatFormatting.RED)
                : Component.translatable("item.alvorecer.heart_of_dawn.ready").withStyle(ChatFormatting.GREEN));
    }

    /** Separado para não carregar classes de cliente no servidor dedicado. */
    private static final class ClientAccess {
        static net.minecraft.world.entity.player.Player player() {
            return net.minecraft.client.Minecraft.getInstance().player;
        }
    }
}
