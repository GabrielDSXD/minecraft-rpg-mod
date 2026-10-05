package com.alvorecer.network;

import com.alvorecer.Alvorecer;
import com.alvorecer.ability.AbilityService;
import com.alvorecer.config.RpgConfig;
import com.alvorecer.registry.ModAttachments;
import com.alvorecer.registry.ModItems;
import com.alvorecer.rpg.Derived;
import com.alvorecer.rpg.Element;
import com.alvorecer.rpg.PlayerRpgData;
import com.alvorecer.rpg.Profession;
import com.alvorecer.rpg.Progression;
import com.alvorecer.rpg.Race;
import com.alvorecer.rpg.RpgClass;
import com.alvorecer.rpg.Stat;
import com.alvorecer.rpg.StatApplier;
import io.netty.buffer.ByteBuf;
import com.alvorecer.achievement.Achievements;
import com.alvorecer.quest.Quests;
import com.alvorecer.rpg.talent.Talents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Pacotes do mod. Todos os C2S são validados no servidor; o cliente nunca decide valores da ficha. */
public final class ModNetwork {
    private ModNetwork() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> typeOf(String name) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Alvorecer.MODID, name));
    }

    // ------------------------------------------------------------ S2C

    public enum Screen { CREATION, SHEET, CODEX, QUEST_BOARD, TALENTS, WORKBENCH }

    public record OpenScreen(int screen) implements CustomPacketPayload {
        public static final Type<OpenScreen> TYPE = typeOf("open_screen");
        public static final StreamCodec<ByteBuf, OpenScreen> CODEC = ByteBufCodecs.VAR_INT.map(OpenScreen::new, OpenScreen::screen);

        @Override
        public Type<OpenScreen> type() {
            return TYPE;
        }
    }

    // ------------------------------------------------------------ C2S

    public record CreateCharacter(int race, int rpgClass, int profession, int element) implements CustomPacketPayload {
        public static final Type<CreateCharacter> TYPE = typeOf("create_character");
        public static final StreamCodec<ByteBuf, CreateCharacter> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, CreateCharacter::race,
                ByteBufCodecs.VAR_INT, CreateCharacter::rpgClass,
                ByteBufCodecs.VAR_INT, CreateCharacter::profession,
                ByteBufCodecs.VAR_INT, CreateCharacter::element,
                CreateCharacter::new);

        @Override
        public Type<CreateCharacter> type() {
            return TYPE;
        }
    }

    public record UseAbility(int slot) implements CustomPacketPayload {
        public static final Type<UseAbility> TYPE = typeOf("use_ability");
        public static final StreamCodec<ByteBuf, UseAbility> CODEC = ByteBufCodecs.VAR_INT.map(UseAbility::new, UseAbility::slot);

        @Override
        public Type<UseAbility> type() {
            return TYPE;
        }
    }

    public record AllocateStat(int stat) implements CustomPacketPayload {
        public static final Type<AllocateStat> TYPE = typeOf("allocate_stat");
        public static final StreamCodec<ByteBuf, AllocateStat> CODEC = ByteBufCodecs.VAR_INT.map(AllocateStat::new, AllocateStat::stat);

        @Override
        public Type<AllocateStat> type() {
            return TYPE;
        }
    }

    public record ChooseSecondary(int profession) implements CustomPacketPayload {
        public static final Type<ChooseSecondary> TYPE = typeOf("choose_secondary");
        public static final StreamCodec<ByteBuf, ChooseSecondary> CODEC = ByteBufCodecs.VAR_INT.map(ChooseSecondary::new, ChooseSecondary::profession);

        @Override
        public Type<ChooseSecondary> type() {
            return TYPE;
        }
    }

    public record SpendTalent(String id) implements CustomPacketPayload {
        public static final Type<SpendTalent> TYPE = typeOf("spend_talent");
        public static final StreamCodec<ByteBuf, SpendTalent> CODEC = ByteBufCodecs.stringUtf8(64).map(SpendTalent::new, SpendTalent::id);

        @Override
        public Type<SpendTalent> type() {
            return TYPE;
        }
    }

    public record ChooseSpec(String id) implements CustomPacketPayload {
        public static final Type<ChooseSpec> TYPE = typeOf("choose_spec");
        public static final StreamCodec<ByteBuf, ChooseSpec> CODEC = ByteBufCodecs.stringUtf8(64).map(ChooseSpec::new, ChooseSpec::id);

        @Override
        public Type<ChooseSpec> type() {
            return TYPE;
        }
    }

    /** Ação sobre missão: 0 aceitar, 1 entregar, 2 abandonar. */
    public record QuestAction(int action, String id) implements CustomPacketPayload {
        public static final Type<QuestAction> TYPE = typeOf("quest_action");
        public static final StreamCodec<ByteBuf, QuestAction> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, QuestAction::action, ByteBufCodecs.stringUtf8(64), QuestAction::id, QuestAction::new);

        @Override
        public Type<QuestAction> type() {
            return TYPE;
        }
    }

    public record PartyMember(String name, float health, float maxHealth, int level, int rpgClass, boolean leader) {
        public static final StreamCodec<ByteBuf, PartyMember> CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(32), PartyMember::name, ByteBufCodecs.FLOAT, PartyMember::health,
                ByteBufCodecs.FLOAT, PartyMember::maxHealth, ByteBufCodecs.VAR_INT, PartyMember::level,
                ByteBufCodecs.VAR_INT, m -> m.rpgClass() + 1, ByteBufCodecs.BOOL, PartyMember::leader,
                (name, hp, max, lvl, cls, leader) -> new PartyMember(name, hp, max, lvl, cls - 1, leader));
    }

    /** Estado do grupo para o HUD (lista vazia = sem grupo). */
    public record PartySync(List<PartyMember> members) implements CustomPacketPayload {
        public static final Type<PartySync> TYPE = typeOf("party_sync");
        public static final StreamCodec<ByteBuf, PartySync> CODEC = PartyMember.CODEC.apply(ByteBufCodecs.list(20)).map(PartySync::new, PartySync::members);

        @Override
        public Type<PartySync> type() {
            return TYPE;
        }
    }

    public record CraftRecipe(String id) implements CustomPacketPayload {
        public static final Type<CraftRecipe> TYPE = typeOf("craft_recipe");
        public static final StreamCodec<ByteBuf, CraftRecipe> CODEC = ByteBufCodecs.stringUtf8(64).map(CraftRecipe::new, CraftRecipe::id);

        @Override
        public Type<CraftRecipe> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        r.playToClient(OpenScreen.TYPE, OpenScreen.CODEC, (msg, ctx) -> com.alvorecer.client.ClientHooks.openScreen(msg.screen()));
        r.playToServer(CreateCharacter.TYPE, CreateCharacter.CODEC, ModNetwork::onCreate);
        r.playToServer(UseAbility.TYPE, UseAbility.CODEC, (msg, ctx) -> AbilityService.use((ServerPlayer) ctx.player(), msg.slot()));
        r.playToServer(AllocateStat.TYPE, AllocateStat.CODEC, ModNetwork::onAllocate);
        r.playToServer(ChooseSecondary.TYPE, ChooseSecondary.CODEC, ModNetwork::onSecondary);
        r.playToServer(SpendTalent.TYPE, SpendTalent.CODEC, (msg, ctx) -> onSpendTalent((ServerPlayer) ctx.player(), msg.id()));
        r.playToServer(ChooseSpec.TYPE, ChooseSpec.CODEC, (msg, ctx) -> onChooseSpec((ServerPlayer) ctx.player(), msg.id()));
        r.playToServer(QuestAction.TYPE, QuestAction.CODEC, (msg, ctx) -> {
            ServerPlayer p = (ServerPlayer) ctx.player();
            switch (msg.action()) {
                case 0 -> Quests.accept(p, msg.id());
                case 1 -> Quests.complete(p, msg.id());
                case 2 -> Quests.abandon(p, msg.id());
                default -> { }
            }
        });
        r.playToServer(CraftRecipe.TYPE, CraftRecipe.CODEC, (msg, ctx) -> com.alvorecer.crafting.ProfessionRecipes.craft((ServerPlayer) ctx.player(), msg.id()));
        r.playToClient(PartySync.TYPE, PartySync.CODEC, (msg, ctx) -> com.alvorecer.client.ClientHooks.party(msg.members()));
    }

    public static void openScreen(ServerPlayer p, Screen screen) {
        if (p.connection.hasChannel(OpenScreen.TYPE)) PacketDistributor.sendToPlayer(p, new OpenScreen(screen.ordinal()));
    }

    private static boolean valid(int index, Enum<?>[] values) {
        return index >= 0 && index < values.length;
    }

    /** Cria o personagem. Retorna false (sem alterar nada) para pedidos inválidos ou se já existe personagem. */
    public static boolean createCharacter(PlayerRpgData d, CreateCharacter msg) {
        if (d.hasCharacter()) return false;
        if (!valid(msg.race(), Race.values()) || !valid(msg.rpgClass(), RpgClass.values())
                || !valid(msg.profession(), Profession.values()) || !valid(msg.element(), Element.values())) return false;
        d.race = Race.values()[msg.race()];
        d.rpgClass = RpgClass.values()[msg.rpgClass()];
        d.profession = Profession.values()[msg.profession()];
        d.element = Element.values()[msg.element()];
        d.secondaryProfession = null;
        java.util.Arrays.fill(d.allocated, 0);
        d.unspentPoints = d.totalStatPoints();
        d.mana = Derived.maxMana(d);
        d.energy = Derived.maxEnergy(d);
        return true;
    }

    private static void onCreate(CreateCharacter msg, IPayloadContext ctx) {
        ServerPlayer p = (ServerPlayer) ctx.player();
        PlayerRpgData d = ModAttachments.get(p);
        if (!createCharacter(d, msg)) return;
        StatApplier.apply(p);
        p.setHealth(p.getMaxHealth());
        if (!d.codexGiven) {
            d.codexGiven = true;
            ItemStack codex = new ItemStack(ModItems.CODEX.get());
            if (!p.getInventory().add(codex)) p.drop(codex, false);
        }
        p.syncData(ModAttachments.RPG);
        p.sendSystemMessage(Component.translatable("msg.alvorecer.character_created",
                Component.translatable(d.race.langKey()), Component.translatable(d.rpgClass.langKey()),
                Component.translatable(d.profession.langKey())).withStyle(ChatFormatting.GOLD));
        p.sendSystemMessage(Component.translatable("msg.alvorecer.points_hint", d.unspentPoints).withStyle(ChatFormatting.YELLOW));
        Achievements.checkProgress(p);
    }

    /** Distribui um ponto. Pura para testes. */
    public static boolean allocate(PlayerRpgData d, int stat) {
        if (!d.hasCharacter() || d.unspentPoints <= 0 || !valid(stat, Stat.values())) return false;
        if (d.score(Stat.values()[stat]) >= Progression.MAX_SCORE) return false;
        d.allocated[stat]++;
        d.unspentPoints--;
        return true;
    }

    private static void onAllocate(AllocateStat msg, IPayloadContext ctx) {
        ServerPlayer p = (ServerPlayer) ctx.player();
        PlayerRpgData d = ModAttachments.get(p);
        if (!allocate(d, msg.stat())) return;
        StatApplier.apply(p);
        p.syncData(ModAttachments.RPG);
    }

    public static void onSpendTalent(ServerPlayer p, String id) {
        PlayerRpgData d = ModAttachments.get(p);
        Talents.Failure f = Talents.canSpend(d, id);
        if (f != null) {
            p.displayClientMessage(Component.translatable("msg.alvorecer.talent." + f.name().toLowerCase()), true);
            return;
        }
        Talents.spend(d, id);
        if (Talents.get(id).tier() == 3) Achievements.grant(p, "capstone");
        StatApplier.apply(p);
        p.syncData(ModAttachments.RPG);
    }

    public static void onChooseSpec(ServerPlayer p, String id) {
        PlayerRpgData d = ModAttachments.get(p);
        if (!Talents.chooseSpec(d, id)) return;
        StatApplier.apply(p);
        Achievements.grant(p, "specialization");
        p.sendSystemMessage(Component.translatable("msg.alvorecer.spec_chosen", Component.translatable(Talents.spec(id).nameKey()))
                .withStyle(ChatFormatting.GOLD));
        p.syncData(ModAttachments.RPG);
    }

    private static void onSecondary(ChooseSecondary msg, IPayloadContext ctx) {
        ServerPlayer p = (ServerPlayer) ctx.player();
        PlayerRpgData d = ModAttachments.get(p);
        if (!d.hasCharacter() || d.secondaryProfession != null || !valid(msg.profession(), Profession.values())) return;
        Profession prof = Profession.values()[msg.profession()];
        if (prof == d.profession) return;
        if (d.level < RpgConfig.SECONDARY_PROFESSION_LEVEL.get()) {
            p.displayClientMessage(Component.translatable("msg.alvorecer.locked", RpgConfig.SECONDARY_PROFESSION_LEVEL.get()), true);
            return;
        }
        d.secondaryProfession = prof;
        d.secondaryLevel = 1;
        d.secondaryXp = 0;
        StatApplier.apply(p);
        p.syncData(ModAttachments.RPG);
        p.sendSystemMessage(Component.translatable("msg.alvorecer.secondary_chosen", Component.translatable(prof.langKey())).withStyle(ChatFormatting.GREEN));
    }
}
