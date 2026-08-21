package com.matchvagas.backend.controller;

import com.matchvagas.backend.service.RealtimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/realtime")
@RequiredArgsConstructor
@Tag(name = "Realtime", description = "Stream de eventos em tempo real via SSE (Fase 2)")
public class RealtimeController {

    private final RealtimeService realtimeService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
        summary = "Abrir stream SSE do usuário autenticado",
        description = "Conexão de longa duração que emite os eventos 'conectado', 'notificacao' e "
                    + "'mensagem'. Autentique enviando o JWT no header Authorization (use um polyfill "
                    + "de EventSource que suporte headers para não expor o token na URL)."
    )
    public SseEmitter stream(Authentication auth) {
        return realtimeService.subscribe(Long.parseLong(auth.getName()));
    }
}
