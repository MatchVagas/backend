package com.matchvagas.backend.dto;

import java.time.LocalDate;
import java.util.List;

import com.matchvagas.backend.entity.Experiencia;
import com.matchvagas.backend.entity.Formacao;

public record CandidatoResponseDTO(
    Long id,
    String nome,
    String email,
    String telefone,
    String cidade,
    String estado,
    LocalDate dataNascimento,
    String nivelFormacao,
    String resumoProfissional,
    List<Formacao> formacoes,
    List<Experiencia> experiencias,
    List<String> habilidades,
    String fotoUrl,
    String perfilPublicoUrl
) {
  
}
