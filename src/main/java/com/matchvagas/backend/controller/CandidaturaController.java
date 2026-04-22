package com.matchvagas.backend.controller;

import com.matchvagas.backend.dto.CandidaturaRequestDTO;
import com.matchvagas.backend.dto.CandidaturaResponseDTO;
import com.matchvagas.backend.service.CandidaturaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/candidaturas")
@RequiredArgsConstructor
@Tag(name = "Candidaturas", description = "Candidatura e acompanhamento de vagas (RF008, RF009)")
public class CandidaturaController {

    private final CandidaturaService candidaturaService;

    // RF008 — Candidatar-se a uma vaga
    @PostMapping
    @Operation(summary = "Candidatar-se a uma vaga")
    public ResponseEntity<CandidaturaResponseDTO> candidatar(
            Authentication authentication,
            @Valid @RequestBody CandidaturaRequestDTO request) {
        Long candidatoId = Long.parseLong(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(candidaturaService.candidatar(candidatoId, request));
    }

    // RF009 — Listar candidaturas do candidato autenticado
    @GetMapping("/minhas")
    @Operation(summary = "Listar minhas candidaturas com status")
    public ResponseEntity<List<CandidaturaResponseDTO>> minhasCandidaturas(Authentication authentication) {
        Long candidatoId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(candidaturaService.findByCandidato(candidatoId));
    }

    // RF009 — Detalhe de uma candidatura específica
    @GetMapping("/{id}")
    @Operation(summary = "Detalhar uma candidatura")
    public ResponseEntity<CandidaturaResponseDTO> findById(
            @PathVariable Long id,
            Authentication authentication) {
        Long candidatoId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(candidaturaService.findByIdAndCandidato(id, candidatoId));
    }
}
