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
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Modelos originais das criaturas (geometria em código, texturas em tools/gen_mobs.py com as mesmas coordenadas UV)
 * e as animações procedurais: caminhada, bater de asas, órbita dos fragmentos, poses de ataque telegrafadas.
 */
public final class MobModels {
    private MobModels() {
    }

    public static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Alvorecer.MODID, name), "main");
    }

    public static final ModelLayerLocation GOBLIN = layer("goblin");
    public static final ModelLayerLocation SKELETON_WARRIOR = layer("skeleton_warrior");
    public static final ModelLayerLocation ARCANE_WOLF = layer("arcane_wolf");
    public static final ModelLayerLocation ELEMENTAL = layer("elemental");
    public static final ModelLayerLocation STONE_SENTINEL = layer("stone_sentinel");
    public static final ModelLayerLocation FAIRY = layer("fairy");
    public static final ModelLayerLocation WYRMLING = layer("wyrmling");
    public static final ModelLayerLocation NPC = layer("npc");

    // ================================================================= humanoides

    public static LayerDefinition goblinLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0);
        PartDefinition head = mesh.getRoot().getChild("head");
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(56, 16).addBox(-7, -6, -0.5f, 3, 2, 1), PartPose.ZERO);
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(56, 20).addBox(4, -6, -0.5f, 3, 2, 1), PartPose.ZERO);
        head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(24, 0).addBox(-1, -4, -5, 2, 2, 1), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    public static class GoblinModel extends HumanoidModel<GoblinEntity> {
        public GoblinModel(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(GoblinEntity e, float limbSwing, float amount, float age, float yaw, float pitch) {
            super.setupAnim(e, limbSwing, amount, age, yaw, pitch);
            if (e.isLeaping()) {
                rightArm.xRot = leftArm.xRot = -2.6f;
                rightLeg.xRot = 0.6f;
                leftLeg.xRot = -0.6f;
            }
        }
    }

    public static LayerDefinition skeletonWarriorLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0);
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4, -8, -4, 8, 8, 8, new CubeDeformation(0.5f)), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-1, -2, -1, 2, 12, 2), PartPose.offset(-5, 2, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1, -2, -1, 2, 12, 2), PartPose.offset(5, 2, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-1, 0, -1, 2, 12, 2), PartPose.offset(-2, 12, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-1, 0, -1, 2, 12, 2), PartPose.offset(2, 12, 0));
        return LayerDefinition.create(mesh, 64, 32);
    }

    public static class SkeletonWarriorModel extends HumanoidModel<SkeletonWarriorEntity> {
        public SkeletonWarriorModel(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(SkeletonWarriorEntity e, float limbSwing, float amount, float age, float yaw, float pitch) {
            super.setupAnim(e, limbSwing, amount, age, yaw, pitch);
            switch (e.chargeState()) {
                case 1 -> { // preparando: espada erguida, escudo à frente
                    rightArm.xRot = -2.8f + Mth.sin(age * 0.8f) * 0.1f;
                    leftArm.xRot = -1.2f;
                    leftArm.yRot = 0.6f;
                }
                case 2 -> { // investida: corpo inclinado, espada apontada
                    rightArm.xRot = -1.5f;
                    leftArm.xRot = -1.0f;
                    body.xRot = 0.3f;
                }
                default -> body.xRot = 0;
            }
        }
    }

    public static LayerDefinition npcLayer() {
        return LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0), 64, 32);
    }

    public static class NpcModel extends HumanoidModel<NpcEntity> {
        public NpcModel(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(NpcEntity e, float limbSwing, float amount, float age, float yaw, float pitch) {
            super.setupAnim(e, limbSwing, amount, age, yaw, pitch);
            if (amount < 0.05f) { // gesticula parado
                rightArm.xRot = -0.25f + Mth.sin(age * 0.05f) * 0.1f;
                leftArm.xRot = -0.25f - Mth.sin(age * 0.05f) * 0.1f;
            }
        }
    }

    // ================================================================= lobo arcano

    public static LayerDefinition arcaneWolfLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -3, -5, 6, 6, 10), PartPose.offset(0, 13, 0));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(32, 0).addBox(-3, -3, -4, 6, 6, 5), PartPose.offset(0, 11, -6));
        head.addOrReplaceChild("snout", CubeListBuilder.create().texOffs(32, 11).addBox(-1.5f, 0, -7, 3, 3, 3), PartPose.ZERO);
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(54, 0).addBox(1, -5, -2, 2, 2, 1), PartPose.ZERO);
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(54, 3).addBox(-3, -5, -2, 2, 2, 1), PartPose.ZERO);
        CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 16).addBox(-1, 0, -1, 2, 8, 2);
        root.addOrReplaceChild("leg_fr", leg, PartPose.offset(-2, 16, -3.5f));
        root.addOrReplaceChild("leg_fl", leg, PartPose.offset(2, 16, -3.5f));
        root.addOrReplaceChild("leg_br", leg, PartPose.offset(-2, 16, 3.5f));
        root.addOrReplaceChild("leg_bl", leg, PartPose.offset(2, 16, 3.5f));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(8, 16).addBox(-1, 0, -1, 2, 7, 2), PartPose.offsetAndRotation(0, 11, 5, 0.9f, 0, 0));
        return LayerDefinition.create(mesh, 64, 32);
    }

    public static class ArcaneWolfModel extends HierarchicalModel<ArcaneWolfEntity> {
        private final ModelPart root, head, legFr, legFl, legBr, legBl, tail;

        public ArcaneWolfModel(ModelPart root) {
            this.root = root;
            head = root.getChild("head");
            legFr = root.getChild("leg_fr");
            legFl = root.getChild("leg_fl");
            legBr = root.getChild("leg_br");
            legBl = root.getChild("leg_bl");
            tail = root.getChild("tail");
        }

        @Override
        public ModelPart root() {
            return root;
        }

        @Override
        public void setupAnim(ArcaneWolfEntity e, float limbSwing, float amount, float age, float yaw, float pitch) {
            float swing = Mth.cos(limbSwing * 0.6662f) * 1.4f * amount;
            legFr.xRot = swing;
            legBl.xRot = swing;
            legFl.xRot = -swing;
            legBr.xRot = -swing;
            head.yRot = yaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD;
            tail.yRot = Mth.sin(age * 0.3f) * 0.4f;
            tail.xRot = 0.9f + (e.isAggressive() ? -0.5f : 0);
        }
    }

    // ================================================================= elemental

    public static LayerDefinition elementalLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -4, -4, 8, 8, 8), PartPose.offset(0, 8, 0));
        PartDefinition orbit = root.addOrReplaceChild("orbit", CubeListBuilder.create(), PartPose.offset(0, 8, 0));
        CubeListBuilder shard = CubeListBuilder.create().texOffs(32, 0).addBox(-2, -2, -2, 4, 4, 4);
        orbit.addOrReplaceChild("shard0", shard, PartPose.offset(7, 0, 0));
        orbit.addOrReplaceChild("shard1", shard, PartPose.offset(-7, 0, 0));
        orbit.addOrReplaceChild("shard2", shard, PartPose.offset(0, 0, 7));
        orbit.addOrReplaceChild("shard3", shard, PartPose.offset(0, 0, -7));
        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 16).addBox(-2, 0, -2, 4, 6, 4), PartPose.offset(0, 13, 0));
        tail.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(16, 16).addBox(-1, 0, -1, 2, 4, 2), PartPose.offset(0, 6, 0));
        return LayerDefinition.create(mesh, 64, 32);
    }

    public static class ElementalModel extends HierarchicalModel<ElementalEntity> {
        private final ModelPart root, core, orbit, tail;
        private final ModelPart[] shards = new ModelPart[4];

        public ElementalModel(ModelPart root) {
            this.root = root;
            core = root.getChild("core");
            orbit = root.getChild("orbit");
            tail = root.getChild("tail");
            for (int i = 0; i < 4; i++) shards[i] = orbit.getChild("shard" + i);
        }

        @Override
        public ModelPart root() {
            return root;
        }

        @Override
        public void setupAnim(ElementalEntity e, float limbSwing, float amount, float age, float yaw, float pitch) {
            float charge = e.pulseCharge() / 20f;
            float bob = Mth.sin(age * 0.1f) * 1.5f;
            core.y = 8 + bob;
            core.yRot = age * 0.05f;
            core.xRot = Mth.sin(age * 0.07f) * 0.2f;
            orbit.y = 8 + bob;
            orbit.yRot = age * (0.12f + charge * 0.5f);
            float radius = 7 + charge * 4;
            float[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int i = 0; i < 4; i++) {
                shards[i].x = dirs[i][0] * radius;
                shards[i].z = dirs[i][1] * radius;
                shards[i].y = Mth.sin(age * 0.2f + i) * 1.5f;
                shards[i].xRot = age * 0.1f;
            }
            tail.y = 13 + bob;
            tail.xRot = Mth.sin(age * 0.15f) * 0.15f;
        }
    }

    // ================================================================= sentinela

    public static LayerDefinition stoneSentinelLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(0, 0).addBox(-8, -20, -5, 16, 20, 10), PartPose.offset(0, 13, 0));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(64, 0).addBox(-4, -8, -4, 8, 8, 8), PartPose.offset(0, -7, 0));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(64, 16).addBox(-3, -2, -3, 6, 24, 6), PartPose.offset(-11, -5, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(64, 16).mirror().addBox(-3, -2, -3, 6, 24, 6), PartPose.offset(11, -5, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 40).addBox(-3, 0, -3, 6, 11, 6), PartPose.offset(-4, 13, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 40).mirror().addBox(-3, 0, -3, 6, 11, 6), PartPose.offset(4, 13, 0));
        return LayerDefinition.create(mesh, 128, 64);
    }

    public static class StoneSentinelModel extends HierarchicalModel<StoneSentinelEntity> {
        private final ModelPart root, head, rightArm, leftArm, rightLeg, leftLeg;

        public StoneSentinelModel(ModelPart root) {
            this.root = root;
            head = root.getChild("head");
            rightArm = root.getChild("right_arm");
            leftArm = root.getChild("left_arm");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
        }

        @Override
        public ModelPart root() {
            return root;
        }

        @Override
        public void setupAnim(StoneSentinelEntity e, float limbSwing, float amount, float age, float yaw, float pitch) {
            float swing = Mth.cos(limbSwing * 0.4f) * amount;
            rightLeg.xRot = swing;
            leftLeg.xRot = -swing;
            rightArm.xRot = -swing * 0.6f;
            leftArm.xRot = swing * 0.6f;
            head.yRot = yaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD;
            int slam = e.slamTicks();
            if (slam > 0) {
                float t = Math.min(1f, slam / (float) StoneSentinelEntity.SLAM_WINDUP);
                float raise = slam < StoneSentinelEntity.SLAM_WINDUP ? -Mth.PI * t : -0.2f;
                rightArm.xRot = raise;
                leftArm.xRot = raise;
            }
        }
    }

    // ================================================================= fada

    public static LayerDefinition fairyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-1, 0, -1, 2, 4, 2), PartPose.offset(0, 16, 0));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(8, 0).addBox(-1.5f, -3, -1.5f, 3, 3, 3), PartPose.offset(0, 16, 0));
        root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 8).addBox(0, -3, 0, 0, 6, 5), PartPose.offset(0.5f, 17, 0.5f));
        root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(10, 8).addBox(0, -3, 0, 0, 6, 5), PartPose.offset(-0.5f, 17, 0.5f));
        return LayerDefinition.create(mesh, 32, 32);
    }

    public static class FairyModel extends HierarchicalModel<FairyEntity> {
        private final ModelPart root, leftWing, rightWing, body, head;

        public FairyModel(ModelPart root) {
            super(net.minecraft.client.renderer.RenderType::entityTranslucent);
            this.root = root;
            leftWing = root.getChild("left_wing");
            rightWing = root.getChild("right_wing");
            body = root.getChild("body");
            head = root.getChild("head");
        }

        @Override
        public ModelPart root() {
            return root;
        }

        @Override
        public void setupAnim(FairyEntity e, float limbSwing, float amount, float age, float yaw, float pitch) {
            float flap = Mth.sin(age * 1.6f) * 0.7f;
            leftWing.yRot = 0.5f + flap;
            rightWing.yRot = -0.5f - flap;
            float bob = Mth.sin(age * 0.15f);
            body.y = 16 + bob;
            head.y = 16 + bob;
            leftWing.y = rightWing.y = 17 + bob;
            head.yRot = yaw * Mth.DEG_TO_RAD;
        }
    }

    // ================================================================= dragonete

    public static LayerDefinition wyrmlingLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -3, -6, 6, 5, 12), PartPose.offset(0, 14, 0));
        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(0, 17).addBox(-1.5f, -1.5f, -4, 3, 3, 4), PartPose.offset(0, -1, -6));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(14, 17).addBox(-2.5f, -2.5f, -5, 5, 4, 5)
                .texOffs(52, 17).addBox(-2, -4.5f, -1, 1, 2, 1).texOffs(56, 17).addBox(1, -4.5f, -1, 1, 2, 1), PartPose.offset(0, -0.5f, -4));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(34, 17).addBox(-2, 0, -5, 4, 1, 5), PartPose.offset(0, 1.5f, 0));
        body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 30).addBox(0, 0, 0, 12, 0, 8), PartPose.offset(3, -3, -2));
        body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 38).addBox(-12, 0, 0, 12, 0, 8), PartPose.offset(-3, -3, -2));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(36, 0).addBox(-1.5f, -1.5f, 0, 3, 3, 8), PartPose.offset(0, -1, 6));
        tail.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(36, 11).addBox(-1, -1, 0, 2, 2, 6), PartPose.offset(0, 0, 8));
        CubeListBuilder leg = CubeListBuilder.create().texOffs(52, 24).addBox(-1, 0, -1, 2, 6, 2);
        body.addOrReplaceChild("leg_fr", leg, PartPose.offset(-2.5f, 2, -4));
        body.addOrReplaceChild("leg_fl", leg, PartPose.offset(2.5f, 2, -4));
        body.addOrReplaceChild("leg_br", leg, PartPose.offset(-2.5f, 2, 4));
        body.addOrReplaceChild("leg_bl", leg, PartPose.offset(2.5f, 2, 4));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static class WyrmlingModel extends HierarchicalModel<WyrmlingEntity> {
        private final ModelPart root, body, neck, head, jaw, leftWing, rightWing, tail, tail2;

        public WyrmlingModel(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            neck = body.getChild("neck");
            head = neck.getChild("head");
            jaw = head.getChild("jaw");
            leftWing = body.getChild("left_wing");
            rightWing = body.getChild("right_wing");
            tail = body.getChild("tail");
            tail2 = tail.getChild("tail2");
        }

        @Override
        public ModelPart root() {
            return root;
        }

        @Override
        public void setupAnim(WyrmlingEntity e, float limbSwing, float amount, float age, float yaw, float pitch) {
            float flap = Mth.sin(age * 0.45f) * 0.7f;
            leftWing.zRot = flap;
            rightWing.zRot = -flap;
            body.y = 14 + Mth.sin(age * 0.45f + 1) * 1.2f;
            neck.yRot = yaw * Mth.DEG_TO_RAD * 0.5f;
            head.yRot = yaw * Mth.DEG_TO_RAD * 0.5f;
            head.xRot = pitch * Mth.DEG_TO_RAD;
            jaw.xRot = e.isBreathing() ? 0.6f + Mth.sin(age * 1.2f) * 0.1f : 0.05f;
            tail.yRot = Mth.sin(age * 0.2f) * 0.3f;
            tail2.yRot = Mth.sin(age * 0.2f - 1) * 0.4f;
        }
    }
}
