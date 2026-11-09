package com.matchvagas.backend.security;

import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.entity.Usuarios.TipoUsuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Segurança — emissão e validação do JWT")
class JwtTokenProviderTest {

    private static final String SEGREDO = "test-only-jwt-secret-com-mais-de-32-bytes-para-hs256";

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = provider(SEGREDO, 60_000);
    }

    private JwtTokenProvider provider(String segredo, long expiracaoMs) {
        JwtTokenProvider p = new JwtTokenProvider();
        ReflectionTestUtils.setField(p, "jwtSecret", segredo);
        ReflectionTestUtils.setField(p, "jwtExpirationInMs", expiracaoMs);
        return p;
    }

    private Usuarios usuario(TipoUsuario tipo) {
        Usuarios u = new Usuarios();
        u.setId(42L);
        u.setEmail("ana@exemplo.com");
        u.setNome("Ana");
        u.setTipoUsuario(tipo);
        return u;
    }

    @Test
    @DisplayName("token gerado carrega id no subject e e-mail, e é válido")
    void geraTokenValido() {
        String token = provider.generateToken(usuario(TipoUsuario.EMPRESA));

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUserIdFromToken(token)).isEqualTo("42");
        assertThat(provider.getEmailFromToken(token)).isEqualTo("ana@exemplo.com");
        assertThat(provider.isTokenExpired(token)).isFalse();
    }

    @Test
    @DisplayName("aceita segredo em Base64 (formato de produção)")
    void segredoBase64() {
        String base64 = Base64.getEncoder().encodeToString(new byte[64]);
        JwtTokenProvider p = provider(base64, 60_000);

        assertThat(p.validateToken(p.generateToken(usuario(TipoUsuario.ADMIN)))).isTrue();
    }

    @Test
    @DisplayName("token expirado é inválido")
    void tokenExpirado() {
        String token = provider(SEGREDO, -1_000).generateToken(usuario(TipoUsuario.CANDIDATO));

        assertThat(provider.validateToken(token)).isFalse();
        assertThat(provider.isTokenExpired(token)).isTrue();
    }

    @Test
    @DisplayName("token malformado ou vazio é inválido")
    void tokenMalformado() {
        assertThat(provider.validateToken("nao.e.jwt")).isFalse();
        assertThat(provider.validateToken("")).isFalse();
        assertThat(provider.isTokenExpired("lixo")).isTrue();
    }

    @Test
    @DisplayName("token assinado com outra chave (forjado) é inválido, sem lançar exceção")
    void tokenForjado() {
        String forjado = Jwts.builder()
                .subject("1")
                .claim("email", "admin@exemplo.com")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor("outra-chave-qualquer-com-mais-de-32-bytes!!"
                        .getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();

        assertThat(provider.validateToken(forjado)).isFalse();
    }
}
