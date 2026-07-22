package com.matchvagas.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MensagemRequestDTO(

        @NotNull(message = "ID da candidatura é obrigatório")
        Long candidaturaId,

        @NotBlank(message = "A mensagem não pode estar vazia")
        @Size(max = 2000, message = "A mensagem deve ter no máximo 2000 caracteres")
        String conteudo
) {}
