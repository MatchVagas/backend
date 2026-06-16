package com.matchvagas.backend.exception;

/**
 * Lançada quando um cliente excede o limite de requisições permitido em uma
 * janela de tempo (SEC-06). Mapeada para HTTP 429 (Too Many Requests).
 */
public class RateLimitException extends RuntimeException {
    public RateLimitException(String message) {
        super(message);
    }
}
