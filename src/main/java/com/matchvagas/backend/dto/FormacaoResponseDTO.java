package com.matchvagas.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FormacaoResponseDTO(
        Long id,
        Long candidatoId,
        String instituicao,
        String curso,
        String nivel,
        String dataInicio,
        String dataFim
) {}
