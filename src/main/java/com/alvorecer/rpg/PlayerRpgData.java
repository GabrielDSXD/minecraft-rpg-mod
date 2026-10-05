package com.alvorecer.rpg;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import com.alvorecer.rpg.talent.BonusType;
import com.alvorecer.rpg.talent.Talents;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Ficha do personagem: persistida no jogador (anexo de dados) e sincronizada só com o dono. */
public class PlayerRpgData implements INBTSerializable<CompoundTag> {
    @Nullable public Race race;
    @Nullable public RpgClass rpgClass;
    @Nullable public Profession profession;
    @Nullable public Profession secondaryProfession;
    public Element element = Element.FIRE;

    public int level = 1;
    public long xp;
    public final int[] allocated = new int[Stat.values().length];
    public int unspentPoints;

    public float mana;
    public float energy;
    public final Map<String, Long> cooldowns = new HashMap<>();

    public int professionLevel = 1;
    public long professionXp;
    public int secondaryLevel = 1;
    public long secondaryXp;
    /** Dia do jogo (gameTime / 24000) e XP de profissão já ganho nele: limite diário anti-farm. */
    public long professionDay;
    public long professionXpToday;

    public final Set<String> visitedBiomes = new HashSet<>();

    /** Ranks gastos por talento e especialização escolhida (Etapa 2). */
    public final Map<String, Integer> talents = new HashMap<>();
    @Nullable public String specialization;
    /** Conquistas do mod já obtidas. */
    public final Set<String> achievements = new HashSet<>();
    /** Missões ativas (id → progresso) e concluídas (id → gameTime da conclusão). */
    public final Map<String, Integer> activeQuests = new HashMap<>();
    public final Map<String, Long> completedQuests = new HashMap<>();
    public int questsCompleted;
    public long lastRespecTime = Long.MIN_VALUE;
    public boolean codexGiven;
    /** Contagem de encantamentos vista pela última vez (estatística vanilla); -1 = ainda não lida. */
    public int enchantsSeen = -1;

    // Estado transitório (não salvo): efeitos de curta duração de habilidades.
    public long noFallUntil;
    public boolean leapPending;
    public long leapStart;
    public double leapPower;
    @Nullable public UUID markedTarget;
    public long markedUntil;
    /** Último Mural de Missões aberto (para validar aceitar/entregar perto dele). */
    @Nullable public net.minecraft.core.BlockPos lastBoardPos;

    public boolean hasCharacter() {
        return race != null && rpgClass != null && profession != null;
    }

    public boolean hasTalent(String id) {
        return talents.getOrDefault(id, 0) > 0;
    }

    public int score(Stat stat) {
        int s = Progression.BASE_SCORE + allocated[stat.ordinal()] + (int) Talents.bonus(this, BonusType.stat(stat));
        if (race != null) s += race.statBonus(stat);
        if (rpgClass != null && rpgClass.keyStat == stat) s += Progression.CLASS_KEY_BONUS;
        return s;
    }

    public int mod(Stat stat) {
        return Progression.modifier(score(stat));
    }

    /** Nível da profissão se for a principal ou a secundária; 0 se o jogador não a tiver. */
    public int levelOf(Profession prof) {
        if (profession == prof) return professionLevel;
        if (secondaryProfession == prof) return secondaryLevel;
        return 0;
    }

    public boolean isOnCooldown(String id, long now) {
        return cooldowns.getOrDefault(id, 0L) > now;
    }

    public long cooldownRemaining(String id, long now) {
        return Math.max(0, cooldowns.getOrDefault(id, 0L) - now);
    }

    /** Apaga as escolhas e as profissões; mantém nível e XP de classe. Os pontos são recalculados na nova criação. */
    public void resetCharacter() {
        race = null;
        rpgClass = null;
        profession = null;
        secondaryProfession = null;
        Arrays.fill(allocated, 0);
        unspentPoints = 0;
        cooldowns.clear();
        professionLevel = 1;
        professionXp = 0;
        secondaryLevel = 1;
        secondaryXp = 0;
        talents.clear();
        specialization = null;
    }

    /** Total de pontos de atributo que o personagem tem direito (criação + níveis). */
    public int totalStatPoints() {
        int total = Progression.CREATION_POINTS;
        for (int l = 2; l <= level; l++) total += Progression.statPointsAt(l);
        return total;
    }

    public void copyFrom(PlayerRpgData other, HolderLookup.Provider provider) {
        deserializeNBT(provider, other.serializeNBT(provider));
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("version", 1);
        if (race != null) tag.putString("race", race.name());
        if (rpgClass != null) tag.putString("class", rpgClass.name());
        if (profession != null) tag.putString("profession", profession.name());
        if (secondaryProfession != null) tag.putString("secondary", secondaryProfession.name());
        tag.putString("element", element.name());
        tag.putInt("level", level);
        tag.putLong("xp", xp);
        tag.putIntArray("allocated", allocated);
        tag.putInt("unspent", unspentPoints);
        tag.putFloat("mana", mana);
        tag.putFloat("energy", energy);
        CompoundTag cds = new CompoundTag();
        cooldowns.forEach(cds::putLong);
        tag.put("cooldowns", cds);
        tag.putInt("profLevel", professionLevel);
        tag.putLong("profXp", professionXp);
        tag.putInt("secLevel", secondaryLevel);
        tag.putLong("secXp", secondaryXp);
        tag.putLong("profDay", professionDay);
        tag.putLong("profToday", professionXpToday);
        ListTag biomes = new ListTag();
        visitedBiomes.forEach(b -> biomes.add(StringTag.valueOf(b)));
        tag.put("biomes", biomes);
        tag.putLong("lastRespec", lastRespecTime);
        tag.putBoolean("codexGiven", codexGiven);
        tag.putInt("enchantsSeen", enchantsSeen);
        CompoundTag tal = new CompoundTag();
        talents.forEach(tal::putInt);
        tag.put("talents", tal);
        if (specialization != null) tag.putString("spec", specialization);
        ListTag ach = new ListTag();
        achievements.forEach(a -> ach.add(StringTag.valueOf(a)));
        tag.put("achievements", ach);
        CompoundTag aq = new CompoundTag();
        activeQuests.forEach(aq::putInt);
        tag.put("quests", aq);
        CompoundTag cq = new CompoundTag();
        completedQuests.forEach(cq::putLong);
        tag.put("questsDone", cq);
        tag.putInt("questsCompleted", questsCompleted);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        race = parse(Race.class, tag.getString("race"));
        rpgClass = parse(RpgClass.class, tag.getString("class"));
        profession = parse(Profession.class, tag.getString("profession"));
        secondaryProfession = parse(Profession.class, tag.getString("secondary"));
        Element e = parse(Element.class, tag.getString("element"));
        element = e == null ? Element.FIRE : e;
        level = tag.contains("level") ? Math.clamp(tag.getInt("level"), 1, Progression.MAX_LEVEL) : 1;
        xp = Math.max(0, tag.getLong("xp"));
        Arrays.fill(allocated, 0);
        int[] a = tag.getIntArray("allocated");
        System.arraycopy(a, 0, allocated, 0, Math.min(a.length, allocated.length));
        unspentPoints = Math.max(0, tag.getInt("unspent"));
        mana = tag.getFloat("mana");
        energy = tag.getFloat("energy");
        cooldowns.clear();
        CompoundTag cds = tag.getCompound("cooldowns");
        for (String k : cds.getAllKeys()) cooldowns.put(k, cds.getLong(k));
        professionLevel = tag.contains("profLevel") ? Math.max(1, tag.getInt("profLevel")) : 1;
        professionXp = tag.getLong("profXp");
        secondaryLevel = tag.contains("secLevel") ? Math.max(1, tag.getInt("secLevel")) : 1;
        secondaryXp = tag.getLong("secXp");
        professionDay = tag.getLong("profDay");
        professionXpToday = tag.getLong("profToday");
        visitedBiomes.clear();
        ListTag biomes = tag.getList("biomes", Tag.TAG_STRING);
        for (int i = 0; i < biomes.size(); i++) visitedBiomes.add(biomes.getString(i));
        lastRespecTime = tag.contains("lastRespec") ? tag.getLong("lastRespec") : Long.MIN_VALUE;
        codexGiven = tag.getBoolean("codexGiven");
        enchantsSeen = tag.contains("enchantsSeen") ? tag.getInt("enchantsSeen") : -1;
        talents.clear();
        CompoundTag tal = tag.getCompound("talents");
        for (String k : tal.getAllKeys()) if (tal.getInt(k) > 0) talents.put(k, tal.getInt(k));
        specialization = tag.contains("spec") ? tag.getString("spec") : null;
        achievements.clear();
        ListTag ach = tag.getList("achievements", Tag.TAG_STRING);
        for (int i = 0; i < ach.size(); i++) achievements.add(ach.getString(i));
        activeQuests.clear();
        CompoundTag aq = tag.getCompound("quests");
        for (String k : aq.getAllKeys()) activeQuests.put(k, aq.getInt(k));
        completedQuests.clear();
        CompoundTag cq = tag.getCompound("questsDone");
        for (String k : cq.getAllKeys()) completedQuests.put(k, cq.getLong(k));
        questsCompleted = tag.getInt("questsCompleted");
    }

    @Nullable
    private static <E extends Enum<E>> E parse(Class<E> type, String name) {
        if (name.isEmpty()) return null;
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
