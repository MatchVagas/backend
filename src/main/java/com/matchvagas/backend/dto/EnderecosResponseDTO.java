package com.matchvagas.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EnderecosResponseDTO(
        Long id,
        String logradouro,
        String numero,
        String completo,
        String bairro,
        String cep,
        String cidade,              // nome da cidade
        String estado,              // nome do estado
        Long cidadeId               // ID para referência
) {}
