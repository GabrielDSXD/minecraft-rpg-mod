package com.alvorecer.quest;

import com.alvorecer.achievement.Achievements;
import com.alvorecer.config.RpgConfig;
import com.alvorecer.registry.ModAttachments;
import com.alvorecer.rpg.Leveling;
import com.alvorecer.rpg.PlayerRpgData;
import com.alvorecer.quest.Quest.Category;
import com.alvorecer.quest.Quest.Objective;
import com.alvorecer.quest.Quest.RewardItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Catálogo de missões do Mural e regras de aceitar, progredir, entregar e abandonar (validadas no servidor). */
public final class Quests {
    public enum Status { LOCKED, AVAILABLE, ACTIVE, READY, COOLDOWN, DONE }

    private static final Map<String, Quest> ALL = new LinkedHashMap<>();

    private Quests() {
    }

    private static void q(String id, Category c, int minLevel, Objective o, String target, int count, long xp, long profXp,
                          boolean repeatable, RewardItem... items) {
        ALL.put(id, new Quest(id, c, minLevel, o, target, count, xp, profXp, List.of(items), repeatable));
    }

    private static RewardItem item(String id, int count) {
        return new RewardItem(id, count);
    }

    static {
        // Combate
        q("zombie_hunt", Category.COMBAT, 1, Objective.KILL, "minecraft:zombie", 10, 120, 0, true, item("minecraft:iron_ingot", 3));
        q("bone_breakers", Category.COMBAT, 2, Objective.KILL, "minecraft:skeleton", 10, 150, 0, true, item("minecraft:arrow", 16));
        q("web_cutters", Category.COMBAT, 3, Objective.KILL, "minecraft:spider", 8, 150, 0, true, item("minecraft:string", 8));
        q("creeper_watch", Category.COMBAT, 4, Objective.KILL, "minecraft:creeper", 5, 250, 0, true, item("minecraft:emerald", 2));
        q("rest_for_the_dead", Category.COMBAT, 6, Objective.KILL, "#minecraft:undead", 25, 500, 0, false, item("minecraft:golden_apple", 1));
        q("void_walkers", Category.COMBAT, 10, Objective.KILL, "minecraft:enderman", 5, 800, 0, false, item("minecraft:diamond", 1));
        q("flame_hunters", Category.COMBAT, 12, Objective.KILL, "minecraft:blaze", 6, 1000, 0, false, item("minecraft:blaze_rod", 4));
        // Exploração
        q("first_steps", Category.EXPLORATION, 1, Objective.BIOMES, "", 5, 150, 0, false, item("minecraft:map", 1));
        q("wanderer", Category.EXPLORATION, 5, Objective.BIOMES, "", 15, 600, 0, false, item("minecraft:spyglass", 1));
        q("into_the_nether", Category.EXPLORATION, 5, Objective.VISIT_DIMENSION, "minecraft:the_nether", 1, 400, 0, false, item("minecraft:fire_charge", 4));
        q("the_end", Category.EXPLORATION, 15, Objective.VISIT_DIMENSION, "minecraft:the_end", 1, 2000, 0, false, item("minecraft:ender_pearl", 4));
        // Coleta (entrega no mural)
        q("lumber", Category.GATHERING, 1, Objective.GATHER, "#minecraft:logs", 32, 100, 30, true, item("minecraft:emerald", 1));
        q("coal_supply", Category.GATHERING, 1, Objective.GATHER, "minecraft:coal", 32, 120, 30, true, item("minecraft:emerald", 1));
        q("iron_supply", Category.GATHERING, 4, Objective.GATHER, "minecraft:iron_ingot", 16, 250, 60, true, item("minecraft:emerald", 3));
        q("harvest", Category.GATHERING, 1, Objective.GATHER, "minecraft:wheat", 32, 120, 30, true, item("minecraft:bread", 6));
        // Profissão
        q("apprentice", Category.PROFESSION, 1, Objective.PROFESSION_LEVEL, "", 5, 300, 0, false, item("minecraft:emerald", 4));
        q("master_craft", Category.PROFESSION, 5, Objective.PROFESSION_LEVEL, "", 15, 1500, 0, false, item("minecraft:diamond", 2));
        // Progressão
        q("adventurer", Category.PROGRESSION, 1, Objective.REACH_LEVEL, "", 5, 200, 0, false, item("minecraft:golden_carrot", 8));
        q("veteran", Category.PROGRESSION, 5, Objective.REACH_LEVEL, "", 10, 800, 0, false, item("alvorecer:respec_tome", 1));
        q("hero", Category.PROGRESSION, 10, Objective.REACH_LEVEL, "", 15, 2000, 0, false, item("minecraft:diamond", 3));
    }

    @Nullable
    public static Quest get(String id) {
        return ALL.get(id);
    }

    public static Map<String, Quest> all() {
        return ALL;
    }

    // ------------------------------------------------------------ progresso e estado

    static boolean matchesEntity(Quest q, EntityType<?> type) {
        if (q.target().startsWith("#")) {
            ResourceLocation tag = ResourceLocation.tryParse(q.target().substring(1));
            return tag != null && type.is(TagKey.create(Registries.ENTITY_TYPE, tag));
        }
        return BuiltInRegistries.ENTITY_TYPE.getKey(type).toString().equals(q.target());
    }

    static boolean matchesItem(Quest q, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (q.target().startsWith("#")) {
            ResourceLocation tag = ResourceLocation.tryParse(q.target().substring(1));
            return tag != null && stack.is(TagKey.create(Registries.ITEM, tag));
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(q.target());
    }

    public static int countItems(Player p, Quest q) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (matchesItem(q, s)) n += s.getCount();
        return n;
    }

    /** Progresso atual (funciona no cliente e no servidor). */
    public static int progress(Player p, PlayerRpgData d, Quest q) {
        int raw = switch (q.objective()) {
            case KILL, VISIT_DIMENSION -> d.activeQuests.getOrDefault(q.id(), 0);
            case GATHER -> countItems(p, q);
            case REACH_LEVEL -> d.level;
            case PROFESSION_LEVEL -> d.professionLevel;
            case BIOMES -> d.visitedBiomes.size();
        };
        return Math.min(raw, q.count());
    }

    public static Status status(Player p, PlayerRpgData d, Quest q, long now) {
        if (d.activeQuests.containsKey(q.id())) return progress(p, d, q) >= q.count() ? Status.READY : Status.ACTIVE;
        Long done = d.completedQuests.get(q.id());
        if (done != null) {
            if (!q.repeatable()) return Status.DONE;
            if (now < done + Quest.REPEAT_TICKS) return Status.COOLDOWN;
        }
        return d.level < q.minLevel() ? Status.LOCKED : Status.AVAILABLE;
    }

    // ------------------------------------------------------------ ações (servidor)

    private static void msg(ServerPlayer p, String key, Object... args) {
        p.displayClientMessage(Component.translatable("quest.alvorecer.msg." + key, args), true);
    }

    /** O mural precisa estar a até 8 blocos (a posição é registrada quando o jogador o abre). */
    private static boolean nearBoard(ServerPlayer p, PlayerRpgData d) {
        BlockPos pos = d.lastBoardPos;
        return pos != null && p.level().getBlockState(pos).is(com.alvorecer.registry.ModBlocks.QUEST_BOARD.get())
                && p.blockPosition().closerThan(pos, 8);
    }

    public static boolean accept(ServerPlayer p, String id) {
        PlayerRpgData d = ModAttachments.get(p);
        Quest q = ALL.get(id);
        if (q == null || !d.hasCharacter() || !nearBoard(p, d)) return false;
        if (status(p, d, q, p.level().getGameTime()) != Status.AVAILABLE) return false;
        if (d.activeQuests.size() >= RpgConfig.MAX_ACTIVE_QUESTS.get()) {
            msg(p, "too_many", RpgConfig.MAX_ACTIVE_QUESTS.get());
            return false;
        }
        d.activeQuests.put(id, 0);
        msg(p, "accepted", Component.translatable(q.nameKey()));
        p.syncData(ModAttachments.RPG);
        return true;
    }

    public static boolean abandon(ServerPlayer p, String id) {
        PlayerRpgData d = ModAttachments.get(p);
        if (d.activeQuests.remove(id) == null) return false;
        p.syncData(ModAttachments.RPG);
        return true;
    }

    public static boolean complete(ServerPlayer p, String id) {
        PlayerRpgData d = ModAttachments.get(p);
        Quest q = ALL.get(id);
        if (q == null || !nearBoard(p, d) || status(p, d, q, p.level().getGameTime()) != Status.READY) return false;
        if (q.objective() == Objective.GATHER) {
            int left = q.count();
            for (ItemStack s : p.getInventory().items) {
                if (left <= 0) break;
                if (!matchesItem(q, s)) continue;
                int take = Math.min(left, s.getCount());
                s.shrink(take);
                left -= take;
            }
            if (left > 0) return false;
        }
        d.activeQuests.remove(id);
        d.completedQuests.put(id, p.level().getGameTime());
        d.questsCompleted++;
        for (RewardItem r : q.items()) {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(r.id()));
            ItemStack stack = new ItemStack(item, r.count());
            if (!p.getInventory().add(stack)) p.drop(stack, false);
        }
        p.sendSystemMessage(Component.translatable("quest.alvorecer.msg.completed", Component.translatable(q.nameKey()), q.xp())
                .withStyle(ChatFormatting.GOLD));
        p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.6f, 1.2f);
        Leveling.grantXp(p, q.xp());
        if (q.professionXp() > 0 && d.profession != null) Leveling.grantProfessionXp(p, d.profession, q.professionXp());
        Achievements.grant(p, "first_quest");
        if (d.questsCompleted >= 10) Achievements.grant(p, "quests_10");
        p.syncData(ModAttachments.RPG);
        return true;
    }

    // ------------------------------------------------------------ eventos de progresso

    public static void onKill(ServerPlayer p, LivingEntity victim) {
        PlayerRpgData d = ModAttachments.get(p);
        boolean changed = false;
        for (var e : d.activeQuests.entrySet()) {
            Quest q = ALL.get(e.getKey());
            if (q == null || q.objective() != Objective.KILL || e.getValue() >= q.count() || !matchesEntity(q, victim.getType())) continue;
            e.setValue(e.getValue() + 1);
            changed = true;
            if (e.getValue() >= q.count()) msg(p, "ready", Component.translatable(q.nameKey()));
        }
        if (changed) p.syncData(ModAttachments.RPG);
    }

    public static void onDimension(ServerPlayer p, ResourceKey<Level> dimension) {
        PlayerRpgData d = ModAttachments.get(p);
        boolean changed = false;
        for (var e : d.activeQuests.entrySet()) {
            Quest q = ALL.get(e.getKey());
            if (q == null || q.objective() != Objective.VISIT_DIMENSION || !dimension.location().toString().equals(q.target())) continue;
            e.setValue(1);
            changed = true;
            msg(p, "ready", Component.translatable(q.nameKey()));
        }
        if (changed) p.syncData(ModAttachments.RPG);
    }
}
