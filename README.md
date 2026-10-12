# final-roz-game-megalodonte

Adaptação de [Final Roz Game New](https://codeberg.org/rozworld/final-roz-game-new), de Christian Röttger, para a Megalodonte Engine. Inclui **17 fases**, 17 telas de abertura, 8 cenas de história do original, menu, opções, ajuda, seleção de fases, recordes e final: 48 cenas editáveis em `game.json`.

## Jogar e editar

Abra `game.json` na engine e selecione uma cena na lista **CENAS**. Use **Play / Stop** para executar o projeto. As builds registram esse projeto como o último aberto no editor.

Colete todas as moedas da fase para avançar e desbloquear a próxima. A partida começa com 25 vidas; inimigos hostis e quedas retiram vidas, com um segundo de invulnerabilidade após dano. Os pedestres são inofensivos. Chaves abrem portas da mesma cor. O melhor tempo de cada fase e as fases desbloqueadas são salvos automaticamente. Reiniciar uma fase começa uma tentativa nova; as moedas e chaves daquela tentativa são restauradas.

| Ação | Teclado | Gamepad | Android |
|---|---|---|---|
| Mover | A/D ou esquerda/direita | Analógico ou direcional | Setas da HUD |
| Pular / salto duplo | Espaço | A | Seta para cima |
| Atirar / colocar bomba na fase 16 | X ou Ctrl | X | Botão de tiro |
| Correr nas fases 15 e 17 | Shift | R1 | CORRER |
| Agachar nas fases 15 e 17 | S ou baixo | Direcional para baixo | Seta para baixo |
| Nadar na fase 15 | Direcional; Espaço sobe | Direcional; A sobe | Setas; salto sobe |
| Movimento isométrico na fase 16 | WASD ou setas | Analógico/direcional em 8 direções | Quatro setas |
| Pausar / continuar | Escape | Start | Botão de pausa |
| Voltar ao menu | Botão MENU | B | MENU |
| Navegar nos menus | Mouse; Enter confirma seleção | Direcional e A | Toque |

No botão **☰** do menu, **ÁUDIO** alterna música/efeitos e **TOQUE** exibe/oculta os controles virtuais. As opções também são preservadas nos dados salvos. O suporte a gamepad está disponível em Linux e Android, seguindo o mapeamento padrão A/B/X/R1.

O início segue **Play → história “Der Anfang” → WEITER! → LEVEL 1 → START! → fase 1**. A arte e os textos dessas telas são os assets originais, em alemão. Todas essas telas aparecem separadamente na lista de cenas do editor. O botão **☰** dá acesso à seleção de fases, importação/exportação, áudio, toque e ajuda.

## Exportar e importar dados salvos

Use **EXPORTAR JSON** e **IMPORTAR JSON** em **☰ → Opções**. No Desktop, o seletor escolhe o arquivo; no Android, o seletor de documentos permite salvar ou abrir o JSON, inclusive em provedores de arquivos.

O documento contém `version`, `applicationId` e `state`: fases desbloqueadas, melhores tempos em milissegundos e opções de áudio/toque. Não inclui posições, moedas da tentativa atual ou objetos da cena. Importar substitui os dados persistentes após validação; JSON inválido, valores fora dos limites ou dados de outro jogo são recusados sem substituir o progresso atual. Faça uma exportação antes de substituir seus dados.

## Build Desktop e Android

Na pasta deste projeto:

```bash
bash tools/build.sh desktop
bash tools/build.sh android
bash tools/build.sh android-release
```

A engine é encontrada em `../../megalodonte-declarative/megalodonte-engine`; configure `MEGALODONTE_ENGINE` para outra instalação. O diretório de saída abre no explorador ao concluir a build.

- Desktop Linux: `build/desktop/finalroz/bin/finalroz`; distribuição em `build/desktop/finalroz-linux-x86_64.tar.gz`.
- APK de teste instalável: `build/android/game-debug.apk`.
- Projeto para assinatura: `build/android-release/project`. Abra essa pasta no Android Studio e use **Build → Generate Signed App Bundle / APK**, com seu keystore. Esse comando atualiza código, scripts e assets e valida a compilação; o APK release sem assinatura não deve ser instalado.

O aplicativo usa `megalodonte.games.finalroz`, ícone do original, categoria Android `game` e orientação `sensorLandscape`. A engine utiliza o SDK configurado em **Configurações** e gera `local.properties` na exportação Android.

## Dados e scripts anexados

Mapas, polígonos de colisão, sprites, frames de animação, HUD, câmera, música e propriedades estão nos dados editáveis do projeto. Para inspecionar um objeto, selecione-o e use **Visibilidade / colisor / propriedades**, **Animar cena**, **Scripts Java** ou o editor de tilemaps. Na fase isométrica, pisos usam camada de desenho `-1`; atores e paredes usam `0` e ordenação por profundidade.

| Script | Anexado a | Responsabilidade |
|---|---|---|
| `Menu` | Menu, histórias, aberturas, opções, ajuda, seleção, recordes e final | Navegação, opções e importação/exportação |
| `Session` | Cada uma das 17 fases | Moedas restantes, tempo, vidas, pausa, recordes e passagem de fase |
| `Player` | Roz de cada fase | Movimento, salto duplo, rampas, agachar, nadar, tiros e bombas |
| `Collectible` | Moedas e chaves | Coleta única e abertura das portas da cor correspondente |
| `Door` | Portas | Bloqueio até encontrar a chave |
| `Enemy` | Inimigos e pedestres | Patrulha, perseguição isométrica, dano e animação de morte; propriedade `harmless` distingue pedestres |
| `Platform` | Plataformas móveis | Percurso definido em `properties.route` e transporte do jogador |
| `Projectile` | Tiros e bombas criados durante a partida | Movimento, impacto, explosão e remoção |

`Progress` e `Actors` são auxiliares Java, sem anexação direta. Os objetos ocultos `bullet-template` e `bomb-template` guardam os assets, animações e colisores dos projéteis. O jogador cria cópias desses modelos e anexa `Projectile`; a engine inicia e encerra automaticamente esses scripts.

## Importação e referência

O clone original está em `upstream/`, ignorado pelo Git desta adaptação. Para reproduzir a importação, instale Python 3 com Pillow e execute `python3 tools/import-project.py --force`. **Regenerar sobrescreve as edições de `game.json`; guarde uma cópia antes.** O relatório está em [docs/import-report.json](docs/import-report.json), e o mapeamento em [docs/SOURCE.md](docs/SOURCE.md).

A fase 1 importada tem 468 moedas, enquanto o contador fixo do original exige 469. Nossa passagem de fase usa a quantidade de moedas realmente presente na cena, e a última coleta é testada nas 17 fases. A mesma regra funciona nos hosts Desktop e Android.

A adaptação recria as mecânicas em Java; não executa GDScript. Fundos com paralaxe, mapas e animações foram convertidos. Shaders, partículas e iluminação dinâmica específicos do Godot não têm reprodução visual idêntica nesta versão. A validação automatizada cobre progressão e mecânicas; a campanha completa ainda deve ser conferida jogando em um aparelho Android real.

## Verificação

Após gerar Desktop:

```bash
java -Xmx256m -cp "build/desktop/finalroz/lib/app/*" tools/SmokeTest.java
java -Xmx256m -cp "build/desktop/finalroz/lib/app/*" tools/MechanicsTest.java
```

Os testes cobrem a coleta final e transição das 17 fases, spawn no chão, salto duplo, bloqueio de fases, recordes, JSON, tiros, portas, plataformas e bombas. A coleta exaustiva posiciona o jogador em cada moeda para validar o contador; ela não substitui uma partida completa pelo percurso. Detalhes em [docs/VALIDATION.md](docs/VALIDATION.md).

Código e assets do original estão sob MIT. Autoria e licença foram preservadas em `LICENSE`.
