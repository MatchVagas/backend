package com.matchvagas.backend.dto;

import jakarta.validation.constraints.NotNull;

public record CandidaturaRequestDTO(

        @NotNull(message = "ID da vaga é obrigatório")
        Long vagaId,

        // ── Preferências de compartilhamento ────────────────────────────────
        // null = usa o padrão definido na entidade (true para dados profissionais,
        //        false para dados de contato pessoal)

        /** Objetivo/resumo profissional — padrão: compartilhado */
        Boolean compartilharObjetivoProfissional,

        /** Disponibilidade — padrão: compartilhado */
        Boolean compartilharDisponibilidade,

        /** Pretensão salarial — padrão: compartilhado */
        Boolean compartilharPretensaoSalarial,

        /** Currículo (arquivo) — padrão: compartilhado */
        Boolean compartilharCurriculo,

        /** Experiências profissionais — padrão: compartilhado */
        Boolean compartilharExperiencias,

        /** Formações acadêmicas — padrão: compartilhado */
        Boolean compartilharFormacoes,

        /** Telefone de contato — padrão: privado */
        Boolean compartilharTelefone,

        /** Endereço residencial — padrão: privado */
        Boolean compartilharEndereco
) {}
