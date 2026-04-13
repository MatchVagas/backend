package com.matchvagas.backend.mapper;

import com.matchvagas.backend.dto.UsuarioResponseDTO;
import com.matchvagas.backend.dto.UsuariosRequestDTO;
import com.matchvagas.backend.entity.Usuarios;

public interface UsuarioMapper {

    Usuarios toEntity(UsuariosRequestDTO dto);

    UsuarioResponseDTO toResponseDTO(Usuarios entity);
    
} 
