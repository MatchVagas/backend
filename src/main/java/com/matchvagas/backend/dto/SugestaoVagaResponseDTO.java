package com.matchvagas.backend.dto;

import java.util.List;

/**
 * Vaga sugerida para um candidato com pontuação de compatibilidade e
 * explicação dos critérios que geraram a sugestão.
 */
public record SugestaoVagaResponseDTO(

        /** Dados completos da vaga */
        VagaResponseDTO vaga,

        /**
         * Pontuação de compatibilidade (0–100).
         * Quanto maior, mais a vaga corresponde ao perfil do candidato.
         */
        int pontuacao,

        /** Motivos que justificam a sugestão (exibidos ao candidato) */
        List<String> motivos
) {}
