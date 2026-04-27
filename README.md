# Banking System

Sistema bancário de nível produção construído com Java 21 + Spring Boot 3.2, aplicando Arquitetura Hexagonal, DDD e padrões de sistemas distribuídos.

---

## Decisões de Arquitetura

### Arquitetura Hexagonal (Ports & Adapters)

O domínio não possui nenhuma dependência de framework. Ports são interfaces; adapters as implementam.

   
adapter/in      ->  application (ports de entrada)
adapter/out     ->  application (ports de saída)
application     ->  domain
domain          ->  (sem dependências externas)
   

### Domain-Driven Design

- **Account** – Aggregate Root com status, moeda e controle de versão para locking otimista
- **Transfer** – Aggregate Root com máquina de estados (PENDING -> COMPLETED / FAILED / COMPENSATED)
- **Money** – Value Object imutável com escala normalizada (HALF_EVEN) e validação de moeda
- **LedgerEntry** – Entidade append-only; nunca atualizada após criação
- **Eventos de domínio** – records Java: AccountCreatedEvent, TransferInitiatedEvent, TransferCompletedEvent

### Livro-Razão de Partidas Dobradas (Double-Entry Ledger)

Cada transferência cria exatamente **1 DÉBITO + 1 CRÉDITO**. O saldo nunca é armazenado diretamente na conta; é sempre calculado em tempo real:


Saldo = SUM(CRÉDITOS) - SUM(DÉBITOS)


Invariante validada programaticamente antes do commit:

SUM(débitos) == SUM(créditos)
   

### Locking Otimista e Prevenção de Deadlock

- `@Version` na `AccountJpaEntity` — o JPA acrescenta `WHERE version=N` no UPDATE
- Conflito -> HTTP 409 -> cliente retenta com a mesma chave de idempotência
- Ordem de aquisição de lock determinística (lexicográfica por AccountId) para prevenir deadlock A->B / B->A

### Idempotência

- Chave UUID por operação lógica gerada pelo cliente
- Constraint `UNIQUE` em `idempotency_key` no banco
- Pré-verificação + armazenamento atômico dentro da transação
- `DataIntegrityViolationException` em duplicatas concorrentes -> resposta idempotente

### Isolamento de Transação

`TransferMoneyService` usa `REPEATABLE_READ` para evitar phantom reads de novos lançamentos inseridos entre a leitura do saldo e a escrita do débito.

### Mensageria Kafka

- Produtor com `enable.idempotence=true`, `acks=all`
- Consumidor com ACK manual, backoff exponencial e Dead Letter Topic (DLT)
- Particionamento por `sourceAccountId` garante ordenação de eventos por conta

### Padrão Saga

`TransferStatus` inclui `COMPENSATING` / `COMPENSATED` para rollback distribuído. Em caso de falha parcial, o serviço inicia compensação e marca a transferência como `COMPENSATED`.

### Teorema CAP

Escolha **CP** (Consistência + Tolerância a Partições) — consistência sobre disponibilidade para operações financeiras. Em partição de rede, a operação falha em vez de aceitar dados potencialmente inconsistentes.

### Padrão Outbox (recomendado, não implementado)

Para entrega garantida de eventos Kafka:
1. Na mesma transação do banco, faça INSERT na tabela `outbox`
2. Um conector Debezium (CDC) ou job de polling lê o outbox e publica
3. Marca a entrada do outbox como publicada

Isso desacopla o commit do banco da disponibilidade do Kafka.

---

## Estrutura do Projeto

   
src/main/java/com/banking/system/
├── BankingSystemApplication.java
├── domain/
│   ├── account/          # Aggregate Account, Money (VO), AccountId (VO), eventos, exceções
│   ├── ledger/           # LedgerEntry (entidade append-only), EntryType
│   ├── transfer/         # Aggregate Transfer, TransferStatus, eventos, exceções
│   └── shared/           # DomainEvent (interface), AggregateRoot (classe base)
├── application/
│   ├── port/in/          # Interfaces de casos de uso (entrada)
│   ├── port/out/         # Interfaces de persistência e mensageria (saída)
│   └── usecase/          # Implementações dos casos de uso
│       ├── command/      # Objetos de comando (imutáveis)
│       └── result/       # Objetos de resultado
├── adapter/
│   ├── in/
│   │   ├── web/          # Controllers REST, GlobalExceptionHandler, DTOs
│   │   └── messaging/    # Consumidores Kafka
│   └── out/
│       ├── persistence/  # Adapters JPA, entidades, repositórios
│       └── messaging/    # Publisher Kafka, DTOs de eventos
└── infrastructure/
    ├── annotation/       # @UseCase, @PersistenceAdapter, @WebAdapter
    ├── config/           # KafkaConfig, SecurityConfig, JpaConfig
    ├── logging/          # CorrelationIdFilter (MDC)
    └── security/         # JwtTokenProvider, JwtAuthenticationFilter
   

---

## Como Executar

### Pré-requisitos

- Java 21+
- Docker + Docker Compose
- Maven 3.9+ (ou use o wrapper `./mvnw`)

### Subir o ambiente completo

   bash
docker-compose up -d
   

Serviços disponíveis:
- API: http://localhost:8080
- Kafka UI: http://localhost:8090
- PostgreSQL: localhost:5432

### Executar localmente (desenvolvimento)

   bash
# 1. Subir apenas infraestrutura
docker-compose up -d postgres kafka zookeeper

# 2. Executar a aplicação
./mvnw spring-boot:run
   

### Executar testes

   bash
# Testes unitários
./mvnw test

# Testes de integração (requer Docker para Testcontainers)
./mvnw verify

# Testes com relatório de cobertura
./mvnw verify jacoco:report
   

---

## Endpoints da API

### Autenticação

Todos os endpoints requerem JWT no header:
   
Authorization: Bearer <token>
   

### Contas

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | /api/v1/accounts | Criar conta |
| GET | /api/v1/accounts/{id} | Buscar conta |
| GET | /api/v1/accounts/{id}/balance | Consultar saldo |

### Transferências

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | /api/v1/transfers | Iniciar transferência |
| GET | /api/v1/transfers/{id} | Buscar transferência |

### Exemplo – Criar transferência

   bash
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: $(uuidgen)" \
  -d '{
    "sourceAccountId": "aaaaaaaa-0000-0000-0000-000000000001",
    "targetAccountId": "bbbbbbbb-0000-0000-0000-000000000002",
    "amount": 100.00,
    "currency": "BRL",
    "idempotencyKey": "'$(uuidgen)'",
    "description": "Pagamento de serviço"
  }'
   

### Códigos de resposta

| Código | Significado |
|--------|-------------|
| 201 | Transferência criada e concluída |
| 200 | Resposta idempotente (já processada) |
| 400 | Erro de validação |
| 401 | Não autenticado |
| 403 | Sem permissão |
| 404 | Conta ou transferência não encontrada |
| 409 | Conflito de locking otimista – retente com a mesma chave |
| 422 | Violação de regra de negócio (saldo insuficiente, conta bloqueada) |
| 500 | Erro interno |

---

## Observabilidade

### Métricas (Micrometer + Prometheus)

| Métrica | Tipo | Descrição |
|---------|------|-----------|
| `transfers.success` | Counter | Transferências concluídas com sucesso |
| `transfers.failed` | Counter | Transferências com falha |
| `transfers.idempotent` | Counter | Respostas idempotentes retornadas |
| `transfers.duration` | Timer | Latência de ponta a ponta |

Acesse em: `GET /actuator/prometheus`

### Rastreamento (Correlation ID)

Cada requisição recebe um `X-Correlation-ID` (gerado se ausente) injetado no MDC. Todos os logs incluem este ID para rastreabilidade completa entre requisições.

### Health Checks

   
GET /actuator/health/liveness   # aplicação viva
GET /actuator/health/readiness  # pronta para receber tráfego
   

---

## Segurança

- Autenticação **JWT HS256** com expiração configurável (padrão: 1 hora)
- Sessões **stateless** (sem armazenamento de sessão no servidor)
- Roles: `CUSTOMER`, `BANK_AGENT`, `ADMIN`
- Senhas com **BCrypt** custo 12
- Usuário sem root no container (UID 1001)
- Headers de segurança HTTP via Spring Security

---

## Banco de Dados

### Migrações (Liquibase)

   
src/main/resources/db/changelog/
├── db.changelog-master.yaml   # arquivo mestre
├── 001-init-schema.sql        # criação das tabelas
└── 002-indexes.sql            # índices de performance
   

### Tabelas principais

| Tabela | Descrição |
|--------|-----------|
| `accounts` | Contas bancárias com locking otimista (`version`) |
| `ledger_entries` | Lançamentos append-only (débitos e créditos) |
| `transfers` | Registro de transferências com chave de idempotência única |
| `idempotency_keys` | Cache de idempotência com TTL |

---

## Trade-offs e Limitações Conhecidas

| Aspecto | Decisão | Alternativa |
|---------|---------|-------------|
| Consistência vs Disponibilidade | CP (consistência) | AP (disponibilidade) com eventual consistency |
| Saldo | Calculado do ledger (100% consistente) | Cache de saldo (mais rápido, risco de divergência) |
| Locking | Otimista (menos contenção) | Pessimista (mais simples, mais contenção) |
| Entrega de eventos | Melhor esforço (sem Outbox) | Outbox Pattern (entrega garantida) |
| Schema de mensagens | JSON simples | Avro/Protobuf com Schema Registry |
| Autenticação | JWT local | OAuth2/OIDC com provedor externo |

---

## Roadmap

- [ ] Implementar Outbox Pattern para entrega garantida de eventos
- [ ] Schema Registry com Avro para evolução de schema Kafka
- [ ] Caso de uso de depósito e saque
- [ ] Extrato paginado de lançamentos
- [ ] Suporte a múltiplas moedas com conversão
- [ ] Rate limiting por conta
- [ ] Auditoria de acessos
