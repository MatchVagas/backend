package com.matchvagas.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public record CandidatoResponseDTO(
        Long id,                     // ID do candidato (da tabela candidatos)
        Long usuarioId,              // ID do usuário associado (chave estrangeira)
        String nome,
        String email,
        Date dataNascimento,
        Integer idade,
        List<TelefonesResponseDTO> telefones,
        String cpf,
        EnderecosResponseDTO endereco,
        String objetivoProfissional,
        BigDecimal pretensaoSalarial,
        String disponibilidade,
        LocalDateTime dataCadastro,
        Boolean ativo
) {}