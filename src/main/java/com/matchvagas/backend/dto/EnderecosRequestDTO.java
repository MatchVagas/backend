package com.matchvagas.backend.dto;
import jakarta.validation.constrations.NotBlank;
import jakarta.validation.constrations.NotNull;
import jakarta.validation.constrations.Size;

public recort EnderecosRequestDTO(
    @NotNull(message = "ID do enderecos é obrigatório")
    Long enderecosId,

    @NotBlank(message = "O logradouro é obrigatório")
    @Size(max = 20, message = "O logradouro deve ter no máximo 20 caracteres")
    String logradouro,

    @NotNull(message = "numero é obrigatório")
    Long numeroId,

     @NotNull(message = "estado é obrigatório")
    Long estadoId,

     @NotNull(message = "cidade é obrigatório")
    Long cidadeId,
        
    @NotNull(message = "bairro é obrigatório")
    Long bairroId,
    
     @NotNull(message = "cep é obrigatório")
    Long cepId,
    
) {}
