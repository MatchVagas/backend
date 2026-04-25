package com.matchvagas.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificacoesResponseDTO(
        Long id,
        String titulo,
        String mensagem,
        String tipoNotificacao,     // nome do tipo
        LocalDateTime dataEnvio,    // ADICIONADO — campo existe na entidade
        Boolean lida,               // ADICIONADO — campo existe na entidade
        Long usuarioId              // ADICIONADO — destinatário
) {}
