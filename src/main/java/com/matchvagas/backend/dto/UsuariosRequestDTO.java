package com.matchvagas.backend.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

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

        Boolean ativo) {

}
