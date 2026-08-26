# Motor de áudio do RADIO+

## Objetivo

O player terá duas rotas de áudio: uma compatível, baseada em Media3/decoders do Android, e uma rota nativa de alta resolução para dispositivos que realmente a suportarem.

## Recursos aprovados

- saída de alta resolução quando confirmada pelo hardware/ROM;
- DSP com equalizador, tom, expansão estéreo, reverb e ritmo/tempo;
- processamento interno em ponto flutuante de 64 bits;
- predefinições AutoEq;
- perfil independente por saída de áudio;
- resampler configurável;
- dither desligado, automático, triangular ou com noise shaping;
- reprodução contínua/gapless e suavização de transições;
- escalas de volume com 30, 50 ou 100 níveis;
- Opus e MKA na rota principal;
- TAK e DSD através da camada nativa de codecs.

## Compatibilidade Android 7+

- Android 8 ou superior: Oboe poderá utilizar AAudio.
- Android 7: Oboe deverá utilizar a rota compatível disponível no dispositivo.
- A interface nunca exibirá “Hi-Res ativo” sem confirmação das capacidades reais da saída.

## Estado atual

Os contratos, configurações, detecção de saídas e matriz de formatos foram adicionados. O DSP e os codecs nativos ainda não foram implementados nem validados por compilação.
