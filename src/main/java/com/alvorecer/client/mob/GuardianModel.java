package com.alvorecer.client.mob;

import com.alvorecer.entity.boss.DawnGuardianEntity;
import com.alvorecer.entity.boss.DawnGuardianEntity.Attack;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Guardião do Primeiro Alvorecer: colosso de pedra dourada com auréola giratória e núcleo no peito (3,6 blocos).
 * UV 128×128, iguais às de tools/gen_boss.py. Animações procedurais pelo ataque sincronizado.
 */
public class GuardianModel extends HierarchicalModel<DawnGuardianEntity> {
    public static final ModelLayerLocation LAYER = MobModels.layer("dawn_guardian");
    private final ModelPart root, body, head, halo, rightArm, leftArm, rightLeg, leftLeg;

    public GuardianModel(ModelPart root) {
        this.root = root;
        body = root.getChild("body");
        head = body.getChild("head");
        halo = head.getChild("halo");
        rightArm = body.getChild("right_arm");
        leftArm = body.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition layer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-10, -24, -6, 20, 24, 12)
                .texOffs(104, 0).addBox(-3, -18, -7, 6, 6, 2), PartPose.offset(0, 2, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(64, 0).addBox(-5, -10, -5, 10, 10, 10)
                .texOffs(64, 24).addBox(-6, -12, -1, 12, 2, 2), PartPose.offset(0, -24, 0));
        head.addOrReplaceChild("halo", CubeListBuilder.create()
                .texOffs(0, 104).addBox(-9, -9, 0, 18, 2, 1).addBox(-9, 7, 0, 18, 2, 1)
                .texOffs(40, 104).addBox(-9, -7, 0, 2, 14, 1).addBox(7, -7, 0, 2, 14, 1), PartPose.offset(0, -6, 7));
        body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 36).addBox(-4, -2, -4, 8, 30, 8)
                .texOffs(64, 30).addBox(-5, 20, -5, 10, 9, 10), PartPose.offset(-14, -21, 0));
        body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 36).mirror().addBox(-4, -2, -4, 8, 30, 8)
                .texOffs(64, 30).mirror().addBox(-5, 20, -5, 10, 9, 10), PartPose.offset(14, -21, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(32, 36).addBox(-4, 0, -4, 8, 22, 8), PartPose.offset(-5, 2, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(32, 36).mirror().addBox(-4, 0, -4, 8, 22, 8), PartPose.offset(5, 2, 0));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(DawnGuardianEntity e, float limbSwing, float amount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        float swing = Mth.cos(limbSwing * 0.35f) * amount * 0.8f;
        rightLeg.xRot = swing;
        leftLeg.xRot = -swing;
        rightArm.xRot = -swing * 0.5f + Mth.sin(age * 0.05f) * 0.05f;
        leftArm.xRot = swing * 0.5f - Mth.sin(age * 0.05f) * 0.05f;
        body.y += Mth.sin(age * 0.08f) * 0.4f;
        head.yRot = yaw * Mth.DEG_TO_RAD;
        head.xRot = pitch * Mth.DEG_TO_RAD;
        halo.zRot = age * 0.04f;
        int t = e.attackTicks();
        switch (e.attack()) {
            case SLAM -> {
                float k = Math.min(1f, t / (float) DawnGuardianEntity.SLAM_WINDUP);
                float raise = t < DawnGuardianEntity.SLAM_WINDUP ? -Mth.PI * 0.95f * k : -0.3f;
                rightArm.xRot = raise;
                leftArm.xRot = raise;
                body.xRot = t < DawnGuardianEntity.SLAM_WINDUP ? -0.15f * k : 0.35f;
            }
            case BEAM -> {
                rightArm.xRot = -1.3f;
                leftArm.xRot = -1.3f;
                rightArm.zRot = 0.25f;
                leftArm.zRot = -0.25f;
                halo.zRot = age * 0.3f;
            }
            case RING -> {
                float k = Math.min(1f, t / (float) DawnGuardianEntity.RING_WINDUP);
                rightArm.zRot = 1.3f * k;
                leftArm.zRot = -1.3f * k;
            }
            case SUMMON -> {
                rightArm.xRot = -2.6f;
                leftArm.xRot = -2.6f;
                halo.zRot = age * 0.2f;
            }
            case TRANSITION -> {
                float shake = Mth.sin(age * 1.7f) * 0.08f;
                rightArm.xRot = -2.9f + shake;
                leftArm.xRot = -2.9f - shake;
                body.xRot = -0.2f;
                head.xRot = -0.6f;
            }
            case DISCHARGE -> {
                rightArm.zRot = 2.2f;
                leftArm.zRot = -2.2f;
                rightArm.xRot = -0.4f;
                leftArm.xRot = -0.4f;
                rightLeg.xRot = 0.3f;
                leftLeg.xRot = 0.3f;
                halo.zRot = age * (0.2f + t * 0.01f);
            }
            default -> {
            }
        }
    }

    /** Brilho do núcleo e da auréola cresce durante a Descarga (usado pelo renderizador). */
    public static boolean charging(DawnGuardianEntity e) {
        return e.attack() == Attack.DISCHARGE || e.attack() == Attack.TRANSITION;
    }
}
