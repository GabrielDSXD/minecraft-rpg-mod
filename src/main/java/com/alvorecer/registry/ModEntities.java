package com.alvorecer.registry;

import com.alvorecer.Alvorecer;
import com.alvorecer.entity.IllusionEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Alvorecer.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<IllusionEntity>> ILLUSION = ENTITIES.register("illusion",
            () -> EntityType.Builder.<IllusionEntity>of(IllusionEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(8)
                    .noSummon()
                    .build("illusion"));

    private ModEntities() {
    }

    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(ILLUSION.get(), IllusionEntity.createAttributes().build());
    }
}
