package com.matchvagas.backend.dto;

import com.matchvagas.backend.entity.NivelHabilidade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HabilidadeRequestDTO(

        @NotBlank(message = "Nome da habilidade é obrigatório")
        @Size(max = 100, message = "Nome da habilidade deve ter no máximo 100 caracteres")
        String nome,

        NivelHabilidade nivel
) {}
