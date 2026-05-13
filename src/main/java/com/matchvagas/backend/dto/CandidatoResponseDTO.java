package com.matchvagas.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CandidatoResponseDTO(
        Long id,
        String nome,
        String email,
        LocalDate dataNascimento,
        String cpf,
        String objetivoProfissional,
        String disponibilidade,
        BigDecimal pretensaoSalarial,
        TelefonesResponseDTO telefone,
        EnderecosResponseDTO localizacao
) {}
