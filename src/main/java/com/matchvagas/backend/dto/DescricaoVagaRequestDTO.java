package com.matchvagas.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Dados do formulário de vaga usados para rascunhar descrição e requisitos. */
public record DescricaoVagaRequestDTO(
        @NotBlank @Size(max = 255) String titulo,
        @NotBlank @Size(max = 100) String areaAtuacao,
        @Size(max = 100) String tipoVaga,
        @Size(max = 100) String modalidade,
        @Size(max = 100) String escolaridadeMinima,
        @Size(max = 50) String cargaHoraria,
        @Size(max = 150) String cidade,
        BigDecimal salarioMinimo,
        BigDecimal salarioMaximo,
        @Size(max = 2000) String beneficios,
        // Ex.: stack, tamanho do time, desafios. É o que mais melhora o rascunho.
        @Size(max = 2000) String informacoesAdicionais
) {}