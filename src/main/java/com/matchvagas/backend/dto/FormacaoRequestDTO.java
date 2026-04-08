package com.matchvagas.backend.dto;

import java.time.LocalDate;

public record FormacaoDto(
    Long id,
    Long candidatoId,
    String tipoFormacao,
    String curso,
    String instituicao,
    LocalDate dataInicio,
    LocalDate dataFim,
    String grau,            
    String descricao,
    String certificadoUrl,
    Integer cargaHoraria,
    Double notaMedia,
    Boolean concluido
) {
  
}
