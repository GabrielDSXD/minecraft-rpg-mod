package com.alvorecer.rpg;

import com.alvorecer.config.RpgConfig;
import com.alvorecer.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Ganho de XP de classe e de profissão, com subida de nível. */
public final class Leveling {
    private Leveling() {
    }

    /**
     * Aplica XP e sobe quantos níveis couberem. Pura (sem jogador) para testes.
     *
     * @return número de níveis ganhos
     */
    public static int addXp(PlayerRpgData d, long amount) {
        if (amount <= 0 || d.level >= Progression.MAX_LEVEL) return 0;
        d.xp += amount;
        int gained = 0;
        while (d.level < Progression.MAX_LEVEL && d.xp >= Progression.xpToNext(d.level)) {
            d.xp -= Progression.xpToNext(d.level);
            d.level++;
            d.unspentPoints += Progression.statPointsAt(d.level);
            gained++;
        }
        if (d.level >= Progression.MAX_LEVEL) d.xp = 0;
        return gained;
    }

    /** XP de classe com multiplicador da configuração e bônus humano de +10%. */
    public static void grantXp(ServerPlayer p, long base) {
        PlayerRpgData d = ModAttachments.get(p);
        if (!d.hasCharacter() || base <= 0) return;
        double mult = RpgConfig.XP_MULTIPLIER.get() * (d.race == Race.HUMAN ? 1.1 : 1);
        long amount = Math.round(base * mult);
        int oldLevel = d.level;
        int gained = addXp(d, amount);
        if (gained > 0) {
            StatApplier.apply(p);
            d.mana = Derived.maxMana(d);
            d.energy = Derived.maxEnergy(d);
            p.setHealth(p.getMaxHealth());
            p.sendSystemMessage(Component.translatable("msg.alvorecer.level_up", d.level, d.unspentPoints).withStyle(ChatFormatting.GOLD));
            for (int i = 0; i < RpgClass.UNLOCK_LEVELS.length; i++) {
                int unlock = RpgClass.UNLOCK_LEVELS[i];
                if (unlock > oldLevel && unlock <= d.level) {
                    p.sendSystemMessage(Component.translatable("msg.alvorecer.ability_unlocked",
                            Component.translatable("ability.alvorecer." + d.rpgClass.abilities.get(i))).withStyle(ChatFormatting.AQUA));
                }
            }
            if (oldLevel < RpgConfig.SECONDARY_PROFESSION_LEVEL.get() && d.level >= RpgConfig.SECONDARY_PROFESSION_LEVEL.get()) {
                p.sendSystemMessage(Component.translatable("msg.alvorecer.secondary_unlocked").withStyle(ChatFormatting.AQUA));
            }
            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 0.8f);
        }
        p.syncData(ModAttachments.RPG);
    }

    /** Avança o dia do limite anti-farm e devolve quanto XP de profissão ainda pode ser ganho hoje. */
    public static long professionAllowance(PlayerRpgData d, long gameTime, int dailyCap) {
        long day = gameTime / 24000L;
        if (day != d.professionDay) {
            d.professionDay = day;
            d.professionXpToday = 0;
        }
        return dailyCap <= 0 ? Long.MAX_VALUE : Math.max(0, dailyCap - d.professionXpToday);
    }

    /** XP de profissão: total para a principal, fração configurável para a secundária, sujeito ao limite diário. */
    public static void grantProfessionXp(ServerPlayer p, Profession prof, long base) {
        PlayerRpgData d = ModAttachments.get(p);
        if (!d.hasCharacter() || base <= 0) return;
        boolean primary = d.profession == prof;
        boolean secondary = d.secondaryProfession == prof;
        if (!primary && !secondary) return;
        double mult = RpgConfig.PROFESSION_XP_MULTIPLIER.get();
        if (secondary) mult *= RpgConfig.SECONDARY_PROFESSION_XP_FACTOR.get();
        if (d.race == Race.GNOME && (prof == Profession.ALCHEMIST || prof == Profession.ARTISAN || prof == Profession.ENCHANTER)) mult *= 1.25;
        long amount = Math.min(Math.round(base * mult), professionAllowance(d, p.level().getGameTime(), RpgConfig.PROFESSION_DAILY_CAP.get()));
        if (amount <= 0) return;
        d.professionXpToday += amount;

        int level = primary ? d.professionLevel : d.secondaryLevel;
        long xp = (primary ? d.professionXp : d.secondaryXp) + amount;
        int before = level;
        while (level < Progression.MAX_PROFESSION_LEVEL && xp >= Progression.professionXpToNext(level)) {
            xp -= Progression.professionXpToNext(level);
            level++;
        }
        if (level >= Progression.MAX_PROFESSION_LEVEL) xp = 0;
        if (primary) {
            d.professionLevel = level;
            d.professionXp = xp;
        } else {
            d.secondaryLevel = level;
            d.secondaryXp = xp;
        }
        if (level > before) {
            p.sendSystemMessage(Component.translatable("msg.alvorecer.profession_level_up",
                    Component.translatable(prof.langKey()), level).withStyle(ChatFormatting.GREEN));
            p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_WORK_TOOLSMITH, SoundSource.PLAYERS, 1f, 1f);
            if (prof == Profession.MINER) StatApplier.apply(p);
        }
        p.syncData(ModAttachments.RPG);
    }
}
