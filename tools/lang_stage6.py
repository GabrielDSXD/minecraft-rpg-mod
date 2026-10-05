"""Textos da Etapa 6 (masmorras). Importado por gen_lang.py."""


def register(add):
    for k, v in {"dungeon_seal": "Selo da Masmorra", "sealed_door": "Portão Selado", "dungeon_rune": "Runa Giratória",
                 "rune_glyph": "Glifo Rúnico", "spike_trap": "Placa de Espinhos", "dungeon_vault": "Relicário"}.items():
        add(f"block.alvorecer.{k}", v)
    add("item.alvorecer.dawn_sigil_fragment", "Fragmento do Sigilo")
    add("entity.alvorecer.dungeon_champion", "Campeão da Masmorra")
    add("structure.alvorecer.dungeon", "Masmorra")
    add("structure.alvorecer.aether_dungeon", "Masmorra Etérea")
    add("msg.alvorecer.dungeon_challenge", "O selo desperta! Desafio para %s aventureiro(s).")
    add("msg.alvorecer.dungeon_wave", "Onda %s de %s")
    add("msg.alvorecer.dungeon_cleared", "Sala vencida! Os portões se abrem.")
    add("msg.alvorecer.dungeon_champion_down", "O campeão caiu! O Relicário aguarda.")
    add("msg.alvorecer.vault_claimed", "Você já recebeu sua parte deste Relicário.")
    add("msg.alvorecer.vault_reward", "Você recebeu sua parte do Relicário.")
    add("quest.alvorecer.objective.clear_dungeon", "Derrotar %s: %s/%s")
    add("quest.alvorecer.target.alvorecer.dungeon_champion", "um Campeão da Masmorra")
    add("quest.alvorecer.dungeon_delver", "Explorador das Profundezas")
    add("quest.alvorecer.dungeon_delver.desc", "Encontre uma masmorra, atravesse suas salas e derrote o campeão.")
    add("advancement.alvorecer.dungeon", "Campeão Derrubado")
    add("advancement.alvorecer.dungeon.desc", "Derrote o campeão de uma masmorra.")
    add("codex.alvorecer.chapter.dungeons", "Masmorras")
    add("codex.alvorecer.dungeons",
        "ENTRADA: arco de pedra musgosa com um poço e escada de mão que desce 24 blocos. Aparece em florestas, taigas, colinas, "
        "savanas, planícies, desertos e montanhas, e também nas ilhas dos Reinos do Éter (Masmorra Etérea, com criaturas do Éter "
        "e recompensas melhores). Frequência: worldgen.dungeon.\n\n"
        "SALAS (em sequência): Início → Combate → Armadilha → Enigma → Combate → Campeão → Tesouro. Os Portões Selados depois do "
        "combate, do enigma e do campeão só abrem quando a sala é vencida; dá sempre para voltar pelo caminho já aberto.\n\n"
        "COMBATE: chegar a 7 blocos do Selo da Masmorra desperta o desafio, com 2 ondas. "
        "ARMADILHA: o piso é de Placas de Espinhos; pisadas, estalam e brilham por 0,6 s e então perfuram (6 de dano). "
        "Continue andando, ou atravesse agachado (não dispara). Cuidado com as teias. "
        "ENIGMA: a parede norte tem 4 Glifos Rúnicos; gire a Runa embaixo de cada glifo (usar) até mostrar o mesmo símbolo "
        "(sol, lua, estrela, olho). Os símbolos mudam em cada masmorra. "
        "CAMPEÃO: uma Sentinela de Pedra brilhante com o triplo de vida, acompanhada de criaturas.\n\n"
        "GRUPO E DIFICULDADE: ao despertar, o selo conta os jogadores a até 24 blocos. Cada jogador além do primeiro adiciona "
        "2 criaturas por onda, +35% de vida e +15% de dano (dungeons.* na configuração). Sozinho, cada onda tem 3 criaturas "
        "sem bônus. Se todos se afastarem mais de 32 blocos por 30 s, o desafio recomeça.\n\n"
        "RECOMPENSAS: o XP dos abates segue a regra de divisão do grupo. Vencer uma sala dá XP de conclusão a cada jogador "
        "presente, sem divisão (combate 80, enigma 40, campeão 400). No Tesouro, o Relicário dá a cada jogador a sua própria "
        "parte uma única vez (sempre com 1 Fragmento do Sigilo), e há baús comuns para o grupo. Os blocos da masmorra não podem "
        "ser quebrados fora do modo criativo.\n\n"
        "ADMINISTRAÇÃO: /place structure alvorecer:dungeon (ou alvorecer:aether_dungeon) cria uma masmorra na posição atual.")
