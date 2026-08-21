package com.matchvagas.backend.dto;

import java.time.LocalDateTime;

/**
 * @param remetentePapel "CANDIDATO" ou "EMPRESA" — de que lado da conversa veio a
 *                       mensagem, para o front alinhar/estilizar a bolha sem precisar
 *                       cruzar IDs.
 */
public record MensagemResponseDTO(
        Long id,
        Long candidaturaId,
        Long remetenteId,
        String remetenteNome,
        String remetentePapel,
        String conteudo,
        LocalDateTime dataEnvio,
        boolean lida
) {}
