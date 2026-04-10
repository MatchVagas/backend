package com.matchvagas.backend.dto;

import java.time.LocalDate;

public record CandidatoRequestDTO(
    Long id,
    String nome,
    String email,
    String telefone,
    String cidade,
    String estado,
    LocalDate dataNascimento,
    String nivelFormacao,
    String resumoProfissional
) {
  
}
