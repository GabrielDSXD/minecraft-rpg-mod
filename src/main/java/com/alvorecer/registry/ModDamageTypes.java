package com.alvorecer.registry;

import com.alvorecer.Alvorecer;
import com.alvorecer.rpg.Element;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

/** Tipos de dano mágico (definidos em data/alvorecer/damage_type). */
public final class ModDamageTypes {
    public static final ResourceKey<DamageType> ARCANE = key("arcane");
    public static final ResourceKey<DamageType> FIRE = key("fire");
    public static final ResourceKey<DamageType> COLD = key("cold");
    public static final ResourceKey<DamageType> LIGHTNING = key("lightning");
    public static final ResourceKey<DamageType> ACID = key("acid");
    public static final ResourceKey<DamageType> RADIANT = key("radiant");
    public static final ResourceKey<DamageType> NECROTIC = key("necrotic");
    public static final ResourceKey<DamageType> NATURE = key("nature");
    public static final ResourceKey<DamageType> PSYCHIC = key("psychic");

    private ModDamageTypes() {
    }

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Alvorecer.MODID, name));
    }

    public static ResourceKey<DamageType> forElement(Element element) {
        return switch (element) {
            case FIRE -> FIRE;
            case COLD -> COLD;
            case LIGHTNING -> LIGHTNING;
            case ACID -> ACID;
        };
    }

    /** Classifica qualquer fonte de dano (vanilla ou do mod) em um elemento, se houver. */
    @Nullable
    public static Element elementOf(DamageSource source) {
        if (source.is(FIRE) || source.is(DamageTypeTags.IS_FIRE)) return Element.FIRE;
        if (source.is(COLD) || source.is(DamageTypeTags.IS_FREEZING)) return Element.COLD;
        if (source.is(LIGHTNING) || source.is(DamageTypeTags.IS_LIGHTNING)) return Element.LIGHTNING;
        if (source.is(ACID) || source.is(Tags.DamageTypes.IS_POISON)) return Element.ACID;
        return null;
    }

    public static boolean isRadiant(DamageSource source) {
        return source.is(RADIANT);
    }

    public static boolean isPoison(DamageSource source) {
        return source.is(Tags.DamageTypes.IS_POISON) || source.is(ACID);
    }

    public static boolean isMagic(DamageSource source) {
        return source.is(Tags.DamageTypes.IS_MAGIC);
    }
}
