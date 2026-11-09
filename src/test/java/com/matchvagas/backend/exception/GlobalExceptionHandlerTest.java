package com.matchvagas.backend.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tratamento global de exceções")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("violação de integridade vira 409 sem expor SQL nem nomes de colunas")
    void violacaoDeIntegridade() {
        var excecao = new DataIntegrityViolationException("could not execute statement",
                new SQLException("NULL not allowed for column \"descricao\"; SQL statement: insert into tipo_vaga"));

        var resposta = handler.handleViolacaoDeIntegridade(excecao);

        assertThat(resposta.getStatusCode().value()).isEqualTo(409);
        assertThat(resposta.getBody().getMessage()).doesNotContain("SQL", "descricao", "tipo_vaga");
    }

    @Test
    @DisplayName("erro inesperado vira 500 com referência, sem a mensagem interna")
    void erroInesperado() {
        var resposta = handler.handleGenericException(new IllegalStateException("detalhe interno sensível"));

        assertThat(resposta.getStatusCode().value()).isEqualTo(500);
        assertThat(resposta.getBody().getMessage()).contains("Referência:").doesNotContain("sensível");
    }

    @Test
    @DisplayName("exceções de negócio mantêm a mensagem e o status")
    void negocio() {
        assertThat(handler.handleBusinessException(new BusinessException("Regra X")).getStatusCode().value())
                .isEqualTo(400);
        assertThat(handler.handleRateLimitException(new RateLimitException("Calma")).getStatusCode().value())
                .isEqualTo(429);
        assertThat(handler.handleLlmIndisponivel(new LlmIndisponivelException("Fora")).getStatusCode().value())
                .isEqualTo(503);
    }
}
