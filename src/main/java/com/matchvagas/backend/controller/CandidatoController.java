package com.matchvagas.backend.controller;

import com.matchvagas.backend.dto.CandidatoRequestDTO;
import com.matchvagas.backend.dto.CandidatoResponseDTO;
import com.matchvagas.backend.service.CandidatoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/candidatos")
@RequiredArgsConstructor
@Tag(name = "Candidatos", description = "Gerenciamento de perfil de candidatos")
public class CandidatoController {

    private final CandidatoService candidatoService;

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('CANDIDATO')")
    @Operation(summary = "Obter perfil do candidato autenticado")
    public ResponseEntity<CandidatoResponseDTO> getMyProfile(Authentication authentication) {
        Long usuarioId = Long.parseLong(authentication.getName());
        CandidatoResponseDTO response = candidatoService.findByUsuarioId(usuarioId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    @PreAuthorize("hasAuthority('CANDIDATO')")
    @Operation(summary = "Atualizar perfil do candidato autenticado")
    public ResponseEntity<CandidatoResponseDTO> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody CandidatoRequestDTO dto) {
        Long usuarioId = Long.parseLong(authentication.getName());
        CandidatoResponseDTO response = candidatoService.update(usuarioId, dto);
        return ResponseEntity.ok(response);
    }
}