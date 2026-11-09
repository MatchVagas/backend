package com.matchvagas.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record EscolaridadeRequestDTO(
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 50, message = "Nome deve ter no máximo 50 caracteres")
    String nome,
    @PositiveOrZero(message = "Ordem não pode ser negativa")
    Integer ordem
) {}
