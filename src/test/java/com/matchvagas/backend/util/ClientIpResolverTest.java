package com.matchvagas.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SEC-06 — Resolução do IP real do cliente")
class ClientIpResolverTest {

    private MockHttpServletRequest request(String remoteAddr, String xff) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        if (xff != null) request.addHeader("X-Forwarded-For", xff);
        return request;
    }

    @Test
    @DisplayName("sem proxy confiável ignora X-Forwarded-For forjado")
    void semProxyIgnoraXff() {
        assertThat(new ClientIpResolver(0).resolve(request("10.0.0.1", "6.6.6.6")))
                .isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("com um proxy usa o último hop, adicionado pelo proxy")
    void umProxyUsaUltimoHop() {
        assertThat(new ClientIpResolver(1).resolve(request("10.0.0.1", "6.6.6.6, 200.1.1.1")))
                .isEqualTo("200.1.1.1");
    }

    @Test
    @DisplayName("com dois proxies usa o penúltimo hop")
    void doisProxies() {
        assertThat(new ClientIpResolver(2).resolve(request("10.0.0.1", "6.6.6.6, 200.1.1.1, 172.16.0.1")))
                .isEqualTo("200.1.1.1");
    }

    @Test
    @DisplayName("mais proxies configurados que hops usa o primeiro hop")
    void maisProxiesQueHops() {
        assertThat(new ClientIpResolver(5).resolve(request("10.0.0.1", "200.1.1.1")))
                .isEqualTo("200.1.1.1");
    }

    @Test
    @DisplayName("sem X-Forwarded-For, ou com hop vazio, usa o IP do socket")
    void semXffUsaSocket() {
        assertThat(new ClientIpResolver(1).resolve(request("10.0.0.1", null))).isEqualTo("10.0.0.1");
        assertThat(new ClientIpResolver(1).resolve(request("10.0.0.1", "  "))).isEqualTo("10.0.0.1");
        assertThat(new ClientIpResolver(1).resolve(request("10.0.0.1", "1.1.1.1, "))).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("contagem negativa é tratada como zero")
    void contagemNegativa() {
        assertThat(new ClientIpResolver(-3).resolve(request("10.0.0.1", "6.6.6.6"))).isEqualTo("10.0.0.1");
    }
}
