package com.matchvagas.backend.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resolve o IP real do cliente de forma resistente a falsificação (SEC-06).
 *
 * <p>O header {@code X-Forwarded-For} é controlado pelo cliente: um atacante pode
 * enviar qualquer valor para variar o "IP" a cada requisição e furar o rate
 * limiting. A proteção é confiar apenas nos hops adicionados pelos proxies que
 * <b>nós</b> controlamos: cada proxy confiável <i>anexa</i> o IP de quem o chamou
 * ao final da lista. Logo, o IP do cliente real é o N-ésimo a partir da direita,
 * onde N = número de proxies confiáveis à frente da aplicação.
 *
 * <p>Configure {@code APP_TRUSTED_PROXY_COUNT}:
 * <ul>
 *   <li>{@code 0} — aplicação exposta diretamente: ignora X-Forwarded-For e usa o IP do socket;</li>
 *   <li>{@code 1} — um reverse proxy/balanceador à frente (ex.: Render) — padrão;</li>
 *   <li>{@code n} — n proxies confiáveis encadeados.</li>
 * </ul>
 */
@Component
public class ClientIpResolver {

    private final int trustedProxyCount;

    public ClientIpResolver(@Value("${app.trusted-proxy-count:1}") int trustedProxyCount) {
        this.trustedProxyCount = Math.max(0, trustedProxyCount);
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        if (trustedProxyCount == 0) {
            return remoteAddr;
        }

        String xff = request.getHeader("X-Forwarded-For");
        if (xff == null || xff.isBlank()) {
            return remoteAddr;
        }

        String[] hops = xff.split(",");
        // Pega o hop adicionado pelo proxy confiável mais distante (da direita p/ esquerda).
        int idx = Math.max(0, hops.length - trustedProxyCount);
        String ip = hops[idx].trim();
        return ip.isEmpty() ? remoteAddr : ip;
    }
}
