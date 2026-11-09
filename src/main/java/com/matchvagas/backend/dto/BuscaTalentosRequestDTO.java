package com.matchvagas.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Perfil ideal descrito livremente pelo recrutador, sem precisar de uma vaga cadastrada. Nada é salvo. */
public record BuscaTalentosRequestDTO(
        // Ex.: "Desenvolvedor Java sênior com Spring e AWS, para trabalho remoto".
        @NotBlank @Size(max = 3000) String descricao,
        @Size(max = 255) String titulo,
        @Size(max = 100) String areaAtuacao,
        @Size(max = 2000) String requisitos,
        @DecimalMin("0.0") BigDecimal salarioMinimo,
        @DecimalMin("0.0") BigDecimal salarioMaximo
) {}
