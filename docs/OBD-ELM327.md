# OBD-II ao vivo — ELM327 Bluetooth

O módulo OBD do RADIO+ conecta a adaptadores já pareados no Android por Bluetooth clássico SPP/RFCOMM. O pareamento e o PIN (`1234` ou `0000`, conforme o adaptador) são tratados pela tela Bluetooth do sistema; o aplicativo não tenta contornar a autorização do Android.

## Fluxo funcional

1. Parear o ELM327 nas configurações Bluetooth da central.
2. Abrir OBD no RADIO+ e tocar em **Selecionar**.
3. Escolher o adaptador pareado e tocar em **Conectar**.
4. O app executa reset, desativa eco/espaços/cabeçalhos, seleciona protocolo automático, identifica o adaptador e consulta a ECU.
5. As leituras suportadas são atualizadas continuamente.

## Leituras iniciais

- RPM (`010C`)
- velocidade (`010D`)
- temperatura do líquido de arrefecimento (`0105`)
- temperatura do ar de admissão (`010F`)
- carga calculada (`0104`)
- posição do acelerador (`0111`)
- tensão informada pelo adaptador (`ATRV`)
- códigos de falha confirmados (`03`)

O comando `04` para apagar falhas exige confirmação. Se o app detectar velocidade acima de zero, a operação é bloqueada.

## Limites reais

Nem todo veículo implementa todos os PIDs. Adaptadores ELM327 clonados podem declarar uma versão falsa, responder lentamente ou omitir comandos. O app mostra dados ausentes como indisponíveis; não cria valores estimados como se fossem medidos.

Esta primeira integração é diagnóstico OBD-II de motor/emissões. ABS, airbag, câmbio, TPMS e módulos proprietários dependem de protocolos e comandos específicos de cada fabricante e não são anunciados como compatíveis nesta etapa.
