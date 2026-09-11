## 📌 Descrição da Tarefa

- **ID do ClickUp:** [Inserir ID ou link da tarefa no ClickUp]
- **Tipo de Alteração:**
  - [ ] `feat`: Nova funcionalidade
  - [ ] `fix`: Correção de bug
  - [ ] `refactor`: Refatoração de código
  - [ ] `test`: Adição / alteração de testes
  - [ ] `docs`: Atualização de documentação
  - [ ] `chore`: Configurações, dependências ou infraestrutura

---

## 📝 O que foi feito?

<!-- Descreva de forma objetiva o que foi implementado ou corrigido nesta branch -->
- 

---

## 🗄️ Alterações no banco de dados

<!-- O schema é versionado por Flyway e o Hibernate roda em validate: se a entidade JPA mudar sem migration, a aplicação não sobe -->
*N/A ou informe a migration criada (ex.: `V6__add_review_counters_trigger.sql`)*

---

## 📸 Evidências (Se aplicável)

<!-- Prints do Swagger, respostas de requisições ou saída dos testes -->
*N/A ou Cole os prints aqui*

---

## ✅ Checklist do Desenvolvedor (Antes de solicitar revisão)

Marque com `[x]` os itens validados:
- [ ] O código segue a estrutura por feature (`controller → service → repository → entity`), conforme `AGENTS.md`.
- [ ] Controller não importa repository nem entity; só o service tem `@Transactional`.
- [ ] Regra de negócio lança exceção da feature herdando de `NotFoundException`, `ConflictException` ou `BusinessRuleException`; nenhum `IllegalArgumentException` para negócio.
- [ ] Não há configuração hardcoded (valores vêm de variáveis de ambiente em `application.properties`).
- [ ] Foi executado `mvn -B clean verify` **com o Docker rodando** e Checkstyle e testes passaram.
- [ ] Service novo tem teste com Mockito; repository com consulta nova tem `@DataJpaTest`; controller novo tem teste MockMvc.
- [ ] Se alguma entidade JPA mudou coluna ou constraint, a migration Flyway correspondente foi criada.
- [ ] A aplicação sobe com `docker compose up --build` sem erros.
- [ ] O Pull Request está apontando para a branch **`develop`**.

---

## 👥 Checklist do Revisor (Reviewer)

- [ ] Código limpo e de fácil compreensão.
- [ ] Papéis respeitados dentro da feature e comunicação entre features só pelo service.
- [ ] Erros seguem as quatro famílias e devolvem `code` estável.
- [ ] Testes cobrem os cenários principais e os casos de erro.
- [ ] Migrations são compatíveis com as entidades e não alteram arquivos `V*` já aplicados.
