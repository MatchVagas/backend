 package com.matchvagas.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VagaResponseDTO(
        
        String nomeFantasiaEmpresa,

        String titulo,
        String descricao,
        String requisitos,
        String beneficios,

        Long tipoVagaId,
        String tipoVagaDescricao,

        Long modalidadeId,
        String modalidadeDescricao,

        BigDecimal salarioMinimo,
        BigDecimal salarioMaximo,

        String cargaHoraria,
        Integer idadeMinima,
        Integer idadeMaxima,

        Long escolaridadeId,
        String escolaridadeNome,

        String areaAtuacao,

        LocalDateTime dataPublicacao,
        LocalDateTime dataExpiracao,

        Long statusVagaId,
        String statusDescricao,

        Integer numeroVagas,

        Long cidadeId,
        String nomeCidade,
        String ufEstado

) {
}