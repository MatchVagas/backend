package com.matchvagas.backend.mapper;

import org.mapstruct.Mapper;

import com.matchvagas.backend.dto.AdministradorRequestDTO;
import com.matchvagas.backend.entity.Administradores;

@Mapper(componentModel = "spring")
public interface AdministradoresMapper {

    Administradores toEntity(AdministradorRequestDTO dto);

    AdministradorRequestDTO toResponseDTO(Administradores entity);
    
}