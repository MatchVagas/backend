package com.matchvagas.backend.service.embedding;

public interface EmbeddingPort {
    boolean isAtivo();
    float[] embed(String texto);
    int dimensao();
    String modelo();
}
