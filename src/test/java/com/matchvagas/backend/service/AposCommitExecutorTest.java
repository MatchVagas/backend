package com.matchvagas.backend.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Efeitos colaterais só após o commit")
class AposCommitExecutorTest {

    private final AposCommitExecutor executor = new AposCommitExecutor();

    @AfterEach
    void limpar() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("sem transação executa na hora")
    void semTransacao() {
        AtomicInteger contador = new AtomicInteger();

        executor.executar(contador::incrementAndGet);

        assertThat(contador).hasValue(1);
    }

    @Test
    @DisplayName("com transação adia para o afterCommit e não executa no rollback")
    void comTransacao() {
        TransactionSynchronizationManager.initSynchronization();
        AtomicInteger contador = new AtomicInteger();

        executor.executar(contador::incrementAndGet);
        assertThat(contador).hasValue(0);

        var sincronizacoes = TransactionSynchronizationManager.getSynchronizations();
        sincronizacoes.forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        assertThat(contador).hasValue(0);

        sincronizacoes.forEach(TransactionSynchronization::afterCommit);
        assertThat(contador).hasValue(1);
    }
}
