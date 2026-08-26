# Rádio Online — RADIO+

## Fonte do catálogo

O catálogo inicial utiliza a API comunitária Radio Browser, sem chave. O cliente:

- identifica-se como RADIOPlus/versão;
- tenta servidores espelho em caso de falha;
- oculta estações marcadas como indisponíveis;
- registra o clique ao iniciar a reprodução;
- não envia dados pessoais do usuário.

## Funcionalidades da primeira etapa

- estações populares do Brasil;
- busca por nome, gênero, país e estado;
- ordenação por popularidade;
- reprodução interna com Media3;
- nome, país, estado, codec e bitrate;
- metadados da faixa quando fornecidos pelo stream;
- logotipo da estação;
- favoritos persistentes;
- histórico das últimas 30 estações;
- fallback entre espelhos da API;
- compatibilidade com streams HTTPS e HTTP.

## Próximas etapas

- filtros por idioma, qualidade e codec;
- estações próximas usando localização, quando houver coordenadas;
- importação manual de M3U/PLS;
- fila e troca automática quando uma estação falhar;
- temas dinâmicos baseados no logotipo;
- cache de logotipos;
- sincronização VIP de favoritos;
- temporizador para dormir;
- gravação somente quando legalmente permitida e autorizada pela estação.

## Segurança

APIs e metadados usam HTTPS. HTTP sem criptografia é aceito somente porque diversas estações ainda transmitem áudio dessa forma; nenhuma credencial será enviada por esses fluxos.

O módulo ainda não foi validado por compilação ou teste em uma central real.
