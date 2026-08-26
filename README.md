# RADIO+

Aplicativo multimídia automotivo para centrais Android, com rádio físico como função principal, música local, rádio online, letras sincronizadas e diagnóstico OBD-II.

## Abrir no Android Studio

1. Clonar ou baixar este repositório.
2. Abrir a pasta raiz `RADIO-PLUS` no Android Studio.
3. Aguardar a sincronização do Gradle.
4. Selecionar a variante `debug` em **Build Variants**.
5. Usar **Build > Build APK(s)**.

A variante `debug` é a versão administrativa: usa o pacote `com.chilenoapps.radioplus.admin`, mostra o selo **ADMIN • VIP ATIVO** e libera o estado VIP exclusivamente para homologação. A versão `release` não contém essa autorização.

O projeto suporta Android 7 (API 24) ou superior e foi desenhado para centrais automotivas e tablets em telas horizontais. A interface aceita redimensionamento, diferentes densidades e as duas posições de rotação horizontal.

Faixa principal de uso: telas de aproximadamente 7 a 13 polegadas. Os controles essenciais permanecem grandes para operação veicular e os módulos usam dimensões em `dp`/`sp`, evitando dependência de uma resolução fixa.

## Estado de hardware

O módulo OBD usa conexão Bluetooth ELM327 real. O rádio FM/AM físico ainda utiliza controlador de prévia até que a MCU e a API do rádio original da central sejam identificadas.
