# Popup “Tocando agora” e reconhecimento musical

## Ordem das fontes

1. Metadados ICY/HLS do rádio online;
2. RDS fornecido pelo rádio físico/MCU;
3. Reconhecimento de impressão digital quando houver internet e áudio acessível;
4. Sem identificação quando nenhuma fonte for confiável.

## Comportamento do popup

- surge quando a música muda;
- mostra título, artista, estação e origem da identificação;
- permanece por oito segundos;
- não interrompe o áudio;
- ignora metadados repetidos;
- deverá ser ocultado durante câmera de ré e chamadas;
- duração e posição serão configuráveis;
- poderá ser desativado pelo usuário.

## Rádio online

O primeiro método é gratuito: aproveitar os metadados enviados pela própria estação. Se a estação não informar título/artista, o reconhecimento externo será opcional.

## Rádio físico

O aplicativo tentará RDS por meio do adaptador da MCU. Sem RDS, a captura dependerá de uma das opções:

- stream PCM oferecido pelo fabricante;
- entrada de áudio acessível pela MCU;
- amostra captada pelo microfone da central com autorização do usuário.

O Android 7 não oferece captura genérica do áudio reproduzido por outros componentes. A API oficial de captura de reprodução apareceu no Android 10 e exige consentimento.

## Provedor externo

Foi criado um contrato independente de provedor. A produção poderá usar AudD, ACRCloud ou outro serviço autorizado. Tokens ficarão em backend seguro, nunca no APK ou no repositório público.

Para controlar custos, a identificação por fingerprint deverá ocorrer apenas quando RDS/metadados falharem, com intervalo mínimo e cache por estação.

## Estado atual

Popup, normalização de metadados, supressão de duplicatas e contrato de reconhecimento foram implementados. Captura do rádio físico e provedor pago aguardam identificação da MCU e decisão comercial.
