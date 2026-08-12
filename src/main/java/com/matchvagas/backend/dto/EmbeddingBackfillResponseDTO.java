package com.matchvagas.backend.dto;

public record EmbeddingBackfillResponseDTO(
        boolean embeddingsAtivos,
        int vagasProcessadas,
        int candidatosProcessados,
        int vetoresAtualizados
) {}
