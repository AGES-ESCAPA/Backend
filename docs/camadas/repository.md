# Repository

> Parte de [docs/camadas/README.md](README.md), que mostra como as camadas se conversam.

Pasta: `<feature>/repository/`. Exemplos: `user/repository/UserRepository`, `course/catalog/repository/CourseCatalogRepository`, `course/management/repository/CourseRepository`.

## Objetivo da camada

Ser a única forma de ler e gravar no banco. Esconde SQL, JPQL e detalhes do Hibernate do resto da feature.

## Por que é necessária

Consulta ao banco é o ponto mais provável de bug sutil (N+1, ordenação instável, parâmetro nulo) e de otimização. Concentrar isso numa interface deixa o service legível e permite testar a consulta contra o Postgres real, isolada da regra.

## Por que foi implementada dessa forma

```java
public interface CourseCatalogRepository extends Repository<CourseEntity, UUID> {

    default Page<CourseCardResponse> findPublished(
            String titlePattern, String category, String level, Pageable pageable) {
        return findByStatus(CourseStatus.PUBLISHED, titlePattern, category, level, pageable);
    }

    @Query(value = """
            SELECT new com.escapa.backend.course.catalog.dto.CourseCardResponse(
                c.id, c.title, c.shortDescription, c.category, c.level, c.durationTime,
                c.lessonsCount, c.price, c.thumbnailUrl, i.name, c.ratingAverage, c.reviewsCount)
            FROM CourseEntity c
            LEFT JOIN c.instructor i
            WHERE c.status = :status
              AND LOWER(c.title) LIKE :titlePattern ESCAPE '!'
              AND (:category = '' OR LOWER(c.category) = :category)
              AND (:level = '' OR LOWER(c.level) = :level)
            ORDER BY c.createdAt DESC, c.id ASC
            """, countQuery = "...")
    Page<CourseCardResponse> findByStatus(...);
}
```

- **Interface Spring Data, sem implementação.** O Spring gera o código. Menos classe para manter, sem `EntityManager` espalhado.
- **`SELECT new ...CourseCardResponse(...)`: projeção direta no DTO.** A consulta já devolve o objeto que o controller vai serializar. Não carrega entidade, não precisa de mapper, não há três cópias do mesmo objeto. Foi isso que eliminou `CourseSummary` e `CourseMapper` da versão anterior.
- **`Repository<T, ID>` (marcador) em vez de `JpaRepository` para leitura.** A vitrine nunca grava. Se estendesse `JpaRepository`, `save` e `delete` estariam disponíveis para qualquer um que injetasse o repositório. Quem grava curso é `course/management/repository/CourseRepository`, que estende `JpaRepository`. Dois repositórios sobre a mesma entidade, cada um com o que sua subfeature precisa.
- **`ORDER BY c.createdAt DESC, c.id ASC` fixo.** Sem ordenação, o Postgres não garante ordem estável entre páginas e a vitrine podia repetir ou pular cursos. O desempate por `id` cobre dois cursos criados no mesmo instante.
- **`LOWER(...)` dos dois lados e `ESCAPE '!'`.** Busca sem diferenciar caixa e `%`, `_` do usuário tratados como texto. O service escapa antes de chamar.
- **Sentinelas `''` e `'%'` em vez de `IS NULL`.** Parâmetro nulo em JPQL vira `bytea` no Postgres e a consulta quebra. O service garante que nunca chega nulo.
- **`countQuery` separada, sem join.** A contagem para `totalElements` não precisa do instrutor.

## Observações

- `UserRepository` estende `JpaRepository` porque a feature grava. Só declara `existsByEmail`; o resto vem herdado.
- Repositório não tem `@Transactional`. A transação é do service. Se o repository abrisse a própria transação, uma operação com dois `save` não seria atômica.
- Teste: `@DataJpaTest` estendendo `common.JpaIntegrationTest`, contra o Postgres do Testcontainers com as migrations do Flyway aplicadas. Roda dentro de transação desfeita ao final. Para ver efeito de trigger, `flush()` e `clear()` antes de reler.

## Com quem conversa

Recebe de: service da própria feature. Fala com: banco, via Hibernate. Devolve: entity ou DTO projetado. Nunca é chamado por controller nem por outra feature.

---

← Anterior: [Service](service.md) · Próxima: [Entity](entity.md) →
