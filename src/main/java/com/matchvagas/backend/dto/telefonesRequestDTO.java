package com.matchvagas.backend.dto;
import jakarta.validation.constrations.NotBlank;
import jakarta.validation.constrations.NotNull;
import jakarta.validation.constrations.Size;

public recort TelefonesRequestDTO(
    @NotNull(message = "ID do numero é obrigatório")
    Long numeroId,

    @NotBlank(message = "Nível de tipo_numero é obrigatório")
    @Size(max = 9, message = "Nível deve ter no máximo 9 caracteres")
    String nivel,

    @NotNull(message = "Wpp é obrigatório")
    Long wppId,


) {}


)
