package com.matchvagas.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EstadoResponseDTO(Long id, String nome, String uf, Long paisId, String paisNome) {}
