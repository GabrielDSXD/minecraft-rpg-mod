package com.alvorecer.registry;

import com.alvorecer.Alvorecer;
import com.alvorecer.block.WorkbenchBlock;
import com.alvorecer.item.ElixirItem;
import com.alvorecer.item.gear.CasterItem;
import com.alvorecer.item.gear.GearInfo;
import com.alvorecer.item.gear.GearRarity;
import com.alvorecer.item.gear.GearTiers;
import com.alvorecer.item.gear.RpgBowItem;
import com.alvorecer.item.gear.RpgCrossbowItem;
import com.alvorecer.rpg.ArmorWeight;
import com.alvorecer.rpg.WeaponCategory;
import com.alvorecer.rpg.talent.BonusType;
import com.mojang.serialization.Codec;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

import static com.alvorecer.item.gear.GearRarity.*;
import static com.alvorecer.rpg.talent.BonusType.*;

/** Etapa 3: materiais, minérios, ferramentas, armas, armaduras, asas, cajados, varinhas e consumíveis. */
public final class ModGear {
    private static final DeferredRegister.Items ITEMS = ModItems.ITEMS;
    private static final DeferredRegister.Blocks BLOCKS = ModBlocks.BLOCKS;
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, Alvorecer.MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Alvorecer.MODID);

    /** Feitiço selecionado no Cajado Arcano. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> SELECTED_SPELL = COMPONENTS.registerComponentType(
            "selected_spell", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    // ------------------------------------------------------------ materiais e blocos

    public static final DeferredItem<Item> RAW_LUNAR_SILVER = ITEMS.registerSimpleItem("raw_lunar_silver");
    public static final DeferredItem<Item> LUNAR_SILVER_INGOT = ITEMS.registerSimpleItem("lunar_silver_ingot");
    public static final DeferredItem<Item> AURORITE_SHARD = ITEMS.registerSimpleItem("aurorite_shard");
    public static final DeferredItem<Item> AURORITE_GEM = ITEMS.registerSimpleItem("aurorite_gem", new Item.Properties().fireResistant());

    public static final DeferredBlock<DropExperienceBlock> LUNAR_SILVER_ORE = BLOCKS.registerBlock("lunar_silver_ore",
            p -> new DropExperienceBlock(UniformInt.of(1, 3), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops());
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_LUNAR_SILVER_ORE = BLOCKS.registerBlock("deepslate_lunar_silver_ore",
            p -> new DropExperienceBlock(UniformInt.of(1, 3), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(4.5f, 3.0f).sound(SoundType.DEEPSLATE).requiresCorrectToolForDrops());
    public static final DeferredBlock<DropExperienceBlock> AURORITE_ORE = BLOCKS.registerBlock("aurorite_ore",
            p -> new DropExperienceBlock(UniformInt.of(3, 7), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(5.0f, 6.0f).sound(SoundType.DEEPSLATE)
                    .lightLevel(s -> 3).requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> LUNAR_SILVER_BLOCK = BLOCKS.registerSimpleBlock("lunar_silver_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> AURORITE_BLOCK = BLOCKS.registerSimpleBlock("aurorite_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(6.0f, 9.0f).sound(SoundType.AMETHYST)
                    .lightLevel(s -> 7).requiresCorrectToolForDrops());
    public static final DeferredBlock<WorkbenchBlock> WORKBENCH = BLOCKS.registerBlock("profession_workbench", WorkbenchBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5f).sound(SoundType.WOOD).ignitedByLava());

    static {
        for (var b : List.of(LUNAR_SILVER_ORE, DEEPSLATE_LUNAR_SILVER_ORE, AURORITE_ORE, LUNAR_SILVER_BLOCK, AURORITE_BLOCK, WORKBENCH)) {
            ITEMS.registerSimpleBlockItem(b);
        }
    }

    // ------------------------------------------------------------ armaduras

    private static Holder<ArmorMaterial> material(String name, int boots, int legs, int chest, int helmet, int ench,
                                                  Holder<net.minecraft.sounds.SoundEvent> sound, Supplier<Ingredient> repair,
                                                  float toughness, float knockback) {
        EnumMap<ArmorItem.Type, Integer> def = Util.make(new EnumMap<>(ArmorItem.Type.class), m -> {
            m.put(ArmorItem.Type.BOOTS, boots);
            m.put(ArmorItem.Type.LEGGINGS, legs);
            m.put(ArmorItem.Type.CHESTPLATE, chest);
            m.put(ArmorItem.Type.HELMET, helmet);
            m.put(ArmorItem.Type.BODY, chest);
        });
        return ARMOR_MATERIALS.register(name, () -> new ArmorMaterial(def, ench, sound, repair,
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Alvorecer.MODID, name))), toughness, knockback));
    }

    public static final Holder<ArmorMaterial> WANDERER_MAT = material("wanderer", 2, 3, 4, 2, 15, SoundEvents.ARMOR_EQUIP_LEATHER,
            () -> Ingredient.of(Items.LEATHER), 0, 0);
    public static final Holder<ArmorMaterial> ARCANE_MAT = material("arcane", 1, 2, 3, 1, 25, SoundEvents.ARMOR_EQUIP_LEATHER,
            () -> Ingredient.of(Items.AMETHYST_SHARD), 0, 0);
    public static final Holder<ArmorMaterial> LUNAR_MAT = material("lunar_silver", 2, 5, 6, 2, 18, SoundEvents.ARMOR_EQUIP_CHAIN,
            () -> Ingredient.of(LUNAR_SILVER_INGOT.get()), 1, 0);
    public static final Holder<ArmorMaterial> AURORITE_MAT = material("aurorite", 3, 6, 8, 3, 16, SoundEvents.ARMOR_EQUIP_NETHERITE,
            () -> Ingredient.of(AURORITE_GEM.get()), 3, 0.1f);

    private static DeferredItem<ArmorItem> armor(String name, Holder<ArmorMaterial> mat, ArmorItem.Type type, int durability, GearInfo info) {
        DeferredItem<ArmorItem> item = ITEMS.register(name, () -> new ArmorItem(mat, type, new Item.Properties().durability(type.getDurability(durability))));
        GearInfo.register(item, info);
        return item;
    }

    private static DeferredItem<ArmorItem>[] armorSet(String prefix, Holder<ArmorMaterial> mat, int durability, String set,
                                                      ArmorWeight weight, GearRarity rarity, int level, BonusType bonus, double amount,
                                                      BonusType bonus2, double amount2) {
        ArmorItem.Type[] types = {ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS};
        String[] names = {"helmet", "chestplate", "leggings", "boots"};
        @SuppressWarnings("unchecked") DeferredItem<ArmorItem>[] out = new DeferredItem[4];
        for (int i = 0; i < 4; i++) {
            GearInfo.Builder b = GearInfo.of(rarity, level).weight(weight).set(set).bonus(bonus, amount);
            if (bonus2 != null) b.bonus(bonus2, amount2);
            out[i] = armor(prefix + "_" + names[i], mat, types[i], durability, b.build());
        }
        return out;
    }

    public static final DeferredItem<ArmorItem>[] WANDERER = armorSet("wanderer", WANDERER_MAT, 14, "wanderer", ArmorWeight.LIGHT,
            UNCOMMON, 3, SPEED, 0.02, null, 0);
    public static final DeferredItem<ArmorItem>[] ARCANE = armorSet("arcane", ARCANE_MAT, 16, "arcane", ArmorWeight.LIGHT,
            RARE, 5, MAX_MANA, 0.05, null, 0);
    public static final DeferredItem<ArmorItem>[] LUNAR_ARMOR = armorSet("lunar_silver", LUNAR_MAT, 22, "lunar", ArmorWeight.MEDIUM,
            UNCOMMON, 6, MAGIC_RESIST, 0.02, null, 0);
    public static final DeferredItem<ArmorItem>[] AURORITE_ARMOR = armorSet("aurorite", AURORITE_MAT, 34, "aurorite", ArmorWeight.HEAVY,
            EPIC, 12, MAX_HEALTH, 2, SPEED, -0.02);

    // ------------------------------------------------------------ armas e ferramentas

    private static ItemAttributeModifiers melee(Tier tier, float base, float speed, double reach) {
        var b = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, base + tier.getAttackDamageBonus(),
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, speed, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND);
        if (reach != 0) {
            b.add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(ResourceLocation.fromNamespaceAndPath(Alvorecer.MODID, "weapon_reach"),
                    reach, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        }
        return b.build();
    }

    /** Tipos de arma corpo a corpo: dano base, velocidade (modificador) e alcance extra. */
    private enum Melee {
        SWORD(3, -2.4f, 0), DAGGER(1, -1.6f, 0), HAMMER(6, -3.2f, 0), SPEAR(4, -2.8f, 1.0);

        final float base, speed;
        final double reach;

        Melee(float base, float speed, double reach) {
            this.base = base;
            this.speed = speed;
            this.reach = reach;
        }

        WeaponCategory category() {
            return switch (this) {
                case SWORD -> WeaponCategory.SWORD;
                case DAGGER -> WeaponCategory.DAGGER;
                case HAMMER -> WeaponCategory.HAMMER;
                case SPEAR -> WeaponCategory.SPEAR;
            };
        }
    }

    private static DeferredItem<SwordItem> weapon(String name, Tier tier, Melee type, float extraDamage, Item.Properties props, GearInfo.Builder info) {
        DeferredItem<SwordItem> item = ITEMS.register(name, () -> new SwordItem(tier,
                props.attributes(melee(tier, type.base + extraDamage, type.speed, type.reach))));
        GearInfo.register(item, info.category(type.category()).build());
        return item;
    }

    private static DeferredItem<SwordItem> weapon(String name, Tier tier, Melee type, GearInfo.Builder info) {
        return weapon(name, tier, type, 0, new Item.Properties(), info);
    }

    private static <T extends Item> DeferredItem<T> gear(String name, Supplier<T> sup, GearInfo info) {
        DeferredItem<T> item = ITEMS.register(name, sup);
        GearInfo.register(item, info);
        return item;
    }

    // Ferro (início de jogo)
    public static final DeferredItem<SwordItem> IRON_DAGGER = weapon("iron_dagger", Tiers.IRON, Melee.DAGGER, GearInfo.of(COMMON, 1).bonus(CRIT_CHANCE, 0.02));
    public static final DeferredItem<SwordItem> IRON_WARHAMMER = weapon("iron_warhammer", Tiers.IRON, Melee.HAMMER, GearInfo.of(COMMON, 3));
    public static final DeferredItem<SwordItem> IRON_SPEAR = weapon("iron_spear", Tiers.IRON, Melee.SPEAR, GearInfo.of(COMMON, 2));

    // Prata Lunar
    private static final GearTiers L = GearTiers.LUNAR_SILVER;
    public static final DeferredItem<SwordItem> LUNAR_SWORD = weapon("lunar_silver_sword", L, Melee.SWORD, GearInfo.of(UNCOMMON, 5).bonus(MELEE, 0.03));
    public static final DeferredItem<SwordItem> LUNAR_DAGGER = weapon("lunar_silver_dagger", L, Melee.DAGGER, GearInfo.of(UNCOMMON, 5).bonus(CRIT_CHANCE, 0.03));
    public static final DeferredItem<SwordItem> LUNAR_WARHAMMER = weapon("lunar_silver_warhammer", L, Melee.HAMMER, GearInfo.of(UNCOMMON, 6).bonus(MELEE, 0.05));
    public static final DeferredItem<SwordItem> LUNAR_SPEAR = weapon("lunar_silver_spear", L, Melee.SPEAR, GearInfo.of(UNCOMMON, 5).bonus(MELEE, 0.03));
    public static final DeferredItem<PickaxeItem> LUNAR_PICKAXE = gear("lunar_silver_pickaxe",
            () -> new PickaxeItem(L, new Item.Properties().attributes(PickaxeItem.createAttributes(L, 1.0f, -2.8f))), GearInfo.of(UNCOMMON, 4).build());
    public static final DeferredItem<AxeItem> LUNAR_AXE = gear("lunar_silver_axe",
            () -> new AxeItem(L, new Item.Properties().attributes(AxeItem.createAttributes(L, 5.5f, -3.1f))),
            GearInfo.of(UNCOMMON, 4).category(WeaponCategory.AXE).build());
    public static final DeferredItem<ShovelItem> LUNAR_SHOVEL = gear("lunar_silver_shovel",
            () -> new ShovelItem(L, new Item.Properties().attributes(ShovelItem.createAttributes(L, 1.5f, -3.0f))), GearInfo.of(UNCOMMON, 4).build());
    public static final DeferredItem<HoeItem> LUNAR_HOE = gear("lunar_silver_hoe",
            () -> new HoeItem(L, new Item.Properties().attributes(HoeItem.createAttributes(L, -2.5f, -1.0f))), GearInfo.of(UNCOMMON, 4).build());
    public static final DeferredItem<RpgBowItem> LUNAR_BOW = gear("lunar_silver_bow",
            () -> new RpgBowItem(new Item.Properties().durability(500), 1.2), GearInfo.of(UNCOMMON, 5).category(WeaponCategory.BOW).bonus(RANGED, 0.05).build());
    public static final DeferredItem<RpgCrossbowItem> LUNAR_CROSSBOW = gear("lunar_silver_crossbow",
            () -> new RpgCrossbowItem(new Item.Properties().durability(560), 1.2), GearInfo.of(UNCOMMON, 5).category(WeaponCategory.CROSSBOW).build());
    public static final DeferredItem<ShieldItem> LUNAR_SHIELD = gear("lunar_silver_shield",
            () -> new ShieldItem(new Item.Properties().durability(520)), GearInfo.of(UNCOMMON, 4).category(WeaponCategory.SHIELD).bonus(DAMAGE_TAKEN, 0.03).build());

    // Aurorita
    private static final GearTiers A = GearTiers.AURORITE;
    public static final DeferredItem<SwordItem> AURORITE_SWORD = weapon("aurorite_sword", A, Melee.SWORD, GearInfo.of(RARE, 10).bonus(MELEE, 0.06).bonus(CRIT_CHANCE, 0.03));
    public static final DeferredItem<SwordItem> AURORITE_DAGGER = weapon("aurorite_dagger", A, Melee.DAGGER, GearInfo.of(RARE, 10).bonus(CRIT_CHANCE, 0.05));
    public static final DeferredItem<SwordItem> AURORITE_WARHAMMER = weapon("aurorite_warhammer", A, Melee.HAMMER, GearInfo.of(RARE, 11).bonus(MELEE, 0.08));
    public static final DeferredItem<SwordItem> AURORITE_SPEAR = weapon("aurorite_spear", A, Melee.SPEAR, GearInfo.of(RARE, 10).bonus(MELEE, 0.05));
    public static final DeferredItem<PickaxeItem> AURORITE_PICKAXE = gear("aurorite_pickaxe",
            () -> new PickaxeItem(A, new Item.Properties().fireResistant().attributes(PickaxeItem.createAttributes(A, 1.0f, -2.8f))), GearInfo.of(RARE, 9).build());
    public static final DeferredItem<AxeItem> AURORITE_AXE = gear("aurorite_axe",
            () -> new AxeItem(A, new Item.Properties().fireResistant().attributes(AxeItem.createAttributes(A, 5.0f, -3.0f))),
            GearInfo.of(RARE, 9).category(WeaponCategory.AXE).build());
    public static final DeferredItem<ShovelItem> AURORITE_SHOVEL = gear("aurorite_shovel",
            () -> new ShovelItem(A, new Item.Properties().fireResistant().attributes(ShovelItem.createAttributes(A, 1.5f, -3.0f))), GearInfo.of(RARE, 9).build());
    public static final DeferredItem<HoeItem> AURORITE_HOE = gear("aurorite_hoe",
            () -> new HoeItem(A, new Item.Properties().fireResistant().attributes(HoeItem.createAttributes(A, -4.0f, 0.0f))), GearInfo.of(RARE, 9).build());
    public static final DeferredItem<RpgBowItem> AURORITE_BOW = gear("aurorite_bow",
            () -> new RpgBowItem(new Item.Properties().durability(1200).fireResistant(), 1.45), GearInfo.of(RARE, 10).category(WeaponCategory.BOW).bonus(RANGED, 0.08).build());
    public static final DeferredItem<RpgCrossbowItem> AURORITE_CROSSBOW = gear("aurorite_crossbow",
            () -> new RpgCrossbowItem(new Item.Properties().durability(1300).fireResistant(), 1.45), GearInfo.of(RARE, 10).category(WeaponCategory.CROSSBOW).bonus(RANGED, 0.06).build());
    public static final DeferredItem<ShieldItem> AURORITE_SHIELD = gear("aurorite_shield",
            () -> new ShieldItem(new Item.Properties().durability(1200).fireResistant()), GearInfo.of(RARE, 10).category(WeaponCategory.SHIELD).bonus(DAMAGE_TAKEN, 0.05).build());

    // Cajados e varinhas
    public static final DeferredItem<CasterItem> OAK_STAFF = gear("oak_staff",
            () -> new CasterItem(new Item.Properties().stacksTo(1), "staff_arcane_bolt", false),
            GearInfo.of(COMMON, 1).category(WeaponCategory.STAFF).bonus(POWER_ARCANE, 0.03).build());
    public static final DeferredItem<CasterItem> AMETHYST_WAND = gear("amethyst_wand",
            () -> new CasterItem(new Item.Properties().stacksTo(1), "wand_bolt", false),
            GearInfo.of(UNCOMMON, 3).category(WeaponCategory.WAND).bonus(POWER_ARCANE, 0.05).build());
    public static final DeferredItem<CasterItem> AURORITE_WAND = gear("aurorite_wand",
            () -> new CasterItem(new Item.Properties().stacksTo(1).fireResistant(), "wand_bolt", false),
            GearInfo.of(EPIC, 12).category(WeaponCategory.WAND).bonus(POWER_ARCANE, 0.10).bonus(COOLDOWN, 0.05).build());
    public static final DeferredItem<CasterItem> ARCANE_STAFF = gear("arcane_staff",
            () -> new CasterItem(new Item.Properties().stacksTo(1).fireResistant(), "staff_arcane_bolt", true),
            GearInfo.of(LEGENDARY, 10).category(WeaponCategory.STAFF).bonus(POWER_ARCANE, 0.15).bonus(POWER_ELEMENTAL, 0.15).bonus(MAX_MANA, 0.10).build());

    // Armas especiais
    public static final DeferredItem<SwordItem> SOLAR_BREAKER = weapon("solar_breaker", A, Melee.HAMMER, 1,
            new Item.Properties().fireResistant(), GearInfo.of(LEGENDARY, 15).bonus(POWER_DIVINE, 0.15).bonus(MELEE, 0.10).ability("item_solar_wave"));
    public static final DeferredItem<SwordItem> DAWN_BLADE = weapon("dawn_blade", A, Melee.SWORD, 1,
            new Item.Properties().fireResistant(), GearInfo.of(LEGENDARY, 14).bonus(MELEE, 0.12).bonus(SPEED, 0.05).ability("item_dawn_dash"));
    public static final DeferredItem<SwordItem> SHADOW_DAGGERS = weapon("shadow_daggers", A, Melee.DAGGER, 0.5f,
            new Item.Properties().fireResistant(), GearInfo.of(EPIC, 12).bonus(CRIT_CHANCE, 0.08).bonus(SNEAK_ATTACK, 0.5).ability("item_shadow_evasion"));
    public static final DeferredItem<com.alvorecer.item.HeartOfDawnItem> HEART_OF_DAWN = gear("heart_of_dawn",
            () -> new com.alvorecer.item.HeartOfDawnItem(new Item.Properties().stacksTo(1).fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC)),
            GearInfo.of(LEGENDARY, com.alvorecer.item.HeartOfDawnItem.LEVEL).bonus(HEALING, 0.15).bonus(MAGIC_RESIST, 0.10).ability("item_dawn_heart").build());
    public static final DeferredItem<ElytraItem> PRISMATIC_WINGS = gear("prismatic_wings",
            () -> new ElytraItem(new Item.Properties().durability(864).rarity(net.minecraft.world.item.Rarity.EPIC)),
            GearInfo.of(EPIC, 15).build());

    // ------------------------------------------------------------ consumíveis

    public static final DeferredItem<ElixirItem> MANA_ELIXIR = ITEMS.register("mana_elixir",
            () -> new ElixirItem(new Item.Properties().stacksTo(16), ElixirItem.Kind.MANA));
    public static final DeferredItem<ElixirItem> VIGOR_ELIXIR = ITEMS.register("vigor_elixir",
            () -> new ElixirItem(new Item.Properties().stacksTo(16), ElixirItem.Kind.VIGOR));
    public static final DeferredItem<ElixirItem> HEALING_SALVE = ITEMS.register("healing_salve",
            () -> new ElixirItem(new Item.Properties().stacksTo(16), ElixirItem.Kind.SALVE));
    public static final DeferredItem<Item> ADVENTURER_STEW = ITEMS.register("adventurer_stew",
            () -> new Item(new Item.Properties().stacksTo(1).food(new FoodProperties.Builder().nutrition(8).saturationModifier(1.0f)
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 200, 0), 1.0f).usingConvertsTo(Items.BOWL).build())));

    private ModGear() {
    }

    /** Força o carregamento da classe durante a construção do mod (registra os itens nos DeferredRegisters). */
    public static void init() {
    }

    public static List<DeferredItem<? extends Item>> bows() {
        return List.of(LUNAR_BOW, AURORITE_BOW);
    }

    public static List<DeferredItem<? extends Item>> crossbows() {
        return List.of(LUNAR_CROSSBOW, AURORITE_CROSSBOW);
    }

    public static List<DeferredItem<? extends Item>> shields() {
        return List.of(LUNAR_SHIELD, AURORITE_SHIELD);
    }
}
