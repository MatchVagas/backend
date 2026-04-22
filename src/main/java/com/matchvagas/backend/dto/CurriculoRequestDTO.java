package com.matchvagas.backend.dto;

import java.math.BigInteger;

public record CurriculoRequestDTO(
    Long candidatoId,
    String nomeArquivo,
    String caminhoArquivo,
    String formatoArquivo
) {}
