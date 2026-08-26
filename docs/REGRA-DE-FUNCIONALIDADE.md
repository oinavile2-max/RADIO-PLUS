# Regra obrigatória de funcionalidade

1. Todo botão visível deve executar sua função real.
2. Não são permitidos botões que abram placeholder, “em breve” ou tela vazia.
3. Um módulo só entra na navegação pública quando seu fluxo principal estiver conectado.
4. Integrações dependentes de hardware devem informar claramente a compatibilidade real.
5. Simuladores existem apenas na versão administrativa e nunca podem ser confundidos com hardware funcional.
6. Erros e indisponibilidade devem produzir retorno visível ao usuário.
7. Cada botão deve ter validação de ação antes de uma build ser considerada testável.

## Estado corrigido

Vídeo, Mapas e Telefone foram ocultados da navegação porque ainda não possuem implementação funcional. Serão reativados individualmente após a integração completa.

O rádio físico permanece dependente da identificação da MCU. A versão administrativa usa um controlador de interface explicitamente identificado como não conectado ao hardware.
