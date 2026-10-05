import json, os, sys

ROOT = sys.argv[1]
L = {}

def add(k, v):
    assert k not in L, k
    L[k] = v

add("itemGroup.alvorecer", "Crônicas do Alvorecer")
add("key.categories.alvorecer", "Crônicas do Alvorecer")
for k, v in {
    "race": "Habilidade racial", "class1": "Habilidade de classe 1", "class2": "Habilidade de classe 2",
    "class3": "Habilidade de classe 3", "class4": "Habilidade de classe 4", "ultimate": "Habilidade suprema",
    "profession": "Habilidade da profissão", "secondary": "Habilidade da profissão secundária",
    "sheet": "Ficha do personagem", "codex": "Codex do Aventureiro"}.items():
    add(f"key.alvorecer.{k}", v)

# ---------------------------------------------------------------- itens, efeitos, entidades
add("item.alvorecer.codex", "Codex do Aventureiro")
add("item.alvorecer.codex.tooltip", "§7Clique com o botão direito para abrir o guia.")
add("item.alvorecer.respec_tome", "Tomo do Recomeço")
add("item.alvorecer.respec_tome.tooltip", "§7Refaz raça, classe e profissão (mantém nível e XP).")
add("entity.alvorecer.illusion", "Ilusão")
for k, v in {
    "determination": "Determinação", "stoneskin": "Pele de Pedra", "evasion": "Esquiva", "orc_fury": "Fúria Orc",
    "barbarian_rage": "Fúria Bárbara", "undying": "Fúria Imortal", "poison_blades": "Lâminas Envenenadas",
    "bear_form": "Forma de Urso", "holy_aura": "Aura de Proteção", "dawn_avatar": "Avatar da Aurora",
    "stun": "Atordoado", "root": "Enraizado", "charm": "Encantado"}.items():
    add(f"effect.alvorecer.{k}", v)

for k, v in {
    "arcane": ("foi desfeito por energia arcana", "foi desfeito pela magia arcana de %2$s"),
    "fire": ("virou cinzas", "virou cinzas pelas chamas de %2$s"),
    "cold": ("congelou", "foi congelado por %2$s"),
    "lightning": ("foi fulminado por um raio", "foi fulminado pelo raio de %2$s"),
    "acid": ("foi corroído por ácido", "foi corroído pelo ácido de %2$s"),
    "radiant": ("foi consumido por luz radiante", "foi consumido pela luz radiante de %2$s"),
    "necrotic": ("sucumbiu às sombras", "sucumbiu às sombras de %2$s"),
    "nature": ("foi tomado pela natureza", "foi tomado pela fúria natural de %2$s"),
    "psychic": ("perdeu a razão", "perdeu a razão diante de %2$s")}.items():
    add(f"death.attack.alvorecer.{k}", "%1$s " + v[0])
    add(f"death.attack.alvorecer.{k}.player", "%1$s " + v[1])

# ---------------------------------------------------------------- atributos
stats = {
    "str": ("Força", "FOR", "Força: +4% de dano corpo a corpo por ponto de modificador. Escala Guerreiro, Bárbaro e Orc. Testes de FOR resistem a enraizamento (Espinhos, Vinhas)."),
    "dex": ("Destreza", "DES", "Destreza: +4% de dano à distância, +2% de velocidade de ataque e +1% de chance de crítico por ponto de modificador. Escala Ladino e Patrulheiro. Testes de DES resistem a sopros, explosões, lentidão, levitação, atordoamento e enraizamento."),
    "con": ("Constituição", "CON", "Constituição: +2 de vida máxima e +5 de energia máxima por ponto de modificador. Escala o Sopro draconato. Testes de CON resistem a veneno, murchar, fome e à Explosão Infernal."),
    "int": ("Inteligência", "INT", "Inteligência: escala o Mago e as magias raciais de Elfo e Gnomo. Para Mago e para as classes que não usam mana como recurso principal, cada ponto de modificador dá +10 de mana máxima."),
    "wis": ("Sabedoria", "SAB", "Sabedoria: +10% de regeneração de mana e 1,5% de redução de dano mágico por ponto de modificador (máx. 30%). Escala Clérigo, Druida e a Marca do Caçador; +10 de mana por modificador para Clérigo e Druida. Testes de SAB resistem a fraqueza, cegueira, náusea, escuridão, encanto e medo."),
    "cha": ("Carisma", "CAR", "Carisma: desconto de 2% por ponto de modificador com aldeões (até 20%) e +0,25 de sorte (qualidade dos saques) por modificador. Escala Paladino, Bardo e a explosão do Tiefling; +10 de mana por modificador para Paladino e Bardo."),
}
for k, (name, short, desc) in stats.items():
    add(f"stat.alvorecer.{k}", name)
    add(f"stat.alvorecer.{k}.short", short)
    add(f"stat.alvorecer.{k}.desc", desc)

add("resource.alvorecer.none", "Nenhum")
add("resource.alvorecer.mana", "Mana")
add("resource.alvorecer.energy", "Energia")
for k, v in {"physical": "Física", "arcane": "Arcana", "elemental": "Elemental", "divine": "Divina", "nature": "Natural", "shadow": "Sombria"}.items():
    add(f"school.alvorecer.{k}", v)
for k, v in {"fire": "Fogo", "cold": "Gelo", "lightning": "Raio", "acid": "Ácido"}.items():
    add(f"element.alvorecer.{k}", v)
for k, v in {"none": "Sem armadura", "light": "Leve", "medium": "Média", "heavy": "Pesada"}.items():
    add(f"armor_weight.alvorecer.{k}", v)
for k, v in {"none": "Desarmado", "sword": "Espadas", "dagger": "Adagas", "axe": "Machados", "mace": "Maças",
             "hammer": "Martelos de guerra", "spear": "Lanças e tridentes", "bow": "Arcos", "crossbow": "Bestas",
             "staff": "Cajados", "wand": "Varinhas", "shield": "Escudos"}.items():
    add(f"weapon.alvorecer.{k}", v)

# ---------------------------------------------------------------- raças
races = {
    "human": ("Humano", "Versáteis e ambiciosos. +1 em todos os atributos e +10% de XP de classe. Desvantagens: mana máxima -10% e nenhuma resistência natural a elementos ou veneno."),
    "elf": ("Elfo", "Graciosos e antigos. Visão noturna permanente, +10% de dano com arcos, +5% de poder arcano e elemental, +10% de mana máxima e +1 bloco de queda segura. Desvantagens: -4 de vida máxima (2 corações) e +50% de dano de veneno e ácido."),
    "dwarf": ("Anão", "Robustos filhos da montanha. -50% de dano de veneno e ácido e +20% de velocidade de mineração; altura 90%. Desvantagens: velocidade de movimento -8% e habilidades de mobilidade custam +50%."),
    "halfling": ("Halfling", "Pequenos, ágeis e sortudos. +1 de sorte, +3% de chance de crítico, agachado é percebido à metade da distância, anda mais rápido agachado e +1 bloco de queda segura; altura 80%. Desvantagens: alcance corpo a corpo -0,5 bloco e -25% de dano com machados, maças e martelos."),
    "orc": ("Orc", "Ferozes guerreiros das estepes. +15% de dano com machados, maças e martelos; altura 105%. Desvantagens: poder mágico -15%, custo de mana +10% e mana máxima -15%; durante a Fúria recebe +20% de dano."),
    "dragonborn": ("Draconato", "Herdeiros do sangue dracônico. Escolhem um elemento na criação (Fogo, Gelo, Raio ou Ácido) e recebem -50% de dano dele; altura 105%. Desvantagens: +50% de dano do elemento oposto (Fogo↔Gelo, Raio↔Ácido) e regeneração de energia -25%."),
    "tiefling": ("Tiefling", "Marcados por um pacto infernal antigo. -50% de dano de fogo e +15% de poder em magia sombria. Desvantagens: +50% de dano radiante recebido e curas recebidas -20%."),
    "gnome": ("Gnomo", "Inventores e alquimistas curiosos. +10% de mana máxima, +5% de poder arcano e elemental e +25% de XP como Alquimista, Artesão ou Encantador; altura 85%. Desvantagens: -1 de Força e -20% de dano com machados, maças e martelos."),
}
for k, (name, desc) in races.items():
    add(f"race.alvorecer.{k}", name)
    add(f"race.alvorecer.{k}.desc", desc)

# ---------------------------------------------------------------- classes
classes = {
    "warrior": ("Guerreiro", "Mestre de armas versátil e resistente; recupera 3 de energia a cada golpe corpo a corpo. Limitações: nenhuma cura para aliados e pouco alcance. Sinergias: Clérigo e Bardo o mantêm na linha de frente; o Grito de Guerra fortalece Ladinos e Patrulheiros."),
    "barbarian": ("Bárbaro", "Combatente selvagem movido pela dor: recupera 1,5 de energia por ponto de dano sofrido. Limitações: sem armadura pesada, sem arcos e a Fúria Bárbara não reduz dano mágico. Sinergias: atrai os inimigos para longe de Magos e Druidas; o Rugido prepara alvos para o Ladino."),
    "wizard": ("Mago", "Estudioso da magia arcana e elemental, com o maior dano em área. Limitações: pouca vida por nível, só armadura leve e poucas armas. Sinergias: precisa de Guerreiros ou Paladinos na frente; os enraizamentos do Druida seguram alvos na Bola de Fogo."),
    "rogue": ("Ladino", "Especialista em ataques furtivos: +35% de dano corpo a corpo contra criaturas que não estão mirando você, crítico +5% com multiplicador ×1,75 e energia regenerando 7 por segundo. Limitações: só armadura leve; frágil quando vira alvo. Sinergias: Guerreiros e Bárbaros atraem inimigos e liberam os ataques furtivos."),
    "ranger": ("Patrulheiro", "Caçador à distância. Tiro Certeiro, Saraivada e Chuva de Flechas exigem arco ou besta em uma das mãos; as flechas dessas habilidades não podem ser recolhidas. Limitações: fraco sem arco; a Marca do Caçador só aumenta o dano do próprio Patrulheiro. Sinergias: marca alvos prioritários e controla a distância para o grupo."),
    "cleric": ("Clérigo", "Canal do poder divino: cura, proteção e dano radiante, dobrado contra mortos-vivos. Limitações: dano baixo contra criaturas vivas. Sinergias: mantém Guerreiros e Bárbaros de pé; a Palavra de Proteção reforça todo o grupo."),
    "paladin": ("Paladino", "Guerreiro sagrado que une armas e magia divina e usa mana como recurso. Limitações: depende de mana para quase tudo e não tem ataques à distância. Sinergias: a Aura de Proteção cobre magos próximos; o Avatar da Aurora encerra lutas contra mortos-vivos."),
    "druid": ("Druida", "Guardião da natureza: enraíza inimigos, cura ao longo do tempo e assume a Forma de Urso. Limitações: na Forma de Urso as outras habilidades de classe ficam bloqueadas. Sinergias: inimigos enraizados facilitam os golpes do Ladino e a Bola de Fogo do Mago."),
    "bard": ("Bardo", "Artista inspirador: fortalece aliados, enfraquece e encanta inimigos; escala com Carisma. Limitações: dano individual baixo e só armadura leve. Sinergias: Inspiração e Sinfonia Heroica ampliam qualquer grupo; restaura energia das classes marciais."),
}
for k, (name, desc) in classes.items():
    add(f"class.alvorecer.{k}", name)
    add(f"class.alvorecer.{k}.desc", desc)

# ---------------------------------------------------------------- profissões
profs = {
    "blacksmith": ("Ferreiro", "XP: fabricar ferramentas, armas, armaduras e escudos (10) e retirar lingotes da fornalha (2 por lingote). Bônus por nível: 1% de chance de lingote extra ao retirar da fornalha e bigornas se desgastam 4% menos."),
    "alchemist": ("Alquimista", "XP: retirar poções com efeito da bancada de poções (5 por poção). Bônus por nível: poções que você bebe duram +2%."),
    "enchanter": ("Encantador", "XP: encantar itens na mesa de encantamentos (15 por encantamento) e usar a bigorna (5). Bônus por nível: +1% de mana máxima e +1% de experiência vanilla recebida."),
    "herbalist": ("Herbalista", "XP: colher plantações maduras — trigo, cenoura, batata, beterraba, fungo do Nether e cacau (2 cada). Bônus por nível: 2% de chance de colheita extra."),
    "miner": ("Minerador", "XP: quebrar minérios sem Toque Suave (carvão/cobre/quartzo 2, redstone 3, ferro/lápis 4, ouro 5, diamante 10, esmeralda 12; detritos ancestrais não contam). Bônus por nível: +2% de velocidade de mineração e 1% de chance de drop dobrado."),
    "hunter": ("Caçador", "XP: abater animais adultos (vida máxima ÷ 2) e monstros (vida máxima ÷ 4); criaturas de geradores rendem 1/4. Bônus por nível: +1% de dano contra animais e 2% de chance de drop extra."),
    "cook": ("Cozinheiro", "XP: retirar comida da fornalha (1 por item) e preparar receitas de comida (3 por item, até 4 por receita). Bônus por nível: comida restaura +3% de saturação e 1% de chance de comida extra ao retirar da fornalha."),
    "artisan": ("Artesão", "XP: qualquer receita com pelo menos 2 ingredientes diferentes (1 + ingredientes distintos, máx. 5). Bônus por nível: 1% de chance de receber uma unidade extra do item fabricado (itens empilháveis)."),
}
for k, (name, desc) in profs.items():
    add(f"profession.alvorecer.{k}", name)
    add(f"profession.alvorecer.{k}.desc", desc)

# ---------------------------------------------------------------- habilidades
abilities = {
    "human_determination": ("Determinação", "Remove lentidão, fraqueza, cegueira, náusea, escuridão, levitação, cansaço, atordoamento, enraizamento e encanto, e dá imunidade a esses efeitos por 8 s."),
    "elf_fey_step": ("Passo Feérico", "Teleporta até 8 blocos na direção da mira, sem atravessar paredes."),
    "dwarf_stoneskin": ("Pele de Pedra", "Por 10 s: dano recebido -40%, +50% de resistência a repulsão e velocidade -15%."),
    "halfling_luck": ("Sorte do Pequenino", "Por 3 s, o próximo ataque de uma criatura ou jogador é completamente evitado."),
    "orc_fury": ("Fúria", "Por 12 s: +30% de dano corpo a corpo e +20% de velocidade de ataque, mas +20% de dano recebido."),
    "dragonborn_breath": ("Sopro Dracônico", "Cone de 6 blocos: 7 × poder de dano do seu elemento. Teste de DES (CD com CON) reduz à metade; quem falha sofre o efeito do elemento — Fogo: queima 4 s; Gelo: Lentidão II 3 s; Raio: atordoa 1 s; Ácido: Fraqueza 4 s."),
    "tiefling_hellblast": ("Explosão Infernal", "Raio de 4,5 blocos: 4 × poder de dano de fogo + 4 × poder de dano necrótico. Teste de CON (CD com CAR) reduz à metade; quem falha queima por 3 s."),
    "gnome_illusion": ("Ilusão", "Cria uma cópia sua por 8 s (some após 5 golpes). Inimigos a até 16 blocos que miravam você passam a atacar a ilusão; você fica invisível por 3 s."),
    "warrior_power_strike": ("Golpe Poderoso", "Golpe no inimigo à frente (até 4,5 blocos): 150% do dano da arma × poder e forte repulsão."),
    "warrior_second_wind": ("Segundo Fôlego", "Recupera 6 × poder de vida."),
    "warrior_charge": ("Investida", "Avança cerca de 6 blocos; inimigos no caminho sofrem 100% do dano da arma × poder, Lentidão II por 2 s e repulsão. Sem dano de queda."),
    "warrior_war_cry": ("Grito de Guerra", "Aliados a até 10 blocos ganham Força I por 10 s. Inimigos a até 6 blocos fazem teste de SAB (CD com FOR) ou sofrem Fraqueza por 6 s."),
    "warrior_action_surge": ("Surto de Ação", "Zera a recarga das outras habilidades de Guerreiro e concede Força II, Pressa II e Resistência I por 10 s."),
    "barbarian_savage_cleave": ("Golpe Selvagem", "Golpe em arco (4 blocos, 120°): 120% do dano da arma × poder em todos os inimigos à frente."),
    "barbarian_rage": ("Fúria Bárbara", "Por 15 s: dano físico recebido -25% e dano corpo a corpo +20%."),
    "barbarian_brutal_leap": ("Salto Brutal", "Salta para a frente; ao aterrissar causa 100% do dano da arma × poder em 3,5 blocos e Lentidão I por 2 s. Sem dano de queda."),
    "barbarian_roar": ("Rugido Intimidador", "Inimigos a até 8 blocos fazem teste de SAB (CD com FOR) ou sofrem Lentidão II e Fraqueza por 6 s."),
    "barbarian_undying_rage": ("Fúria Imortal", "Por 10 s sua vida não cai abaixo de 1 e você entra em Fúria Bárbara; ao final recupera 30% da vida máxima."),
    "wizard_arcane_missile": ("Míssil Arcano", "Projétil arcano que não erra (até 24 blocos): 5 × poder de dano arcano."),
    "wizard_arcane_shield": ("Escudo Arcano", "Absorção por 10 s: 4 pontos por nível do escudo (nível 1 a 4, conforme o poder)."),
    "wizard_fireball": ("Bola de Fogo", "Explode no ponto mirado (até 30 blocos), raio 3: 8 × poder de dano de fogo. Teste de DES (CD com INT) reduz à metade; quem falha queima por 4 s."),
    "wizard_blink": ("Piscar", "Teleporta até 10 blocos na direção da mira, sem atravessar paredes."),
    "wizard_meteor_storm": ("Chuva de Meteoros", "Marca com chamas uma área de 5 blocos no ponto mirado; a partir de 0,6 s caem 5 meteoros ao longo de 3 s, cada um causando 5 × poder de dano de fogo em 2,5 blocos e incendiando por 3 s."),
    "rogue_treacherous_strike": ("Golpe Traiçoeiro", "Golpe no inimigo à frente (até 4,5 blocos): 130% do dano da arma × poder, ou 260% se ele não estiver mirando você (ou se você estiver invisível)."),
    "rogue_smoke_bomb": ("Bomba de Fumaça", "Invisibilidade por 6 s; inimigos a até 5 blocos ficam cegos por 3 s e criaturas a até 16 blocos perdem você de vista."),
    "rogue_shadow_step": ("Passo Sombrio", "Teleporta para as costas do inimigo mirado (até 12 blocos), já virado para ele."),
    "rogue_poison_blades": ("Lâminas Envenenadas", "Por 20 s, seus golpes corpo a corpo aplicam Veneno II por 3 s."),
    "rogue_blade_dance": ("Dança das Lâminas", "Até 6 inimigos a até 8 blocos sofrem 3 golpes (90% do dano da arma × poder cada) ao longo de 0,8 s; você ganha Esquiva por 3 s."),
    "ranger_aimed_shot": ("Tiro Certeiro", "Exige arco ou besta. Dispara uma flecha veloz com 7 × poder de dano."),
    "ranger_hunters_mark": ("Marca do Caçador", "Marca o inimigo mirado (até 32 blocos) por 20 s: ele brilha e recebe +20% do seu dano."),
    "ranger_volley": ("Saraivada", "Exige arco ou besta. Dispara 5 flechas em leque, 4 × poder de dano cada."),
    "ranger_disengage": ("Recuo Ágil", "Salta para trás; inimigos a até 3 blocos sofrem Lentidão II por 3 s. Sem dano de queda."),
    "ranger_arrow_rain": ("Chuva de Flechas", "Exige arco ou besta. Uma área de 5 blocos no ponto mirado (até 40 blocos) recebe 40 flechas ao longo de 3 s, 3 × poder de dano cada."),
    "cleric_sacred_flame": ("Chama Sagrada", "Chama radiante no inimigo mirado (até 20 blocos): 5 × poder de dano, dobrado contra mortos-vivos."),
    "cleric_heal": ("Cura", "Cura o aliado mirado (até 20 blocos) ou você mesmo: 6 × poder de vida."),
    "cleric_word_of_protection": ("Palavra de Proteção", "Aliados a até 8 blocos ganham Resistência I e Absorção I por 10 s."),
    "cleric_turn_undead": ("Expulsar Mortos-Vivos", "Mortos-vivos a até 10 blocos sofrem 10 × poder de dano radiante. Teste de SAB (CD com SAB) reduz à metade; quem falha recebe Lentidão III e Fraqueza por 6 s e deixa de atacar."),
    "cleric_divine_intervention": ("Intervenção Divina", "Aliados a até 15 blocos: vida totalmente restaurada, efeitos negativos removidos e Regeneração II por 10 s."),
    "paladin_divine_smite": ("Golpe Divino", "Golpe no inimigo à frente (até 4,5 blocos): 120% do dano da arma + 4 × poder de dano radiante (dobrado contra mortos-vivos)."),
    "paladin_lay_on_hands": ("Imposição de Mãos", "Cura o aliado mirado (até 20 blocos) ou você mesmo: 8 × poder de vida, removendo Veneno e Murchar."),
    "paladin_aura_of_protection": ("Aura de Proteção", "Por 15 s, a cada segundo, aliados a até 8 blocos recebem Resistência I por 2 s."),
    "paladin_holy_charge": ("Investida Sagrada", "Avança cerca de 7 blocos; inimigos no caminho sofrem 5 × poder de dano radiante (dobrado contra mortos-vivos) e repulsão. Sem dano de queda."),
    "paladin_dawn_avatar": ("Avatar da Aurora", "Por 15 s: Força II, Regeneração I, Resistência II e, a cada segundo, 2 × poder de dano radiante em inimigos a até 4 blocos (dobrado contra mortos-vivos)."),
    "druid_thorns": ("Espinhos", "Espinhos no inimigo mirado (até 20 blocos): 4 × poder de dano natural; teste de FOR (CD com SAB) ou fica enraizado por 2 s."),
    "druid_rejuvenate": ("Rejuvenescer", "Regeneração II por 10 s no aliado mirado (até 20 blocos) ou em você."),
    "druid_bear_form": ("Forma de Urso", "Por 20 s: +8 de vida máxima, +4 de armadura, +3 de dano de ataque e velocidade -10%. As outras habilidades de classe ficam bloqueadas enquanto durar."),
    "druid_entangle": ("Vinhas Constritoras", "Vinhas numa área de 4 blocos no ponto mirado (até 25 blocos): teste de FOR (CD com SAB) ou enraizado por 4 s; 1,5 × poder de dano natural por segundo durante 4 s."),
    "druid_natures_wrath": ("Fúria da Natureza", "Raios atingem até 5 inimigos a até 15 blocos: 9 × poder de dano elétrico cada."),
    "bard_vicious_mockery": ("Zombaria Cruel", "Insulto mágico no inimigo mirado (até 20 blocos): 4 × poder de dano psíquico; teste de SAB (CD com CAR) ou Fraqueza por 4 s."),
    "bard_inspiration": ("Inspiração", "Aliados a até 10 blocos ganham Velocidade I e Força I por 15 s."),
    "bard_song_of_rest": ("Canção de Descanso", "Aliados a até 10 blocos ganham Regeneração I por 10 s; jogadores recuperam 15 de energia."),
    "bard_charm": ("Encantar", "O inimigo mirado (até 20 blocos) faz teste de SAB (CD com CAR); se falhar, fica Encantado por 6 s: não ataca e esquece o alvo."),
    "bard_heroic_symphony": ("Sinfonia Heroica", "Aliados a até 15 blocos: Força I, Velocidade I, Resistência I e Regeneração I por 20 s; outros jogadores recuperam 30 de mana e de energia e você recupera 30 de energia."),
    "blacksmith_field_repair": ("Reparo de Campo", "Repara o item da mão principal em 20% + 1,5% por nível de Ferreiro da durabilidade máxima."),
    "alchemist_improvised_elixir": ("Elixir Improvisado", "Efeito positivo aleatório (Regeneração, Velocidade, Força, Resistência, Resistência ao Fogo, Visão Noturna ou Respiração Aquática) por 30 s + 3 s por nível; nível II a partir do nível 10 de Alquimista."),
    "enchanter_arcane_channel": ("Canalização Arcana", "Recupera 20% + 1% por nível de Encantador da mana máxima."),
    "herbalist_bloom": ("Florescer", "Aplica o efeito de farinha de osso em todas as plantas num raio de 2 + (nível ÷ 5) blocos."),
    "miner_ore_sense": ("Sentido Mineral", "Destaca, só para você, os minérios num raio de 8 + (nível ÷ 2) blocos e indica o mais próximo."),
    "hunter_track": ("Rastrear", "Criaturas não aliadas num raio de 32 + nível blocos brilham por 15 s."),
    "cook_feast": ("Banquete", "Você e os jogadores aliados a até 8 blocos recuperam 6 de fome e saturação."),
    "artisan_portable_bench": ("Bancada Portátil", "Abre uma bancada de trabalho em qualquer lugar."),
}
for k, (name, desc) in abilities.items():
    add(f"ability.alvorecer.{k}", name)
    add(f"ability.alvorecer.{k}.desc", desc)

# ---------------------------------------------------------------- interface
ui = {
    "creation.title": "Criação de Personagem",
    "tab.race": "Raça", "tab.class": "Classe", "tab.profession": "Profissão", "tab.confirm": "Confirmar",
    "confirm": "Confirmar", "back": "Voltar", "next": "Próximo",
    "stat_bonus": "Bônus de atributo:", "scale": "Tamanho:", "active": "Habilidade ativa:",
    "element_choice": "Elemento escolhido:", "element_opposite": " (vulnerável a %s)",
    "resource": "Recurso principal:", "key_stat": "Atributo-chave:", "saves": "Testes de resistência proficientes:",
    "weapons": "Armas proficientes:", "max_armor": "Armadura máxima:", "hit_die": "Vida por nível:",
    "level_at": "Nível %s:", "ultimate_at": "Suprema (nível %s):",
    "secondary_hint": "Você poderá escolher uma profissão secundária no nível configurado pelo servidor (padrão 10); ela recebe metade do XP.",
    "summary": "Resumo", "summary_hint": "Ao confirmar você recebe o Codex do Aventureiro e %s pontos de atributo para distribuir na ficha (tecla K). Trocar estas escolhas depois depende da regra do servidor (Tomo do Recomeço, comando de administrador ou /rpg respec).",
    "cost": "Custo: %s %s", "cooldown": "Recarga: %ss", "mobility": "Mobilidade",
    "sheet.title": "Ficha do Personagem", "level": "Nível %s", "level_short": "Nv %s", "unspent": "Pontos livres: %s",
    "d.crit": "Crítico: %s (×%s)", "d.melee": "Corpo a corpo: +%s  •  Distância: +%s", "d.power": "Poder: ×%s  •  CD das habilidades: %s",
    "d.resist": "Resist. mágica: %s  •  Proficiência: +%s", "d.regen": "Regeneração: %s mana/s  •  %s energia/s",
    "abilities": "Habilidades", "empty_slot": "—", "professions": "Profissões", "choose_secondary": "Escolher profissão secundária",
    "secondary_locked": "Profissão secundária: libera no nível %s", "open_codex": "Codex",
    "codex.title": "Codex do Aventureiro",
}
for k, v in ui.items():
    add(f"ui.alvorecer.{k}", v)

msgs = {
    "no_target": "Nenhum alvo válido.", "no_space": "Não há espaço livre nessa direção.", "no_undead": "Nenhum morto-vivo por perto.",
    "need_bow": "É preciso empunhar um arco ou uma besta.", "nothing_to_repair": "O item na mão não precisa de reparo.",
    "nothing_to_grow": "Nenhuma planta para fazer crescer.", "no_ores": "Nenhum minério por perto.",
    "ores_found": "%s minérios por perto; o mais próximo é %s a %s blocos.",
    "locked": "Desbloqueia no nível %s.", "cooldown": "%s em recarga (%ss).",
    "fail.no_character": "Crie seu personagem primeiro (tecla K).", "fail.empty_slot": "Nenhuma habilidade neste espaço.",
    "fail.no_mana": "Mana insuficiente.", "fail.no_energy": "Energia insuficiente.", "fail.stunned": "Você está atordoado!",
    "fail.bear_form": "Na Forma de Urso você não pode usar essa habilidade.",
    "fail.locked": "Habilidade bloqueada.", "fail.cooldown": "Em recarga.",
    "sneak_attack": "Ataque furtivo!", "resisted": "%s resistiu.", "evaded": "Você se esquivou!",
    "save_success": "Teste de %s bem-sucedido (d20 = %s): efeito evitado.",
    "level_up": "Você alcançou o nível %s! Pontos de atributo livres: %s (tecla K).",
    "ability_unlocked": "Nova habilidade: %s", "secondary_unlocked": "Profissão secundária disponível! Escolha-a na ficha (tecla K).",
    "secondary_chosen": "Profissão secundária escolhida: %s.", "profession_level_up": "%s alcançou o nível %s!",
    "new_biome": "Novo bioma descoberto: +%s XP", "character_created": "Personagem criado: %s %s, %s.",
    "points_hint": "Você tem %s pontos de atributo para distribuir. Abra a ficha com a tecla K.",
    "respec_wait": "Aguarde %s minuto(s) para trocar de novo.", "respec_admin_only": "Neste servidor, só administradores podem refazer personagens.",
}
for k, v in msgs.items():
    add(f"msg.alvorecer.{k}", v)

cmds = {
    "no_character": "%s ainda não criou um personagem.",
    "info": "%s — %s %s, nível %s (%s/%s XP)",
    "info2": "Mana %s/%s • Energia %s/%s • %s nível %s",
    "reset": "Personagem de %s apagado; a tela de criação foi aberta.",
    "setlevel": "%s agora está no nível %s.", "proflevel": "Profissão principal de %s agora no nível %s.",
    "cooldowns": "Recargas de %s zeradas.", "respec_disabled": "A troca livre está desativada (modo atual: %s).",
}
for k, v in cmds.items():
    add(f"cmd.alvorecer.{k}", v)

# ---------------------------------------------------------------- Codex
chapters = {"history": "História", "attributes": "Atributos", "races": "Raças", "classes": "Classes", "professions": "Profissões",
            "formulas": "Fórmulas", "journal": "Diário", "controls": "Controles", "admin": "Administração"}
for k, v in chapters.items():
    add(f"codex.alvorecer.chapter.{k}", v)

add("codex.alvorecer.locked_level", "Este capítulo de informação avançada se revela quando você alcança o nível %s.")
add("codex.alvorecer.history",
    "Antes do primeiro dia havia apenas o Éter, um mar de luz sem forma. Quando a primeira aurora rompeu esse mar, seus fragmentos caíram sobre o mundo e despertaram dons nos povos que os tocaram: a determinação dos humanos, a graça dos elfos, a teimosia dos anões, a sorte dos halflings, a fúria dos orcs, o sangue dos draconatos, o pacto dos tieflings e a engenhosidade dos gnomos.\n\n"
    "Diz-se que um guardião foi deixado para vigiar o lugar onde a primeira luz tocou a terra, e que ele ainda aguarda os aventureiros dignos de enfrentá-lo.\n\n"
    "Este Codex acompanha sua jornada: registra o que você é, o que aprendeu e por onde passou.")
add("codex.alvorecer.attributes",
    "Cada atributo começa em 10, recebe o bônus da raça, +2 no atributo-chave da classe e os pontos que você distribuir. O modificador é (valor − 10) ÷ 2, arredondado para baixo; é ele que entra nas fórmulas.")
add("codex.alvorecer.races", "Toda raça tem uma habilidade ativa (tecla R), vantagens e desvantagens permanentes. As desvantagens alteram o estilo de jogo, mas nenhuma combinação de raça e classe fica inviável.")
add("codex.alvorecer.classes",
    "Cada classe libera habilidades nos níveis 1, 3, 6 e 10 e a habilidade suprema no nível 15 (teclas Z, V, B, N e G). "
    "Usar uma arma fora das proficiências da classe reduz o dano em 25%. Cada peça de armadura mais pesada do que a classe permite reduz a velocidade em 5% e aumenta o custo de mana em 10%.\n\n"
    "Couro e casco de tartaruga são leves; cota de malha e ouro são médias; ferro, diamante e netherita são pesadas.")
add("codex.alvorecer.professions",
    "Profissões evoluem separadamente da classe (níveis 1 a 20) e têm habilidade ativa própria (tecla H). A profissão secundária (tecla U) é liberada no nível de personagem definido pelo servidor (padrão 10) e recebe 50% do XP.\n\n"
    "Proteções contra ciclos infinitos: receitas de um único tipo de ingrediente não dão XP, minério só conta sem Toque Suave, plantas só maduras, filhotes não contam, criaturas de geradores rendem 1/4 e há um limite diário de XP de profissão (padrão 1500 por dia de jogo).")
add("codex.alvorecer.formulas",
    "PROGRESSÃO\n"
    "• XP para o próximo nível = 50 × nível^1,6 (nível máximo 20).\n"
    "• Pontos de atributo: 3 na criação, +1 por nível e +3 nos níveis 4, 8, 12, 16 e 19. Máximo 24 por atributo.\n"
    "• XP por abate de monstro = 1,5 × vida máxima + 2 × armadura (geradores ×0,25). Novo bioma: +30 XP. Humanos: +10%.\n"
    "• XP de profissão para o próximo nível = 20 × nível^1,5.\n\n"
    "HABILIDADES\n"
    "• Proficiência = 2 + (nível − 1) ÷ 4.\n"
    "• CD de resistência = 8 + proficiência + modificador do atributo da habilidade.\n"
    "• Teste de resistência = d20 + modificador (+ proficiência se a classe for proficiente) ≥ CD; 20 natural sempre passa e 1 natural sempre falha. Criaturas somam vida máxima ÷ 10 (máx. +8).\n"
    "• Poder = (1 + 4% × (nível − 1)) × (1 + 6% × modificador) × fator racial da escola × multiplicador do servidor.\n"
    "• Habilidades físicas usam o dano da arma em mãos e depois recebem os bônus normais de golpe (Força, crítico).\n"
    "• Custo final = base × servidor × 1,5 (Anão, mobilidade) × 1,1 (Orc, mana) × (1 + 10% por peça de armadura acima da classe, mana).\n\n"
    "COMBATE\n"
    "• Crítico: 5% + 1% × mod DES (+5% Ladino, +3% Halfling), máximo 50%; multiplicador ×1,5 (Ladino ×1,75). Vale para golpes, flechas e habilidades.\n"
    "• Corpo a corpo × (1 + 4% × mod FOR); à distância × (1 + 4% × mod DES).\n"
    "• Dano recebido (em ordem): Esquiva → elemento racial → veneno racial → resistência mágica (1,5% × mod SAB, máx. 30%) → Pele de Pedra ×0,6 → Fúria Orc ×1,2 → Fúria Bárbara ×0,75 (físico) → armadura vanilla.\n"
    "• Efeitos negativos de outras fontes pedem teste (CD 15, configurável): CON contra veneno, murchar e fome; DES contra lentidão, levitação, atordoamento e enraizamento; SAB contra os demais.\n\n"
    "RECURSOS\n"
    "• Vida extra = (nível − 1) × dado de vida da classe + 2 × mod CON (Elfo −4).\n"
    "• Mana máxima = 100 (conjuradores) ou 40 + 4 ou 2 por nível + 10 × mod do atributo mágico; × raça (Humano 0,9; Orc 0,85; Elfo e Gnomo 1,1) × (1 + 1% por nível de Encantador).\n"
    "• Energia máxima = 100 (marciais) ou 60 + 2 por nível + 5 × mod CON.\n"
    "• Regeneração por segundo: mana 1,5% da máxima × (1 + 10% × mod SAB); energia 5 (Ladino 7), Draconato ×0,75.")
add("codex.alvorecer.journal", "Biomas descobertos: %s. Cada bioma novo concede XP de exploração.")
add("codex.alvorecer.controls", "Todas as teclas podem ser alteradas em Opções → Controles → Crônicas do Alvorecer. A barra de habilidades fica na lateral esquerda da tela e mostra a recarga restante.")
add("codex.alvorecer.admin",
    "CONFIGURAÇÃO\nO arquivo serverconfig/alvorecer-server.toml dentro da pasta do mundo controla: multiplicadores de XP, XP de geradores e de exploração, XP e limite diário de profissões, nível da profissão secundária, custo/recarga/dano das habilidades, regras de PvP (habilidades em jogadores, multiplicador de dano, efeitos de controle), testes de resistência e o modo de troca de personagem (ADMIN_ONLY, ITEM ou FREE) com sua recarga.\n\n"
    "COMANDOS (nível de permissão 2)\n"
    "• /rpg info [jogador]\n• /rpg reset <jogador> — apaga as escolhas e reabre a criação\n• /rpg setlevel <jogador> <nível>\n• /rpg addxp <jogador> <quantidade>\n• /rpg proflevel <jogador> <nível>\n• /rpg cooldowns <jogador>\n\n"
    "JOGADORES\n• /rpg info — sua ficha resumida\n• /rpg respec — só no modo FREE")

os.makedirs(ROOT, exist_ok=True)
for name in ("pt_br.json", "en_us.json"):
    with open(os.path.join(ROOT, name), "w", encoding="utf-8") as f:
        json.dump(L, f, ensure_ascii=False, indent=2, sort_keys=False)
print(len(L), "keys")
