package com.matchvagas.backend.dto;

import java.time.LocalDateTime;

public record CandidaturaResponseDTO(
        Long id,
        Long candidatoId,
        String nomeCandidato,
        Long vagaId,
        String tituloVaga,
        LocalDateTime dataCandidatura,
        String status,

        // Preferências de compartilhamento visíveis somente ao próprio candidato
        boolean compartilharObjetivoProfissional,
        boolean compartilharDisponibilidade,
        boolean compartilharPretensaoSalarial,
        boolean compartilharCurriculo,
        boolean compartilharExperiencias,
        boolean compartilharFormacoes,
        boolean compartilharTelefone,
        boolean compartilharEndereco
) {}
