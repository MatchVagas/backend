package com.matchvagas.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VagaResponseDTO(

        Long id,                    // ADICIONADO — necessário para editar/remover

        String nomeFantasiaEmpresa,
        Long empresaId,             // ADICIONADO — facilita vínculo no frontend

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

) {}
