package com.matchvagas.backend.service;

import com.matchvagas.backend.exception.RateLimitException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter simples, em memória, por chave (tipicamente endpoint + IP de
 * origem). Protege os endpoints de autenticação contra força bruta (SEC-06)
 * usando uma janela fixa de tempo.
 *
 * <p>Implementação propositalmente leve, sem dependências externas. Para um
 * cenário multi-instância (escala horizontal), trocar por um backend
 * compartilhado (Redis + bucket4j).
 */
@Service
public class RateLimiterService {

    private static final int  MAX_TENTATIVAS   = 5;
    private static final long JANELA_MS        = 15 * 60 * 1000L; // 15 minutos
    private static final int  LIMITE_LIMPEZA   = 10_000;          // purga oportunista

    private final Map<String, Janela> janelas = new ConcurrentHashMap<>();

    /**
     * Registra uma tentativa para a chave e lança {@link RateLimitException}
     * se o limite da janela atual foi excedido.
     */
    public void verificar(String chave) {
        long agora = System.currentTimeMillis();

        Janela janela = janelas.compute(chave, (k, atual) -> {
            if (atual == null || agora - atual.inicio >= JANELA_MS) {
                return new Janela(agora);
            }
            atual.contador++;
            return atual;
        });

        if (janelas.size() > LIMITE_LIMPEZA) {
            janelas.entrySet().removeIf(e -> agora - e.getValue().inicio >= JANELA_MS);
        }

        if (janela.contador > MAX_TENTATIVAS) {
            throw new RateLimitException(
                    "Muitas tentativas. Aguarde alguns minutos antes de tentar novamente.");
        }
    }

    private static final class Janela {
        final long inicio;
        int contador;

        Janela(long inicio) {
            this.inicio = inicio;
            this.contador = 1;
        }
    }
}
