package com.matchvagas.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Payload para reenviar o e-mail de verificação. */
public record ReenviarVerificacaoRequestDTO(
        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        String email
) {}
