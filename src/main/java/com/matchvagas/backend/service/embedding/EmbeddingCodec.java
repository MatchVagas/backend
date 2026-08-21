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

    public static double cosseno(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || a.length != b.length) {
            throw new IllegalArgumentException("Vetores devem ser não vazios e ter a mesma dimensão");
        }

        double produtoEscalar = 0.0;
        double normaA = 0.0;
        double normaB = 0.0;
        for (int i = 0; i < a.length; i++) {
            produtoEscalar += (double) a[i] * b[i];
            normaA += (double) a[i] * a[i];
            normaB += (double) b[i] * b[i];
        }
        if (normaA == 0.0 || normaB == 0.0) return 0.0;
        return produtoEscalar / (Math.sqrt(normaA) * Math.sqrt(normaB));
    }
}
