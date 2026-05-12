package com.matchvagas.backend.dto;

import jakarta.validation.constraints.NotNull;

public record AtualizarCompartilhamentoRequestDTO(

        @NotNull Boolean compartilharObjetivoProfissional,
        @NotNull Boolean compartilharDisponibilidade,
        @NotNull Boolean compartilharPretensaoSalarial,
        @NotNull Boolean compartilharCurriculo,
        @NotNull Boolean compartilharExperiencias,
        @NotNull Boolean compartilharFormacoes,
        @NotNull Boolean compartilharTelefone,
        @NotNull Boolean compartilharEndereco
) {}
