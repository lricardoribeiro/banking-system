--liquibase formatted sql
--changeset banking:002 comment:Performance indexes

CREATE INDEX idx_accounts_owner_id        ON accounts(owner_id);
CREATE INDEX idx_accounts_status          ON accounts(status);

CREATE INDEX idx_ledger_account_id        ON ledger_entries(account_id);
CREATE INDEX idx_ledger_transfer_id       ON ledger_entries(transfer_id);
CREATE INDEX idx_ledger_account_created   ON ledger_entries(account_id, created_at);

-- Índice parcial para transferências pendentes – usado por queries de monitoramento
CREATE INDEX idx_transfers_pending        ON transfers(status, created_at)
    WHERE status = 'PENDING';
CREATE INDEX idx_transfers_source_account ON transfers(source_account_id);

CREATE INDEX idx_idempotency_expires      ON idempotency_keys(expires_at)
    WHERE expires_at IS NOT NULL;
