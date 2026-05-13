package com.matchvagas.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import org.hibernate.validator.constraints.br.CPF;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CandidatoRequestDTO(

        @NotBlank(message = "CPF é obrigatório")
        @CPF(message = "CPF inválido")
        String cpf,

        String nomeCompleto,

        @Email(message = "Email inválido")
        String email,

        @Past(message = "Data de nascimento deve ser no passado")
        LocalDate dataNascimento,

        String resumoProfissional,

        String disponibilidade,

        @DecimalMin(value = "0.0", message = "Pretensão salarial não pode ser negativa")
        BigDecimal pretensaoSalarial,

        @Valid
        TelefonesRequestDTO telefone,

        @Valid
        LocalizacaoRequestDTO localizacao
) {}
