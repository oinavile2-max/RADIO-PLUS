# Compatibilidade com tablets

O RADIO+ atende centrais Android genéricas e tablets usados como sistema multimídia veicular.

## Escopo inicial

- Android 7 / API 24 ou superior.
- Telas normais, grandes e extragrandes.
- Densidades variadas, sem depender de resolução específica.
- Uso horizontal nas duas rotações (`sensorLandscape`).
- Atividade redimensionável para janelas compatíveis.
- Controles grandes e contraste adequado ao uso no veículo.

## Faixa principal

A interface deve ser validada principalmente em equivalentes a 7, 8, 9, 10, 11, 12 e 13 polegadas. A dimensão física não é suficiente para definir o layout; densidade, proporção e área disponível também serão consideradas.

## Regras

1. Nenhum botão funcional pode ficar fora da tela.
2. Textos críticos não podem ser cortados.
3. O painel deve continuar utilizável em 800×480 e crescer de forma equilibrada em tablets.
4. Modo Essencial e Modo Noturno devem funcionar igualmente em central e tablet.
5. Recursos dependentes de hardware, como rádio físico e câmera de ré, devem permanecer condicionados à compatibilidade real do dispositivo.
6. Bluetooth OBD, música local, rádio online e demais módulos independentes da MCU devem funcionar também em tablets compatíveis.
