package com.matchvagas.backend.dto;

public record EstatisticasSistemaDTO(
        long totalUsuarios,
        long usuariosAtivos,
        long usuariosInativos,
        long totalCandidatos,
        long totalEmpresas,
        long totalVagas,
        long totalCandidaturas,
        long totalAdmins
) {}
