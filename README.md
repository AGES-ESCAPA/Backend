# Escapa! — Plataforma de Cursos (Backend)

Backend da plataforma de **Educação Continuada da ESCAPA**, desenvolvido em **Java + Spring Boot** para atender cursos, usuários, módulos, progresso e gestão administrativa em um ambiente escalável e organizado pela arquitetura de Clean Architecture.

O backend foi pensado para servir o frontend e manter o domínio isolado de detalhes de infraestrutura, banco e frameworks.

---

## 📋 Sumário

- [Visão Geral e Contexto](#-visão-geral-e-contexto)
- [Tecnologias Utilizadas](#-tecnologias-utilizadas)
- [Estrutura de Pastas e Onde Desenvolver](#-estrutura-de-pastas-e-onde-desenvolver)
- [Pré-requisitos](#-pré-requisitos)
- [Instalação e Setup](#-instalação-e-setup)
- [Comandos Disponíveis (Maven)](#-comandos-disponíveis-maven)
- [Fluxo de Validação de Tarefas (Evite falhas na CI)](#-fluxo-de-validação-de-tarefas-evite-falhas-na-ci)
- [Padrão de Commits](#-padrão-de-commits)
- [Estratégia de Branches](#-estratégia-de-branches)
- [Regras de Clean Architecture](#-regras-de-clean-architecture)
- [Containerização com Docker](#-containerização-com-docker)

---

## 🎯 Visão Geral e Contexto

### O que é a plataforma?
Uma plataforma digital dedicada a cursos, capacitação profissional e certificação para estudantes, profissionais e empresas do setor de turismo e hospitalidade.

### Funcionalidades no Escopo Inicial:
- **Cadastro e consulta de usuários**
- **Endpoints base para health check e operação inicial**
- **Estrutura pronta para cursos, módulos, aulas, progresso e gestão administrativa**
- **Persistência em PostgreSQL**
- **API REST organizada por camadas**

### 🚫 Fora do Escopo Inicial:
- autenticação e autorização avançada
- integrações com pagamentos
- integrações com IA
- streaming em tempo real
- multi-tenancy e multilíngue

---

## 📁 Estrutura de Pastas e Onde Desenvolver

Para manter o projeto organizado e fácil de evoluir, cada camada tem uma responsabilidade bem definida:

```text
src/
├── main/
│   ├── java/com/escapa/backend/
│   │   ├── domain/                  → entidades e regras de negócio puras
│   │   │   └── user/
│   │   ├── application/             → casos de uso e portas de comunicação
│   │   │   ├── port/
│   │   │   └── usecase/
│   │   ├── adapters/                → controllers, DTOs, handlers e adaptadores web
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── exception/
│   │   │   └── mapper/
│   │   ├── infrastructure/          → JPA, repositórios, configurações e integrações externas
│   │   │   ├── config/
│   │   │   └── persistence/
│   │   └── EscapaBackendApplication.java
│   └── resources/
│       └── application.properties
└── test/
    └── java/
        └── com/escapa/backend/
```

### 🧭 Guia Prático: Onde colocar meu código?

#### 1. `domain/` — Núcleo do negócio
- **O que vai aqui:** entidades puras e regras do domínio.
- **Regra:** sem Spring, sem JPA, sem frameworks.
- **Exemplo:** `User`, regras de validação e comportamento do domínio.

#### 2. `application/` — Casos de uso e portas
- **O que vai aqui:** orquestração da funcionalidade e interfaces para comunicação externa.
- **Regra:** depende apenas do domínio.
- **Exemplo:** `CreateUserUseCase`, `UserRepositoryPort`.

#### 3. `adapters/` — Entrada e saída da API
- **O que vai aqui:** controllers REST, DTOs, tratamento de exceções e conversões.
- **Regra:** não contém lógica de negócio.
- **Exemplo:** `UserController`, `CreateUserRequest`, `GlobalExceptionHandler`.

#### 4. `infrastructure/` — Implementação técnica
- **O que vai aqui:** JPA, banco de dados, configuração do Spring, repositórios concretos.
- **Regra:** implementa as portas da camada `application` e integra com frameworks.
- **Exemplo:** `UserEntity`, `UserJpaRepository`, `UserRepositoryAdapter`.

---

## 🛠️ Tecnologias Utilizadas

- **Linguagem**: Java 21
- **Framework**: Spring Boot 3.5.5
- **Persistência**: Spring Data JPA
- **Banco de Dados**: PostgreSQL
- **Migrações de banco**: Flyway (scripts versionados em `src/main/resources/db/migration/`)
- **Build**: Maven
- **Validação**: Bean Validation (`@Valid`)
- **Documentação da API**: SpringDoc OpenAPI (Swagger UI)
- **Qualidade de código**: Checkstyle (executado na fase `validate` do Maven)
- **Cobertura de testes**: Jacoco (relatório gerado em `target/site/jacoco/`)
- **Containerização**: Docker + Docker Compose
- **Testes**: JUnit 5 + Spring Test + Testcontainers

---

## ⚙️ Pré-requisitos

- **Java 21** ou superior
- **Maven 3.9+**
- **Docker** e **Docker Compose** — **obrigatórios para rodar os testes**: os testes de integração sobem um PostgreSQL descartável via Testcontainers, então `mvn test` falha se o Docker não estiver em execução
- **PostgreSQL** — não precisa instalar: sobe em container pelo `docker-compose.yml`
- **Git**

---

## 🚀 Instalação e Setup

1. **Clone o repositório:**
   ```bash
   git clone <url-do-repositorio>
   cd Backend
   ```

2. **Crie o arquivo de ambiente a partir do exemplo:**
   ```bash
   cp .env.example .env
   ```

3. **Ajuste as variáveis de ambiente conforme seu banco local**.

4. **Inicie o banco e a aplicação localmente**:

### Opção A — tudo em containers
   ```bash
   docker compose up --build
   ```

### Opção B — banco em container, aplicação pelo Maven
   Útil para desenvolver com hot reload sem reconstruir a imagem a cada mudança:
   ```bash
   docker compose up -d postgres   # sobe apenas o banco
   mvn spring-boot:run
   ```

A API ficará disponível em: `http://localhost:8080`

---

## 💻 Comandos Disponíveis (Maven)

| Comando | O que faz? | Quando usar? |
|---|---|---|
| `mvn spring-boot:run` | Inicia a aplicação localmente. | Durante o desenvolvimento. |
| `mvn test` | Executa os testes unitários e os de integração. **Requer Docker em execução** (Testcontainers). | Antes de commit / MR. |
| `mvn clean test` | Remove artefatos antigos e roda testes novamente. **Requer Docker.** | Validação limpa do projeto. |
| `mvn clean verify` | Roda o mesmo que a CI: Checkstyle, testes e relatório de cobertura. **Requer Docker.** | Antes de abrir o MR. |
| `mvn clean package` | Gera o pacote compilado da aplicação. | Verificação de build final. |
| `mvn validate` | Valida a estrutura e dependências do Maven. | Para checar a configuração do projeto. |
| `mvn checkstyle:check` | Roda só as regras de `checkstyle.xml`, sem compilar/testar. | Para checar estilo isoladamente e mais rápido. |

> 📊 Após rodar os testes, o relatório de cobertura do Jacoco fica em `target/site/jacoco/index.html`.

---

## ✅ Fluxo de Validação de Tarefas (Evite falhas na CI)

Antes de abrir um **Merge Request**, execute o checklist abaixo:

```bash
# 1. Rodar testes (o Checkstyle roda automaticamente na fase validate)
mvn test

# 2. Compilar a aplicação
mvn clean package
```

> 💡 **Validação básica recomendada:**
> ```bash
> mvn clean test && mvn clean package
> ```

> ⚠️ O build falha se houver violação das regras de `checkstyle.xml` (chaves obrigatórias em `if`, variáveis locais `final`, etc.). Para checar isoladamente: `mvn checkstyle:check`.

> 🐳 **O Docker precisa estar rodando** antes de executar os testes: os de integração sobem um PostgreSQL descartável via Testcontainers. Sem ele, `mvn test` falha na inicialização do container, não por erro no seu código.

---

## 📝 Padrão de Commits

Utilizamos o padrão com o **ID da tarefa** do ClickUp:

### Formato:
```text
<tipo>(<id_clickup>): <descrição clara>
```

### Tipos:
- `feat`: nova funcionalidade
- `fix`: correção de bug
- `docs`: atualização de documentação
- `style`: ajustes de formatação e estilo
- `refactor`: refatoração sem mudança funcional
- `test`: adição ou ajuste de testes
- `chore`: manutenção de dependências e setup

### Exemplos:
- `feat(86a1b2c): add user creation flow`
- `feat(86a1b2d): create health check endpoint`
- `fix(86a1b2e): fix invalid email validation`
- `test(86a1b2f): add unit tests for create user use case`

---

## 🌿 Estratégia de Branches

Adotamos o fluxo com branch de integração **`develop`** e branch principal **`main`**.

```text
main (Produção estável)
   ↑
develop (Integração do time)
   ↑
├── feat/86a1b2c-criar-usuario
├── feat/86a1b2d-health-check
└── fix/86a1b2e-validacao-email
```

### Nomenclatura das branches:
```text
<tipo>/<id_clickup>-<breve-descricao>
```

### Passo a passo para desenvolver uma tarefa:
```bash
# 1. Atualizar a branch develop local
git checkout develop
git pull origin develop

# 2. Criar a branch da tarefa
git checkout -b feat/86a1b2c-criar-usuario

# 3. Desenvolver e validar
git add .
git commit -m "feat(86a1b2c): create user registration flow"

# 4. Enviar para o repositório
git push -u origin feat/86a1b2c-criar-usuario
```

---

## 🧱 Regras de Clean Architecture

> ⚠️ **A regra da dependência deve ser respeitada sempre**: as dependências devem apontar para o núcleo, nunca o contrário.

### Camadas obrigatórias

#### 1. `domain/`
- entidades puras
- sem Spring, JPA ou frameworks
- sem `@Entity`, `@Service`, `@Controller` e similares
- regras de negócio e validação do núcleo

#### 2. `application/`
- casos de uso e portas
- comunicação com o mundo externo via interfaces
- orquestração do fluxo de negócio

#### 3. `adapters/`
- controllers REST
- DTOs de entrada/saída
- tratamento de erros e adaptação HTTP

#### 4. `infrastructure/`
- persistência
- Spring configuration
- JPA, repositories e serviços externos

### Regras de implementação
- **Nunca misture entidade de domínio com entidade JPA**
- **Nunca coloque lógica de banco na camada de domínio**
- **Nunca esconda regras de negócio dentro do controller**
- **Use nomes consistentes**: `UserController`, `CreateUserUseCase`, `UserRepositoryPort`
- **Use `record` em DTOs quando fizer sentido**

---

## 🐳 Containerização com Docker

Para rodar a aplicação em um ambiente mais próximo do deploy real:

```bash
docker compose up --build
```

### Serviços incluídos
- PostgreSQL em container
- Backend Spring Boot em container

### Endpoints úteis
- `http://localhost:8080/api/v1/health` → health check da aplicação
- `http://localhost:8080/api/v1/users` → cadastro e consulta de usuários
- `http://localhost:8080/swagger-ui/index.html` → documentação interativa da API (Swagger UI)
- `http://localhost:8080/v3/api-docs` → especificação OpenAPI em JSON

---

## 🌐 CORS

A origem do frontend liberada para consumir a API é configurada via `APP_CORS_ALLOWED_ORIGINS` (ver `.env.example`), aplicada a todas as rotas `/api/**`. O valor padrão é `http://localhost:3000` (porta do Vite dev server do frontend).

---

## 📌 Endpoints principais do Boilerplate

Todas as respostas de sucesso são padronizadas no envelope `ApiResponse` (`success`, `data`, `message`). Erros seguem o formato `ApiError` (`status`, `error`, `message`, `path`, `timestamp`).

### Health Check
```http
GET /api/v1/health
```

Resposta esperada:
```json
{
  "success": true,
  "data": { "status": "UP", "service": "escapa-backend" },
  "message": "Operation completed successfully"
}
```

### Cadastro de Usuário
```http
POST /api/v1/users
```

Payload:
```json
{
  "name": "Maria Souza",
  "email": "maria@email.com",
  "role": "student"
}
```

Resposta (`201 Created`):
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "Maria Souza",
    "email": "maria@email.com",
    "role": "STUDENT"
  },
  "message": "User created successfully"
}
```

### Listagem de Usuários
```http
GET /api/v1/users
```

### Consulta de Usuário por ID
```http
GET /api/v1/users/{id}
```

Retorna `404` com `ApiError` quando o `id` não existe.

---

## 🔎 Observações finais

Este README reflete o estado inicial do projeto: a estrutura foi preparada para crescer com segurança, sem acoplamentos desnecessários, e com padrões que permitirão expandir para módulos de cursos, módulos, aulas, progresso e painel administrativo sem reestruturar a base.
