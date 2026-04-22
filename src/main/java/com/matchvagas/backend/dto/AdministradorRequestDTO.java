package com.matchvagas.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdministradorRequestDTO(

        @NotNull(message = "ID do usuário é obrigatório")
        Long usuarioId,

        @NotBlank(message = "Nível de acesso é obrigatório")
        @Size(max = 50, message = "Nível deve ter no máximo 50 caracteres")
        String nivel,

        @NotNull(message = "Departamento é obrigatório")
        Long departamentoId,

        @Size(max = 500, message = "Permissões devem ter no máximo 500 caracteres")
        String permissoes
) {}
