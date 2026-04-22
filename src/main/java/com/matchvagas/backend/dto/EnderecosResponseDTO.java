package com.matchvagas.backend.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EnderecosResponseDTO(
    Long id,
    String logradouro,
    String numero,
    String estado,
    String cidade,
    String bairro,
    String cep,
    List<String> enderecos

) {
    
}