package com.matchvagas.backend.service.embedding;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmbeddingCodecTest {
    @Test
    void serializaEDesserializaSemPerderValores() {
        float[] vetor = { 0.1f, -0.25f, 0.33333334f };
        assertThat(EmbeddingCodec.fromCsv(EmbeddingCodec.toCsv(vetor))).containsExactly(vetor);
    }

    @Test
    void calculaSimilaridadeDeCosseno() {
        assertThat(EmbeddingCodec.cosseno(new float[]{1, 0}, new float[]{1, 0})).isEqualTo(1.0);
        assertThat(EmbeddingCodec.cosseno(new float[]{1, 0}, new float[]{0, 1})).isEqualTo(0.0);
        assertThat(EmbeddingCodec.cosseno(new float[]{1, 0}, new float[]{-1, 0})).isEqualTo(-1.0);
    }

    @Test
    void rejeitaDimensoesDiferentes() {
        assertThatThrownBy(() -> EmbeddingCodec.cosseno(new float[]{1}, new float[]{1, 2}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
