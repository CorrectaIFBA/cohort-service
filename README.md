# cohort-service

Microsserviço do Correcta responsável pelas **turmas** (cohorts): criação,
ciclo de vida, membros, convites diretos e códigos de convite.

Stack: Spring Boot 3.4, Kotlin 1.9, Java 17, JPA/PostgreSQL com Flyway,
OpenFeign (identity-service), RabbitMQ (e-mails transacionais), SpringDoc
OpenAPI e Actuator. As mensagens de erro da API são em português.

## Domínio

| Entidade | Descrição |
| --- | --- |
| `Cohort` | Turma com dono (perfil FACULTY), `slug`, `publicCode` e estado. |
| `CohortMembership` | Vínculo de um perfil com a turma, com papel `STUDENT` ou `FACULTY`. Membros removidos mantêm a linha com `leftAt` preenchido. |
| `CohortMembershipInvitation` | Convite direto de um docente para um perfil específico. |
| `CohortInvitationCode` | Código reutilizável (8 caracteres) para entrar na turma. Cada turma tem no máximo um código ativo. |

### Regras principais

- **Estados da turma:** `DRAFT → ACTIVE → ARCHIVED`, `DRAFT → ARCHIVED` e
  `ARCHIVED → ACTIVE`. Turmas são criadas só em `DRAFT`; só turmas `DRAFT`
  podem ser excluídas; turmas `ARCHIVED` não podem ser editadas.
- **URL da turma:** `{slug}-{publicCode}`. O `publicCode` (6 caracteres
  `[a-z0-9]`) nunca muda e identifica a turma sozinho; o `slug` pode mudar e
  pode se repetir entre turmas. Slugs reservados: `members`, `faculty`,
  `invitations`, `invitation-code`.
- **Visibilidade:** STAFF vê todas as turmas, mas não as gerencia. Os demais
  perfis veem só turmas das quais são donos ou membros atuais. Turma não
  visível responde `404`.
- **Gestão:** só o dono altera, muda estado ou exclui a turma. Só docentes
  (membros `FACULTY`) de turma `ACTIVE` gerenciam convites, códigos e membros.
  O dono não pode ser removido.
- **Elegibilidade:** convites e entradas exigem perfil `ACTIVE`, com perfil e
  credencial ativos, do tipo `STUDENT` ou `FACULTY`. O papel na turma sempre
  corresponde ao tipo do perfil.
- **Convites e códigos** expiram em no máximo 30 dias (padrão: 30 dias).
- **Membro removido** não volta por código de convite; só por novo convite
  direto. A remoção revoga os convites pendentes do perfil.
- **Aceite idempotente:** aceitar um convite já aceito responde `200`; o
  primeiro aceite responde `201`.
- Ao enviar um convite direto, o serviço publica o e-mail no RabbitMQ
  **depois do commit**. Falha no broker é registrada em log e não falha a
  requisição.

## Endpoints

Todos sob `/api/v1`. Detalhes de payloads e respostas estão nas anotações
OpenAPI dos controllers (`/swagger-ui.html` quando habilitado).

| Método | Caminho | Acesso |
| --- | --- | --- |
| `GET` | `/cohorts` | autenticado |
| `GET` | `/cohorts/{cohortId}/{slug}` | autenticado |
| `GET` | `/cohorts/by-code/{publicCode}` | autenticado |
| `POST` | `/cohorts` | FACULTY |
| `PUT` | `/cohorts/{cohortId}` | FACULTY dono |
| `PATCH` | `/cohorts/{cohortId}/status` | FACULTY dono |
| `DELETE` | `/cohorts/{cohortId}` | FACULTY dono |
| `GET` | `/cohorts/{cohortId}/members` | membro ou STAFF |
| `GET` | `/cohorts/{cohortId}/faculty` | membro ou STAFF |
| `DELETE` | `/cohorts/{cohortId}/members/{profileId}` | docente da turma |
| `POST` | `/cohorts/{cohortId}/invitations` | docente da turma |
| `GET` | `/cohorts/{cohortId}/invitations` | docente da turma |
| `GET` | `/cohorts/{cohortId}/invitations/candidates?q=&role=` | docente da turma |
| `DELETE` | `/cohorts/{cohortId}/invitations/{profileId}` | docente da turma |
| `POST` | `/cohorts/{cohortId}/invitations/accept` | perfil convidado |
| `GET` | `/cohort-invitations` | autenticado (convites pendentes do próprio perfil) |
| `POST` | `/cohorts/{cohortId}/invitation-code` | docente da turma |
| `GET` | `/cohorts/{cohortId}/invitation-code` | docente da turma |
| `DELETE` | `/cohorts/{cohortId}/invitation-code` | docente da turma |
| `POST` | `/cohort-invitation-codes/{code}/join` | autenticado e elegível |
| `GET` | `/internal/cohorts/{cohortId}/members/{profileId}` | interno, sem autenticação |

Listagens aceitam `page`, `size` (1 a 100), `sortBy` e `sortDirection`
(`asc`/`desc`). A listagem de turmas aceita também `status`, `createdAfter`,
`createdBefore`, `mine`, `search` e `memberProfileId`.

O endpoint interno retorna o papel do perfil e o estado da turma, ou `404`.
Outros serviços (ex.: assessment-service) chamam ele direto dentro do
cluster. **Não exponha `/api/v1/internal` pelo gateway.**

## Autenticação e autorização

O serviço não valida tokens. O gateway chama `GET /account/me` do
identity-service e repassa o resultado em headers:

| Header | Origem em `/account/me` |
| --- | --- |
| `X-User-Id` | `profile.profileId` |
| `X-User-Person-Id` | `person.personId` |
| `X-User-Profile-Type` | `profile.type` |
| `X-User-Login` | `account.login` |
| `X-User-Email` | `account.email` |

`HeaderAuthenticationFilter` transforma esses headers em `ProfilePrincipal`.
`SecurityAspect` aplica as anotações dos controllers:

- `@IsLoggedUser`: exige `X-User-Id`, senão `401`.
- `@IsProfileType("FACULTY")`: exige o tipo de perfil, senão `403`.

Nos serviços, use `ScopeEvaluatorComponent.authenticatedProfile()` para ler
o chamador.

> **Segurança:** os headers são aceitos sem verificação. O serviço só pode
> ser acessível pelo gateway, e o gateway deve remover headers `X-User-*`
> enviados pelo cliente.

## Integrações

- **identity-service** (`IdentityClient`, OpenFeign): `GET /api/v1/profiles/{profileId}`,
  `GET /api/v1/profiles/search` e `POST /api/v1/profiles/batch-lookup`, sempre
  com o header `X-Internal-Token`. `404` vira "perfil não encontrado"; outras
  falhas respondem `503` (fail closed).
- **RabbitMQ** (`TransactionalMailProducer`): publica
  `SendTransactionalMailRequest` na exchange do mailer com o template
  `templates/cohort-invitation/v1.html`.

## Configuração

| Variável | Padrão |
| --- | --- |
| `ENV_DATASOURCE_POSTGRES_URL` | obrigatória |
| `ENV_DATASOURCE_POSTGRES_USERNAME` | obrigatória |
| `ENV_DATASOURCE_POSTGRES_PASSWORD` | obrigatória |
| `ENV_JPA_DDL_AUTO` | `validate` |
| `ENV_SPRINGDOC_ENABLED` | `false` |
| `ENV_IDENTITY_SERVICE_URL` | obrigatória |
| `ENV_IDENTITY_SERVICE_INTERNAL_TOKEN` | obrigatória |
| `ENV_IDENTITY_SERVICE_CONNECT_TIMEOUT_MS` | `2000` |
| `ENV_IDENTITY_SERVICE_READ_TIMEOUT_MS` | `5000` |
| `ENV_RABBITMQ_URL` | obrigatória |
| `ENV_MAILER_RABBITMQ_EXCHANGE` | `send-transactional-mail-exchange` |
| `ENV_MAILER_RABBITMQ_ROUTING_KEY` | `send-transactional-mail-routing-key` |
| `ENV_APP_URL` | `https://correcta.staily.dev` |

O perfil `local` (`SPRING_PROFILES_ACTIVE=local`) traz padrões para
desenvolvimento, `ddl-auto=update` e Swagger habilitado.

## Banco de dados

O Flyway controla o schema (`src/main/resources/db/migration`); o Hibernate
só valida as entidades.

- `V1__initial_schema.sql`: tabelas `cohorts`, `cohort_memberships`,
  `cohort_membership_invitations` e `cohort_invitation_codes`.
- `V2__add_cohort_public_code.sql`: adiciona `public_code` único e remove a
  unicidade do `slug`.

Bancos criados antes do Flyway são marcados como baseline na versão 0, então
a V1 (idempotente) ainda roda neles.

## Desenvolvimento

```sh
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun   # executa localmente
./gradlew test                                   # testes + relatório JaCoCo
./gradlew check                                  # testes, Spotless e cobertura mínima
./gradlew spotlessApply                          # formata o código (ktlint)
```

No workspace, rode `./local/run cohort` na raiz (porta 8085) junto do
identity-service e do proxy. As rotas ficam em
`http://127.0.0.1:8080/api/cohort/api/v1/...`.

Os testes usam H2 em memória e mocks do identity-service e do RabbitMQ; não
precisam de serviços externos. O schema dos testes vem das entidades, porque
as migrations usam SQL do PostgreSQL.

### Testes e cobertura

- Testes unitários (Mockito) por pacote em `src/test/kotlin/io/correcta/cohort/`
  (serviços, segurança, handler, mailer, utilitários, entidades e modelos).
- Testes de integração com MockMvc: `CohortFlowTests` e
  `HeaderAuthorizationTests`.
- A cobertura atual é **100%** de linhas, branches, instruções e métodos.
  `./gradlew check` falha se linhas ou branches ficarem abaixo de 100%.
  Só a função `main` fica fora da medição.
- Relatório HTML: `build/reports/jacoco/test/html/index.html`.

O hook `.githooks/pre-commit` roda `spotlessApply` nos arquivos Kotlin
alterados. A compilação configura `core.hooksPath` automaticamente.

## Deploy

`Dockerfile` gera a imagem com JRE 17 na porta 8080. Cada push na `main`
dispara `.github/workflows/release.yml`: build e push da imagem
`docker.correcta.staily.dev/correcta/cohort-service:sha-<commit>` e
atualização da tag no overlay de produção do repositório `CorrectaIFBA/infra`.
