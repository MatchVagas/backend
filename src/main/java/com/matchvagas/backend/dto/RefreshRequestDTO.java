package com.matchvagas.backend.dto;

import jakarta.validation.constraints.NotBlank;

/** Payload para renovar o access token (ou revogar a sessão no logout). */
public record RefreshRequestDTO(
        @NotBlank(message = "O refresh token é obrigatório.")
        String refreshToken
) {}
