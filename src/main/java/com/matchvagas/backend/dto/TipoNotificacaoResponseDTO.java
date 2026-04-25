package com.matchvagas.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TipoNotificacaoResponseDTO(
        Long id,        // era: Integer — corrigido para Long (padrão do projeto)
        String status
) {}
