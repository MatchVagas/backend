package com.matchvagas.backend.dto;

import java.time.LocalDate;

public record CandidatoDto(
    Long id,
    String nome,
    String email,
    String telefone,
    String cidade,
    String estado,
    LocalDate dataNascimento,
    String nivelFormacao,
    String resumoProfissional,
    String linkedinUrl
) {
  
}
