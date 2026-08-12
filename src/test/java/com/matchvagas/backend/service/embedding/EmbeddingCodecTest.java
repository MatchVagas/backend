package com.matchvagas.backend.service.embedding;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddingCodecTest {
    @Test
    void serializaEDesserializaSemPerderValores() {
        float[] vetor = { 0.1f, -0.25f, 0.33333334f };
        assertThat(EmbeddingCodec.fromCsv(EmbeddingCodec.toCsv(vetor))).containsExactly(vetor);
    }
}
