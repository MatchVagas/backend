package com.matchvagas.backend.dto;

import java.time.LocalDateTime;

public record VisibilidadeRecomendacoesResponseDTO(
        boolean disponivel,
        LocalDateTime consentimentoEm,
        LocalDateTime revogacaoEm
) {}
