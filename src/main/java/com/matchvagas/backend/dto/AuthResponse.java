package com.matchvagas.backend.dto;

public record AuthResponse(
        String token,           // JWT Token
        String tipo,            // "Bearer"
        Long usuarioId,
        String nome,
        String email,
        String perfil          // CANDIDATO, EMPRESA ou ADMINISTRADOR
) {
}
