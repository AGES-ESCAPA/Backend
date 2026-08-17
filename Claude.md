# Claude.md — Escapa! Backend

## Sobre o Projeto

**Escapa!** é uma plataforma digital de **cursos e qualificação profissional em Turismo e Hospitalidade**.

- O backend é responsável pela API principal da plataforma.
- A aplicação é construida em **Java + Spring Boot**.
- O objetivo inicial é estruturar a base em **Clean Architecture** para escalar sem acoplamento de framework.
- A persistência principal será em **PostgreSQL**.
- O backend deve ser pensado para atender cursos, módulos, aulas, progresso, certificação e gestão administrativa.

---

## Arquitetura de Pastas e Responsabilidades

```text
src/
├── main/
│   ├── java/com/escapa/backend/
│   │   ├── domain/              # Entidades e regras internas do domínio
│   │   ├── application/         # Casos de uso, interfaces e regras de aplicação
│   │   ├── adapters/            # Controllers, DTOs, exceptions e adaptadores web
│   │   ├── infrastructure/      # Spring, JPA, persistência e conexões externas
│   │   └── EscapaBackendApplication.java
│   └── resources/
│       └── application.properties
├── test/
│   └── java/
│       └── com/escapa/backend/
├── Dockerfile
├── docker-compose.yml
├── pom.xml
├── .env.example
├── .gitignore
├── README.md
└── AGENTS.md
```

### Onde colocar o código:
- **Entidades e regras puras**: `domain/`
- **Fluxos e regras de aplicação**: `application/`
- **Controllers, DTOs e tratamento de API**: `adapters/`
- **Persistência e configurações do Spring**: `infrastructure/`

---

## Regras de Código e Estilo

### 1. Clean Architecture — Separação obrigatória

- **Domain** nunca depende de framework.
- **Application** depende apenas do domínio e de portas/contratos.
- **Infrastructure** implementa os contratos e integra com banco e frameworks.
- **Adapters** orquestram requisições e respostas HTTP.

### 2. Padrões de Código
- **Classes de domínio**: sem anotações do Spring ou JPA.
- **Casos de uso**: nomes em formato `CreateUserUseCase`, `ListCoursesUseCase`.
- **Interfaces de repositório**: nomes terminados em `Port`.
- **Controllers**: implementam endpoints REST, sem lógica de negócio.
- **DTOs**: usados para validar entrada e saída de API.
- **Entidades JPA**: ficam na camada `infrastructure.persistence` e não devem ser usadas fora dela.

### 3. Testes
- Todo caso de uso deve ter um teste unitário.
- Testes devem validar comportamento real e não apenas mocks imaginários.
- A estrutura deve permitir criação rápida de testes isolados para cada caso de uso.

### 4. Qualidade de Código
- O Checkstyle (`checkstyle.xml`) roda automaticamente na fase `validate` do Maven e quebra o build em caso de violação (chaves obrigatórias em `if`, variáveis locais `final`, etc.).
- O `Dockerfile` precisa copiar `checkstyle.xml` (além de `pom.xml`) para o estágio de build, senão `docker compose up --build` falha.

### 5. Commits e Branches
- **Commits**: `<tipo>(<id_clickup>): <descrição curta>`
- **Branches**: criadas a partir de **`develop`** no formato `<tipo>/<id_clickup>-<descricao>`
- **Merge Requests**: sempre para **`develop`**.

---

## Boas Práticas do Backend

- Prefira composição e injeção de dependência.
- Use exceções de domínio e tratamento global de erro.
- Mantenha a API response padronizada (envelope `ApiResponse` para sucesso, `ApiError` para erro).
- Valide entradas com `@Valid` e annotations.
- Conserve nomes consistentes em inglês para classes, métodos e pacotes.
- Todas as rotas HTTP usam o prefixo `/api/v1`.
- CORS é restrito às origens definidas em `APP_CORS_ALLOWED_ORIGINS` (aplicado a `/api/**`), nunca liberar `*` em produção.

---

*Última atualização: Agosto/2026*
