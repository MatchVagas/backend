package com.matchvagas.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record ExperienciaRequestDTO(

        @NotBlank(message = "Empresa é obrigatória")
        String empresa,

        @NotBlank(message = "Cargo é obrigatório")
        String cargo,

        String descricao,

        @NotBlank(message = "Data de início é obrigatória")
        String dataInicio,

        String dataFim              // null se emprego atual
) {}
