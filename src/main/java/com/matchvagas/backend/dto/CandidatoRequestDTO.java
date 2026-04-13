package com.matchvagas.backend.dto;

import java.time.LocalDate;
import java.util.List;

import com.matchvagas.backend.entity.Telefones;

public record CandidatoRequestDTO(
    Long id,
    String nome,
    String email,
    List<Telefones> telefones,
    String cidade,
    String estado,
    LocalDate dataNascimento,
    String nivelFormacao,
    String resumoProfissional
) {
  
}
