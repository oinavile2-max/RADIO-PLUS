# Escopo avançado do player RADIO+

## Núcleo de reprodução

- equalizador gráfico de 10 bandas com predefinições;
- graves, agudos, expansão estéreo, mixagem mono e balanço;
- andamento, reverb e integração MusicFX quando fornecida pela ROM;
- DVC condicionado ao caminho de áudio e à compatibilidade da central;
- crossfade configurável, gapless e suavização contínua;
- ReplayGain por faixa ou álbum;
- pastas, biblioteca, fila dinâmica e retomada automática;
- playlists e fluxos HTTP M3U/PLS;
- scanner incremental de biblioteca;
- informações completas da cadeia de processamento.

## Metadados e conteúdo

- capas incorporadas e busca MusicBrainz/Cover Art Archive;
- imagens de artistas com cache e fonte identificada;
- letras incorporadas, sincronizadas e pesquisa por provedor/plugin;
- arquivos CUE externos e incorporados;
- editor de tags com confirmação explícita antes de alterar arquivos;
- scrobbling opcional e desativado por padrão.

## Integrações Android

- MediaLibraryService e MediaSession para reprodução em segundo plano;
- controles do sistema, tela bloqueada, fones e Bluetooth;
- pausa/retomada configurável ao conectar ou desconectar uma saída;
- Android Auto e comandos compatíveis do Google Assistant;
- Chromecast com controlador compacto e expandido;
- widgets redimensionáveis e estilos selecionáveis.

## Aparência

- temas do próprio RADIO+ e pacotes de pele validados;
- tela “Tocando agora” personalizável;
- visualizações internas e compatibilidade futura com presets do tipo Milkdrop;
- plugins de terceiros deverão ser isolados e validados antes da instalação.

## Matriz de implementação

| Grupo | Estado |
|---|---|
| Biblioteca local, reprodução, fila inicial e capas | Em desenvolvimento |
| M3U/PLS HTTP | Parser inicial implementado |
| Configurações de DSP, DVC, ReplayGain e crossfade | Modelo persistente implementado |
| Equalizador/DSP de 64 bits | Requer motor nativo |
| TAK, DSD e CUE | Requer codecs/extrator nativo |
| Android Auto, Assistant e tela bloqueada | Requer MediaLibraryService |
| Chromecast | Requer SDK Google Cast e testes com receptor |
| Letras, artista e scrobbling | Requer definição de provedores e privacidade |
| Widgets, skins e Milkdrop | Planejado |

Nenhum recurso dependente de serviço, codec ou hardware será apresentado como ativo antes de validação real.
