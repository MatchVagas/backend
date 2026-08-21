package com.matchvagas.backend.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Envelope de resposta paginada com contrato estável para o front-end.
 *
 * <p>Preferido ao {@code Page<T>} cru do Spring Data, cuja serialização JSON
 * expõe estrutura interna ({@code pageable}, {@code sort}) e é explicitamente
 * documentada como instável entre versões.
 *
 * @param content        itens da página atual
 * @param page           índice da página (base 0)
 * @param size           tamanho solicitado da página
 * @param totalElements  total de registros em todas as páginas
 * @param totalPages     total de páginas
 * @param last           indica se esta é a última página
 */
public record PageResponseDTO<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PageResponseDTO<T> of(Page<T> page) {
        return new PageResponseDTO<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }
}
