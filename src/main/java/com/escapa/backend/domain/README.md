# domain — o coração do negócio

Esta pasta representa **o que a Escapa! é como negócio**, sem depender de Spring, banco de dados, HTTP ou qualquer outra tecnologia. Se você apagasse todo o resto do projeto (API, banco, Docker) e ficasse só com essa pasta, as regras continuariam fazendo sentido sozinhas — como as regras de um jogo escritas num papel, que valem independente da mesa onde o jogo é jogado.

## O que vai aqui
- **Entidades**: classes que representam conceitos do negócio (ex.: um usuário, futuramente um curso, um módulo).
- **Exceções de domínio**: erros que fazem sentido pro negócio, não pra tecnologia (ex.: "esse usuário não existe").
- **Regras de negócio** que pertencem diretamente a esses conceitos.

## O que NUNCA vai aqui
- Anotações do Spring (`@Service`, `@Controller`, `@Component`).
- Anotações do JPA (`@Entity`, `@Column`, `@Table`).
- Qualquer import de banco de dados, HTTP ou biblioteca externa de infraestrutura.
- Lombok é uma exceção.

## Por que essa regra existe
Se um dia trocarmos o Spring Boot por outro framework, ou o PostgreSQL por outro banco, **essa pasta não deveria precisar mudar** — ela simplesmente não sabe que esses detalhes existem.

## O que já temos
- `entity/User.java` — um usuário da plataforma (id, nome, email, hash da senha, tipo, data de criação). O `id` é um `java.util.UUID`, igual em todas as entidades: todas as PKs do banco são do tipo `uuid` nativo do Postgres. O campo é `passwordHash`, não `password`: a senha em texto puro nunca entra no domínio — o `CreateUserUseCase` já a converte via `PasswordHasherPort`.
- `entity/Admin.java`, `entity/Company.java` — especializações de `User`.
- `entity/Course.java`, `entity/Module.java`, `entity/Content.java` — a estrutura de um curso.
- `entity/UserCourse.java`, `entity/CompanyCourse.java`, `entity/UsersCompany.java` — matrículas e vínculos.
- `user/UserNotFoundException.java` — erro de domínio disparado quando um usuário buscado não existe.

## Quem depende de quem
`domain` não depende de **nada** dentro do projeto. Todas as outras camadas (`application`, `adapters`, `infrastructure`) é que dependem dela — nunca o contrário.
