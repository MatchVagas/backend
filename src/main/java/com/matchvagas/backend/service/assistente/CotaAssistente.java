package com.matchvagas.backend.service.assistente;

import com.matchvagas.backend.service.RateLimiterService;
import com.matchvagas.backend.service.llm.LlmEstruturado;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cota de gerações por usuário, somando os três recursos assistivos. Cada geração
 * em CPU pode ocupar o modelo por minutos; sem teto, um usuário monopoliza a fila.
 */
@Component
class CotaAssistente {
    static final long JANELA_MS = 60 * 60 * 1000L;

    private final LlmEstruturado llm;
    private final RateLimiterService rateLimiterService;
    private final int limitePorHora;

    CotaAssistente(LlmEstruturado llm, RateLimiterService rateLimiterService,
                   @Value("${app.llm.limite-por-hora:30}") int limitePorHora) {
        this.llm = llm;
        this.rateLimiterService = rateLimiterService;
        this.limitePorHora = limitePorHora;
    }

    /** Chamar só quando for gerar de fato: resposta em cache não consome cota. LLM desligado → 503 sem gastar cota. */
    void consumir(Long usuarioId) {
        llm.exigirAtivo();
        rateLimiterService.verificar("llm:" + usuarioId, limitePorHora, JANELA_MS);
    }
}