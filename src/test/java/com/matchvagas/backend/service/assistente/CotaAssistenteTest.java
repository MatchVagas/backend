package com.matchvagas.backend.service.assistente;

import com.matchvagas.backend.exception.LlmIndisponivelException;
import com.matchvagas.backend.service.RateLimiterService;
import com.matchvagas.backend.service.llm.LlmEstruturado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("Fase 3 — cota de gerações do assistente")
class CotaAssistenteTest {

    @Mock LlmEstruturado llm;
    @Mock RateLimiterService rateLimiterService;

    @Test
    @DisplayName("consome a cota do usuário com o limite configurado")
    void consome() {
        new CotaAssistente(llm, rateLimiterService, 7).consumir(5L);

        verify(rateLimiterService).verificar("llm:5", 7, CotaAssistente.JANELA_MS);
    }

    @Test
    @DisplayName("LLM desligado falha antes de gastar cota")
    void desligadoNaoGastaCota() {
        doThrow(new LlmIndisponivelException("desligado")).when(llm).exigirAtivo();

        assertThatThrownBy(() -> new CotaAssistente(llm, rateLimiterService, 7).consumir(5L))
                .isInstanceOf(LlmIndisponivelException.class);
        verifyNoInteractions(rateLimiterService);
    }
}
