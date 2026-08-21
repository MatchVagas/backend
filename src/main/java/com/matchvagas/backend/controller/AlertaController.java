package com.matchvagas.backend.controller;

import com.matchvagas.backend.dto.AlertaVagaRequestDTO;
import com.matchvagas.backend.dto.AlertaVagaResponseDTO;
import com.matchvagas.backend.service.AlertaVagaService;
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

@RestController
@RequestMapping("/api/alertas")
@RequiredArgsConstructor
@Tag(name = "Alertas de Vaga", description = "Alertas por critérios; notificam o candidato quando surge vaga compatível (Fase 2)")
public class AlertaController {

    private final AlertaVagaService alertaService;

    @PostMapping
    @PreAuthorize("hasAuthority('CANDIDATO')")
    @Operation(
        summary = "Criar um alerta de vaga por critérios",
        description = "Critérios são opcionais (null = qualquer). Um alerta sem critérios notifica toda vaga nova."
    )
    public ResponseEntity<AlertaVagaResponseDTO> criar(
            Authentication auth,
            @Valid @RequestBody AlertaVagaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alertaService.criar(usuarioId(auth), dto));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CANDIDATO')")
    @Operation(summary = "Listar meus alertas de vaga")
    public ResponseEntity<List<AlertaVagaResponseDTO>> listar(Authentication auth) {
        return ResponseEntity.ok(alertaService.listar(usuarioId(auth)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('CANDIDATO')")
    @Operation(summary = "Ativar/desativar um alerta")
    public ResponseEntity<AlertaVagaResponseDTO> alternarAtivo(
            @PathVariable Long id,
            @RequestParam boolean ativo,
            Authentication auth) {
        return ResponseEntity.ok(alertaService.alternarAtivo(usuarioId(auth), id, ativo));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CANDIDATO')")
    @Operation(summary = "Remover um alerta")
    public ResponseEntity<Void> remover(@PathVariable Long id, Authentication auth) {
        alertaService.remover(usuarioId(auth), id);
        return ResponseEntity.noContent().build();
    }

    private Long usuarioId(Authentication auth) {
        return Long.parseLong(auth.getName());
    }
}
