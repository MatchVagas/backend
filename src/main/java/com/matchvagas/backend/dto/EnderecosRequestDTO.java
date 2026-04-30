package com.matchvagas.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EnderecosRequestDTO(

        @NotBlank(message = "Logradouro é obrigatório")
        @Size(max = 200, message = "Logradouro deve ter no máximo 200 caracteres")
        String logradouro,

        @NotBlank(message = "Número é obrigatório")
        @Size(max = 10, message = "Número deve ter no máximo 10 caracteres")
        String numero,              // era: Long numeroId — endereço usa String

        @Size(max = 100, message = "Complemento deve ter no máximo 100 caracteres")
        String completo,

        @NotBlank(message = "Bairro é obrigatório")
        @Size(max = 100, message = "Bairro deve ter no máximo 100 caracteres")
        String bairro,              // era: Long bairroId — bairro é texto

        @NotBlank(message = "CEP é obrigatório")
        @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP inválido")
        String cep,                 // era: Long cepId — CEP é texto

        Long cidadeId               // cidade referencia a tabela de cidades
) {}
