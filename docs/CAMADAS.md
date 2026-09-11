# Camadas do Escapa! Backend: como se conversam

Este é o arquivo geral. Cada camada tem o seu próprio documento, com objetivo, por que é necessária, por que foi implementada dessa forma e observações:

| Camada | Documento | Em uma frase |
|---|---|---|
| Controller | [camadas/controller.md](camadas/controller.md) | porta de entrada HTTP; recebe, valida formato, chama um service, devolve DTO |
| Service | [camadas/service.md](camadas/service.md) | toda decisão de negócio; única camada com `@Transactional`; lança exceção da feature |
| Repository | [camadas/repository.md](camadas/repository.md) | única forma de ler e gravar no banco; pode projetar direto em DTO |
| Entity | [camadas/entity.md](camadas/entity.md) | espelho JPA da tabela; sem lógica |
| DTO | [camadas/dto.md](camadas/dto.md) | contrato JSON da API; entrada validada, saída via `from(entity)` |
| Exception | [camadas/exception.md](camadas/exception.md) | nome e `code` para cada "não" do negócio; herda de uma base em `common` |
| common | [camadas/common.md](camadas/common.md) | transversal: envelopes, handler global, exceções base, configuração |

---

## O desenho

```text
                         ┌──────────────────────────── common ─────────────────────────────┐
                         │  api/        ApiResponse  ApiError  PageResponse  GlobalExceptionHandler │
                         │  exception/  NotFoundException  ConflictException  BusinessRuleException │
                         │  config/     CorsConfig  SecurityConfig(PasswordEncoder)  OpenApiConfig  │
                         └────────────────────────────────▲─────────────────────────────────┘
                                                          │ usa (nunca o contrário)
   ┌──────────────────────────────── feature ─────────────┼──────────────────────────────────┐
   │                                                      │                                  │
   │   HTTP ──► controller/ ──► service/ ──► repository/ ──► entity/ ──► banco               │
   │              │  ▲            │   │          │                                            │
   │              │  │            │   │          └── SELECT new ──► dto/ (saída projetada)    │
   │              │  │            │   └── lança ──► exception/ ──► sobe até o handler         │
   │              │  └── from(entity) ──► dto/ (saída)                                        │
   │              └── @Valid ──► dto/ (entrada)                                               │
   │                                                                                          │
   └──────────────────────────────────────────────────────────────────────────────────────────┘
```

A seta principal só anda para a direita: controller chama service, service chama repository, repository devolve entity. Nada volta a chamar quem está à esquerda. DTO e exception são as duas coisas que atravessam camadas: DTO como dado, exception como interrupção.

---

## Um pedido de sucesso: `POST /api/v1/users`

```text
1. HTTP        POST /api/v1/users  {"name":" Maria ","email":"MARIA@email.com","password":"senha12345","userType":"student"}
                  │
2. common      CorsConfig libera a origem. DispatcherServlet acha UserController.create.
                  │
3. dto         Jackson monta CreateUserRequest. Bean Validation roda (@NotBlank, @Email, @Size).
   (entrada)   Tudo válido: segue. (Se não, para aqui: ver "pedido com erro" abaixo.)
                  │
4. controller  UserController.create(request)
               → chama userService.create(request)              ← única linha de lógica
                  │
5. service     UserService.create(request)          @Transactional abre aqui
               → normaliza: "maria@email.com", "Maria", "STUDENT"
               → userRepository.existsByEmail("maria@email.com")  → false
               → passwordEncoder.encode("senha12345")            ← bean de common/config
               → monta UserEntity(id novo, ..., hash, ..., agora)
               → userRepository.save(entity)
                  │
6. repository  UserRepository.save  (herdado de JpaRepository)
               → Hibernate: INSERT INTO users (...)
                  │
7. entity      UserEntity é o que foi gravado e o que volta.  @Transactional fecha: COMMIT
                  │
8. controller  recebe UserEntity
               → UserResponse.from(entity)                        ← dto de saída, sem passwordHash
               → ResponseEntity.status(201).body(ApiResponse.success(response, "User created successfully"))
                  │
9. HTTP        201  {"success":true,"data":{"id":"...","name":"Maria","email":"maria@email.com","userType":"STUDENT","createdAt":"..."},"message":"User created successfully"}
```

Repare no que cada camada **não** fez: o controller não normalizou nem checou email. O service não montou JSON nem escolheu status. O repository não decidiu nada. A entidade não sabe que existe HTTP.

---

## O mesmo pedido com erro: email já usado

```text
1..4  iguais ao caso de sucesso
                  │
5. service     userRepository.existsByEmail("maria@email.com")  → true
               → throw new EmailAlreadyUsedException("maria@email.com")
                    (extends ConflictException, code EMAIL_ALREADY_USED)
               @Transactional fecha: ROLLBACK (nada foi gravado)
                  │
6. controller  a exceção atravessa UserController.create sem try/catch
                  │
7. common      GlobalExceptionHandler.handleConflict(ConflictException)
               → LOG.info("Business rule rejected POST /api/v1/users: EMAIL_ALREADY_USED (...)")
               → ApiError(409, "Conflict", "EMAIL_ALREADY_USED", "Email already in use: maria@email.com", "/api/v1/users", agora)
                  │
8. HTTP        409  {"status":409,"error":"Conflict","code":"EMAIL_ALREADY_USED","message":"...","path":"/api/v1/users","timestamp":"..."}
```

O service escolheu **o tipo** da falha ao herdar de `ConflictException`. O handler escolheu **o status**. Nenhum dos dois conhece o outro.

Os outros caminhos de erro do mesmo endpoint:

| Falha | Onde para | Quem trata | Resposta |
|---|---|---|---|
| `"email":"nao-e-email"` | passo 3, Bean Validation | handler, família validação | 400 `VALIDATION_ERROR` |
| JSON quebrado | passo 3, Jackson | handler, família validação | 400 `MALFORMED_REQUEST` |
| email duplicado | passo 5, service | handler, família negócio | 409 `EMAIL_ALREADY_USED` |
| dois pedidos simultâneos com o mesmo email | passo 6, constraint `UNIQUE` | handler, família infraestrutura | 409 `DATA_CONFLICT` (mensagem genérica) |
| banco fora do ar | passo 6 | handler, família infraestrutura | 500 `INTERNAL_ERROR` (detalhe só no log) |

---

## Um pedido de leitura: `GET /api/v1/public/courses?level=Iniciante&page=0&size=10`

```text
1. HTTP        GET /api/v1/public/courses?level=Iniciante      (sem token: rota pública)
                  │
2. controller  CourseCatalogController.list(title=null, category=null, level="Iniciante", page=0, size=10)
               → catalogService.listPublished(...)
                  │
3. service     CourseCatalogService.listPublished     @Transactional(readOnly = true)
               → normaliza: title → "%", category → "", level → "iniciante", PageRequest.of(0, 10)
               → catalogRepository.findPublished("%", "", "iniciante", pageable)
                  │
4. repository  CourseCatalogRepository.findByStatus(PUBLISHED, "%", "", "iniciante", pageable)
               → JPQL: SELECT new CourseCardResponse(c.id, c.title, ..., i.name, ...)
                       FROM CourseEntity c LEFT JOIN c.instructor i
                       WHERE c.status = PUBLISHED AND LOWER(c.title) LIKE '%' AND LOWER(c.level) = 'iniciante'
                       ORDER BY c.createdAt DESC, c.id
               → Hibernate executa SELECT + COUNT
                  │
5. dto         a consulta já devolve Page<CourseCardResponse>. Nenhuma CourseEntity foi carregada.
   (projeção)  Nenhum mapper. Por isso entity/ não aparece neste fluxo.
                  │
6. controller  → PageResponse.from(page)
               → ResponseEntity.ok(...)
                  │
7. HTTP        200  {"content":[{"id":"...","title":"...","instructor":"Dra. Mariana","lessonsCount":32,...}],"pageNumber":0,"pageSize":10,"totalElements":1,"totalPages":1}
```

Duas diferenças em relação ao `POST`: o repository devolve DTO direto (projeção), e não há `ApiResponse`, porque endpoints paginados devolvem `PageResponse` por contrato com o frontend. Filtro sem resultado devolve `200` com `content: []`; não é erro.

---

## Regras de comunicação

1. **Dentro de uma feature, a seta só anda para a direita.** Controller → Service → Repository → Entity. Controller nunca importa repository nem entity.
2. **Entre features, só pelo service.** `EnrollmentService` chama `CourseCatalogService`, nunca `CourseCatalogRepository`. O service da outra feature é a API pública dela.
3. **Entre subfeatures da mesma feature, idem.** `CourseReviewService` chama `CourseManagementService` para achar o curso.
4. **Entidades JPA podem referenciar entidades de qualquer feature.** `UserCourseEntity` aponta para `UserEntity` e `CourseEntity`. É o mapeamento do banco, não acoplamento de lógica.
5. **DTO é o único objeto que sai pela borda HTTP.** Entity nunca vira JSON.
6. **Exception é o único objeto que volta contra a seta.** Sobe do service até o handler sem ninguém capturar.
7. **`common` é usado por todos e não usa ninguém.**
8. **Só o service abre transação.** Controller e repository nunca têm `@Transactional`.

---

## Onde cada camada é testada

| Camada | Tipo de teste | Base | Precisa de Docker? |
|---|---|---|---|
| Controller | `@SpringBootTest` + MockMvc | `common.WebIntegrationTest` | sim |
| Service | Mockito, sem Spring | nenhuma | não |
| Repository | `@DataJpaTest` | `common.JpaIntegrationTest` | sim |
| Entity | só quando há trigger ou constraint | `common.JpaIntegrationTest` | sim |
| DTO | não tem; coberto por controller e repository | | |
| Exception | não tem; coberta por service e controller | | |
| common | handler com MockMvc standalone; health com contexto | | só o health |

A árvore de teste espelha a de produção, no mesmo pacote da classe testada.

---

## Resumo: o que cada camada sabe e não sabe

| Camada | Sabe de | Não sabe de |
|---|---|---|
| Controller | HTTP, rota, DTO, um service | repository, entity, regra, status de erro |
| Service | regra, transação, repository, exceção da feature | HTTP, `HttpStatus`, JSON |
| Repository | JPQL, entity, projeção em DTO | regra, HTTP, transação |
| Entity | tabela, coluna, relacionamento | regra, JSON, HTTP |
| DTO | JSON, validação de formato, `from(entity)` | banco, regra |
| Exception | tipo da falha (404/409/422), `code` | `HttpStatus` |
| common | envelopes, handler, config | qualquer feature |

*Última atualização: Setembro/2026*
