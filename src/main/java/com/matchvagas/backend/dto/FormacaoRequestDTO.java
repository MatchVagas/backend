package com.matchvagas.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record FormacaoRequestDTO(

        @NotBlank(message = "Instituição é obrigatória")
        String instituicao,

        @NotBlank(message = "Curso é obrigatório")
        String curso,

        @NotBlank(message = "Nível é obrigatório")
        String nivel,               // campo real da entidade Formacao

        @NotBlank(message = "Data de início é obrigatória")
        String dataInicio,          // String — como está na entidade

        String dataFim
) {}
