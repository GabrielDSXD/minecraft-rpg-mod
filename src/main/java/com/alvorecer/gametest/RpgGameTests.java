package com.alvorecer.gametest;

import com.alvorecer.Alvorecer;
import com.alvorecer.ability.AbilityService;
import com.alvorecer.network.ModNetwork;
import com.alvorecer.registry.ModAttachments;
import com.alvorecer.rpg.Derived;
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
}
