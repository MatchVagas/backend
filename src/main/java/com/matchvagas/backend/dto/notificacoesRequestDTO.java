package com.matchvagas.backend.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record NotificacoesRequestDTO(
        
        @NotBlank(message = "titulo é obrigatório") 
        @Email(message = "titulo deve ser válido") 
        String titulo,

        @NotBlank(message = "Mensagem é obrigatória") 
        @Size(min = 50, message = "Mensagem deve ter no mínimo 50 caracteres") 
        String mensagem,
        List<String> notificacoes

        
       ) {}
