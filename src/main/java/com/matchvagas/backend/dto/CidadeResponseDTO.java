package com.matchvagas.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CidadeResponseDTO(Long id, String nome, Long estadoId, String estadoNome, String ufEstado) {}
