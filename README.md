# Crônicas do Alvorecer

Mod de RPG de fantasia para **Minecraft Java 1.21.1** com **NeoForge 21.1.x** (testado com 21.1.255). Funciona em singleplayer e em servidor dedicado; o mod precisa estar instalado no cliente e no servidor.

A entrega é feita em etapas (status abaixo). Concluídas: **Etapa 1** (base jogável), **Etapa 2** (talentos, grupos, missões e conquistas), **Etapa 3** (equipamentos), **Etapa 4** (criaturas e NPCs) e **Etapa 5** (Reinos do Éter, portal e estruturas).

## Compilar

Requisitos: um JDK 17 ou mais novo para rodar o Gradle. O JDK 21 usado para compilar o mod é baixado automaticamente (toolchain do Gradle).

```bash
./gradlew build                 # compila, roda os testes JUnit e gera build/libs/alvorecer-<versão>.jar
./gradlew runGameTestServer     # sobe um servidor e roda os GameTests do mod
./gradlew runClient             # cliente de desenvolvimento
./gradlew runClient -PquickPlay=NomeDoMundo   # entra direto num mundo salvo em run/saves
./gradlew runServer             # servidor dedicado de desenvolvimento (pasta run/)
./gradlew runClient -PquickPlayServer=localhost  # conecta direto no servidor (use online-mode=false em run/server.properties)
```

## Instalar

1. Instale o NeoForge 21.1.255 (ou mais novo da linha 21.1) para Minecraft 1.21.1.
2. Copie o jar de `build/libs/` para a pasta `mods` do cliente e do servidor.

## Jogar

Na primeira entrada no mundo abre a criação de personagem (raça → classe → profissão → confirmar). O Draconato escolhe também o elemento. Ao confirmar você recebe o **Codex do Aventureiro** e 3 pontos de atributo.

| Tecla padrão | Ação |
|---|---|
| R | Habilidade racial |
| Z, V, B, N | Habilidades de classe (níveis 1, 3, 6, 10) |
| G | Habilidade suprema (nível 15) |
| H / U | Habilidade da profissão principal / secundária |
| K | Ficha do personagem (distribuir pontos, escolher profissão secundária) |
| Y | Talentos e especialização |
| J | Codex do Aventureiro |

Todas podem ser trocadas em Opções → Controles → Crônicas do Alvorecer. As fórmulas completas estão no Codex (capítulo Fórmulas, liberado no nível 3).

**Talentos:** 1 ponto a cada nível par; 3 tiers por classe (níveis 1, 8 e 14) com 2 talentos cada; especialização no nível 5.
**Missões:** fabrique o **Mural de Missões** (toras nos cantos, papel nas laterais, livro no centro) e clique nele para aceitar e entregar missões de combate, exploração, coleta, profissão e progressão.
**Grupos:** `/grupo convidar <jogador>`, `/grupo aceitar`, `/grupo sair`, `/grupo expulsar <jogador>`, `/grupo lista`, `/grupo saque <livre|alternado|aleatorio>`.

## Administração

Configuração por mundo em `serverconfig/alvorecer-server.toml` (sincronizada com os clientes): multiplicadores de XP, XP de geradores e de exploração, XP e limite diário de profissões, nível da profissão secundária, custo/recarga/dano das habilidades, PvP de habilidades (liga/desliga, multiplicador de dano, efeitos de controle), testes de resistência, grupos (tamanho, divisão de XP, bônus, alcance, fogo amigo), máximo de missões ativas e o modo de troca de personagem (`ADMIN_ONLY`, `ITEM` com o Tomo do Recomeço, ou `FREE` com `/rpg respec`) com recarga.

Comandos (permissão 2): `/rpg info [jogador]`, `/rpg reset <jogador>`, `/rpg setlevel <jogador> <nível>`, `/rpg addxp <jogador> <qtd>`, `/rpg proflevel <jogador> <nível>`, `/rpg resettalents <jogador>`, `/rpg cooldowns <jogador>`. Jogadores: `/rpg info`, `/rpg respec` (modo FREE).

## Status por etapa

### Etapa 1 — base jogável (concluída)
- Ficha persistida no jogador (anexo de dados NeoForge), copiada na morte e sincronizada só com o dono.
- 8 raças (habilidade ativa, vantagens e desvantagens com valores, duração e recarga), 9 classes (recurso principal, proficiências com penalidades, 4 habilidades progressivas + suprema), 8 profissões (XP por atividade, bônus por nível, habilidade ativa, profissão secundária). 61 habilidades validadas no servidor.
- Atributos, dano físico/mágico (9 tipos), resistências, críticos, efeitos de controle e testes de resistência d20; níveis 1–20; anti-farm.
- HUD, ficha, criação, Codex, Tomo do Recomeço, comandos e configuração. Textos em pt-BR.

### Etapa 2 — talentos, grupos, missões e conquistas (concluída)
- Árvores de talentos (9 classes × 6 talentos em 3 tiers, 14 ranks para 10 pontos) com 18 efeitos especiais de tier 3, e 18 especializações (2 por classe, nível 5). Bônus entram em todas as fórmulas (poder, crítico, custo, recarga, cura, resistência, vida, velocidade, CD).
- Grupos com convite, líder, divisão de XP com bônus por membro, crédito de missão compartilhado, regras de saque (livre/alternado/aleatório), aliados para habilidades, sem fogo amigo, painel no HUD.
- 20 missões (combate, exploração, coleta, profissão, progressão; diárias repetíveis) no Mural de Missões, com diário no Codex.
- 15 conquistas (aba própria em Progressos e capítulo no Codex).

### Etapa 3 — equipamentos (concluída)
- Materiais com minério gerado no mundo: Prata Lunar (entre −48 e 48) e Aurorita (ardósia abaixo de −16).
- Ferramentas completas de Prata Lunar e Aurorita; adagas, espadas, martelos de guerra, lanças (+1 de alcance), arcos e bestas (multiplicam o dano das flechas), escudos, cajados e varinhas.
- Raridades (Comum a Lendário), nível exigido (abaixo dele: sem bônus, metade do dano, sem habilidade), bônus ao equipar e tooltips completos.
- 4 conjuntos de armadura (leve, leve, média, pesada) com bônus por peça e de conjunto.
- Ruptora Solar (onda radiante perfurante), Lâmina do Alvorecer (investida com rastro de luz), Cajado Arcano (lança os feitiços desbloqueados, agachado troca), Adagas das Sombras (furtivo + evasão), Asas Prismáticas (élitros com rastro de cores).
- Bancada de Ofício com receitas que exigem nível de profissão; elixires, unguento e ensopado.

### Etapa 4 — criaturas e NPCs (concluída)
- 7 criaturas originais com modelo, animação procedural, textura, sons próprios (OGG sintetizados), atributos, IA, drops e nascimento natural por bioma: Goblin (emboscada com salto), Esqueleto Guerreiro (escudo bloqueia projéteis frontais, investida telegrafada), Lobo Arcano (teleporte pelas costas), Elemental de fogo/gelo (projéteis e pulso em área), Sentinela de Pedra (golpe no chão com anel de aviso), Fada (cega e foge) e Dragonete (sopro de fogo em cone).
- 3 NPCs (Mestre da Guilda, Mercador Itinerante, Sábio) com diálogo, comércio (desconto de Carisma, estoque diário) e missões; não podem ser feridos por jogadores.
- Ovos de invocação, Cetro do Bestiário (ferramenta de administrador para invocar qualquer criatura) e capítulo Bestiário no Codex (informações avançadas liberadas ao derrotar cada criatura). Drops usados nas receitas de ofício.
- Frequência de nascimento configurável (`creatures.spawnRate`).

### Etapa 5 — Reinos do Éter, Portal do Alvorecer e estruturas (concluída)
- Dimensão Reinos do Éter (`alvorecer:aether_realms`): ilhas flutuantes com 4 biomas (Prados Etéreos, Floresta Luminosa, Planaltos de Cristal, Vastidão em Ruínas), céu e névoa próprios, minérios de Prata Lunar e Aurorita, criaturas por bioma.
- Portal do Alvorecer: Pedra do Alvorecer (8 tijolos de pedra + Gema de Aurorita = 8) e Chave do Alvorecer (Gema de Aurorita + lingote de ouro + Pó de Fada, 8 usos). Moldura retangular vertical com interior de 2×3 a 21×21 (cantos opcionais); a chave acende o portal no mundo principal ou no Éter; 3 s dentro do portal para viajar (escala 1:1). O destino reutiliza um portal num raio de 32 blocos ou constrói um com plataforma no terreno mais próximo; quebrar a moldura apaga o portal.
- Estruturas com frequência configurável (`worldgen.*`, 0 a 1): Acampamento Goblin, Torre de Vigia e Santuário do Alvorecer (NPCs, Mural, Bancada) no mundo principal; Ruínas Etéreas e Arena Celeste (8 pilares, sentinelas) no Éter. Baús com loot próprio.
- Missão "Além do Alvorecer", conquistas "A Primeira Luz" e "Reinos do Éter", capítulo "Mundo e Portal" no Codex.

### Testes (estado atual)
- `./gradlew build`: 36 testes JUnit — fórmulas, persistência, validação de habilidades e custos, traduções, talentos, divisão de XP, missões, conquistas, equipamentos (textos, modelos, categorias, receitas, Cajado Arcano), criaturas (textos, ovos, loot, atributos, sons e legendas, texturas), dados do mundo (estruturas → moldes, conjuntos, tags e loot; biomas, ordem das features, temperatura sem neve nas ilhas, dimensão, missão e conquistas). Todos passando.
- `./gradlew runGameTestServer`: 19 GameTests em servidor real — habilidades (custo, recarga, linha de visão, nível), passivas, persistência, talentos, grupo, missão completa, bônus de equipamento e conjunto, habilidade de arma especial, Bancada de Ofício, todas as criaturas nascendo e agindo sem erro, golpe telegrafado da Sentinela, bloqueio frontal do Esqueleto Guerreiro, imunidades do Elemental, NPCs (comércio, proteção e missões), Chave do Alvorecer (moldura incompleta recusada, acendimento, durabilidade, conquista, busca por POI, quebra da moldura apaga o portal) e portal de retorno (construção, saída na plataforma, terreno de chunk não carregado). Todos passando. O GameTest usa um mundo plano próprio em `run/gametest`, sem as dimensões de datapack; a viagem até o Éter é validada no servidor dedicado.
- Servidor dedicado inicia sem erros; cliente conectado ao servidor dedicado testado (Etapas 1 e 5). Etapa 5 no servidor dedicado: `locate` encontra os 4 biomas e as 5 estruturas geradas naturalmente; portal aceso com a chave pelo cliente, viagem ao Éter com portal de retorno sobre uma ilha, conquistas concedidas; Santuário e Arena Celeste vistos no jogo.
- Cliente singleplayer testado pela interface: criação, HUD, ficha, Codex, habilidades, comandos, persistência, tela de talentos (gastar pontos, especialização), Mural de Missões (aceitar e entregar missão de coleta) e avisos de conquista.
- Cliente: equipamentos (aba criativa, tooltips, armadura no corpo, Onda Solar, Bancada de Ofício), todas as criaturas e NPCs renderizando, diálogo e comércio com NPC.
- Ainda não testado visualmente: o painel do grupo com dois clientes reais, o voo das Asas Prismáticas e a animação de puxar o arco.

## Próximas etapas (ainda não implementadas)

6. Masmorras com salas, armadilhas, enigmas e dificuldade por tamanho de grupo.
7. Chefe Guardião do Primeiro Alvorecer (3 fases) e o Coração do Alvorecer.

## Estrutura

- `src/main/java/com/alvorecer/rpg` — modelo (raças, classes, profissões, fórmulas, ficha) e `rpg/talent` (talentos)
- `.../ability` — habilidades, validação e utilidades de alvo/dano
- `.../party`, `.../quest`, `.../achievement` — grupos, missões e conquistas
- `.../item/gear`, `.../crafting` — equipamentos e Bancada de Ofício
- `.../entity/mob`, `.../entity/npc`, `.../client/mob` — criaturas, NPCs, modelos e renderização
- `.../event` — combate, ciclo do jogador e profissões
- `.../client` — HUD, telas e renderização
- `.../gametest` — GameTests
- `tools/` — geradores de idioma, texturas, equipamentos, criaturas, sons e estruturas (`python tools/gen_lang.py src/main/resources/assets/alvorecer/lang`; `gen_sounds.py` requer numpy e soundfile)
