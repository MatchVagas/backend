package com.matchvagas.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdministradorResponseDTO(
    Long id,
    String nomeUsuario,
    String emailUsuario,
    String nivel,
    String departamento,
    String permissoes
) {}