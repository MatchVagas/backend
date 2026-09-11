package com.matchvagas.backend.service.llm;

public class DisabledLlmPort implements LlmPort {
    @Override public boolean isAtivo() { return false; }
    @Override public String modelo() { return "disabled"; }
    @Override public String gerarJson(String sistema, String usuario) {
        throw new IllegalStateException("LLM desativado (app.llm.enabled=false).");
    }
}