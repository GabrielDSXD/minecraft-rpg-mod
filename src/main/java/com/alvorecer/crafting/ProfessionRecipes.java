package com.alvorecer.crafting;

import com.alvorecer.registry.ModAttachments;
import com.alvorecer.registry.ModGear;
import com.alvorecer.rpg.Leveling;
import com.alvorecer.rpg.PlayerRpgData;
import com.alvorecer.rpg.Profession;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Receitas exclusivas de profissão feitas na Bancada de Ofício. Os ingredientes vêm do inventário; o servidor valida
 * profissão (principal ou secundária), nível, ingredientes e a proximidade da bancada.
 */
public final class ProfessionRecipes {
    /** Ingrediente: id de item ou tag com "#". */
    public record Input(String id, int count) {
        public boolean matches(ItemStack s) {
            if (s.isEmpty()) return false;
            if (id.startsWith("#")) return s.is(TagKey.create(Registries.ITEM, ResourceLocation.parse(id.substring(1))));
            return BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals(id);
        }
    }

    public record Recipe(String id, Profession profession, int level, Supplier<? extends Item> result, int count, List<Input> inputs, long xp) {
        public ItemStack resultStack() {
            return new ItemStack(result.get(), count);
        }
    }

    private static final Map<String, Recipe> ALL = new LinkedHashMap<>();

    private ProfessionRecipes() {
    }

    private static Input in(String id, int count) {
        return new Input(id, count);
    }

    private static void r(String id, Profession prof, int level, Supplier<? extends Item> result, int count, long xp, Input... inputs) {
        ALL.put(id, new Recipe(id, prof, level, result, count, List.of(inputs), xp));
    }

    static {
        String gem = "alvorecer:aurorite_gem", ingot = "alvorecer:lunar_silver_ingot";
        // Ferreiro
        r("lunar_refining", Profession.BLACKSMITH, 3, ModGear.LUNAR_SILVER_INGOT, 3, 6, in("alvorecer:raw_lunar_silver", 2), in("#minecraft:coals", 1));
        r("aurorite_sword", Profession.BLACKSMITH, 8, ModGear.AURORITE_SWORD, 1, 40, in(gem, 2), in("minecraft:stick", 1));
        r("aurorite_dagger", Profession.BLACKSMITH, 8, ModGear.AURORITE_DAGGER, 1, 30, in(gem, 1), in("minecraft:stick", 1));
        r("aurorite_warhammer", Profession.BLACKSMITH, 10, ModGear.AURORITE_WARHAMMER, 1, 50, in(gem, 4), in("minecraft:stick", 2));
        r("aurorite_spear", Profession.BLACKSMITH, 10, ModGear.AURORITE_SPEAR, 1, 45, in(gem, 2), in("minecraft:stick", 3));
        String[] pieces = {"helmet", "chestplate", "leggings", "boots"};
        int[] gems = {5, 8, 7, 4};
        for (int i = 0; i < 4; i++) {
            r("aurorite_" + pieces[i], Profession.BLACKSMITH, 12, ModGear.AURORITE_ARMOR[i], 1, 60, in(gem, gems[i]), in(ingot, 2));
        }
        r("dawn_blade", Profession.BLACKSMITH, 16, ModGear.DAWN_BLADE, 1, 200, in(gem, 6), in("alvorecer:wyrmling_scale", 4), in("minecraft:gold_block", 1));
        r("solar_breaker", Profession.BLACKSMITH, 18, ModGear.SOLAR_BREAKER, 1, 250, in(gem, 8), in("minecraft:gold_block", 2), in("alvorecer:sentinel_heart", 2));
        // Artesão
        r("aurorite_bow", Profession.ARTISAN, 10, ModGear.AURORITE_BOW, 1, 40, in(gem, 3), in("minecraft:string", 3));
        r("aurorite_crossbow", Profession.ARTISAN, 10, ModGear.AURORITE_CROSSBOW, 1, 45, in(gem, 3), in("minecraft:string", 2), in("minecraft:tripwire_hook", 1));
        r("aurorite_shield", Profession.ARTISAN, 10, ModGear.AURORITE_SHIELD, 1, 45, in(gem, 3), in("#minecraft:planks", 4));
        r("prismatic_wings", Profession.ARTISAN, 18, ModGear.PRISMATIC_WINGS, 1, 250, in("minecraft:elytra", 1), in(gem, 6), in("alvorecer:fairy_dust", 12));
        // Encantador
        for (int i = 0; i < 4; i++) {
            r("arcane_" + pieces[i], Profession.ENCHANTER, 6, ModGear.ARCANE[i], 1, 35, in("#minecraft:wool", 3 + i % 2), in("minecraft:amethyst_shard", 2 + i % 2), in("minecraft:lapis_lazuli", 3));
        }
        r("aurorite_wand", Profession.ENCHANTER, 10, ModGear.AURORITE_WAND, 1, 50, in(gem, 2), in("minecraft:amethyst_shard", 4));
        r("arcane_staff", Profession.ENCHANTER, 15, ModGear.ARCANE_STAFF, 1, 200, in(gem, 4), in("minecraft:amethyst_block", 2), in("alvorecer:elemental_core", 3));
        // Caçador
        r("shadow_daggers", Profession.HUNTER, 14, ModGear.SHADOW_DAGGERS, 1, 150, in(gem, 3), in("minecraft:phantom_membrane", 4), in("alvorecer:arcane_fang", 4));
        // Alquimista, Herbalista, Cozinheiro
        r("mana_elixir", Profession.ALCHEMIST, 2, ModGear.MANA_ELIXIR, 2, 8, in("minecraft:glass_bottle", 2), in("minecraft:lapis_lazuli", 2), in("minecraft:glowstone_dust", 1));
        r("vigor_elixir", Profession.ALCHEMIST, 2, ModGear.VIGOR_ELIXIR, 2, 8, in("minecraft:glass_bottle", 2), in("minecraft:sugar", 2), in("minecraft:redstone", 1));
        r("healing_salve", Profession.HERBALIST, 2, ModGear.HEALING_SALVE, 2, 8, in("minecraft:wheat_seeds", 4), in("minecraft:honeycomb", 1));
        r("adventurer_stew", Profession.COOK, 2, ModGear.ADVENTURER_STEW, 1, 8, in("minecraft:bowl", 1), in("minecraft:cooked_beef", 1),
                in("minecraft:carrot", 1), in("minecraft:baked_potato", 1));
        // Minerador
        r("aurorite_cutting", Profession.MINER, 5, ModGear.AURORITE_GEM, 1, 10, in("alvorecer:aurorite_shard", 2));
    }

    public static Map<String, Recipe> all() {
        return ALL;
    }

    @Nullable
    public static Recipe get(String id) {
        return ALL.get(id);
    }

    public static int count(Player p, Input input) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (input.matches(s)) n += s.getCount();
        return n;
    }

    public static boolean hasInputs(Player p, Recipe r) {
        for (Input in : r.inputs()) if (count(p, in) < in.count()) return false;
        return true;
    }

    public enum Failure { NO_PROFESSION, LEVEL, INPUTS, TOO_FAR }

    @Nullable
    public static Failure check(Player p, PlayerRpgData d, Recipe r) {
        int lvl = d.levelOf(r.profession());
        if (lvl == 0) return Failure.NO_PROFESSION;
        if (lvl < r.level()) return Failure.LEVEL;
        if (!hasInputs(p, r)) return Failure.INPUTS;
        return null;
    }

    public static boolean craft(ServerPlayer p, String id) {
        PlayerRpgData d = ModAttachments.get(p);
        Recipe r = ALL.get(id);
        if (r == null) return false;
        BlockPos pos = d.lastWorkbenchPos;
        Failure f = (pos == null || !p.blockPosition().closerThan(pos, 8) || !p.level().getBlockState(pos).is(ModGear.WORKBENCH.get()))
                ? Failure.TOO_FAR : check(p, d, r);
        if (f != null) {
            p.displayClientMessage(Component.translatable("msg.alvorecer.craft." + f.name().toLowerCase()), true);
            return false;
        }
        for (Input in : r.inputs()) {
            int left = in.count();
            for (ItemStack s : p.getInventory().items) {
                if (left <= 0) break;
                if (!in.matches(s)) continue;
                int take = Math.min(left, s.getCount());
                s.shrink(take);
                left -= take;
            }
        }
        ItemStack out = r.resultStack();
        if (!p.getInventory().add(out)) p.drop(out, false);
        p.level().playSound(null, p.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 0.8f, 1.1f);
        Leveling.grantProfessionXp(p, r.profession(), r.xp());
        return true;
    }

    /** Receitas visíveis para o jogador: as das profissões dele primeiro. */
    public static List<Recipe> visible(PlayerRpgData d) {
        List<Recipe> mine = new ArrayList<>(), others = new ArrayList<>();
        for (Recipe r : ALL.values()) (d.levelOf(r.profession()) > 0 ? mine : others).add(r);
        mine.addAll(others);
        return mine;
    }
}
