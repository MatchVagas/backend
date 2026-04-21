package com.matchvagas.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UsuarioResponseDTO(
    Long id,
    String nome,
    String email,
    Integer idade,
    Boolean ativo,
    LocalDateTime dataCriacao,
    LocalDateTime dataAtualizacao,
    List<TelefonesResponseDTO> telefones

) {
}
