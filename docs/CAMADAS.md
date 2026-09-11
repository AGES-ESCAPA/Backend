# Camadas do Escapa! Backend

Este documento explica cada papel que existe dentro de uma feature, sempre com os mesmos quatro pontos: objetivo, por que é necessária, por que foi implementada dessa forma e observações. Os exemplos são código real do projeto, das features `user` e `course/catalog`.

O fluxo de um pedido HTTP passa pelas camadas nesta ordem, e só nesta ordem:

```text
Requisição HTTP
   │
   ▼
Controller ──── valida formato (DTO de entrada), chama UM service, devolve DTO de saída
   │
   ▼
Service ─────── decide (regra de negócio), abre a transação, lança exceção da feature
   │
   ▼
Repository ──── consulta ou grava (Spring Data JPA), pode projetar direto em DTO
   │
   ▼
Entity ──────── espelho da tabela (JPA), sem lógica
```

Transversal a tudo isso fica `common`: envelopes de resposta, exceções base, handler global e configuração. `common` não conhece nenhuma feature.

---

## 1. Controller

Pasta: `<feature>/controller/`. Exemplos: `user/controller/UserController`, `course/catalog/controller/CourseCatalogController`.

### Objetivo da camada

Ser a porta de entrada HTTP da feature. Traduz requisição em chamada de método e resultado em resposta JSON. Só isso.

### Por que é necessária

Alguém precisa saber de rota, verbo HTTP, status code, `@RequestParam`, `@RequestBody` e envelope de resposta. Se esse conhecimento vazasse para o service, o service não poderia ser chamado por outra feature nem testado sem Spring MVC.

### Por que foi implementada dessa forma

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        final UserResponse response = UserResponse.from(userService.create(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "User created successfully"));
    }
}
```

- **Injeta só o service, nunca repository ou entity.** Se o controller pudesse consultar o banco, a regra de negócio acabaria espalhada entre controller e service e ninguém saberia onde procurar.
- **`@Valid` no DTO de entrada.** A validação de formato acontece antes do service ser chamado. Se falhar, o Spring lança `MethodArgumentNotValidException` e o handler global devolve 400. O controller não tem `if`.
- **Um método, uma chamada de service, um `return`.** Não há `try/catch`: erro sobe para o `GlobalExceptionHandler`. Não há conversão de entidade à mão: `UserResponse.from(...)` faz isso.
- **Status de sucesso é a única decisão HTTP que o controller toma** (`201` para criação, `200` para o resto). Status de erro é sempre do handler.
- **Rota sob `/api/v1`**, e `/api/v1/public/**` para o que não exige autenticação.

### Observações

- Endpoints paginados devolvem `PageResponse` direto, sem `ApiResponse`, por contrato com o frontend definido na US-01. É a única exceção ao envelope.
- Teste: `@SpringBootTest` + MockMvc, estendendo `common.WebIntegrationTest`. Cobre rota, status, envelope, nomes dos campos e `code` de erro. Não testa regra de negócio: isso é do service.
- Quando autenticação entrar, o controller continua igual. Quem sabe de token é um filtro em `common/config`, não o controller.

---

## 2. Service

Pasta: `<feature>/service/`. Exemplos: `user/service/UserService`, `course/catalog/service/CourseCatalogService`.

### Objetivo da camada

Concentrar toda decisão de negócio da feature. É a única camada que responde "pode ou não pode" e "o que acontece depois".

### Por que é necessária

Regra de negócio precisa de um lugar único, testável sem HTTP e sem banco. Se ficasse no controller, não seria reutilizável por outra feature. Se ficasse no repository, dependeria de SQL para ser testada. O service é o meio-termo: Java puro que orquestra repositórios.

### Por que foi implementada dessa forma

```java
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserEntity create(CreateUserRequest request) {
        final String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException(email);
        }
        final UserEntity user = new UserEntity(UUID.randomUUID(), request.name().trim(), email,
                passwordEncoder.encode(request.password()),
                request.userType().trim().toUpperCase(Locale.ROOT), LocalDateTime.now());
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserEntity getById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }
}
```

- **`@Service`** em vez de fiação manual de beans. O Spring injeta tudo pelo construtor. A versão anterior exigia um método `@Bean` por caso de uso numa classe de configuração, e isso impedia `@Transactional` no lugar certo.
- **`@Transactional` só aqui.** A transação abre no início do método do service e fecha no fim. Tudo que o método faz é atômico. `readOnly = true` em leitura deixa o Hibernate pular verificações de alteração e o Postgres otimizar.
- **Verifica antes de gravar.** `existsByEmail` antes de `save`. A constraint `UNIQUE` do banco continua lá como rede de segurança para condição de corrida, mas a regra principal é explícita no Java, com exceção nomeada e mensagem clara.
- **Lança exceção da feature, nunca `IllegalArgumentException`.** `EmailAlreadyUsedException` herda de `ConflictException`, então o handler sabe que é 409 sem o service saber o que é HTTP.
- **Recebe o DTO de entrada e devolve entidade.** O controller converte a entidade em DTO de saída. Assim o service pode ser chamado por outro service que precise da entidade completa.
- **Normalização mora aqui.** Trim, minúsculas, valores padrão de paginação. O `CourseCatalogService` faz só isso, porque a vitrine não tem regra que falhe: filtro sem resultado é página vazia com 200, não erro.

### Observações

- Service de uma feature chama service de outra, nunca repository de outra. Se `EnrollmentService` precisar de um curso, chama `CourseCatalogService`.
- Não captura `DataAccessException`. Falha de banco sobe para o handler, que devolve 500 genérico e registra o stack trace. Capturar aqui só esconderia a causa.
- Teste: unitário com Mockito, sem Spring. Repositório e encoder são mocks; o teste confere a regra e exatamente o que chega ao repositório. Roda em milissegundos, sem Docker.

---

## 3. Repository

Pasta: `<feature>/repository/`. Exemplos: `user/repository/UserRepository`, `course/catalog/repository/CourseCatalogRepository`, `course/management/repository/CourseRepository`.

### Objetivo da camada

Ser a única forma de ler e gravar no banco. Esconde SQL, JPQL e detalhes do Hibernate do resto da feature.

### Por que é necessária

Consulta ao banco é o ponto mais provável de bug sutil (N+1, ordenação instável, parâmetro nulo) e de otimização. Concentrar isso numa interface deixa o service legível e permite testar a consulta contra o Postgres real, isolada da regra.

### Por que foi implementada dessa forma

```java
public interface CourseCatalogRepository extends Repository<CourseEntity, UUID> {

    default Page<CourseCardResponse> findPublished(
            String titlePattern, String category, String level, Pageable pageable) {
        return findByStatus(CourseStatus.PUBLISHED, titlePattern, category, level, pageable);
    }

    @Query(value = """
            SELECT new com.escapa.backend.course.catalog.dto.CourseCardResponse(
                c.id, c.title, c.shortDescription, c.category, c.level, c.durationTime,
                c.lessonsCount, c.price, c.thumbnailUrl, i.name, c.ratingAverage, c.reviewsCount)
            FROM CourseEntity c
            LEFT JOIN c.instructor i
            WHERE c.status = :status
              AND LOWER(c.title) LIKE :titlePattern ESCAPE '!'
              AND (:category = '' OR LOWER(c.category) = :category)
              AND (:level = '' OR LOWER(c.level) = :level)
            ORDER BY c.createdAt DESC, c.id ASC
            """, countQuery = "...")
    Page<CourseCardResponse> findByStatus(...);
}
```

- **Interface Spring Data, sem implementação.** O Spring gera o código. Menos classe para manter, sem `EntityManager` espalhado.
- **`SELECT new ...CourseCardResponse(...)`: projeção direta no DTO.** A consulta já devolve o objeto que o controller vai serializar. Não carrega entidade, não precisa de mapper, não há três cópias do mesmo objeto. Foi isso que eliminou `CourseSummary` e `CourseMapper` da versão anterior.
- **`Repository<T, ID>` (marcador) em vez de `JpaRepository` para leitura.** A vitrine nunca grava. Se estendesse `JpaRepository`, `save` e `delete` estariam disponíveis para qualquer um que injetasse o repositório. Quem grava curso é `course/management/repository/CourseRepository`, que estende `JpaRepository`. Dois repositórios sobre a mesma entidade, cada um com o que sua subfeature precisa.
- **`ORDER BY c.createdAt DESC, c.id ASC` fixo.** Sem ordenação, o Postgres não garante ordem estável entre páginas e a vitrine podia repetir ou pular cursos. O desempate por `id` cobre dois cursos criados no mesmo instante.
- **`LOWER(...)` dos dois lados e `ESCAPE '!'`.** Busca sem diferenciar caixa e `%`, `_` do usuário tratados como texto. O service escapa antes de chamar.
- **Sentinelas `''` e `'%'` em vez de `IS NULL`.** Parâmetro nulo em JPQL vira `bytea` no Postgres e a consulta quebra. O service garante que nunca chega nulo.
- **`countQuery` separada, sem join.** A contagem para `totalElements` não precisa do instrutor.

### Observações

- `UserRepository` estende `JpaRepository` porque a feature grava. Só declara `existsByEmail`; o resto vem herdado.
- Repositório não tem `@Transactional`. A transação é do service. Se o repository abrisse a própria transação, uma operação com dois `save` não seria atômica.
- Teste: `@DataJpaTest` estendendo `common.JpaIntegrationTest`, contra o Postgres do Testcontainers com as migrations do Flyway aplicadas. Roda dentro de transação desfeita ao final. Para ver efeito de trigger, `flush()` e `clear()` antes de reler.

---

## 4. Entity

Pasta: `<feature>/entity/`, ou `<feature>/shared/entity/` quando a feature tem subfeatures. Exemplos: `user/entity/UserEntity`, `course/shared/entity/CourseEntity`.

### Objetivo da camada

Ser o espelho Java de uma tabela. Diz ao Hibernate qual classe corresponde a qual tabela, qual campo a qual coluna, e como as tabelas se relacionam.

### Por que é necessária

O Hibernate precisa de um mapeamento para montar SQL, e o `ddl-auto=validate` usa esse mapeamento para conferir na subida que o schema do Flyway e o código concordam. Sem entidade não há JPA.

### Por que foi implementada dessa forma

```java
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
public class UserEntity {

    @Id
    @Column(nullable = false, unique = true)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;
}
```

- **Sufixo `Entity`.** Deixa claro no import que é classe de persistência, não DTO nem regra.
- **Só mapeamento, sem lógica.** Regra fica no service. Se a entidade tivesse método `publicar()`, a regra ficaria escondida em uma classe que o service manipula por setter.
- **Anotações do banco espelham a migration** (`nullable`, `unique`, nome da coluna). Não criam schema, só validam. Se divergirem, a aplicação não sobe, o que é bom: erro na subida é mais barato que erro em produção.
- **Sem entidade de domínio separada.** A versão anterior tinha `User` (domínio) e `UserEntity` (JPA), com mapper entre eles e sem nenhuma regra em `User`. Duas classes para o mesmo dado, divergindo entre si. Agora a entidade JPA é o modelo.
- **Entidade pode referenciar entidade de outra feature.** `UserCourseEntity` (enrollment) aponta para `UserEntity` (user) e `CourseEntity` (course). É o mapeamento do banco; esconder isso criaria mais problema do que resolve.
- **Enums de coluna ficam junto** (`UserStatus`, `CourseStatus`, `ContentType`) e são gravados como `STRING`, batendo com os `CHECK` das migrations.

### Observações

- Entidades de curso ficam em `course/shared/entity` porque `catalog`, `management` e `review` usam as mesmas tabelas.
- `CourseEntity` implementa `Persistable` com `isNew()` baseado em `id == null`, porque o id é gerado pelo Hibernate. `UserEntity` tem id atribuído pelo service; o Spring Data então usa `merge`, que funciona igual para inserção.
- Entidade não tem teste próprio. O que se testa é comportamento do banco ligado a ela: o trigger de `lessons_count` está em `course/shared/entity/LessonsCountTriggerTest`.
- Mudou coluna ou constraint na entidade: precisa de migration nova. Nunca editar migration já aplicada.

---

## 5. DTO

Pasta: `<feature>/dto/`. Exemplos: `user/dto/CreateUserRequest`, `user/dto/UserResponse`, `course/catalog/dto/CourseCardResponse`.

### Objetivo da camada

Definir o contrato JSON da API: o que entra e o que sai. É o que o frontend vê.

### Por que é necessária

O JSON da API e a tabela do banco mudam por motivos diferentes. `UserResponse` não tem `passwordHash`; `CourseCardResponse` tem `instructor` como nome, não como objeto. Se o controller devolvesse a entidade direto, qualquer coluna nova viraria campo público e o hash da senha sairia no JSON.

### Por que foi implementada dessa forma

```java
public record CreateUserRequest(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Email is required") @Email(message = "Invalid email") String email,
        @NotBlank(message = "Password is required")
        @Size(min = MIN_PASSWORD_LENGTH, message = "Password must be at least 8 characters")
        String password,
        @NotBlank(message = "User type is required") String userType
) {
    public static final int MIN_PASSWORD_LENGTH = 8;
}

public record UserResponse(UUID id, String name, String email, String userType, LocalDateTime createdAt) {
    public static UserResponse from(UserEntity user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
```

- **`record`.** Imutável, sem getter e setter à mão, `equals` e `toString` de graça. Um DTO não tem motivo para mudar depois de criado.
- **Validação de formato na entrada, via Bean Validation.** `@NotBlank`, `@Email`, `@Size` com mensagem. O Spring valida antes do controller e o handler devolve 400 com todas as mensagens. O service não repete essas checagens.
- **`from(entity)` na saída.** A conversão fica no DTO, ao lado dos campos que ela preenche. O controller só chama `UserResponse.from(...)`. Se um campo novo entrar no JSON, muda um arquivo.
- **Nomes do JSON são contrato.** `userType` no JSON, embora a coluna seja `role`, porque o frontend já consome assim. `instructor` como string porque a US pediu assim.
- **`CourseCardResponse` é preenchido pela consulta**, via `SELECT new`. Por isso a ordem e os tipos dos componentes precisam bater com a projeção. É um DTO de saída que nunca passa por `from`.

### Observações

- DTO de entrada não vira entidade sozinho. O service monta a entidade a partir do DTO, porque é o service que gera id, hash e normaliza.
- DTO não tem teste próprio. O teste de controller confere os nomes dos campos do JSON; o de repository confere a projeção.
- `CourseCardResponse` mora em `catalog/dto`, não em `shared`, porque só a vitrine o usa. Se `management` precisar de outra visão de curso, cria o próprio DTO.

---

## 6. Exception

Pasta: `<feature>/exception/`. Exemplos: `user/exception/UserNotFoundException`, `user/exception/EmailAlreadyUsedException`.

### Objetivo da camada

Dar nome e código estável a cada forma de o negócio dizer "não". A feature declara o que pode dar errado; o `common` decide como isso vira HTTP.

### Por que é necessária

Sem exceção nomeada, o service teria que devolver `null`, `Optional` ou `boolean` e o controller teria que interpretar cada caso com `if`. Com exceção nomeada, o service lança, o handler traduz, e nem controller nem service sabem de status HTTP.

### Por que foi implementada dessa forma

```java
// common/exception
public abstract class BusinessException extends RuntimeException {
    private final String code;
    protected BusinessException(String code, String message) { super(message); this.code = code; }
}
public class NotFoundException extends BusinessException { ... }      // → 404
public class ConflictException extends BusinessException { ... }      // → 409
public class BusinessRuleException extends BusinessException { ... }  // → 422

// user/exception
public class EmailAlreadyUsedException extends ConflictException {
    public static final String CODE = "EMAIL_ALREADY_USED";
    public EmailAlreadyUsedException(String email) { super(CODE, "Email already in use: " + email); }
}
```

- **Três bases em `common`, uma por tipo de falha.** Não existe (404), colide (409), regra proíbe (422). São as únicas três respostas que uma regra de negócio dá. O handler tem três `@ExceptionHandler` e nunca precisa de um quarto.
- **A feature herda da base certa.** Ao escrever `extends ConflictException`, o autor decide o tipo da falha sem tocar em HTTP. O status é consequência.
- **`code` estável, em maiúsculas.** É o que o frontend usa para decidir o que mostrar. Mensagem é para humano e pode mudar; código não.
- **Unchecked** (`RuntimeException`). Sem `throws` na assinatura, sem `try/catch` obrigatório. A exceção atravessa service e controller até o handler.
- **`IllegalArgumentException` proibida para negócio.** Ela é lançada pelo JDK e por bibliotecas por bug interno. A versão anterior mapeava para 400, então um bug aparecia como erro do cliente. Agora `IllegalArgumentException` cai no handler genérico e vira 500, que é o correto para bug.

### Observações

- 400 não é regra de negócio. 400 é formato errado (Bean Validation, `page=abc`, JSON quebrado) e vem de exceções do próprio Spring. Regra de negócio começa em 404.
- Erro de infraestrutura (banco caiu, constraint violada em condição de corrida, `NullPointerException`) não tem classe na feature. Sobe cru até o handler, que devolve 409 ou 500 com mensagem fixa e registra o detalhe só no log.
- `course/catalog/exception` está vazia de propósito: a vitrine não tem regra que falhe.
- Exceção não tem teste próprio. O teste de service confere que ela é lançada; o de controller confere o status e o `code`.

---

## 7. common (transversal)

Pastas: `common/api`, `common/exception`, `common/config`.

### Objetivo da camada

Guardar o que toda feature usa e nenhuma feature possui: envelopes de resposta, exceções base, tradução de erro em HTTP, configuração do Spring, health check.

### Por que é necessária

Se cada feature tivesse o próprio envelope e o próprio handler, a API teria formatos diferentes por rota. `common` garante que `ApiResponse`, `ApiError` e os status são iguais em todo lugar.

### Por que foi implementada dessa forma

- **`common/api/GlobalExceptionHandler`** é o único lugar que importa `HttpStatus` para erro. Quatro famílias, uma seção por família, um método privado por família por causa do limite de 40 linhas do Checkstyle. Detalhe de infraestrutura vai para o log, nunca para o cliente.
- **`common/api/ApiError`** tem `code` além de `message`, para o frontend não precisar comparar texto.
- **`common/api/PageResponse.from(Page)`** converte a página do Spring Data no envelope da API em um lugar só.
- **`common/exception`** tem só as três bases mais a abstrata. Exceção concreta nunca fica aqui.
- **`common/config/SecurityConfig`** expõe o `PasswordEncoder`. Quando autenticação entrar, a cadeia de filtros e a whitelist de `/api/v1/public/**` vêm para cá.
- **`HealthController` em `common/api`** porque health check não é feature: não tem service, regra nem dado.

### Observações

- `common` nunca importa nada de feature. Se importasse, uma mudança em `user` quebraria `course`.
- Teste: `GlobalExceptionHandlerTest` com MockMvc standalone, sem contexto Spring, um controller de mentira lançando cada família. `HealthControllerTest` com contexto completo, que também prova que a aplicação sobe.
- As bases de teste (`PostgresTestContainer`, `JpaIntegrationTest`, `WebIntegrationTest`) ficam em `src/test/.../common`. É o único lugar onde a árvore de teste tem algo que a de produção não tem.

---

## Resumo em uma tabela

| Camada | Sabe de | Não sabe de | Testa com |
|---|---|---|---|
| Controller | HTTP, rota, DTO, um service | repository, entity, regra, status de erro | MockMvc + contexto |
| Service | regra, transação, repository, exceção da feature | HTTP, `HttpStatus`, JSON | Mockito, sem Spring |
| Repository | JPQL, entity, projeção em DTO | regra, HTTP, transação | `@DataJpaTest` + Postgres real |
| Entity | tabela, coluna, relacionamento | regra, JSON, HTTP | só quando há trigger ou constraint |
| DTO | JSON, validação de formato, `from(entity)` | banco, regra | via controller e repository |
| Exception | tipo da falha (404/409/422), `code` | `HttpStatus` | via service e controller |
| common | envelopes, handler, config | qualquer feature | standalone + contexto |

*Última atualização: Setembro/2026*
