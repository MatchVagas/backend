package com.matchvagas.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record HistoricoStatusResponseDTO(
        Long id,
        String statusAnterior,   // nulo quando a candidatura ainda não tinha status
        String statusNovo,
        Long usuarioId,          // quem efetuou a mudança (recrutador/empresa)
        String usuarioNome,
        LocalDateTime dataHora
) {}
