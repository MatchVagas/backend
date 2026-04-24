package com.matchvagas.backend.security;

import com.matchvagas.backend.entity.Usuarios;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // 24 horas por padrão
    private long jwtExpirationInMs;

    /**
     * Gera o SecretKey a partir da string configurada.
     * Usa UTF-8 explícito para garantir >= 256 bits.
     * Se a chave estiver em Base64 no properties, usa Decoders.BASE64.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes;
        try {
            // Tenta decodificar como Base64 primeiro (chave de produção)
            keyBytes = Decoders.BASE64.decode(jwtSecret);
        } catch (Exception e) {
            // Fallback: usa os bytes UTF-8 direto (desenvolvimento)
            keyBytes = jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Gera o Token JWT para o usuário
     */
    public String generateToken(Usuarios usuario) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .subject(String.valueOf(usuario.getId()))       // ID como subject (para extrair no filter)
                .claim("email", usuario.getEmail())
                .claim("nome", usuario.getNome())
                .claim("perfil", usuario.getTipoUsuario() != null
                        ? usuario.getTipoUsuario().name()
                        : "CANDIDATO")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)      // HS256 exige >= 256 bits (32 chars)
                .compact();
    }

    /**
     * Extrai o ID do usuário (subject) do token
     */
    public String getUserIdFromToken(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Extrai o email do token
     */
    public String getEmailFromToken(String token) {
        return getClaims(token).get("email", String.class);
    }

    /**
     * Valida se o token é válido
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            System.out.println("Token expirado: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.out.println("Token não suportado: " + e.getMessage());
        } catch (MalformedJwtException e) {
            System.out.println("Token malformado: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Token vazio ou inválido: " + e.getMessage());
        }
        return false;
    }

    /**
     * Verifica se o token está expirado
     */
    public boolean isTokenExpired(String token) {
        try {
            return getClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
