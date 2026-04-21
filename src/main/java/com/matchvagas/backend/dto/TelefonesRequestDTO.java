package com.matchvagas.backend.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public record TelefonesRequestDTO(
    @NotNull(message = "ID do numero é obrigatório")
    Long numeroId,

    @NotBlank(message = "Nível de tipo_numero é obrigatório")
    @Size(max = 9, message = "Nível deve ter no máximo 9 caracteres")
    String nivel,

    @NotNull(message = "Wpp é obrigatório")
    boolean wppId

) {}
