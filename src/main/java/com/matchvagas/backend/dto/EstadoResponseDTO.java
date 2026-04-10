package com.matchvagas.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EstadoResponseDTO(
    Integer id,
    String nome,
    String uf,
    Integer paisId,
    String paisNome
) {}
