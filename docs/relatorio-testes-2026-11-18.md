# Relatório de testes — Escapa! Backend

**Data da execução:** 18/11/2026  
**Escopo da execução:** suíte padrão do Maven e testes PDF com Chromium  
**Resultado consolidado:** 1.232 execuções de teste, sem falhas ou erros

## Resumo executivo

- **175 classes/suítes** reportadas pelo Surefire após agregar as execuções.
- **1.232 execuções**, incluindo casos parametrizados: **0 falhas, 0 erros, 0 ignorados**.
- **Cobertura agregada de linhas: 96,63%** (5.015 de 5.190).
- **Cobertura agregada de branches: 90,83%** (1.396 de 1.537).
- Checkstyle: **0 violações**.
- `mvn clean verify` executou a suíte padrão: **1.225 testes**, sem falhas ou erros. A cobertura
  dessa execução limpa foi de **95,82% de linhas** e **90,05% de branches**.
- Os **7 testes PDF** foram executados separadamente e agregados ao relatório JaCoCo.

## Comandos executados

| Comando | Resultado | Observação |
|---|---|---|
| `mvn clean verify` | **Sucesso** | Build limpo, Checkstyle sem violações, 1.225 testes padrão sem falhas/erros e relatório JaCoCo. |
| `mvn test "-Dsurefire.excludedGroups=" "-Dgroups=pdf"` | **Sucesso** | Executou os 7 testes `@Tag("pdf")` com Chromium 153.0.8010.12; JaCoCo agregou os dados da execução limpa e dos testes PDF. |

O relatório final foi gerado depois da execução padrão limpa e da execução PDF. No Windows, o
perfil `windows-clean-generated-output` limpa o atributo somente leitura dos artefatos de `target`
antes da fase `clean`.

## Cobertura JaCoCo

O relatório JaCoCo é gerado em `target/site/jacoco/index.html` ao rodar `mvn test` ou `mvn verify`. Esse caminho é artefato de build e não faz parte do repositório.

| Métrica | Cobertos / total | Cobertura |
|---|---:|---:|
| Instruções | 23.292 / 24.207 | **96,22%** |
| Linhas | 5.015 / 5.190 | **96,63%** |
| Branches | 1.396 / 1.537 | **90,83%** |
| Métodos | 1.305 / 1.370 | **95,26%** |
| Complexidade | 1.939 / 2.140 | **90,61%** |

**Leitura para apresentação:** mais de 96% das linhas e mais de 90% dos caminhos condicionais
analisados foram exercitados. Branches medem os caminhos alternativos de decisões (`if`, ternários
e condições compostas); por isso, cobrir uma linha não significa necessariamente cobrir todos os
seus resultados.

O relatório mede as classes de produção analisadas pelo JaCoCo. A suíte padrão exclui os testes
PDF; execute também o comando PDF acima para agregar sua execução. `jacoco:check` falha o build
(`haltOnFailure`) se a cobertura de linhas cair abaixo de 95% ou a de branches abaixo de 90%.

## Tipos de teste existentes

| Grupo | Suítes | Execuções | O que verifica |
|---|---:|---:|---|
| Casos de uso e domínio | 89 | 621 | Regras e transições de negócio, validações, resultados e efeitos observáveis. Casos de uso reutilizam fakes `InMemory*Port`. |
| API: controllers, DTOs e exceções | 36 | 296 | Contratos HTTP, serialização, validação, autorização e tratamento global de erros; inclui testes com MockMvc. |
| Persistência | 35 | 217 | Adaptadores JPA, consultas, schema e migrations em PostgreSQL via Testcontainers compartilhado. |
| Segurança | 4 | 41 | JWT, autenticação/autorização e cenários de acesso protegido. |
| Configuração, e-mail e validação de infraestrutura | 8 | 43 | Transações, métricas/OpenAPI, envio/templates de e-mail e validação de contatos. |
| Armazenamento | 2 | 7 | URLs públicas e assinatura de requisições para MinIO/Garage, com cliente HTTP local de teste. |
| PDF com Chromium | 1 | 7 | Geração e leitura dos PDFs do certificado nos temas claro e escuro, com Playwright e PDFBox. |
| **Total** | **175** | **1.232** | **0 falhas, 0 erros, 0 ignorados** |

### Distribuição por pacote

| Pacote de teste | Suítes | Execuções |
|---|---:|---:|
| `adapters.controller` | 28 | 275 |
| `adapters.dto` e `adapters.dto.course` | 6 | 14 |
| `adapters.exception` | 2 | 7 |
| `application.course` e `application.dto` | 2 | 5 |
| `application.usecase` | 78 | 578 |
| `domain.*` | 9 | 38 |
| `infrastructure.config` | 4 | 8 |
| `infrastructure.email` | 3 | 11 |
| `infrastructure.persistence` (inclui `course`, `entity`) | 35 | 217 |
| `infrastructure.security` | 4 | 41 |
| `infrastructure.storage` | 2 | 7 |
| `infrastructure.validation` | 1 | 24 |
| `infrastructure.pdf` | 1 | 7 |

## Ferramentas e recursos de teste

- **JUnit 5** para testes unitários, parametrizados e de integração.
- **Fakes em memória `InMemory*Port`** para testar casos de uso sem mocks de persistência.
- **Spring Test / MockMvc** para contratos dos controllers.
- **Testcontainers com PostgreSQL** para persistência e schema.
- **JaCoCo** para instruções, linhas, branches, métodos e complexidade.
- **Playwright/Chromium** para renderização PDF; **PDFBox** para conferir os arquivos gerados.
- **Checkstyle** executado no ciclo Maven: nenhuma violação na execução principal.

Os testes PDF geram arquivos de pré-visualização em `target/certificate-preview/`, incluindo PDFs
e imagens PNG nos temas claro e escuro.

## Pontos para apresentação e acompanhamento

1. As metas documentadas de cobertura foram atingidas: **96,63% de linhas** e **90,83% de branches**.
2. A suíte cobre regras de negócio, API, persistência real, segurança, infraestrutura e geração de certificados.
3. A validação completa foi feita com `mvn clean verify`; os testes PDF foram executados separadamente e agregados às métricas.
4. A configuração JaCoCo publica as métricas, mas não aplica as metas como limite bloqueante do build.
