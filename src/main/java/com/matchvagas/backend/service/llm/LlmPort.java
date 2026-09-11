package com.matchvagas.backend.service.llm;

/**
 * Porta para o LLM generativo dos recursos assistivos. O resto do código depende só
 * desta interface — trocar Ollama por outro provedor é escrever um novo adaptador.
 */
public interface LlmPort {
    /** true quando há um modelo configurado (app.llm.enabled=true). */
    boolean isAtivo();

    String modelo();

    /** Gera uma resposta que deve conter um objeto JSON. Lança se !isAtivo() ou se o provedor falhar. */
    String gerarJson(String sistema, String usuario);
}