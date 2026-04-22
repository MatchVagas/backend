package com.matchvagas.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record CandidaturaRequestDTO(
        @NotNull(message = "ID da vaga é obrigatório")
        Long vagaId
) {

}
