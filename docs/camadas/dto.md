# DTO

> Parte de [docs/CAMADAS.md](../CAMADAS.md), que mostra como as camadas se conversam.

Pasta: `<feature>/dto/`. Exemplos: `user/dto/CreateUserRequest`, `user/dto/UserResponse`, `course/catalog/dto/CourseCardResponse`.

## Objetivo da camada

Definir o contrato JSON da API: o que entra e o que sai. É o que o frontend vê.

## Por que é necessária

O JSON da API e a tabela do banco mudam por motivos diferentes. `UserResponse` não tem `passwordHash`; `CourseCardResponse` tem `instructor` como nome, não como objeto. Se o controller devolvesse a entidade direto, qualquer coluna nova viraria campo público e o hash da senha sairia no JSON.

## Por que foi implementada dessa forma

```java
public record CreateUserRequest(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Email is required") @Email(message = "Invalid email") String email,
        @NotBlank(message = "Password is required")
        @Size(min = MIN_PASSWORD_LENGTH, message = "Password must be at least 8 characters")
        String password,
        @NotBlank(message = "User type is required") String userType
) {
    public static final int MIN_PASSWORD_LENGTH = 8;
}

public record UserResponse(UUID id, String name, String email, String userType, LocalDateTime createdAt) {
    public static UserResponse from(UserEntity user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
```

- **`record`.** Imutável, sem getter e setter à mão, `equals` e `toString` de graça. Um DTO não tem motivo para mudar depois de criado.
- **Validação de formato na entrada, via Bean Validation.** `@NotBlank`, `@Email`, `@Size` com mensagem. O Spring valida antes do controller e o handler devolve 400 com todas as mensagens. O service não repete essas checagens.
- **`from(entity)` na saída.** A conversão fica no DTO, ao lado dos campos que ela preenche. O controller só chama `UserResponse.from(...)`. Se um campo novo entrar no JSON, muda um arquivo.
- **Nomes do JSON são contrato.** `userType` no JSON, embora a coluna seja `role`, porque o frontend já consome assim. `instructor` como string porque a US pediu assim.
- **`CourseCardResponse` é preenchido pela consulta**, via `SELECT new`. Por isso a ordem e os tipos dos componentes precisam bater com a projeção. É um DTO de saída que nunca passa por `from`.

## Observações

- DTO de entrada não vira entidade sozinho. O service monta a entidade a partir do DTO, porque é o service que gera id, hash e normaliza.
- DTO não tem teste próprio. O teste de controller confere os nomes dos campos do JSON; o de repository confere a projeção.
- `CourseCardResponse` mora em `catalog/dto`, não em `shared`, porque só a vitrine o usa. Se `management` precisar de outra visão de curso, cria o próprio DTO.

## Com quem conversa

Entrada: chega ao controller já validada pelo Bean Validation e segue para o service. Saída: montada por `from(entity)` no controller, ou direto pela consulta via `SELECT new`. Nunca toca o banco.

---

← Anterior: [Entity](entity.md) · Próxima: [Exception](exception.md) →
