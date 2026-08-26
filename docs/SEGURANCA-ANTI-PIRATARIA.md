# Segurança, integridade e proteção do VIP

## Regra central

O aplicativo nunca considera o usuário VIP apenas por uma variável, preferência ou banco local. O direito VIP deve ser confirmado por um servidor seguro usando o token de compra da Google Play e um veredito válido da Play Integrity API.

Nenhum segredo de servidor, chave da Google Play Developer API ou chave privada pode ser incluído no APK/AAB.

## Arquitetura obrigatória

1. O app inicia a compra pelo Google Play Billing.
2. O token da compra é enviado por HTTPS ao backend do RADIO+.
3. O backend consulta a Google Play Developer API e valida produto, pacote, estado, usuário e token.
4. O backend valida um token recente da Play Integrity vinculado à requisição.
5. Apenas o backend concede ou renova o direito VIP.
6. Cancelamentos, reembolsos e revogações chegam ao backend por Real-time Developer Notifications e Voided Purchases API.
7. O app recebe uma autorização curta, assinada e renovável; nunca recebe uma chave mestra nem decide sozinho que a compra é válida.

## Camadas da versão de produção

- Google Play Billing com verificação e acknowledgment no backend.
- Play Integrity: integridade do app, licença da conta, integridade do dispositivo, risco de captura/controle e Play Protect.
- Automatic integrity protection ativada no painel Protected with Play quando elegível.
- Assinatura por Play App Signing e separação total entre chaves admin, debug e release.
- R8, otimização e remoção de recursos na release.
- Backup Android desativado para dados do aplicativo.
- TLS/HTTPS obrigatório para autenticação, compras, VIP e APIs privadas.
- Tokens curtos, rotacionáveis, vinculados a usuário, instalação, versão e nonce/hash da operação.
- Sem credenciais em código, recursos, BuildConfig, assets ou repositório Git.
- Logs da release sem tokens, dados pessoais, respostas de compra ou detalhes internos de segurança.
- Componentes Android não exportados por padrão e permissões mínimas.
- Detecção e telemetria de APK alterado, assinatura inválida, instalação fora da Play, automação arriscada e repetição de tokens.
- Respostas graduais: renovar verificação, restringir somente recursos VIP, solicitar correção pela Play e bloquear abuso confirmado.

## Compatibilidade com Android 7 e centrais genéricas

O mínimo do RADIO+ é API 24, compatível com as requisições da Play Integrity. Porém, algumas centrais genéricas não são certificadas pela Google, não possuem Play Store oficial ou usam ROM modificada. Por isso, os sinais de integridade devem ser avaliados no backend com política controlada, sem bloquear automaticamente usuários legítimos apenas por um único sinal.

O modo offline pode manter uma autorização VIP assinada e de curta validade. Ele não pode liberar VIP indefinidamente sem nova validação, pois isso tornaria simples congelar ou modificar o estado local.

## Rádio HTTP legado

Algumas estações online ainda usam fluxos HTTP. Essa exceção deve ficar isolada ao player de rádio e nunca pode ser reutilizada por login, compras, VIP, atualização ou APIs privadas. Antes da release, será criada uma política de segurança de rede com HTTPS como padrão e exceções mínimas compatíveis com os fluxos realmente suportados.

## Administração e testes

A versão administrativa usa applicationId separado e nunca pode conceder VIP válido na produção. Bypass de teste deve existir somente em ambiente de teste da Google Play e backend de homologação. Nenhum menu secreto ou código universal de desbloqueio será incluído na versão pública.

## Critério para liberar a versão Play Store

A versão pública não será considerada pronta enquanto compra, restauração, cancelamento, reembolso, expiração, reinstalação, modo offline, APK alterado, assinatura incorreta e falha da Play Integrity não tiverem comportamento testado e documentado.
