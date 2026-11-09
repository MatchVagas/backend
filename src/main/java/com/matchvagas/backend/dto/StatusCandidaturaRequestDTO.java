package com.matchvagas.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StatusCandidaturaRequestDTO(
    @NotBlank(message = "Status é obrigatório")
    @Size(max = 50, message = "Status deve ter no máximo 50 caracteres")
    String status
) {}
