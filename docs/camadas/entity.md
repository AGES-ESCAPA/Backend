# Entity

> Parte de [docs/CAMADAS.md](../CAMADAS.md), que mostra como as camadas se conversam.

Pasta: `<feature>/entity/`, ou `<feature>/shared/entity/` quando a feature tem subfeatures. Exemplos: `user/entity/UserEntity`, `course/shared/entity/CourseEntity`.

## Objetivo da camada

Ser o espelho Java de uma tabela. Diz ao Hibernate qual classe corresponde a qual tabela, qual campo a qual coluna, e como as tabelas se relacionam.

## Por que é necessária

O Hibernate precisa de um mapeamento para montar SQL, e o `ddl-auto=validate` usa esse mapeamento para conferir na subida que o schema do Flyway e o código concordam. Sem entidade não há JPA.

## Por que foi implementada dessa forma

```java
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
public class UserEntity {

    @Id
    @Column(nullable = false, unique = true)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;
}
```

- **Sufixo `Entity`.** Deixa claro no import que é classe de persistência, não DTO nem regra.
- **Só mapeamento, sem lógica.** Regra fica no service. Se a entidade tivesse método `publicar()`, a regra ficaria escondida em uma classe que o service manipula por setter.
- **Anotações do banco espelham a migration** (`nullable`, `unique`, nome da coluna). Não criam schema, só validam. Se divergirem, a aplicação não sobe, o que é bom: erro na subida é mais barato que erro em produção.
- **Sem entidade de domínio separada.** A versão anterior tinha `User` (domínio) e `UserEntity` (JPA), com mapper entre eles e sem nenhuma regra em `User`. Duas classes para o mesmo dado, divergindo entre si. Agora a entidade JPA é o modelo.
- **Entidade pode referenciar entidade de outra feature.** `UserCourseEntity` (enrollment) aponta para `UserEntity` (user) e `CourseEntity` (course). É o mapeamento do banco; esconder isso criaria mais problema do que resolve.
- **Enums de coluna ficam junto** (`UserStatus`, `CourseStatus`, `ContentType`) e são gravados como `STRING`, batendo com os `CHECK` das migrations.

## Observações

- Entidades de curso ficam em `course/shared/entity` porque `catalog`, `management` e `review` usam as mesmas tabelas.
- `CourseEntity` implementa `Persistable` com `isNew()` baseado em `id == null`, porque o id é gerado pelo Hibernate. `UserEntity` tem id atribuído pelo service; o Spring Data então usa `merge`, que funciona igual para inserção.
- Entidade não tem teste próprio. O que se testa é comportamento do banco ligado a ela: o trigger de `lessons_count` está em `course/shared/entity/LessonsCountTriggerTest`.
- Mudou coluna ou constraint na entidade: precisa de migration nova. Nunca editar migration já aplicada.

## Com quem conversa

Manipulada por: service (cria, altera) e repository (lê, grava). Referencia: entities de qualquer feature, porque é o mapeamento do banco. Nunca chega ao controller nem ao JSON.

---

← Anterior: [Repository](repository.md) · Próxima: [DTO](dto.md) →
