package com.matchvagas.backend.dto;

import com.matchvagas.backend.entity.RecomendacaoTriagem;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Triagem assistida de uma candidatura. {@code desatualizado} indica que o perfil,
 * o currículo, a vaga ou as permissões de compartilhamento mudaram depois da geração.
 */
public record TriagemResponseDTO(
        Long candidaturaId,
        String parecer,
        List<String> pontosFortes,
        List<String> lacunas,
        RecomendacaoTriagem recomendacao,
        String modelo,
        LocalDateTime geradoEm,
        boolean desatualizado,
        String aviso
) {
    public static final String AVISO =
            "Sugestão gerada por IA para apoiar a triagem. A decisão final é do recrutador (LGPD, art. 20).";
}