package com.matchvagas.backend.dto.vaga;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VagaRequestDTO(

        @NotNull(message = "ID da empresa é obrigatório")
        Long empresaId,

        @NotBlank(message = "Título é obrigatório")
        @Size(min = 5, max = 255, message = "Título deve ter entre 5 e 255 caracteres")
        String titulo,

        @NotBlank(message = "Descrição é obrigatória")
        @Size(min = 20, message = "Descrição deve ter no mínimo 20 caracteres")
        String descricao,

        @NotBlank(message = "Requisitos são obrigatórios")
        String requisitos,

        @NotNull(message = "Tipo da vaga é obrigatório")
        Long tipoVagaId,

        @NotNull(message = "Modalidade é obrigatória")
        Long modalidadeId,

        @NotNull(message = "Salário mínimo é obrigatório")
        @DecimalMin(value = "0.0", message = "Salário mínimo não pode ser negativo")
        BigDecimal salarioMinimo,

        @NotNull(message = "Salário máximo é obrigatório")
        @DecimalMin(value = "0.0", message = "Salário máximo não pode ser negativo")
        BigDecimal salarioMaximo,

        String beneficios,

        @NotBlank(message = "Carga horária é obrigatória")
        String cargaHoraria,

        @Min(value = 16, message = "Idade mínima deve ser maior ou igual a 16 anos")
        Integer idadeMinima,

        @Min(value = 16, message = "Idade máxima deve ser maior ou igual a 16 anos")
        Integer idadeMaxima,

        @NotNull(message = "Nível de escolaridade mínimo é obrigatório")
        Long escolaridadeId,

        @NotBlank(message = "Área de atuação é obrigatória")
        @Size(max = 100)
        String areaAtuacao,

        @FutureOrPresent(message = "Data de expiração deve ser hoje ou no futuro")
        LocalDateTime dataExpiracao,

        @NotNull(message = "Status da vaga é obrigatório")
        Long statusVagaId,

        @Min(value = 1, message = "Número de vagas deve ser no mínimo 1")
        Integer numeroVagas,

        @NotNull(message = "Cidade é obrigatória")
        Long cidadeId

) {
}