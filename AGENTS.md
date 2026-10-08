# AGENTS.md — Escapa! Backend

> Este arquivo é lido por agentes de IA (Copilot, Cursor, Gemini, Claude, etc.) para entender o contexto do projeto e seguir as convenções do time. **Leia-o completamente antes de sugerir ou gerar código.**

## Contexto do Projeto

**Escapa!** é uma plataforma digital de **cursos e qualificação profissional em Turismo e Hospitalidade**.

- **Backend principal**: Java com Spring Boot.
- **Arquitetura alvo**: Clean Architecture, com separação entre domínio, casos de uso, adaptadores e infraestrutura.
- **Persistência**: Spring Data JPA + PostgreSQL.
- **Execução local**: via Maven, Docker e Docker Compose.
- **Objetivo inicial**: bootstrap da aplicação com estrutura pronta para crescimento, sem acoplamento entre regras de negócio e frameworks.

**Escopo inicial do backend**:
- cadastro e consulta de usuários
- endpoints base para health check e operação inicial
- estrutura que suporte futuramente cursos, módulos, aulas, progresso, certificação e administração

**🚫 Fora do escopo inicial**:
- autenticação e autorização avançada
- integrações com pagamentos
- integrações com IA
- lógica de streaming em tempo real
- multi-tenancy ou multilíngue

## Infraestrutura

A aplicação será executada em ambiente containerizado e deve manter boas práticas de desenvolvimento e deploy:

- Build: `Dockerfile` multi-stage com Java 21, executado por um usuário não-root
- Orquestração local: `docker-compose.yml` (o backend só sobe após o healthcheck do banco)
- Banco: PostgreSQL em container, com schema versionado por migrations Flyway
- Testes: JUnit 5 + Testcontainers — exigem Docker em execução
- CI/CD: GitHub Actions (`.github/workflows/ci.yml`), com build, Checkstyle, testes, cobertura Jacoco e build da imagem Docker

## Arquitetura de Pastas e Responsabilidades

```text
src/
├── main/
│   ├── java/com/escapa/backend/
│   │   ├── domain/                  → entidades e regras puras do núcleo
│   │   │   └── user/
│   │   ├── application/             → casos de uso e portas de saída/entrada
│   │   │   ├── port/
│   │   │   └── usecase/
│   │   ├── adapters/                → controllers, DTOs, handlers e adaptadores web
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── exception/
│   │   │   └── security/        → leitura do header X-User-Id e guarda de acesso ADMIN
│   │   ├── infrastructure/          → JPA, repositórios, configurações e integração externa
│   │   │   ├── config/
│   │   │   └── persistence/
│   │   └── EscapaBackendApplication.java
│   └── resources/
│       └── application.properties
└── test/
    └── java/
        └── com/escapa/backend/
```

### 📌 Diretrizes de Arquitetura

1. **Domain (`domain/`)**: entidades puras, sem Spring, sem JPA, sem anotações de framework. Exemplo: `User`.
2. **Application (`application/`)**: casos de uso e interfaces/portas do sistema; depende apenas do `domain`. Exemplo: `CreateUserUseCase`, `UserRepositoryPort`.
3. **Adapters (`adapters/`)**: controllers REST, DTOs, tratadores de erro e adaptadores externos; sem lógica de negócio. Exemplo: `UserController`, `GlobalExceptionHandler`.
4. **Infrastructure (`infrastructure/`)**: persistência, banco, configurações e integração com bibliotecas; implementa as portas definidas em `application`. Exemplo: `UserJpaRepository`, `UserRepositoryAdapter`.
5. **Regra da Dependência**: todas as dependências devem apontar para o centro, nunca o contrário — `adapters` e `infrastructure` dependem de `application`, que depende só de `domain`, e o `domain` não depende de nada.

```text
adapters ──────┐
               ├──> application ──> domain
infrastructure ┘
```

---

## Regras para Agentes de IA

### ⚠️ Regras Invioláveis

1. **Nunca acople a camada de domínio a Spring, JPA ou qualquer framework.**
2. **Nunca coloque lógica de banco dentro da camada de domínio.**
3. **Nunca use `any` em TypeScript ou Java sem necessidade**. Em Java, prefira tipos explícitos e classes bem definidas.
4. **Nunca misture DTO, entidade de domínio e entidade JPA na mesma camada.**
5. **Nunca esconda regras de negócio dentro do controller.**
6. **Use nomes de pacotes consistentes**: `domain`, `application`, `adapters`, `infrastructure`.
7. **Use `record` para DTOs quando fizer sentido**, mantendo clareza e simplicidade.
8. **Mantenha convenções de nomenclatura**: `UserController`, `CreateUserUseCase`, `UserRepositoryPort`, `UserEntity`.

---

### ✅ Padrões Obrigatórios de Código

> Os exemplos abaixo são simplificados: consulte `User`, `CreateUserUseCase` e `UserController` para a versão real.

#### 1. Entidade de domínio

```java
public class User {
    private final String id;
    private final String name;
    private final String email;
    private final String userType;

    public User(String id, String name, String email, String userType) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.userType = userType;
    }
}
```

#### 2. Caso de uso

```java
public class CreateUserUseCase {
    private final UserRepositoryPort userRepositoryPort;

    public CreateUserUseCase(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    public User execute(String name, String email, String userType) {
        User user = new User(name, email, userType);
        if (userRepositoryPort.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("User already exists");
        }
        return userRepositoryPort.save(user);
    }
}
```

#### 3. Controller REST

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final CreateUserUseCase createUserUseCase;

    public UserController(CreateUserUseCase createUserUseCase) {
        this.createUserUseCase = createUserUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        User user = createUserUseCase.execute(request.name(), request.email(), request.password(), request.userType());
        UserResponse response = UserResponse.from(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "User created successfully"));
    }
}
```

> Todas as rotas da API são prefixadas com `/api/v1` e respostas de sucesso usam o envelope `ApiResponse`. O CORS libera apenas as origens definidas em `APP_CORS_ALLOWED_ORIGINS`.

#### 4. Testes

Cada caso de uso tem seu teste, usando um fake de `UserRepositoryPort` em memória compartilhado entre os testes do pacote (`InMemoryUserRepositoryPort`, package-private em `src/test/.../application/usecase/`) em vez de mocks:

```java
class CreateUserUseCaseTest {

    @Test
    void shouldCreateUserWithNormalizedData() {
        UserRepositoryPort repository = new InMemoryUserRepositoryPort();
        CreateUserUseCase useCase = new CreateUserUseCase(repository);

        User user = useCase.execute(" maria ", " maria@email.com ", "student");

        assertNotNull(user.getId());
        assertEquals("maria", user.getName());
        assertEquals("maria@email.com", user.getEmail());
        assertEquals("STUDENT", user.getUserType());
    }
}
```

---

### 🧪 Regras de Validação e Qualidade

- Todo caso de uso deve ter teste unitário correspondente.
- Todo endpoint novo deve ter teste de integração ou teste de controller quando aplicável.
- Validação de entrada deve ocorrer no DTO/controller via Bean Validation.
- Erros de domínio devem ser transformados em respostas padronizadas da API (ver `GlobalExceptionHandler`: 400 para validação/regra de domínio e parâmetro ausente, 401/403 para acesso negado, 404 para recurso não encontrado, 409 para conflito de dados, 422 para publicação de curso incompleto, 500 para erro inesperado).
- O Checkstyle (`checkstyle.xml`) roda na fase `validate` do Maven e quebra o build em caso de violação — rode `mvn checkstyle:check` antes de abrir MR.
- O projeto deve continuar funcionando em Maven e em Docker Compose (o `Dockerfile` precisa copiar `checkstyle.xml`, não só `pom.xml`, para o build multi-stage não quebrar).

---

### 🧩 Convenções de Implementação

- **O nome do caso de uso define a transação.** `UseCaseTransactionConfig` envolve o `execute` de toda classe `*UseCase`: os prefixos `Get`, `List`, `Search` e `Authorize` rodam em transação **somente leitura**; os demais, em transação de escrita. Um caso de uso que grave dados **não** pode usar esses prefixos (no PostgreSQL falha em tempo de execução).
- **Sem Spring em `application/`**: nada de `@Service` ou `@Transactional`. Casos de uso são classes puras registradas como `@Bean` em `SpringConfig` / `CourseConfig`.
- **Acesso às rotas `/admin`**: receba `@RequestHeader(value = UserIdHeader.NAME, required = false) String xUserId` e chame `AdminRequestGuard.requireAdmin(xUserId)` (403). Rotas do aluno usam `UserIdHeader.requireStudentId(xUserId)` (401). Nunca reimplemente o parse do header nem compare `"ADMIN"` como texto: use `User.isAdmin()` / `UserTypes`. O header é provisório até o login (US-23).
- **Controllers só roteiam**: a conversão entidade/modelo → resposta fica em `XxxResponse.from(...)` e páginas usam `PageResponse.of(...)`. Regras (ex.: limites de paginação) ficam no caso de uso.
- **DTOs de entrada**: Bean Validation em todos os campos; textos gravados em `VARCHAR(255)` usam `@Size(max = FieldLimits.VARCHAR_MAX, ...)` e números não negativos usam `@PositiveOrZero`. Um único DTO atende criação e edição quando os campos coincidem (`ContentRequest`, `ModuleRequest`).
- **Contratos existentes não mudam sem combinar com o frontend**: alguns endpoints respondem sem `ApiResponse` (lista e filtros públicos, regras do curso, change-log, busca de pré-requisitos). Não "padronize" o envelope por conta própria.
- **Consultas**: não carregue o agregado inteiro (ex.: `CourseEntity` com módulos e aulas) só para listar; use projeção (ex.: `findAllExcludingStatus`) ou `@EntityGraph`. Mappers que percorrem coleções lazy geram N+1.
- **Checkstyle**: método com no máximo **40 linhas** e variáveis locais `final`; a build quebra se violar.
- **OpenAPI**: todo controller novo tem `@Tag` e cada endpoint `@Operation(summary = ...)`. As respostas de erro padrão (400/401/403/404/500) são anexadas automaticamente pelo `OpenApiConfig`.
- **Testes**: reutilize os fakes `InMemory*Port` (públicos, em `src/test/.../application/usecase/`), sem Mockito. Todo controller novo ganha teste com `MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler())` cobrindo acesso (403/401), validação (400) e o formato da resposta. Testes de persistência estendem `PostgresIntegrationTest`.

---

### Commits & Branches

- Formato de Commit: `<tipo>(<id_clickup>): <descrição curta>` (ex: `feat(86a1b2c): add user creation flow`)
- Branches: criadas a partir de **`develop`** no formato `<tipo>/<id_clickup>-<breve-descricao>`
- Merge Requests sempre apontando para a branch **`develop`**.

---

*Última atualização: Agosto/2026*
