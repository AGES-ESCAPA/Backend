# Controller

> Parte de [flow-between-layers.md](flow-between-layers.md), que mostra como as camadas se conversam.

Pasta: `<feature>/controller/`. Exemplos: `user/controller/UserController`, `course/catalog/controller/CourseCatalogController`.

## Objetivo da camada

Ser a porta de entrada HTTP da feature. Traduz requisição em chamada de método e resultado em resposta JSON. Só isso.

## Por que é necessária

Alguém precisa saber de rota, verbo HTTP, status code, `@RequestParam`, `@RequestBody` e envelope de resposta. Se esse conhecimento vazasse para o service, o service não poderia ser chamado por outra feature nem testado sem Spring MVC.

## Por que foi implementada dessa forma

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

## Observações

- Endpoints paginados devolvem `PageResponse` direto, sem `ApiResponse`, por contrato com o frontend definido na US-01. É a única exceção ao envelope.
- Teste: `@SpringBootTest` + MockMvc, estendendo `common.WebIntegrationTest`. Cobre rota, status, envelope, nomes dos campos e `code` de erro. Não testa regra de negócio: isso é do service.
- Quando autenticação entrar, o controller continua igual. Quem sabe de token é um filtro em `common/config`, não o controller.

## Com quem conversa

Recebe de: cliente HTTP. Chama: **um** service da própria feature. Devolve: DTO de saída dentro de `ApiResponse` ou `PageResponse`. Nunca fala com repository, entity ou outra feature.

---

Próxima: [Service](service.md) →
