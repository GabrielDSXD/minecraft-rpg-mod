package com.alvorecer.event;

import com.alvorecer.Alvorecer;
import com.alvorecer.ability.Abilities;
import com.alvorecer.ability.Scheduler;
import com.alvorecer.config.RpgConfig;
import com.alvorecer.network.ModNetwork;
import com.alvorecer.registry.ModAttachments;
import com.alvorecer.registry.ModEffects;
import com.alvorecer.rpg.Derived;
import com.alvorecer.rpg.Leveling;
import com.alvorecer.rpg.PlayerRpgData;
import com.alvorecer.rpg.Progression;
import com.alvorecer.rpg.Race;
import com.alvorecer.rpg.StatApplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import com.alvorecer.achievement.Achievements;
import com.alvorecer.party.Parties;
import com.alvorecer.quest.Quests;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Ciclo de vida do personagem: login, respawn, regeneração, XP, exploração e passivas raciais contínuas. */
public final class PlayerEvents {
    public static final String SPAWNER_TAG = Alvorecer.MODID + ":from_spawner";

    private PlayerEvents() {
    }

    private static void refresh(ServerPlayer p) {
        StatApplier.apply(p);
        p.syncData(ModAttachments.RPG);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        refresh(p);
        if (!ModAttachments.get(p).hasCharacter()) ModNetwork.openScreen(p, ModNetwork.Screen.CREATION);
        else Achievements.checkProgress(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) Parties.leave(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        PlayerRpgData d = ModAttachments.get(p);
        StatApplier.apply(p);
        p.setHealth(p.getMaxHealth());
        if (d.hasCharacter()) {
            d.mana = Derived.maxMana(d);
            d.energy = Derived.maxEnergy(d);
        }
        p.syncData(ModAttachments.RPG);
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            refresh(p);
            Quests.onDimension(p, event.getTo());
        }
    }

    @SubscribeEvent
    public static void onEquipment(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) StatApplier.apply(p);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        PlayerRpgData d = ModAttachments.get(p);
        if (!d.hasCharacter()) return;
        long now = p.level().getGameTime();

        if (d.leapPending && now - d.leapStart > 4) {
            if (p.onGround() || p.isInWater()) {
                d.leapPending = false;
                Abilities.leapLanding(p, d);
            } else if (now - d.leapStart > 100) {
                d.leapPending = false;
            }
        }

        if (p.tickCount % 20 != 0) return;
        boolean changed = false;
        float maxMana = Derived.maxMana(d), maxEnergy = Derived.maxEnergy(d);
        if (d.mana < maxMana) {
            d.mana = Math.min(maxMana, d.mana + Derived.manaRegen(d));
            changed = true;
        }
        if (d.energy < maxEnergy) {
            d.energy = Math.min(maxEnergy, d.energy + Derived.energyRegen(d));
            changed = true;
        }
        if (d.race == Race.ELF) {
            MobEffectInstance nv = p.getEffect(MobEffects.NIGHT_VISION);
            if (nv == null || (nv.getDuration() < 260 && nv.isAmbient())) {
                p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, false));
            }
        }
        if (p.hasEffect(ModEffects.HOLY_AURA)) Abilities.auraPulse(p);
        if (p.hasEffect(ModEffects.DAWN_AVATAR)) Abilities.avatarPulse(p, d);

        if (p.tickCount % 40 == 0) {
            var biome = p.level().getBiome(p.blockPosition()).unwrapKey();
            if (biome.isPresent() && d.visitedBiomes.add(biome.get().location().toString())) {
                int xp = RpgConfig.EXPLORATION_XP.get();
                if (xp > 0) {
                    p.displayClientMessage(Component.translatable("msg.alvorecer.new_biome", xp), true);
                    Leveling.grantXp(p, xp);
                }
                Achievements.checkProgress(p);
                changed = true;
            }
        }
        if (changed) p.syncData(ModAttachments.RPG);
    }

    /** Queda anulada após impulsos de habilidades. */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && ModAttachments.get(p).noFallUntil > p.level().getGameTime()) {
            event.setCanceled(true);
        }
    }

    /** Halfling agachado é detectado a metade da distância. */
    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (event.getEntity() instanceof Player p && p.isCrouching() && ModAttachments.get(p).race == Race.HALFLING) {
            event.modifyVisibility(0.5);
        }
    }

    @SubscribeEvent
    public static void onSpawn(FinalizeSpawnEvent event) {
        if (event.getSpawnType() == MobSpawnType.SPAWNER) event.getEntity().getPersistentData().putBoolean(SPAWNER_TAG, true);
    }

    /** XP de classe por abater monstros (geradores rendem menos). */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide || !(event.getSource().getEntity() instanceof ServerPlayer p)) return;
        if (!(victim instanceof Enemy) || victim instanceof Player) return;
        PlayerRpgData d = ModAttachments.get(p);
        if (d.hasTalent("barbarian_carnage")) d.energy = Math.min(Derived.maxEnergy(d), d.energy + 15);
        long xp = Progression.killXp(victim.getMaxHealth(), victim.getArmorValue());
        if (victim.getPersistentData().getBoolean(SPAWNER_TAG)) xp = Math.round(xp * RpgConfig.SPAWNER_XP_MULTIPLIER.get());
        com.alvorecer.party.Parties.grantKillXp(p, victim, xp);
    }

    /** Desconto de Carisma com aldeões, aplicado depois que a tela de comércio abre (os preços são zerados ao fechar). */
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !(event.getTarget() instanceof AbstractVillager villager)) return;
        PlayerRpgData d = ModAttachments.get(p);
        double discount = d.hasCharacter() ? Derived.tradeDiscount(d) : 0;
        if (discount == 0) return;
        Scheduler.after(1, () -> {
            if (villager.getTradingPlayer() != p) return;
            for (MerchantOffer offer : villager.getOffers()) {
                int base = offer.getBaseCostA().getCount();
                offer.addToSpecialPriceDiff(-(int) Math.round(base * discount));
            }
            int level = villager instanceof Villager v ? v.getVillagerData().getLevel() : 0;
            p.sendMerchantOffers(p.containerMenu.containerId, villager.getOffers(), level, villager.getVillagerXp(),
                    villager.showProgressBar(), villager.canRestock());
        });
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Scheduler.tick();
        if (event.getServer().getTickCount() % 10 == 0) Parties.tick(event.getServer());
    }

    /** Regra de saque do grupo (roda depois dos bônus de drop das profissões). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDropsAssign(LivingDropsEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer p && !(event.getEntity() instanceof Player)) {
            Parties.assignLoot(p, new java.util.ArrayList<>(event.getDrops()));
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        Scheduler.clear();
        Parties.clear();
    }
}
