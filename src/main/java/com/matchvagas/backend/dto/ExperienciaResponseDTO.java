package com.matchvagas.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExperienciaResponseDTO(
        Long id,
        Long candidatoId,
        String empresa,             // alinhado com entidade Experiencia
        String cargo,
        String descricao,
        String dataInicio,          // String — como está na entidade
        String dataFim
) {}
