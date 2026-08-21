package com.matchvagas.backend.dto;

import java.math.BigInteger;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CurriculoResponseDTO(
    Long id,
    Long candidatoId,
    String nomeArquivo,
    String caminhoArquivo,
    LocalDateTime dataUpload,
    BigInteger tamanhoArquivo,
    String formatoArquivo,
    String urlArquivo,
    LocalDateTime parseadoEm,
    String dadosEstruturados
) {}
