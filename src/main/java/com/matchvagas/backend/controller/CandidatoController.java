package com.matchvagas.backend.controller;

import com.matchvagas.backend.dto.CandidatoRequestDTO;
import com.matchvagas.backend.dto.CandidatoResponseDTO;
import com.matchvagas.backend.service.CandidatoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/candidatos")
@RequiredArgsConstructor
@Tag(name = "Candidatos", description = "Gerenciamento de perfil do candidato (RF003)")
public class CandidatoController {

    private final CandidatoService candidatoService;

    // RF003 — Visualizar próprio perfil
    @GetMapping("/meu-perfil")
    @Operation(summary = "Visualizar meu perfil de candidato")
    public ResponseEntity<CandidatoResponseDTO> meuPerfil(Authentication authentication) {
        Long usuarioId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(candidatoService.findByUsuarioId(usuarioId));
    }

    // RF003 — Criar perfil de candidato
    @PostMapping
    @Operation(summary = "Criar perfil de candidato")
    public ResponseEntity<CandidatoResponseDTO> create(
            Authentication authentication,
            @Valid @RequestBody CandidatoRequestDTO dto) {
        Long usuarioId = Long.parseLong(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(candidatoService.create(usuarioId, dto));
    }

    // RF003 — Atualizar perfil de candidato
    @PutMapping("/meu-perfil")
    @Operation(summary = "Atualizar meu perfil de candidato")
    public ResponseEntity<CandidatoResponseDTO> update(
            Authentication authentication,
            @Valid @RequestBody CandidatoRequestDTO dto) {
        Long usuarioId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(candidatoService.update(usuarioId, dto));
    }
}
