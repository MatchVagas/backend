package com.matchvagas.backend.service.embedding;

import org.springframework.ai.embedding.EmbeddingModel;

public class TransformersEmbeddingAdapter implements EmbeddingPort {
    private final EmbeddingModel model;
    private final int dimensao;
    private final String nomeModelo;

    public TransformersEmbeddingAdapter(EmbeddingModel model, int dimensao, String nomeModelo) {
        this.model = model;
        this.dimensao = dimensao;
        this.nomeModelo = nomeModelo;
    }

    @Override public boolean isAtivo() { return true; }
    @Override public int dimensao() { return dimensao; }
    @Override public String modelo() { return nomeModelo; }

    @Override
    public float[] embed(String texto) {
        float[] vetor = model.embed(texto == null ? "" : texto);
        if (vetor.length != dimensao) {
            throw new IllegalStateException("Dimensão inesperada: " + vetor.length + "; configurada: " + dimensao);
        }
        double norma = 0;
        for (float valor : vetor) norma += (double) valor * valor;
        if (norma == 0) return vetor;
        norma = Math.sqrt(norma);
        float[] normalizado = new float[vetor.length];
        for (int i = 0; i < vetor.length; i++) normalizado[i] = (float) (vetor[i] / norma);
        return normalizado;
    }
}
