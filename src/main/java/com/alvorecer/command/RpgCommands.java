package com.alvorecer.command;

import com.alvorecer.config.RpgConfig;
import com.alvorecer.network.ModNetwork;
import com.alvorecer.registry.ModAttachments;
import com.alvorecer.rpg.Derived;
import com.alvorecer.rpg.Leveling;
import com.alvorecer.rpg.PlayerRpgData;
import com.alvorecer.rpg.Progression;
import com.alvorecer.rpg.Stat;
import com.alvorecer.rpg.StatApplier;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Arrays;

/**
 * /rpg info [jogador] — ficha resumida (qualquer um, sobre si; admin sobre outros)
 * /rpg respec — troca de escolhas pelo próprio jogador (modo FREE)
 * /rpg reset|setlevel|addxp|proflevel|cooldowns — administração (nível de permissão 2)
 */
public final class RpgCommands {
    private RpgCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("rpg")
                .then(Commands.literal("info")
                        .executes(c -> info(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).requires(s -> s.hasPermission(2))
                                .executes(c -> info(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("respec").executes(RpgCommands::selfRespec))
                .then(Commands.literal("reset").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> reset(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("setlevel").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, Progression.MAX_LEVEL))
                                        .executes(c -> setLevel(c, EntityArgument.getPlayer(c, "player"), IntegerArgumentType.getInteger(c, "level"))))))
                .then(Commands.literal("addxp").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(c -> {
                                            ServerPlayer p = EntityArgument.getPlayer(c, "player");
                                            Leveling.grantXp(p, IntegerArgumentType.getInteger(c, "amount"));
                                            return 1;
                                        }))))
                .then(Commands.literal("proflevel").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, Progression.MAX_PROFESSION_LEVEL))
                                        .executes(c -> profLevel(c, EntityArgument.getPlayer(c, "player"), IntegerArgumentType.getInteger(c, "level"))))))
                .then(Commands.literal("resettalents").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> {
                                    ServerPlayer p = EntityArgument.getPlayer(c, "player");
                                    PlayerRpgData data = ModAttachments.get(p);
                                    data.talents.clear();
                                    data.specialization = null;
                                    StatApplier.apply(p);
                                    p.syncData(ModAttachments.RPG);
                                    c.getSource().sendSuccess(() -> Component.translatable("cmd.alvorecer.resettalents", p.getDisplayName()), true);
                                    return 1;
                                })))
                .then(Commands.literal("cooldowns").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> {
                                    ServerPlayer p = EntityArgument.getPlayer(c, "player");
                                    ModAttachments.get(p).cooldowns.clear();
                                    p.syncData(ModAttachments.RPG);
                                    c.getSource().sendSuccess(() -> Component.translatable("cmd.alvorecer.cooldowns", p.getDisplayName()), true);
                                    return 1;
                                }))));
    }

    private static int info(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        PlayerRpgData d = ModAttachments.get(p);
        if (!d.hasCharacter()) {
            c.getSource().sendFailure(Component.translatable("cmd.alvorecer.no_character", p.getDisplayName()));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.translatable("cmd.alvorecer.info", p.getDisplayName(),
                Component.translatable(d.race.langKey()), Component.translatable(d.rpgClass.langKey()), d.level, d.xp,
                Progression.xpToNext(d.level)).withStyle(ChatFormatting.GOLD), false);
        var stats = Component.empty();
        for (Stat s : Stat.values()) stats.append(Component.translatable(s.langKey() + ".short")).append(" " + d.score(s) + "  ");
        c.getSource().sendSuccess(() -> stats, false);
        c.getSource().sendSuccess(() -> Component.translatable("cmd.alvorecer.info2", Math.round(d.mana), Math.round(Derived.maxMana(d)),
                Math.round(d.energy), Math.round(Derived.maxEnergy(d)), Component.translatable(d.profession.langKey()), d.professionLevel), false);
        return 1;
    }

    /** Apaga as escolhas e reabre a criação. Usado pelo comando de admin, pelo Tomo e pelo modo FREE. */
    public static void doReset(ServerPlayer p) {
        PlayerRpgData d = ModAttachments.get(p);
        d.resetCharacter();
        d.lastRespecTime = p.level().getGameTime();
        StatApplier.apply(p);
        p.syncData(ModAttachments.RPG);
        ModNetwork.openScreen(p, ModNetwork.Screen.CREATION);
    }

    /** Verifica a recarga de troca feita pelo jogador; envia a mensagem de erro se ainda não pode. */
    public static boolean respecReady(ServerPlayer p) {
        PlayerRpgData d = ModAttachments.get(p);
        long wait = RpgConfig.RESPEC_COOLDOWN_MINUTES.get() * 60L * 20L;
        long remaining = d.lastRespecTime == Long.MIN_VALUE ? 0 : d.lastRespecTime + wait - p.level().getGameTime();
        if (remaining > 0) {
            p.displayClientMessage(Component.translatable("msg.alvorecer.respec_wait", (remaining + 1199) / 1200), false);
            return false;
        }
        return true;
    }

    private static int selfRespec(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        if (RpgConfig.RESPEC_MODE.get() != RpgConfig.RespecMode.FREE) {
            c.getSource().sendFailure(Component.translatable("cmd.alvorecer.respec_disabled", RpgConfig.RESPEC_MODE.get().name()));
            return 0;
        }
        if (!respecReady(p)) return 0;
        doReset(p);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        doReset(p);
        ModAttachments.get(p).lastRespecTime = Long.MIN_VALUE;
        c.getSource().sendSuccess(() -> Component.translatable("cmd.alvorecer.reset", p.getDisplayName()), true);
        return 1;
    }

    private static int setLevel(CommandContext<CommandSourceStack> c, ServerPlayer p, int level) {
        PlayerRpgData d = ModAttachments.get(p);
        d.level = level;
        d.xp = 0;
        int spent = Arrays.stream(d.allocated).sum();
        int total = d.totalStatPoints();
        if (spent > total) {
            Arrays.fill(d.allocated, 0);
            spent = 0;
        }
        d.unspentPoints = d.hasCharacter() ? total - spent : 0;
        StatApplier.apply(p);
        p.syncData(ModAttachments.RPG);
        c.getSource().sendSuccess(() -> Component.translatable("cmd.alvorecer.setlevel", p.getDisplayName(), level), true);
        return 1;
    }

    private static int profLevel(CommandContext<CommandSourceStack> c, ServerPlayer p, int level) {
        PlayerRpgData d = ModAttachments.get(p);
        d.professionLevel = level;
        d.professionXp = 0;
        StatApplier.apply(p);
        p.syncData(ModAttachments.RPG);
        c.getSource().sendSuccess(() -> Component.translatable("cmd.alvorecer.proflevel", p.getDisplayName(), level), true);
        return 1;
    }
}
