# infrastructure — onde a tecnologia entra

Aqui moram os detalhes técnicos concretos: como salvamos dados de verdade, como configuramos o Spring, como resolvemos CORS, como geramos a documentação da API. É a **única** camada que pode depender de bibliotecas externas e frameworks.

## Subpastas

### `persistence/`
Implementação real de acesso a banco de dados via JPA.

Exemplos já implementados:
- `UserEntity` — o mapeamento da tabela `users` (com anotações `@Entity`/`@Column`). Fica só aqui dentro — nunca deve "vazar" pra outras camadas.
- `UserJpaRepository` — repositório do Spring Data que fala com o Postgres.
- `UserMapper` — converte entre o `User` de domínio (puro) e o `UserEntity` de banco (com anotações).
- `UserRepositoryAdapter` — implementa o `UserRepositoryPort` (definido em `application/port`) usando o JPA por baixo dos panos.

### `config/`
Configuração de Spring e de bibliotecas de infraestrutura.

Exemplos já implementados:
- `SpringConfig` — registra manualmente os beans dos use cases (`CreateUserUseCase`, etc.), conectando-os às implementações concretas das portas.
- `CorsConfig` — define quais origens (domínios do frontend) podem chamar a API.
- `OpenApiConfig` — configura o título/descrição da documentação Swagger.

Também ficam aqui as **migrations do Flyway** (`src/main/resources/db/migration/`), que descrevem o schema real do banco de dados.

## Por que essa é a única camada "livre"?
Porque é aqui que as decisões técnicas (qual banco, qual framework web, qual biblioteca de segurança) realmente moram. Trocar Postgres por outro banco, ou Spring por outro framework, deveria significar reescrever *só* essa camada — `domain`, `application` e `adapters` continuam intactos.

## Regra de dependência
`infrastructure` implementa os contratos (portas) definidos em `application` — nunca o contrário. `application` não sabe que `infrastructure` existe.

## Analogia rápida
Se `application` é a "lista de compras" (o que precisa ser feito), `infrastructure` é "ir ao mercado de verdade" — trocar de mercado (banco, framework) não muda a lista.
