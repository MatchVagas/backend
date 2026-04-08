package com.matchvagas.backend.dto;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record CandidaturaDto(
    Long id,
    Long candidatoId,
    Long vagaId,
    LocalDateTime dataCandidatura,
    String status,               
    String mensagemCandidato,
    String curriculoUrl,
    BigDecimal pretensaoSalarial,
    String disponibilidade,
    String etapaProcesso,
    String notasRecrutador
) {
  
}
