# Diagnóstico e otimizações — 2026-10-11

O relato de FPS melhorar após eliminar inimigos correspondeu a um gargalo de CPU nas colisões. A fase 1 tem 639 objetos e **7.524 obstáculos** de mapa; a fase 9 tem **14.955 obstáculos**. Antes, cada consulta de chão/movimento percorria todos os obstáculos. Inimigos, gravidade e subpassos do tiro multiplicavam essas consultas, inclusive as buscas binárias usadas para parar na borda de um colisor.

As imagens Android já eram carregadas sob demanda, porém a decodificação era síncrona no desenho. Efeitos sonoros criavam e preparavam um `MediaPlayer` a cada disparo/coleta na thread da UI. O runtime mantinha duas cópias completas da campanha, e a ordenação isométrica incluía objetos fora da câmera. Portanto havia tanto trabalho excessivo contínuo quanto causas de pausas na primeira utilização de recursos.

## Correções na engine

- Índice espacial de obstáculos por células de 4 unidades: busca somente a geometria próxima, preservando o teste real dos polígonos. Objetos dinâmicos continuam usando posição/colisor atuais, sem uma fotografia defasada de inimigos ou plataformas.
- O índice é reutilizado. Trocar cena, adicionar/remover obstáculos ou mover/escalar mapas o invalida automaticamente. Scripts que alteram a posição/tamanho de um obstáculo existente, ou substituem um obstáculo sem mudar a quantidade, devem chamar `c.invalidateCollisionGeometry()`; consultas externas ao update também verificam edições diretas.
- Caminho rápido para cenas sem hierarquia, rejeição AABB antes de verificar parentesco/polígonos, e animações inativas/ocultas deixam de avançar seus relógios.
- Apenas a cena ativa tem uma cópia mutável de execução. Ao sair dela, essa cópia é retirada da lista da campanha. A fotografia original continua disponível para reiniciar fases. O JSON inteiro ainda é lido na abertura; não foi implementado streaming de documentos de fase.
- Culling antes da ordenação isométrica, cálculo de profundidade uma vez por objeto visível e inclusão conservadora dos sprites rotacionados na borda da câmera.
- Android: imagens decodificadas em um worker, fila limitada a 64 solicitações, deduplicação de cargas, pré-busca dos três próximos quadros visíveis e cache LRU de aproximadamente 64 MiB. Uma imagem individual maior que o orçamento pode ficar sozinha no cache. Bitmaps expulsos não são reciclados enquanto o renderizador pode referenciá-los.
- Desktop/editor: cache de imagens limitado por pixels decodificados e quantidade; Desktop carrega imagens em segundo plano.
- Android: efeitos curtos em `SoundPool`, carregamento assíncrono e até 12 vozes simultâneas. Sons conhecidos da cena são preparados antes de disparar. Música usa `prepareAsync()`; o áudio do menu continua compartilhado entre seus submenus.
- Desktop: efeitos reutilizam `AudioClip` em vez de criar um novo player por disparo. Música continua em `MediaPlayer`.

Referências das APIs: [SoundPool](https://developer.android.com/reference/android/media/SoundPool) e [JavaFX AudioClip](https://openjfx.io/javadoc/25/javafx.media/javafx/scene/media/AudioClip.html).

## Correções no combate

O frame único de morte tinha sido importado sem os tracks de rotação/fade do `AnimationPlayer` original; por isso o inimigo parecia congelar antes de sumir. Agora ele gira e perde opacidade gradualmente, deixa de causar colisão/dano e termina a transição mesmo quando fica longe do jogador. A duração pode ser configurada pela propriedade `deathSeconds` (padrão 1,1 s). A opacidade do objeto também pode ser editada na engine.

Cada bala é removida ao atingir um inimigo visível. Isso inclui disparos seguintes durante a animação de morte: o colisor físico já desativado não permite que a bala atravesse a figura ainda visível. Os subpassos do projétil continuam evitando atravessar alvos estreitos.

## Como verificar

Depois de gerar Desktop:

```bash
java -Xmx256m -cp 'build/desktop/finalroz/lib/app/*' tools/PerformanceTest.java
java -Xmx256m -cp 'build/desktop/finalroz/lib/app/*' tools/CombatTest.java
java -Xmx256m -cp 'build/desktop/finalroz/lib/app/*' tools/SmokeTest.java
java -Xmx256m -cp 'build/desktop/finalroz/lib/app/*' tools/MechanicsTest.java
```

`PerformanceTest` usa as fases 1, 9 e 16, com cenário parado e cenário andando/atirando, descarta 120 quadros de aquecimento e mede 360 updates com passo fixo de 1/60 s. Ele mede **CPU do runtime**, sem renderização ou reprodução de áudio. Não equivale ao FPS do Android. A linha de base foi capturada dos jars antes das mudanças (engine `61eef47`, jogo `5166618`). Resultados locais constam em `performance-results.json`.

Os testes da engine comparam milhares de consultas do índice com busca exaustiva em retângulos/polígonos, verificam atualizações do índice, geometrias grandes, culling/rotação, liberação/reset de cenas e orçamento de candidatos. Os testes do jogo cobrem acertos sucessivos durante a morte, progressão das 17 fases, pontos de nascimento, portas, plataformas, bombas, natação e dados JSON.

Ainda é necessário medir FPS e pausas em um aparelho Android real. As builds são atualizadas, mas uma APK já instalada não recebe essas mudanças automaticamente.

## Medições locais

| Cena / cenário | Mediana anterior | Mediana corrigida | P95 anterior | P95 corrigido |
| --- | ---: | ---: | ---: | ---: |
| level-1 / idle | 3.325 ms | 0.384 ms | 4.035 ms | 0.993 ms |
| level-1 / combat | 5.433 ms | 0.396 ms | 6.737 ms | 0.558 ms |
| level-9 / idle | 0.142 ms | 0.046 ms | 0.421 ms | 0.065 ms |
| level-9 / combat | 3.389 ms | 0.145 ms | 3.747 ms | 0.293 ms |
| level-16 / idle | 0.160 ms | 0.085 ms | 0.487 ms | 0.102 ms |
| level-16 / combat | 0.263 ms | 0.152 ms | 0.355 ms | 0.189 ms |

Medição final executada em sequência contra os jars anteriores e os novos, depois de terminar as builds. A mediana de combate da fase 1 caiu aproximadamente 13,7 vezes; a fase 9 caiu aproximadamente 23,4 vezes. Isso não significa multiplicar o FPS na mesma proporção: desenho, áudio, aparelho, aquecimento/JIT e coleta de lixo ainda influenciam o tempo total de um quadro. Os máximos oscilam e constam no JSON, sem promessa de ausência de pausas.
