package com.matchvagas.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.matchvagas.backend.entity.NivelHabilidade;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record HabilidadeResponseDTO(
        String nome,
        NivelHabilidade nivel
) {}
