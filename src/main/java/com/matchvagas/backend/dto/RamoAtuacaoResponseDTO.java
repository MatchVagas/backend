package com.matchvagas.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RamoAtuacaoResponseDTO(
    Long id,
    String descricao
) {}
