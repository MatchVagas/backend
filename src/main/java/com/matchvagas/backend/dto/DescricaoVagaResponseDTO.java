package com.matchvagas.backend.dto;

import java.time.LocalDateTime;

/** Rascunho para a empresa editar antes de publicar. Nada é salvo na vaga automaticamente. */
public record DescricaoVagaResponseDTO(
        String descricao,
        String requisitos,
        String modelo,
        LocalDateTime geradoEm
) {}