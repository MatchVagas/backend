package com.matchvagas.backend.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Critérios de um alerta de vaga. Todos opcionais — {@code null} = "qualquer".
 * Um alerta sem nenhum critério casa com toda vaga nova.
 */
public record AlertaVagaRequestDTO(

        @Size(max = 100, message = "A área de atuação deve ter no máximo 100 caracteres")
        String areaAtuacao,

        Long cidadeId,

        Long modalidadeId,

        @PositiveOrZero(message = "O salário mínimo desejado não pode ser negativo")
        BigDecimal salarioMinimoDesejado,

        /** null é tratado como ativo=true na criação. */
        Boolean ativo
) {}
