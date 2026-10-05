"""Textos da Etapa 4 (criaturas, NPCs, bestiário, sons). Importado por gen_lang.py."""


def register(add):
    entities = {
        "goblin": "Goblin", "skeleton_warrior": "Esqueleto Guerreiro", "arcane_wolf": "Lobo Arcano", "elemental": "Elemental",
        "stone_sentinel": "Sentinela de Pedra", "fairy": "Fada", "wyrmling": "Dragonete", "elemental_bolt": "Projétil Elemental",
        "guild_master": "Mestre da Guilda", "merchant": "Mercador Itinerante", "sage": "Sábio do Alvorecer",
    }
    for k, v in entities.items():
        add(f"entity.alvorecer.{k}", v)
        if k != "elemental_bolt":
            add(f"item.alvorecer.{k}_spawn_egg", f"Ovo de {v}")

    drops = {
        "goblin_ear": ("Orelha de Goblin", "Prova de caça aceita pelo Mestre da Guilda."),
        "arcane_fang": ("Presa Arcana", "Ainda pulsa com magia de teleporte."),
        "elemental_core": ("Núcleo Elemental", "O coração instável de um elemental."),
        "sentinel_heart": ("Coração de Sentinela", "Uma pedra rúnica que nunca esfria."),
        "fairy_dust": ("Pó de Fada", "Brilha mesmo no escuro."),
        "wyrmling_scale": ("Escama de Dragonete", "Resistente ao fogo e leve como couro."),
    }
    for k, (name, desc) in drops.items():
        add(f"item.alvorecer.{k}", name)
        add(f"item.alvorecer.{k}.lore", desc)
    add("item.alvorecer.bestiary_scepter", "Cetro do Bestiário")
    add("item.alvorecer.bestiary_scepter.tooltip", "§7Ferramenta de administrador. Selecionado: %s\n§8Botão direito invoca; agachado + botão direito troca.")
    add("msg.alvorecer.scepter_denied", "Apenas administradores ou o modo criativo podem usar o cetro.")
    add("msg.alvorecer.scepter_selected", "Criatura selecionada: %s")
    add("msg.alvorecer.bestiary_entry", "Bestiário atualizado: %s")

    subtitles = {
        "goblin.ambient": "Goblin tagarela", "goblin.hurt": "Goblin ferido", "goblin.death": "Goblin morre", "goblin.leap": "Goblin salta",
        "skeleton_warrior.ambient": "Ossos chacoalham", "skeleton_warrior.hurt": "Esqueleto Guerreiro ferido",
        "skeleton_warrior.death": "Esqueleto Guerreiro desmorona", "skeleton_warrior.charge": "Esqueleto Guerreiro investe",
        "skeleton_warrior.block": "Escudo bloqueia",
        "arcane_wolf.ambient": "Lobo Arcano rosna", "arcane_wolf.hurt": "Lobo Arcano ganindo", "arcane_wolf.death": "Lobo Arcano uiva",
        "arcane_wolf.blink": "Lobo Arcano se teleporta",
        "elemental.ambient": "Elemental crepita", "elemental.hurt": "Elemental ferido", "elemental.death": "Elemental se dissipa",
        "elemental.shoot": "Elemental dispara", "elemental.pulse": "Pulso elemental",
        "stone_sentinel.ambient": "Sentinela ressoa", "stone_sentinel.hurt": "Pedra racha", "stone_sentinel.death": "Sentinela desmorona",
        "stone_sentinel.slam": "Golpe no chão",
        "fairy.ambient": "Fada cintila", "fairy.hurt": "Fada ferida", "fairy.death": "Fada desaparece",
        "wyrmling.ambient": "Dragonete chilreia", "wyrmling.hurt": "Dragonete ferido", "wyrmling.death": "Dragonete cai",
        "wyrmling.breath": "Sopro de fogo", "npc.greet": "NPC cumprimenta",
    }
    for k, v in subtitles.items():
        add(f"subtitles.alvorecer.{k}", v)

    bestiary = {
        "goblin": ("Pequenos saqueadores que andam em bandos de 2 a 4 em planícies, florestas e savanas. Saltam sobre quem está de costas.",
                   "Vida 14 • Dano 3 • Velocidade alta. EMBOSCADA: salto de 3 a 8 blocos quando o alvo não está olhando; o golpe do salto causa +50%. "
                   "Drops: Orelha de Goblin, pepitas de ouro, às vezes a adaga. Dica: mantenha-os à vista."),
        "skeleton_warrior": ("Mortos-vivos de espada e escudo que vagam à noite por todo o mundo.",
                             "Vida 26 • Armadura 6 • Dano 5. Bloqueia 70% dos projéteis vindos da frente. INVESTIDA: fica 0,5 s parado com chamas azuis e avança; "
                             "o golpe causa +50%. Queima ao sol; fraco contra dano radiante e Golpear. Drops: ossos, Prata Lunar Bruta (15%)."),
        "arcane_wolf": ("Lobos impregnados de magia arcana que caçam em matilha nas taigas, à noite.",
                        "Vida 20 • Dano 4. TELEPORTE: a cada 6 a 10 s aparece nas costas do alvo (som e partículas de portal avisam). Saltam em você. "
                        "Drops: Presa Arcana, couro."),
        "elemental": ("Espíritos de fogo (desertos e terras áridas) ou de gelo (regiões nevadas) que flutuam.",
                      "Vida 24 • Armadura 4. Dispara projéteis do seu elemento (fogo incendeia, gelo causa Lentidão II). PULSO: quando você chega a 3 blocos, "
                      "um anel de partículas gira por 1 s e então explode (6 de dano). Imune ao próprio elemento e recebe dano dobrado do oposto. "
                      "Drops: Núcleo Elemental (50%), pó de blaze."),
        "stone_sentinel": ("Guardiões rúnicos de pedra das montanhas. Lentos, enormes e quase imunes a empurrões.",
                           "Vida 80 • Armadura 12 • Dano 10. GOLPE NO CHÃO: ergue os braços por 1 s enquanto um anel marca 4,5 blocos; então esmaga (12 de dano) "
                           "e arremessa quem estiver no chão dentro do anel — pule ou saia do anel. Drops: Coração de Sentinela, pedra, Aurorita (30%)."),
        "fairy": ("Criaturas pacíficas de prados e florestas floridas. Curam outras fadas próximas.",
                  "Vida 6. Se atacada, cega o agressor por 3 s e se teleporta. Drops: Pó de Fada."),
        "wyrmling": ("Pequenos dragões voadores de picos e terras áridas.",
                     "Vida 30 • Armadura 4 • Dano 5. SOPRO: abre a boca e lança chamas em cone de 7 blocos por 1,5 s (0,4 s de aviso antes do dano). "
                     "Imune a fogo. Drops: Escama de Dragonete, Gema de Aurorita (10%)."),
        "guild_master": ("Organiza a guilda dos aventureiros: oferece as missões do Mural e vende armas de ferro, o Mural e o Tomo do Recomeço.", None),
        "merchant": ("Compra e vende materiais e elixires: Prata Lunar, Aurorita, elixires, unguentos e flechas.", None),
        "sage": ("Guardião do conhecimento: vende o Codex, cajados e varinhas, compra componentes mágicos e oferece missões.", None),
    }
    for k, (basic, adv) in bestiary.items():
        add(f"bestiary.alvorecer.{k}", basic)
        if adv:
            add(f"bestiary.alvorecer.{k}.advanced", adv)
    add("codex.alvorecer.chapter.bestiary", "Bestiário")
    add("codex.alvorecer.bestiary", "Criaturas registradas: %s de %s. Derrote uma criatura para liberar as informações avançadas (atributos, ataques especiais e drops). "
                                    "A frequência de nascimento é configurável (creatures.spawnRate).")
    add("codex.alvorecer.bestiary_locked", "Informações avançadas: derrote um exemplar para revelar.")

    npc = {
        "guild_master": ("Bem-vindo à guilda, aventureiro! Há sempre trabalho para quem tem coragem e uma boa lâmina.",
                         ["Goblins andam roubando as fazendas. Traga as orelhas como prova e eu pago.",
                          "Os esqueletos de escudo bloqueiam flechas de frente. Ataque pelos lados.",
                          "Dizem que um guardião antigo ainda vigia o lugar onde a primeira luz tocou a terra.",
                          "Missões diárias voltam a cada dia. Um bom aventureiro nunca fica parado."]),
        "merchant": ("Mercadorias de todos os reinos! Prata da lua, cristais da aurora e elixires frescos.",
                     ["Quem tem carisma paga menos. É assim que funciona o comércio.",
                      "Compro escamas de dragonete. Pago bem, porque quase ninguém volta com elas.",
                      "A Aurorita só aparece nas camadas mais profundas da ardósia.",
                      "Volte amanhã: meu estoque se renova todo dia."]),
        "sage": ("A luz do primeiro alvorecer ainda ecoa em tudo o que é mágico. Em que posso ajudar?",
                 ["As fadas se ofendem fácil. Um golpe e você ficará cego por um tempo.",
                  "Elementais de fogo temem o gelo, e os de gelo temem o fogo.",
                  "O Codex registra o que você aprende. Leia-o com frequência.",
                  "Núcleos elementais e presas arcanas são os componentes do Cajado Arcano."]),
    }
    for k, (greet, talks) in npc.items():
        add(f"npc.alvorecer.{k}.greeting", greet)
        for i, line in enumerate(talks):
            add(f"npc.alvorecer.{k}.talk.{i}", line)
    for k, v in {"talk": "Conversar", "trade": "Comércio", "quests": "Missões", "bye": "Até logo"}.items():
        add(f"ui.alvorecer.npc.{k}", v)
