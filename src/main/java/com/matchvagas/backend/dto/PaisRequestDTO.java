package com.matchvagas.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaisRequestDTO(

        @NotBlank(message = "Nome do país é obrigatório")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String nome,

        @NotBlank(message = "Código ISO é obrigatório")
        @Size(min = 2, max = 2, message = "Código ISO deve ter exatamente 2 caracteres")
        String codigoIso
) {}
