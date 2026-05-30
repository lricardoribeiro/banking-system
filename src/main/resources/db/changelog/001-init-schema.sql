--liquibase formatted sql
--changeset banking:001 comment:Initial banking schema

CREATE TABLE accounts (
    id          UUID        NOT NULL,
    owner_id    VARCHAR(100) NOT NULL,
    status      VARCHAR(20)  NOT NULL CHECK (status IN ('ACTIVE','BLOCKED','CLOSED')),
    currency    VARCHAR(3)   NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_accounts PRIMARY KEY (id)
);

-- Ledger imutável de apenas-inserção.
-- NUNCA adicione ON UPDATE ou triggers que modifiquem estas linhas.
CREATE TABLE ledger_entries (
    id          UUID         NOT NULL,
    transfer_id UUID         NOT NULL,
    account_id  UUID         NOT NULL,
    entry_type  VARCHAR(10)  NOT NULL CHECK (entry_type IN ('DEBIT','CREDIT')),
    amount      NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    currency    VARCHAR(3)    NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_ledger_entries PRIMARY KEY (id),
    CONSTRAINT fk_ledger_account FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE TABLE transfers (
    id               UUID         NOT NULL,
    source_account_id UUID        NOT NULL,
    target_account_id UUID        NOT NULL,
    amount           NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    currency         VARCHAR(3)    NOT NULL,
    idempotency_key  VARCHAR(64)   NOT NULL,
    status           VARCHAR(20)   NOT NULL CHECK (status IN ('PENDING','COMPLETED','FAILED','COMPENSATING','COMPENSATED')),
    failure_reason   VARCHAR(1000),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_transfers PRIMARY KEY (id),
    CONSTRAINT uq_transfers_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT fk_transfer_source FOREIGN KEY (source_account_id) REFERENCES accounts(id),
    CONSTRAINT fk_transfer_target FOREIGN KEY (target_account_id) REFERENCES accounts(id)
);

-- Repositório de idempotência para operações da API.
-- Chaves expiram após 24h e são limpas por job agendado.
CREATE TABLE idempotency_keys (
    key_value   VARCHAR(64)  NOT NULL,
    result_json VARCHAR(4000),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMPTZ,
    CONSTRAINT pk_idempotency_keys PRIMARY KEY (key_value)
);

COMMENT ON TABLE ledger_entries IS 'Ledger de partidas dobradas. Imutável – apenas INSERT, sem UPDATE/DELETE.';
COMMENT ON TABLE idempotency_keys IS 'Repositório de chaves de idempotência. Previne dupla execução de operações da API.';
