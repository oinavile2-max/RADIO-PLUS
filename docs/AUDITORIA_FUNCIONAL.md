# RADIO+ — Auditoria funcional

Atualização: 26/08/2026

Este documento separa funções implementadas no código, funções que precisam de teste no equipamento e funções ainda não implementadas. Nenhum valor de hardware deve ser simulado.

## Implementado no código — requer validação no Android Studio e no equipamento

- Launcher HOME opcional, gaveta de aplicativos, modo essencial e atalhos internos.
- Paleta neon com 12 cores sólidas, intensidade, contorno, adaptação dia/noite, prévia, aplicação e restauração.
- Player de música local com biblioteca, fontes, busca, fila, favoritos, letras, capa online e troca de lado persistente.
- Rádio online com busca, favoritos, histórico, metadados, capa e troca de lado persistente.
- Vídeo local via MediaStore e Media3, com fila e trava por velocidade OBD recente ou GPS recente.
- Google Maps aberto por intent oficial de navegação.
- Acesso opcional às notificações do Google Maps e popup de rota enquanto o RADIO+ estiver visível.
- Telefone com teclado, seleção de contatos e encaminhamento ao discador instalado.
- Acesso às configurações Bluetooth da central.
- OBD ELM327 Bluetooth SPP, dados ao vivo, leitura/limpeza de DTCs, seleção de parâmetros e monitor vertical.
- Diagnóstico NWD/K2001N do rádio físico na build Admin.

## Dependente de teste real / compatibilidade externa

- O texto das notificações do Google Maps varia conforme versão, idioma e firmware; precisa ser validado na central Android 7.
- A trava de vídeo libera somente com velocidade OBD ou GPS recente igual ou inferior a 3 km/h. Sem leitura confiável, permanece bloqueada.
- Formatos de vídeo e áudio dependem dos codecs de hardware da central.
- O ELM327 e os PIDs disponíveis dependem do veículo, protocolo e qualidade do adaptador.
- O rádio físico depende das APIs privilegiadas NWD/MCU e da assinatura/permissões aceitas pela ROM.
- USB e cartão SD dependem de montagem, indexação do MediaStore e permissões do Android.

## Não implementado / não deve ser anunciado como disponível

- Atender e encerrar chamadas diretamente no Android 7 sem o RADIO+ ser aplicativo de telefone privilegiado ou possuir API do fabricante.
- Controle direto do áudio HFP, agenda e histórico do aplicativo Bluetooth original da central.
- Histórico de chamadas via `READ_CALL_LOG` (permissão restrita na Play Store).
- Mapa incorporado dentro do RADIO+; a versão aprovada abre o aplicativo Google Maps.
- Sobreposição de rota sobre outros aplicativos por permissão de desenho sobre apps; o popup atual é limitado às telas do RADIO+.
- Dados meteorológicos no Launcher sem provedor/API de clima configurado.
- Metadados e controles globais de qualquer player no Launcher sem MediaSession compartilhada.
- Integração final da câmera de ré e sinal de marcha ré.
- Comandos de voz completos para rádio, música, mapas e chamadas.
- DSP próprio de 64 bits, DVC, AutoEQ completo, resampler, dither e decodificadores TAK/DSD.
- Chromecast e Android Auto completos.
- Google Play Billing público, validação de compra no servidor e restauração VIP.
- Reconhecimento musical estilo Shazam com provedor/licença configurados.
- Proteção absoluta contra engenharia reversa ou cracks; ofuscação reduz risco, mas não torna APK inviolável.

## Regra de build

APK e AAB devem ser gerados exclusivamente no Android Studio local do proprietário. O ambiente em nuvem não deve gerar artefatos de instalação.
