# Service

> Parte de [docs/camadas/README.md](README.md), que mostra como as camadas se conversam.

Pasta: `<feature>/service/`. Exemplos: `user/service/UserService`, `course/catalog/service/CourseCatalogService`.

## Objetivo da camada

Concentrar toda decisão de negócio da feature. É a única camada que responde "pode ou não pode" e "o que acontece depois".

## Por que é necessária

Regra de negócio precisa de um lugar único, testável sem HTTP e sem banco. Se ficasse no controller, não seria reutilizável por outra feature. Se ficasse no repository, dependeria de SQL para ser testada. O service é o meio-termo: Java puro que orquestra repositórios.

## Por que foi implementada dessa forma

```java
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserEntity create(CreateUserRequest request) {
        final String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException(email);
        }
        final UserEntity user = new UserEntity(UUID.randomUUID(), request.name().trim(), email,
                passwordEncoder.encode(request.password()),
                request.userType().trim().toUpperCase(Locale.ROOT), LocalDateTime.now());
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserEntity getById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }
}
```

- **`@Service`** em vez de fiação manual de beans. O Spring injeta tudo pelo construtor. A versão anterior exigia um método `@Bean` por caso de uso numa classe de configuração, e isso impedia `@Transactional` no lugar certo.
- **`@Transactional` só aqui.** A transação abre no início do método do service e fecha no fim. Tudo que o método faz é atômico. `readOnly = true` em leitura deixa o Hibernate pular verificações de alteração e o Postgres otimizar.
- **Verifica antes de gravar.** `existsByEmail` antes de `save`. A constraint `UNIQUE` do banco continua lá como rede de segurança para condição de corrida, mas a regra principal é explícita no Java, com exceção nomeada e mensagem clara.
- **Lança exceção da feature, nunca `IllegalArgumentException`.** `EmailAlreadyUsedException` herda de `ConflictException`, então o handler sabe que é 409 sem o service saber o que é HTTP.
- **Recebe o DTO de entrada e devolve entidade.** O controller converte a entidade em DTO de saída. Assim o service pode ser chamado por outro service que precise da entidade completa.
- **Normalização mora aqui.** Trim, minúsculas, valores padrão de paginação. O `CourseCatalogService` faz só isso, porque a vitrine não tem regra que falhe: filtro sem resultado é página vazia com 200, não erro.

## Observações

- Service de uma feature chama service de outra, nunca repository de outra. Se `EnrollmentService` precisar de um curso, chama `CourseCatalogService`.
- Não captura `DataAccessException`. Falha de banco sobe para o handler, que devolve 500 genérico e registra o stack trace. Capturar aqui só esconderia a causa.
- Teste: unitário com Mockito, sem Spring. Repositório e encoder são mocks; o teste confere a regra e exatamente o que chega ao repositório. Roda em milissegundos, sem Docker.

## Com quem conversa

Recebe de: controller da própria feature ou service de outra feature. Chama: repositories da própria feature, `PasswordEncoder` e outros beans de `common/config`, services de outras features. Lança: exceções de `<feature>/exception`. Nunca fala com repository de outra feature nem com HTTP.

---

← Anterior: [Controller](controller.md) · Próxima: [Repository](repository.md) →
