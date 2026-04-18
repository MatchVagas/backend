package com.matchvagas.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record AlterarSenhaRequestDTO(
        @NotBlank(message = "Senha atual é obrigatória")
        String senhaAtual,

        @NotBlank(message = "Nova senha é obrigatória")
        String novaSenha
) {
}
