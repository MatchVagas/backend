package com.matchvagas.backend.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;


@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificacoesResponseDTO(
    Long id,
    String titulo,
    String mensagem,
    List<String> notificacoes
) {
    
}

