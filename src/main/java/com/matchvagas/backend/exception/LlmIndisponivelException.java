package com.matchvagas.backend.exception;

/** O assistente de IA está desligado, fora do ar ou não produziu resposta válida (HTTP 503). */
public class LlmIndisponivelException extends RuntimeException {
    public LlmIndisponivelException(String message) { super(message); }
    public LlmIndisponivelException(String message, Throwable cause) { super(message, cause); }
}