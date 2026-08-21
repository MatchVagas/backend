package com.matchvagas.backend.dto;

public record AuthResponse(
        String token,           // JWT Token (access token, curta duração)
        String tipo,            // "Bearer"
        Long usuarioId,
        String nome,
        String email,
        String perfil,          // CANDIDATO, EMPRESA ou ADMINISTRADOR
        String refreshToken     // token de renovação (longa duração, revogável)
) {
}
