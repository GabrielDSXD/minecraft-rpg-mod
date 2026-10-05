package com.alvorecer.item.gear;

import com.alvorecer.registry.ModAttachments;
import com.alvorecer.rpg.PlayerRpgData;
import com.alvorecer.rpg.talent.BonusType;
import com.alvorecer.rpg.talent.Talent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Soma os bônus dos equipamentos do mod usados pelo jogador (arma na mão principal, escudo na secundária,
 * armadura e asas) e os bônus de conjunto (4 peças). Equipamento acima do nível do jogador não conta.
 */
public final class Equipment {
    public record SetBonus(String id, List<Talent.Bonus> bonuses) {
        public String nameKey() {
            return "set.alvorecer." + id;
        }
    }

    public static final int SET_PIECES = 4;
    private static final Map<String, SetBonus> SETS = new LinkedHashMap<>();

    static {
        set("wanderer", new Talent.Bonus(BonusType.SPEED, 0.10), new Talent.Bonus(BonusType.JUMP, 0.25));
        set("arcane", new Talent.Bonus(BonusType.REGEN, 0.50), new Talent.Bonus(BonusType.POWER_ARCANE, 0.10));
        set("lunar", new Talent.Bonus(BonusType.DAMAGE_TAKEN, 0.08), new Talent.Bonus(BonusType.MAGIC_RESIST, 0.15));
        set("aurorite", new Talent.Bonus(BonusType.DAMAGE_TAKEN, 0.10), new Talent.Bonus(BonusType.MAX_HEALTH, 4));
    }

    private Equipment() {
    }

    private static void set(String id, Talent.Bonus... bonuses) {
        SETS.put(id, new SetBonus(id, List.of(bonuses)));
    }

    @Nullable
    public static SetBonus set(String id) {
        return id == null ? null : SETS.get(id);
    }

    public static Map<String, SetBonus> sets() {
        return SETS;
    }

    @Nullable
    public static GearInfo gear(ItemStack stack) {
        return stack.isEmpty() ? null : GearInfo.get(stack.getItem());
    }

    public static boolean meets(PlayerRpgData d, @Nullable GearInfo g) {
        return g == null || (d.hasCharacter() && d.level >= g.level());
    }

    private static void add(double[] out, GearInfo g) {
        for (Talent.Bonus b : g.bonuses()) out[b.type().ordinal()] += b.amount();
    }

    /** Peças do conjunto vestidas que o jogador pode usar. */
    public static int setPieces(Player p, PlayerRpgData d, String set) {
        int n = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;
            GearInfo g = gear(p.getItemBySlot(slot));
            if (g != null && set.equals(g.set()) && meets(d, g)) n++;
        }
        return n;
    }

    public static double[] compute(Player p, PlayerRpgData d) {
        double[] out = new double[BonusType.values().length];
        GearInfo main = gear(p.getMainHandItem());
        if (main != null && meets(d, main) && !(p.getMainHandItem().getItem() instanceof ShieldItem)) add(out, main);
        GearInfo off = gear(p.getOffhandItem());
        if (off != null && meets(d, off) && p.getOffhandItem().getItem() instanceof ShieldItem) add(out, off);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;
            GearInfo g = gear(p.getItemBySlot(slot));
            if (g != null && meets(d, g)) add(out, g);
        }
        for (SetBonus s : SETS.values()) {
            if (setPieces(p, d, s.id()) >= SET_PIECES) for (Talent.Bonus b : s.bonuses()) out[b.type().ordinal()] += b.amount();
        }
        return out;
    }

    public static void refresh(Player p) {
        PlayerRpgData d = ModAttachments.get(p);
        d.gearBonus = compute(p, d);
    }
}
