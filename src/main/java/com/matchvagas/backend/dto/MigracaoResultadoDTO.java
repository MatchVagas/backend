package com.matchvagas.backend.dto;

import java.util.List;

/**
 * Resultado de uma migração operacional pontual (backfill de CPF, normalização
 * de URLs de imagem, etc.). Todas as migrações são idempotentes: registros já
 * processados são contabilizados em {@code jaProcessados} e ignorados.
 *
 * @param operacao       identificação da migração executada
 * @param total          total de registros avaliados
 * @param atualizados    registros efetivamente migrados nesta execução
 * @param jaProcessados  registros que já estavam no formato novo (ignorados)
 * @param semValor       registros sem o dado a migrar (ex.: sem CPF / sem imagem)
 * @param erros          registros que falharam ao migrar
 * @param mensagens      detalhes por registro com erro (para diagnóstico)
 */
public record MigracaoResultadoDTO(
        String operacao,
        int total,
        int atualizados,
        int jaProcessados,
        int semValor,
        int erros,
        List<String> mensagens
) {}
