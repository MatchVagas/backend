package com.matchvagas.backend.service;

import com.matchvagas.backend.exception.RateLimitException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SEC-06 — Rate limiting por chave")
class RateLimiterServiceTest {

    private final RateLimiterService limiter = new RateLimiterService();

    @Test
    @DisplayName("padrão: 5 tentativas passam, a 6ª é bloqueada")
    void limitePadrao() {
        for (int i = 0; i < 5; i++) limiter.verificar("login:1.1.1.1");

        assertThatThrownBy(() -> limiter.verificar("login:1.1.1.1"))
                .isInstanceOf(RateLimitException.class);
    }

    @Test
    @DisplayName("chaves diferentes têm contadores independentes")
    void chavesIndependentes() {
        for (int i = 0; i < 5; i++) limiter.verificar("login:1.1.1.1");

        assertThatCode(() -> limiter.verificar("login:2.2.2.2")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("limite e janela próprios; a janela expirada recomeça a contagem")
    void janelaPropria() throws InterruptedException {
        limiter.verificar("llm:1", 1, 50);
        assertThatThrownBy(() -> limiter.verificar("llm:1", 1, 50)).isInstanceOf(RateLimitException.class);

        Thread.sleep(80);

        assertThatCode(() -> limiter.verificar("llm:1", 1, 50)).doesNotThrowAnyException();
    }
}
