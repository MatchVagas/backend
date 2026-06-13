package com.matchvagas.backend.entity;

import java.text.Normalizer;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Estados canônicos do fluxo de uma candidatura e as transições permitidas entre eles.
 *
 * <p>Os status são persistidos como texto livre em {@link StatusCandidatura}; este enum
 * normaliza o nome (ignorando acentos, caixa e espaços) para reconhecer os estados
 * conhecidos e impor uma sequência lógica. Status que não casam com nenhum estado
 * conhecido ficam fora da validação (transição liberada), preservando a flexibilidade
 * de status customizados.
 *
 * <p>Fluxo:
 * <pre>
 *   EM_ANALISE ──► ENTREVISTA_AGENDADA ──► APROVADO
 *        │                  │
 *        └──────────────────┴────────────► REPROVADO
 * </pre>
 * {@code APROVADO} e {@code REPROVADO} são terminais.
 */
public enum FluxoStatusCandidatura {

    EM_ANALISE,
    ENTREVISTA_AGENDADA,
    APROVADO,
    REPROVADO;

    private static final Map<FluxoStatusCandidatura, Set<FluxoStatusCandidatura>> TRANSICOES = Map.of(
            EM_ANALISE,          EnumSet.of(ENTREVISTA_AGENDADA, APROVADO, REPROVADO),
            ENTREVISTA_AGENDADA, EnumSet.of(APROVADO, REPROVADO),
            APROVADO,            EnumSet.noneOf(FluxoStatusCandidatura.class),
            REPROVADO,           EnumSet.noneOf(FluxoStatusCandidatura.class)
    );

    public boolean ehTerminal() {
        return TRANSICOES.getOrDefault(this, Set.of()).isEmpty();
    }

    public boolean podeTransicionarPara(FluxoStatusCandidatura destino) {
        return TRANSICOES.getOrDefault(this, Set.of()).contains(destino);
    }

    /** Resolve o estado canônico a partir do nome livre do status (ex.: "Em Análise" → EM_ANALISE). */
    public static Optional<FluxoStatusCandidatura> fromNome(String nome) {
        if (nome == null || nome.isBlank()) return Optional.empty();
        String chave = Normalizer.normalize(nome, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")   // remove acentos
                .trim()
                .toUpperCase()
                .replaceAll("\\s+", "_");
        try {
            return Optional.of(valueOf(chave));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
