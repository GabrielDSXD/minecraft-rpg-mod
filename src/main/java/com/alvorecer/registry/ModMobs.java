package com.alvorecer.registry;

import com.alvorecer.config.RpgConfig;
import com.alvorecer.entity.mob.ArcaneWolfEntity;
import com.alvorecer.entity.mob.ElementalBoltEntity;
import com.alvorecer.entity.mob.ElementalEntity;
import com.alvorecer.entity.mob.FairyEntity;
import com.alvorecer.entity.mob.GoblinEntity;
import com.alvorecer.entity.mob.SkeletonWarriorEntity;
import com.alvorecer.entity.mob.StoneSentinelEntity;
import com.alvorecer.entity.mob.WyrmlingEntity;
import com.alvorecer.entity.npc.NpcEntity;
import com.alvorecer.item.BestiaryScepterItem;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Etapa 4: criaturas, NPCs, projétil elemental, drops, ovos e o Cetro do Bestiário. */
public final class ModMobs {
    private static final DeferredRegister<EntityType<?>> E = ModEntities.ENTITIES;
    private static final DeferredRegister.Items I = ModItems.ITEMS;

    private static <T extends net.minecraft.world.entity.Entity> DeferredHolder<EntityType<?>, EntityType<T>> type(
            String name, EntityType.EntityFactory<T> factory, MobCategory cat, float w, float h, boolean fireImmune) {
        return E.register(name, () -> {
            EntityType.Builder<T> b = EntityType.Builder.of(factory, cat).sized(w, h).clientTrackingRange(10);
            if (fireImmune) b.fireImmune();
            return b.build(name);
        });
    }

    public static final DeferredHolder<EntityType<?>, EntityType<GoblinEntity>> GOBLIN = type("goblin", GoblinEntity::new, MobCategory.MONSTER, 0.6f, 1.3f, false);
    public static final DeferredHolder<EntityType<?>, EntityType<SkeletonWarriorEntity>> SKELETON_WARRIOR = type("skeleton_warrior", SkeletonWarriorEntity::new, MobCategory.MONSTER, 0.6f, 1.99f, false);
    public static final DeferredHolder<EntityType<?>, EntityType<ArcaneWolfEntity>> ARCANE_WOLF = type("arcane_wolf", ArcaneWolfEntity::new, MobCategory.MONSTER, 0.7f, 0.9f, false);
    public static final DeferredHolder<EntityType<?>, EntityType<ElementalEntity>> ELEMENTAL = type("elemental", ElementalEntity::new, MobCategory.MONSTER, 0.8f, 1.6f, false);
    public static final DeferredHolder<EntityType<?>, EntityType<StoneSentinelEntity>> STONE_SENTINEL = type("stone_sentinel", StoneSentinelEntity::new, MobCategory.MONSTER, 1.4f, 2.7f, false);
    public static final DeferredHolder<EntityType<?>, EntityType<FairyEntity>> FAIRY = type("fairy", FairyEntity::new, MobCategory.CREATURE, 0.4f, 0.6f, false);
    public static final DeferredHolder<EntityType<?>, EntityType<WyrmlingEntity>> WYRMLING = type("wyrmling", WyrmlingEntity::new, MobCategory.MONSTER, 1.2f, 1.0f, true);
    public static final DeferredHolder<EntityType<?>, EntityType<ElementalBoltEntity>> ELEMENTAL_BOLT = E.register("elemental_bolt",
            () -> EntityType.Builder.<ElementalBoltEntity>of(ElementalBoltEntity::new, MobCategory.MISC).sized(0.4f, 0.4f)
                    .clientTrackingRange(4).updateInterval(10).build("elemental_bolt"));
    public static final DeferredHolder<EntityType<?>, EntityType<NpcEntity>> GUILD_MASTER = type("guild_master", NpcEntity::new, MobCategory.MISC, 0.6f, 1.95f, false);
    public static final DeferredHolder<EntityType<?>, EntityType<NpcEntity>> MERCHANT = type("merchant", NpcEntity::new, MobCategory.MISC, 0.6f, 1.95f, false);
    public static final DeferredHolder<EntityType<?>, EntityType<NpcEntity>> SAGE = type("sage", NpcEntity::new, MobCategory.MISC, 0.6f, 1.95f, false);

    /** Criaturas e NPCs na ordem do Bestiário e do Cetro. */
    public static final Map<String, Supplier<? extends EntityType<? extends Mob>>> CREATURES = new LinkedHashMap<>();

    static {
        CREATURES.put("goblin", GOBLIN);
        CREATURES.put("skeleton_warrior", SKELETON_WARRIOR);
        CREATURES.put("arcane_wolf", ARCANE_WOLF);
        CREATURES.put("elemental", ELEMENTAL);
        CREATURES.put("stone_sentinel", STONE_SENTINEL);
        CREATURES.put("fairy", FAIRY);
        CREATURES.put("wyrmling", WYRMLING);
        CREATURES.put("guild_master", GUILD_MASTER);
        CREATURES.put("merchant", MERCHANT);
        CREATURES.put("sage", SAGE);
    }

    // ------------------------------------------------------------ drops

    public static final DeferredItem<Item> GOBLIN_EAR = I.registerSimpleItem("goblin_ear");
    public static final DeferredItem<Item> ARCANE_FANG = I.registerSimpleItem("arcane_fang");
    public static final DeferredItem<Item> ELEMENTAL_CORE = I.registerSimpleItem("elemental_core");
    public static final DeferredItem<Item> SENTINEL_HEART = I.registerSimpleItem("sentinel_heart", new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<Item> FAIRY_DUST = I.registerSimpleItem("fairy_dust");
    public static final DeferredItem<Item> WYRMLING_SCALE = I.registerSimpleItem("wyrmling_scale", new Item.Properties().fireResistant());

    // ------------------------------------------------------------ ovos e cetro

    private static void egg(String name, Supplier<? extends EntityType<? extends Mob>> type, int bg, int fg) {
        I.register(name + "_spawn_egg", () -> new DeferredSpawnEggItem(type, bg, fg, new Item.Properties()));
    }

    static {
        egg("goblin", GOBLIN, 0x4E7A2E, 0xC9B458);
        egg("skeleton_warrior", SKELETON_WARRIOR, 0xD8D3C2, 0x5A4632);
        egg("arcane_wolf", ARCANE_WOLF, 0x3B3F7A, 0x8FD3FF);
        egg("elemental", ELEMENTAL, 0xE0662F, 0x9BD7FF);
        egg("stone_sentinel", STONE_SENTINEL, 0x7A7A7A, 0xF29E46);
        egg("fairy", FAIRY, 0xF6C6F0, 0xFFF7B0);
        egg("wyrmling", WYRMLING, 0xA8362A, 0xFFC04A);
        egg("guild_master", GUILD_MASTER, 0x5A3A1E, 0xE0B85A);
        egg("merchant", MERCHANT, 0x2E6B3A, 0xE8D8A0);
        egg("sage", SAGE, 0x3A2E6B, 0xC9B8FF);
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> SELECTED_CREATURE = ModGear.COMPONENTS.registerComponentType(
            "selected_creature", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));
    public static final DeferredItem<BestiaryScepterItem> BESTIARY_SCEPTER = I.register("bestiary_scepter",
            () -> new BestiaryScepterItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    private ModMobs() {
    }

    public static void init() {
    }

    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(GOBLIN.get(), GoblinEntity.createAttributes().build());
        event.put(SKELETON_WARRIOR.get(), SkeletonWarriorEntity.createAttributes().build());
        event.put(ARCANE_WOLF.get(), ArcaneWolfEntity.createAttributes().build());
        event.put(ELEMENTAL.get(), ElementalEntity.createAttributes().build());
        event.put(STONE_SENTINEL.get(), StoneSentinelEntity.createAttributes().build());
        event.put(FAIRY.get(), FairyEntity.createAttributes().build());
        event.put(WYRMLING.get(), WyrmlingEntity.createAttributes().build());
        for (var npc : new Supplier[]{GUILD_MASTER, MERCHANT, SAGE}) {
            @SuppressWarnings("unchecked") EntityType<NpcEntity> t = (EntityType<NpcEntity>) npc.get();
            event.put(t, NpcEntity.createAttributes().build());
        }
    }

    /** Regras de nascimento; a taxa global vem da configuração (creatureSpawnRate). */
    public static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        var op = RegisterSpawnPlacementsEvent.Operation.REPLACE;
        for (var t : new Supplier[]{GOBLIN, SKELETON_WARRIOR, ARCANE_WOLF, ELEMENTAL, STONE_SENTINEL, WYRMLING}) {
            @SuppressWarnings("unchecked") EntityType<Monster> type = (EntityType<Monster>) t.get();
            event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (et, level, reason, pos, random) -> random.nextDouble() < RpgConfig.CREATURE_SPAWN_RATE.get()
                            && Monster.checkMonsterSpawnRules(et, level, reason, pos, random), op);
        }
        event.register(FAIRY.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (et, level, reason, pos, random) -> random.nextDouble() < RpgConfig.CREATURE_SPAWN_RATE.get()
                        && Mob.checkMobSpawnRules(et, level, reason, pos, random), op);
    }
}
