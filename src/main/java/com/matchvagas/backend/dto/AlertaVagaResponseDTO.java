package com.matchvagas.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AlertaVagaResponseDTO(
        Long id,
        String areaAtuacao,
        Long cidadeId,
        Long modalidadeId,
        BigDecimal salarioMinimoDesejado,
        boolean ativo,
        LocalDateTime dataCriacao
) {}
