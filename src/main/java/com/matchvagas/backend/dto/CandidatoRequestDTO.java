package com.matchvagas.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.br.CPF;

import java.math.BigDecimal;

public record CandidatoRequestDTO(

        @NotBlank(message = "CPF é obrigatório")
        @CPF(message = "CPF inválido")
        String cpf,

        String resumoProfissional,

        String disponibilidade,

        @DecimalMin(value = "0.0", message = "Pretensão salarial não pode ser negativa")
        BigDecimal pretensaoSalarial
) {}
