package com.alvorecer.rpg;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.alvorecer.rpg.WeaponCategory.*;

/**
 * Classes jogáveis. {@code abilities} tem 4 habilidades (níveis 1, 3, 6 e 10) seguidas da suprema (nível 15).
 * {@code hitDie} multiplica a vida ganha por nível.
 */
public enum RpgClass {
    WARRIOR(Resource.ENERGY, Stat.STR, Stat.STR, Stat.CON, 1.25, ArmorWeight.HEAVY,
            EnumSet.allOf(WeaponCategory.class),
            List.of("warrior_power_strike", "warrior_second_wind", "warrior_charge", "warrior_war_cry", "warrior_action_surge")),
    BARBARIAN(Resource.ENERGY, Stat.STR, Stat.STR, Stat.CON, 1.5, ArmorWeight.MEDIUM,
            EnumSet.of(NONE, SWORD, DAGGER, AXE, MACE, HAMMER, SPEAR, SHIELD),
            List.of("barbarian_savage_cleave", "barbarian_rage", "barbarian_brutal_leap", "barbarian_roar", "barbarian_undying_rage")),
    WIZARD(Resource.MANA, Stat.INT, Stat.INT, Stat.WIS, 0.75, ArmorWeight.LIGHT,
            EnumSet.of(NONE, DAGGER, STAFF, WAND),
            List.of("wizard_arcane_missile", "wizard_arcane_shield", "wizard_fireball", "wizard_blink", "wizard_meteor_storm")),
    ROGUE(Resource.ENERGY, Stat.DEX, Stat.DEX, Stat.INT, 1.0, ArmorWeight.LIGHT,
            EnumSet.of(NONE, DAGGER, SWORD, BOW, CROSSBOW),
            List.of("rogue_treacherous_strike", "rogue_smoke_bomb", "rogue_shadow_step", "rogue_poison_blades", "rogue_blade_dance")),
    RANGER(Resource.ENERGY, Stat.DEX, Stat.STR, Stat.DEX, 1.0, ArmorWeight.MEDIUM,
            EnumSet.of(NONE, SWORD, DAGGER, AXE, SPEAR, BOW, CROSSBOW),
            List.of("ranger_aimed_shot", "ranger_hunters_mark", "ranger_volley", "ranger_disengage", "ranger_arrow_rain")),
    CLERIC(Resource.MANA, Stat.WIS, Stat.WIS, Stat.CHA, 1.0, ArmorWeight.MEDIUM,
            EnumSet.of(NONE, MACE, HAMMER, STAFF, SPEAR, CROSSBOW, SHIELD),
            List.of("cleric_sacred_flame", "cleric_heal", "cleric_word_of_protection", "cleric_turn_undead", "cleric_divine_intervention")),
    PALADIN(Resource.MANA, Stat.CHA, Stat.WIS, Stat.CHA, 1.25, ArmorWeight.HEAVY,
            EnumSet.of(NONE, SWORD, AXE, MACE, HAMMER, SPEAR, SHIELD),
            List.of("paladin_divine_smite", "paladin_lay_on_hands", "paladin_aura_of_protection", "paladin_holy_charge", "paladin_dawn_avatar")),
    DRUID(Resource.MANA, Stat.WIS, Stat.INT, Stat.WIS, 1.0, ArmorWeight.MEDIUM,
            EnumSet.of(NONE, DAGGER, STAFF, SPEAR, SHIELD),
            List.of("druid_thorns", "druid_rejuvenate", "druid_bear_form", "druid_entangle", "druid_natures_wrath")),
    BARD(Resource.MANA, Stat.CHA, Stat.DEX, Stat.CHA, 1.0, ArmorWeight.LIGHT,
            EnumSet.of(NONE, SWORD, DAGGER, BOW, CROSSBOW, WAND),
            List.of("bard_vicious_mockery", "bard_inspiration", "bard_song_of_rest", "bard_charm", "bard_heroic_symphony"));

    /** Nível em que cada habilidade da lista {@code abilities} é desbloqueada. */
    public static final int[] UNLOCK_LEVELS = {1, 3, 6, 10, 15};

    public final Resource resource;
    /** Atributo que escala as habilidades da classe. */
    public final Stat keyStat;
    /** Proficiências em testes de resistência. */
    public final Stat save1, save2;
    public final double hitDie;
    public final ArmorWeight maxArmor;
    public final Set<WeaponCategory> weapons;
    public final List<String> abilities;

    RpgClass(Resource resource, Stat keyStat, Stat save1, Stat save2, double hitDie, ArmorWeight maxArmor,
             Set<WeaponCategory> weapons, List<String> abilities) {
        this.resource = resource;
        this.keyStat = keyStat;
        this.save1 = save1;
        this.save2 = save2;
        this.hitDie = hitDie;
        this.maxArmor = maxArmor;
        this.weapons = weapons;
        this.abilities = abilities;
    }

    public boolean proficientSave(Stat stat) {
        return stat == save1 || stat == save2;
    }

    public boolean proficient(WeaponCategory category) {
        return weapons.contains(category);
    }

    public String id() {
        return name().toLowerCase();
    }

    public String langKey() {
        return "class.alvorecer." + id();
    }
}
