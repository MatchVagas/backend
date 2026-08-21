package com.matchvagas.backend.dto;

import jakarta.validation.constraints.NotBlank;

/** Payload de confirmação de e-mail: o token recebido no link enviado por e-mail. */
public record ConfirmarEmailRequestDTO(
        @NotBlank(message = "O token de confirmação é obrigatório.")
        String token
) {}
