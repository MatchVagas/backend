package com.matchvagas.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmpresaResponseDTO(
    Long id,
    String cnpj,
    String razaoSocial,
    String nomeFantasia,
    String descricao,
    String porte,
    String ramoAtuacao,
    String site,
    Integer totalVagasAtivas
) {}