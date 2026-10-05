"""Textos da Etapa 2 (talentos, especializações, missões, grupos, conquistas). Importado por gen_lang.py."""


def register(add):
    add("key.alvorecer.talents", "Talentos")
    add("ui.alvorecer.talents.title", "Talentos")
    add("ui.alvorecer.talents.points", "Pontos de talento: %s (1 a cada nível par)")
    add("ui.alvorecer.spec", "Especialização")
    add("ui.alvorecer.spec_locked", "Especialização disponível no nível %s.")
    add("ui.alvorecer.spec_choose", "Escolha sua especialização (passe o mouse para ver os bônus):")
    add("ui.alvorecer.tier", "Tier %s — nível %s, %s pontos nos tiers anteriores")
    add("ui.alvorecer.per_rank", " por rank")
    add("ui.alvorecer.open_talents", "Talentos (%s)")
    add("ui.alvorecer.party", "Grupo")
    add("msg.alvorecer.talent_point", "Você ganhou um ponto de talento! Abra os talentos com a tecla Y.")
    add("msg.alvorecer.spec_unlocked", "Especialização liberada! Escolha-a nos talentos (tecla Y).")
    add("msg.alvorecer.spec_chosen", "Especialização escolhida: %s.")
    for k, v in {"no_character": "Crie seu personagem primeiro.", "wrong_class": "Esse talento é de outra classe.",
                 "max_rank": "Talento já no rank máximo.", "no_points": "Sem pontos de talento.",
                 "tier_level": "Seu nível ainda não libera esse tier.", "tier_points": "Gaste mais pontos nos tiers anteriores.",
                 "unknown": "Talento desconhecido."}.items():
        add(f"msg.alvorecer.talent.{k}", v)
    add("cmd.alvorecer.resettalents", "Talentos e especialização de %s foram zerados.")

    bonus = {
        "str": "+%s de Força", "dex": "+%s de Destreza", "con": "+%s de Constituição", "int": "+%s de Inteligência",
        "wis": "+%s de Sabedoria", "cha": "+%s de Carisma",
        "power_physical": "+%s de poder físico", "power_arcane": "+%s de poder arcano", "power_elemental": "+%s de poder elemental",
        "power_divine": "+%s de poder divino", "power_nature": "+%s de poder natural", "power_shadow": "+%s de poder sombrio",
        "melee": "+%s de dano corpo a corpo", "ranged": "+%s de dano à distância", "crit_chance": "+%s de chance de crítico",
        "crit_damage": "+%s no multiplicador de crítico", "cooldown": "-%s de recarga", "cost": "-%s de custo",
        "max_mana": "+%s de mana máxima", "max_energy": "+%s de energia máxima", "regen": "+%s de regeneração de mana e energia",
        "max_health": "+%s de vida máxima", "healing": "+%s de cura realizada", "damage_taken": "-%s de dano recebido",
        "magic_resist": "+%s de resistência mágica", "speed": "+%s de velocidade", "save_dc": "+%s na CD das habilidades",
        "sneak_attack": "+%s no multiplicador de ataque furtivo",
    }
    for k, v in bonus.items():
        add(f"bonus.alvorecer.{k}", v)

    talents = {
        "warrior_weapon_training": ("Treinamento com Armas", None), "warrior_tough_hide": ("Pele Curtida", None),
        "warrior_endless_breath": ("Fôlego Inesgotável", None), "warrior_tactics": ("Táticas de Campo", None),
        "warrior_unbreakable": ("Inquebrável", "Abaixo de 30% de vida, você recebe 25% menos dano."),
        "warrior_executioner": ("Executor", "+30% de dano contra alvos abaixo de 30% de vida."),
        "barbarian_bloodthirst": ("Sede de Sangue", None), "barbarian_thick_skin": ("Couro Grosso", None),
        "barbarian_rising_fury": ("Fúria Crescente", None), "barbarian_brutality": ("Brutalidade", None),
        "barbarian_unstoppable": ("Imparável", "Durante a Fúria Bárbara você é imune a lentidão e enraizamento."),
        "barbarian_carnage": ("Massacre", "Abater um monstro restaura 15 de energia."),
        "wizard_arcane_study": ("Estudo Arcano", None), "wizard_elemental_affinity": ("Afinidade Elemental", None),
        "wizard_mana_reserve": ("Reserva de Mana", None), "wizard_focus": ("Concentração", None),
        "wizard_overload": ("Sobrecarga", "Acertos críticos mágicos devolvem 10 de mana."),
        "wizard_sharp_mind": ("Mente Afiada", "Seus feitiços ficam mais difíceis de resistir."),
        "rogue_sharp_blades": ("Lâminas Afiadas", None), "rogue_reflexes": ("Reflexos", None),
        "rogue_precise_strikes": ("Golpes Precisos", None), "rogue_thief_breath": ("Fôlego de Ladrão", None),
        "rogue_assassinate": ("Assassinar", "+50% de dano corpo a corpo contra alvos com a vida cheia."),
        "rogue_elusive": ("Sombra Fugidia", "Ao cair abaixo de 30% de vida, ganha Esquiva por 3 s (recarga de 60 s)."),
        "ranger_eagle_eye": ("Olho de Águia", None), "ranger_light_steps": ("Passos Leves", None),
        "ranger_hunt": ("Caçada", None), "ranger_deep_quiver": ("Aljava Funda", None),
        "ranger_deadly_shot": ("Tiro Mortal", "A Marca do Caçador passa a dar +50% de dano (em vez de +20%)."),
        "ranger_instinct": ("Instinto Predador", "Seus sentidos aguçados encontram pontos fracos."),
        "cleric_faith": ("Fé", None), "cleric_healing_hands": ("Mãos Curadoras", None),
        "cleric_devotion": ("Devoção", None), "cleric_aegis": ("Égide", None),
        "cleric_lingering_light": ("Luz Persistente", "Suas curas também concedem Regeneração I por 4 s."),
        "cleric_judgment": ("Julgamento", "+50% de dano radiante contra mortos-vivos."),
        "paladin_zeal": ("Zelo", None), "paladin_oath": ("Juramento", None),
        "paladin_holy_plate": ("Couraça Sagrada", None), "paladin_blessing": ("Bênção", None),
        "paladin_shield_of_faith": ("Escudo da Fé", "A Aura de Proteção concede Resistência II em vez de I."),
        "paladin_burning_smite": ("Castigo Ardente", "O Golpe Divino incendeia o alvo por 3 s."),
        "druid_deep_roots": ("Raízes Profundas", None), "druid_wild_vigor": ("Vigor Selvagem", None),
        "druid_cycle_of_life": ("Ciclo da Vida", None), "druid_harmony": ("Harmonia", None),
        "druid_ancestral_bear": ("Urso Ancestral", "A Forma de Urso também concede Resistência I."),
        "druid_venom_thorns": ("Espinhos Venenosos", "Espinhos também aplicam Veneno I por 4 s."),
        "bard_sharp_verse": ("Verso Afiado", None), "bard_natural_charm": ("Carisma Natural", None),
        "bard_repertoire": ("Repertório", None), "bard_restoring_melody": ("Melodia Restauradora", None),
        "bard_crescendo": ("Crescendo", "Inspiração e Sinfonia Heroica duram 50% a mais."),
        "bard_dissonance": ("Dissonância", "Seus encantamentos ficam mais difíceis de resistir."),
    }
    for k, (name, desc) in talents.items():
        add(f"talent.alvorecer.{k}", name)
        if desc:
            add(f"talent.alvorecer.{k}.desc", desc)

    specs = {
        "warrior_champion": ("Campeão", "Mestre do golpe decisivo: críticos mais frequentes e mais fortes."),
        "warrior_defender": ("Defensor", "Muralha do grupo: menos dano recebido e mais vida."),
        "barbarian_berserker": ("Berserker", "Fúria sem freio: mais dano e mais críticos."),
        "barbarian_totem": ("Guerreiro Totêmico", "Espíritos ancestrais protegem contra magia."),
        "wizard_evoker": ("Evocador", "Especialista em fogo, gelo e raio."),
        "wizard_arcanist": ("Arcanista", "Domínio da magia arcana pura, com feitiços mais baratos."),
        "rogue_assassin": ("Assassino", "Ataques furtivos e críticos devastadores."),
        "rogue_trickster": ("Trapaceiro", "Mobilidade e habilidades recarregando mais rápido."),
        "ranger_marksman": ("Atirador", "Precisão letal à distância."),
        "ranger_scout": ("Batedor", "Velocidade e recargas menores para dominar o terreno."),
        "cleric_life": ("Domínio da Vida", "O curandeiro definitivo."),
        "cleric_war": ("Domínio da Guerra", "Fé armada: mais dano radiante e corpo a corpo."),
        "paladin_devotion": ("Juramento de Devoção", "Protetor dos aliados: mais cura e resistência mágica."),
        "paladin_vengeance": ("Juramento de Vingança", "Punição implacável: mais dano físico e radiante."),
        "druid_moon": ("Círculo da Lua", "Resistente como as feras: menos dano e mais vida."),
        "druid_land": ("Círculo da Terra", "Magia natural mais forte e mais barata."),
        "bard_lore": ("Colégio do Saber", "Magia mais poderosa e difícil de resistir."),
        "bard_valor": ("Colégio da Bravura", "O bardo vai à linha de frente."),
    }
    for k, (name, desc) in specs.items():
        add(f"spec.alvorecer.{k}", name)
        add(f"spec.alvorecer.{k}.desc", desc)

    # ------------------------------------------------------------ missões
    add("block.alvorecer.quest_board", "Mural de Missões")
    for k, v in {"accept": "Aceitar", "complete": "Entregar", "abandon": "Abandonar", "active": "Missões ativas: %s",
                 "daily": " • diária", "min_level": "Requer nível %s.", "cooldown": "Disponível de novo em %s min.",
                 "reward": "Recompensa: %s XP", "reward_prof": " + %s XP de profissão"}.items():
        add(f"ui.alvorecer.quest.{k}", v)
    for k, v in {"combat": "Combate", "exploration": "Exploração", "gathering": "Coleta", "profession": "Profissão",
                 "progression": "Progressão"}.items():
        add(f"quest.alvorecer.category.{k}", v)
    for k, v in {"locked": "Bloqueada", "available": "Disponível", "active": "Em andamento", "ready": "Pronta para entregar",
                 "cooldown": "Em espera", "done": "Concluída"}.items():
        add(f"quest.alvorecer.status.{k}", v)
    for k, v in {"kill": "Abater %s: %s/%s", "gather": "Entregar %s: %s/%s", "reach_level": "Alcançar o nível%s %s/%s",
                 "profession_level": "Profissão principal no nível%s %s/%s", "biomes": "Biomas descobertos%s: %s/%s",
                 "visit_dimension": "Visitar %s: %s/%s"}.items():
        add(f"quest.alvorecer.objective.{k}", v)
    add("quest.alvorecer.target.minecraft.undead", "mortos-vivos")
    add("quest.alvorecer.target.minecraft.logs", "toras de madeira")
    add("quest.alvorecer.target.minecraft.the_nether", "o Nether")
    add("quest.alvorecer.target.minecraft.the_end", "o End")
    for k, v in {"too_many": "Você já tem %s missões ativas.", "accepted": "Missão aceita: %s",
                 "ready": "Missão pronta para entregar: %s", "completed": "Missão concluída: %s (+%s XP)"}.items():
        add(f"quest.alvorecer.msg.{k}", v)
    quests = {
        "zombie_hunt": ("Caça aos Zumbis", "Os mortos andam à noite perto das vilas. Reduza o número deles."),
        "bone_breakers": ("Quebra-Ossos", "Arqueiros esqueléticos ameaçam as estradas. Derrube-os."),
        "web_cutters": ("Cortadores de Teias", "Aranhas infestam as cavernas próximas."),
        "creeper_watch": ("Vigília Silenciosa", "Elimine creepers antes que destruam mais construções."),
        "rest_for_the_dead": ("Descanso aos Mortos", "Dê descanso a qualquer tipo de morto-vivo."),
        "void_walkers": ("Andarilhos do Vazio", "Endermen carregam fragmentos do vazio. Derrote-os."),
        "flame_hunters": ("Caçadores de Chamas", "Blazes guardam as fortalezas do Nether."),
        "first_steps": ("Primeiros Passos", "Conheça o mundo ao redor da sua casa."),
        "wanderer": ("Andarilho", "Viaje para longe e descubra muitas terras."),
        "into_the_nether": ("Portas do Inferno", "Atravesse um portal para o Nether."),
        "the_end": ("O Fim", "Encontre o caminho até o End."),
        "lumber": ("Madeira para a Vila", "Entregue toras de madeira para reparos."),
        "coal_supply": ("Carvão para as Forjas", "As forjas precisam de carvão."),
        "iron_supply": ("Ferro para as Armas", "Entregue lingotes de ferro aos ferreiros."),
        "harvest": ("Colheita", "Entregue trigo para os celeiros."),
        "apprentice": ("Aprendiz de Ofício", "Evolua sua profissão principal."),
        "master_craft": ("Mestre de Ofício", "Torne-se um mestre na sua profissão principal."),
        "adventurer": ("Aventureiro", "Ganhe experiência em combate e exploração."),
        "veteran": ("Veterano", "Prove seu valor alcançando o nível 10."),
        "hero": ("Herói", "Alcance o nível 15 e seja reconhecido como herói."),
    }
    for k, (name, desc) in quests.items():
        add(f"quest.alvorecer.{k}", name)
        add(f"quest.alvorecer.{k}.desc", desc)

    # ------------------------------------------------------------ grupo
    party = {
        "already_in_party": "%s já está em um grupo.", "not_leader": "Só o líder do grupo pode fazer isso.",
        "full": "O grupo está cheio.", "invite_sent": "Convite enviado para %s.",
        "invite_received": "%s convidou você para um grupo. Use /grupo aceitar ou /grupo recusar (expira em 60 s).",
        "no_invite": "Você não tem convite pendente.", "joined": "%s entrou no grupo.", "declined": "Convite recusado.",
        "left_self": "Você saiu do grupo.", "left": "%s saiu do grupo.", "disbanded": "O grupo foi desfeito.",
        "loot_changed": "Regra de saque: %s.", "none": "Você não está em um grupo.", "list_header": "Membros do grupo (saque: %s):",
        "loot.free": "livre", "loot.round_robin": "alternado", "loot.random": "aleatório",
    }
    for k, v in party.items():
        add(f"party.alvorecer.{k}", v)

    # ------------------------------------------------------------ conquistas
    ach = {
        "root": ("Crônicas do Alvorecer", "Crie seu personagem."),
        "level_5": ("Iniciado", "Alcance o nível 5."), "level_10": ("Experiente", "Alcance o nível 10."),
        "level_15": ("Lendário em Formação", "Alcance o nível 15."), "level_20": ("Ápice", "Alcance o nível 20."),
        "specialization": ("Caminho Escolhido", "Escolha uma especialização."),
        "capstone": ("Maestria", "Aprenda um talento do tier 3."),
        "first_ultimate": ("Poder Supremo", "Use sua habilidade suprema."),
        "profession_10": ("Artífice", "Alcance o nível 10 em uma profissão."),
        "profession_20": ("Grão-Mestre", "Alcance o nível 20 em uma profissão."),
        "biomes_10": ("Explorador", "Descubra 10 biomas."), "biomes_30": ("Cartógrafo", "Descubra 30 biomas."),
        "first_quest": ("Primeira Missão", "Conclua uma missão do Mural."),
        "quests_10": ("Herói da Vila", "Conclua 10 missões."),
        "party": ("Companheiros", "Forme um grupo com outro jogador."),
    }
    for k, (name, desc) in ach.items():
        add(f"advancement.alvorecer.{k}", name)
        add(f"advancement.alvorecer.{k}.desc", desc)

    # ------------------------------------------------------------ Codex
    for k, v in {"talents": "Talentos", "quests": "Missões", "achievements": "Conquistas", "party": "Grupos"}.items():
        add(f"codex.alvorecer.chapter.{k}", v)
    add("codex.alvorecer.talents",
        "Você ganha 1 ponto de talento a cada nível par (10 no nível 20). Cada classe tem 3 tiers com 2 talentos: o tier 1 abre no nível 1, "
        "o tier 2 no nível 8 (com 3 pontos gastos no tier 1) e o tier 3 no nível 14 (com 6 pontos gastos antes). Talentos dos tiers 1 e 2 têm 3 ranks; "
        "os do tier 3 têm 1 rank e um efeito especial. Como só existem 10 pontos para 14 ranks, é preciso escolher. "
        "No nível 5 escolha uma especialização (2 por classe). Talentos e especialização só são refeitos com o Tomo do Recomeço ou por um administrador.")
    add("codex.alvorecer.quests", "Missões concluídas: %s. Aceite e entregue missões no Mural de Missões; o progresso das ativas aparece aqui.")
    add("codex.alvorecer.quests_none", "Nenhuma missão ativa.")
    add("codex.alvorecer.quests_done", "Já concluídas:")
    add("codex.alvorecer.achievements", "Conquistas: %s de %s. Elas também aparecem na aba de Progressos do jogo.")
    add("codex.alvorecer.party",
        "Comandos: /grupo convidar <jogador>, /grupo aceitar, /grupo recusar, /grupo sair, /grupo expulsar <jogador>, /grupo lista e "
        "/grupo saque <livre|alternado|aleatorio> (só o líder).\n\n"
        "XP COMPARTILHADO: o XP de cada abate é dividido entre os membros a até 48 blocos no mesmo mundo, com +10% por membro extra "
        "(XP de cada um = XP × (1 + 0,1 × (n − 1)) ÷ n). O crédito de abate das missões vale para todos os membros próximos.\n\n"
        "SAQUE: livre (quem pegar primeiro), alternado (cada item vai para o próximo membro da fila) ou aleatório. "
        "Membros do mesmo grupo não se ferem e as habilidades os tratam como aliados (curas e bênçãos os alcançam).\n\n"
        "Valores configuráveis em serverconfig (seção party).")
