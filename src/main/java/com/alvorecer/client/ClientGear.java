package com.alvorecer.client;

import com.alvorecer.Alvorecer;
import com.alvorecer.ability.Abilities;
import com.alvorecer.ability.Ability;
import com.alvorecer.item.gear.CasterItem;
import com.alvorecer.item.gear.Equipment;
import com.alvorecer.item.gear.GearInfo;
import com.alvorecer.registry.ModAttachments;
import com.alvorecer.registry.ModGear;
import com.alvorecer.rpg.PlayerRpgData;
import com.alvorecer.rpg.talent.Talent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.joml.Vector3f;

import java.util.List;

/** Cliente da Etapa 3: tooltips de equipamento, modelos animados de arco/besta/escudo, escudos, asas e rastros. */
public final class ClientGear {
    private ClientGear() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientGear::setup);
        modBus.addListener(ClientGear::extensions);
        modBus.addListener(ClientGear::layers);
        NeoForge.EVENT_BUS.addListener(ClientGear::tooltip);
        NeoForge.EVENT_BUS.addListener(ClientGear::tick);
    }

    private static ResourceLocation mc(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    private static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (var bow : ModGear.bows()) {
                Item item = bow.get();
                ItemProperties.register(item, mc("pull"), (stack, level, e, seed) -> e == null || e.getUseItem() != stack ? 0
                        : (stack.getUseDuration(e) - e.getUseItemRemainingTicks()) / 20.0f);
                ItemProperties.register(item, mc("pulling"), (stack, level, e, seed) -> e != null && e.isUsingItem() && e.getUseItem() == stack ? 1 : 0);
            }
            for (var cb : ModGear.crossbows()) {
                Item item = cb.get();
                ItemProperties.register(item, mc("pull"), (stack, level, e, seed) -> e == null || CrossbowItem.isCharged(stack) ? 0
                        : (stack.getUseDuration(e) - e.getUseItemRemainingTicks()) / (float) CrossbowItem.getChargeDuration(stack, e));
                ItemProperties.register(item, mc("pulling"), (stack, level, e, seed) -> e != null && e.isUsingItem()
                        && e.getUseItem() == stack && !CrossbowItem.isCharged(stack) ? 1 : 0);
                ItemProperties.register(item, mc("charged"), (stack, level, e, seed) -> CrossbowItem.isCharged(stack) ? 1 : 0);
                ItemProperties.register(item, mc("firework"), (stack, level, e, seed) -> {
                    ChargedProjectiles p = stack.get(DataComponents.CHARGED_PROJECTILES);
                    return p != null && p.contains(Items.FIREWORK_ROCKET) ? 1 : 0;
                });
            }
            for (var sh : ModGear.shields()) {
                ItemProperties.register(sh.get(), mc("blocking"), (stack, level, e, seed) -> e != null && e.isUsingItem() && e.getUseItem() == stack ? 1 : 0);
            }
            ItemProperties.register(ModGear.PRISMATIC_WINGS.get(), mc("broken"), (stack, level, e, seed) -> ElytraItem.isFlyEnabled(stack) ? 0 : 1);
        });
    }

    // ------------------------------------------------------------ escudos

    private static final class ShieldRenderer extends BlockEntityWithoutLevelRenderer {
        private ShieldModel model;

        ShieldRenderer() {
            super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            if (model == null) model = new ShieldModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.SHIELD));
            ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(Alvorecer.MODID, "textures/entity/shield/" + id.getPath() + ".png");
            pose.pushPose();
            pose.scale(1, -1, -1);
            VertexConsumer vc = ItemRenderer.getFoilBufferDirect(buffers, model.renderType(tex), true, stack.hasFoil());
            model.handle().render(pose, vc, light, overlay);
            model.plate().render(pose, vc, light, overlay);
            pose.popPose();
        }
    }

    private static void extensions(RegisterClientExtensionsEvent event) {
        ShieldRenderer renderer = new ShieldRenderer();
        IClientItemExtensions ext = new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        };
        for (var sh : ModGear.shields()) event.registerItem(ext, sh.get());
    }

    // ------------------------------------------------------------ asas

    private static final class WingsLayer<T extends LivingEntity, M extends EntityModel<T>> extends ElytraLayer<T, M> {
        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Alvorecer.MODID, "textures/entity/prismatic_wings.png");

        WingsLayer(RenderLayerParent<T, M> parent, EntityModelSet models) {
            super(parent, models);
        }

        @Override
        public boolean shouldRender(ItemStack stack, T entity) {
            return stack.is(ModGear.PRISMATIC_WINGS.get());
        }

        @Override
        public ResourceLocation getElytraTexture(ItemStack stack, T entity) {
            return TEXTURE;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void layers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            LivingEntityRenderer renderer = event.getSkin(skin);
            if (renderer != null) renderer.addLayer(new WingsLayer(renderer, event.getEntityModels()));
        }
    }

    // ------------------------------------------------------------ tooltip

    private static void tooltip(ItemTooltipEvent event) {
        GearInfo g = GearInfo.get(event.getItemStack().getItem());
        if (g == null) return;
        List<Component> tip = event.getToolTip();
        Player player = event.getEntity();
        PlayerRpgData d = player == null ? null : ModAttachments.get(player);
        if (!tip.isEmpty()) tip.set(0, tip.get(0).copy().withStyle(g.rarity().color));
        int i = Math.min(1, tip.size());
        tip.add(i++, Component.translatable(g.rarity().langKey()).withStyle(g.rarity().color, ChatFormatting.ITALIC));
        if (g.category() != null) tip.add(i++, Component.translatable("ui.alvorecer.gear.type", Component.translatable(g.category().langKey())).withStyle(ChatFormatting.GRAY));
        if (g.weight() != null) tip.add(i++, Component.translatable("ui.alvorecer.gear.weight", Component.translatable(g.weight().langKey())).withStyle(ChatFormatting.GRAY));
        boolean meets = d == null || Equipment.meets(d, g);
        tip.add(i++, Component.translatable("ui.alvorecer.gear.level", g.level()).withStyle(meets ? ChatFormatting.GRAY : ChatFormatting.RED));
        if (d != null && d.hasCharacter() && g.category() != null && d.rpgClass != null && !d.rpgClass.proficient(g.category())) {
            tip.add(i++, Component.translatable("ui.alvorecer.gear.not_proficient").withStyle(ChatFormatting.RED));
        }
        String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem()).getPath();
        tip.add(i++, Component.translatable("item.alvorecer." + path + ".lore").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        if (!g.bonuses().isEmpty()) {
            tip.add(i++, Component.translatable("ui.alvorecer.gear.bonuses").withStyle(ChatFormatting.GRAY));
            for (Talent.Bonus b : g.bonuses()) tip.add(i++, Component.literal(" ").append(Ui.bonusLine(b, 1)));
        }
        Equipment.SetBonus set = Equipment.set(g.set());
        if (set != null) {
            int pieces = player == null || d == null ? 0 : Equipment.setPieces(player, d, set.id());
            boolean active = pieces >= Equipment.SET_PIECES;
            tip.add(i++, Component.translatable("ui.alvorecer.gear.set", Component.translatable(set.nameKey()), pieces, Equipment.SET_PIECES)
                    .withStyle(ChatFormatting.GOLD));
            for (Talent.Bonus b : set.bonuses()) {
                MutableComponent line = Component.literal(" ").append(Ui.bonusLine(b, 1));
                tip.add(i++, active ? line : line.withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        if (g.ability() != null) {
            Ability a = Abilities.get(g.ability());
            tip.add(i++, Component.translatable("ui.alvorecer.gear.ability").append(Component.translatable(a.nameKey()).withStyle(ChatFormatting.GOLD)));
            tip.add(i++, Ui.costLine(a));
            tip.add(i++, Component.translatable(a.descKey()).withStyle(ChatFormatting.GRAY));
        }
        if (event.getItemStack().getItem() instanceof CasterItem caster && d != null) {
            String sel = caster.selected(event.getItemStack(), d);
            tip.add(i++, Component.translatable("ui.alvorecer.gear.spell").append(Component.translatable("ability.alvorecer." + sel).withStyle(ChatFormatting.AQUA)));
            tip.add(i++, Ui.costLine(Abilities.get(sel)));
            tip.add(i++, Component.translatable(caster.selectable() ? "ui.alvorecer.gear.staff_hint" : "ui.alvorecer.gear.wand_hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    // ------------------------------------------------------------ tick: bônus locais e rastros visuais

    private static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.isPaused()) return;
        Equipment.refresh(mc.player);
        for (AbstractClientPlayer p : mc.level.players()) {
            if (p.isFallFlying() && p.getItemBySlot(EquipmentSlot.CHEST).is(ModGear.PRISMATIC_WINGS.get())) {
                float hue = (p.tickCount % 60) / 60f;
                int rgb = Mth.hsvToRgb(hue, 0.8f, 1f);
                var dust = new DustParticleOptions(new Vector3f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f), 1.4f);
                Vec3 back = p.position().add(p.getLookAngle().scale(-0.8)).add(0, 0.6, 0);
                for (int s : new int[]{-1, 1}) {
                    Vec3 side = new Vec3(-p.getLookAngle().z, 0, p.getLookAngle().x).normalize().scale(0.9 * s);
                    mc.level.addParticle(dust, back.x + side.x, back.y, back.z + side.z, 0, 0, 0);
                }
            }
            if (p.swinging && p.getMainHandItem().is(ModGear.DAWN_BLADE.get())) {
                float progress = p.getAttackAnim(1f);
                Vec3 look = p.getLookAngle();
                Vec3 side = new Vec3(-look.z, 0, look.x).normalize();
                double t = (progress - 0.5) * 2;
                Vec3 at = p.getEyePosition().add(look.scale(1.4)).add(side.scale(t * 1.2)).add(0, -0.3 - 0.4 * t * t, 0);
                mc.level.addParticle(ParticleTypes.END_ROD, at.x, at.y, at.z, 0, 0, 0);
                mc.level.addParticle(new DustParticleOptions(new Vector3f(1f, 0.82f, 0.35f), 1.2f), at.x, at.y, at.z, 0, 0, 0);
            }
        }
    }
}
