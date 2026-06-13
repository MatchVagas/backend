package com.matchvagas.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TipoNotificacaoRequestDTO(

    @NotBlank(message = "Nome do tipo de notificação é obrigatório")
    @Size(max = 50, message = "Nome deve ter no máximo 50 caracteres")
    String status
) {}
