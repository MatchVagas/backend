package com.matchvagas.backend.dto;

public record EstadoRequestDTO(
    String nome,
    String uf,
    Integer paisId
) {}
