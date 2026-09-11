# common (transversal)

> Parte de [docs/CAMADAS.md](../CAMADAS.md), que mostra como as camadas se conversam.

Pastas: `common/api`, `common/exception`, `common/config`.

## Objetivo da camada

Guardar o que toda feature usa e nenhuma feature possui: envelopes de resposta, exceções base, tradução de erro em HTTP, configuração do Spring, health check.

## Por que é necessária

Se cada feature tivesse o próprio envelope e o próprio handler, a API teria formatos diferentes por rota. `common` garante que `ApiResponse`, `ApiError` e os status são iguais em todo lugar.

## Por que foi implementada dessa forma

- **`common/api/GlobalExceptionHandler`** é o único lugar que importa `HttpStatus` para erro. Quatro famílias, uma seção por família, um método privado por família por causa do limite de 40 linhas do Checkstyle. Detalhe de infraestrutura vai para o log, nunca para o cliente.
- **`common/api/ApiError`** tem `code` além de `message`, para o frontend não precisar comparar texto.
- **`common/api/PageResponse.from(Page)`** converte a página do Spring Data no envelope da API em um lugar só.
- **`common/exception`** tem só as três bases mais a abstrata. Exceção concreta nunca fica aqui.
- **`common/config/SecurityConfig`** expõe o `PasswordEncoder`. Quando autenticação entrar, a cadeia de filtros e a whitelist de `/api/v1/public/**` vêm para cá.
- **`HealthController` em `common/api`** porque health check não é feature: não tem service, regra nem dado.

## Observações

- `common` nunca importa nada de feature. Se importasse, uma mudança em `user` quebraria `course`.
- Teste: `GlobalExceptionHandlerTest` com MockMvc standalone, sem contexto Spring, um controller de mentira lançando cada família. `HealthControllerTest` com contexto completo, que também prova que a aplicação sobe.
- As bases de teste (`PostgresTestContainer`, `JpaIntegrationTest`, `WebIntegrationTest`) ficam em `src/test/.../common`. É o único lugar onde a árvore de teste tem algo que a de produção não tem.

---

## Com quem conversa

Usado por: todas as features (envelopes, exceções base, `PasswordEncoder`). Usa: nenhuma feature. Recebe: toda exceção não tratada, no handler.

---

← Anterior: [Exception](exception.md)
