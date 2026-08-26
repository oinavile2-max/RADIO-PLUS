# Letras com rolagem automática

## Fontes

1. Letra sincronizada disponível no cache;
2. LRCLIB usando título, artista, álbum e duração;
3. Letra comum com rolagem proporcional;
4. Futuro suporte a letras incorporadas e arquivos `.lrc` ao lado da música.

## Interface

- botão **LETRAS** no player;
- painel no estilo visual dos demais popups;
- título e artista;
- linha atual destacada;
- centralização suave automática;
- atualização quatro vezes por segundo;
- opção de pausar/retomar a rolagem;
- indicação “Sincronizada” ou “Rolagem automática”.

## Funcionamento

Quando existem timestamps LRC, a linha é selecionada pela posição real da reprodução. Quando existe apenas texto comum, o avanço é estimado usando posição e duração da faixa.

As respostas ficam em cache local para reduzir consultas e permitir nova visualização sem rede enquanto o cache estiver disponível.

## Próximas melhorias

- ajuste de tamanho da fonte;
- deslocamento manual de sincronização;
- busca manual por outro resultado;
- leitura de tags de letras incorporadas;
- leitura e edição de arquivo LRC local;
- modo de tela dividida;
- tradução opcional separada da letra original.

O recurso ainda não foi validado por compilação ou teste na central.
