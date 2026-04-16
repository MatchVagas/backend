package com.matchvagas.backend.security;

import com.matchvagas.backend.entity.Usuarios;
import io.jsonwebtoken.*;
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
     * Gera o SecretKey a partir da string configurada
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Gera o Token JWT para o usuário
     */
    public String generateToken(Usuarios usuario) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .subject(usuario.getEmail())                    // Principal claim
                .claim("id", usuario.getId())
                .claim("nome", usuario.getNome())
                .claim("perfil", determinarPerfil(usuario))     // Pode ser melhorado depois
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS512)      // Algoritmo HS512 (mais seguro)
                .compact();
    }

    /**
     * Extrai o email (subject) do token
     */
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    /**
     * Extrai o ID do usuário do token
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("id", Long.class);
    }

    /**
     * Valida se o token é válido
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            System.out.println("Token expirado: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.out.println("Token não suportado: " + e.getMessage());
        } catch (MalformedJwtException e) {
            System.out.println("Token malformado: " + e.getMessage());
        } catch (SignatureException e) {
            System.out.println("Assinatura inválida: " + e.getMessage());
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
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Método auxiliar para determinar o perfil (pode ser melhorado futuramente)
     */
    private String determinarPerfil(Usuarios usuario) {
        // TODO: Melhorar essa lógica quando implementar Roles ou verificação em Candidatos/Empresas
        return "USER"; // Temporário
    }
}