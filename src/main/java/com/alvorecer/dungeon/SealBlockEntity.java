package com.alvorecer.dungeon;

import com.alvorecer.Alvorecer;
import com.alvorecer.achievement.Achievements;
import com.alvorecer.config.RpgConfig;
import com.alvorecer.quest.Quests;
import com.alvorecer.registry.ModDungeon;
import com.alvorecer.registry.ModMobs;
import com.alvorecer.registry.ModWorld;
import com.alvorecer.rpg.Leveling;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Encontro de uma sala. Ao ativar, conta os jogadores em {@link #SCALE_RADIUS} blocos e escala as ondas (quantidade,
 * vida e dano). Sem jogadores por perto durante {@link #RESET_TICKS}, o encontro some e volta ao início. Vencido: abre os
 * portões a até {@link #DOOR_RADIUS} blocos e dá o XP de conclusão a cada jogador presente (não dividido).
 */
public class SealBlockEntity extends BlockEntity {
    public static final int ACTIVATE_RADIUS = 7, SCALE_RADIUS = 24, DOOR_RADIUS = 10, LEASH_RADIUS = 32, RESET_TICKS = 600;
    public static final int COMBAT_BASE = 3, COMBAT_WAVES = 2, CHAMPION_ADDS = 1;
    public static final double CHAMPION_HEALTH = 3.0;
    public static final String MOB_TAG = Alvorecer.MODID + ":dungeon_mob";
    private static final ResourceLocation SCALE_ID = ResourceLocation.fromNamespaceAndPath(Alvorecer.MODID, "dungeon_scale");

    private final List<UUID> mobs = new ArrayList<>();
    private int wave, players = 1, absentTicks, loadedTicks;

    public SealBlockEntity(BlockPos pos, BlockState state) {
        super(ModDungeon.SEAL_BE.get(), pos, state);
    }

    public int wave() {
        return wave;
    }

    public int players() {
        return players;
    }

    public List<UUID> mobs() {
        return mobs;
    }

    void tick(ServerLevel level, BlockState state) {
        loadedTicks++;
        if (level.getGameTime() % 10 != 0) return;
        SealBlock.Kind kind = state.getValue(SealBlock.KIND);
        SealBlock.Phase phase = state.getValue(SealBlock.PHASE);
        if (phase == SealBlock.Phase.CLEARED || kind == SealBlock.Kind.PUZZLE) return;
        if (phase == SealBlock.Phase.IDLE) {
            if (!near(level, ACTIVATE_RADIUS, 4).isEmpty()) activate(level, state);
            return;
        }
        // Logo depois de carregar o chunk as criaturas podem ainda não estar no mundo: não as conta como mortas
        if (loadedTicks > 100) mobs.removeIf(id -> {
            Entity e = level.getEntity(id);
            return e == null || !e.isAlive();
        });
        if (near(level, LEASH_RADIUS, 16).isEmpty()) {
            absentTicks += 10;
            if (absentTicks >= RESET_TICKS) reset(level, state);
            return;
        }
        absentTicks = 0;
        if (!mobs.isEmpty()) return;
        if (wave < (kind == SealBlock.Kind.CHAMPION ? 1 : COMBAT_WAVES)) spawnWave(level, kind);
        else clear(level, state);
    }

    private List<ServerPlayer> near(ServerLevel level, double radius, double dy) {
        return level.players().stream().filter(p -> !p.isSpectator() && p.isAlive()
                && Math.abs(p.getY() - worldPosition.getY()) <= dy
                && p.distanceToSqr(worldPosition.getX() + 0.5, p.getY(), worldPosition.getZ() + 0.5) <= radius * radius).toList();
    }

    private void activate(ServerLevel level, BlockState state) {
        players = Math.max(1, near(level, SCALE_RADIUS, 8).size());
        wave = 0;
        absentTicks = 0;
        level.setBlock(worldPosition, state.setValue(SealBlock.PHASE, SealBlock.Phase.ACTIVE), 3);
        level.playSound(null, worldPosition, SoundEvents.TRIAL_SPAWNER_DETECT_PLAYER, SoundSource.HOSTILE, 1.5f, 0.7f);
        for (ServerPlayer p : near(level, SCALE_RADIUS, 8)) {
            p.displayClientMessage(Component.translatable("msg.alvorecer.dungeon_challenge", players).withStyle(ChatFormatting.RED), true);
        }
        setChanged();
    }

    private void spawnWave(ServerLevel level, SealBlock.Kind kind) {
        wave++;
        int perExtra = RpgConfig.DUNGEON_MOBS_PER_PLAYER.get();
        double health = Dungeons.multiplier(players, RpgConfig.DUNGEON_HEALTH_PER_PLAYER.get());
        double damage = Dungeons.multiplier(players, RpgConfig.DUNGEON_DAMAGE_PER_PLAYER.get());
        boolean aether = level.dimension() == ModWorld.AETHER;
        List<EntityType<? extends Mob>> pool = aether
                ? List.of(ModMobs.ARCANE_WOLF.get(), ModMobs.ELEMENTAL.get(), ModMobs.SKELETON_WARRIOR.get())
                : List.of(ModMobs.GOBLIN.get(), ModMobs.SKELETON_WARRIOR.get(), ModMobs.GOBLIN.get());
        int count;
        if (kind == SealBlock.Kind.CHAMPION) {
            Mob champion = spawn(level, ModMobs.STONE_SENTINEL.get(), health * CHAMPION_HEALTH, damage);
            if (champion != null) {
                champion.setCustomName(Component.translatable("entity.alvorecer.dungeon_champion").withStyle(ChatFormatting.GOLD));
                champion.addEffect(new MobEffectInstance(MobEffects.GLOWING, -1, 0, false, false));
            }
            count = Dungeons.mobCount(CHAMPION_ADDS, players, perExtra);
        } else {
            count = Dungeons.mobCount(COMBAT_BASE, players, perExtra);
        }
        for (int i = 0; i < count; i++) spawn(level, pool.get(level.random.nextInt(pool.size())), health, damage);
        level.playSound(null, worldPosition, SoundEvents.TRIAL_SPAWNER_SPAWN_MOB, SoundSource.HOSTILE, 1.5f, 0.8f);
        int waves = kind == SealBlock.Kind.CHAMPION ? 1 : COMBAT_WAVES;
        for (ServerPlayer p : near(level, SCALE_RADIUS, 8)) {
            p.displayClientMessage(Component.translatable("msg.alvorecer.dungeon_wave", wave, waves).withStyle(ChatFormatting.RED), true);
        }
        setChanged();
    }

    private Mob spawn(ServerLevel level, EntityType<? extends Mob> type, double health, double damage) {
        Mob mob = type.create(level);
        if (mob == null) return null;
        BlockPos pos = spawnPos(level);
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360, 0);
        EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        mob.setPersistenceRequired();
        mob.getPersistentData().putBoolean(MOB_TAG, true);
        scale(mob.getAttribute(Attributes.MAX_HEALTH), health);
        scale(mob.getAttribute(Attributes.ATTACK_DAMAGE), damage);
        mob.setHealth(mob.getMaxHealth());
        level.addFreshEntity(mob);
        mob.setTarget(level.getNearestPlayer(mob, SCALE_RADIUS));
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, mob.getX(), mob.getY() + 0.5, mob.getZ(), 15, 0.3, 0.5, 0.3, 0.02);
        mobs.add(mob.getUUID());
        return mob;
    }

    private static void scale(AttributeInstance attr, double mult) {
        if (attr == null || mult == 1) return;
        attr.removeModifier(SCALE_ID);
        attr.addPermanentModifier(new AttributeModifier(SCALE_ID, mult - 1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)); // salvo com a criatura
    }

    /** Ponto livre no piso da sala, a até 5 blocos do selo; senão, em cima do selo. */
    private BlockPos spawnPos(ServerLevel level) {
        for (int i = 0; i < 12; i++) {
            BlockPos p = worldPosition.offset(level.random.nextInt(11) - 5, 1, level.random.nextInt(11) - 5);
            if (level.isEmptyBlock(p) && level.isEmptyBlock(p.above()) && level.getBlockState(p.below()).isSolid()) return p;
        }
        return worldPosition.above();
    }

    private void reset(ServerLevel level, BlockState state) {
        for (UUID id : mobs) {
            Entity e = level.getEntity(id);
            if (e != null) e.discard();
        }
        mobs.clear();
        wave = 0;
        absentTicks = 0;
        level.setBlock(worldPosition, state.setValue(SealBlock.PHASE, SealBlock.Phase.IDLE), 3);
        setChanged();
    }

    /** Vence a sala: abre os portões próximos e recompensa quem está presente. */
    public void clear(ServerLevel level, BlockState state) {
        SealBlock.Kind kind = state.getValue(SealBlock.KIND);
        level.setBlock(worldPosition, state.setValue(SealBlock.PHASE, SealBlock.Phase.CLEARED), 3);
        openDoors(level, worldPosition);
        level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.5f, 1.2f);
        long xp = switch (kind) {
            case CHAMPION -> RpgConfig.DUNGEON_CHAMPION_XP.get();
            case PUZZLE -> RpgConfig.DUNGEON_CLEAR_XP.get() / 2;
            case COMBAT -> RpgConfig.DUNGEON_CLEAR_XP.get();
        };
        for (ServerPlayer p : near(level, SCALE_RADIUS, 8)) {
            p.displayClientMessage(Component.translatable(kind == SealBlock.Kind.CHAMPION ? "msg.alvorecer.dungeon_champion_down"
                    : "msg.alvorecer.dungeon_cleared").withStyle(ChatFormatting.GREEN), true);
            Leveling.grantXp(p, xp);
            if (kind == SealBlock.Kind.CHAMPION) {
                Achievements.grant(p, "dungeon");
                Quests.onDungeonClear(p);
            }
        }
        mobs.clear();
        setChanged();
    }

    public static void openDoors(ServerLevel level, BlockPos center) {
        boolean any = false;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-DOOR_RADIUS, -2, -DOOR_RADIUS), center.offset(DOOR_RADIUS, 6, DOOR_RADIUS))) {
            if (p.distSqr(center) <= DOOR_RADIUS * DOOR_RADIUS && level.getBlockState(p).is(ModDungeon.SEALED_DOOR.get())) {
                level.removeBlock(p, false);
                level.sendParticles(ParticleTypes.END_ROD, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.02);
                any = true;
            }
        }
        if (any) level.playSound(null, center, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.5f, 0.6f);
    }

    /** Chamado quando uma runa gira: vence o selo-enigma mais próximo se todas as runas dele estiverem certas. */
    public static boolean checkPuzzle(ServerLevel level, BlockPos rune) {
        for (BlockPos p : BlockPos.betweenClosed(rune.offset(-12, -4, -12), rune.offset(12, 4, 12))) {
            BlockState s = level.getBlockState(p);
            if (!s.is(ModDungeon.SEAL.get()) || s.getValue(SealBlock.KIND) != SealBlock.Kind.PUZZLE
                    || s.getValue(SealBlock.PHASE) == SealBlock.Phase.CLEARED) continue;
            BlockPos seal = p.immutable();
            int runes = 0;
            for (BlockPos q : BlockPos.betweenClosed(seal.offset(-9, -2, -9), seal.offset(9, 6, 9))) {
                BlockState r = level.getBlockState(q);
                if (!r.is(ModDungeon.RUNE.get())) continue;
                if (r.getValue(RuneBlock.RUNE) != r.getValue(RuneBlock.TARGET)) return false;
                runes++;
            }
            if (runes == 0 || !(level.getBlockEntity(seal) instanceof SealBlockEntity be)) return false;
            be.clear(level, s);
            return true;
        }
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (UUID id : mobs) list.add(NbtUtils.createUUID(id));
        tag.put("Mobs", list);
        tag.putInt("Wave", wave);
        tag.putInt("Players", players);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        mobs.clear();
        for (Tag t : tag.getList("Mobs", Tag.TAG_INT_ARRAY)) mobs.add(NbtUtils.loadUUID(t));
        wave = tag.getInt("Wave");
        players = Math.max(1, tag.getInt("Players"));
    }
}
