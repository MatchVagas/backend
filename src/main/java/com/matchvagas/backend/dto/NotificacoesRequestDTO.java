package com.matchvagas.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NotificacoesRequestDTO(

        @NotBlank(message = "Título é obrigatório")
        @Size(max = 200, message = "Título deve ter no máximo 200 caracteres")
        String titulo,              // era: @Email no título — incorreto, removido

        @NotBlank(message = "Mensagem é obrigatória")
        @Size(min = 10, message = "Mensagem deve ter no mínimo 10 caracteres")
        String mensagem,

        @NotNull(message = "ID do usuário destinatário é obrigatório")
        Long usuarioId              // era: List<String> notificacoes — campo sem sentido
) {}
