package com.matchvagas.backend.service.embedding;

public class DisabledEmbeddingPort implements EmbeddingPort {
    @Override public boolean isAtivo() { return false; }
    @Override public int dimensao() { return 0; }
    @Override public String modelo() { return "disabled"; }
    @Override public float[] embed(String texto) {
        throw new IllegalStateException("Embeddings desativados (app.embeddings.enabled=false).");
    }
}
