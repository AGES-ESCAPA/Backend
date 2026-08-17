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

- Build: `Dockerfile` multi-stage com Java 17
- Orquestração local: `docker-compose.yml`
- Banco: PostgreSQL em container
- CI/CD: GitLab CI e passos de validação de build/test

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
│   │   │   └── mapper/
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

#### 1. Entidade de domínio

```java
public class User {
    private final String id;
    private final String name;
    private final String email;
    private final String role;

    public User(String id, String name, String email, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
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

    public User execute(String name, String email, String role) {
        User user = User.create(name, email, role);
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
        User user = createUserUseCase.execute(request.name(), request.email(), request.role());
        UserResponse response = new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
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
        assertEquals("STUDENT", user.getRole());
    }
}
```

---

### 🧪 Regras de Validação e Qualidade

- Todo caso de uso deve ter teste unitário correspondente.
- Todo endpoint novo deve ter teste de integração ou teste de controller quando aplicável.
- Validação de entrada deve ocorrer no DTO/controller via Bean Validation.
- Erros de domínio devem ser transformados em respostas padronizadas da API (ver `GlobalExceptionHandler`: 400 para validação/regra de domínio, 404 para recurso não encontrado, 409 para conflito de dados, 500 para erro inesperado).
- O Checkstyle (`checkstyle.xml`) roda na fase `validate` do Maven e quebra o build em caso de violação — rode `mvn checkstyle:check` antes de abrir MR.
- O projeto deve continuar funcionando em Maven e em Docker Compose (o `Dockerfile` precisa copiar `checkstyle.xml`, não só `pom.xml`, para o build multi-stage não quebrar).

---

### Commits & Branches

- Formato de Commit: `<tipo>(<id_clickup>): <descrição curta>` (ex: `feat(86a1b2c): add user creation flow`)
- Branches: criadas a partir de **`develop`** no formato `<tipo>/<id_clickup>-<breve-descricao>`
- Merge Requests sempre apontando para a branch **`develop`**.

---

*Última atualização: Agosto/2026*
