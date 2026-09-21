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
*N/A ou informe a migration criada (ex.: `V2__add_password_to_users.sql`)*

---

## 📸 Evidências (Se aplicável)

<!-- Prints do Swagger, respostas de requisições ou saída dos testes -->
*N/A ou Cole os prints aqui*

---

## ✅ Checklist do Desenvolvedor (Antes de solicitar revisão)

Marque com `[x]` os itens validados:
- [ ] O código respeita a Clean Architecture (`domain` sem Spring/JPA, `application` dependendo apenas do domínio).
- [ ] Não há configuração hardcoded (valores vêm de variáveis de ambiente em `application.properties`).
- [ ] Foi executado `mvn checkstyle:check` e não há violações.
- [ ] Foi executado `mvn clean verify` **com o Docker rodando** e todos os testes passaram.
- [ ] Todo caso de uso novo tem teste unitário correspondente.
- [ ] Se alguma entidade JPA mudou, a migration Flyway correspondente foi criada.
- [ ] A aplicação sobe com `docker compose up --build` sem erros.
- [ ] O Pull Request está apontando para a branch **`develop`**.

---

## 👥 Checklist do Revisor (Reviewer)

- [ ] Código limpo e de fácil compreensão.
- [ ] Separação clara de responsabilidades (`domain`, `application`, `adapters`, `infrastructure`).
- [ ] Testes cobrem os cenários principais e os casos de erro.
- [ ] Migrations são compatíveis com as entidades e não alteram arquivos `V*` já aplicados.
