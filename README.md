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
- Swagger/OpenAPI no MS-Users

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

### Como encontrar a URL da MockAPI

Abra o `.env` na raiz do projeto e procure a variável `MOCKAPI_ACCOUNTS_URL`. O endereço configurado nessa linha é a URL usada pelo MS-Transactions para consultar contas na MockAPI.

Se precisar obter uma URL nova, acesse o projeto da MockAPI, abra o recurso de contas e copie a URL do endpoint. Use esse endereço como valor de `MOCKAPI_ACCOUNTS_URL` no `.env`.

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

## Documentação da API

O Swagger do MS-Users permite consultar e executar as operações documentadas. Acesse:

[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

As rotas protegidas usam HTTP Basic. No Swagger, clique em **Authorize** e informe o e-mail e a senha de um usuário cadastrado.

## Endpoints principais

### MS-Users

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/users` | Público | Cadastrar usuário |
| `POST` | `/users/import` | Basic Auth | Importar usuários por CSV |
| `GET` | `/users` | Basic Auth | Listar usuários |
| `GET` | `/users/me` | Basic Auth | Consultar usuário autenticado |
| `GET` | `/users/{id}` | Basic Auth | Consultar usuário por ID |
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
| `GET` | `/transactions` | Listar transações do usuário autenticado |
| `GET` | `/transactions/{id}` | Consultar uma transação própria |
| `GET` | `/transactions/analysis` | Analisar despesas do usuário autenticado |

A análise aceita opcionalmente `startDate` e `endDate`, no formato `AAAA-MM-DD`. Se as datas forem omitidas, a análise considera o mês atual.

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

## Testes

Para executar os testes do MS-Users:

```powershell
cd ms-users
.\mvnw.cmd verify
```

Para executar os testes do MS-Transactions:

```powershell
cd ..\ms-transactions
.\mvnw.cmd verify
```