package com.alvorecer.item.gear;

import com.alvorecer.rpg.ArmorWeight;
import com.alvorecer.rpg.WeaponCategory;
import com.alvorecer.rpg.talent.BonusType;
import com.alvorecer.rpg.talent.Talent;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Dados RPG de um equipamento: raridade, nível exigido, bônus enquanto equipado, categoria/peso, conjunto e
 * habilidade de uso (botão direito). Equipamentos abaixo do nível exigido não dão bônus, causam metade do dano e não
 * usam a habilidade. Os dados ficam num registro item → GearInfo (sem precisar de subclasses de item).
 */
public record GearInfo(GearRarity rarity, int level, List<Talent.Bonus> bonuses, @Nullable WeaponCategory category,
                       @Nullable ArmorWeight weight, @Nullable String set, @Nullable String ability) {

    private static final List<Map.Entry<Supplier<? extends Item>, GearInfo>> PENDING = new ArrayList<>();
    private static Map<Item, GearInfo> byItem;

    public static Builder of(GearRarity rarity, int level) {
        return new Builder(rarity, level);
    }

    /** Chamado durante o registro dos itens; o mapa é montado na primeira consulta. */
    public static void register(Supplier<? extends Item> item, GearInfo info) {
        PENDING.add(Map.entry(item, info));
        byItem = null;
    }

    @Nullable
    public static GearInfo get(Item item) {
        if (byItem == null) {
            Map<Item, GearInfo> map = new IdentityHashMap<>();
            for (var e : PENDING) map.put(e.getKey().get(), e.getValue());
            byItem = map;
        }
        return byItem.get(item);
    }

    public static List<Map.Entry<Supplier<? extends Item>, GearInfo>> all() {
        return PENDING;
    }

    public static final class Builder {
        private final GearRarity rarity;
        private final int level;
        private final List<Talent.Bonus> bonuses = new ArrayList<>();
        private WeaponCategory category;
        private ArmorWeight weight;
        private String set;
        private String ability;

        private Builder(GearRarity rarity, int level) {
            this.rarity = rarity;
            this.level = level;
        }

        public Builder bonus(BonusType type, double amount) {
            bonuses.add(new Talent.Bonus(type, amount));
            return this;
        }

        public Builder category(WeaponCategory category) {
            this.category = category;
            return this;
        }

        public Builder weight(ArmorWeight weight) {
            this.weight = weight;
            return this;
        }

        public Builder set(String set) {
            this.set = set;
            return this;
        }

        public Builder ability(String ability) {
            this.ability = ability;
            return this;
        }

        public GearInfo build() {
            return new GearInfo(rarity, level, List.copyOf(bonuses), category, weight, set, ability);
        }
    }
}
