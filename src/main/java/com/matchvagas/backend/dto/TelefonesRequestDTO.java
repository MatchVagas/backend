package com.matchvagas.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TelefonesRequestDTO(

        @NotBlank(message = "Número de telefone é obrigatório")
        @Size(max = 20, message = "Número deve ter no máximo 20 caracteres")
        @Pattern(regexp = "^\\+?[0-9\\s\\-\\(\\)]+$", message = "Número de telefone inválido")
        String numero,              // era: Long numeroId — corrigido para String

        @NotNull(message = "Tipo de telefone é obrigatório")
        Long tipoTelefoneId,        // era: String nivel — corrigido para o ID correto

        boolean wpp                 // era: boolean wppId — removido o sufixo Id
) {}
