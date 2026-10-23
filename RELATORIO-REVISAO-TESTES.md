# Relatório da revisão da suíte de testes

## Resumo

Foi feita uma revisão da suíte de testes do backend Escapa! para fortalecer os
asserts, adicionar cenários de borda e erro, ampliar a cobertura e documentar
como executar e manter os testes.

| Indicador | Resultado |
| --- | ---: |
| Testes executados na suíte regular | 518 |
| Testes marcados para PDF | 7 |
| Execuções registradas nos relatórios Surefire após as duas execuções | 525 |
| Classes de teste alteradas | 17 |
| Novas classes de teste | 15 |
| Testes removidos por duplicidade/falso positivo | 0 |
| Cobertura regular de branches | 90,70% (673 de 742) |
| Cobertura regular de linhas | 92,71% (2.418 de 2.608) |
| Cobertura combinada de branches, incluindo PDF | 91,37% (678 de 742) |
| Cobertura combinada de linhas, incluindo PDF | 93,67% (2.443 de 2.608) |

**Como interpretar as contagens:** o Surefire conta cada invocação, incluindo
cada caso de um teste parametrizado. Portanto, 525 é o total de execuções
registradas — 518 da suíte regular mais 7 testes PDF — e não o número de
métodos `@Test` únicos. Nenhum teste foi removido: não foi encontrado caso
comprovadamente redundante; testes semelhantes em camadas diferentes foram
mantidos quando verificavam contratos distintos. Testes existentes foram
corrigidos ou ampliados quando necessário.

## O que foi revisado e alterado

- **Falsos positivos e qualidade dos asserts:** os testes passaram a verificar
  resultados observáveis e valores relevantes, não apenas que um objeto existe.
  O teste de arquivamento, por exemplo, agora verifica o estado arquivado.
- **Atualização de curso:** foram adicionados casos parametrizados para verificar
  que mudanças em cada campo relevante de um curso publicado incrementam a
  versão e registram o histórico. Também foram cobertos título ausente/em branco
  e alteração sem autor.
- **Validações de entrada e regras de domínio:** foram acrescentados cenários para
  valores nulos, em branco, inválidos e limites de senha, telefone, conteúdo e
  publicação de curso.
- **Controllers:** foi ampliada a cobertura HTTP com `MockMvc`, incluindo acesso,
  validação, formato das respostas e publicação com ou sem notificação.
- **Autenticação JWT:** foram atualizados os testes de avatar e OpenAPI para o
  contrato `Authorization: Bearer`, e cobertos principal ausente/inválido,
  cabeçalho sem token e documentação de acesso restrito versus público.
- **Persistência e mapeamento:** foram adicionados testes para conversores e
  adaptadores, consulta de histórico, estados de matrícula, dados opcionais,
  atualização de curso e preservação de associações/contadores.
- **PDF e infraestrutura:** foram cobertos os assets do certificado, a geração
  de HTML/PDF e caminhos de configuração OpenAPI e armazenamento.
- **Cobertura:** `mvn clean verify` agora aplica um limite JaCoCo mínimo de 90%
  tanto para branches quanto para linhas. A cobertura combinada inclui também
  os testes PDF executados separadamente.
- **Documentação:** `AGENTS.md` e `README.md` foram atualizados com convenções
  para testes, prevenção de falsos positivos e comandos de execução/cobertura.

### Novas classes de teste

- `AvatarControllerTest`
- `HealthControllerTest`
- `ContentTypeRulesTest`
- `ModuleTest`
- `OpenApiConfigTest`
- `CertificateAssetsTest`
- `PlaywrightCertificatePdfGeneratorUnitTest`
- `CompanyMapperTest`
- `ContentMapperTest`
- `CourseChangeLogRepositoryAdapterTest`
- `CoursePrerequisiteRepositoryAdapterTest`
- `ModuleMapperTest`
- `UserMapperTest`
- `CourseMapperTest`
- `MinioFileStorageAdapterTest`

## Classes de teste existentes ampliadas ou corrigidas

- `AdminCourseControllerTest`
- `ArchiveCourseUseCaseTest`
- `ChangePasswordUseCaseTest`
- `CreateContentUseCaseTest`
- `CreateUserUseCaseTest`
- `LessonStatusCalculatorTest`
- `PublishCourseUseCaseTest`
- `RegisterCompanyUseCaseTest`
- `UpdateContentUseCaseTest`
- `UpdateCourseUseCaseTest`
- `UpdateProgressRulesUseCaseTest`
- `UseCaseTransactionConfigTest`
- `PlaywrightCertificatePdfGeneratorTest`
- `CourseRepositoryAdapterTest`
- `ModuleRepositoryAdapterTest`
- `StudentLessonAdaptersTest`
- `TokenSecurityIntegrationTest`

## Tipos de teste presentes

- **Unitários/de domínio e casos de uso:** exercitam regras puras e fluxos da
  aplicação sem subir o contexto Spring. Os casos de uso usam fakes
  `InMemory*Port`.
- **Testes de controller (web/API):** usam MockMvc standalone para verificar
  requisições, status HTTP, acesso, validação e respostas, sem depender do
  backend real.
- **Testes de integração de persistência:** exercitam repositórios, adaptadores,
  mapeamento JPA e schema contra PostgreSQL via Testcontainers. Reutilizam o
  container compartilhado por `PostgresIntegrationTest`.
- **Testes de mapeadores e configuração:** verificam conversões entre modelos e
  entidades, configurações transacionais e documentação OpenAPI.
- **Testes de PDF:** exercitam a geração real do certificado com Chromium /
  Playwright e validação do documento gerado. São separados da suíte regular.

## Como executar os testes

### Suíte regular e verificação completa

Com Docker Desktop disponível, no PowerShell do Windows:

```powershell
$env:DOCKER_HOST = 'npipe:////./pipe/dockerDesktopLinuxEngine'
mvn clean verify
```

`mvn clean verify` limpa artefatos anteriores, verifica o Checkstyle, executa os
testes unitários e de integração e gera o relatório JaCoCo. Os testes de
persistência usam Testcontainers e precisam do Docker em execução.

### Executar uma classe específica

```powershell
mvn -Dtest=CreateUserUseCaseTest test
```

Substitua o nome pelo teste que deseja executar. O filtro executa somente a
classe selecionada.

### Testes PDF e cobertura combinada

Os testes PDF usam Chromium e são excluídos da execução regular. Para executar
esses testes e atualizar o relatório combinado, sem rodar `clean` entre os
comandos:

```powershell
mvn test '-Dsurefire.excludedGroups=' -Dgroups=pdf
mvn jacoco:report
```

O comando PDF executa os testes marcados com `@Tag("pdf")`. A geração do
relatório após essa execução combina os dados guardados em `target/jacoco.exec`.

## Resultados de validação

- `mvn clean verify`: **BUILD SUCCESS**; 518 testes, 0 falhas, 0 erros, 0
  ignorados; 90,70% de branches e 92,71% de linhas na suíte regular.
- `mvn test '-Dsurefire.excludedGroups=' -Dgroups=pdf`: **BUILD SUCCESS**; 7
  testes PDF, 0 falhas, 0 erros, 0 ignorados.
- `mvn jacoco:report`: **BUILD SUCCESS**; relatório combinado atualizado.
- `git diff --check` e `git diff --cached --check`: sem erros de whitespace.

O relatório HTML atual está em `target/site/jacoco/index.html`.
