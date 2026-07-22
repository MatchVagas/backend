package com.matchvagas.backend.dto;

import java.math.BigDecimal;

/**
 * Critérios da busca avançada de vagas (Fase 2). Todos opcionais — {@code null}/vazio =
 * "não filtra". Convertido em {@code Specification} portátil (Postgres e MySQL).
 *
 * @param termo        texto livre buscado em título, descrição, requisitos e área
 * @param salarioMin   piso desejado — casa vagas cujo teto (salarioMaximo) é ≥ este valor
 * @param salarioMax   teto desejado — casa vagas cujo piso (salarioMinimo) é ≤ este valor
 * @param apenasAtivas quando true, só vagas ATIVA e não expiradas
 */
public record VagaBuscaFiltro(
        String termo,
        String areaAtuacao,
        Long tipoVagaId,
        Long modalidadeId,
        Long escolaridadeId,
        Long cidadeId,
        Long estadoId,
        String nomeEmpresa,
        BigDecimal salarioMin,
        BigDecimal salarioMax,
        boolean apenasAtivas
) {}
