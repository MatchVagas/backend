package com.matchvagas.backend.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Executa uma ação somente após o commit da transação corrente. Serve para efeitos
 * colaterais externos (push SSE, e-mail) que não devem disparar se a transação sofrer
 * rollback — evitando notificar o usuário sobre algo que não chegou a persistir.
 *
 * <p>Sem transação ativa (ex.: chamada fora de contexto transacional ou em teste unitário),
 * executa a ação imediatamente.
 */
@Component
public class AposCommitExecutor {

    public void executar(Runnable acao) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    acao.run();
                }
            });
        } else {
            acao.run();
        }
    }
}
