package com.matchvagas.backend.service.embedding;

public final class EmbeddingCodec {
    private EmbeddingCodec() {}

    public static String toCsv(float[] vetor) {
        StringBuilder out = new StringBuilder(vetor.length * 10);
        for (int i = 0; i < vetor.length; i++) {
            if (i > 0) out.append(',');
            out.append(Float.toString(vetor[i]));
        }
        return out.toString();
    }

    public static float[] fromCsv(String csv) {
        if (csv == null || csv.isBlank()) return new float[0];
        String[] partes = csv.split(",");
        float[] vetor = new float[partes.length];
        for (int i = 0; i < partes.length; i++) vetor[i] = Float.parseFloat(partes[i]);
        return vetor;
    }
}
