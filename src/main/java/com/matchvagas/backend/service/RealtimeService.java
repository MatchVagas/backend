package com.matchvagas.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Push em tempo real via Server-Sent Events (Fase 2). Mantém, por usuário, o conjunto
 * de conexões SSE abertas (uma por aba/dispositivo) e entrega eventos a todas elas.
 *
 * <p>SSE (e não WebSocket) porque o fluxo é unidirecional servidor→cliente e a API é
 * stateless com JWT: reaproveita o filtro de autenticação por header, sem broker/STOMP.
 * O front deve abrir o stream enviando o JWT no header {@code Authorization} (via
 * polyfill de EventSource), evitando token na URL.
 */
@Slf4j
@Service
public class RealtimeService {

    // 30 min: ao expirar, o cliente reconecta sozinho.
    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<Long, Set<SseEmitter>> emittersPorUsuario = new ConcurrentHashMap<>();

    /** Abre um stream SSE para o usuário e o registra. */
    public SseEmitter subscribe(Long usuarioId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        registrar(usuarioId, emitter);
        // Evento inicial: confirma a conexão e faz o flush dos headers da resposta.
        enviar(emitter, "conectado", Map.of("usuarioId", usuarioId));
        return emitter;
    }

    /** Entrega um evento a todas as conexões do usuário (no-op se ele não estiver conectado). */
    public void enviarPara(Long usuarioId, String evento, Object dados) {
        if (usuarioId == null) return;
        Set<SseEmitter> conexoes = emittersPorUsuario.get(usuarioId);
        if (conexoes == null || conexoes.isEmpty()) return;
        for (SseEmitter emitter : conexoes) {
            if (!enviar(emitter, evento, dados)) {
                remover(usuarioId, emitter);
            }
        }
    }

    /** Heartbeat: mantém as conexões vivas através de proxies (Render) e limpa as mortas. */
    @Scheduled(fixedRate = 25_000L)
    public void heartbeat() {
        emittersPorUsuario.forEach((usuarioId, conexoes) ->
                conexoes.forEach(emitter -> {
                    try {
                        emitter.send(SseEmitter.event().comment("ping"));
                    } catch (Exception e) {
                        remover(usuarioId, emitter);
                    }
                }));
    }

    /** Número de conexões abertas do usuário (útil para diagnóstico/testes). */
    public int contarConexoes(Long usuarioId) {
        Set<SseEmitter> conexoes = emittersPorUsuario.get(usuarioId);
        return conexoes == null ? 0 : conexoes.size();
    }

    // ── Internos (visibilidade de pacote para teste) ──────────────────────────

    void registrar(Long usuarioId, SseEmitter emitter) {
        emittersPorUsuario.computeIfAbsent(usuarioId, k -> ConcurrentHashMap.newKeySet()).add(emitter);
        emitter.onCompletion(() -> remover(usuarioId, emitter));
        emitter.onTimeout(() -> remover(usuarioId, emitter));
        emitter.onError(e -> remover(usuarioId, emitter));
    }

    void remover(Long usuarioId, SseEmitter emitter) {
        Set<SseEmitter> conexoes = emittersPorUsuario.get(usuarioId);
        if (conexoes == null) return;
        conexoes.remove(emitter);
        if (conexoes.isEmpty()) {
            emittersPorUsuario.remove(usuarioId);
        }
    }

    private boolean enviar(SseEmitter emitter, String evento, Object dados) {
        try {
            emitter.send(SseEmitter.event().name(evento).data(dados));
            return true;
        } catch (Exception e) {
            log.debug("SSE: falha ao enviar evento '{}', removendo conexão: {}", evento, e.getMessage());
            return false;
        }
    }
}
