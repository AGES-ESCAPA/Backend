# GitHub Copilot Code Review — Escapa! Backend

## Contexto

Backend da plataforma de cursos e qualificação profissional em Turismo e Hospitalidade.

**Stack:** Java 21 (LTS), Spring Boot 3.5.5, Spring Data JPA, Bean Validation, PostgreSQL 16,
Flyway, SpringDoc OpenAPI, Checkstyle, Jacoco, JUnit 5 + Mockito + Testcontainers,
Docker/Docker Compose, GitHub Actions.

**Arquitetura:** pacote por feature, com subfeatures e pastas por papel
(`controller`, `service`, `repository`, `entity`, `dto`, `exception`). O guia completo está em
`AGENTS.md`. **Não sugira Clean Architecture, portas, adapters, casos de uso, mappers nem entidades
de domínio separadas das JPA** — esse desenho foi removido do repositório de propósito.

**Objetivo do review:** apontar bugs, riscos e violações das regras abaixo com sugestões concretas,
priorizadas por impacto. Prefira comentários acionáveis a observações de estilo (o Checkstyle já
cobre formatação e quebra o build sozinho).

---

## Regras de estrutura (violação = bloqueante)

```
Controller  →  Service  →  Repository  →  Entity
```

- **Controller** só recebe, valida com `@Valid`, chama um service e devolve DTO. Sinalize controller
  que importa `repository` ou `entity`, que tem `if` de regra de negócio, ou que monta `ResponseEntity`
  de erro à mão.
- **Service** é o único lugar com `@Transactional` (`readOnly = true` em leitura) e com decisão de
  negócio. Sinalize `@Transactional` em controller ou repository.
- **Repository** é interface Spring Data. Repositório só de leitura deve estender `Repository<T, ID>`,
  não `JpaRepository`. Parâmetro de JPQL nunca pode chegar nulo (vira `bytea` no Postgres); o service
  normaliza antes.
- **Entity** é só mapeamento JPA, sufixo `Entity`, sem lógica.
- **DTOs** são `record`s em `dto/`; entrada com Bean Validation, saída com `from(entity)`.
- Entre subfeatures e entre features, a comunicação é **só pelo service**. Sinalize service de uma
  feature importando repository de outra.
- `common/` não pode importar nada de feature.
- Pasta nova dentro de feature só pode ser um dos papéis acima ou uma subfeature. Sinalize
  `course/controller/`, `course/service/` etc. no nível da feature: isso é camada disfarçada.
- Toda feature e subfeature tem o template completo (`controller`, `service`, `repository`, `dto`,
  `exception`, e `entity` fora de `shared/`). Feature nova sem alguma dessas pastas (com
  `package-info.java` quando vazia) deve ser apontada. `common` é a única pasta fora do template;
  o health check mora em `common/api` por não ser feature.

---

## Erros (violação = bloqueante)

- Regra de negócio lança exceção da feature em `<feature>/exception/`, herdando de
  `common.exception.NotFoundException` (404), `ConflictException` (409) ou `BusinessRuleException` (422),
  com `code` estável em maiúsculas (`USER_NOT_FOUND`).
- Sinalize `IllegalArgumentException`, `IllegalStateException` ou `RuntimeException` crua usada como
  regra de negócio, e qualquer `HttpStatus` fora de `common.api`.
- Sinalize `try/catch` de `DataAccessException` em service: deve subir para o `GlobalExceptionHandler`.
- Sinalize mensagem de erro que exponha nome de constraint, SQL ou stack trace ao cliente.
- Service deve checar a regra antes de gravar (`existsBy...` antes de `save`); a constraint do banco é
  rede de segurança, não validação principal.

---

## Prioridades de Revisão

1. **Regras de estrutura e de erro** acima.
2. **Corretude & bugs** — `Optional` mal usado, nulos, conversões, datas/fusos, coleções mutáveis
   expostas, concorrência.
3. **Migrations e schema** — seção própria abaixo.
4. **Contratos de API** — Bean Validation no DTO, status correto, envelope certo, rotas sob `/api/v1`.
5. **Segurança** — validação de entrada, parâmetros nomeados em queries, CORS restrito, segredos fora
   do código.
6. **Persistência** — N+1 (sugira projeção `SELECT new` ou `@EntityGraph`), paginação com `ORDER BY`
   determinístico, `equals`/`hashCode` em entidades.
7. **Testes** — seção própria abaixo.
8. **Manutenibilidade** — nomes, dead code, complexidade.

---

## Migrations (Flyway)

Schema por Flyway; Hibernate roda em `ddl-auto=validate`, só confere.

- Alteração em entidade JPA que mude coluna, tipo, nullability, constraint ou tabela **exige**
  migration nova em `src/main/resources/db/migration/`. Sem ela a aplicação não sobe. Bloqueante.
- Migrations aplicadas são **imutáveis**: correção vem como migration nova. Sinalize edição de `V*`.
- Confira que migration e entidade descrevem o mesmo schema.
- Constraints (unique, FK, check, default) e contadores desnormalizados por trigger são aceitos no
  banco. Regra de decisão (publicar, bloquear, notificar) em trigger **não** é aceita: pertence ao service.
- Coluna derivada nova precisa de mecanismo de atualização declarado e testado. Hoje `lessons_count`
  tem trigger (V5) e teste; `reviews_count`, `rating_average`, `materials_count` e `students_count`
  ainda não têm, e isso está documentado como pendência.

---

## Testes

Espelham a árvore de produção, no **mesmo pacote** da classe testada, sufixo `Test`.

- **Service**: unitário com Mockito, sem Spring. Confere regra e o que chega ao repositório.
- **Repository**: `@DataJpaTest` estendendo `common.JpaIntegrationTest` (Testcontainers). Para
  trigger, `flush()` e `clear()` antes de reler.
- **Controller**: `@SpringBootTest` + MockMvc estendendo `common.WebIntegrationTest`. Cobre status,
  envelope, nomes dos campos e `code` de erro. Não é transacional: dados únicos por teste.
- Um único container Postgres em `common.PostgresTestContainer`. Sinalize `@Container` por classe.
- Sinalize fake em memória de repositório: o padrão é Mockito para service e banco real para repository.
- Todo service, repository com consulta própria e controller novo precisa de teste. Não peça teste de
  DTO, entidade ou configuração trivial.
- Asserts significativos: comportamento e valores, não só `notNull`.

---

## Contratos de API

- Rotas sob `/api/v1`; públicas sob `/api/v1/public/**`.
- Sucesso em `ApiResponse` (`success`, `data`, `message`). Endpoints paginados devolvem `PageResponse`
  direto (`content`, `pageNumber`, `pageSize`, `totalElements`, `totalPages`).
- Erro em `ApiError` (`status`, `error`, `code`, `message`, `path`, `timestamp`). Mapeamento em
  `common.api.GlobalExceptionHandler`: 400 validação, 404/409/422 negócio, 404/405/415 protocolo,
  409 integridade, 500 inesperado. Endpoint novo se encaixa nele; não trata exceção no controller.
- Se o status real diverge do OpenAPI gerado, aponte.

---

## Configuração e infraestrutura

- Configuração via `${VAR:default}` em `application.properties`. Sem valor fixo no código.
- CORS nunca `*`; origens em `APP_CORS_ALLOWED_ORIGINS`.
- Segredos fora do código e de arquivos versionados.
- `Dockerfile`: dependências antes do `COPY src`, usuário não-root, jar com nome fixo.

---

## O que NÃO comentar

- Formatação, chaves, imports e locais `final` — Checkstyle já reprova.
- Sugestões de Clean Architecture, hexagonal, portas, adapters, casos de uso ou domínio separado.
- Cobertura de teste em DTOs, entidades, getters e configuração trivial.
