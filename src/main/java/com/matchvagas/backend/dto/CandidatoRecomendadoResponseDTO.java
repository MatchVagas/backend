package com.matchvagas.backend.dto;

import java.math.BigDecimal;
import java.util.List;

/** Dados profissionais mínimos que um candidato autorizou expor para descoberta. */
public record CandidatoRecomendadoResponseDTO(
        Long candidatoId,
        String objetivoProfissional,
        String disponibilidade,
        BigDecimal pretensaoSalarial,
        List<String> habilidades,
        int pontuacao,
        List<String> motivos
) {}
