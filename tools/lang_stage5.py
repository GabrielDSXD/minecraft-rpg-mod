"""Textos da Etapa 5 (Reinos do Éter, portal, estruturas). Importado por gen_lang.py."""


def register(add):
    add("block.alvorecer.dawnstone", "Pedra do Alvorecer")
    add("block.alvorecer.dawn_portal", "Portal do Alvorecer")
    add("item.alvorecer.dawn_key", "Chave do Alvorecer")
    add("item.alvorecer.dawn_key.tooltip", "§7Use numa moldura de Pedra do Alvorecer para abrir o Portal do Alvorecer.")
    add("msg.alvorecer.portal_invalid", "A moldura precisa ser um retângulo de Pedra do Alvorecer com interior vazio de pelo menos 2×3.")
    add("msg.alvorecer.portal_dimension", "O Portal do Alvorecer só pode ser aberto no mundo principal ou nos Reinos do Éter.")
    for k, v in {"aether_meadows": "Prados Etéreos", "luminous_forest": "Floresta Luminosa", "crystal_highlands": "Planaltos de Cristal",
                 "ruined_expanse": "Vastidão em Ruínas"}.items():
        add(f"biome.alvorecer.{k}", v)
    add("dimension.alvorecer.aether_realms", "Reinos do Éter")
    add("quest.alvorecer.into_the_aether", "Além do Alvorecer")
    add("quest.alvorecer.into_the_aether.desc", "Construa o Portal do Alvorecer e pise nos Reinos do Éter.")
    add("quest.alvorecer.target.alvorecer.aether_realms", "os Reinos do Éter")
    add("advancement.alvorecer.portal", "A Primeira Luz")
    add("advancement.alvorecer.portal.desc", "Acenda um Portal do Alvorecer.")
    add("advancement.alvorecer.aether", "Reinos do Éter")
    add("advancement.alvorecer.aether.desc", "Entre nos Reinos do Éter.")
    for k, v in {"goblin_camp": "Acampamento Goblin", "watch_tower": "Torre de Vigia", "dawn_shrine": "Santuário do Alvorecer",
                 "aether_ruins": "Ruínas Etéreas", "sky_arena": "Arena Celeste"}.items():
        add(f"structure.alvorecer.{k}", v)
    add("codex.alvorecer.chapter.world", "Mundo e Portal")
    add("codex.alvorecer.world",
        "REINOS DO ÉTER: dimensão de ilhas flutuantes com quatro biomas — Prados Etéreos (flores e fadas), Floresta Luminosa (cerejeiras, "
        "líquen brilhante, lobos arcanos e elementais), Planaltos de Cristal (geodos de ametista, mais Aurorita, elementais, sentinelas e dragonetes) "
        "e Vastidão em Ruínas (esqueletos guerreiros, goblins e dragonetes). Prata Lunar e Aurorita aparecem na pedra comum das ilhas. "
        "Cuidado com a borda das ilhas: abaixo delas só há o vazio.\n\n"
        "PORTAL DO ALVORECER: 1) fabrique Pedra do Alvorecer (8 tijolos de pedra em volta de 1 Gema de Aurorita = 8 blocos); "
        "2) monte uma moldura retangular vertical com interior vazio de 2×3 até 21×21 (cantos opcionais, como o portal do Nether; 10 blocos bastam); "
        "3) use a Chave do Alvorecer (Gema de Aurorita + lingote de ouro + Pó de Fada, 8 usos) na moldura. Só funciona no mundo principal e nos Reinos do Éter. "
        "Fique 3 s dentro do portal para viajar. A escala é 1:1: o portal de destino é procurado num raio de 32 blocos e, se não existir, "
        "é construído com uma plataforma num ponto seguro. Quebrar a moldura apaga o portal.\n\n"
        "ESTRUTURAS DO MUNDO PRINCIPAL: Acampamento Goblin (planícies, florestas e savanas: goblins e baú de saque), Torre de Vigia "
        "(florestas, taigas e colinas: esqueletos guerreiros e baú no topo) e Santuário do Alvorecer (planícies e prados floridos: refúgio seguro com "
        "Mestre da Guilda, Mercador, Sábio, Mural de Missões, Bancada de Ofício e baú). "
        "ESTRUTURAS DO ÉTER: Ruínas Etéreas (esqueletos guerreiros e tesouro) e Arena Celeste (círculo de pedra do alvorecer com 8 pilares e duas sentinelas).\n\n"
        "FREQUÊNCIA: cada estrutura tem uma chance configurável (seção worldgen do arquivo de configuração do servidor, de 0 a 1; 0 desativa).")
