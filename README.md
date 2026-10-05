# Crônicas do Alvorecer

Mod de RPG de fantasia para **Minecraft Java 1.21.1** com **NeoForge 21.1.x** (testado com 21.1.255). Funciona em singleplayer e em servidor dedicado; o mod precisa estar instalado no cliente e no servidor.

A entrega é feita em etapas. Esta é a **Etapa 1 — base jogável**: criação de personagem, as 8 raças, as 9 classes, as 8 profissões, progressão até o nível 20, combate com atributos e testes de resistência, HUD, ficha, Codex e administração.

## Compilar

Requisitos: um JDK 17 ou mais novo para rodar o Gradle. O JDK 21 usado para compilar o mod é baixado automaticamente (toolchain do Gradle).

```bash
./gradlew build                 # compila, roda os testes JUnit e gera build/libs/alvorecer-0.1.0.jar
./gradlew runGameTestServer     # sobe um servidor e roda os GameTests do mod
./gradlew runClient             # cliente de desenvolvimento
./gradlew runClient -PquickPlay=NomeDoMundo   # entra direto num mundo salvo em run/saves
./gradlew runServer             # servidor dedicado de desenvolvimento (pasta run/)
./gradlew runClient -PquickPlayServer=localhost  # conecta direto no servidor (use online-mode=false em run/server.properties)
```

## Instalar

1. Instale o NeoForge 21.1.255 (ou mais novo da linha 21.1) para Minecraft 1.21.1.
2. Copie `build/libs/alvorecer-0.1.0.jar` para a pasta `mods` do cliente e do servidor.

## Jogar

Na primeira entrada no mundo abre a criação de personagem (raça → classe → profissão → confirmar). O Draconato escolhe também o elemento. Ao confirmar você recebe o **Codex do Aventureiro** e 3 pontos de atributo.

| Tecla padrão | Ação |
|---|---|
| R | Habilidade racial |
| Z, V, B, N | Habilidades de classe (níveis 1, 3, 6, 10) |
| G | Habilidade suprema (nível 15) |
| H / U | Habilidade da profissão principal / secundária |
| K | Ficha do personagem (distribuir pontos, escolher profissão secundária) |
| J | Codex do Aventureiro |

Todas podem ser trocadas em Opções → Controles → Crônicas do Alvorecer. As fórmulas completas estão no Codex (capítulo Fórmulas, liberado no nível 3).

## Administração

Configuração por mundo em `serverconfig/alvorecer-server.toml` (sincronizada com os clientes): multiplicadores de XP, XP de geradores e de exploração, XP e limite diário de profissões, nível da profissão secundária, custo/recarga/dano das habilidades, PvP de habilidades (liga/desliga, multiplicador de dano, efeitos de controle), testes de resistência e o modo de troca de personagem (`ADMIN_ONLY`, `ITEM` com o Tomo do Recomeço, ou `FREE` com `/rpg respec`) com recarga.

Comandos (permissão 2): `/rpg info [jogador]`, `/rpg reset <jogador>`, `/rpg setlevel <jogador> <nível>`, `/rpg addxp <jogador> <qtd>`, `/rpg proflevel <jogador> <nível>`, `/rpg cooldowns <jogador>`. Jogadores: `/rpg info`, `/rpg respec` (modo FREE).

## O que foi implementado, compilado e testado nesta etapa

Implementado:
- Ficha persistida no jogador (anexo de dados NeoForge), copiada na morte e sincronizada só com o dono.
- 8 raças, cada uma com habilidade ativa, vantagens e desvantagens com valores, duração e recarga explícitos; 9 classes com recurso principal, proficiências de armas/armaduras (com penalidades), 4 habilidades progressivas + suprema; 8 profissões com XP por atividade, bônus por nível, habilidade ativa e profissão secundária. Total: 61 habilidades, todas validadas no servidor (personagem, nível, atordoamento, recarga, recurso, alvo e linha de visão).
- Atributos FOR/DES/CON/INT/SAB/CAR afetando dano, vida, mana, energia, crítico, resistência mágica, testes de resistência, sorte e desconto com aldeões.
- Dano físico e mágico (9 tipos de dano do mod), resistências/vulnerabilidades raciais, críticos, efeitos de controle (atordoar, enraizar, encantar) e testes de resistência d20.
- Níveis 1–20, pontos de atributo, XP por abate (geradores rendem 1/4) e por exploração de biomas; limite diário e regras anti-ciclo para profissões.
- HUD (mana, energia, XP, barra de habilidades com recarga), ficha, criação, Codex com capítulos e diário de exploração, Tomo do Recomeço, comandos e configuração. Interface e textos em pt-BR (o arquivo en_us usa o mesmo texto).

Testado:
- `./gradlew build`: 19 testes JUnit (fórmulas, persistência NBT, validação de habilidades e custos, criação e distribuição de pontos, traduções) — todos passando.
- `./gradlew runGameTestServer`: 5 GameTests em servidor real (golpe com custo e recarga, linha de visão bloqueada por parede, suprema bloqueada por nível, passivas e habilidade racial, ficha persistida no NBT do jogador) — todos passando.
- Servidor dedicado (`runServer`) inicia sem erros com o mod.
- Cliente singleplayer: criação completa pela interface, HUD, ficha, distribuição de pontos, Codex, habilidades (Sopro, Bola de Fogo, Chuva de Meteoros, Ilusão), comandos, persistência após sair/entrar e após morrer.

- Multiplayer real: cliente conectado ao servidor dedicado — criação pela rede, uso de habilidade com custo e recarga, passivas raciais e ficha sincronizada.

## Próximas etapas (ainda não implementadas)

2. Árvores de talentos e especializações; grupos de jogadores com XP compartilhado; diário de missões e conquistas.
3. Equipamentos: conjuntos de ferramentas e armas (adagas, cajados, varinhas, arcos, bestas, martelos, lanças, escudos), raridades e requisitos, armaduras leves/médias/pesadas com bônus de conjunto, Ruptora Solar, Lâmina do Alvorecer, Cajado Arcano (seleção de feitiços), Adagas das Sombras, Asas Prismáticas e receitas de profissão.
4. Criaturas originais com modelos, animações, sons e drops; ovos e ferramenta de demonstração; NPCs com diálogo, comércio e missões.
5. Dimensão Reinos do Éter, Portal do Alvorecer e estruturas no mundo principal com frequência configurável.
6. Masmorras com salas, armadilhas, enigmas e dificuldade por tamanho de grupo.
7. Chefe Guardião do Primeiro Alvorecer (3 fases) e o Coração do Alvorecer.

## Estrutura

- `src/main/java/com/alvorecer/rpg` — modelo (raças, classes, profissões, fórmulas, ficha)
- `.../ability` — habilidades, validação e utilidades de alvo/dano
- `.../event` — combate, ciclo do jogador e profissões
- `.../client` — HUD, telas e renderização
- `.../gametest` — GameTests
- `tools/` — geradores do arquivo de idioma, texturas e estruturas (`python tools/gen_lang.py src/main/resources/assets/alvorecer/lang`)
