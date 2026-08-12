package com.matchvagas.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

public record CurriculoParseResult(
        @JsonIgnore
        String texto,
        String nome,
        String email,
        String telefone,
        List<String> competencias,
        List<String> formacoes,
        List<String> experiencias
) {}
