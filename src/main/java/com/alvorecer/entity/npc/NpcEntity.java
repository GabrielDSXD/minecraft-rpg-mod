package com.alvorecer.entity.npc;

import com.alvorecer.network.ModNetwork;
import com.alvorecer.registry.ModAttachments;
import com.alvorecer.registry.ModGear;
import com.alvorecer.registry.ModItems;
import com.alvorecer.registry.ModMobs;
import com.alvorecer.registry.ModSounds;
import com.alvorecer.rpg.Derived;
import com.alvorecer.rpg.PlayerRpgData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * NPCs da guilda: conversam (diálogo), comerciam (desconto de Carisma) e, o Mestre da Guilda e o Sábio, oferecem as
 * missões do Mural. Não podem ser feridos por jogadores fora do modo criativo e não somem.
 */
public class NpcEntity extends PathfinderMob implements Merchant {
    public enum Role {
        GUILD_MASTER, MERCHANT, SAGE;

        public boolean givesQuests() {
            return this != MERCHANT;
        }

        public String id() {
            return name().toLowerCase();
        }
    }

    public static final double INTERACT_RANGE = 6;
    @Nullable private Player tradingPlayer;
    @Nullable private MerchantOffers offers;
    private long lastRestockDay = -1;

    public NpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 30).add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    public Role role() {
        if (getType() == ModMobs.GUILD_MASTER.get()) return Role.GUILD_MASTER;
        if (getType() == ModMobs.MERCHANT.get()) return Role.MERCHANT;
        return Role.SAGE;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.4));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp && isAlive() && tradingPlayer == null) {
            if (sp.connection.hasChannel(ModNetwork.OpenNpc.TYPE)) {
                PacketDistributor.sendToPlayer(sp, new ModNetwork.OpenNpc(getId(), role().ordinal()));
            }
            playSound(ModSounds.get("npc.greet"), 1f, role() == Role.SAGE ? 0.8f : 1.1f);
            getLookControl().setLookAt(sp);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    /** Ações do diálogo validadas no servidor: 0 comércio, 1 missões. */
    public void handleAction(ServerPlayer p, int action) {
        if (!isAlive() || distanceTo(p) > INTERACT_RANGE) return;
        if (action == 0 && tradingPlayer == null) openTrade(p);
        if (action == 1 && role().givesQuests()) {
            ModAttachments.get(p).questGiver = getUUID();
            ModNetwork.openScreen(p, ModNetwork.Screen.QUEST_BOARD);
        }
    }

    private void openTrade(ServerPlayer p) {
        restockIfNewDay();
        PlayerRpgData d = ModAttachments.get(p);
        double discount = d.hasCharacter() ? Derived.tradeDiscount(d) : 0;
        for (MerchantOffer o : getOffers()) {
            o.resetSpecialPriceDiff();
            o.addToSpecialPriceDiff(-(int) Math.round(o.getBaseCostA().getCount() * discount));
        }
        setTradingPlayer(p);
        openTradingScreen(p, getDisplayName(), 1);
    }

    private void restockIfNewDay() {
        long day = level().getGameTime() / 24000L;
        if (day != lastRestockDay) {
            lastRestockDay = day;
            for (MerchantOffer o : getOffers()) o.resetUses();
        }
    }

    // ------------------------------------------------------------ Merchant

    private static MerchantOffer sell(int emeralds, ItemLike item, int count) {
        return new MerchantOffer(new ItemCost(Items.EMERALD, emeralds), new ItemStack(item, count), 12, 0, 0.05f);
    }

    private static MerchantOffer sell(int emeralds, ItemLike extra, int extraCount, ItemLike item) {
        return new MerchantOffer(new ItemCost(Items.EMERALD, emeralds), Optional.of(new ItemCost(extra, extraCount)), new ItemStack(item), 3, 0, 0.05f);
    }

    private static MerchantOffer buy(ItemLike item, int count, int emeralds) {
        return new MerchantOffer(new ItemCost(item, count), new ItemStack(Items.EMERALD, emeralds), 16, 0, 0.05f);
    }

    @Override
    public MerchantOffers getOffers() {
        if (offers == null) {
            offers = new MerchantOffers();
            Item ingot = ModGear.LUNAR_SILVER_INGOT.get();
            switch (role()) {
                case MERCHANT -> {
                    offers.add(sell(2, ingot, 1));
                    offers.add(buy(ModGear.RAW_LUNAR_SILVER.get(), 6, 1));
                    offers.add(sell(12, ModGear.AURORITE_SHARD.get(), 1));
                    offers.add(buy(ModGear.AURORITE_SHARD.get(), 2, 5));
                    offers.add(sell(3, ModGear.MANA_ELIXIR.get(), 1));
                    offers.add(sell(3, ModGear.VIGOR_ELIXIR.get(), 1));
                    offers.add(sell(2, ModGear.HEALING_SALVE.get(), 1));
                    offers.add(sell(1, Items.ARROW, 16));
                    offers.add(buy(ModMobs.WYRMLING_SCALE.get(), 1, 6));
                }
                case GUILD_MASTER -> {
                    offers.add(sell(4, com.alvorecer.registry.ModBlocks.QUEST_BOARD_ITEM.get(), 1));
                    offers.add(sell(3, ModGear.IRON_DAGGER.get(), 1));
                    offers.add(sell(4, ModGear.IRON_SPEAR.get(), 1));
                    offers.add(sell(5, ModGear.IRON_WARHAMMER.get(), 1));
                    offers.add(sell(6, ModGear.WANDERER[3].get(), 1));
                    offers.add(buy(Items.BONE, 16, 1));
                    offers.add(buy(Items.ROTTEN_FLESH, 24, 1));
                    offers.add(buy(ModMobs.GOBLIN_EAR.get(), 4, 1));
                    offers.add(sell(32, Items.DIAMOND, 1, ModItems.RESPEC_TOME.get()));
                }
                case SAGE -> {
                    offers.add(sell(2, ModItems.CODEX.get(), 1));
                    offers.add(sell(4, ModGear.OAK_STAFF.get(), 1));
                    offers.add(sell(8, ModGear.AMETHYST_WAND.get(), 1));
                    offers.add(buy(ModMobs.FAIRY_DUST.get(), 3, 1));
                    offers.add(buy(ModMobs.ELEMENTAL_CORE.get(), 1, 4));
                    offers.add(buy(ModMobs.ARCANE_FANG.get(), 2, 3));
                    offers.add(buy(Items.LAPIS_LAZULI, 8, 1));
                }
            }
        }
        return offers;
    }

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        if (player == null && offers != null) offers.forEach(MerchantOffer::resetSpecialPriceDiff);
        tradingPlayer = player;
    }

    @Nullable
    @Override
    public Player getTradingPlayer() {
        return tradingPlayer;
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {
        this.offers = offers;
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        playSound(SoundEvents.VILLAGER_YES, 0.6f, 1.2f);
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    @Override
    public boolean isClientSide() {
        return level().isClientSide;
    }

    @Override
    public void tick() {
        super.tick();
        if (tradingPlayer != null && (!tradingPlayer.isAlive() || tradingPlayer.distanceToSqr(this) > 64
                || !(tradingPlayer.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu))) {
            setTradingPlayer(null);
        }
    }

    // ------------------------------------------------------------ proteção

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean admin = source.getEntity() instanceof Player p && p.isCreative();
        if (!admin && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return super.hurt(source, amount);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }
}
