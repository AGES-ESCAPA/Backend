# Exception

> Parte de [docs/CAMADAS.md](../CAMADAS.md), que mostra como as camadas se conversam.

Pasta: `<feature>/exception/`. Exemplos: `user/exception/UserNotFoundException`, `user/exception/EmailAlreadyUsedException`.

## Objetivo da camada

Dar nome e código estável a cada forma de o negócio dizer "não". A feature declara o que pode dar errado; o `common` decide como isso vira HTTP.

## Por que é necessária

Sem exceção nomeada, o service teria que devolver `null`, `Optional` ou `boolean` e o controller teria que interpretar cada caso com `if`. Com exceção nomeada, o service lança, o handler traduz, e nem controller nem service sabem de status HTTP.

## Por que foi implementada dessa forma

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

## Observações

- 400 não é regra de negócio. 400 é formato errado (Bean Validation, `page=abc`, JSON quebrado) e vem de exceções do próprio Spring. Regra de negócio começa em 404.
- Erro de infraestrutura (banco caiu, constraint violada em condição de corrida, `NullPointerException`) não tem classe na feature. Sobe cru até o handler, que devolve 409 ou 500 com mensagem fixa e registra o detalhe só no log.
- `course/catalog/exception` está vazia de propósito: a vitrine não tem regra que falhe.
- Exceção não tem teste próprio. O teste de service confere que ela é lançada; o de controller confere o status e o `code`.

## Com quem conversa

Lançada por: service. Atravessa: controller, sem `try/catch`. Capturada por: `common/api/GlobalExceptionHandler`, que escolhe o status pela classe base. Nunca carrega `HttpStatus`.

---

← Anterior: [DTO](dto.md) · Próxima: [common (transversal)](common.md) →
