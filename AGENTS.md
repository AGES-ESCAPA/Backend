# AGENTS.md — Escapa! Backend

> Este arquivo é lido por agentes de IA (Copilot, Cursor, Gemini, Claude, etc.) e por quem entra no time. **Leia-o completamente antes de sugerir ou gerar código.** É a fonte de verdade sobre como o projeto está organizado.

## Contexto do Projeto

**Escapa!** é uma plataforma digital de **cursos e qualificação profissional em Turismo e Hospitalidade**.

- **Stack**: Java 21, Spring Boot 3.5, Spring Data JPA, Bean Validation, PostgreSQL 16, Flyway, SpringDoc OpenAPI, Checkstyle, Jacoco, JUnit 5, Mockito, Testcontainers, Docker Compose, GitHub Actions.
- **Arquitetura**: **pacote por feature**, com subfeatures quando a feature tem mais de um contexto de uso, e pastas por papel (`controller`, `service`, `repository`, `entity`, `dto`, `exception`) dentro de cada uma.
- **Natureza do sistema**: majoritariamente CRUD com regras pontuais. Por isso não há camadas de domínio, casos de uso, portas ou adapters. Esse desenho foi usado e abandonado em setembro/2026 por custar mais do que entregava aqui.

**🚫 Fora do escopo atual**: autenticação e autorização (existe só o bean `PasswordEncoder`), pagamentos, IA, streaming, multi-tenancy, multilíngue.


## Por que pacote por feature (registro da decisão)

Decidido em setembro/2026, substituindo Clean Architecture. Resumo para agente ou pessoa que pensar em "melhorar" a estrutura:

- Sistema CRUD com regras pontuais: não há múltiplos pontos de entrada, múltiplos bancos nem regra complexa que justifique domínio isolado.
- A versão anterior exigia oito arquivos e três cópias do mesmo objeto por endpoint, tinha nove classes de domínio sem regra (oito sem uso) e fakes em memória que duplicavam a JPQL e divergiam dela.
- Trocar Spring ou Postgres não é cenário real; a regra mais importante do banco já era trigger.
- Time com rotatividade: custo de onboarding pago a cada semestre.

Não proponha reintroduzir portas, adapters, casos de uso, mappers, entidades de domínio separadas das JPA ou fakes em memória. Se o projeto crescer, a evolução é multi-módulo Maven por feature, não camadas globais. O README tem a versão longa desta seção.

---

## Estrutura de Pastas

```text
com.escapa.backend
├── EscapaBackendApplication.java
│
├── common/                          # transversal, sem regra de negócio, não conhece nenhuma feature
│   ├── api/                         # ApiResponse, ApiError, PageResponse, GlobalExceptionHandler, HealthController
│   ├── exception/                   # BusinessException, NotFoundException, ConflictException, BusinessRuleException
│   └── config/                      # CorsConfig, OpenApiConfig, SecurityConfig, StartupInfoLogger
│
├── user/
│   ├── controller/  UserController
│   ├── service/     UserService
│   ├── repository/  UserRepository
│   ├── entity/      UserEntity, AdminEntity, RegularUserEntity, CompanyEntity, UsersCompanyEntity, UsersCompanyId, UserStatus
│   ├── dto/         CreateUserRequest, UserResponse
│   └── exception/   UserNotFoundException, EmailAlreadyUsedException
│
├── course/
│   ├── shared/entity/               # entidades e enums usados por todas as subfeatures de curso
│   ├── catalog/                     # vitrine pública (US-01): controller, service, repository (só leitura), dto, exception
│   ├── management/                  # CRUD do admin: repository e dto prontos; demais pastas com package-info
│   └── review/                      # avaliações: só package-info por enquanto
│
├── enrollment/                      # entity pronta (UserCourseEntity, CompanyCourseEntity); demais pastas com package-info
└── notification/                    # entity pronta (NotificationEntity, NotificationType); demais pastas com package-info
```

**Toda pasta abaixo de `com.escapa.backend`, exceto `common`, é uma feature com o template completo.** Feature simples: `controller`, `service`, `repository`, `entity`, `dto`, `exception`. Feature com subfeatures: `shared/entity` mais, em cada subfeature, `controller`, `service`, `repository`, `dto`, `exception`. Pasta ainda sem classe tem um `package-info.java` dizendo o que vai ali. O health check não é feature (não tem service, regra nem dado) e por isso mora em `common/api`.

Migrations Flyway ficam em `src/main/resources/db/migration/`, seed de desenvolvimento em `src/main/resources/db/seed/`.

### Papel de cada pasta

Versão resumida. Como as camadas se conversam, com dois pedidos reais percorrendo o fluxo, está em [docs/camadas/fluxo-entre-camadas.md](docs/camadas/fluxo-entre-camadas.md). Cada camada tem seu documento (objetivo, por que existe, por que foi feita assim, exemplos) em [docs/camadas/](docs/camadas/).

```text
Controller  →  Service  →  Repository  →  Entity
  DTO in        regra       Spring Data      JPA
  DTO out    @Transactional
```

- **`controller/`**: recebe a requisição, valida com `@Valid`, chama **um** service, devolve DTO. Nunca importa repository nem entity. Nunca decide nada.
- **`service/`**: único lugar com `@Transactional` (`readOnly = true` para leitura). Toda decisão de negócio mora aqui. Lança exceções da feature.
- **`repository/`**: interface Spring Data. Consulta de leitura pode projetar direto no DTO com `SELECT new ...`. Um repositório só de leitura estende `Repository<T, ID>` (marcador) em vez de `JpaRepository`, para não expor `save`/`delete`.
- **`entity/`**: só mapeamento JPA (sufixo `Entity`). Sem lógica.
- **`dto/`**: `record`s. Entrada com Bean Validation; saída com método estático `from(entity)`.
- **`exception/`**: exceções da feature, herdando de uma das três bases em `common.exception`.

### Regras da casa

1. **Dentro de uma subfeature, a seta só anda para a direita.** Controller → Service → Repository → Entity.
2. **Entre subfeatures da mesma feature, só pelo service.** `CourseReviewService` chama `CourseManagementService`, nunca `CourseRepository`.
3. **Entre features, também só pelo service.**
4. **Entidades JPA podem referenciar entidades de qualquer feature.** É o mapeamento do banco.
5. **`shared/` de uma feature guarda o que duas ou mais subfeatures usam.**
6. **`common/` não conhece nenhuma feature.** Só recebe dependência.
7. **Só o service tem `@Transactional`.**
8. **Feature ou subfeature nova nasce com o template completo** (`controller`, `service`, `repository`, `dto`, `exception`, e `entity` quando não há `shared/`), mesmo vazias, com `package-info.java` descrevendo o que vai ali. Ninguém precisa decidir onde criar uma pasta: ela já existe.
9. **Feature vira subfeatures quando tem mais de um contexto de uso** (ex.: `course` tem vitrine pública, gestão do admin e avaliação). Nunca divida por camada (`course/controller/`, `course/service/`): isso é voltar para camadas com outro nome.

---

## Erros: quatro famílias

| Família | Quem gera | HTTP | `code` |
|---|---|---|---|
| Validação de entrada | Bean Validation no DTO, conversão de parâmetro, JSON malformado | 400 | `VALIDATION_ERROR`, `INVALID_PARAMETER`, `MISSING_PARAMETER`, `MALFORMED_REQUEST` |
| Regra de negócio | **só o service**, via exceção da feature | 404 / 409 / 422 | definido pela exceção (ex.: `USER_NOT_FOUND`, `EMAIL_ALREADY_USED`) |
| Protocolo HTTP | Spring MVC | 404 / 405 / 415 | `RESOURCE_NOT_FOUND`, `METHOD_NOT_ALLOWED`, `UNSUPPORTED_MEDIA_TYPE` |
| Infraestrutura | banco, JPA, inesperado | 409 / 500 | `DATA_CONFLICT`, `INTERNAL_ERROR` |

- Exceção de negócio herda de `NotFoundException` (404), `ConflictException` (409) ou `BusinessRuleException` (422), todas filhas de `BusinessException`, que carrega `code` e `message`. **A feature escolhe o tipo da falha; `common.api.GlobalExceptionHandler` escolhe o status.** Nenhuma classe fora de `common.api` importa `HttpStatus` para erro.
- **Nunca** use `IllegalArgumentException`, `IllegalStateException` ou `RuntimeException` crua para regra de negócio.
- Service verifica antes de gravar (`existsByEmail` antes do `save`). A constraint do banco é rede de segurança para condição de corrida; por isso `DataIntegrityViolationException` vira um 409 genérico.
- Service **não captura** exceção de infraestrutura. Deixa subir para o handler.
- Cliente nunca vê detalhe de infraestrutura: mensagem do 500 é fixa, stack trace vai só para o log (`ERROR`). Erro de negócio é logado em `INFO`, sem stack trace.
- Envelope de erro (`ApiError`): `status`, `error`, `code`, `message`, `path`, `timestamp`.

---

## Contratos de API

- Todas as rotas usam o prefixo `/api/v1`. Rotas públicas ficam sob `/api/v1/public/**`.
- Sucesso usa `ApiResponse` (`success`, `data`, `message`). **Exceção**: endpoints paginados devolvem `PageResponse` direto (`content`, `pageNumber`, `pageSize`, `totalElements`, `totalPages`), por contrato com o frontend.
- Entrada validada com `@Valid` e anotações no DTO. Nunca `if` manual no controller.
- CORS restrito às origens de `APP_CORS_ALLOWED_ORIGINS`, aplicado a `/api/**`.

---

## Testes

Espelham a árvore de produção, **no mesmo pacote da classe testada**, sufixo `Test`. Um tipo de teste por papel:

| Papel | Tipo | Base | O que cobre |
|---|---|---|---|
| Service | unitário com Mockito (`@ExtendWith(MockitoExtension.class)`) | nenhuma | regra: normalização, exceções, o que chega ao repositório |
| Repository | `@DataJpaTest` + Testcontainers | `common.JpaIntegrationTest` | consulta JPQL, projeção, ordenação, constraints, triggers |
| Controller | `@SpringBootTest` + MockMvc | `common.WebIntegrationTest` | contrato HTTP: rota, status, envelope, nomes dos campos, `code` de erro |

- O Postgres de teste é **um só**, em `common.PostgresTestContainer`. Nunca crie `@Container` por classe.
- `@DataJpaTest` roda dentro de transação desfeita ao final. Para ver efeito de trigger, faça `flush()` e `clear()` antes de reler.
- Testes de controller não são transacionais: use dados únicos por teste (UUID no email ou no título) em vez de limpar tabela.
- Todo service, repository com consulta própria e controller novo precisa do seu teste. DTO, entidade e configuração trivial não.
- A árvore de teste nasce junto com a feature: `controller/`, `service/` e `repository/` em `src/test`, cada uma com `package-info.java` dizendo o tipo de teste e a base a estender. `dto/`, `entity/` e `exception/` não têm pasta de teste; teste de entidade só aparece quando há comportamento de banco a validar (ex.: `course/shared/entity/LessonsCountTriggerTest`).

---

## Banco de dados

- Schema por Flyway, Hibernate em `ddl-auto=validate`: mudança em entidade que altere coluna, tipo, nullability ou constraint **exige migration nova**. Migration aplicada é imutável.
- Constraints (unique, FK, check, default) ficam no banco. Regra de decisão fica no service.
- Contadores desnormalizados em `courses`: `lessons_count` é mantido pelo trigger da V5 (testado em `course.shared.entity.LessonsCountTriggerTest`). **`reviews_count`, `rating_average`, `materials_count` e `students_count` ainda não têm mecanismo de atualização**; é pendência para uma US própria.

---

## Qualidade e fluxo

- Checkstyle (`checkstyle.xml`) roda na fase `validate` e quebra o build: chaves obrigatórias, variáveis locais `final`, sem número mágico (exceto 0–5, 10, 100, 1000), métodos até 40 linhas. Não se aplica a testes.
- `mvn -B clean verify` antes de abrir MR, com Docker rodando.
- Commits: `<tipo>(<id_clickup>): <descrição curta>`. Branches: `<tipo>/<id_clickup>-<descricao>` a partir de `develop`. MR sempre para `develop`.

---

## Template de subfeature nova

```text
course/review/
├── controller/CourseReviewController.java
├── service/CourseReviewService.java          # @Service, @Transactional
├── repository/CourseReviewRepository.java    # JpaRepository<CourseReviewEntity, UUID>
├── dto/CreateReviewRequest.java, ReviewResponse.java
└── exception/ReviewAlreadyExistsException.java  # extends ConflictException
```

Testes correspondentes em `src/test/java/.../course/review/{controller,service,repository}/`.

---

*Última atualização: Setembro/2026*
