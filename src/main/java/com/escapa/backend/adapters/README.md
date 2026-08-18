# adapters — a porta de entrada e saída da API

Esta é a camada que conversa com o mundo de fora via HTTP: recebe requisições, valida entrada, chama o caso de uso certo (`application`) e formata a resposta.

## Subpastas

### `controller/`
Os endpoints REST em si. Só **orquestram**: recebem a requisição, chamam o use case certo, devolvem a resposta — sem tomar nenhuma decisão de negócio.

Exemplos já implementados: `UserController`, `HealthController`.

### `dto/`
Os formatos de entrada e saída da API (*Data Transfer Objects*) — o "contrato" JSON que o cliente vê, que não precisa ser igual à entidade de domínio por dentro.

Exemplos já implementados:
- `CreateUserRequest` — o que o cliente envia pra criar um usuário.
- `UserResponse` — o que devolvemos sobre um usuário.
- `ApiResponse` — o envelope padrão de toda resposta de sucesso (`success`, `data`, `message`).

### `exception/`
Transforma erros (de domínio, validação, etc.) em respostas HTTP padronizadas e previsíveis.

Exemplos já implementados:
- `GlobalExceptionHandler` — intercepta exceções da aplicação inteira e decide o status HTTP certo (400 validação, 404 não encontrado, 409 conflito, 500 erro inesperado).
- `ApiError` — o envelope padrão de toda resposta de erro (`status`, `error`, `message`, `path`, `timestamp`).

## O que NUNCA vai aqui
Lógica de negócio. Se um controller estiver decidindo algo (tipo "não pode ter dois usuários com o mesmo email"), essa regra está no lugar errado — deveria estar no `domain` ou no `application`.

## Por que separar DTO de entidade de domínio?
Porque o formato que o cliente HTTP manda/recebe (JSON) não precisa ser idêntico ao formato interno das entidades — isso dá liberdade pra mudar a API sem mexer no núcleo do negócio, e vice-versa.

## Regra de dependência
`adapters` depende de `application` (chama os use cases) e, através dele, indiretamente de `domain`. Nunca depende de `infrastructure` diretamente.
