package com.alvorecer.gametest;

import com.alvorecer.Alvorecer;
import com.alvorecer.ability.AbilityService;
import com.alvorecer.rpg.Derived;
import com.alvorecer.network.ModNetwork;
import com.alvorecer.registry.ModAttachments;
import com.alvorecer.rpg.Element;
import com.alvorecer.rpg.PlayerRpgData;
import com.alvorecer.rpg.Profession;
import com.alvorecer.rpg.Race;
import com.alvorecer.rpg.RpgClass;
import com.alvorecer.rpg.StatApplier;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/**
 * Testes no servidor real (gradlew runGameTestServer). Validam que as habilidades passam pelo servidor:
 * alvo e linha de visão, custo, recarga, nível de desbloqueio, e que a ficha persiste no NBT do jogador.
 */
@GameTestHolder(Alvorecer.MODID)
@PrefixGameTestTemplate(false)
public class RpgGameTests {

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper h, Race race, RpgClass cls, Vec3 relPos) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        Vec3 abs = h.absoluteVec(relPos);
        p.moveTo(abs.x, abs.y, abs.z, 0, 0); // yaw 0 = olhando para +Z
        p.setYHeadRot(0);
        PlayerRpgData d = ModAttachments.get(p);
        d.resetCharacter();
        ModNetwork.createCharacter(d, new ModNetwork.CreateCharacter(race.ordinal(), cls.ordinal(), Profession.MINER.ordinal(), Element.FIRE.ordinal()));
        StatApplier.apply(p);
        return p;
    }

    private static void leave(GameTestHelper h, ServerPlayer p) {
        h.getLevel().getServer().getPlayerList().remove(p);
    }

    private static Zombie zombie(GameTestHelper h, double x, double z) {
        Zombie zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, (float) x, 2, (float) z);
        zombie.setYRot(180);
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET)); // não queima ao sol
        zombie.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2000));
        return zombie;
    }

    @GameTest(template = "arena")
    public static void powerStrikeHitsChargesAndCoolsDown(GameTestHelper h) {
        ServerPlayer p = player(h, Race.HUMAN, RpgClass.WARRIOR, new Vec3(3.5, 2, 1.5));
        Zombie zombie = zombie(h, 3.5, 3.5);
        PlayerRpgData d = ModAttachments.get(p);
        float energy = d.energy;

        AbilityService.use(p, 1);
        h.assertTrue(zombie.getHealth() < zombie.getMaxHealth(), "o zumbi devia ter sofrido dano");
        h.assertTrue(Math.abs(d.energy - (energy - 15)) < 0.01, "custo de 15 de energia esperado, sobrou " + d.energy);
        h.assertTrue(d.isOnCooldown("warrior_power_strike", p.level().getGameTime()), "recarga devia estar ativa");

        float health = zombie.getHealth();
        float afterFirst = d.energy;
        AbilityService.use(p, 1);
        h.assertTrue(zombie.getHealth() == health, "em recarga a habilidade não pode causar dano");
        h.assertTrue(d.energy == afterFirst, "em recarga nada deve ser cobrado");
        leave(h, p);
        h.succeed();
    }

    @GameTest(template = "arena")
    public static void abilityRespectsLineOfSight(GameTestHelper h) {
        ServerPlayer p = player(h, Race.HUMAN, RpgClass.WARRIOR, new Vec3(3.5, 2, 1.5));
        h.setBlock(new BlockPos(3, 2, 2), Blocks.STONE);
        h.setBlock(new BlockPos(3, 3, 2), Blocks.STONE);
        Zombie zombie = zombie(h, 3.5, 3.5);
        PlayerRpgData d = ModAttachments.get(p);
        float energy = d.energy;

        AbilityService.use(p, 1);
        h.assertTrue(zombie.getHealth() == zombie.getMaxHealth(), "a parede devia bloquear o golpe");
        h.assertTrue(d.energy == energy, "sem alvo válido nada deve ser cobrado");
        h.assertFalse(d.isOnCooldown("warrior_power_strike", p.level().getGameTime()), "sem alvo não deve entrar em recarga");
        leave(h, p);
        h.succeed();
    }

    @GameTest(template = "arena")
    public static void lockedUltimateIsRejected(GameTestHelper h) {
        ServerPlayer p = player(h, Race.ORC, RpgClass.BARBARIAN, new Vec3(3.5, 2, 3.5));
        PlayerRpgData d = ModAttachments.get(p);
        float energy = d.energy;
        AbilityService.use(p, AbilityService.SLOT_ULTIMATE);
        h.assertTrue(d.energy == energy, "suprema bloqueada no nível 1 não pode ser usada");
        h.assertFalse(d.cooldowns.containsKey("barbarian_undying_rage"), "suprema bloqueada não deve entrar em recarga");

        d.level = 15;
        AbilityService.use(p, AbilityService.SLOT_ULTIMATE);
        h.assertTrue(d.cooldowns.containsKey("barbarian_undying_rage"), "no nível 15 a suprema deve funcionar");
        leave(h, p);
        h.succeed();
    }

    @GameTest(template = "arena")
    public static void raceAbilityAndPassivesApply(GameTestHelper h) {
        ServerPlayer p = player(h, Race.DWARF, RpgClass.CLERIC, new Vec3(3.5, 2, 3.5));
        PlayerRpgData d = ModAttachments.get(p);
        h.assertTrue(Math.abs(p.getAttributeValue(Attributes.SCALE) - 0.9) < 1e-6, "anão deve ter escala 0,9");
        h.assertTrue(p.getAttribute(Attributes.MOVEMENT_SPEED).getValue() < p.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue(),
                "anão deve ser mais lento");
        float maxHp = p.getMaxHealth();
        h.assertTrue(Math.abs(maxHp - (20 + Derived.bonusHealth(d))) < 1e-4, "vida máxima deve incluir o bônus da ficha");

        AbilityService.use(p, AbilityService.SLOT_RACE);
        h.assertTrue(p.hasEffect(com.alvorecer.registry.ModEffects.STONESKIN), "Pele de Pedra deve estar ativa");
        leave(h, p);
        h.succeed();
    }

    @GameTest(template = "arena")
    public static void sheetPersistsInPlayerNbt(GameTestHelper h) {
        ServerPlayer p = player(h, Race.DRAGONBORN, RpgClass.WIZARD, new Vec3(3.5, 2, 3.5));
        PlayerRpgData d = ModAttachments.get(p);
        d.element = Element.COLD;
        d.level = 7;
        d.allocated[3] = 4;
        d.cooldowns.put("wizard_fireball", 12345L);
        d.visitedBiomes.add("minecraft:plains");
        CompoundTag saved = p.saveWithoutId(new CompoundTag());

        ServerPlayer copy = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), new GameProfile(UUID.randomUUID(), "copia"), ClientInformation.createDefault());
        copy.load(saved);
        PlayerRpgData c = ModAttachments.get(copy);
        h.assertTrue(c.race == Race.DRAGONBORN && c.rpgClass == RpgClass.WIZARD && c.element == Element.COLD, "escolhas não persistiram");
        h.assertTrue(c.level == 7 && c.allocated[3] == 4, "nível/atributos não persistiram");
        h.assertTrue(c.cooldowns.getOrDefault("wizard_fireball", 0L) == 12345L, "recargas não persistiram");
        h.assertTrue(c.visitedBiomes.contains("minecraft:plains"), "diário de exploração não persistiu");
        leave(h, p);
        h.succeed();
    }

    @GameTest(template = "arena")
    public static void talentSpendingIsValidatedOnServer(GameTestHelper h) {
        ServerPlayer p = player(h, Race.HUMAN, RpgClass.WARRIOR, new Vec3(3.5, 2, 3.5));
        PlayerRpgData d = ModAttachments.get(p);
        ModNetwork.onSpendTalent(p, "warrior_tough_hide");
        h.assertTrue(d.talents.isEmpty(), "sem pontos no nível 1");
        d.level = 6;
        float hp = p.getMaxHealth();
        ModNetwork.onSpendTalent(p, "warrior_tough_hide");
        h.assertTrue(d.talents.getOrDefault("warrior_tough_hide", 0) == 1, "talento devia ter sido aprendido");
        h.assertTrue(p.getMaxHealth() > hp, "Pele Curtida deve aumentar a vida máxima");
        ModNetwork.onSpendTalent(p, "warrior_tactics");
        h.assertFalse(d.talents.containsKey("warrior_tactics"), "tier 2 sem os 3 pontos do tier 1 deve ser recusado");
        ModNetwork.onChooseSpec(p, "warrior_defender");
        h.assertTrue("warrior_defender".equals(d.specialization), "especialização no nível 6");
        h.assertTrue(d.achievements.contains("specialization"), "conquista de especialização");
        leave(h, p);
        h.succeed();
    }

    @GameTest(template = "arena")
    public static void partySharesXpAndBlocksFriendlyFire(GameTestHelper h) {
        ServerPlayer a = player(h, Race.HUMAN, RpgClass.WARRIOR, new Vec3(2.5, 2, 2.5));
        ServerPlayer b = player(h, Race.ELF, RpgClass.CLERIC, new Vec3(4.5, 2, 2.5));
        h.assertTrue(com.alvorecer.party.Parties.invite(a, b), "convite");
        h.assertTrue(com.alvorecer.party.Parties.accept(b), "aceite");
        h.assertTrue(com.alvorecer.party.Parties.sameParty(a, b), "mesmo grupo");
        h.assertTrue(com.alvorecer.ability.AbilityUtil.isAlly(a, b), "membros são aliados para as habilidades");

        Zombie zombie = zombie(h, 3.5, 4.5);
        zombie.hurt(a.damageSources().playerAttack(a), 1000);
        PlayerRpgData da = ModAttachments.get(a), db = ModAttachments.get(b);
        long expected = com.alvorecer.party.Parties.shareXp(com.alvorecer.rpg.Progression.killXp(20, 2), 2, 0.1);
        h.assertTrue(db.xp > 0, "o outro membro devia receber XP do abate");
        h.assertTrue(Math.abs(da.xp - db.xp) <= 2 && Math.abs(db.xp - Math.round(expected * (db.race == Race.HUMAN ? 1.1 : 1))) <= 2,
                "XP dividido de forma igual (a=" + da.xp + ", b=" + db.xp + ", esperado~" + expected + ")");

        float hp = b.getHealth();
        b.hurt(a.damageSources().playerAttack(a), 4);
        h.assertTrue(b.getHealth() == hp, "fogo amigo desativado por padrão");
        com.alvorecer.party.Parties.leave(b);
        h.assertFalse(com.alvorecer.party.Parties.sameParty(a, b), "grupo desfeito");
        leave(h, a);
        leave(h, b);
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 200)
    public static void questBoardAcceptKillAndComplete(GameTestHelper h) {
        ServerPlayer p = player(h, Race.ORC, RpgClass.BARBARIAN, new Vec3(3.5, 2, 3.5));
        PlayerRpgData d = ModAttachments.get(p);
        BlockPos board = new BlockPos(1, 2, 1);
        h.setBlock(board, com.alvorecer.registry.ModBlocks.QUEST_BOARD.get());
        h.assertFalse(com.alvorecer.quest.Quests.accept(p, "zombie_hunt"), "sem abrir o mural não pode aceitar");
        d.lastBoardPos = h.absolutePos(board);
        h.assertTrue(com.alvorecer.quest.Quests.accept(p, "zombie_hunt"), "aceitar perto do mural");
        h.assertFalse(com.alvorecer.quest.Quests.complete(p, "zombie_hunt"), "não pode entregar sem progresso");
        for (int i = 0; i < 10; i++) {
            Zombie z = zombie(h, 3.5, 5.5);
            z.hurt(p.damageSources().playerAttack(p), 1000);
        }
        h.assertTrue(d.activeQuests.get("zombie_hunt") == 10, "10 abates contados, contou " + d.activeQuests.get("zombie_hunt"));
        int iron = p.getInventory().countItem(Items.IRON_INGOT);
        h.assertTrue(com.alvorecer.quest.Quests.complete(p, "zombie_hunt"), "entrega");
        h.assertTrue(p.getInventory().countItem(Items.IRON_INGOT) == iron + 3, "recompensa: 3 lingotes de ferro");
        h.assertTrue(d.completedQuests.containsKey("zombie_hunt") && !d.activeQuests.containsKey("zombie_hunt"), "missão concluída");
        h.assertTrue(d.achievements.contains("first_quest"), "conquista da primeira missão");
        h.assertFalse(com.alvorecer.quest.Quests.accept(p, "zombie_hunt"), "diária em espera");
        leave(h, p);
        h.succeed();
    }

    @GameTest(template = "arena")
    public static void gearBonusesRespectLevelAndSets(GameTestHelper h) {
        ServerPlayer p = player(h, Race.HUMAN, RpgClass.WARRIOR, new Vec3(3.5, 2, 3.5));
        PlayerRpgData d = ModAttachments.get(p);
        float baseMax = p.getMaxHealth();
        var armor = com.alvorecer.registry.ModGear.AURORITE_ARMOR;
        net.minecraft.world.entity.EquipmentSlot[] slots = {net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET};
        for (int i = 0; i < 4; i++) p.setItemSlot(slots[i], new ItemStack(armor[i].get()));
        StatApplier.apply(p);
        h.assertTrue(p.getMaxHealth() == baseMax, "nível 1 não recebe bônus de equipamento de nível 12");
        d.level = 12;
        StatApplier.apply(p);
        h.assertTrue(Math.abs(p.getMaxHealth() - (20 + Derived.bonusHealth(d))) < 1e-3, "vida máxima com equipamento");
        h.assertTrue(com.alvorecer.item.gear.Equipment.setPieces(p, d, "aurorite") == 4, "conjunto completo");
        h.assertTrue(Derived.damageReduction(d) >= 0.1 - 1e-9, "bônus de conjunto: -10% de dano");
        p.setItemSlot(slots[0], ItemStack.EMPTY);
        StatApplier.apply(p);
        h.assertTrue(Derived.damageReduction(d) < 0.1 - 1e-9, "sem a 4ª peça o bônus de conjunto some");
        leave(h, p);
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 60)
    public static void specialWeaponAbilityIsValidated(GameTestHelper h) {
        ServerPlayer p = player(h, Race.HUMAN, RpgClass.PALADIN, new Vec3(3.5, 2, 1.5));
        PlayerRpgData d = ModAttachments.get(p);
        ItemStack breaker = new ItemStack(com.alvorecer.registry.ModGear.SOLAR_BREAKER.get());
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, breaker);
        Zombie zombie = zombie(h, 3.5, 5.5);
        float energy = d.energy;
        h.assertFalse(AbilityService.useItemAbility(p, breaker, "item_solar_wave"), "nível 1 não pode usar a Ruptora Solar");
        h.assertTrue(d.energy == energy, "nada cobrado");
        d.level = 15;
        d.energy = com.alvorecer.rpg.Derived.maxEnergy(d);
        h.assertTrue(AbilityService.useItemAbility(p, breaker, "item_solar_wave"), "nível 15 usa a habilidade");
        h.assertTrue(p.getCooldowns().isOnCooldown(breaker.getItem()), "recarga visual no item");
        h.assertFalse(AbilityService.useItemAbility(p, breaker, "item_solar_wave"), "em recarga");
        h.runAfterDelay(12, () -> {
            h.assertTrue(zombie.getHealth() < zombie.getMaxHealth(), "a onda deve atingir o zumbi a 4 blocos");
            leave(h, p);
            h.succeed();
        });
    }

    @GameTest(template = "arena")
    public static void workbenchCraftingRequiresProfession(GameTestHelper h) {
        ServerPlayer p = player(h, Race.DWARF, RpgClass.WARRIOR, new Vec3(3.5, 2, 3.5));
        PlayerRpgData d = ModAttachments.get(p);
        BlockPos bench = new BlockPos(1, 2, 1);
        h.setBlock(bench, com.alvorecer.registry.ModGear.WORKBENCH.get());
        d.lastWorkbenchPos = h.absolutePos(bench);
        p.getInventory().add(new ItemStack(com.alvorecer.registry.ModGear.AURORITE_SHARD.get(), 4));
        d.professionLevel = 4;
        h.assertFalse(com.alvorecer.crafting.ProfessionRecipes.craft(p, "aurorite_cutting"), "Minerador nível 4 não lapida");
        d.professionLevel = 5;
        long xp = d.professionXp;
        h.assertTrue(com.alvorecer.crafting.ProfessionRecipes.craft(p, "aurorite_cutting"), "Minerador nível 5 lapida");
        h.assertTrue(p.getInventory().countItem(com.alvorecer.registry.ModGear.AURORITE_GEM.get()) == 1, "gema criada");
        h.assertTrue(p.getInventory().countItem(com.alvorecer.registry.ModGear.AURORITE_SHARD.get()) == 2, "2 fragmentos consumidos");
        h.assertTrue(d.professionXp > xp || d.professionLevel > 5, "XP de profissão concedido");
        h.assertFalse(com.alvorecer.crafting.ProfessionRecipes.craft(p, "mana_elixir"), "sem a profissão Alquimista");
        d.lastWorkbenchPos = null;
        h.assertFalse(com.alvorecer.crafting.ProfessionRecipes.craft(p, "aurorite_cutting"), "longe da bancada");
        leave(h, p);
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 100)
    public static void allCreaturesSpawnAndTick(GameTestHelper h) {
        java.util.List<net.minecraft.world.entity.Mob> mobs = new java.util.ArrayList<>();
        int i = 0;
        for (var type : com.alvorecer.registry.ModMobs.CREATURES.values()) {
            mobs.add(h.spawn(type.get(), new BlockPos(1 + (i % 5), 2, 1 + (i / 5) * 3)));
            i++;
        }
        h.runAfterDelay(60, () -> {
            for (var m : mobs) h.assertTrue(m.isAlive(), m.getType().getDescriptionId() + " deveria continuar vivo após 3 s (remoção: "
                    + m.getRemovalReason() + ", vida " + m.getHealth() + ", última causa: " + m.getLastDamageSource() + ")");
            h.succeed();
        });
    }

    @GameTest(template = "arena", timeoutTicks = 140)
    public static void sentinelSlamIsTelegraphedThenHits(GameTestHelper h) {
        var sentinel = h.spawn(com.alvorecer.registry.ModMobs.STONE_SENTINEL.get(), new Vec3(3.5, 2, 2.0));
        var pig = h.spawn(EntityType.PIG, new Vec3(3.5, 2, 5.6));
        pig.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 10));
        sentinel.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 10));
        sentinel.setTarget(pig);
        float hp = pig.getHealth();
        int[] slamSeenAt = {-1};
        int[] damagedAt = {-1};
        int[] tick = {0};
        h.onEachTick(() -> {
            tick[0]++;
            if (slamSeenAt[0] < 0 && sentinel.slamTicks() > 0) slamSeenAt[0] = tick[0];
            if (damagedAt[0] < 0 && pig.getHealth() < hp) {
                damagedAt[0] = tick[0];
                h.assertTrue(slamSeenAt[0] >= 0 && tick[0] - slamSeenAt[0] >= StoneSentinelEntityTicks.WINDUP - 1,
                        "o dano só pode vir depois do aviso de 1 s (dano no tick " + tick[0] + ", aviso no tick " + slamSeenAt[0]
                                + ", causa " + pig.getLastDamageSource() + ")");
            }
        });
        h.runAfterDelay(110, () -> {
            h.assertTrue(slamSeenAt[0] >= 0, "a sentinela devia preparar o golpe no chão");
            h.assertTrue(!pig.isAlive() || pig.getHealth() < hp, "o golpe no chão devia ferir o alvo dentro do anel (fora do alcance corpo a corpo)");
            h.succeed();
        });
    }

    @GameTest(template = "arena")
    public static void skeletonWarriorBlocksOnlyFrontalProjectiles(GameTestHelper h) {
        var sk = h.spawnWithNoFreeWill(com.alvorecer.registry.ModMobs.SKELETON_WARRIOR.get(), new BlockPos(3, 2, 3));
        sk.setYRot(0);
        sk.setYBodyRot(0);
        sk.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1000));
        sk.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        int frontHits = 0, backHits = 0;
        for (int i = 0; i < 20; i++) {
            var front = new net.minecraft.world.entity.projectile.Arrow(EntityType.ARROW, h.getLevel());
            front.setPos(sk.getX(), sk.getY() + 1, sk.getZ() + 3); // yaw 0 olha para +Z
            sk.invulnerableTime = 0;
            sk.setHealth(sk.getMaxHealth());
            if (sk.hurt(h.getLevel().damageSources().arrow(front, null), 1)) frontHits++;
            var back = new net.minecraft.world.entity.projectile.Arrow(EntityType.ARROW, h.getLevel());
            back.setPos(sk.getX(), sk.getY() + 1, sk.getZ() - 3);
            sk.invulnerableTime = 0;
            if (sk.hurt(h.getLevel().damageSources().arrow(back, null), 1)) backHits++;
        }
        h.assertTrue(backHits == 20, "projéteis pelas costas sempre acertam (" + backHits + ")");
        h.assertTrue(frontHits < 15, "a maioria dos projéteis frontais é bloqueada (" + frontHits + " acertos)");
        h.succeed();
    }

    @GameTest(template = "arena")
    public static void elementalResistsOwnElement(GameTestHelper h) {
        var fire = h.spawnWithNoFreeWill(com.alvorecer.registry.ModMobs.ELEMENTAL.get(), new BlockPos(2, 3, 2));
        fire.setFrost(false);
        var frost = h.spawnWithNoFreeWill(com.alvorecer.registry.ModMobs.ELEMENTAL.get(), new BlockPos(4, 3, 4));
        frost.setFrost(true);
        var src = h.getLevel().damageSources().source(com.alvorecer.registry.ModDamageTypes.FIRE);
        h.assertFalse(fire.hurt(src, 4), "elemental de fogo é imune a fogo");
        float before = frost.getHealth();
        h.assertTrue(frost.hurt(src, 4), "elemental de gelo sofre fogo");
        h.assertTrue(before - frost.getHealth() >= 7.9f, "dano de fogo dobrado no de gelo");
        h.succeed();
    }

    @GameTest(template = "arena")
    public static void npcTradesProtectionAndQuests(GameTestHelper h) {
        ServerPlayer p = player(h, Race.HUMAN, RpgClass.BARD, new Vec3(3.5, 2, 3.5));
        PlayerRpgData d = ModAttachments.get(p);
        var master = h.spawn(com.alvorecer.registry.ModMobs.GUILD_MASTER.get(), new BlockPos(3, 2, 5));
        var merchant = h.spawn(com.alvorecer.registry.ModMobs.MERCHANT.get(), new BlockPos(5, 2, 5));
        h.assertTrue(!master.getOffers().isEmpty() && !merchant.getOffers().isEmpty(), "NPCs têm ofertas");
        var zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        h.assertFalse(master.hurt(h.getLevel().damageSources().mobAttack(zombie), 10), "NPC não pode ser ferido");
        h.assertFalse(com.alvorecer.quest.Quests.accept(p, "zombie_hunt"), "sem falar com o NPC não aceita");
        master.handleAction(p, 1);
        h.assertTrue(d.questGiver != null && d.questGiver.equals(master.getUUID()), "conversa com o Mestre registrada");
        h.assertTrue(com.alvorecer.quest.Quests.accept(p, "zombie_hunt"), "o Mestre da Guilda oferece missões");
        merchant.handleAction(p, 1);
        h.assertTrue(d.questGiver.equals(master.getUUID()), "o Mercador não oferece missões");
        leave(h, p);
        h.succeed();
    }

    /** Moldura 4×5 (interior 2×3) no plano X, de x=1 a 4, em z=3, base em y=2. */
    private static void portalFrame(GameTestHelper h) {
        var stone = com.alvorecer.registry.ModWorld.DAWNSTONE.get();
        for (int x = 1; x <= 4; x++) {
            h.setBlock(x, 2, 3, stone);
            h.setBlock(x, 6, 3, stone);
        }
        for (int y = 3; y <= 5; y++) {
            h.setBlock(1, y, 3, stone);
            h.setBlock(4, y, 3, stone);
        }
    }

    private static net.minecraft.world.InteractionResult useKey(GameTestHelper h, ServerPlayer p, ItemStack key) {
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, key);
        BlockPos clicked = h.absolutePos(new BlockPos(2, 2, 3));
        var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(clicked), net.minecraft.core.Direction.UP, clicked, false);
        return key.useOn(new net.minecraft.world.item.context.UseOnContext(p, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
    }

    @GameTest(template = "arena", timeoutTicks = 40)
    public static void dawnKeyLightsAndFrameBreakClosesPortal(GameTestHelper h) {
        var portal = com.alvorecer.registry.ModWorld.DAWN_PORTAL.get();
        ServerPlayer p = player(h, Race.HUMAN, RpgClass.WARRIOR, new Vec3(2.5, 2, 1.5));
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); // em criativo a chave não gasta
        portalFrame(h);
        h.setBlock(4, 4, 3, Blocks.AIR);
        ItemStack key = new ItemStack(com.alvorecer.registry.ModWorld.DAWN_KEY.get());
        h.assertTrue(useKey(h, p, key) == net.minecraft.world.InteractionResult.FAIL, "moldura incompleta não acende");
        h.assertBlockNotPresent(portal, new BlockPos(2, 3, 3));
        h.setBlock(4, 4, 3, com.alvorecer.registry.ModWorld.DAWNSTONE.get());
        h.assertTrue(useKey(h, p, key).consumesAction(), "moldura completa acende");
        for (int x = 2; x <= 3; x++) for (int y = 3; y <= 5; y++) h.assertBlockPresent(portal, new BlockPos(x, y, 3));
        h.assertTrue(key.getDamageValue() == 1, "a chave gasta 1 de durabilidade");
        h.assertTrue(ModAttachments.get(p).achievements.contains("portal"), "conquista do portal");
        leave(h, p);
        // O POI é registrado no tick seguinte; depois disso a busca de destino encontra este portal
        h.runAfterDelay(2, () -> {
            var found = com.alvorecer.world.PortalTravel.findPortal(h.getLevel(), h.absolutePos(new BlockPos(3, 4, 1)));
            h.assertTrue(found.isPresent() && found.get().getY() == h.absolutePos(new BlockPos(2, 3, 3)).getY()
                    && h.getLevel().getBlockState(found.get()).is(portal), "base do portal achada pelo POI: " + found);
            h.setBlock(1, 4, 3, Blocks.AIR);
            for (int x = 2; x <= 3; x++) for (int y = 3; y <= 5; y++) h.assertBlockNotPresent(portal, new BlockPos(x, y, 3));
            h.succeed();
        });
    }

    /** O servidor de GameTest usa um mundo plano sem dimensões de datapack; a viagem real é validada no servidor dedicado. */
    @GameTest(template = "arena", timeoutTicks = 40)
    public static void returnPortalIsBuiltOnSolidGround(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos portal = com.alvorecer.world.PortalTravel.buildPortal(level, h.absolutePos(new BlockPos(3, 2, 3)));
        var shape = com.alvorecer.world.PortalShape.find(level, portal);
        h.assertTrue(shape != null && shape.width() == 2 && shape.height() == 3 && shape.bottomLeft().equals(portal), "moldura 2×3 válida: " + shape);
        h.assertTrue(level.getBlockState(portal).is(com.alvorecer.registry.ModWorld.DAWN_PORTAL.get()), "portal aceso");
        BlockPos exit = com.alvorecer.world.PortalTravel.exit(level, portal);
        h.assertTrue(exit.equals(portal.south().below()) && level.getBlockState(exit.below()).is(com.alvorecer.registry.ModWorld.DAWNSTONE.get()),
                "saída ao lado do portal, em pé na plataforma: " + exit);
        h.runAfterDelay(2, () -> {
            var found = com.alvorecer.world.PortalTravel.findPortal(level, portal.offset(5, 2, 5));
            h.assertTrue(found.isPresent() && found.get().getY() == portal.getY() && found.get().distManhattan(portal) <= 1,
                    "a próxima viagem reutiliza o portal: " + found);
            h.succeed();
        });
    }

    /** Destino em chunk ainda não gerado: a altura vem do gerador, não do fallback no ar (y=100). */
    @GameTest(template = "arena")
    public static void returnPortalUsesTerrainInUnloadedChunks(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos far = h.absolutePos(new BlockPos(3, 2, 3)).offset(4000, 0, 4000);
        h.assertFalse(level.hasChunk(far.getX() >> 4, far.getZ() >> 4), "chunk de destino ainda não carregado");
        BlockPos portal = com.alvorecer.world.PortalTravel.buildPortal(level, far);
        int ground = level.getChunkSource().getGenerator().getBaseHeight(far.getX(), far.getZ(),
                net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, level, level.getChunkSource().randomState());
        h.assertTrue(portal.getY() == ground + 1, "portal no chão do terreno (" + ground + "), não no ar: " + portal);
        h.succeed();
    }

    private static final class StoneSentinelEntityTicks {
        static final int WINDUP = com.alvorecer.entity.mob.StoneSentinelEntity.SLAM_WINDUP;
    }
}
