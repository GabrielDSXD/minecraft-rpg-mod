package com.alvorecer.command;

import com.alvorecer.party.Parties;
import com.alvorecer.party.Parties.LootMode;
import com.alvorecer.registry.ModAttachments;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.UUID;

/** /grupo convidar|aceitar|recusar|sair|expulsar|saque|lista */
public final class PartyCommands {
    private PartyCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        var root = Commands.literal("grupo")
                .then(Commands.literal("convidar").then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> Parties.invite(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "player")) ? 1 : 0)))
                .then(Commands.literal("aceitar").executes(c -> Parties.accept(c.getSource().getPlayerOrException()) ? 1 : 0))
                .then(Commands.literal("recusar").executes(c -> {
                    Parties.decline(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("sair").executes(c -> {
                    Parties.leave(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("expulsar").then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> Parties.kick(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "player")) ? 1 : 0)))
                .then(Commands.literal("lista").executes(c -> list(c.getSource().getPlayerOrException())));
        var loot = Commands.literal("saque");
        for (var e : new Object[][]{{"livre", LootMode.FREE}, {"alternado", LootMode.ROUND_ROBIN}, {"aleatorio", LootMode.RANDOM}}) {
            LootMode mode = (LootMode) e[1];
            loot.then(Commands.literal((String) e[0]).executes(c -> Parties.setLoot(c.getSource().getPlayerOrException(), mode) ? 1 : 0));
        }
        d.register(root.then(loot));
    }

    private static int list(ServerPlayer p) {
        var party = Parties.get(p);
        if (party == null) {
            p.sendSystemMessage(Component.translatable("party.alvorecer.none").withStyle(ChatFormatting.GRAY));
            return 0;
        }
        p.sendSystemMessage(Component.translatable("party.alvorecer.list_header",
                Component.translatable("party.alvorecer.loot." + party.loot.name().toLowerCase())).withStyle(ChatFormatting.AQUA));
        for (UUID id : party.members) {
            ServerPlayer m = p.server.getPlayerList().getPlayer(id);
            if (m == null) continue;
            var d = ModAttachments.get(m);
            Component cls = d.rpgClass == null ? Component.literal("?") : Component.translatable(d.rpgClass.langKey());
            p.sendSystemMessage(Component.literal(party.leader.equals(id) ? "★ " : "• ").append(m.getDisplayName())
                    .append(" — ").append(cls).append(" ").append(Component.translatable("ui.alvorecer.level", d.level)));
        }
        return 1;
    }
}
