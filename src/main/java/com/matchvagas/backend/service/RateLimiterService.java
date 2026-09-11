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
        verificar(chave, MAX_TENTATIVAS, JANELA_MS);
    }

    /** Como {@link #verificar(String)}, com limite e janela próprios (ex.: cota do assistente de IA). */
    public void verificar(String chave, int maxTentativas, long janelaMs) {
        long agora = System.currentTimeMillis();

        Janela janela = janelas.compute(chave, (k, atual) -> {
            if (atual == null || agora - atual.inicio >= atual.duracaoMs) {
                return new Janela(agora, janelaMs);
            }
            atual.contador++;
            return atual;
        });

        if (janelas.size() > LIMITE_LIMPEZA) {
            janelas.entrySet().removeIf(e -> agora - e.getValue().inicio >= e.getValue().duracaoMs);
        }

        if (janela.contador > maxTentativas) {
            throw new RateLimitException(
                    "Muitas tentativas. Aguarde alguns minutos antes de tentar novamente.");
        }
    }

    private static final class Janela {
        final long inicio;
        final long duracaoMs;
        int contador;

        Janela(long inicio, long duracaoMs) {
            this.inicio = inicio;
            this.duracaoMs = duracaoMs;
            this.contador = 1;
        }
    }
}
