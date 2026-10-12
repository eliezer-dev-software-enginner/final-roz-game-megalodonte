# Projeto de referência e mapeamento

Repositório: https://codeberg.org/rozworld/final-roz-game-new

Revisão clonada: `e93e48529650b87c20e5ca1b4e114d43aee11e2b`. O clone completo está em `upstream/`, ignorado no Git desta adaptação. O original usa Godot/Redot 4.3 e licença MIT, preservada em `LICENSE`.

Autoria: Christian Röttger (RozWareProjekte), com bases dos demos Godot 2D Platformer e Isometric.

## Ordem das fases

| Fase nesta adaptação | Cena original |
|---|---|
| 1 | `level/level_1_neu.tscn` |
| 2 | `level/level_2_neu.tscn` |
| 3–14 | `level/level_3.tscn` a `level/level_14.tscn` |
| 15 | `level/level_20.tscn` |
| 16 | `isometricRoz/dungeon.tscn` |
| 17 | `level/level_15.tscn` |

A ordem segue a progressão de `Global.gd`, não o número do arquivo. A cena inicial original é `GameRoz.tscn`.

## Conversão

`tools/godot_import.py` lê recursos de texto, instâncias, atlas, regiões de textura, polígonos e animações. O importador decodifica células binárias de `TileMapLayer`; mantém coordenadas com 64 pixels por unidade e inverte o eixo Y para a convenção da engine. Imagens WebP são convertidas em PNG para Desktop/Android; regiões animadas viram frames individuais.

Os mapas ortogonais preservam os polígonos originais de rampas e agrupam retângulos adjacentes. A fase 16 usa a projeção isométrica dos tiles 128×64, pisos atrás dos atores e paredes ordenadas pela base de seus colisores. Chaves, portas, moedas, inimigos, pedestres e plataformas viram objetos editáveis com scripts. A área de natação da fase 15 é um trigger oculto editável.

Os fundos panorâmicos usam os assets de `ParallaxBackground` e paralaxe horizontal 0,2, configurável no editor. Rotinas GDScript foram reimplementadas em Java. Os efeitos de shader, partículas e luzes dinâmicas do Godot não foram convertidos com equivalência visual.

O relatório `docs/import-report.json` registra fonte, quantidade de moedas, objetos, tiles, polígonos e assets por fase. O campo `unhandled` identifica falhas de conversão nas categorias de objetos suportadas; não declara equivalência de todos os nós ou efeitos Godot.

## Progressão independente dos totais fixos

Moedas por fase: 468, 87, 147, 158, 66, 180, 183, 129, 135, 177, 108, 108, 57, 60, 120, 30 e 8.

O original compara o total acumulado com números fixos, começando por 469. A adaptação conta as moedas de cada cena ao iniciar a tentativa e registra cada objeto uma única vez. Adicionar/remover moedas pelo editor mantém a regra de conclusão coerente sem editar uma tabela de totais no script.

## Telas de história e abertura

O menu mantém a arte `RoZWareGameTitle.png`, Play e Beste Zeit. A fase 1 é precedida por `level_1_info.tscn` (DerAnfang.png), WEITER!, `level_1_start.tscn` (GegnerRozGame.png) e START!. Os assets originais são preservados como HUD com área de referência centralizada, inclusive em telas largas.

Há histórias nas fases 1, 2, 5, 7, 9, 11, 15 e 17. O arquivo `level_12_info.tscn` pertence à entrada da fase 15, e `level_15_info.tscn` precede a fase 17. As cenas `intro-1` a `intro-16` usam os cartões originais de LEVEL; a fase 17 tem um cartão adicional criado com texto editável, pois o original entra nela pela tela de congratulação.

As histórias 2, 5, 7, 9 e 11 existem no repositório de referência e foram incluídas no fluxo da adaptação; algumas não são alcançadas pelo contador fixo de `Global.gd` no original. A seleção de fases e o menu de opções preservam os recursos adicionais de progresso e JSON desta versão.
