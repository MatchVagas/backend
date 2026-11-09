package com.matchvagas.backend.service.embedding;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Fase 3 — adaptador do modelo de embeddings")
class TransformersEmbeddingAdapterTest {

    @Mock EmbeddingModel model;

    @Test
    @DisplayName("normaliza o vetor para norma 1")
    void normaliza() {
        when(model.embed("texto")).thenReturn(new float[]{3f, 4f});
        var adapter = new TransformersEmbeddingAdapter(model, 2, "modelo");

        float[] vetor = adapter.embed("texto");

        assertThat(vetor[0]).isCloseTo(0.6f, within(1e-6f));
        assertThat(vetor[1]).isCloseTo(0.8f, within(1e-6f));
        assertThat(adapter.isAtivo()).isTrue();
        assertThat(adapter.modelo()).isEqualTo("modelo");
        assertThat(adapter.dimensao()).isEqualTo(2);
    }

    @Test
    @DisplayName("texto nulo é enviado como vazio e vetor zero é devolvido como está")
    void textoNuloVetorZero() {
        when(model.embed("")).thenReturn(new float[]{0f, 0f});

        assertThat(new TransformersEmbeddingAdapter(model, 2, "m").embed(null)).containsExactly(0f, 0f);
    }

    @Test
    @DisplayName("dimensão diferente da configurada é erro")
    void dimensaoErrada() {
        when(model.embed("t")).thenReturn(new float[]{1f, 2f, 3f});

        assertThatThrownBy(() -> new TransformersEmbeddingAdapter(model, 384, "m").embed("t"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("384");
    }

    @Test
    @DisplayName("porta desligada não gera vetor")
    void portaDesligada() {
        var desligada = new DisabledEmbeddingPort();

        assertThat(desligada.isAtivo()).isFalse();
        assertThatThrownBy(() -> desligada.embed("x")).isInstanceOf(IllegalStateException.class);
    }
}
