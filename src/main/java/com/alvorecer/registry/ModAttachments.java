package com.alvorecer.registry;

import com.alvorecer.Alvorecer;
import com.alvorecer.rpg.PlayerRpgData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import com.alvorecer.network.ModNetwork;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Alvorecer.MODID);

    /** Ficha completa enviada só ao próprio jogador (NBT dentro do pacote). */
    private static final StreamCodec<RegistryFriendlyByteBuf, PlayerRpgData> SYNC_CODEC = StreamCodec.of(
            (buf, data) -> ByteBufCodecs.COMPOUND_TAG.encode(buf, data.serializeNBT(buf.registryAccess())),
            buf -> {
                PlayerRpgData data = new PlayerRpgData();
                data.deserializeNBT(buf.registryAccess(), ByteBufCodecs.COMPOUND_TAG.decode(buf));
                return data;
            });

    public static final Supplier<AttachmentType<PlayerRpgData>> RPG = ATTACHMENTS.register("rpg",
            () -> AttachmentType.serializable(PlayerRpgData::new)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to && hasMod(to), SYNC_CODEC)
                    .build());

    private ModAttachments() {
    }

    /** Só envia a clientes que negociaram os canais do mod (jogadores simulados de testes não têm). */
    private static boolean hasMod(ServerPlayer to) {
        return to.connection != null && to.connection.hasChannel(ModNetwork.OpenScreen.TYPE);
    }

    public static PlayerRpgData get(Player player) {
        return player.getData(RPG);
    }
}
