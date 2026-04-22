package com.matchvagas.backend.dto;

import java.math.BigDecimal;

public record CandidatoRequestDTO(
        String cpf,
        String resumoProfissional,
        String disponibilidade,
        BigDecimal pretensaoSalarial
) {}
