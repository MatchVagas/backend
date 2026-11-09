package com.matchvagas.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EstadoRequestDTO(
        @NotBlank(message = "Nome do estado é obrigatório")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String nome,
        @NotBlank(message = "UF é obrigatória")
        @Size(min = 2, max = 2, message = "UF deve ter exatamente 2 caracteres")
        String uf,
        @NotNull(message = "País é obrigatório")
        Long paisId
) {}
