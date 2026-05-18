package com.matchvagas.backend.controller;

import com.matchvagas.backend.dto.NotificacoesRequestDTO;
import com.matchvagas.backend.dto.NotificacoesResponseDTO;
import com.matchvagas.backend.service.NotificacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificacoes")
@RequiredArgsConstructor
@Tag(name = "Notificações", description = "Notificações in-app com envio automático de email")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Criar notificação para um usuário (ADMIN)",
        description = "Cria uma notificação in-app e envia email para o usuário destinatário."
    )
    public ResponseEntity<NotificacoesResponseDTO> criar(
            @Valid @RequestBody NotificacoesRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificacaoService.criar(dto));
    }

    @GetMapping("/minhas")
    @Operation(summary = "Listar todas as minhas notificações")
    public ResponseEntity<List<NotificacoesResponseDTO>> minhas(Authentication auth) {
        return ResponseEntity.ok(notificacaoService.listarPorUsuario(usuarioId(auth)));
    }

    @GetMapping("/minhas/nao-lidas")
    @Operation(summary = "Listar minhas notificações não lidas")
    public ResponseEntity<List<NotificacoesResponseDTO>> naoLidas(Authentication auth) {
        return ResponseEntity.ok(notificacaoService.listarNaoLidas(usuarioId(auth)));
    }

    @GetMapping("/minhas/contagem")
    @Operation(summary = "Contar notificações não lidas")
    public ResponseEntity<Map<String, Long>> contagem(Authentication auth) {
        return ResponseEntity.ok(Map.of("naoLidas", notificacaoService.contarNaoLidas(usuarioId(auth))));
    }

    @PatchMapping("/{id}/lida")
    @Operation(summary = "Marcar notificação como lida")
    public ResponseEntity<NotificacoesResponseDTO> marcarLida(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(notificacaoService.marcarComoLida(id, usuarioId(auth)));
    }

    @PatchMapping("/lidas/todas")
    @Operation(summary = "Marcar todas as notificações como lidas")
    public ResponseEntity<Void> marcarTodasLidas(Authentication auth) {
        notificacaoService.marcarTodasComoLidas(usuarioId(auth));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir notificação (própria ou ADMIN)")
    public ResponseEntity<Void> deletar(@PathVariable Long id, Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));
        notificacaoService.deletar(id, usuarioId(auth), isAdmin);
        return ResponseEntity.noContent().build();
    }

    private Long usuarioId(Authentication auth) {
        return Long.parseLong(auth.getName());
    }
}
