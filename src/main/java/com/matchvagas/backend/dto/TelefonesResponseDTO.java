package com.matchvagas.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TelefonesResponseDTO(
    Long id,
    String numeroTelefones,
    String tipo_numeroTelefones,
    String wppTelefones

) {
    public TelefonesResponseDTO(Long id, String numeroTelefones, String tipo_numeroTelefones, String wppTelefones) {
        this.id = id;
        this.numeroTelefones = numeroTelefones;
        this.tipo_numeroTelefones = tipo_numeroTelefones;
        this.wppTelefones = wppTelefones;
    }
}