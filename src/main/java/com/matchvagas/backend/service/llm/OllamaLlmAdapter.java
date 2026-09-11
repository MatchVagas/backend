package com.matchvagas.backend.service.llm;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

/** Adaptador para um LLM aberto servido pelo Ollama. O formato JSON é forçado nas opções (LlmConfig). */
public class OllamaLlmAdapter implements LlmPort {
    private final ChatModel chatModel;
    private final String modelo;

    public OllamaLlmAdapter(ChatModel chatModel, String modelo) {
        this.chatModel = chatModel;
        this.modelo = modelo;
    }

    @Override public boolean isAtivo() { return true; }
    @Override public String modelo() { return modelo; }

    @Override
    public String gerarJson(String sistema, String usuario) {
        ChatResponse resposta = chatModel.call(new Prompt(List.of(
                new SystemMessage(sistema), new UserMessage(usuario))));
        if (resposta == null || resposta.getResult() == null || resposta.getResult().getOutput() == null) {
            throw new IllegalStateException("Resposta vazia do Ollama.");
        }
        String texto = resposta.getResult().getOutput().getText();
        if (texto == null || texto.isBlank()) {
            throw new IllegalStateException("Resposta vazia do Ollama.");
        }
        return texto;
    }
}