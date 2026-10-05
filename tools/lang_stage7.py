"""Textos da Etapa 7 (Guardião do Primeiro Alvorecer e Coração do Alvorecer). Importado por gen_lang.py."""


def register(add):
    add("entity.alvorecer.dawn_guardian", "Guardião do Primeiro Alvorecer")
    add("item.alvorecer.dawn_guardian_spawn_egg", "Ovo de Guardião do Primeiro Alvorecer")
    add("bestiary.alvorecer.dawn_guardian", "Colosso de pedra dourada que vigia o lugar onde a primeira luz tocou o mundo. Desperta na Arena Celeste.")
    add("bestiary.alvorecer.dawn_guardian.advanced",
        "Vida 800 (+50% por jogador extra) • Armadura 10. GOLPE NO CHÃO (anel de chamas, 1,25 s): 16 de dano em 6 blocos — saia do anel ou pule. "
        "RAIO: mira por 1 s (linha branca), trava a mira (linha de fogo) e dispara 14 de dano radiante; blocos param o raio. "
        "ANEL (fase 2+): onda de fogo que se expande até 20 blocos; 12 de dano em quem estiver no chão quando ela passa — pule. "
        "CENTELHAS: invoca elementais de fogo. DESCARGA (fase 3, a cada 25 s): 5 s de carga no centro da arena e um clarão que atinge "
        "quem estiver à vista (40 de dano) — esconda-se atrás de um pilar. Invulnerável nas transições e durante a carga. "
        "Drops: Essência do Alvorecer, Gemas de Aurorita, Coração de Sentinela, diamantes, Chave do Alvorecer (50%); cada participante recebe o Coração do Alvorecer.")
    add("item.alvorecer.heart_of_dawn", "Coração do Alvorecer")
    add("item.alvorecer.heart_of_dawn.lore", "A primeira luz do mundo ainda pulsa dentro dele.")
    add("item.alvorecer.heart_of_dawn.passive", "Último Alvorecer: no inventário, impede uma morte e devolve metade da vida (recarga de %s min).")
    add("item.alvorecer.heart_of_dawn.recharging", "Último Alvorecer recarregando: %s min")
    add("item.alvorecer.heart_of_dawn.ready", "Último Alvorecer pronto")
    add("item.alvorecer.dawn_sigil", "Sigilo do Primeiro Alvorecer")
    add("item.alvorecer.dawn_essence", "Essência do Alvorecer")
    add("block.alvorecer.dawn_altar", "Altar do Alvorecer")
    add("ability.alvorecer.item_dawn_heart", "Aurora Renascente")
    add("ability.alvorecer.item_dawn_heart.desc",
        "Cura você e os membros do grupo a até 12 blocos em 40% da vida máxima, com Resistência II (6 s) e Regeneração II (5 s); "
        "inimigos a até 6 blocos sofrem 12 × poder de dano radiante e são repelidos. Recarga 5 min.")
    for k, v in {
        "guardian_phase": "O Guardião ruge e muda de forma!",
        "guardian_phase_name.2": "Fúria Solar",
        "guardian_phase_name.3": "Último Clarão",
        "guardian_hide": "ESCONDA-SE ATRÁS DOS PILARES!",
        "guardian_discharge": "A Descarga do Primeiro Alvorecer vem aí: %s s! Fique fora da vista do Guardião!",
        "guardian_sheltered": "O pilar te protegeu da Descarga.",
        "guardian_summon": "O Sigilo arde no altar... algo antigo desperta!",
        "guardian_awake": "O Guardião já está desperto.",
        "guardian_reward_heart": "O Guardião caiu! Você recebeu o Coração do Alvorecer.",
        "guardian_reward_essence": "O Guardião caiu! Você já tem um Coração do Alvorecer e recebeu 3 Essências do Alvorecer.",
        "heart_revive": "O Coração do Alvorecer recusou a sua morte!",
    }.items():
        add(f"msg.alvorecer.{k}", v)
    for k, v in {"ambient": "Guardião vibra", "hurt": "Guardião ressoa", "death": "Guardião se desfaz em luz", "roar": "Guardião ruge",
                 "slam": "Guardião golpeia o chão", "beam_charge": "Guardião carrega o raio", "beam": "Raio de energia",
                 "ring": "Anel de fogo se forma", "summon": "Centelhas são invocadas", "discharge_charge": "A Descarga se acumula",
                 "discharge": "Descarga do Primeiro Alvorecer"}.items():
        add(f"subtitles.alvorecer.dawn_guardian.{k}", v)
    add("quest.alvorecer.first_dawn", "O Primeiro Alvorecer")
    add("quest.alvorecer.first_dawn.desc", "Forje o Sigilo, desperte o Guardião no altar da Arena Celeste e derrote-o.")
    add("advancement.alvorecer.guardian", "O Primeiro Alvorecer")
    add("advancement.alvorecer.guardian.desc", "Derrote o Guardião do Primeiro Alvorecer.")
    add("codex.alvorecer.chapter.bosses", "Chefe: o Guardião")
    add("codex.alvorecer.bosses",
        "INVOCAÇÃO: cada Relicário de masmorra dá 1 Fragmento do Sigilo a cada jogador. 4 Fragmentos + 1 Gema de Aurorita (em cruz) = "
        "Sigilo do Primeiro Alvorecer; para revanches, 6 Essências do Alvorecer + 2 Fragmentos + 1 Gema também fazem um Sigilo. Use o Sigilo no Altar do Alvorecer, "
        "no centro da Arena Celeste (Reinos do Éter). Após 3 s de luz o Guardião surge. A vida dele escala com os jogadores na arena "
        "(boss.healthPerExtraPlayer). Se todos saírem da arena por 30 s ele some e devolve o Sigilo.\n\n"
        "FASE 1 (100–66%): GOLPE NO CHÃO — os braços sobem e um anel de chamas marca 6 blocos; saia ou pule. RAIO — uma linha branca segue você "
        "por 1 s, vira linha de fogo (mira travada) e dispara: saia da linha ou ponha um bloco no caminho.\n"
        "FASE 2 (66–33%): transição com rugido (3 s invulnerável) e Centelhas (elementais de fogo). ANÉIS: a onda de fogo se expande pelo chão; "
        "pule quando ela chegar. Novas Centelhas a cada 30 s.\n"
        "FASE 3 (abaixo de 33%): tudo mais rápido. A cada 25 s, a DESCARGA DO PRIMEIRO ALVORECER: o Guardião flutua no centro, a barra fica "
        "vermelha e uma contagem de 5 s aparece. Quem continuar com linha de visão até ele recebe 40 de dano (boss.dischargeDamage); quem estiver "
        "atrás de um dos 8 pilares não sofre nada. Ele é invulnerável durante a carga.\n\n"
        "RECOMPENSAS: cada participante (quem causou dano ou estava na arena) recebe 3000 XP de classe (boss.xpPerPlayer) e o Coração do Alvorecer "
        "(quem já tem um recebe 3 Essências). Drops: Essência do Alvorecer, Gemas de Aurorita, Coração de Sentinela, diamantes e Chave do Alvorecer.\n\n"
        "CORAÇÃO DO ALVORECER (lendário, nível 15): na mão, +15% de cura recebida e +10% de resistência mágica. AURORA RENASCENTE (botão direito, "
        "recarga 5 min): cura você e o grupo próximo e repele inimigos. ÚLTIMO ALVORECER (passiva, em qualquer lugar do inventário): impede uma "
        "morte e devolve metade da vida; recarga de 20 min (boss.heartReviveCooldownMinutes). Abaixo do nível 15 nada funciona.\n\n"
        "ADMINISTRAÇÃO: ovo do Guardião ou o Cetro do Bestiário invocam-no em qualquer lugar (sem pilares, qualquer bloco serve de cobertura).")
