# Escapa! — Plataforma de Cursos (Backend)

Backend da plataforma de **Educação Continuada da ESCAPA**, em **Java 21 + Spring Boot**, para cursos, usuários, módulos, progresso e gestão administrativa. O código é organizado **por feature**: cada funcionalidade tem sua pasta com controller, service, repository, entity, dto e exception.

O guia completo de regras para quem desenvolve ou revisa está em [AGENTS.md](AGENTS.md). Como as camadas se conversam está em [docs/layers/flow-between-layers.md](docs/layers/flow-between-layers.md), e cada camada tem seu documento em [docs/layers/](docs/layers/).

---

## 📋 Sumário

- [Visão Geral e Contexto](#-visão-geral-e-contexto)
- [Estrutura de Pastas e Onde Desenvolver](#-estrutura-de-pastas-e-onde-desenvolver)
  - [Por que essa estrutura?](#-por-que-essa-estrutura)
  - [Como as camadas se conversam (docs/layers/flow-between-layers.md)](docs/layers/flow-between-layers.md)
  - [Cada camada em detalhe (docs/layers/)](docs/layers/)
- [Tecnologias Utilizadas](#-tecnologias-utilizadas)
- [Pré-requisitos](#-pré-requisitos)
- [Instalação e Setup](#-instalação-e-setup)
- [Comandos Disponíveis (Maven)](#-comandos-disponíveis-maven)
- [Fluxo de Validação de Tarefas](#-fluxo-de-validação-de-tarefas-evite-falhas-na-ci)
- [Padrão de Commits](#-padrão-de-commits)
- [Estratégia de Branches](#-estratégia-de-branches)
- [Regras de Arquitetura](#-regras-de-arquitetura)
- [Erros e contrato de API](#-erros-e-contrato-de-api)
- [Endpoints](#-endpoints)
- [Containerização com Docker](#-containerização-com-docker)
- [Banco de dados](#-banco-de-dados)
- [Pendências conhecidas](#-pendências-conhecidas)

---

## 🎯 Visão Geral e Contexto

### O que é a plataforma?
Uma plataforma digital dedicada a cursos, capacitação profissional e certificação para estudantes, profissionais e empresas do setor de turismo e hospitalidade.

### Funcionalidades já implementadas
- **Cadastro e consulta de usuários** (`/api/v1/users`)
- **Vitrine pública de cursos** com busca, filtros e paginação (`/api/v1/public/courses`, US-01)
- **Health check** (`/api/v1/health`)
- Schema completo de cursos, módulos, aulas, matrículas, avaliações e notificações, versionado por Flyway

### 🚫 Fora do escopo atual
- autenticação e autorização (só existe o bean `PasswordEncoder`)
- integrações com pagamentos e IA
- streaming em tempo real
- multi-tenancy e multilíngue

---

## 📁 Estrutura de Pastas e Onde Desenvolver

```text
src/main/java/com/escapa/backend/
├── EscapaBackendApplication.java
│
├── common/                      → transversal, sem regra de negócio
│   ├── api/                     → ApiResponse, ApiError, PageResponse, GlobalExceptionHandler, HealthController
│   ├── exception/               → NotFoundException, ConflictException, BusinessRuleException
│   └── config/                  → CORS, OpenAPI, PasswordEncoder, log de startup
│
├── user/                        → feature de usuários
│   ├── controller/              → UserController
│   ├── service/                 → UserService
│   ├── repository/              → UserRepository
│   ├── entity/                  → UserEntity, AdminEntity, RegularUserEntity, CompanyEntity, ...
│   ├── dto/                     → CreateUserRequest, UserResponse
│   └── exception/               → UserNotFoundException, EmailAlreadyUsedException
│
├── course/                      → feature de cursos, dividida em subfeatures
│   ├── shared/entity/           → entidades e enums usados por todas as subfeatures
│   ├── catalog/                 → vitrine pública: controller, service, repository (só leitura), dto, exception
│   ├── management/              → CRUD do admin: repository e dto prontos, demais pastas com package-info
│   └── review/                  → avaliações: só package-info por enquanto
│
├── enrollment/                  → matrículas: entity pronta, demais pastas com package-info
└── notification/                → notificações: entity pronta, demais pastas com package-info

src/main/resources/
├── application.properties
├── application-dev.properties   → perfil dev: aplica o seed
└── db/
    ├── migration/               → V1..V6 (Flyway)
    └── seed/R__seed_dev.sql     → dados de desenvolvimento
```

### 🤔 Por que essa estrutura?

O projeto nasceu em Clean Architecture (`domain`, `application`, `adapters`, `infrastructure`, com portas, adapters, mappers e casos de uso). Em setembro de 2026 o time trocou por pacote por feature. Os motivos, para ninguém precisar refazer a discussão:

- **O sistema é majoritariamente CRUD.** Regras de negócio são pontuais (email único, curso publicado, contadores). Clean Architecture paga quando há múltiplos pontos de entrada, múltiplos bancos ou regras complexas que valem testar isoladas. Aqui não há nenhum dos três.
- **O custo fixo era alto.** Um `GET` de listagem exigia oito arquivos e três cópias do mesmo objeto (`CourseEntity`, `CourseSummary`, `CourseCardResponse`) ligadas por construtores posicionais de doze argumentos. Adicionar um campo tocava quatro arquivos; adicionar um filtro, cinco.
- **O domínio era peso morto.** Nove classes de domínio, oito sem uso, nenhuma com regra, todas divergindo das entidades JPA. Era Clean Architecture em pastas, com domínio anêmico.
- **Os fakes duplicavam o banco.** Cada porta tinha um fake em memória que reimplementava a consulta em Java e precisava ser mantido em sincronia com a JPQL na mão. Já tinha divergido uma vez.
- **A promessa de trocar framework não se cumpre.** Ninguém vai trocar Spring nem Postgres, e a regra mais importante do banco (`lessons_count`) já vivia num trigger PL/pgSQL.
- **Time de estudantes com rotatividade.** Cada pessoa nova pagava o custo de entender porta, adapter, fake, read model e fiação manual de beans antes de escrever o primeiro endpoint.

O que a estrutura por feature dá em troca:

- **Quatro arquivos no caminho de um pedido** (controller, service, repository, entity), com nome de papel na pasta e no arquivo. Quem abre `catalog/service/CourseCatalogService.java` sabe o que é sem ler.
- **Tudo de uma feature no mesmo lugar.** Apagar ou mover uma feature é apagar ou mover uma pasta.
- **Teste por papel, com a ferramenta certa.** Mockito para regra, banco real para consulta, MockMvc para contrato. Sem fake.
- **Erro com dono.** Quatro famílias, três classes base, um handler que nunca muda. Feature nova só cria suas exceções.

O que foi mantido de propósito: Flyway, `record` para DTO, `ApiResponse`/`ApiError`, `GlobalExceptionHandler`, Checkstyle, Testcontainers. Eram baratos e funcionavam.

O que ficou explicitamente fora desse refactor: o banco. Nenhuma migration, coluna, constraint ou trigger mudou.

Se um dia o sistema crescer para múltiplas entradas ou regras pesadas, o caminho natural é **multi-módulo Maven**, um módulo por feature, não voltar às camadas globais.

### 🧭 Guia prático: onde colocar meu código?

Dentro de uma feature ou subfeature, o fluxo é sempre:

```text
Controller  →  Service  →  Repository  →  Entity
  DTO in        regra       Spring Data      JPA
  DTO out    @Transactional
```

| Pasta | O que vai aqui | O que nunca vai aqui |
|---|---|---|
| `controller/` | rota, `@Valid`, chamada a **um** service, DTO de saída | import de repository ou entity, `if` de regra |
| `service/` | toda decisão de negócio, `@Transactional`, exceções da feature | `HttpStatus`, `ResponseEntity` |
| `repository/` | interface Spring Data, JPQL, projeção `SELECT new` | lógica |
| `entity/` | mapeamento JPA com sufixo `Entity` | lógica |
| `dto/` | `record`s: entrada com Bean Validation, saída com `from(entity)` | anotações JPA |
| `exception/` | exceções da feature herdando das bases em `common.exception` | `HttpStatus` |

Toda pasta fora de `common` é uma feature com o template completo (`controller`, `service`, `repository`, `entity`, `dto`, `exception`). Pasta ainda sem classe tem um `package-info.java` dizendo o que vai ali. Uma feature vira subfeatures quando tem mais de um contexto de uso (ex.: `course` tem vitrine pública, gestão do admin e avaliação); nesse caso as entidades ficam em `shared/entity` e cada subfeature tem as outras cinco pastas. Nunca divida por camada (`course/controller/`, `course/service/`). O health check não é feature e mora em `common/api`.

Os testes espelham a mesma árvore em `src/test/java`, no mesmo pacote da classe testada. Cada feature já nasce com `controller/`, `service/` e `repository/` de teste (com `package-info.java` dizendo o tipo de teste e a base a estender); `dto/`, `entity/` e `exception/` não têm pasta de teste.

---

## 🛠️ Tecnologias Utilizadas

- **Linguagem**: Java 21
- **Framework**: Spring Boot 3.5.5
- **Persistência**: Spring Data JPA + PostgreSQL 16
- **Migrações**: Flyway (`src/main/resources/db/migration/`)
- **Build**: Maven
- **Validação**: Bean Validation
- **Documentação**: SpringDoc OpenAPI (Swagger UI)
- **Qualidade**: Checkstyle (fase `validate`), Jacoco (`target/site/jacoco/`)
- **Testes**: JUnit 5, Mockito, Spring Test (MockMvc), Testcontainers
- **Containerização**: Docker + Docker Compose

---

## ⚙️ Pré-requisitos

- **Java 21** ou superior
- **Maven 3.9+**
- **Docker** e **Docker Compose** — **obrigatórios para rodar os testes**: os testes de repository e de controller sobem um PostgreSQL descartável via Testcontainers
- **Git**

---

## 🚀 Instalação e Setup

1. **Clone o repositório:**
   ```bash
   git clone <url-do-repositorio>
   cd Backend
   ```

2. **Suba o banco e a aplicação**, de uma das duas formas:

   **Opção A — tudo em containers**
   ```bash
   docker compose up --build          # ou ./scripts/dev-up.sh --build, que desvia de porta ocupada
   ```

   **Opção B — banco em container, aplicação pelo Maven** (sem reconstruir a imagem a cada mudança; reinicie o `mvn spring-boot:run` para aplicar alterações de código)
   ```bash
   docker compose up -d postgres
   mvn spring-boot:run
   ```

   O `mvn spring-boot:run` sobe com o perfil **`dev`**, que aplica o seed (`db/seed/R__seed_dev.sql`) depois das migrations. O seed trunca e recria os dados a cada start; para preservar dados manuais use `mvn spring-boot:run -Dspring-boot.run.profiles=default`.

3. **Configuração** (opcional). Os padrões já batem com o `docker-compose.yml`: nada precisa ser configurado para rodar localmente. Se o seu ambiente for diferente, copie `.env.example` para `.env` e ajuste só o que muda. **Um arquivo serve os dois modos**: o Docker Compose lê `.env` por natureza, e o perfil `dev` do Spring o importa via `spring.config.import`. Fora de `dev` (jar, homologação, produção) só variáveis de ambiente valem. Variável exportada no terminal tem prioridade sobre o arquivo.

> 🔌 **Porta do banco.** O container do Postgres é exposto na **15432** da sua máquina (`POSTGRES_HOST_PORT`), não na 5432. Motivo: quem tem PostgreSQL instalado no Windows ou no Mac já ocupa a 5432, e uma segunda instalação ocupa a 5433, 5434 e assim por diante; `localhost:5432` cairia nesse servidor em vez do container, com erro de autenticação na subida. Dentro da rede do Compose o banco continua em `postgres:5432`. Para conectar com um cliente SQL na sua máquina, use `localhost:15432`, usuário `escapa`, senha `escapa123`.

> 🔌 **Porta da API.** Padrão 8080. Se estiver ocupada (um Vite ou outro serviço local):
> - **Pelo Maven**, o perfil `dev` faz fallback sozinho: tenta 8081, 8082, 8083 e loga em `WARN` qual usou. O bloco de log do fim da subida mostra a URL real. Em homologação e produção o fallback fica desligado e a porta é fixa.
> - **Pelo Docker**, use `./scripts/dev-up.sh` (Git Bash) ou `.\scripts\dev-up.ps1` (PowerShell) em vez de `docker compose up`: o script acha a primeira porta livre a partir de 8080 e sobe o compose com ela. Ou fixe `SERVER_PORT=8081` no `.env`, que vale para os dois modos.
>
> Em qualquer caso, **o frontend precisa apontar para a porta escolhida**; a API não tem como avisá-lo.

A API fica em `http://localhost:8080`. A raiz redireciona para o Swagger.

---

## 💻 Comandos Disponíveis (Maven)

| Comando | O que faz? | Quando usar? |
|---|---|---|
| `mvn spring-boot:run` | Sobe a aplicação com perfil `dev` (migrations + seed). | Desenvolvimento. |
| `mvn spring-boot:run -Dspring-boot.run.profiles=default` | Sobe sem o seed. | Preservar dados manuais. |
| `mvn test` | Roda todos os testes. **Requer Docker.** | Antes de commit. |
| `mvn -B clean verify` | O mesmo que a CI: Checkstyle, testes e cobertura. **Requer Docker.** | Antes de abrir o MR. |
| `mvn test -Dtest=UserServiceTest` | Roda uma classe de teste. Testes de service não precisam de Docker. | Ciclo rápido. |
| `mvn checkstyle:check` | Só as regras de `checkstyle.xml`. | Checar estilo isolado. |
| `mvn clean package` | Gera o jar. | Build final. |

---

## ✅ Fluxo de Validação de Tarefas (Evite falhas na CI)

```bash
mvn -B clean verify
```

> ⚠️ O build falha com violação de `checkstyle.xml`: chaves obrigatórias em `if`, variáveis locais `final`, sem número mágico fora de 0–5, 10, 100 e 1000, método com até 40 linhas. Testes estão isentos.

> 🐳 **Docker precisa estar rodando.** Sem ele os testes de repository e controller falham na subida do container, não por erro no seu código.

---

## 📝 Padrão de Commits

```text
<tipo>(<id_clickup>): <descrição clara>
```

Tipos: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`.

Exemplos: `feat(86a1b2c): add user creation flow`, `test(86a1b2f): add unit tests for user service`.

---

## 🌿 Estratégia de Branches

```text
main (produção)  ←  develop (integração)  ←  <tipo>/<id_clickup>-<descricao>
```

```bash
git checkout develop && git pull origin develop
git checkout -b feat/86a1b2c-criar-usuario
# ... desenvolver, validar ...
git push -u origin feat/86a1b2c-criar-usuario
```

MR sempre para `develop`.

---

## 🧱 Regras de Arquitetura

1. **Dentro de uma feature ou subfeature, a seta só anda para a direita.** Controller → Service → Repository → Entity.
2. **Entre subfeatures e entre features, só pelo service.** Nunca importe repository de outra feature.
3. **Entidades JPA podem referenciar entidades de qualquer feature.** É o mapeamento do banco.
4. **`shared/` de uma feature** guarda o que duas ou mais subfeatures usam.
5. **`common/` não conhece nenhuma feature.**
6. **Só o service tem `@Transactional`** (`readOnly = true` em leitura).
7. **Feature ou subfeature nova nasce com o template completo**, mesmo vazio, com `package-info.java` em cada pasta.
8. **Repositório só de leitura** estende `Repository<T, ID>`, não `JpaRepository`, para não expor `save`/`delete`.

Detalhes e exemplos em [AGENTS.md](AGENTS.md).

---

## 🚨 Erros e contrato de API

Todas as rotas usam `/api/v1`. Rotas públicas ficam em `/api/v1/public/**`.

Sucesso usa `ApiResponse`:
```json
{ "success": true, "data": { }, "message": "Operation completed successfully" }
```

Endpoints paginados devolvem `PageResponse` direto, por contrato com o frontend:
```json
{ "content": [ ], "pageNumber": 0, "pageSize": 10, "totalElements": 1, "totalPages": 1 }
```

Erro usa `ApiError`. O campo `code` é estável e feito para o frontend decidir o que exibir; `message` é para humanos.
```json
{
  "status": 404,
  "error": "Not Found",
  "code": "USER_NOT_FOUND",
  "message": "User not found: 3f2a...",
  "path": "/api/v1/users/3f2a...",
  "timestamp": "2026-09-11T14:00:00Z"
}
```

| Família | HTTP | `code` |
|---|---|---|
| Validação de entrada | 400 | `VALIDATION_ERROR`, `INVALID_PARAMETER`, `MISSING_PARAMETER`, `MALFORMED_REQUEST` |
| Regra de negócio | 404 / 409 / 422 | definido pela exceção da feature (`USER_NOT_FOUND`, `EMAIL_ALREADY_USED`, ...) |
| Protocolo HTTP | 404 / 405 / 415 | `RESOURCE_NOT_FOUND`, `METHOD_NOT_ALLOWED`, `UNSUPPORTED_MEDIA_TYPE` |
| Infraestrutura | 409 / 500 | `DATA_CONFLICT`, `INTERNAL_ERROR` (detalhe só no log) |

---

## 📌 Endpoints

### Health
```http
GET /api/v1/health
```

### Vitrine pública de cursos (US-01)
```http
GET /api/v1/public/courses?title=ia&category=Inteligência Artificial&level=Iniciante&page=0&size=10
```

Sem autenticação. Todos os parâmetros são opcionais.

| Parâmetro | Comportamento |
|---|---|
| `title` | busca parcial, sem diferenciar caixa; `%` e `_` são texto literal |
| `category`, `level` | igualdade sem diferenciar caixa (`Iniciante` = `INICIANTE`) |
| `page` | 0-based; padrão 0; negativo vira 0 |
| `size` | padrão 10; máximo 100 |

Só cursos com `status = PUBLISHED`. Ordem: mais recentes primeiro, desempate por id. `lessonsCount`, `ratingAverage` e `reviewsCount` vêm desnormalizados da tabela `courses`; `instructor` é o nome do admin responsável.

Resposta:
```json
{
  "content": [
    {
      "id": "a1b2c3d4-...",
      "title": "IA Aplicada ao Turismo",
      "shortDescription": "Domine as ferramentas de inteligência artificial",
      "category": "Inteligência Artificial",
      "level": "Iniciante",
      "durationTime": 12,
      "lessonsCount": 32,
      "price": 97.0,
      "thumbnailUrl": "https://cdn.escapa.com.br/courses/101/thumb.jpg",
      "instructor": "Dra. Mariana",
      "ratingAverage": 4.8,
      "reviewsCount": 56
    }
  ],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

### Cadastro de usuário
```http
POST /api/v1/users
```
```json
{ "name": "Maria Souza", "email": "maria@email.com", "password": "senha12345", "userType": "student" }
```

Resposta `201`:
```json
{
  "success": true,
  "data": { "id": "uuid", "name": "Maria Souza", "email": "maria@email.com", "userType": "STUDENT", "createdAt": "2026-09-11T14:00:00" },
  "message": "User created successfully"
}
```

`userType` aceita `STUDENT`, `ADMIN` ou `COMPANY`, sem diferenciar caixa. `STUDENT` cria a linha em `regular_users`, `ADMIN` em `admins` (e por isso pode ser instrutor). `COMPANY` devolve `422 USER_TYPE_NOT_SUPPORTED` até existir o cadastro de empresa.

`400 VALIDATION_ERROR` para corpo inválido ou `userType` fora do vocabulário, `409 EMAIL_ALREADY_USED` para email repetido.

### Listagem e consulta de usuários
```http
GET /api/v1/users
GET /api/v1/users/{id}     → 404 USER_NOT_FOUND quando não existe
```

Documentação interativa: `http://localhost:8080/swagger-ui/index.html`.

---

## 🐳 Containerização com Docker

```bash
docker compose up --build
```

Sobe PostgreSQL e o backend. O backend só inicia após o healthcheck do banco, aplica as migrations e o seed, e responde em `http://localhost:8080`. O Postgres fica acessível da sua máquina em `localhost:15432` (ver `POSTGRES_HOST_PORT` em `.env.example`).

### 🌐 CORS

Origens liberadas vêm de `APP_CORS_ALLOWED_ORIGINS` (padrão `http://localhost:3000`), aplicadas a `/api/**`. Nunca `*`.

---

## 🗄️ Banco de dados

Schema versionado por Flyway em `src/main/resources/db/migration/`. O Hibernate roda com `ddl-auto=validate`: só confere se entidade e tabela batem, nunca cria nem altera. Se uma entidade mudar coluna ou constraint sem migration, a aplicação não sobe.

| Migration | O que cria |
|---|---|
| `V1` | `users` |
| `V2` | `status` em `users`; `admins`, `company`, `regular_users`, `users_company` (herança JOINED) |
| `V3` | `courses`, `course_prerequisites`, `course_materials`, `course_change_log`, `modules`, `module_prerequisites`, `content` |
| `V4` | `user_courses`, `company_courses`, `course_reviews`, `notifications` |
| `V5` | trigger `trg_sync_lessons_count`, que mantém `courses.lessons_count` |
| `V6` | `CHECK` de `users.role`, faixas (`progress`, `price`, ordens, contadores, `rating_average`), datas coerentes em matrículas; triggers `trg_sync_review_counters` (`reviews_count`, `rating_average`) e `trg_sync_materials_count` (`materials_count`) |

Regras:
- Migration aplicada é **imutável**. Correção vem como migration nova.
- Constraints (unique, FK, check, default) e contadores desnormalizados por trigger ficam no banco. Regra de decisão (publicar, bloquear, notificar) fica no service, nunca em trigger.
- O seed `db/seed/R__seed_dev.sql` só roda no perfil `dev` e no Docker Compose. Homologação, produção e testes nunca o veem.

Diagrama: [docs/database.png](docs/database.png) (fonte em [docs/database.puml](docs/database.puml)).

---

## 📝 Pendências conhecidas

- **`courses.students_count` sem mecanismo de atualização**: só tem valor pelo seed. Os demais contadores (`lessons_count`, `reviews_count`, `rating_average`, `materials_count`) são mantidos por trigger (V5 e V6), com testes em `course/shared/entity/*TriggerTest`. `students_count` entra quando a US de matrícula definir o que conta (matrícula expirada? licença de empresa?).
- **`course/management`** tem só repositórios e DTOs; **`course/review`**, **`enrollment`** e **`notification`** têm só entidades ou `package-info`. Controllers e services chegam com as USs correspondentes.
- **Autenticação**: quando entrar, `/api/v1/public/**` precisa ficar na whitelist em `common.config.SecurityConfig`. Até lá, `POST /api/v1/users` aceita `userType=ADMIN` sem nenhuma proteção, e o admin criado vale como instrutor. Decisão registrada: manter assim até a US de autenticação; se o time preferir, basta remover `ADMIN` do `@Pattern` em `CreateUserRequest`.
- **Cadastro de empresa**: `userType=COMPANY` devolve `422 USER_TYPE_NOT_SUPPORTED`, porque empresa exige razão social e CNPJ que o endpoint de usuário não recebe. Entra com a US de empresas, provavelmente em endpoint próprio.
- **`level` e `category` de curso são texto livre.** A vitrine compara sem diferenciar caixa, mas o vocabulário não tem dono (`INICIANTE` no seed, `Iniciante` na US). Decisão pendente com o frontend: `level` como enum (`BEGINNER`, `INTERMEDIATE`, `ADVANCED`) e `category` como tabela própria. Muda o contrato da vitrine, então vira migration V7 só depois do alinhamento.
- **Exclusão de curso**: a política (arquivar em vez de apagar; hard delete só de `DRAFT` sem matrícula) está no AGENTS.md e será implementada no service de `course/management`.
