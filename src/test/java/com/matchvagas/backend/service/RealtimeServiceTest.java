package com.matchvagas.backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("Fase 2 — Realtime (SSE)")
class RealtimeServiceTest {

    private final RealtimeService realtimeService = new RealtimeService();

    @Test
    @DisplayName("subscribe registra a conexão do usuário")
    void subscribeRegistraConexao() {
        realtimeService.subscribe(1L);

        assertThat(realtimeService.contarConexoes(1L)).isEqualTo(1);
        assertThat(realtimeService.contarConexoes(2L)).isZero();
    }

    @Test
    @DisplayName("registrar e remover atualizam a contagem de conexões")
    void registrarERemoverAtualizamContagem() {
        SseEmitter emitter = new SseEmitter();

        realtimeService.registrar(1L, emitter);
        assertThat(realtimeService.contarConexoes(1L)).isEqualTo(1);

        realtimeService.remover(1L, emitter);
        assertThat(realtimeService.contarConexoes(1L)).isZero();
    }

    @Test
    @DisplayName("O mesmo usuário pode ter várias conexões (abas/dispositivos)")
    void multiplasConexoesDoMesmoUsuario() {
        realtimeService.registrar(1L, new SseEmitter());
        realtimeService.registrar(1L, new SseEmitter());

        assertThat(realtimeService.contarConexoes(1L)).isEqualTo(2);
    }

    @Test
    @DisplayName("Enviar para usuário sem conexão é um no-op (não lança)")
    void enviarParaUsuarioSemConexaoNaoFalha() {
        assertThatCode(() -> realtimeService.enviarPara(999L, "mensagem", Map.of("x", 1)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Enviar remove conexões mortas")
    void enviarParaRemoveConexaoMorta() {
        SseEmitter emitter = new SseEmitter();
        realtimeService.registrar(1L, emitter);
        emitter.complete(); // conexão encerrada → o próximo send falha

        realtimeService.enviarPara(1L, "mensagem", Map.of("x", 1));

        assertThat(realtimeService.contarConexoes(1L)).isZero();
    }
}
