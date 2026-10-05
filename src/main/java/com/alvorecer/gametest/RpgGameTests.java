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
        Zombie zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, (float) x, 1, (float) z);
        zombie.setYRot(180);
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET)); // não queima ao sol
        zombie.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2000));
        return zombie;
    }

    @GameTest(template = "arena")
    public static void powerStrikeHitsChargesAndCoolsDown(GameTestHelper h) {
        ServerPlayer p = player(h, Race.HUMAN, RpgClass.WARRIOR, new Vec3(3.5, 1, 1.5));
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
        ServerPlayer p = player(h, Race.HUMAN, RpgClass.WARRIOR, new Vec3(3.5, 1, 1.5));
        h.setBlock(new BlockPos(3, 1, 2), Blocks.STONE);
        h.setBlock(new BlockPos(3, 2, 2), Blocks.STONE);
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
        ServerPlayer p = player(h, Race.ORC, RpgClass.BARBARIAN, new Vec3(3.5, 1, 3.5));
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
        ServerPlayer p = player(h, Race.DWARF, RpgClass.CLERIC, new Vec3(3.5, 1, 3.5));
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
        ServerPlayer p = player(h, Race.DRAGONBORN, RpgClass.WIZARD, new Vec3(3.5, 1, 3.5));
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
        ServerPlayer p = player(h, Race.HUMAN, RpgClass.WARRIOR, new Vec3(3.5, 1, 3.5));
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
        ServerPlayer a = player(h, Race.HUMAN, RpgClass.WARRIOR, new Vec3(2.5, 1, 2.5));
        ServerPlayer b = player(h, Race.ELF, RpgClass.CLERIC, new Vec3(4.5, 1, 2.5));
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
        ServerPlayer p = player(h, Race.ORC, RpgClass.BARBARIAN, new Vec3(3.5, 1, 3.5));
        PlayerRpgData d = ModAttachments.get(p);
        BlockPos board = new BlockPos(1, 1, 1);
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
        ServerPlayer p = player(h, Race.HUMAN, RpgClass.WARRIOR, new Vec3(3.5, 1, 3.5));
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
        ServerPlayer p = player(h, Race.HUMAN, RpgClass.PALADIN, new Vec3(3.5, 1, 1.5));
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
        ServerPlayer p = player(h, Race.DWARF, RpgClass.WARRIOR, new Vec3(3.5, 1, 3.5));
        PlayerRpgData d = ModAttachments.get(p);
        BlockPos bench = new BlockPos(1, 1, 1);
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
}
