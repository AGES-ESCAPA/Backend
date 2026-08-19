# application — a orquestração dos casos de uso

Esta pasta descreve **o que o sistema faz**, passo a passo, sem se preocupar em *como* os dados chegam (isso é HTTP, fica em `adapters`) nem *como* são salvos de verdade (isso é banco, fica em `infrastructure`).

## Subpastas

### `usecase/`
Cada ação que o sistema oferece vira uma classe própria — um **caso de uso**. Cada caso de uso sabe executar exatamente uma ação de negócio, usando o `domain` e as portas abaixo.

Exemplos já implementados: `CreateUserUseCase`, `ListUsersUseCase`, `GetUserByIdUseCase`.

> Convenção de nome: `<Ação><Entidade>UseCase` (ex.: `CreateUserUseCase`, `ListCoursesUseCase`).

### `port/`
Interfaces (contratos) que descrevem **o que a aplicação precisa do mundo externo**, sem dizer como isso é implementado. Uma "porta" é uma promessa: "eu preciso conseguir salvar e buscar usuários", sem saber se isso é feito com PostgreSQL, outro banco, ou um arquivo em memória.

Exemplo já implementado: `UserRepositoryPort`.

> Convenção de nome: interfaces terminam em `Port` (ex.: `UserRepositoryPort`).

## Por que usar portas (interfaces) em vez de chamar o banco direto?
Porque isso permite **testar os casos de uso sem precisar de um banco real**. Nos testes (`src/test/.../application/usecase/`), a mesma porta é implementada por um fake em memória (`InMemoryUserRepositoryPort`) — o caso de uso nem percebe a diferença.

## Regra de dependência
`application` só pode depender de `domain`. Nunca de Spring, JPA, ou qualquer coisa de `infrastructure`/`adapters` — quem implementa as portas daqui é a camada `infrastructure`, não o contrário.
