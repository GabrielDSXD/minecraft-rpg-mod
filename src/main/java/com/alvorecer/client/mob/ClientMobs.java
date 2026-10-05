package com.alvorecer.client.mob;

import com.alvorecer.Alvorecer;
import com.alvorecer.entity.mob.ArcaneWolfEntity;
import com.alvorecer.entity.mob.ElementalEntity;
import com.alvorecer.entity.mob.FairyEntity;
import com.alvorecer.entity.mob.GoblinEntity;
import com.alvorecer.entity.mob.SkeletonWarriorEntity;
import com.alvorecer.entity.mob.StoneSentinelEntity;
import com.alvorecer.entity.mob.WyrmlingEntity;
import com.alvorecer.entity.npc.NpcEntity;
import com.alvorecer.registry.ModMobs;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.util.function.Function;

/** Registro de camadas de modelo e renderizadores das criaturas e NPCs. */
public final class ClientMobs {
    private ClientMobs() {
    }

    public static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(Alvorecer.MODID, "textures/entity/" + name + ".png");
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientMobs::layers);
        modBus.addListener(ClientMobs::renderers);
    }

    private static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MobModels.GOBLIN, MobModels::goblinLayer);
        event.registerLayerDefinition(MobModels.SKELETON_WARRIOR, MobModels::skeletonWarriorLayer);
        event.registerLayerDefinition(MobModels.ARCANE_WOLF, MobModels::arcaneWolfLayer);
        event.registerLayerDefinition(MobModels.ELEMENTAL, MobModels::elementalLayer);
        event.registerLayerDefinition(MobModels.STONE_SENTINEL, MobModels::stoneSentinelLayer);
        event.registerLayerDefinition(MobModels.FAIRY, MobModels::fairyLayer);
        event.registerLayerDefinition(MobModels.WYRMLING, MobModels::wyrmlingLayer);
        event.registerLayerDefinition(MobModels.NPC, MobModels::npcLayer);
        event.registerLayerDefinition(GuardianModel.LAYER, GuardianModel::layer);
    }

    /** Camada com brilho próprio (olhos, runas, núcleo), desenhada em brilho total. */
    static final class GlowLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
        private final Function<T, ResourceLocation> texture;

        GlowLayer(RenderLayerParent<T, M> parent, Function<T, ResourceLocation> texture) {
            super(parent);
            this.texture = texture;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float amount,
                           float partialTick, float age, float yaw, float pitch) {
            getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(texture.apply(entity))), 0xF000F0, OverlayTexture.NO_OVERLAY);
        }
    }

    /** Renderizador simples com textura fixa e camada de brilho opcional. */
    static class SimpleRenderer<T extends Mob, M extends EntityModel<T>> extends MobRenderer<T, M> {
        private final Function<T, ResourceLocation> texture;

        SimpleRenderer(EntityRendererProvider.Context ctx, M model, float shadow, Function<T, ResourceLocation> texture,
                       Function<T, ResourceLocation> glow) {
            super(ctx, model, shadow);
            this.texture = texture;
            if (glow != null) addLayer(new GlowLayer<>(this, glow));
        }

        @Override
        public ResourceLocation getTextureLocation(T entity) {
            return texture.apply(entity);
        }
    }

    static class ScaledHumanoid<T extends Mob, M extends net.minecraft.client.model.HumanoidModel<T>> extends HumanoidMobRenderer<T, M> {
        private final Function<T, ResourceLocation> texture;

        ScaledHumanoid(EntityRendererProvider.Context ctx, M model, float shadow, float scale, Function<T, ResourceLocation> texture) {
            super(ctx, model, shadow, scale, scale, scale);
            this.texture = texture;
        }

        @Override
        public ResourceLocation getTextureLocation(T entity) {
            return texture.apply(entity);
        }
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        ResourceLocation goblin = tex("goblin"), skeleton = tex("skeleton_warrior"), wolf = tex("arcane_wolf"),
                wolfEyes = tex("arcane_wolf_eyes"), fire = tex("elemental_fire"), frost = tex("elemental_frost"),
                sentinel = tex("stone_sentinel"), sentinelGlow = tex("stone_sentinel_glow"), fairy = tex("fairy"), wyrm = tex("wyrmling");
        event.registerEntityRenderer(ModMobs.GOBLIN.get(), ctx -> new ScaledHumanoid<GoblinEntity, MobModels.GoblinModel>(
                ctx, new MobModels.GoblinModel(ctx.bakeLayer(MobModels.GOBLIN)), 0.35f, 0.68f, e -> goblin));
        event.registerEntityRenderer(ModMobs.SKELETON_WARRIOR.get(), ctx -> new ScaledHumanoid<SkeletonWarriorEntity, MobModels.SkeletonWarriorModel>(
                ctx, new MobModels.SkeletonWarriorModel(ctx.bakeLayer(MobModels.SKELETON_WARRIOR)), 0.5f, 1f, e -> skeleton));
        event.registerEntityRenderer(ModMobs.ARCANE_WOLF.get(), ctx -> new SimpleRenderer<ArcaneWolfEntity, MobModels.ArcaneWolfModel>(
                ctx, new MobModels.ArcaneWolfModel(ctx.bakeLayer(MobModels.ARCANE_WOLF)), 0.5f, e -> wolf, e -> wolfEyes));
        event.registerEntityRenderer(ModMobs.ELEMENTAL.get(), ctx -> new SimpleRenderer<ElementalEntity, MobModels.ElementalModel>(
                ctx, new MobModels.ElementalModel(ctx.bakeLayer(MobModels.ELEMENTAL)), 0.4f, e -> e.isFrost() ? frost : fire, e -> e.isFrost() ? frost : fire));
        event.registerEntityRenderer(ModMobs.STONE_SENTINEL.get(), ctx -> new SimpleRenderer<StoneSentinelEntity, MobModels.StoneSentinelModel>(
                ctx, new MobModels.StoneSentinelModel(ctx.bakeLayer(MobModels.STONE_SENTINEL)), 1.0f, e -> sentinel, e -> sentinelGlow));
        event.registerEntityRenderer(ModMobs.FAIRY.get(), ctx -> new SimpleRenderer<FairyEntity, MobModels.FairyModel>(
                ctx, new MobModels.FairyModel(ctx.bakeLayer(MobModels.FAIRY)), 0.15f, e -> fairy, e -> fairy));
        event.registerEntityRenderer(ModMobs.WYRMLING.get(), ctx -> new SimpleRenderer<WyrmlingEntity, MobModels.WyrmlingModel>(
                ctx, new MobModels.WyrmlingModel(ctx.bakeLayer(MobModels.WYRMLING)), 0.6f, e -> wyrm, null));
        event.registerEntityRenderer(ModMobs.ELEMENTAL_BOLT.get(), NoopRenderer::new);
        ResourceLocation guardian = tex("dawn_guardian"), guardianGlow = tex("dawn_guardian_glow"), guardianCharge = tex("dawn_guardian_charge");
        event.registerEntityRenderer(ModMobs.DAWN_GUARDIAN.get(), ctx -> new SimpleRenderer<com.alvorecer.entity.boss.DawnGuardianEntity, GuardianModel>(
                ctx, new GuardianModel(ctx.bakeLayer(GuardianModel.LAYER)), 1.4f, e -> guardian, e -> GuardianModel.charging(e) ? guardianCharge : guardianGlow));
        for (var npc : new java.util.function.Supplier[]{ModMobs.GUILD_MASTER, ModMobs.MERCHANT, ModMobs.SAGE}) {
            @SuppressWarnings("unchecked") var type = (net.minecraft.world.entity.EntityType<NpcEntity>) npc.get();
            event.registerEntityRenderer(type, ctx -> new ScaledHumanoid<NpcEntity, MobModels.NpcModel>(
                    ctx, new MobModels.NpcModel(ctx.bakeLayer(MobModels.NPC)), 0.5f, 1f, e -> tex("npc_" + e.role().id())));
        }
    }
}
