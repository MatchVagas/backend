package com.matchvagas.backend.dto;

import jakarta.validation.constraints.NotNull;

public record VisibilidadeRecomendacoesRequestDTO(
        @NotNull Boolean disponivel
) {}
