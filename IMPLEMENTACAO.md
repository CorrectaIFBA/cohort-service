# Implementação do brainstorm — cohort-service

Resumo do que foi feito a partir do `brainstorm.txt`, seguindo os padrões já existentes
(controller interface + `impl`, services com `@Transactional`, specifications, mensagens de erro
em português, autorização por anotações).

## Endpoints

Base: `/api/v1`. Todos exigem `@IsLoggedUser`.

| Método | Rota | Quem pode |
| --- | --- | --- |
| `GET` | `/cohorts` | Qualquer perfil (só vê turmas que é dono ou membro atual) |
| `GET` | `/cohorts/{cohortId}/{slug}` | Dono ou membro atual |
| `POST` | `/cohorts` | Perfil `FACULTY` |
| `PUT` | `/cohorts/{cohortId}` | `FACULTY` dono |
| `PATCH` | `/cohorts/{cohortId}/status` | `FACULTY` dono |
| `DELETE` | `/cohorts/{cohortId}` | `FACULTY` dono, turma em `DRAFT` |
| `POST` | `/cohorts/{cohortId}/invitation-code` | Docente de turma `ACTIVE` |
| `GET` | `/cohorts/{cohortId}/invitation-code` | Docente de turma `ACTIVE` |
| `DELETE` | `/cohorts/{cohortId}/invitation-code` | Docente de turma `ACTIVE` |
| `POST` | `/cohort-invitation-codes/{code}/join` | Perfil elegível |
| `POST` | `/cohorts/{cohortId}/invitations` | Docente de turma `ACTIVE` |
| `GET` | `/cohorts/{cohortId}/invitations` | Docente de turma `ACTIVE` |
| `DELETE` | `/cohorts/{cohortId}/invitations/{profileId}` | Docente de turma `ACTIVE` |
| `GET` | `/cohorts/{cohortId}/invitations/candidates?q=&role=` | Docente de turma `ACTIVE` |
| `POST` | `/cohorts/{cohortId}/invitations/accept` | Dono do convite pendente |
| `GET` | `/cohort-invitations` | Qualquer perfil (só os próprios convites pendentes de turmas `ACTIVE`) |
| `GET` | `/cohorts/{cohortId}/members` | Membro atual de turma `ACTIVE` |
| `GET` | `/cohorts/{cohortId}/faculty` | Membro atual de turma `ACTIVE` |
| `DELETE` | `/cohorts/{cohortId}/members/{profileId}` | Docente de turma `ACTIVE` |

"Docente da turma" é quem tem membership ativa com papel `FACULTY`.

## Regras de negócio

**Turmas**
- `name` mapeia para `Cohort.title`; `status` mapeia para `Cohort.state`.
- Criação só em `DRAFT`. O criador vira `ownerProfileId` e recebe membership `FACULTY`.
- Slug gerado a partir do nome quando omitido (sem acentos, minúsculo, com hífens). Em colisão,
  vira `slug-2`, `slug-3`...
- Slug informado já em uso retorna `409`. Os slugs `members`, `faculty`, `invitations` e
  `invitation-code` são reservados, porque colidem com as sub-rotas de `GET /cohorts/{id}/{slug}`.
- Slug errado em `GET /cohorts/{id}/{slug}` retorna `404`. Não há redirecionamento de slugs antigos.
- Turmas `ARCHIVED` podem ser lidas por membros, mas não editadas.
- Transições: `DRAFT → ACTIVE | ARCHIVED`, `ACTIVE → ARCHIVED`, `ARCHIVED → ACTIVE` (reativação
  permitida). Transição inválida retorna `409`. `archivedAt` é preenchido/limpo automaticamente.
- Exclusão só em `DRAFT`. Como convites e códigos exigem turma `ACTIVE`, uma turma em rascunho só tem
  a membership do dono, que é removida junto.
- Filtros da listagem: `status`, `createdAfter`, `createdBefore`, `mine`. A visibilidade é aplicada
  na query, antes da paginação.

**Códigos de convite**
- Gerados com `SecureRandom`: 8 caracteres do alfabeto `23456789ABCDEFGHJKMNPQRSTVWXYZ` (sem `0/O`,
  `1/I/L` e `U`), ~39 bits. O código é salvo em texto (coluna `code`, única). O join ignora maiúsculas e
  separadores, então `abcd-efgh` vale como `ABCDEFGH`.
- O código não é tratado como segredo: o `GET` mostra o código para docentes até expirar ou ser revogado.
- Gerar um novo código revoga o atual (fluxo de substituição). A linha da turma é bloqueada com
  `PESSIMISTIC_WRITE`, então só existe um código ativo por turma.
- Expiração padrão e máxima: 30 dias.
- Join: o código precisa estar válido, a turma `ACTIVE` e o perfil elegível no identity-service.
  O papel vem do tipo do perfil (`STUDENT` ou `FACULTY`).

**Convites diretos**
- Nova enum `MembershipInvitationState` e campos `state`, `expiresAt`, `revokedAt` em
  `CohortMembershipInvitation`.
- O perfil alvo é validado no identity-service: precisa existir, estar elegível e ter tipo igual ao
  papel pedido.
- Rejeita membro atual e convite pendente duplicado (`409`). Pendentes vencidos viram `EXPIRED`.
- Enviar um convite publica um e-mail (`templates/cohort-invitation/v1.html`) na fila do
  transactional-mail-sender, só depois do commit. Falha no RabbitMQ é registrada em log e não desfaz o convite.
- Aceitar cria a membership (ou reativa uma antiga) e marca o convite `ACCEPTED`, na mesma transação.
- Aceitar de novo retorna `200` com a membership existente (idempotente). A primeira vez retorna `201`.

**Memberships**
- Constraint única em `(cohort_id, profile_id)`. Sair da turma preenche `leftAt`, sem apagar
  histórico. Voltar à turma reativa a mesma linha (limpa `leftAt`, atualiza `role` e `joinedAt`).
- O dono não pode ser removido (`409`). Como o dono é sempre `FACULTY`, a turma nunca fica sem
  docente; por isso não há uma checagem separada de "último docente".
- "Faculty" é definido por `CohortMembership.role`. A compatibilidade com o tipo do perfil é
  garantida na hora de criar a membership.

## Integração com identity-service

- `IdentityClient` (OpenFeign) com `GET /api/v1/profiles/{profileId}` e
  `POST /api/v1/profiles/batch-lookup`, conforme o contrato do brainstorm.
- `IdentityProfileService` encapsula o client: `404` vira `null`; qualquer outra falha vira `503`
  (fail closed).
- Elegível = `state == ACTIVE`, `profileActive`, `credentialActive` e tipo `STUDENT` ou `FACULTY`.
- Listagens de membros e convites usam batch lookup (sem N+1).
- Busca de candidatos a convite: `GET /api/v1/profiles/search?q=&type=&size=` (interno). Retorna até 10
  perfis elegíveis por nome, login ou e-mail (nunca CPF), marcados como `AVAILABLE`, `MEMBER` ou `INVITED`.
- Toda chamada envia o header `X-Internal-Token` (`clients.identity-service.internal-token`).
- Timeouts configuráveis: connect 2s, read 5s. Não há retries (padrão do Feign no Spring Cloud).

## Outras mudanças

- `build.gradle.kts`: `spring-cloud-starter-openfeign` + BOM `2024.0.0` (mesma versão do
  identity-service).
- `CustomExceptionHandler`: novos handlers para `503` (`ServiceUnavailableException`), `409`
  (`DataIntegrityViolationException`) e `400` (corpo JSON inválido, como enum desconhecido).
- `PaginationRequest.sortBy` agora é opcional. Cada endpoint define seu campo padrão
  (`createdAt`, `joinedAt`, `invitedAt`).
- `ScopeEvaluatorComponent.authenticatedProfile()` centraliza a leitura do principal.
- O recurso de exemplo (`Examples*`) foi removido e os testes foram reescritos para turmas.

## Testes

`./gradlew test` passa com 7 testes:
- `HeaderAuthorizationTests`: `401` sem headers, `403` para `STUDENT` criando turma, slug gerado,
  visibilidade (`404` para não membro e slug errado).
- `CohortFlowTests` (identity-service mockado com `@MockitoBean`): transições de status, exclusão só
  em `DRAFT`, fluxo completo de código (gerar, entrar, duplicado, revogar), convite direto (papel
  incompatível, duplicado, aceitar `201`/`200`), listagem de docentes e remoção de membros.

## Decisões que divergem do brainstorm

| Brainstorm | Implementado | Motivo |
| --- | --- | --- |
| Validar JWT localmente | Mantida a autenticação por headers `X-User-*` do gateway | Padrão atual do projeto (ver README). O gateway já valida o token no identity-service. |
| Erros com `ProblemDetail` | Mantido `ErrorResponse` / `ValidationErrorResponse` | Padrão atual do projeto, igual ao identity-service. |
| `publicId` e `parameters` | Não adicionados | O próprio brainstorm diz para adicionar só se o produto precisar. |
| `sort` | `sortBy` + `sortDirection` | Padrão atual de `PaginationRequest`. |
| `410 Gone` para código expirado | `404` | O brainstorm deixa opcional; `404` não revela se o código existiu. |

## Pendências resolvidas (fora deste repositório)

**identity-service**
- Novo `ProfileLookupController`: `GET /api/v1/profiles/{profileId}` e
  `POST /api/v1/profiles/batch-lookup` (máximo 100 IDs, IDs desconhecidos em `notFoundProfileIds`).
  `displayName` usa o nome social quando existe.
- `@IsInternalRequest` agora funciona. O `AuthenticationFilter` lê `X-Internal-Token` só em
  `/api/v1/profiles/**` e compara em tempo constante com `identities.internal.token`
  (`ENV_IDENTITIES_INTERNAL_TOKEN`). Token vazio no servidor rejeita tudo; o token não autentica
  outras rotas.
- `InstitutionalProfile.type` e `.state` passaram para `EnumType.STRING`.
- `db/postgresql/schema.sql` roda antes do Hibernate (`spring.sql.init`). Ele converte as colunas
  ordinais existentes (`0/1/2`) para texto e remove os check constraints antigos. É idempotente:
  não faz nada se a coluna já for texto ou se a tabela não existir. Para desligar:
  `ENV_SQL_INIT_MODE=never`.
- Novo `ProfileLookupTests`. Os 3 testes passam.

**local/** (workspace, sem git)
- `proxy/routes.json`: rota `/api/cohort` para `127.0.0.1:8085`.
- `run`: comandos `cohort` e `cluster-cohort`. `identity` e `cluster-identity` exportam
  `ENV_IDENTITIES_INTERNAL_TOKEN` (padrão `local-internal-token`), e o cohort reaproveita o mesmo valor.
- `postgres/init.sql`: cria `cohort_service` (só em volume novo).
- `.env.example` e `README.md` documentam as novas portas e variáveis.

**Validação**
- Migração testada em Postgres 18 descartável, com dados ordinais reais, rodando 2x e sem tabela.
- Smoke test ponta a ponta (identity + cohort reais): criar turma, ativar, convidar aluno (perfil
  enriquecido via Feign), perfil inexistente `404`, aceitar `201` e listar membros com nomes.

## Ainda em aberto

- A URL externa fica `/api/cohort/api/v1/...`, porque o proxy remove só o prefixo. Para tirar o
  `/api/v1` duplicado, basta mudar os `@RequestMapping` dos controllers.
- No cluster: criar o banco `cohort_service` e definir o mesmo token interno nos dois serviços.
  Os manifests do identity e do cohort não estão no repositório `infra`.
- Ordenar perfis por `type`/`state` no identity agora é alfabético (antes era pela ordem do enum).
- Fase 4: rate limit no join (mais importante agora, com o código curto), audit log, tratamento de desativação de perfil e cache de perfis.
