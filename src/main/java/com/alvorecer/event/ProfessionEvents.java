package com.alvorecer.event;

import com.alvorecer.ability.Abilities;
import com.alvorecer.registry.ModAttachments;
import com.alvorecer.rpg.Leveling;
import com.alvorecer.rpg.PlayerRpgData;
import com.alvorecer.rpg.Profession;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.brewing.PlayerBrewedPotionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * XP e bônus de profissões. Regras anti-farm: minério só conta sem Toque Suave (o drop não pode ser o próprio bloco),
 * plantas só maduras, criaturas de gerador e filhotes rendem pouco/nada, receitas de compactação (um único ingrediente)
 * não contam, e há limite diário configurável.
 */
public final class ProfessionEvents {
    private ProfessionEvents() {
    }

    private static int level(Player p, Profession prof) {
        return Abilities.professionLevel(ModAttachments.get(p), prof);
    }

    private static boolean roll(Player p, double chance) {
        return p.getRandom().nextDouble() < chance;
    }

    private static void give(Player p, ItemStack stack) {
        if (!p.getInventory().add(stack)) p.drop(stack, false);
    }

    // ------------------------------------------------------------ Minerador e Herbalista

    static int oreXp(BlockState state) {
        if (state.is(Tags.Blocks.ORES_NETHERITE_SCRAP)) return 0; // detrito ancestral dropa a si mesmo: sem XP
        if (state.is(Tags.Blocks.ORES_EMERALD)) return 12;
        if (state.is(Tags.Blocks.ORES_DIAMOND)) return 10;
        if (state.is(Tags.Blocks.ORES_GOLD)) return 5;
        if (state.is(Tags.Blocks.ORES_IRON) || state.is(Tags.Blocks.ORES_LAPIS)) return 4;
        if (state.is(Tags.Blocks.ORES_REDSTONE)) return 3;
        if (state.is(Tags.Blocks.ORES_COAL) || state.is(Tags.Blocks.ORES_COPPER) || state.is(Tags.Blocks.ORES_QUARTZ)) return 2;
        return state.is(Tags.Blocks.ORES) ? 3 : 0;
    }

    static boolean matureCrop(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) return crop.isMaxAge(state);
        if (state.getBlock() instanceof NetherWartBlock) return state.getValue(NetherWartBlock.AGE) >= 3;
        if (state.getBlock() instanceof CocoaBlock) return state.getValue(CocoaBlock.AGE) >= 2;
        return false;
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer p)) return;
        BlockState state = event.getState();
        List<ItemEntity> drops = event.getDrops();
        int oreXp = oreXp(state);
        if (oreXp > 0) {
            Item self = state.getBlock().asItem();
            boolean selfDrop = drops.stream().anyMatch(e -> e.getItem().is(self));
            if (selfDrop || drops.isEmpty()) return;
            Leveling.grantProfessionXp(p, Profession.MINER, oreXp);
            int lvl = level(p, Profession.MINER);
            if (lvl > 0 && roll(p, 0.01 * lvl)) duplicateOne(p, drops);
        } else if (matureCrop(state)) {
            Leveling.grantProfessionXp(p, Profession.HERBALIST, 2);
            int lvl = level(p, Profession.HERBALIST);
            if (lvl > 0 && roll(p, 0.02 * lvl)) duplicateOne(p, drops);
        }
    }

    private static void duplicateOne(Player p, List<ItemEntity> drops) {
        if (drops.isEmpty()) return;
        ItemEntity pick = drops.get(p.getRandom().nextInt(drops.size()));
        ItemEntity copy = new ItemEntity(pick.level(), pick.getX(), pick.getY(), pick.getZ(), pick.getItem().copy());
        copy.setDefaultPickUpDelay();
        drops.add(copy);
    }

    // ------------------------------------------------------------ Ferreiro, Cozinheiro e Artesão

    /** Receitas com só um tipo de ingrediente (lingotes ↔ bloco, etc.) não dão XP: impede ciclos infinitos. */
    static int distinctIngredients(Container matrix) {
        Set<Item> items = new HashSet<>();
        for (int i = 0; i < matrix.getContainerSize(); i++) {
            ItemStack s = matrix.getItem(i);
            if (!s.isEmpty()) items.add(s.getItem());
        }
        return items.size();
    }

    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        ItemStack result = event.getCrafting();
        int distinct = distinctIngredients(event.getInventory());
        if (result.isEmpty() || distinct < 2) return;
        Item item = result.getItem();
        if (item instanceof TieredItem || item instanceof ArmorItem || item instanceof ShieldItem) {
            Leveling.grantProfessionXp(p, Profession.BLACKSMITH, 10);
        }
        if (result.has(DataComponents.FOOD)) Leveling.grantProfessionXp(p, Profession.COOK, 3L * Math.min(result.getCount(), 4));
        Leveling.grantProfessionXp(p, Profession.ARTISAN, 1L + Math.min(distinct, 4));
        int lvl = level(p, Profession.ARTISAN);
        if (lvl > 0 && result.getMaxStackSize() > 1 && roll(p, 0.01 * lvl)) give(p, new ItemStack(item, 1));
    }

    @SubscribeEvent
    public static void onSmelt(PlayerEvent.ItemSmeltedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        ItemStack out = event.getSmelting();
        int n = out.getCount();
        if (out.is(Tags.Items.INGOTS)) {
            Leveling.grantProfessionXp(p, Profession.BLACKSMITH, 2L * n);
            bonus(p, out, 0.01 * level(p, Profession.BLACKSMITH));
        } else if (out.has(DataComponents.FOOD)) {
            Leveling.grantProfessionXp(p, Profession.COOK, n);
            bonus(p, out, 0.01 * level(p, Profession.COOK));
        }
    }

    /** Cada item da pilha tem {@code chance} de render um extra. */
    private static void bonus(Player p, ItemStack out, double chance) {
        if (chance <= 0) return;
        int extra = 0;
        for (int i = 0; i < out.getCount(); i++) if (roll(p, chance)) extra++;
        if (extra > 0) give(p, out.copyWithCount(extra));
    }

    // ------------------------------------------------------------ Alquimista e Cozinheiro (consumo)

    @SubscribeEvent
    public static void onBrew(PlayerBrewedPotionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        PotionContents contents = event.getStack().get(DataComponents.POTION_CONTENTS);
        if (contents != null && contents.getAllEffects().iterator().hasNext()) Leveling.grantProfessionXp(p, Profession.ALCHEMIST, 5);
    }

    @SubscribeEvent
    public static void onFinishUse(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        ItemStack used = event.getItem();
        int alch = level(p, Profession.ALCHEMIST);
        if (alch > 0 && used.getItem() instanceof PotionItem) {
            PotionContents contents = used.get(DataComponents.POTION_CONTENTS);
            if (contents == null) return;
            List<MobEffectInstance> effects = new ArrayList<>();
            contents.forEachEffect(effects::add);
            for (MobEffectInstance e : effects) {
                if (e.getEffect().value().isInstantenous()) continue;
                p.addEffect(new MobEffectInstance(e.getEffect(), (int) (e.getDuration() * (1 + 0.02 * alch)), e.getAmplifier(),
                        e.isAmbient(), e.isVisible(), e.showIcon()));
            }
        }
        int cook = level(p, Profession.COOK);
        FoodProperties food = used.get(DataComponents.FOOD);
        if (cook > 0 && food != null) {
            var data = p.getFoodData();
            data.setSaturation(Math.min(data.getFoodLevel(), data.getSaturationLevel() + food.saturation() * 0.03f * cook));
        }
    }

    // ------------------------------------------------------------ Caçador

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || victim instanceof Player || victim.isBaby()) return;
        long xp;
        if (victim instanceof Animal) xp = Math.round(victim.getMaxHealth() / 2);
        else if (victim instanceof Enemy) xp = Math.round(victim.getMaxHealth() / 4);
        else return;
        if (victim.getPersistentData().getBoolean(PlayerEvents.SPAWNER_TAG)) xp /= 4;
        Leveling.grantProfessionXp(p, Profession.HUNTER, Math.max(1, xp));
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || event.getEntity() instanceof Player) return;
        int lvl = level(p, Profession.HUNTER);
        if (lvl > 0 && roll(p, 0.02 * lvl)) {
            List<ItemEntity> drops = new ArrayList<>(event.getDrops());
            if (!drops.isEmpty()) {
                ItemEntity pick = drops.get(p.getRandom().nextInt(drops.size()));
                event.getDrops().add(new ItemEntity(pick.level(), pick.getX(), pick.getY(), pick.getZ(), pick.getItem().copy()));
            }
        }
    }

    // ------------------------------------------------------------ Encantador

    /** Encantamentos na mesa são detectados pela estatística vanilla (o jogo não dispara evento próprio). */
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 7) return;
        PlayerRpgData d = ModAttachments.get(p);
        int count = p.getStats().getValue(Stats.CUSTOM.get(Stats.ENCHANT_ITEM));
        if (d.enchantsSeen < 0) {
            d.enchantsSeen = count;
        } else if (count > d.enchantsSeen) {
            int delta = count - d.enchantsSeen;
            d.enchantsSeen = count;
            Leveling.grantProfessionXp(p, Profession.ENCHANTER, 15L * delta);
        }
    }

    @SubscribeEvent
    public static void onAnvil(AnvilRepairEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        Leveling.grantProfessionXp(p, Profession.ENCHANTER, 5);
        int smith = level(p, Profession.BLACKSMITH);
        if (smith > 0) event.setBreakChance(event.getBreakChance() * (1 - 0.04f * smith));
    }

    /** Encantador: +1% de experiência vanilla por nível de profissão. */
    @SubscribeEvent
    public static void onXp(PlayerXpEvent.XpChange event) {
        int lvl = level(event.getEntity(), Profession.ENCHANTER);
        if (lvl > 0 && event.getAmount() > 0) event.setAmount((int) Math.round(event.getAmount() * (1 + 0.01 * lvl)));
    }
}
