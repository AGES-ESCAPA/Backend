# US-21 — API de cadastro de aluno individual

## Objetivo e escopo

Criar uma pessoa física, persistir `users` e `regular_users` na mesma transação e devolver o contrato de autenticação do login da US-23, com `role` e `profile` fixos em `STUDENT`.

Endpoint público: `POST /api/v1/auth/register/student`. Não exige JWT nem `X-User-Id`.

Esta entrega cobre o cadastro, suas validações, persistência, geração do token, documentação e testes. A migração das outras rotas de `X-User-Id` para JWT pertence à autenticação/autorização. Restringir ou descontinuar `POST /api/v1/users` também exige outra alteração coordenada com seus consumidores. Essas rotas não foram modificadas nesta US.

## Requisição

```json
{
  "name": "Nome Completo",
  "cpf": "123.456.789-09",
  "email": "aluno@exemplo.com",
  "phone": "(54) 99999-8888",
  "password": "senha-segura"
}
```

| Campo | Regra |
| --- | --- |
| `name` | Obrigatório; remove espaços externos; máximo de 255 caracteres após normalização. |
| `cpf` | Obrigatório; valida os dois dígitos verificadores e rejeita sequências repetidas; aceita máscara; grava somente os 11 dígitos. |
| `email` | Obrigatório; remove espaços externos e converte para minúsculas com `Locale.ROOT`; usa a mesma validação `@Email` do DTO; máximo de 255 caracteres. |
| `phone` | Opcional; aceita telefone brasileiro com ou sem máscara; grava somente dígitos; entrada limitada a 255 caracteres. |
| `password` | Obrigatória e não pode conter somente espaços; mínimo de `CreateUserUseCase.MIN_PASSWORD_LENGTH` (atualmente 8). A senha é preservada como enviada e persistida somente como hash. |

O cliente não escolhe papel, perfil nem status. O novo usuário recebe `STUDENT` e status `ACTIVE`.

## Decisão: máscara de telefone

O cadastro de aluno usa a mesma regra do cadastro de empresa: telefone brasileiro com 10 ou 11 dígitos. Máscaras são aceitas na entrada e o banco recebe apenas os dígitos.

- `null`, campo ausente ou somente espaços resultam em `null` no banco.
- Parênteses, espaços e hífens são removidos na normalização.
- Valores com menos de 10 ou mais de 11 dígitos retornam `400` com `Invalid phone`.
- Suporte a telefone internacional, DDI e ramal deve ser tratado em uma task própria que altere os dois cadastros juntos.

| Entrada | Armazenamento |
| --- | --- |
| `(54) 99999-8888` | `54999998888` |
| `(51) 3333-4444` | `5133334444` |
| `""` ou `null` | `null` |

A coluna `regular_users.phone` já é `VARCHAR(255)`, portanto não é necessária migration. A normalização vale para novos cadastros; registros antigos não são reescritos. O telefone não é incluído na resposta de autenticação, preservando o contrato do login.

## Resposta e erros

Sucesso: `201 Created`, com o mesmo envelope e campos do login:

```json
{
  "success": true,
  "data": {
    "accessToken": "<jwt-assinado>",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": "<uuid>",
      "name": "Nome Completo",
      "email": "aluno@exemplo.com",
      "profile": "STUDENT"
    }
  },
  "message": "Student registered successfully"
}
```

`expiresIn` depende da configuração do JWT; 3600 é o exemplo com expiração de 60 minutos. Senha, hash, CPF e telefone não são expostos nessa resposta.

| Status | Situação / mensagem |
| --- | --- |
| `400` | Campo obrigatório ausente, CPF/e-mail/telefone inválido, senha curta ou campo acima do limite. A mensagem identifica o campo. |
| `409` | `email already registered` ou `cpf already registered`. |
| `500` | Falha inesperada, inclusive na geração do token; não confirma o cadastro. |

As respostas de erro usam `ApiError`, como o restante da API. O OpenAPI documenta explicitamente `201` e `409`; `400` e `500` são acrescentados pela configuração existente.

## Implementação

- `RegisterStudentRequest` normaliza nome/e-mail antes do Bean Validation; o caso de uso também valida chamadas diretas.
- `ContactValidatorPort` mantém a validação compartilhada de e-mail e telefone fora de `application` e `domain`. O adaptador aplica `@Email`, sem uma segunda expressão regular divergente no caso de uso.
- `RegularUserRepositoryPort` persiste uma `RegularUserEntity` com herança JPA `JOINED`, gerando as linhas de `users` e `regular_users`.
- `UseCaseTransactionConfig` envolve o bean `RegisterStudentUseCase`: gravação e geração do token participam da mesma transação de escrita. Erros propagados revertem as gravações.
- A verificação antecipada de duplicidade melhora a resposta usual; as constraints de banco garantem unicidade em concorrência.
- O adaptador identifica SQLSTATE `23505` e os nomes exatos `users_email_key` / `regular_users_cpf_key`. Não procura nomes de campos no texto do erro, evitando confundir um e-mail como `cpf@exemplo.com` com CPF duplicado.
- `LoginResponse.from(...)` é compartilhado com o login. Os dados dos fakes de usuário e pessoa física também são compartilhados, permitindo testar cadastro seguido de login.

## Cobertura de testes

- Caso de uso: sucesso, normalização, preservação da senha, campos ausentes/inválidos, limites, telefone opcional e conflitos após cadastros reais nos fakes.
- Controller: acesso público, `201`, contrato de resposta, ausência do hash, normalização, login posterior, `400` por campo e `409`.
- Contatos: coerência da validação de e-mail com o DTO; telefones brasileiros com máscara, entrada opcional e rejeição de comprimentos inválidos.
- Persistência: as duas tabelas, conflitos de e-mail/CPF e rollback da tabela principal quando a gravação do perfil falha.
- Integração do fluxo: cadastro HTTP com PostgreSQL e JWT real, login posterior, rollback quando a geração do token falha e dois cadastros simultâneos disputando e-mail ou CPF. A barreira de teste faz ambas as requisições ultrapassarem a consulta prévia antes de tentar gravar.
- Regressão: uma conta cadastrada mantém CPF e telefone ao atualizar avatar; apenas o armazenamento externo de arquivo é substituído pelo fake nessa verificação.
- Configuração: proxy transacional de escrita e documentação OpenAPI do cadastro.

Executar os testes unitários e de controller, sem Docker:

```shell
mvn "-Dtest=RegisterStudentUseCaseTest,RegisterStudentControllerTest,RegistrationContactValidatorTest,RegularUserConflictTranslationTest,LoginUseCaseTest,AuthControllerTest" test
```

Executar integração, com Docker em execução:

```shell
mvn "-Dtest=StudentRegistrationIntegrationTest,RegularUserRepositoryAdapterTest,UseCaseTransactionConfigTest" test
```

Verificação completa:

```shell
mvn checkstyle:check
mvn clean verify
```

### Verificação desta alteração — 16/10/2026

- `git diff --check`: aprovado.
- Análise sintática dos 14 arquivos Java alterados/adicionados com gramática Java 21: sem erros. Essa análise não substitui a compilação com dependências nem os testes.
- `pom.xml`: XML válido.
- Maven: a tentativa de executar Checkstyle e os testes foi interrompida antes dessas etapas por timeout ao baixar `spring-boot-starter-parent:3.5.5` do Maven Central.
- Docker indisponível no ambiente de revisão; os testes com PostgreSQL ainda precisam ser executados em outro ambiente ou na CI.
