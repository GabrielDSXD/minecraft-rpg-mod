# Crônicas do Alvorecer

Mod de RPG de fantasia para **Minecraft Java 1.21.1** com **NeoForge 21.1.x** (testado com 21.1.255). Funciona em singleplayer e em servidor dedicado; o mod precisa estar instalado no cliente e no servidor.

A entrega é feita em etapas (status abaixo). Concluídas: **Etapa 1** (base jogável) e **Etapa 2** (talentos, especializações, grupos, missões e conquistas).

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

### Testes (estado atual)
- `./gradlew build`: 27 testes JUnit — fórmulas, persistência NBT, validação de habilidades e custos, criação/pontos, traduções, regras de talentos e especializações, divisão de XP, catálogo e fluxo de missões, conquistas. Todos passando.
- `./gradlew runGameTestServer`: 8 GameTests em servidor real — golpe com custo e recarga, linha de visão, suprema bloqueada, passivas raciais, persistência no NBT do jogador, gasto de talento validado no servidor, grupo (XP dividido e sem fogo amigo), missão completa no mural (aceitar, abater 10 zumbis, entregar e receber recompensa). Todos passando.
- Servidor dedicado inicia sem erros; cliente conectado ao servidor dedicado testado (Etapa 1).
- Cliente singleplayer testado pela interface: criação, HUD, ficha, Codex, habilidades, comandos, persistência, tela de talentos (gastar pontos, especialização), Mural de Missões (aceitar e entregar missão de coleta) e avisos de conquista.
- Ainda não testado visualmente: o painel do grupo no HUD com dois clientes reais (a lógica do grupo foi testada nos GameTests).

## Próximas etapas (ainda não implementadas)

3. Equipamentos: conjuntos de ferramentas e armas (adagas, cajados, varinhas, arcos, bestas, martelos, lanças, escudos), raridades e requisitos, armaduras leves/médias/pesadas com bônus de conjunto, Ruptora Solar, Lâmina do Alvorecer, Cajado Arcano (seleção de feitiços), Adagas das Sombras, Asas Prismáticas e receitas de profissão.
4. Criaturas originais com modelos, animações, sons e drops; ovos e ferramenta de demonstração; NPCs com diálogo, comércio e missões.
5. Dimensão Reinos do Éter, Portal do Alvorecer e estruturas no mundo principal com frequência configurável.
6. Masmorras com salas, armadilhas, enigmas e dificuldade por tamanho de grupo.
7. Chefe Guardião do Primeiro Alvorecer (3 fases) e o Coração do Alvorecer.

## Estrutura

- `src/main/java/com/alvorecer/rpg` — modelo (raças, classes, profissões, fórmulas, ficha) e `rpg/talent` (talentos)
- `.../ability` — habilidades, validação e utilidades de alvo/dano
- `.../party`, `.../quest`, `.../achievement` — grupos, missões e conquistas
- `.../event` — combate, ciclo do jogador e profissões
- `.../client` — HUD, telas e renderização
- `.../gametest` — GameTests
- `tools/` — geradores do arquivo de idioma, texturas e estruturas (`python tools/gen_lang.py src/main/resources/assets/alvorecer/lang`)
