package com.banking.system.adapter.out.persistence;

import com.banking.system.adapter.out.persistence.entity.IdempotencyKeyJpaEntity;
import com.banking.system.adapter.out.persistence.repository.IdempotencyKeyJpaRepository;
import com.banking.system.application.port.out.IdempotencyPort;
import com.banking.system.infrastructure.annotation.PersistenceAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/** Adaptador de persistência para o mecanismo de idempotência. */
@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class IdempotencyPersistenceAdapter implements IdempotencyPort {

    private static final int TTL_HORAS = 24;
    private final IdempotencyKeyJpaRepository repository;

    @Override
    public boolean exists(String chave) {
        return repository.existsById(chave);
    }

    @Override
    public void store(String chave, String resultJson) {
        IdempotencyKeyJpaEntity entidade = IdempotencyKeyJpaEntity.builder()
                .keyValue(chave)
                .resultJson(resultJson)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(TTL_HORAS, ChronoUnit.HOURS))
                .build();
        repository.save(entidade);
        log.debug("Chave de idempotência armazenada: {}", chave);
    }

    @Override
    public Optional<String> getResult(String chave) {
        return repository.findById(chave).map(IdempotencyKeyJpaEntity::getResultJson);
    }
}
