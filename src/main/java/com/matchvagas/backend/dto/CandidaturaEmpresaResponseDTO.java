package com.matchvagas.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Visão da candidatura para a empresa — contém apenas os dados que
 * o candidato autorizou compartilhar. Campos null são omitidos na resposta JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CandidaturaEmpresaResponseDTO(

        Long id,
        Long vagaId,
        String tituloVaga,
        LocalDateTime dataCandidatura,
        String status,

        // ── Identificação básica (sempre visível) ────────────────────────────
        Long candidatoId,
        String nomeCandidato,

        // ── Dados profissionais (visíveis conforme preferência) ──────────────

        /** Objetivo/resumo profissional */
        String objetivoProfissional,

        /** Disponibilidade para início */
        String disponibilidade,

        /** Pretensão salarial */
        BigDecimal pretensaoSalarial,

        /** Nome e caminho do currículo para download */
        String curriculoNomeArquivo,
        String curriculoCaminho,

        /** Experiências profissionais — null se o candidato não autorizou compartilhamento */
        List<ExperienciaResponseDTO> experiencias,

        /** Formações acadêmicas — null se o candidato não autorizou compartilhamento */
        List<FormacaoResponseDTO> formacoes,

        // ── Dados de contato pessoal (privados por padrão) ──────────────────

        /** Telefones de contato */
        List<String> telefones,

        /** Endereço formatado */
        String endereco
) {}
