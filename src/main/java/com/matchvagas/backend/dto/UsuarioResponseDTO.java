package com.matchvagas.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.matchvagas.backend.entity.Usuarios.TipoUsuario;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        Integer idade,
        Boolean ativo,
        TipoUsuario tipoUsuario,
        LocalDateTime dataCadastro,
        LocalDateTime dataUltimoAcesso,
        List<TelefonesResponseDTO> telefones
) {}
