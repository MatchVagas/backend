package com.matchvagas.backend.dto;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record CandidaturaRequestDTO(
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
