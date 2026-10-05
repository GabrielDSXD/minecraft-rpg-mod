package com.alvorecer.client;

import com.alvorecer.entity.IllusionEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;

/** Desenha a ilusão com a pele do jogador que a conjurou (modelo largo ou fino conforme a pele). */
public class IllusionRenderer extends LivingEntityRenderer<IllusionEntity, PlayerModel<IllusionEntity>> {
    private final PlayerModel<IllusionEntity> wide;
    private final PlayerModel<IllusionEntity> slim;

    public IllusionRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        wide = model;
        slim = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), ctx.getModelManager()));
        addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
    }

    private PlayerSkin skin(IllusionEntity e) {
        var owner = e.getOwnerId();
        if (owner.isEmpty()) return DefaultPlayerSkin.get(e.getUUID());
        var connection = Minecraft.getInstance().getConnection();
        PlayerInfo info = connection == null ? null : connection.getPlayerInfo(owner.get());
        return info != null ? info.getSkin() : DefaultPlayerSkin.get(owner.get());
    }

    @Override
    public void render(IllusionEntity entity, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        model = skin(entity).model() == PlayerSkin.Model.SLIM ? slim : wide;
        super.render(entity, yaw, partialTicks, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(IllusionEntity entity) {
        return skin(entity).texture();
    }

    @Override
    protected boolean shouldShowName(IllusionEntity entity) {
        return false;
    }
}
