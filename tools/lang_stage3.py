"""Textos da Etapa 3 (equipamentos, bancada de ofício, consumíveis). Importado por gen_lang.py."""


def register(add):
    items = {
        # materiais e blocos
        "raw_lunar_silver": ("Prata Lunar Bruta", "Minério que brilha fraco sob a luz da lua."),
        "lunar_silver_ingot": ("Lingote de Prata Lunar", "Metal leve e resistente, usado por ferreiros e caçadores."),
        "aurorite_shard": ("Fragmento de Aurorita", "Cristal das profundezas que guarda um pouco da primeira luz."),
        "aurorite_gem": ("Gema de Aurorita", "Fragmentos lapidados em uma gema perfeita."),
        # armas de ferro
        "iron_dagger": ("Adaga de Ferro", "Leve e rápida, ideal para golpes precisos."),
        "iron_warhammer": ("Martelo de Guerra de Ferro", "Lento, mas cada golpe é uma avalanche."),
        "iron_spear": ("Lança de Ferro", "Alcance maior que o de uma espada."),
        # prata lunar
        "lunar_silver_sword": ("Espada de Prata Lunar", "Equilibrada e afiada."),
        "lunar_silver_dagger": ("Adaga de Prata Lunar", "Encontra brechas em qualquer armadura."),
        "lunar_silver_warhammer": ("Martelo de Prata Lunar", "Pesado como a lua cheia."),
        "lunar_silver_spear": ("Lança de Prata Lunar", "Mantém os inimigos à distância."),
        "lunar_silver_pickaxe": ("Picareta de Prata Lunar", "Mais rápida que o ferro."),
        "lunar_silver_axe": ("Machado de Prata Lunar", "Corta madeira e inimigos."),
        "lunar_silver_shovel": ("Pá de Prata Lunar", "Cava com facilidade."),
        "lunar_silver_hoe": ("Enxada de Prata Lunar", "Para hortas dignas de aventureiros."),
        "lunar_silver_bow": ("Arco de Prata Lunar", "Flechas com 20% a mais de dano."),
        "lunar_silver_crossbow": ("Besta de Prata Lunar", "Virotes com 20% a mais de dano."),
        "lunar_silver_shield": ("Escudo de Prata Lunar", "Reforçado com prata lunar."),
        # aurorita
        "aurorite_sword": ("Espada de Aurorita", "Lâmina cristalina que corta como a aurora."),
        "aurorite_dagger": ("Adaga de Aurorita", "Um lampejo e o golpe já foi dado."),
        "aurorite_warhammer": ("Martelo de Aurorita", "Esmaga com a força de um nascer do sol."),
        "aurorite_spear": ("Lança de Aurorita", "Ponta de cristal que nunca perde o fio."),
        "aurorite_pickaxe": ("Picareta de Aurorita", "Mais rápida que o diamante."),
        "aurorite_axe": ("Machado de Aurorita", "Cristal afiado e equilibrado."),
        "aurorite_shovel": ("Pá de Aurorita", "Afunda no solo como em neve."),
        "aurorite_hoe": ("Enxada de Aurorita", "Até a terra floresce mais rápido."),
        "aurorite_bow": ("Arco de Aurorita", "Flechas com 45% a mais de dano."),
        "aurorite_crossbow": ("Besta de Aurorita", "Virotes com 45% a mais de dano."),
        "aurorite_shield": ("Escudo de Aurorita", "Brilha quando apara um golpe."),
        # magia
        "oak_staff": ("Cajado de Carvalho", "Canaliza um Míssil do Cajado simples."),
        "amethyst_wand": ("Varinha de Ametista", "Dispara raios rápidos e baratos."),
        "aurorite_wand": ("Varinha de Aurorita", "Raios ainda mais fortes."),
        "arcane_staff": ("Cajado Arcano", "Guarda os feitiços que você domina e os lança à vontade."),
        # especiais
        "solar_breaker": ("Ruptora Solar", "Forjada com o brilho de mil manhãs."),
        "dawn_blade": ("Lâmina do Alvorecer", "Deixa um rastro de luz a cada golpe."),
        "shadow_daggers": ("Adagas das Sombras", "Um par de lâminas que só é visto quando já é tarde."),
        "prismatic_wings": ("Asas Prismáticas", "Asas que pintam o céu com todas as cores."),
        # consumíveis
        "mana_elixir": ("Elixir de Mana", None), "vigor_elixir": ("Elixir de Vigor", None),
        "healing_salve": ("Unguento Curativo", None), "adventurer_stew": ("Ensopado do Aventureiro", None),
    }
    for k, (name, lore) in items.items():
        add(f"item.alvorecer.{k}", name)
        if lore:
            add(f"item.alvorecer.{k}.lore", lore)
    for s, sname in (("wanderer", "do Andarilho"), ("arcane", "Arcano(a)"), ("lunar_silver", "de Prata Lunar"), ("aurorite", "de Aurorita")):
        names = {"wanderer": ("Capuz do Andarilho", "Gibão do Andarilho", "Calças do Andarilho", "Botas do Andarilho"),
                 "arcane": ("Chapéu Arcano", "Manto Arcano", "Saia Arcana", "Sapatos Arcanos"),
                 "lunar_silver": ("Elmo de Prata Lunar", "Cota de Prata Lunar", "Grevas de Prata Lunar", "Botas de Prata Lunar"),
                 "aurorite": ("Elmo de Aurorita", "Couraça de Aurorita", "Grevas de Aurorita", "Botas de Aurorita")}[s]
        lore = {"wanderer": "Couro leve para viagens longas.", "arcane": "Tecido bordado com runas de mana.",
                "lunar_silver": "Malha que repele magia.", "aurorite": "Placas cristalinas quase indestrutíveis."}[s]
        for piece, n in zip(("helmet", "chestplate", "leggings", "boots"), names):
            add(f"item.alvorecer.{s}_{piece}", n)
            add(f"item.alvorecer.{s}_{piece}.lore", lore)
    add("item.alvorecer.mana_elixir.tooltip", "§7Restaura 40% da mana máxima.")
    add("item.alvorecer.vigor_elixir.tooltip", "§7Restaura 50% da energia máxima e dá Velocidade I por 30 s.")
    add("item.alvorecer.healing_salve.tooltip", "§7Cura 8 de vida e remove o Veneno.")
    for b, n in {"lunar_silver_ore": "Minério de Prata Lunar", "deepslate_lunar_silver_ore": "Minério de Prata Lunar de Ardósia",
                 "aurorite_ore": "Minério de Aurorita", "lunar_silver_block": "Bloco de Prata Lunar", "aurorite_block": "Bloco de Aurorita",
                 "profession_workbench": "Bancada de Ofício"}.items():
        add(f"block.alvorecer.{b}", n)

    for k, v in {"common": "Comum", "uncommon": "Incomum", "rare": "Raro", "epic": "Épico", "legendary": "Lendário"}.items():
        add(f"rarity.alvorecer.{k}", v)
    for k, v in {"wanderer": "Traje do Andarilho", "arcane": "Vestes Arcanas", "lunar": "Cota de Prata Lunar", "aurorite": "Placas de Aurorita"}.items():
        add(f"set.alvorecer.{k}", v)
    ui = {
        "gear.type": "Tipo: %s", "gear.weight": "Armadura %s", "gear.level": "Requer nível %s",
        "gear.not_proficient": "Sua classe não é proficiente (−25% de dano)", "gear.bonuses": "Quando equipado:",
        "gear.set": "Conjunto %s (%s/%s peças):", "gear.ability": "Habilidade (botão direito): ", "gear.spell": "Feitiço: ",
        "gear.staff_hint": "Botão direito lança; agachado + botão direito troca o feitiço.",
        "gear.wand_hint": "Botão direito lança o feitiço.",
        "craft": "Fabricar", "craft.requires": "Requer: %s nível %s", "craft.xp": "+%s XP de profissão", "craft.inputs": "Ingredientes:",
    }
    for k, v in ui.items():
        add(f"ui.alvorecer.{k}", v)
    add("bonus.alvorecer.jump", "+%s de altura de salto")
    for k, v in {"no_profession": "Você não tem essa profissão.", "level": "Seu nível de profissão é baixo demais.",
                 "inputs": "Faltam ingredientes.", "too_far": "Fique perto da Bancada de Ofício."}.items():
        add(f"msg.alvorecer.craft.{k}", v)
    add("msg.alvorecer.gear_level", "Este item exige nível %s.")
    for k, v in {"coals": "Carvão", "wool": "Lã", "planks": "Tábuas", "logs": "Toras"}.items():
        add(f"tag.item.minecraft.{k}", v)
    add("msg.alvorecer.spell_selected", "Feitiço selecionado: %s")

    abilities = {
        "staff_arcane_bolt": ("Míssil do Cajado", "Projétil arcano no alvo mirado (até 24 blocos): 4 × poder de dano arcano."),
        "wand_bolt": ("Raio da Varinha", "Raio arcano rápido no alvo mirado (até 18 blocos): 3 × poder de dano arcano."),
        "item_solar_wave": ("Onda Solar", "Libera uma onda radiante que avança 14 blocos atravessando todos os inimigos: 10 × poder de dano radiante (dobrado contra mortos-vivos) e repulsão."),
        "item_dawn_dash": ("Investida do Alvorecer", "Avança cerca de 8 blocos deixando um rastro de luz; inimigos no caminho sofrem 80% do dano da arma × poder. Sem dano de queda."),
        "item_shadow_evasion": ("Evasão Sombria", "Salta para trás, fica invisível e evita o próximo ataque por 2 s."),
    }
    for k, (name, desc) in abilities.items():
        add(f"ability.alvorecer.{k}", name)
        add(f"ability.alvorecer.{k}.desc", desc)

    for k, v in {"equipment": "Equipamentos", "recipes": "Receitas de Ofício"}.items():
        add(f"codex.alvorecer.chapter.{k}", v)
    add("codex.alvorecer.equipment",
        "RARIDADES: Comum, Incomum, Raro, Épico e Lendário (cor do nome). REQUISITOS: abaixo do nível exigido o item não dá bônus, causa metade do dano e não usa a habilidade. "
        "PROFICIÊNCIA: armas fora das proficiências da classe causam −25% de dano.\n\n"
        "TIPOS: adagas (rápidas, críticos), espadas, martelos de guerra (pesados, lentos), lanças (+1 bloco de alcance), arcos e bestas (multiplicam o dano das flechas), "
        "escudos, cajados (Míssil do Cajado; o Cajado Arcano escolhe entre seus feitiços desbloqueados) e varinhas (raio rápido). "
        "ARMADURAS: leves (Andarilho, Arcanas), médias (Prata Lunar) e pesadas (Aurorita, −2% de velocidade por peça). "
        "Vestindo as 4 peças de um conjunto você ganha o bônus do conjunto.\n\n"
        "MATERIAIS: Prata Lunar (minério entre as camadas −48 e 48; derrete em lingotes) e Aurorita (minério de ardósia raro abaixo de −16; 4 fragmentos viram uma gema na bancada comum, "
        "ou 2 fragmentos com Minerador nível 5 na Bancada de Ofício).\n\n"
        "ASAS PRISMÁTICAS: funcionam como élitros e deixam um rastro de cores durante o voo.")
    add("codex.alvorecer.recipes",
        "A Bancada de Ofício (bancada de trabalho + 2 ferro + ametista + tábuas) faz receitas exclusivas das profissões, usando os ingredientes do inventário. "
        "Cada receita exige a profissão (principal ou secundária) num nível mínimo e concede XP de profissão. Lista:")
