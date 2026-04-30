package com.matchvagas.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TelefonesResponseDTO(
        Long id,
        String numero,              // era: String numeroTelefones — alinhado com entidade
        Long tipoTelefoneId,        // era: Integer — corrigido para Long
        String tipoTelefoneNome,    // ADICIONADO — mais útil que só o ID
        boolean wpp                 // era: String wppTelefones — corrigido para boolean
) {}
