# Validação da adaptação

Validado em 11/10/2026, com as 17 fases e 48 cenas da versão 0.2.0.

## Testes do jogo

`tools/SmokeTest.java`, executado contra os jars da distribuição Desktop com heap máximo de 256 MiB:

- Menu → história original da fase 1 → cartão LEVEL 1 → fase 1.
- Apoio do jogador no chão e salto duplo.
- Criação e remoção de tiros com script dinâmico.
- Recusa de entrada em fase bloqueada.
- Exportação de melhores tempos e desbloqueios sem estado transitório da tentativa.
- Importação válida e recusa de desbloqueio fora dos limites sem alterar os dados anteriores.
- Coleta de todas as moedas reais e transição de cada uma das 17 fases, incluindo a última moeda da fase 1 (468/468).

A coleta exaustiva usa os scripts reais de moeda, porta e sessão, posicionando o jogador em cada moeda. Movimento e combate são desativados nesse percurso específico para isolar a regra de conclusão; isso não comprova que um jogador percorreu toda a campanha manualmente.

`tools/MechanicsTest.java`, também com heap de 256 MiB:

- Apoio dos 16 spawns platformer no chão.
- Movimento isométrico na fase 16.
- Tiro rápido atingindo um inimigo com colisor estreito sem atravessá-lo, chave abrindo porta, plataforma móvel levando o jogador.
- Explosão de bomba e remoção do objeto temporário.
- Natação para cima usando a ação de gamepad na fase 15.

## Engine e apresentação

58 testes do core e 25 testes de regressão de Alex vs Bus passaram. O teste `FinalRozVisualTest` compila os scripts do projeto, usa o renderizador Desktop real e captura menu, história da fase 1, cartão LEVEL 1, opções, seleção e fases 1, 4, 15, 16 e 17. Também confere que os textos dos botões originais cabem em suas áreas.

As imagens são gravadas em `megalodonte-engine/build/preview/finalroz-*.png`. A conferência visual corrigiu a sobreposição indevida do piso sobre o jogador isométrico e os textos WEITER!/START! cortados.

## Distribuições

Desktop, APK debug e projeto Android release foram compilados. A verificação do pacote compara o documento fonte com os documentos incluídos no Desktop e no APK e confirma a presença dos assets importados. O APK debug tem assinatura de desenvolvimento; o projeto release deve ser assinado com um keystore no Android Studio antes de distribuição.

O projeto Android usa API mínima/alvo 35 (Android 15), conforme a configuração atual da Megalodonte Engine. Não havia aparelho autorizado conectado por ADB nesta validação. Ainda falta uma partida completa em Android real para conferir desempenho, conforto dos controles e o percurso de cada fase; a passagem de fase foi verificada no runtime compartilhado e com os mesmos dados empacotados no APK.

## Limites da equivalência

Os mapas, sprites, animações, itens e principais mecânicas estão na adaptação. Efeitos de shader, partículas e iluminação específica do Godot não têm reprodução visual idêntica. As histórias adicionais disponíveis no repositório foram incluídas no fluxo e permanecem como cenas separadas para edição.

## Otimizações e combate

A revisão de desempenho está em [PERFORMANCE.md](PERFORMANCE.md). O teste `CombatTest` verifica remoção de cada bala no impacto em inimigos vivos/morrendo e a rotação/fade da morte. O índice espacial é comparado com consultas exaustivas no core, e as 17 fases mantêm os testes de progressão e nascimento.
