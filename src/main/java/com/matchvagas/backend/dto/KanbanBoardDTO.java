package com.matchvagas.backend.dto;

import java.util.List;

/**
 * Funil de candidaturas de uma vaga em formato Kanban: colunas por status na ordem
 * canônica do fluxo, cada uma com seus cards (respeitando a privacidade do candidato).
 */
public record KanbanBoardDTO(
        Long vagaId,
        String tituloVaga,
        int totalCandidaturas,
        List<KanbanColunaDTO> colunas
) {}
