# US-06 · Organização dos Módulos — guia para o frontend

Como administrador, quero criar, editar, reordenar, remover e visualizar os módulos de um curso, para estruturar a divisão dos assuntos.

Este documento descreve a feature do ponto de vista de quem constrói a tela e o contrato HTTP que ela consome. O backend está na branch `feat/US-06-BE-04-crud-modulos`.

---

## 1. O que a tela faz

Um curso é dividido em **módulos**, e cada módulo contém **conteúdos** (aulas em vídeo, texto ou arquivo). A tela de organização mostra a lista de módulos de um curso, na ordem em que o aluno vai percorrê-los, e permite ao admin:

| Ação | O que acontece no backend |
|---|---|
| Ver a lista | Módulos ordenados por `order`, cada um com quantidade de conteúdos, duração total e os conteúdos resumidos |
| Criar módulo | Informa só o título. O módulo entra **sempre no fim** da lista; a posição é calculada pelo servidor |
| Renomear módulo | Altera só o título. A posição não muda |
| Reordenar (arrastar e soltar) | O front envia a **lista completa** de IDs na nova ordem. O servidor reatribui as posições 1..n |
| Remover módulo | Remove o módulo **e todos os seus conteúdos**. Sem confirmação no backend: a confirmação é responsabilidade da tela |

Regras que afetam a UI:

- **`order` é do servidor.** O front nunca envia posição, nem no create nem no update. A única forma de mudar posição é o endpoint de reorder.
- **Reorder é tudo ou nada.** A lista enviada precisa conter exatamente os IDs dos módulos do curso, sem faltar nem sobrar, sem repetição. Caso contrário o servidor responde `400` e não altera nada. Depois de um drag and drop, envie o array inteiro na ordem exibida.
- **Remover deixa lacuna no `order`.** Remover o módulo 2 de [1, 2, 3] resulta em [1, 3]. A listagem continua ordenada e o próximo módulo criado entra como 4. Se a tela exibir o número da posição, use o índice do array, não o campo `order`.
- **Remover cascateia.** Todos os conteúdos do módulo são apagados. Mostre na confirmação quantos conteúdos serão perdidos (`totalContents` já vem na listagem).
- **Só ADMIN.** Todos os endpoints exigem o header `X-User-Id` com o UUID de um usuário ADMIN. Ver seção 2.

---

## 2. Autenticação (estado atual)

O projeto ainda não tem login nem token. Enquanto isso, a identificação é feita pelo header:

```
X-User-Id: <uuid do usuário admin>
```

| Situação | Resposta |
|---|---|
| Header ausente | `403 Forbidden` · "Missing X-User-Id header" |
| Valor não é UUID | `403 Forbidden` · "Invalid X-User-Id: must be a valid UUID" |
| Usuário não existe | `403 Forbidden` · "User not found" |
| Usuário não é ADMIN | `403 Forbidden` · "Access denied: user is not ADMIN" |

Para desenvolvimento local com o seed (`mvn spring-boot:run`), há dois admins:

| Nome | UUID |
|---|---|
| Beatriz Nunes | `a0000000-0000-4000-a000-000000000001` |
| Rafael Antunes | `a0000000-0000-4000-a000-000000000002` |

Quando a autenticação por token entrar (Spring Security + JWT), este header será substituído por `Authorization: Bearer <token>`. Isole o envio do header em um único lugar do cliente HTTP para a troca ser trivial.

---

## 3. Convenções gerais

**Base URL local:** `http://localhost:8080/api/v1/admin`
**Swagger:** `http://localhost:8080/swagger-ui/index.html`

**Envelope de sucesso** (todas as respostas com corpo):

```json
{
  "success": true,
  "data": { ... },
  "message": "Module created successfully"
}
```

**Envelope de erro** (qualquer status 4xx/5xx):

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "moduleIds must contain every module of the course exactly once",
  "path": "/api/v1/admin/courses/e0000000-0000-4000-e000-000000000001/modules/reorder",
  "timestamp": "2026-09-13T18:42:11.123Z"
}
```

Erros de validação de Bean Validation (campo obrigatório etc.) chegam com `status: 400` e as mensagens dos campos concatenadas por vírgula em `message`.

Todos os IDs são UUID v4 em string. Datas em ISO-8601 UTC.

---

## 4. Tipos

```ts
type ContentType = "VIDEO" | "TEXT" | "FILE";

interface ContentSummary {
  id: string;
  title: string;
  type: ContentType;
  order: number;
}

interface Module {
  id: string;
  title: string;
  order: number;               // posição no curso; pode ter lacunas após remoções
  totalContents: number;       // = contents.length
  totalDurationMinutes: number; // soma de durationMinutes dos conteúdos; TEXT/FILE sem duração contam 0
  contents: ContentSummary[];  // já ordenados por order
}

interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string;
}

interface ApiError {
  status: number;
  error: string;
  message: string;
  path: string;
  timestamp: string;
}
```

---

## 5. Endpoints

### 5.1 Listar módulos do curso

```
GET /api/v1/admin/courses/{courseId}/modules
```

Retorna `ApiResponse<Module[]>`, ordenado por `order` crescente. Curso sem módulos retorna `data: []`.

**Resposta 200**

```json
{
  "success": true,
  "data": [
    {
      "id": "01000000-0000-4000-9000-000000000001",
      "title": "Fundamentos do Atendimento",
      "order": 1,
      "totalContents": 3,
      "totalDurationMinutes": 52,
      "contents": [
        { "id": "02000000-0000-4000-9000-000000000001", "title": "A jornada do hospede", "type": "VIDEO", "order": 1 },
        { "id": "02000000-0000-4000-9000-000000000002", "title": "Padroes de comunicacao", "type": "TEXT", "order": 2 },
        { "id": "02000000-0000-4000-9000-000000000003", "title": "Check-in sem atrito", "type": "VIDEO", "order": 3 }
      ]
    },
    {
      "id": "01000000-0000-4000-9000-000000000002",
      "title": "Situacoes Criticas",
      "order": 2,
      "totalContents": 2,
      "totalDurationMinutes": 26,
      "contents": [
        { "id": "02000000-0000-4000-9000-000000000004", "title": "Contornando uma reclamacao", "type": "VIDEO", "order": 1 },
        { "id": "02000000-0000-4000-9000-000000000005", "title": "Modelo de carta de desculpas", "type": "FILE", "order": 2 }
      ]
    }
  ],
  "message": "Operation completed successfully"
}
```

**Erros:** `403` (não ADMIN) · `404` (curso não existe)

---

### 5.2 Criar módulo

```
POST /api/v1/admin/courses/{courseId}/modules
Content-Type: application/json
```

**Request**

```json
{ "title": "Módulo 3: JavaScript na Prática" }
```

`title` obrigatório, não vazio. Espaços nas pontas são removidos. Não envie `order`; será ignorado se enviado.

**Resposta 201** · `ApiResponse<Module>`

```json
{
  "success": true,
  "data": {
    "id": "7c1a0f3e-9b2d-4e5f-8a6b-1c2d3e4f5a6b",
    "title": "Módulo 3: JavaScript na Prática",
    "order": 3,
    "totalContents": 0,
    "totalDurationMinutes": 0,
    "contents": []
  },
  "message": "Module created successfully"
}
```

**Erros:** `400` (title vazio: "title is required") · `403` · `404` (curso não existe)

Sugestão de UI: após o 201, faça append do `data` no fim da lista local, sem precisar refazer o GET.

---

### 5.3 Renomear módulo

```
PUT /api/v1/admin/modules/{id}
Content-Type: application/json
```

**Request**

```json
{ "title": "Módulo 1: Fundamentos da Web" }
```

**Resposta 200** · `ApiResponse<Module>` com o módulo atualizado. `order` e `contents` permanecem iguais.

**Erros:** `400` (title vazio) · `403` · `404` (módulo não existe)

---

### 5.4 Reordenar módulos

```
PUT /api/v1/admin/courses/{courseId}/modules/reorder
Content-Type: application/json
```

**Request**

```json
{
  "moduleIds": [
    "01000000-0000-4000-9000-000000000002",
    "01000000-0000-4000-9000-000000000001"
  ]
}
```

A posição no array vira o `order` (primeiro = 1). O array deve conter **todos** os módulos do curso, **uma vez cada**.

**Resposta 200** · `ApiResponse<Module[]>` com a lista completa já na nova ordem, `order` reatribuído de 1 a n. Use esse retorno para substituir o estado local.

**Erros**

| Status | Quando | `message` |
|---|---|---|
| 400 | Array vazio ou ausente | `moduleIds is required` |
| 400 | ID repetido | `moduleIds must not contain duplicates` |
| 400 | Falta ou sobra algum ID, ou ID de outro curso | `moduleIds must contain every module of the course exactly once` |
| 403 | Não ADMIN | ver seção 2 |
| 404 | Curso não existe | `Course not found: <id>` |

Em caso de `400`, nada foi alterado no servidor. Reverta o drag and drop para a ordem anterior.

---

### 5.5 Remover módulo

```
DELETE /api/v1/admin/modules/{id}
```

**Resposta 204** · sem corpo.

Remove o módulo e todos os seus conteúdos. Operação irreversível.

**Erros:** `403` · `404` (módulo não existe)

---

## 6. Endpoints relacionados (conteúdos, BE-05)

A mesma tela provavelmente vai abrir um módulo e gerenciar seus conteúdos. Esses endpoints já existem, sob o mesmo prefixo, e hoje **não exigem** `X-User-Id` (pendência conhecida):

| Método | Rota |
|---|---|
| GET | `/modules/{moduleId}/contents` |
| POST | `/modules/{moduleId}/contents` |
| GET | `/modules/{moduleId}/contents/{id}` |
| PUT | `/contents/{id}` |
| PUT | `/modules/{moduleId}/contents/reorder` (body `{ "contentIds": [...] }`) |
| DELETE | `/contents/{id}` |

O `ContentSummary` da listagem de módulos é um recorte. Para o objeto completo (url, descrição, isFree, duração), use o GET de conteúdo.

---

## 7. Dados de teste (seed local)

Com o perfil `dev` o banco sobe com:

| Curso | ID | Módulos |
|---|---|---|
| Atendimento de Excelencia em Hospedagem | `e0000000-0000-4000-e000-000000000001` | Fundamentos do Atendimento (1, 3 conteúdos), Situacoes Criticas (2, 2 conteúdos) |
| Gestao de Reservas e Overbooking | `e0000000-0000-4000-e000-000000000002` | Motor de Reservas (1) |
| Ingles para Recepcao | `e0000000-0000-4000-e000-000000000003` | Primeiros Contatos (1) |

Atenção: o seed trunca e recria os dados a cada start da aplicação.

**Exemplo com curl**

```bash
ADMIN=a0000000-0000-4000-a000-000000000001
COURSE=e0000000-0000-4000-e000-000000000001
BASE=http://localhost:8080/api/v1/admin

curl -s -H "X-User-Id: $ADMIN" $BASE/courses/$COURSE/modules

curl -s -X POST -H "X-User-Id: $ADMIN" -H "Content-Type: application/json" \
  -d '{"title":"Novo módulo"}' $BASE/courses/$COURSE/modules
```

---

## 8. Fluxo sugerido para a tela

1. Ao abrir o curso, `GET .../modules` e renderize a lista. Mostre `totalContents` e `totalDurationMinutes` no card de cada módulo.
2. Botão "Novo módulo": input de título, `POST`, append do retorno.
3. Título editável inline: `PUT /modules/{id}` no blur ou Enter.
4. Drag and drop: ao soltar, reordene localmente, envie `PUT .../reorder` com todos os IDs, substitua o estado pelo `data` da resposta. Em `400`, reverta.
5. Remover: modal de confirmação citando o título e o número de conteúdos. `DELETE`, remova da lista local.
6. Expandir módulo: use os endpoints de conteúdo (seção 6).

---

## 9. Pendências conhecidas no backend

- Remoção é física. Quando houver progresso de aluno, deve virar soft-delete. A tela não precisa mudar, mas o comportamento pode passar a ser "arquivar".
- Header `X-User-Id` será substituído por token.
- Endpoints de conteúdo ainda não checam ADMIN.
