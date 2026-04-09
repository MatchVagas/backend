package com.matchvagas.backend.dto;

import java.time.LocalDate;
import java.util.List;

public record ExperienciaResponseDTO(
    Long id,
    Long candidatoId,
    String cargo,
    String empresa,
    String localTrabalho,
    LocalDate dataInicio,
    LocalDate dataFim,
    Boolean atual,
    String tipoContrato,
    String descricao,
    List<String> responsabilidades,
    List<String> tecnologias,
    Double salario,
    String comprovanteUrl
) {
  
}
