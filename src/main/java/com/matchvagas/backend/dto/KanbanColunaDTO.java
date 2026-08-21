package com.matchvagas.backend.dto;

import java.util.List;

/**
 * Uma coluna do funil Kanban: um status e as candidaturas nele.
 *
 * @param statusId id do status ({@code null} na coluna "Novas", que reúne candidaturas
 *                 ainda sem status). Use este id no PATCH de mudança de status ao mover
 *                 o card para outra coluna.
 */
public record KanbanColunaDTO(
        Integer statusId,
        String status,
        int total,
        List<CandidaturaEmpresaResponseDTO> candidaturas
) {}
