package com.alvorecer.party;

import com.alvorecer.config.RpgConfig;
import com.alvorecer.network.ModNetwork;
import com.alvorecer.registry.ModAttachments;
import com.alvorecer.rpg.Leveling;
import com.alvorecer.rpg.PlayerRpgData;
import com.alvorecer.quest.Quests;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Grupos de jogadores (em memória: um grupo dura enquanto o servidor estiver ligado e os membros online).
 * Só é acessado no thread do servidor.
 */
public final class Parties {
    public enum LootMode { FREE, ROUND_ROBIN, RANDOM }

    public static final class Party {
        public UUID leader;
        public final List<UUID> members = new ArrayList<>();
        public LootMode loot = LootMode.FREE;
        int next;

        Party(UUID leader) {
            this.leader = leader;
            members.add(leader);
        }
    }

    private record Invite(Party party, long expires) {
    }

    private static final Map<UUID, Party> BY_MEMBER = new HashMap<>();
    private static final Map<UUID, Invite> INVITES = new HashMap<>();

    private Parties() {
    }

    @Nullable
    public static Party get(Player p) {
        return BY_MEMBER.get(p.getUUID());
    }

    public static boolean sameParty(Player a, Player b) {
        Party pa = BY_MEMBER.get(a.getUUID());
        return pa != null && pa == BY_MEMBER.get(b.getUUID());
    }

    private static void msg(ServerPlayer p, String key, Object... args) {
        p.sendSystemMessage(Component.translatable("party.alvorecer." + key, args).withStyle(ChatFormatting.AQUA));
    }

    private static List<ServerPlayer> online(MinecraftServer server, Party party) {
        List<ServerPlayer> list = new ArrayList<>();
        for (UUID id : party.members) {
            ServerPlayer sp = server.getPlayerList().getPlayer(id);
            if (sp != null) list.add(sp);
        }
        return list;
    }

    private static void broadcast(MinecraftServer server, Party party, String key, Object... args) {
        online(server, party).forEach(p -> msg(p, key, args));
    }

    // ------------------------------------------------------------ gestão

    public static boolean invite(ServerPlayer from, ServerPlayer to) {
        if (from == to) return false;
        if (BY_MEMBER.containsKey(to.getUUID())) {
            msg(from, "already_in_party", to.getDisplayName());
            return false;
        }
        Party party = BY_MEMBER.get(from.getUUID());
        if (party == null) {
            party = new Party(from.getUUID());
            BY_MEMBER.put(from.getUUID(), party);
        } else if (!party.leader.equals(from.getUUID())) {
            msg(from, "not_leader");
            return false;
        }
        if (party.members.size() >= RpgConfig.PARTY_MAX_SIZE.get()) {
            msg(from, "full");
            return false;
        }
        INVITES.put(to.getUUID(), new Invite(party, from.level().getGameTime() + 1200));
        msg(from, "invite_sent", to.getDisplayName());
        msg(to, "invite_received", from.getDisplayName());
        return true;
    }

    public static boolean accept(ServerPlayer p) {
        Invite inv = INVITES.remove(p.getUUID());
        if (inv == null || inv.expires() < p.level().getGameTime() || !BY_MEMBER.containsValue(inv.party())) {
            msg(p, "no_invite");
            return false;
        }
        if (BY_MEMBER.containsKey(p.getUUID())) leave(p);
        if (inv.party().members.size() >= RpgConfig.PARTY_MAX_SIZE.get()) {
            msg(p, "full");
            return false;
        }
        inv.party().members.add(p.getUUID());
        BY_MEMBER.put(p.getUUID(), inv.party());
        broadcast(p.server, inv.party(), "joined", p.getDisplayName());
        online(p.server, inv.party()).forEach(m -> com.alvorecer.achievement.Achievements.grant(m, "party"));
        sync(p.server, inv.party());
        return true;
    }

    public static void decline(ServerPlayer p) {
        if (INVITES.remove(p.getUUID()) != null) msg(p, "declined");
    }

    public static void leave(ServerPlayer p) {
        Party party = BY_MEMBER.remove(p.getUUID());
        if (party == null) return;
        party.members.remove(p.getUUID());
        msg(p, "left_self");
        sendEmpty(p);
        if (party.members.size() <= 1) {
            for (UUID id : party.members) {
                BY_MEMBER.remove(id);
                ServerPlayer last = p.server.getPlayerList().getPlayer(id);
                if (last != null) {
                    msg(last, "disbanded");
                    sendEmpty(last);
                }
            }
            party.members.clear();
            return;
        }
        if (party.leader.equals(p.getUUID())) party.leader = party.members.get(0);
        broadcast(p.server, party, "left", p.getDisplayName());
        sync(p.server, party);
    }

    public static boolean kick(ServerPlayer leader, ServerPlayer target) {
        Party party = BY_MEMBER.get(leader.getUUID());
        if (party == null || !party.leader.equals(leader.getUUID())) {
            msg(leader, "not_leader");
            return false;
        }
        if (BY_MEMBER.get(target.getUUID()) != party || target == leader) return false;
        leave(target);
        return true;
    }

    public static boolean setLoot(ServerPlayer leader, LootMode mode) {
        Party party = BY_MEMBER.get(leader.getUUID());
        if (party == null || !party.leader.equals(leader.getUUID())) {
            msg(leader, "not_leader");
            return false;
        }
        party.loot = mode;
        broadcast(leader.server, party, "loot_changed", Component.translatable("party.alvorecer.loot." + mode.name().toLowerCase()));
        return true;
    }

    public static void clear() {
        BY_MEMBER.clear();
        INVITES.clear();
    }

    // ------------------------------------------------------------ XP e saque

    /** XP por membro: total × (1 + bônus × (n − 1)) ÷ n, arredondado. */
    public static long shareXp(long xp, int members, double bonusPerMember) {
        if (members <= 1) return xp;
        return Math.round(xp * (1 + bonusPerMember * (members - 1)) / members);
    }

    /** Membros online no mesmo mundo e dentro do alcance do grupo (inclui o próprio jogador). */
    public static List<ServerPlayer> nearbyMembers(ServerPlayer p) {
        Party party = BY_MEMBER.get(p.getUUID());
        if (party == null) return List.of(p);
        double range = RpgConfig.PARTY_RANGE.get();
        return online(p.server, party).stream()
                .filter(m -> m == p || (m.level() == p.level() && m.distanceToSqr(p) <= range * range && m.isAlive()))
                .toList();
    }

    public static void grantKillXp(ServerPlayer killer, LivingEntity victim, long xp) {
        List<ServerPlayer> members = RpgConfig.PARTY_SHARE_XP.get() ? nearbyMembers(killer) : List.of(killer);
        long each = shareXp(xp, members.size(), RpgConfig.PARTY_XP_BONUS.get());
        for (ServerPlayer m : members) Leveling.grantXp(m, each);
        for (ServerPlayer m : nearbyMembers(killer)) Quests.onKill(m, victim);
    }

    /** Distribui os itens de um abate conforme a regra do grupo (só quem foi escolhido pode pegar). */
    public static void assignLoot(ServerPlayer killer, List<ItemEntity> drops) {
        Party party = BY_MEMBER.get(killer.getUUID());
        if (party == null || party.loot == LootMode.FREE || drops.isEmpty()) return;
        List<ServerPlayer> members = nearbyMembers(killer);
        for (ItemEntity drop : drops) {
            ServerPlayer owner = switch (party.loot) {
                case ROUND_ROBIN -> members.get(Math.floorMod(party.next++, members.size()));
                case RANDOM -> members.get(killer.getRandom().nextInt(members.size()));
                case FREE -> killer;
            };
            drop.setTarget(owner.getUUID());
        }
    }

    // ------------------------------------------------------------ sincronização

    public static void tick(MinecraftServer server) {
        INVITES.values().removeIf(i -> i.expires() < server.overworld().getGameTime());
        for (Party party : new HashMap<>(BY_MEMBER).values().stream().distinct().toList()) sync(server, party);
    }

    public static void sync(MinecraftServer server, Party party) {
        List<ModNetwork.PartyMember> list = new ArrayList<>();
        for (ServerPlayer m : online(server, party)) {
            PlayerRpgData d = ModAttachments.get(m);
            list.add(new ModNetwork.PartyMember(m.getGameProfile().getName(), m.getHealth(), m.getMaxHealth(), d.level,
                    d.rpgClass == null ? -1 : d.rpgClass.ordinal(), party.leader.equals(m.getUUID())));
        }
        var payload = new ModNetwork.PartySync(list);
        for (ServerPlayer m : online(server, party)) {
            if (m.connection.hasChannel(ModNetwork.PartySync.TYPE)) PacketDistributor.sendToPlayer(m, payload);
        }
    }

    private static void sendEmpty(ServerPlayer p) {
        if (p.connection.hasChannel(ModNetwork.PartySync.TYPE)) PacketDistributor.sendToPlayer(p, new ModNetwork.PartySync(List.of()));
    }
}
