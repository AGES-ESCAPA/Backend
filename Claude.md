# Claude.md — Escapa! Backend

Resumo operacional. O guia completo, com regras, tabela de erros e estratégia de teste, está em [AGENTS.md](AGENTS.md). Leia os dois.

## O projeto

Backend em **Java 21 + Spring Boot 3.5** da plataforma de cursos em Turismo e Hospitalidade. PostgreSQL 16 com Flyway. Sistema majoritariamente CRUD.

## Arquitetura: pacote por feature

```text
com.escapa.backend
├── common/        api/ (inclui HealthController)  exception/  config/   # transversal, não conhece feature
├── user/          controller/ service/ repository/ entity/ dto/ exception/
├── course/
│   ├── shared/    entity/                            # usado por todas as subfeatures
│   ├── catalog/   controller/ service/ repository/ dto/ exception/   # vitrine pública (US-01)
│   ├── management/ (mesmo template)                  # CRUD do admin
│   └── review/    (mesmo template)                   # avaliações
├── enrollment/    controller/ service/ repository/ entity/ dto/ exception/
└── notification/  controller/ service/ repository/ entity/ dto/ exception/
```

Toda pasta fora de `common` é uma feature com o template completo. Pasta ainda sem classe tem `package-info.java`.

Fluxo dentro de uma subfeature: **Controller → Service → Repository → Entity**. Controller nunca vê repository nem entity. Só o service tem `@Transactional`. Entre features e subfeatures, só pelo service.

## Regras que quebram o build ou a revisão

- Sem `domain/`, `application/`, `adapters/`, `infrastructure/`, portas, adapters, mappers ou fakes em memória. Esse desenho foi removido de propósito.
- Regra de negócio lança exceção da feature herdando de `NotFoundException` (404), `ConflictException` (409) ou `BusinessRuleException` (422), de `common.exception`. Nunca `IllegalArgumentException`. Nunca `HttpStatus` fora de `common.api`.
- Toda rota sob `/api/v1`. Sucesso em `ApiResponse`; endpoints paginados em `PageResponse` direto. Erro em `ApiError` com `code`.
- Entidade JPA mudou coluna ou constraint: migration nova, sem editar as antigas. Hibernate roda em `validate`.
- Checkstyle na fase `validate`: chaves obrigatórias, locais `final`, sem número mágico, método até 40 linhas.

## Testes

Mesmo pacote da classe testada. Service com Mockito. Repository com `@DataJpaTest` estendendo `common.JpaIntegrationTest`. Controller com MockMvc estendendo `common.WebIntegrationTest`. Um único container Postgres em `common.PostgresTestContainer`.

## Comandos

```powershell
mvn -B clean verify        # Checkstyle + testes + cobertura (Docker rodando)
mvn spring-boot:run        # perfil dev, com seed
docker compose up --build
```

## Commits e branches

`<tipo>(<id_clickup>): <descrição>`. Branch `<tipo>/<id_clickup>-<descricao>` a partir de `develop`. MR para `develop`.

*Última atualização: Setembro/2026*
