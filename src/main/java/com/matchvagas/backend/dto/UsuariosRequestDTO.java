package com.matchvagas.backend.dto;

import com.matchvagas.backend.entity.Usuarios.TipoUsuario;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record UsuariosRequestDTO(

        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
        String nome,

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email deve ser válido")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
        String senha,

        @NotNull(message = "Data de nascimento é obrigatória")
        @Past(message = "Data de nascimento deve ser no passado")
        LocalDateTime dataNascimento,

        // ADICIONADO: tipo obrigatório no cadastro
        @NotNull(message = "Tipo de usuário é obrigatório (CANDIDATO, EMPRESA ou ADMIN)")
        TipoUsuario tipoUsuario,

        Boolean ativo,

        // LGPD Art. 7º/8º — aceite explícito dos termos e da política de privacidade
        @NotNull(message = "É necessário aceitar os termos de uso e a política de privacidade")
        @AssertTrue(message = "É necessário aceitar os termos de uso e a política de privacidade")
        Boolean aceitouTermos
) {}
