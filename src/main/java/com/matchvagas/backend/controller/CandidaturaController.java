package com.matchvagas.backend.controller;

import com.matchvagas.backend.dto.CandidaturaRequestDTO;
import com.matchvagas.backend.dto.CandidaturaResponseDTO;
import com.matchvagas.backend.service.CandidaturaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/candidaturas")
@RequiredArgsConstructor
public class CandidaturaController {

    private final CandidaturaService candidaturaService;

    @PostMapping
    public ResponseEntity<CandidaturaResponseDTO> candidatar(
            Authentication authentication,
            @Valid @RequestBody CandidaturaRequestDTO request) {

        // O subject do token JWT deve ser o ID do usuário
        Long candidatoId = Long.parseLong(authentication.getName());

        CandidaturaResponseDTO response = candidaturaService.candidatar(candidatoId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}