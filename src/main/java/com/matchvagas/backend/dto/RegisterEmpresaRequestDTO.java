package com.matchvagas.backend.dto;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.br.CNPJ;
import java.time.LocalDateTime;

public record RegisterEmpresaRequestDTO(

        // ── Dados do usuário ────────────────────────────────────
        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 3, max = 100)
        String nome,

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
        String senha,

        @Past(message = "Data de nascimento deve ser no passado")
        LocalDateTime dataNascimento,

        // ── Dados da empresa ────────────────────────────────────
        @NotBlank(message = "CNPJ é obrigatório")
        @CNPJ(message = "CNPJ inválido")
        String cnpj,

        @NotBlank(message = "Razão social é obrigatória")
        @Size(max = 150)
        String razaoSocial,

        @NotBlank(message = "Nome fantasia é obrigatório")
        @Size(max = 150)
        String nomeFantasia,

        @Size(max = 500)
        String descricao,

        @NotNull(message = "Porte é obrigatório")
        Long porteId,

        @NotNull(message = "Ramo de atuação é obrigatório")
        Long ramoId,

        @Pattern(regexp = "^(http|https)://.*$", message = "URL do site inválida")
        String site,

        // LGPD Art. 7º/8º — aceite explícito dos termos e da política de privacidade
        @NotNull(message = "É necessário aceitar os termos de uso e a política de privacidade")
        @AssertTrue(message = "É necessário aceitar os termos de uso e a política de privacidade")
        Boolean aceitouTermos
) {}
