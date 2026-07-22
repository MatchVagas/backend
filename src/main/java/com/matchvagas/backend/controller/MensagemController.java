package com.matchvagas.backend.controller;

import com.matchvagas.backend.dto.MensagemRequestDTO;
import com.matchvagas.backend.dto.MensagemResponseDTO;
import com.matchvagas.backend.dto.PageResponseDTO;
import com.matchvagas.backend.service.MensagemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mensagens")
@RequiredArgsConstructor
@Tag(name = "Mensagens", description = "Conversa empresa ↔ candidato no contexto de uma candidatura (Fase 2)")
public class MensagemController {

    private final MensagemService mensagemService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('CANDIDATO','EMPRESA')")
    @Operation(
        summary = "Enviar mensagem em uma candidatura",
        description = "Envia uma mensagem para a outra parte da conversa (candidato ou empresa). "
                    + "Só participantes da candidatura podem enviar; o destinatário é notificado."
    )
    public ResponseEntity<MensagemResponseDTO> enviar(
            Authentication auth,
            @Valid @RequestBody MensagemRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mensagemService.enviar(usuarioId(auth), dto));
    }

    @GetMapping("/candidatura/{candidaturaId}")
    @PreAuthorize("hasAnyAuthority('CANDIDATO','EMPRESA')")
    @Operation(summary = "Listar a conversa de uma candidatura (paginado, mais antigas primeiro)")
    public ResponseEntity<PageResponseDTO<MensagemResponseDTO>> listar(
            @PathVariable Long candidaturaId,
            Authentication auth,
            @PageableDefault(size = 30) Pageable pageable) {
        return ResponseEntity.ok(mensagemService.listar(usuarioId(auth), candidaturaId, pageable));
    }

    @GetMapping("/candidatura/{candidaturaId}/nao-lidas/contagem")
    @PreAuthorize("hasAnyAuthority('CANDIDATO','EMPRESA')")
    @Operation(summary = "Contar mensagens não lidas nesta conversa")
    public ResponseEntity<Map<String, Long>> contarNaoLidas(
            @PathVariable Long candidaturaId,
            Authentication auth) {
        return ResponseEntity.ok(Map.of("naoLidas",
                mensagemService.contarNaoLidas(usuarioId(auth), candidaturaId)));
    }

    @GetMapping("/nao-lidas/contagem")
    @PreAuthorize("hasAnyAuthority('CANDIDATO','EMPRESA')")
    @Operation(summary = "Contar todas as mensagens não lidas do usuário (badge global)")
    public ResponseEntity<Map<String, Long>> contarNaoLidasTotais(Authentication auth) {
        return ResponseEntity.ok(Map.of("naoLidas",
                mensagemService.contarNaoLidasTotais(usuarioId(auth))));
    }

    @PatchMapping("/candidatura/{candidaturaId}/lidas")
    @PreAuthorize("hasAnyAuthority('CANDIDATO','EMPRESA')")
    @Operation(summary = "Marcar como lidas as mensagens recebidas nesta conversa")
    public ResponseEntity<Map<String, Integer>> marcarComoLidas(
            @PathVariable Long candidaturaId,
            Authentication auth) {
        return ResponseEntity.ok(Map.of("marcadas",
                mensagemService.marcarComoLidas(usuarioId(auth), candidaturaId)));
    }

    private Long usuarioId(Authentication auth) {
        return Long.parseLong(auth.getName());
    }
}
