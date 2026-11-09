# Beca API Financeira

API financeira composta por dois microsserviços Spring Boot:

- **MS-Users**: cadastro, autenticação e gerenciamento de usuários, além de importação por CSV.
- **MS-Transactions**: criação, consulta e análise de transações. Valida as credenciais consultando o MS-Users.

## Arquitetura

- O MS-Users usa o PostgreSQL `users_db`.
- O MS-Transactions usa o PostgreSQL `transactions_db`.
- Cada microsserviço se conecta ao seu próprio banco.
- No Docker Compose, o MS-Transactions consulta o MS-Users pelo endereço interno `http://ms-users:8080`.
- As senhas dos usuários são armazenadas com hash BCrypt.
- As rotas protegidas usam autenticação HTTP Basic.

## Tecnologias

- Java 25
- Spring Boot
- Spring Data JPA
- PostgreSQL 17
- Docker Compose
- JUnit e Mockito
- Swagger/OpenAPI nos dois microsserviços

## Pré-requisitos

- Docker Desktop com Docker Compose
- Git

Para executar os microsserviços fora do Docker, também é necessário ter Java 25.

## Configuração local

O Docker Compose lê as variáveis do arquivo `.env`, localizado na raiz do projeto. Se ele ainda não existir, copie `.env.example` para `.env`.

O arquivo deve conter estas variáveis:

```dotenv
POSTGRES_USER=beca
POSTGRES_PASSWORD=defina_uma_senha_local
USERS_DB_NAME=users_db
TRANSACTIONS_DB_NAME=transactions_db
MOCKAPI_ACCOUNTS_URL=https://seu-projeto.mockapi.io/accounts
MS_USERS_BASE_URL=http://localhost:8080
```

### Configurar a MockAPI

O MS-Transactions consulta o recurso `accounts` da MockAPI para obter o saldo, o limite diário e o limite por transação.

No projeto da MockAPI, abra o recurso `accounts` e copie a URL do endpoint. Configure essa URL na variável `MOCKAPI_ACCOUNTS_URL` do arquivo `.env`, na raiz do repositório:

```dotenv
MOCKAPI_ACCOUNTS_URL=https://seu-projeto.mockapi.io/accounts
```

Cada usuário que consultar saldo ou registrar um depósito, retirada, transferência ou compra precisa ter um registro correspondente no recurso `accounts`. Primeiro, cadastre o usuário no MS-Users e anote o `id` retornado. Use esse mesmo valor no campo `userId` da conta na MockAPI. O cadastro no MS-Users não cria automaticamente uma conta na MockAPI. Depósitos atualizam o saldo, mas não passam pelas validações de saldo disponível e limites de saída.

Exemplo de registro:

```json
[
  {
    "userId": "52",
    "balance": 1000.00,
    "dailyLimit": 500.00,
    "perTransactionLimit": 200.00
  }
]
```

O valor `"52"` é apenas ilustrativo: substitua-o pelo ID real retornado pelo MS-Users. Ao adicionar uma conta na MockAPI, preserve os registros existentes. Se não houver conta para o usuário autenticado, o MS-Transactions responde `404`; se a MockAPI estiver indisponível, responde `503`.

Quando uma operação aprovada precisa alterar saldos, o MS-Transactions atualiza a MockAPI antes de aprovar e salvar a transação. Se um PUT falhar, a transação é registrada como `REJECTED` com um motivo. Em uma transferência, o serviço tenta restaurar os saldos que já tentou atualizar e só registra o crédito no histórico do destinatário quando as atualizações dos dois saldos funcionam.

Se também falhar a tentativa de restauração, a transação continua registrada como `REJECTED`, mas os saldos precisam de conferência manual na MockAPI.

No Docker Compose, o MS-Users é acessado pelo nome do serviço `ms-users`. Por isso, o Compose configura `MS_USERS_BASE_URL` como `http://ms-users:8080`.

> Não compartilhe nem versione o arquivo `.env`: ele contém credenciais locais. O repositório mantém o `.env.example` como modelo.

## Executar com Docker Compose

Na raiz do projeto:

```powershell
docker compose up -d --build
```

Verificar os serviços:

```powershell
docker compose ps
```

Consultar os logs:

```powershell
docker compose logs --tail 50 ms-users
docker compose logs --tail 50 ms-transactions
```

Parar os serviços:

```powershell
docker compose down
```

Os dados dos bancos ficam em volumes Docker e são preservados ao executar `docker compose down`.

## Endereços locais

| Serviço | Endereço |
|---|---|
| MS-Users | `http://localhost:8080` |
| MS-Transactions | `http://localhost:8081` |
| PostgreSQL do MS-Users | `localhost:5433` |
| PostgreSQL do MS-Transactions | `localhost:5434` |
| Swagger do MS-Users | `http://localhost:8080/swagger-ui/index.html` |
| Swagger do MS-Transactions | `http://localhost:8081/swagger-ui/index.html` |

## Documentação da API

Os Swagger dos dois microsserviços permitem consultar e executar as operações documentadas:

- MS-Users: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- MS-Transactions: [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html)

As rotas protegidas usam HTTP Basic. No Swagger, clique em **Authorize** e informe o e-mail e a senha de um usuário cadastrado.

## Endpoints principais

### MS-Users

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/users` | Público | Cadastrar usuário |
| `POST` | `/users/import` | Basic Auth | Importar usuários por CSV |
| `GET` | `/users/me` | Basic Auth | Consultar usuário autenticado |
| `GET` | `/users/{id}` | Basic Auth | Consultar o próprio usuário por ID |
| `PUT` | `/users/{id}` | Basic Auth | Atualizar o próprio cadastro |
| `DELETE` | `/users/{id}` | Basic Auth | Excluir o próprio cadastro |

A senha do cadastro deve ter pelo menos 8 caracteres. As respostas não incluem a senha nem seu hash.

O CSV deve usar o cabeçalho:

```text
name;email;password
```

Cada linha deve conter nome, e-mail e senha separados por ponto e vírgula.

### MS-Transactions

Todas as rotas exigem Basic Auth. O MS-Transactions valida as credenciais no MS-Users e usa a identidade autenticada para filtrar as transações.

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/transactions` | Criar transação para o usuário autenticado |
| `GET` | `/transactions` | Listar transações do usuário autenticado com filtros opcionais de período e tipo |
| `GET` | `/transactions/{id}` | Consultar uma transação própria |
| `GET` | `/transactions/analysis` | Analisar despesas do usuário autenticado |
| `GET` | `/transactions/export` | Exportar transações do usuário no período informado por `startDate` e `endDate` |
| `GET` | `/transactions/account-limits` | Consultar saldo, limite diário e limite por transação do usuário autenticado, com dados da MockAPI |

A análise aceita opcionalmente `startDate` e `endDate`, no formato ISO `AAAA-MM-DD`. Se as datas forem omitidas, a análise considera o mês atual.

Os parâmetros de data das rotas de transações usam o formato ISO `AAAA-MM-DD`, por exemplo `2026-11-01`. Na listagem, `startDate` e `endDate` devem ser informadas juntas; o parâmetro `type` pode ser usado sozinho ou combinado com o período. A exportação CSV aceita períodos de até 12 meses e apresenta as datas também no formato ISO.

Exemplos de filtros para a listagem:

```http
GET http://localhost:8081/transactions?type=DEPOSIT
GET http://localhost:8081/transactions?startDate=2026-11-01&endDate=2026-11-30
GET http://localhost:8081/transactions?startDate=2026-11-01&endDate=2026-11-30&type=PURCHASE
```

As datas inicial e final são inclusivas. A listagem retorna somente transações do usuário autenticado; sem filtros, retorna todas as transações desse usuário.

Os tipos de transação são `DEPOSIT`, `WITHDRAW`, `TRANSFER` e `PURCHASE`.

## Exemplo: cadastrar usuário

```http
POST http://localhost:8080/users
Content-Type: application/json
```

```json
{
  "name": "Ana Silva",
  "email": "ana.silva@example.com",
  "password": "SenhaSegura123!"
}
```

## Exemplo: criar transação

Envie a requisição para o MS-Transactions com autenticação Basic. O usuário da transação é identificado pelas credenciais, não por um `userId` enviado no JSON.

```http
POST http://localhost:8081/transactions
Content-Type: application/json
```

```json
{
  "description": "Compra no mercado",
  "amount": 50.00,
  "type": "PURCHASE",
  "category": "Alimentação"
}
```

Para uma transferência, informe também o identificador do usuário de destino:

```json
{
  "description": "Transferência",
  "amount": 25.00,
  "type": "TRANSFER",
  "destinationUserId": 47
}
```

## Testes e cobertura de código

Os microsserviços utilizam JUnit e Mockito para testes automatizados e JaCoCo para análise de cobertura de código.

### MS-Users

Para executar os testes e verificar a cobertura mínima de 80% de linhas:

```powershell
cd ms-users
.\mvnw.cmd clean verify
```

O relatório HTML do JaCoCo é gerado em:

`ms-users/target/site/jacoco/index.html`

**Resultado da última execução verificada:**

- Cobertura de linhas: **86,9%** (119 de 137 linhas).
- Cobertura de instruções: 82%.
- Cobertura de branches: 67%.
- Meta mínima configurada: 80% de linhas.

O Maven executa `jacoco:check` na fase `verify` e falha caso a cobertura considerada fique abaixo da meta.

### MS-Transactions

Para executar os testes:

```powershell
cd ms-transactions
.\mvnw.cmd clean verify
```

O relatório HTML do JaCoCo é gerado em:

`ms-transactions/target/site/jacoco/index.html`

**Última execução verificada (`.\mvnw.cmd clean verify`):**

- Linhas cobertas: **538 de 577 (93,2%)**.
- Instruções: **93%**.
- Branches: **85%**.
- Meta mínima: **80% de cobertura de linhas** — cumprida.

### Observações

Nos dois microsserviços, `jacoco:check` executa na fase `verify` e exige pelo menos 80% de cobertura de linhas nas classes incluídas no cálculo, respeitando as exclusões configuradas.
