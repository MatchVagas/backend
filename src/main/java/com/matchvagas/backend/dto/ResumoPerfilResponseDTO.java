package com.matchvagas.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Sugestão gerada a partir do currículo. Não altera o perfil: o candidato revisa e
 * aplica. {@code desatualizado} indica que o currículo mudou depois da geração.
 */
public record ResumoPerfilResponseDTO(
        String objetivoProfissional,
        List<String> habilidadesSugeridas,
        String modelo,
        LocalDateTime geradoEm,
        boolean desatualizado
) {}