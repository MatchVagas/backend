package com.matchvagas.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RamoAtuacaoRequestDTO(
    @NotBlank(message = "Descrição é obrigatória")
    @Size(max = 100, message = "Descrição deve ter no máximo 100 caracteres")
    String descricao
) {}
