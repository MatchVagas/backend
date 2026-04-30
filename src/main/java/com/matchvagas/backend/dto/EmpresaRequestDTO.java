package com.matchvagas.backend.dto;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.br.CNPJ;

public record EmpresaRequestDTO(

        @NotBlank(message = "CNPJ é obrigatório")
        @CNPJ(message = "CNPJ inválido")
        String cnpj,

        @NotBlank(message = "Razão social é obrigatória")
        @Size(max = 150, message = "Razão social deve ter no máximo 150 caracteres")
        String razaoSocial,

        @NotBlank(message = "Nome fantasia é obrigatório")
        @Size(max = 150, message = "Nome fantasia deve ter no máximo 150 caracteres")
        String nomeFantasia,

        @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
        String descricao,

        @NotNull(message = "Porte da empresa é obrigatório")
        Long porteId,

        @NotNull(message = "Ramo de atuação é obrigatório")
        Long ramoId,

        @Pattern(regexp = "^(http|https)://.*$", message = "URL do site inválida")
        String site,

        // Usado apenas por ADMIN para vincular a empresa a um usuário específico
        // Usuários EMPRESA ignoram este campo — a empresa é vinculada a eles automaticamente
        Long usuarioGestorId
) {}
