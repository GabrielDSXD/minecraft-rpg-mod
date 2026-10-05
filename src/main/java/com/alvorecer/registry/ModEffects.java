package com.alvorecer.registry;

import com.alvorecer.Alvorecer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Efeitos das habilidades. Os ícones aparecem no HUD vanilla (indicação visual de efeitos ativos).
 * Efeitos sem modificadores de atributo têm a lógica nos manipuladores de combate/tick.
 */
public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Alvorecer.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> DETERMINATION = EFFECTS.register("determination",
            () -> new RpgEffect(MobEffectCategory.BENEFICIAL, 0xF2C94C));
    public static final DeferredHolder<MobEffect, MobEffect> STONESKIN = EFFECTS.register("stoneskin",
            () -> new RpgEffect(MobEffectCategory.BENEFICIAL, 0x8A8A8A)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, id("stoneskin_speed"), -0.15, Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, id("stoneskin_kb"), 0.5, Operation.ADD_VALUE));
    public static final DeferredHolder<MobEffect, MobEffect> EVASION = EFFECTS.register("evasion",
            () -> new RpgEffect(MobEffectCategory.BENEFICIAL, 0x9BE7C4));
    public static final DeferredHolder<MobEffect, MobEffect> ORC_FURY = EFFECTS.register("orc_fury",
            () -> new RpgEffect(MobEffectCategory.BENEFICIAL, 0xB33A3A)
                    .addAttributeModifier(Attributes.ATTACK_SPEED, id("orc_fury_speed"), 0.2, Operation.ADD_MULTIPLIED_TOTAL));
    public static final DeferredHolder<MobEffect, MobEffect> BARBARIAN_RAGE = EFFECTS.register("barbarian_rage",
            () -> new RpgEffect(MobEffectCategory.BENEFICIAL, 0xD9534F));
    public static final DeferredHolder<MobEffect, MobEffect> UNDYING = EFFECTS.register("undying",
            () -> new RpgEffect(MobEffectCategory.BENEFICIAL, 0x7A0000));
    public static final DeferredHolder<MobEffect, MobEffect> POISON_BLADES = EFFECTS.register("poison_blades",
            () -> new RpgEffect(MobEffectCategory.BENEFICIAL, 0x4E9A06));
    public static final DeferredHolder<MobEffect, MobEffect> BEAR_FORM = EFFECTS.register("bear_form",
            () -> new RpgEffect(MobEffectCategory.BENEFICIAL, 0x6B4423)
                    .addAttributeModifier(Attributes.MAX_HEALTH, id("bear_health"), 8, Operation.ADD_VALUE)
                    .addAttributeModifier(Attributes.ARMOR, id("bear_armor"), 4, Operation.ADD_VALUE)
                    .addAttributeModifier(Attributes.ATTACK_DAMAGE, id("bear_damage"), 3, Operation.ADD_VALUE)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, id("bear_speed"), -0.1, Operation.ADD_MULTIPLIED_TOTAL));
    public static final DeferredHolder<MobEffect, MobEffect> HOLY_AURA = EFFECTS.register("holy_aura",
            () -> new RpgEffect(MobEffectCategory.BENEFICIAL, 0xFFF3B0));
    public static final DeferredHolder<MobEffect, MobEffect> DAWN_AVATAR = EFFECTS.register("dawn_avatar",
            () -> new RpgEffect(MobEffectCategory.BENEFICIAL, 0xFFD166));
    public static final DeferredHolder<MobEffect, MobEffect> STUN = EFFECTS.register("stun",
            () -> new RpgEffect(MobEffectCategory.HARMFUL, 0xFFFF66)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, id("stun_speed"), -1.0, Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.JUMP_STRENGTH, id("stun_jump"), -1.0, Operation.ADD_MULTIPLIED_TOTAL));
    public static final DeferredHolder<MobEffect, MobEffect> ROOT = EFFECTS.register("root",
            () -> new RpgEffect(MobEffectCategory.HARMFUL, 0x2E7D32)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, id("root_speed"), -1.0, Operation.ADD_MULTIPLIED_TOTAL));
    public static final DeferredHolder<MobEffect, MobEffect> CHARM = EFFECTS.register("charm",
            () -> new RpgEffect(MobEffectCategory.HARMFUL, 0xF48FB1));

    private ModEffects() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Alvorecer.MODID, "effect." + path);
    }

    /** MobEffect tem construtor protegido; esta subclasse só o expõe. */
    public static class RpgEffect extends MobEffect {
        public RpgEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
