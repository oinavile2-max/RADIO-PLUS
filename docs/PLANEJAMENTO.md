# Planejamento funcional — RADIO+

## 1. Conceito

O RADIO+ será uma central multimídia completa para dispositivos Android automotivos genéricos. O rádio físico FM/AM será o protagonista da tela inicial, enquanto os demais módulos funcionarão dentro da mesma identidade visual.

## 2. Tela Home e rádio físico

- Inicialização diretamente no rádio físico;
- frequência e nome da estação em destaque;
- bandas FM e AM;
- sintonia manual e automática;
- estações anterior e seguinte;
- memórias rápidas e favoritos;
- RDS, estéreo e intensidade de sinal quando fornecidos pelo hardware;
- volume e acesso ao equalizador;
- relógio, clima, Bluetooth e GPS;
- comandos de voz para sintonia e controle;
- barra permanente de navegação interna.

### Modo Essencial

A Home poderá ocultar partes secundárias da interface e manter somente:

- frequência e estação;
- anterior, silenciar/reproduzir e próxima;
- volume;
- voz;
- mapas;
- telefone;
- botão para restaurar o modo completo.

O usuário poderá escolher os botões visíveis e o RADIO+ memorizará sua preferência.

## 3. Módulos internos

### Música

- memória interna, cartão SD e USB;
- biblioteca por faixas, artistas, álbuns e pastas;
- playlists, favoritos e reprodução aleatória;
- rádio continua acessível por um controle compacto.

### Vídeo

- memória interna, cartão SD e USB;
- biblioteca e player próprios;
- tela cheia;
- restrições de segurança durante a condução.

### Rádio online

- busca por nome, gênero, cidade e país;
- favoritos e histórico;
- metadados, capa e nome da música quando disponíveis;
- alternância rápida entre rádio físico e online.

### Mapas

- Google Maps exibido dentro do RADIO+;
- localização atual;
- pesquisa de endereços e estabelecimentos;
- destinos, rotas e alternativas;
- orientações de rota e voz conforme APIs e autorizações aplicáveis;
- rádio ou música continuam tocando durante a navegação.

Quando Mapas estiver em segundo plano, um popup interno mostrará:

- próxima manobra;
- distância;
- rua ou saída;
- faixa recomendada, quando disponível;
- tempo restante e previsão de chegada;
- ação para ouvir novamente;
- botão para retornar ao mapa.

O popup não cobrirá câmera de ré ou chamadas e poderá ser recolhido.

### Telefone e Bluetooth

- pareamento;
- contatos;
- teclado;
- histórico;
- atendimento e encerramento;
- controle de rota do áudio;
- integração dependente dos recursos oferecidos pela central.

### Voz

- comandos em português brasileiro;
- controle de rádio, música, navegação e chamadas;
- botão de microfone sempre acessível;
- retorno visual e sonoro discreto.

### Câmera de ré

- prioridade máxima ao engatar a ré;
- retorno automático ao módulo anterior;
- integração condicionada à MCU e ao hardware.

## 4. Temas dinâmicos

Rádio físico e rádio online terão:

- tema escuro premium;
- tema claro;
- neon moderado;
- painel clássico;
- retrô;
- minimalista;
- cores personalizadas;
- imagem própria;
- fundos animados discretos;
- tema automático baseado em estação, gênero, logotipo ou capa;
- intensidade de animação configurável.

Temas nunca poderão comprometer contraste e legibilidade.

## 5. Direção Noturna

- preto profundo e painéis escurecidos;
- redução de branco intenso e luz azul;
- perfis suave, âmbar, vermelho e escuro total;
- redução de animações e imagens;
- ativação manual, por horário, pôr do sol, brilho ou faróis quando a MCU informar;
- transição gradual;
- mapa e popups acompanhando o perfil;
- retorno automático ao brilho noturno após câmera de ré.

## 6. Gratuito e VIP

### Gratuito

- rádio físico essencial;
- memórias e favoritos básicos;
- música e vídeo locais;
- rádio online básico;
- mapas;
- telefone, câmera e voz básica quando compatíveis;
- Modo Essencial;
- Direção Noturna;
- tema padrão.

### RADIO+ VIP

- coleção completa de temas;
- temas personalizados e por estação;
- fundos animados;
- equalizador e perfis avançados;
- personalização completa do Modo Essencial;
- perfis por motorista;
- backup e sincronização;
- popups de rota personalizáveis;
- comandos de voz avançados;
- recursos premium futuros;
- experiência sem anúncios.

Modelos planejados: mensal, anual e vitalício. Preços ainda não definidos.

### Lembrete VIP de cinco segundos

Para usuários gratuitos:

- tela promocional cronometrada;
- botão VIP disponível imediatamente;
- botão Continuar grátis após cinco segundos;
- restaurar compra;
- frequência controlada para evitar excesso;
- nunca sobre câmera de ré, chamada ou navegação;
- preferência por exibição somente com veículo parado.

## 7. Versão administrativa

A versão de testes permitirá simular:

- usuário gratuito;
- VIP mensal, anual e vitalício;
- assinatura expirada;
- compra pendente;
- falha de pagamento;
- compatibilidade de hardware;
- frequência e conteúdo da tela VIP;
- diferentes resoluções e modos de direção.

## 8. Limitações e validações necessárias

Antes de integrar hardware, identificar:

- marca e modelo da central;
- versão exata do Android;
- resolução;
- plataforma/SoC;
- modelo e versão da MCU;
- pacote do rádio original;
- pacote do Bluetooth;
- comportamento da câmera de ré;
- disponibilidade de Google Play Services.

O Navigation SDK do Google possui condições específicas para dispositivos embarcados. A autorização e os termos aplicáveis deverão ser verificados antes da publicação.

## 9. Regra de início

Este documento é planejamento. Nenhuma implementação ou compilação deverá começar sem aprovação explícita do proprietário.
