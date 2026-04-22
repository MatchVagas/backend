package com.matchvagas.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record CandidaturaResponseDTO(
        Long id,
        Long candidatoId,
        String nomeCandidato,
        Long vagaId,
        String tituloVaga,
        LocalDateTime dataCandidatura,
        String status
) {
}
