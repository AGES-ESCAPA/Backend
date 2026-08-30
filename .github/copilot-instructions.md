# GitHub Copilot Code Review — Escapa! Backend

## Contexto

Backend da plataforma de cursos e qualificação profissional em Turismo e Hospitalidade.

**Stack:** Java 21 (LTS), Spring Boot 3.5.5, Spring Data JPA, Bean Validation, PostgreSQL 16,
Flyway, SpringDoc OpenAPI, Checkstyle, Jacoco, JUnit 5 + Testcontainers, Docker/Docker Compose,
GitHub Actions.

**Arquitetura:** Clean Architecture. Esta é a característica mais importante do repositório e a
fonte mais comum de problemas em revisão. **Não sugira o padrão Controller/Service/Repository** —
não é o padrão adotado aqui.

**Objetivo do review:** apontar bugs, riscos e violações de arquitetura com sugestões concretas de
correção, priorizadas por impacto. Prefira comentários objetivos e acionáveis a observações
genéricas de estilo (o Checkstyle já cobre formatação e quebra o build sozinho).

---

## Regra da dependência (violação = bloqueante)

```
adapters ──────┐
               ├──> application ──> domain
infrastructure ┘
```

- **`domain/`** — entidades e regras puras. **Nunca** deve conter `@Entity`, `@Service`,
  `@Component`, `@Column` ou qualquer import de Spring, JPA ou biblioteca de infraestrutura.
  Sinalize qualquer anotação de framework aqui como erro grave.
- **`application/`** — casos de uso (`usecase/`) e portas (`port/`). Depende **apenas** do domínio.
  Casos de uso seguem o nome `<Ação><Entidade>UseCase`; interfaces terminam em `Port`.
  Não deve importar nada de `infrastructure` nem de `adapters`.
- **`adapters/`** — controllers REST, DTOs e tratamento de erro. **Sem regra de negócio.**
  Se um controller decide algo do domínio (ex.: "não pode haver dois usuários com o mesmo e-mail"),
  a regra está na camada errada.
- **`infrastructure/`** — JPA, configuração do Spring, integrações. Implementa as portas de
  `application`. Entidades JPA (`UserEntity`) **não podem vazar** para fora desta camada.

Nunca misture DTO, entidade de domínio e entidade JPA na mesma classe ou assinatura.

---

## Prioridades de Revisão

1. **Violações de arquitetura** — a regra da dependência acima. É o item de maior impacto.
2. **Corretude & bugs** — `Optional` mal usado, nulos, exceções não tratadas, conversões
   numéricas, datas/fusos, coleções mutáveis expostas, concorrência.
3. **Migrations e schema** — ver seção própria abaixo.
4. **Contratos de API** — validação via Bean Validation no DTO, códigos HTTP corretos,
   envelope de resposta padronizado, rotas sob `/api/v1`.
5. **Segurança** — validação de entrada, parâmetros nomeados em queries, CORS restrito,
   segredos fora do código, sem vazar stack trace em resposta de erro.
6. **Persistência** — N+1 (sugira `@EntityGraph` ou join fetch), paginação em listagens que
   possam crescer, `@Transactional` no boundary correto, `equals`/`hashCode` em entidades.
7. **Testes** — ver seção própria abaixo.
8. **Manutenibilidade** — nomes descritivos, dead code, complexidade desnecessária.

---

## Migrations (Flyway)

O schema é gerenciado por Flyway, e o Hibernate roda com `ddl-auto=validate` — ele apenas confere,
nunca cria nem altera tabelas.

- Toda alteração em entidade JPA que mude coluna, tipo, nullability, constraint ou tabela
  **exige** uma migration nova em `src/main/resources/db/migration/`. Sem ela, a aplicação
  **não sobe**. Sinalize a ausência como bloqueante.
- Migrations já aplicadas são **imutáveis**: correções vêm como uma migration nova, nunca
  editando um arquivo `V*` existente. Sinalize qualquer edição de migration existente.
- Mudanças apenas na entidade de domínio (`domain/user/User.java`), sem reflexo na `UserEntity`,
  **não** exigem migration.
- Confira que a migration e a entidade descrevem o mesmo schema (nome de coluna, tipo, nullable,
  unique).

---

## Testes

- Todo caso de uso novo precisa de teste unitário.
- Casos de uso são testados contra o fake `InMemoryUserRepositoryPort`, **não** com mocks de
  repositório. Prefira sugerir o fake existente a introduzir Mockito.
- Persistência, mapeamento JPA e migrations são testados com **Testcontainers**, estendendo
  `PostgresIntegrationTest` (o container é compartilhado; não crie um novo por classe).
- Endpoints devem ter teste de controller (`@WebMvcTest`) cobrindo status codes e o caminho de erro.
- Asserts significativos: prefira verificar comportamento e valores a apenas checar `notNull`.

---

## Contratos de API

- Todas as rotas usam o prefixo `/api/v1`.
- Sucesso usa o envelope `ApiResponse` (`success`, `data`, `message`); erro usa `ApiError`
  (`status`, `error`, `message`, `path`, `timestamp`).
- O `GlobalExceptionHandler` mapeia: **400** validação e regra de domínio, **404** recurso não
  encontrado, **409** conflito de dados, **500** erro inesperado. Endpoints novos devem se encaixar
  nesse mapeamento em vez de tratar exceção no controller.
- Valide entrada com `@Valid` e anotações no DTO, nunca com `if` manual no controller.
- Prefira `record` para DTOs.
- Se o status real diverge do documentado no OpenAPI (ex.: método retorna `201` mas a spec diz
  `200`), aponte — a documentação é gerada e pode ficar incorreta sem anotação explícita.

---

## Configuração e infraestrutura

- Toda configuração vem de variável de ambiente no formato `${VAR:default}` em
  `application.properties`. Não aceite valores fixos no código.
- Nunca libere CORS com `*`; as origens vêm de `APP_CORS_ALLOWED_ORIGINS`.
- Segredos jamais no código ou em arquivo versionado.
- Alterações no `Dockerfile` devem preservar: resolução de dependências antes do `COPY src`
  (cache de camada), execução como usuário não-root e o jar referenciado por nome fixo.

---

## O que NÃO comentar

- Formatação, chaves, imports e variáveis locais `final` — o Checkstyle já reprova o build.
- Sugestões de trocar a arquitetura por Controller/Service/Repository.
- Pedidos de cobertura de teste em DTOs, getters e classes de configuração triviais.
