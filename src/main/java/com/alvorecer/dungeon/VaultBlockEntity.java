package com.alvorecer.dungeon;

import com.alvorecer.Alvorecer;
import com.alvorecer.registry.ModDungeon;
import com.alvorecer.registry.ModWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VaultBlockEntity extends BlockEntity {
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(Alvorecer.MODID, "dungeon/vault"));
    public static final ResourceKey<LootTable> LOOT_AETHER = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(Alvorecer.MODID, "dungeon/vault_aether"));

    private final Set<UUID> claimed = new HashSet<>();

    public VaultBlockEntity(BlockPos pos, BlockState state) {
        super(ModDungeon.VAULT_BE.get(), pos, state);
    }

    public boolean hasClaimed(UUID player) {
        return claimed.contains(player);
    }

    /** @return true se o jogador recebeu a parte dele agora */
    public boolean claim(ServerPlayer p) {
        ServerLevel level = (ServerLevel) this.level;
        if (level == null) return false;
        if (!claimed.add(p.getUUID())) {
            p.displayClientMessage(Component.translatable("msg.alvorecer.vault_claimed").withStyle(ChatFormatting.GRAY), true);
            level.playSound(null, worldPosition, SoundEvents.VAULT_REJECT_REWARDED_PLAYER, SoundSource.BLOCKS, 1f, 1f);
            return false;
        }
        LootTable table = level.getServer().reloadableRegistries().getLootTable(level.dimension() == ModWorld.AETHER ? LOOT_AETHER : LOOT);
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(worldPosition))
                .withParameter(LootContextParams.THIS_ENTITY, p).withLuck(p.getLuck()).create(LootContextParamSets.CHEST);
        for (ItemStack stack : table.getRandomItems(params)) p.getInventory().placeItemBackInInventory(stack);
        level.playSound(null, worldPosition, SoundEvents.VAULT_OPEN_SHUTTER, SoundSource.BLOCKS, 1f, 1f);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5, 12, 0.4, 0.3, 0.4, 0);
        p.displayClientMessage(Component.translatable("msg.alvorecer.vault_reward").withStyle(ChatFormatting.GOLD), true);
        setChanged();
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (UUID id : claimed) list.add(NbtUtils.createUUID(id));
        tag.put("Claimed", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        claimed.clear();
        for (Tag t : tag.getList("Claimed", Tag.TAG_INT_ARRAY)) claimed.add(NbtUtils.loadUUID(t));
    }
}
