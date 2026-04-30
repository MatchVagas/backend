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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/candidaturas")
@RequiredArgsConstructor
@Tag(name = "Candidaturas", description = "Candidatura e acompanhamento de vagas (RF008, RF009)")
public class CandidaturaController {

    private final CandidaturaService candidaturaService;

    // RF008 — Candidatar-se a uma vaga (somente CANDIDATO)
    @PostMapping
    @PreAuthorize("hasAuthority('CANDIDATO')")
    @Operation(summary = "Candidatar-se a uma vaga")
    public ResponseEntity<CandidaturaResponseDTO> candidatar(
            Authentication authentication,
            @Valid @RequestBody CandidaturaRequestDTO request) {
        Long candidatoId = Long.parseLong(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(candidaturaService.candidatar(candidatoId, request));
    }

    // RF009 — Listar minhas candidaturas (CANDIDATO)
    @GetMapping("/minhas")
    @PreAuthorize("hasAuthority('CANDIDATO')")
    @Operation(summary = "Listar minhas candidaturas com status")
    public ResponseEntity<List<CandidaturaResponseDTO>> minhasCandidaturas(Authentication authentication) {
        Long candidatoId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(candidaturaService.findByCandidato(candidatoId));
    }

    // RF009 — Detalhar uma candidatura (CANDIDATO)
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CANDIDATO')")
    @Operation(summary = "Detalhar uma candidatura")
    public ResponseEntity<CandidaturaResponseDTO> findById(
            @PathVariable Long id,
            Authentication authentication) {
        Long candidatoId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(candidaturaService.findByIdAndCandidato(id, candidatoId));
    }

    // Empresa — ver candidaturas recebidas nas vagas da minha empresa
    @GetMapping("/empresa")
    @PreAuthorize("hasAuthority('EMPRESA') or hasAuthority('ADMIN')")
    @Operation(summary = "Listar candidaturas recebidas nas vagas da minha empresa")
    public ResponseEntity<List<CandidaturaResponseDTO>> candidaturasEmpresa(Authentication authentication) {
        Long usuarioId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(candidaturaService.findByEmpresa(usuarioId));
    }
}
