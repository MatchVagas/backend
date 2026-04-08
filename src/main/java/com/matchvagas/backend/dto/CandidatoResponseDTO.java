package com.matchvagas.backend.dto;

import java.time.LocalDate;
import java.util.List;

public record CandidatoResponse(
    Long id,
    String nome,
    String email,
    String telefone,
    String cidade,
    String estado,
    LocalDate dataNascimento,
    String nivelFormacao,
    String resumoProfissional,
    List<FormacaoDto> formacoes,
    List<ExperienciaDto> experiencias,
    List<String> habilidades,
    String fotoUrl,
    String perfilPublicoUrl
) {
  
}
